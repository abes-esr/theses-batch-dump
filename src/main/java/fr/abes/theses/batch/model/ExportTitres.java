package fr.abes.theses.batch.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * DTO représentant les titres dans le format d'export historique.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportTitres(
    String fr,
    String en,
    List<String> autre
) {}
