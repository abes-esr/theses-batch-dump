package fr.abes.theses.batch.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Planificateur interne exécutant périodiquement le contrôle de lancement du
 * Job.
 * Activé uniquement lorsque la propriété app.scheduler.enabled est à true.
 * L'annotation @EnableScheduling active le support de planification Spring.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class ScheduledJobRunner {
    private static final Logger log = LoggerFactory.getLogger(ScheduledJobRunner.class);

    private final JobLauncherService jobLauncherService;

    /**
     * Constructeur injectant le service de lancement.
     */
    public ScheduledJobRunner(JobLauncherService jobLauncherService) {
        this.jobLauncherService = jobLauncherService;
    }

    /**
     * Tâche planifiée. La fréquence est configurée via la propriété
     * 'app.scheduler.cron'.
     * Par défaut, s'exécute chaque jour à 2h00 du matin.
     */
    @Scheduled(cron = "${app.scheduler.cron:0 0 2 * * ?}")
    public void runJobOnSchedule() {
        log.info("Planificateur interne : declenchement du controle periodique de la tache...");
        jobLauncherService.checkAndRunJob();
    }
}
