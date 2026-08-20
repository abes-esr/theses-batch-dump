package fr.abes.theses.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.theses.batch.model.Thesis;
import fr.abes.theses.batch.model.ExportThesis;
import fr.abes.theses.batch.processor.ThesisProcessor;
import fr.abes.theses.batch.reader.ThesesItemReader;
import fr.abes.theses.batch.tasklet.UploadTasklet;
import fr.abes.theses.batch.writer.NdjsonLineAggregator;
import fr.abes.theses.batch.writer.ThesisCsvLineAggregator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.List;

/**
 * Configuration globale de la tâche (Job) Spring Batch.
 * Définit les Readers, Writers, les étapes (Steps) et le Job principal.
 */
@Configuration
@SuppressWarnings("null")
public class BatchConfig {
    private static final Logger log = LoggerFactory.getLogger(BatchConfig.class);

    /**
     * Bean RestClient configuré pour requêter l'API theses.fr.
     */
    @Bean
    public RestClient thesesRestClient(@Value("${app.theses.api-base-url}") String apiBaseUrl) {
        log.info("Initialisation du RestClient de theses.fr avec l'URL de base : {}", apiBaseUrl);
        
        // Configuration des timeouts de connexion et lecture à 10 secondes
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000); // 10s
        requestFactory.setReadTimeout(10000);    // 10s

