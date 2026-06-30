package fr.abes.theses.batch.scheduler;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runner exécuté au démarrage de l'application.
 * Utilisé lorsque le planificateur interne est désactivé
 * (app.scheduler.enabled=false).
 * Effectue l'évaluation, exécute le Batch si nécessaire, puis éteint proprement
 * l'application.
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "false", matchIfMissing = true)
public class StartupJobRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupJobRunner.class);

    private final JobLauncherService jobLauncherService;
    private final ApplicationContext context;

    /**
     * Constructeur injectant le service de lancement et le contexte applicatif.
     */
    public StartupJobRunner(JobLauncherService jobLauncherService, ApplicationContext context) {
        this.jobLauncherService = jobLauncherService;
        this.context = context;
    }

    /**
     * Point d'entrée CommandLineRunner. Déclenche le Batch puis arrête la JVM.
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("Demarrage de l'application en mode execution unique (tache ponctuelle).");
        jobLauncherService.checkAndRunJob();
        log.info("Execution terminee. Arret propre de l'application Spring Boot...");

        int exitCode = 0;
        SpringApplication.exit(context, () -> exitCode);
        System.exit(exitCode);
    }
}
