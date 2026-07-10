package fr.abes.theses.batch.reader;

import fr.abes.theses.batch.model.Thesis;
import fr.abes.theses.batch.model.ThesesResponse;
import org.springframework.batch.item.ItemReader;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * ItemReader personnalisé pour lire les thèses de theses.fr de façon paginée.
 * Ce reader utilise le RestClient de Spring Boot 3 pour interroger l'API REST.
 */
public class ThesesItemReader implements ItemReader<Thesis> {
    private static final Logger log = LoggerFactory.getLogger(ThesesItemReader.class);

    private final RestClient restClient;
    private final int pageSize;
    private final boolean modeTest;
    private static final int MAX_ITEMS_FOR_TEST = 50;

    private int currentDebut = 0;
    private List<Thesis> currentBatch;
    private int currentIndex = 0;
    private boolean isFinished = false;

    /**
     * Constructeur pour initialiser le Reader paginé.
     *
     * @param restClient Client HTTP configuré.
     * @param pageSize   Taille d'une page de résultats.
     * @param modeTest   Vrai si le mode test (limité) est activé.
     */
    public ThesesItemReader(RestClient restClient, int pageSize, boolean modeTest) {
        this.restClient = restClient;
        this.pageSize = pageSize;
        this.modeTest = modeTest;
    }

    /**
     * Lit l'élément suivant. S'il n'y a plus d'éléments disponibles dans le cache
     * local,
     * interroge l'API pour charger la page suivante.
     *
     * @return Une thèse (Thesis), ou null s'il n'y a plus aucun élément à lire.
     */
    @Override
    public Thesis read() throws Exception {
        if (currentBatch == null || currentIndex >= currentBatch.size()) {
            if (isFinished) {
                return null;
            }
            fetchNextBatch();
            if (currentBatch == null || currentBatch.isEmpty()) {
                return null;
            }
            currentIndex = 0;
        }

        return currentBatch.get(currentIndex++);
    }

    /**
     * Récupère la page suivante de thèses depuis l'API REST.
     * En mode test, s'arrête dès que la limite configurée (50) est atteinte.
     */
    private void fetchNextBatch() {
        if (modeTest && currentDebut >= MAX_ITEMS_FOR_TEST) {
            log.info("Mode test actif : arret de la recuperation apres {} theses.", MAX_ITEMS_FOR_TEST);
            isFinished = true;
            currentBatch = null;
            return;
        }

        Integer nombre = modeTest ? Math.min(pageSize, MAX_ITEMS_FOR_TEST - currentDebut) : pageSize;

        log.info("Recuperation des theses de l'API: debut={}, nombre={}", currentDebut, nombre);

        ThesesResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/theses/recherche/")
                        .queryParam("q", "*")
                        .queryParam("debut", currentDebut)
                        .queryParam("nombre", nombre)
                        .queryParam("filtres", "[Statut=\"soutenue\"]")
                        .build())
                .retrieve()
                .body(ThesesResponse.class);

        if (response == null || response.theses() == null || response.theses().isEmpty()) {
            log.info("Fin de la recuperation. Aucun resultat ou liste vide renvoyee.");
            isFinished = true;
            currentBatch = null;
            return;
        }

        currentBatch = response.theses();
        currentDebut += currentBatch.size();

        log.info("Page recue avec succes. Nombre d'elements: {}, TotalHits estime: {}", currentBatch.size(),
                response.totalHits());

        // Si nous avons récupéré moins d'éléments que la taille de page demandée,
        // nous sommes arrivés à la fin du jeu de données.
        if (currentBatch.size() < nombre) {
            log.info("Fin de la recuperation de l'API (derniere page incomplete).");
            isFinished = true;
        }
    }
}