        return RestClient.builder()
                .baseUrl(apiBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Bean RestClient générique pour téléverser sur data.gouv.fr.
     */
    @Bean
    public RestClient dataGouvRestClient() {
        return RestClient.builder().build();
    }

    /**
     * Reader récupérant les thèses de l'API.
     */
    @Bean
    public ThesesItemReader thesesReader(RestClient thesesRestClient,
            @Value("${app.theses.page-size:1000}") int pageSize,
            @Value("${app.mode-test:false}") boolean modeTest) {
        return new ThesesItemReader(thesesRestClient, pageSize, modeTest);
    }

    /**
     * Processeur nettoyant et harmonisant les données des thèses avant écriture.
     */
    @Bean
    public ThesisProcessor thesisProcessor() {
        return new ThesisProcessor();
    }

    /**
     * Writer écrivant les thèses au format CSV avec BOM UTF-8 et en-tête.
     */
    @Bean
    @StepScope
    public FlatFileItemWriter<ExportThesis> csvWriter(@Value("${app.output-dir}") String outputDir,
            @Value("#{jobParameters['exportDate']}") String exportDate) {
        FlatFileItemWriter<ExportThesis> writer = new FlatFileItemWriter<>();
        writer.setName("csvWriter");
        writer.setResource(new FileSystemResource(new File(outputDir, "theses-soutenues-" + exportDate + ".csv")));
        writer.setEncoding("UTF-8");
        // Écriture du BOM UTF-8 (\uFEFF) suivi de l'en-tête CSV de 243 colonnes attendu par data.gouv.fr
        writer.setHeaderCallback(headerWriter -> headerWriter.write(
                "\uFEFF\"accessible\",\"auteur.idref\",\"auteur.nom\",\"auteur.prenom\",\"cas\",\"code_etab\",\"date_soutenance\",\"directeurs_these.0.idref\",\"directeurs_these.0.nom\",\"directeurs_these.0.prenom\",\"directeurs_these.1.idref\",\"directeurs_these.1.nom\",\"directeurs_these.1.prenom\",\"directeurs_these.2.idref\",\"directeurs_these.2.nom\",\"directeurs_these.2.prenom\",\"directeurs_these.3.idref\",\"directeurs_these.3.nom\",\"directeurs_these.3.prenom\",\"directeurs_these.4.idref\",\"directeurs_these.4.nom\",\"directeurs_these.4.prenom\",\"directeurs_these.5.idref\",\"directeurs_these.5.nom\",\"directeurs_these.5.prenom\",\"directeurs_these.6.idref\",\"directeurs_these.6.nom\",\"directeurs_these.6.prenom\",\"discipline\",\"ecoles_doctorales.0.idref\",\"ecoles_doctorales.0.nom\",\"ecoles_doctorales.1.idref\",\"ecoles_doctorales.1.nom\",\"embargo\",\"etablissements_soutenance.0.idref\",\"etablissements_soutenance.0.nom\",\"etablissements_soutenance.1.idref\",\"etablissements_soutenance.1.nom\",\"etablissements_soutenance.2.idref\",\"etablissements_soutenance.2.nom\",\"etablissements_soutenance.3.idref\",\"etablissements_soutenance.3.nom\",\"etablissements_soutenance.4.idref\",\"etablissements_soutenance.4.nom\",\"langues.0\",\"langues.1\",\"langues.2\",\"langues.3\",\"membres_jury.0.idref\",\"membres_jury.0.nom\",\"membres_jury.0.prenom\",\"membres_jury.10.idref\",\"membres_jury.10.nom\",\"membres_jury.10.prenom\",\"membres_jury.11.idref\",\"membres_jury.11.nom\",\"membres_jury.11.prenom\",\"membres_jury.1.idref\",\"membres_jury.1.nom\",\"membres_jury.1.prenom\",\"membres_jury.2.idref\",\"membres_jury.2.nom\",\"membres_jury.2.prenom\",\"membres_jury.3.idref\",\"membres_jury.3.nom\",\"membres_jury.3.prenom\",\"membres_jury.4.idref\",\"membres_jury.4.nom\",\"membres_jury.4.prenom\",\"membres_jury.5.idref\",\"membres_jury.5.nom\",\"membres_jury.5.prenom\",\"membres_jury.6.idref\",\"membres_jury.6.nom\",\"membres_jury.6.prenom\",\"membres_jury.7.idref\",\"membres_jury.7.nom\",\"membres_jury.7.prenom\",\"membres_jury.8.idref\",\"membres_jury.8.nom\",\"membres_jury.8.prenom\",\"membres_jury.9.idref\",\"membres_jury.9.nom\",\"membres_jury.9.prenom\",\"nnt\",\"oai_set_specs\",\"partenaires_recherche.0.idref\",\"partenaires_recherche.0.nom\",\"partenaires_recherche.0.type\",\"partenaires_recherche.1.idref\",\"partenaires_recherche.1.nom\",\"partenaires_recherche.1.type\",\"partenaires_recherche.2.idref\",\"partenaires_recherche.2.nom\",\"partenaires_recherche.2.type\",\"partenaires_recherche.3.idref\",\"partenaires_recherche.3.nom\",\"partenaires_recherche.3.type\",\"partenaires_recherche.4.idref\",\"partenaires_recherche.4.nom\",\"partenaires_recherche.4.type\",\"partenaires_recherche.5.idref\",\"partenaires_recherche.5.nom\",\"partenaires_recherche.5.type\",\"partenaires_recherche.6.idref\",\"partenaires_recherche.6.nom\",\"partenaires_recherche.6.type\",\"partenaires_recherche.7.idref\",\"partenaires_recherche.7.nom\",\"partenaires_recherche.7.type\",\"president_jury.idref\",\"president_jury.nom\",\"president_jury.prenom\",\"rapporteurs.0.idref\",\"rapporteurs.0.nom\",\"rapporteurs.0.prenom\",\"rapporteurs.1.idref\",\"rapporteurs.1.nom\",\"rapporteurs.1.prenom\",\"rapporteurs.2.idref\",\"rapporteurs.2.nom\",\"rapporteurs.2.prenom\",\"rapporteurs.3.idref\",\"rapporteurs.3.nom\",\"rapporteurs.3.prenom\",\"rapporteurs.4.idref\",\"rapporteurs.4.nom\",\"rapporteurs.4.prenom\",\"rapporteurs.5.idref\",\"rapporteurs.5.nom\",\"rapporteurs.5.prenom\",\"resumes.autre.0\",\"resumes.autre.1\",\"resumes.autre.2\",\"resumes.autre.3\",\"resumes.autre.4\",\"resumes.autre.5\",\"resumes.en\",\"resumes.fr\",\"source\",\"status\",\"sujets.autre.0\",\"sujets.autre.1\",\"sujets.autre.2\",\"sujets.autre.3\",\"sujets.autre.4\",\"sujets.autre.5\",\"sujets.autre.6\",\"sujets.autre.7\",\"sujets.en\",\"sujets.fr\",\"sujets_rameau.0\",\"sujets_rameau.1\",\"sujets_rameau.10\",\"sujets_rameau.11\",\"sujets_rameau.12\",\"sujets_rameau.13\",\"sujets_rameau.14\",\"sujets_rameau.15\",\"sujets_rameau.16\",\"sujets_rameau.17\",\"sujets_rameau.18\",\"sujets_rameau.19\",\"sujets_rameau.2\",\"sujets_rameau.20\",\"sujets_rameau.21\",\"sujets_rameau.22\",\"sujets_rameau.23\",\"sujets_rameau.24\",\"sujets_rameau.25\",\"sujets_rameau.26\",\"sujets_rameau.27\",\"sujets_rameau.28\",\"sujets_rameau.29\",\"sujets_rameau.3\",\"sujets_rameau.30\",\"sujets_rameau.31\",\"sujets_rameau.32\",\"sujets_rameau.33\",\"sujets_rameau.34\",\"sujets_rameau.35\",\"sujets_rameau.36\",\"sujets_rameau.37\",\"sujets_rameau.38\",\"sujets_rameau.39\",\"sujets_rameau.4\",\"sujets_rameau.40\",\"sujets_rameau.41\",\"sujets_rameau.42\",\"sujets_rameau.43\",\"sujets_rameau.44\",\"sujets_rameau.45\",\"sujets_rameau.46\",\"sujets_rameau.47\",\"sujets_rameau.48\",\"sujets_rameau.49\",\"sujets_rameau.5\",\"sujets_rameau.50\",\"sujets_rameau.51\",\"sujets_rameau.52\",\"sujets_rameau.53\",\"sujets_rameau.54\",\"sujets_rameau.6\",\"sujets_rameau.7\",\"sujets_rameau.8\",\"sujets_rameau.9\",\"these_sur_travaux\",\"titres.autre.0\",\"titres.autre.1\",\"titres.autre.2\",\"titres.autre.3\",\"titres.en\",\"titres.fr\""));
        writer.setLineAggregator(new ThesisCsvLineAggregator());
        return writer;
    }

    /**
     * Writer écrivant les thèses au format NDJSON (une thèse par ligne au format
     * JSON).
     */
    @Bean
    @StepScope
    public FlatFileItemWriter<ExportThesis> ndjsonWriter(@Value("${app.output-dir}") String outputDir,
            @Value("#{jobParameters['exportDate']}") String exportDate,
            ObjectMapper objectMapper) {
        FlatFileItemWriter<ExportThesis> writer = new FlatFileItemWriter<>();
        writer.setName("ndjsonWriter");
        writer.setResource(new FileSystemResource(new File(outputDir, "theses-soutenues-" + exportDate + ".ndjson")));
        writer.setEncoding("UTF-8");
        writer.setLineAggregator(new NdjsonLineAggregator(objectMapper));
        return writer;
    }

    /**
     * Writer standard Spring Batch générant un tableau JSON valide.
     */
    @Bean
    @StepScope
    public JsonFileItemWriter<ExportThesis> jsonWriter(@Value("${app.output-dir}") String outputDir,
            @Value("#{jobParameters['exportDate']}") String exportDate) {
        return new JsonFileItemWriterBuilder<ExportThesis>()
                .name("jsonWriter")
                .resource(new FileSystemResource(new File(outputDir, "theses-soutenues-" + exportDate + ".json")))
                .jsonObjectMarshaller(new JacksonJsonObjectMarshaller<>())
                .build();
    }

    /**
     * Writer composite distribuant chaque thèse lue à chacun des 3 fichiers
     * d'export (CSV, JSON, NDJSON).
     */
    @Bean
    @StepScope
    public CompositeItemWriter<ExportThesis> compositeWriter(FlatFileItemWriter<ExportThesis> csvWriter,
            FlatFileItemWriter<ExportThesis> ndjsonWriter,
            JsonFileItemWriter<ExportThesis> jsonWriter) {
        CompositeItemWriter<ExportThesis> writer = new CompositeItemWriter<>();
        writer.setDelegates(List.of(csvWriter, ndjsonWriter, jsonWriter));
        return writer;
    }

    /**
     * Première étape du Job : Lecture de l'API et écriture simultanée dans les 3
     * formats.
     * En Spring Batch 5, le constructeur StepBuilder prend directement le
     * jobRepository et transactionManager.
     */
    @Bean
    public Step fetchAndWriteStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ThesesItemReader reader,
            ThesisProcessor thesisProcessor,
            CompositeItemWriter<ExportThesis> compositeWriter,
            FlatFileItemWriter<ExportThesis> csvWriter,
            FlatFileItemWriter<ExportThesis> ndjsonWriter,
            JsonFileItemWriter<ExportThesis> jsonWriter) {
        return new StepBuilder("fetchAndWriteStep", jobRepository)
                .<Thesis, ExportThesis>chunk(100, transactionManager)
                .reader(reader)
                .processor(thesisProcessor)
                .writer(compositeWriter)
                // Déclaration explicite des streams délégués pour que Spring Batch gère leur
                // ouverture/fermeture
                .stream(csvWriter)
                .stream(ndjsonWriter)
                .stream(jsonWriter)
                .build();
    }

