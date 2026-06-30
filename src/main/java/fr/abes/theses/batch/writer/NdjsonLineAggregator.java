package fr.abes.theses.batch.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.theses.batch.model.ExportThesis;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.lang.NonNull;

/**
 * Agrégateur de ligne NDJSON (Newline Delimited JSON) transformant un objet ExportThesis 
 * en sa représentation JSON sur une seule ligne.
 */
public class NdjsonLineAggregator implements LineAggregator<ExportThesis> {

    private final ObjectMapper objectMapper;

    /**
     * Constructeur injectant le mapper JSON Jackson.
     *
     * @param objectMapper L'instance ObjectMapper de Spring.
     */
    public NdjsonLineAggregator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Sérialise la thèse en chaîne de caractères JSON.
     *
     * @param item L'objet ExportThesis.
     * @return La chaîne JSON correspondante.
     * @throws IllegalStateException si la sérialisation échoue.
     */
    @Override
    @NonNull
    public String aggregate(@NonNull ExportThesis item) {
        try {
            String json = objectMapper.writeValueAsString(item);
            if (json == null) {
                throw new IllegalStateException("La sérialisation JSON de la thèse a retourné une valeur nulle.");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erreur de sérialisation JSON de la thèse", e);
        }
    }
}
