package fr.abes.theses.batch.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.lang.NonNull;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

/**
 * Tasklet Spring Batch chargée de téléverser les fichiers CSV, JSON et NDJSON
 * générés
 * vers data.gouv.fr via l'API Multipart.
 * 
 * Si le mode test est activé, l'upload est ignoré.
 */
@SuppressWarnings("null")
public class UploadTasklet implements Tasklet {
    private static final Logger log = LoggerFactory.getLogger(UploadTasklet.class);

    private final RestClient restClient;
    private final String datasetId;
    private final String csvResourceId;
    private final String jsonResourceId;
    private final String ndjsonResourceId;
    private final String apiKey;
    private final String outputDir;
    private final boolean uploadEnabled;
    private final String exportDate;

    /**
     * Constructeur initialisant les dépendances et les clés de ressources
     * data.gouv.fr ainsi que la date d'export.
     */
    @SuppressWarnings("java:S107")
    public UploadTasklet(RestClient restClient, String datasetId, String csvResourceId,
            String jsonResourceId, String ndjsonResourceId, String apiKey,
            String outputDir, boolean uploadEnabled, String exportDate) {
        this.restClient = restClient;
        this.datasetId = datasetId;
        this.csvResourceId = csvResourceId;
        this.jsonResourceId = jsonResourceId;
        this.ndjsonResourceId = ndjsonResourceId;
        this.apiKey = apiKey;
        this.outputDir = outputDir;
        this.uploadEnabled = uploadEnabled;
        this.exportDate = exportDate;
    }

    /**
     * Exécute le téléversement des fichiers générés.
     */
    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext)
            throws Exception {
        if (!uploadEnabled) {
            log.info("Televersement vers data.gouv.fr desactive (app.datagouv.upload-enabled = false).");
            log.info("Les 3 fichiers d'export ont ete deposes et restent disponibles dans le dossier : {}",
                    new File(outputDir).getAbsolutePath());
            return RepeatStatus.FINISHED;
        }

        log.info("Debut du televersement des exports vers data.gouv.fr...");

        uploadFile("CSV", csvResourceId, "theses-soutenues-" + exportDate + ".csv", "theses-soutenues.csv");
        uploadFile("JSON", jsonResourceId, "theses-soutenues-" + exportDate + ".json", "theses-soutenues.json");
        uploadFile("NDJSON", ndjsonResourceId, "theses-soutenues-" + exportDate + ".ndjson",
                "theses-soutenues.ndjson");

        log.info("Tous les fichiers ont ete televerses avec succes sur data.gouv.fr.");
        return RepeatStatus.FINISHED;
    }

    /**
     * Téléverse un fichier spécifique vers data.gouv.fr.
     *
     * @param formatName       Nom lisible du format (ex: "CSV").
     * @param resourceId       ID de ressource data.gouv.fr cible.
     * @param localFileName    Nom du fichier local dans le répertoire de sortie.
     * @param targetUploadName Nom sous lequel le fichier doit être téléversé.
     */
    private void uploadFile(String formatName, String resourceId, String localFileName, String targetUploadName) {
        File file = new File(outputDir, localFileName);
        if (!file.exists()) {
            throw new IllegalStateException("Le fichier " + file.getAbsolutePath() + " a televerser n'existe pas.");
        }

        log.info("Televersement du fichier {} ({}) vers data.gouv.fr sous le nom {}. Taille: {} octets.",
                localFileName, formatName, targetUploadName, file.length());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file) {
            @Override
            public String getFilename() {
                return targetUploadName;
            }
        });

        restClient.post()
                .uri("https://www.data.gouv.fr/api/1/datasets/{datasetId}/resources/{resourceId}/upload/",
                        datasetId, resourceId)
                .header("X-API-KEY", apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Televersement reussi pour le fichier {} sous le nom {}.", localFileName, targetUploadName);
    }
}