    /**
     * Seconde étape du Job : Exécution de la Tasklet de téléversement vers
     * data.gouv.fr.
     */
    @Bean
    public Step uploadStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            UploadTasklet uploadTasklet) {
        return new StepBuilder("uploadStep", jobRepository)
                .tasklet(uploadTasklet, transactionManager)
                .build();
    }

    /**
     * Définition du Job principal enchaînant la récupération et l'envoi.
     */
    @Bean
    public Job thesesUpdateJob(JobRepository jobRepository, Step fetchAndWriteStep, Step uploadStep) {
        return new JobBuilder("thesesUpdateJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fetchAndWriteStep)
                .next(uploadStep)
                .build();
    }

    /**
     * Déclaration de la Tasklet d'upload en tant que Bean Spring.
     */
    @Bean
    @StepScope
    public UploadTasklet uploadTasklet(RestClient dataGouvRestClient,
            @Value("${app.datagouv.dataset-id}") String datasetId,
            @Value("${app.datagouv.csv-resource-id}") String csvResourceId,
            @Value("${app.datagouv.json-resource-id}") String jsonResourceId,
            @Value("${app.datagouv.ndjson-resource-id}") String ndjsonResourceId,
            @Value("${app.datagouv.api-key}") String apiKey,
            @Value("${app.output-dir}") String outputDir,
            @Value("${app.datagouv.upload-enabled:false}") boolean uploadEnabled,
            @Value("#{jobParameters['exportDate']}") String exportDate) {
        return new UploadTasklet(dataGouvRestClient, datasetId, csvResourceId,
                jsonResourceId, ndjsonResourceId, apiKey, outputDir, uploadEnabled, exportDate);
    }
}
