package fr.abes.theses.batch.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service chargé d'orchestrer la vérification des conditions de lancement
 * et de déclencher l'exécution effective du Job Spring Batch.
 */
@Service
@SuppressWarnings("null")
public class JobLauncherService {
    private static final Logger log = LoggerFactory.getLogger(JobLauncherService.class);

    private final JobLauncher jobLauncher;
    private final Job thesesUpdateJob;
    private final JobExplorer jobExplorer;

    @Value("${app.force-run:false}")
    private boolean forceRun;

    @Value("${app.mode-test:false}")
    private boolean modeTest;

    @Value("${app.run-time:0}")
    private long cmdRunTime;

    /**
     * Constructeur injectant les composants Spring Batch requis.
     */
    public JobLauncherService(JobLauncher jobLauncher, Job thesesUpdateJob, JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.thesesUpdateJob = thesesUpdateJob;
        this.jobExplorer = jobExplorer;
    }

    /**
     * Evalue la règle métier (6 mois) et lance le job si nécessaire.
     *
     * @return true si le job s'est lancé (avec succès ou erreur), false s'il a été
     *         ignoré.
     */
    public boolean checkAndRunJob() {
        log.info("Verification des conditions d'execution de la tâche thesesUpdateJob...");

        JobRunDecider decider = new JobRunDecider(jobExplorer, thesesUpdateJob.getName(), forceRun, modeTest);

        if (decider.shouldRun()) {
            log.info("Conditions reunies. Demarrage de la tache...");
            try {
                // Calcul de la date du jour pour l'export des fichiers
                String exportDate = java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                // Si un run-time spécifique est passé en paramètre, on l'utilise pour
                // permettre la reprise du job existant (sinon on génère un nouveau timestamp).
                long runTimeVal = (cmdRunTime > 0) ? cmdRunTime : System.currentTimeMillis();

                JobParameters params = new JobParametersBuilder()
                        .addLong("runTime", runTimeVal)
                        .addString("exportDate", exportDate)
                        .toJobParameters();

                jobLauncher.run(thesesUpdateJob, params);
                log.info("Execution de la tache terminee avec succes.");
                return true;
            } catch (Exception e) {
                log.error("Erreur critique lors de l'execution de la tache thesesUpdateJob", e);
                return true; // Retourne true car l'action a été tentée
            }
        } else {
            log.info("La tache thesesUpdateJob a ete ignoree (Regle des 6 mois non echue).");
            return false;
        }
    }
}
