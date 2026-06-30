package fr.abes.theses.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principale de démarrage de l'application Spring Boot.
 */
@SpringBootApplication
public class ThesesBatchApplication {

    /**
     * Méthode d'entrée Java Standard lancant l'application.
     *
     * @param args Arguments passés en ligne de commande.
     */
    public static void main(String[] args) {
        SpringApplication.run(ThesesBatchApplication.class, args);
    }
}
