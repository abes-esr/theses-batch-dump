package fr.abes.theses.batch.scheduler;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Classe chargée de décider si le Job du Batch doit être lancé.
 * Implémente la règle métier d'exécution tous les 6 mois depuis la dernière
 * réussite
 * et gère également l'option de forçage manuel (FORCE_RUN).
 */
@SuppressWarnings("null")
public class JobRunDecider {
    private static final Logger log = LoggerFactory.getLogger(JobRunDecider.class);

    private final JobExplorer jobExplorer;
    private final String jobName;
    private final boolean forceRun;
    private final boolean modeTest;

    /**
     * Constructeur initialisant les dépendances pour la décision.
     *
     * @param jobExplorer Explorateur de métadonnées du Batch.
     * @param jobName     Nom du job à inspecter.
     * @param forceRun    Vrai si le lancement immédiat est forcé.
     * @param modeTest    Vrai si le mode test est activé.
     */
    public JobRunDecider(JobExplorer jobExplorer, String jobName, boolean forceRun, boolean modeTest) {
        this.jobExplorer = jobExplorer;
        this.jobName = jobName;
        this.forceRun = forceRun;
        this.modeTest = modeTest;
    }

    /**
     * Analyse les exécutions passées pour décider si le job doit être exécuté.
     *
     * @return true si le job doit être exécuté, false sinon.
     */
    public boolean shouldRun() {
        // 1. Vérification du mode test ou de l'option de forçage
        if (modeTest) {
            log.info("Mode test actif. Le controle de la regle des 6 mois est ignore.");
            return true;
        }
        if (forceRun || "true".equalsIgnoreCase(System.getenv("FORCE_RUN"))) {
            log.info("Forcage d'execution actif via parametre ou variable d'environnement FORCE_RUN.");
            return true;
        }

        try {
            // 2. Récupération du nombre d'instances existantes pour ce Job
            long jobInstanceCount = jobExplorer.getJobInstanceCount(jobName);
            if (jobInstanceCount == 0) {
                log.info("Aucune execution precedente detectee pour le job '{}'. Lancement requis.", jobName);
                return true;
            }

            // 3. Parcours des dernières instances à la recherche d'une exécution terminée
            // avec succès
            List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 20);
            return checkPastExecutions(jobInstances);
        } catch (NoSuchJobException e) {
            log.info("Aucune execution precedente (job non trouve) detectee pour le job '{}'. Lancement requis.",
                    jobName);
            return true;
        }
    }

    /**
     * Parcourt les instances de job fournies à la recherche de la dernière
     * exécution réussie.
     *
     * @param jobInstances Liste des instances à inspecter.
     * @return true si le job doit être exécuté, false sinon.
     */
    private boolean checkPastExecutions(List<JobInstance> jobInstances) {
        for (JobInstance instance : jobInstances) {
            List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
            for (JobExecution execution : executions) {
                Optional<Boolean> decision = checkExecutionStatus(execution);
                if (decision.isPresent()) {
                    return decision.get();
                }
            }
        }
        // 4. Si aucune execution precedente n'etait COMPLETE (que des echecs ou
        // abandons),
        // on lance le Job.
        log.info(
                "Aucune execution precedente n'a le statut COMPLETE. Lancement du Job pour correction/initialisation.");
        return true;
    }

    /**
     * Évalue si une exécution spécifique s'est terminée avec succès et calcule la
     * décision de planification.
     *
     * @param execution L'exécution de job à évaluer.
     * @return Boolean.TRUE si le job doit s'exécuter, Boolean.FALSE si l'exécution
     *         est récente, null si l'exécution n'est pas COMPLETED.
     */
    private Optional<Boolean> checkExecutionStatus(JobExecution execution) {
        if (execution.getStatus() == BatchStatus.COMPLETED) {
            LocalDateTime endTime = execution.getEndTime();
            if (endTime != null) {
                log.info("Derniere execution reussie detectee le : {}.", endTime);
                LocalDateTime sixMonthsAgo = LocalDateTime.now(ZoneId.systemDefault()).minusMonths(6);

                if (endTime.isBefore(sixMonthsAgo)) {
                    log.info("La derniere execution reussie remonte a plus de 6 mois. Execution requise.");
                    return Optional.of(Boolean.TRUE);
                } else {
                    log.info("La derniere execution reussie remonte a moins de 6 mois. Execution non requise.");
                    return Optional.of(Boolean.FALSE);
                }
            }
        }
        return Optional.empty();
    }
}
