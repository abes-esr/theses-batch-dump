package fr.abes.theses.batch.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * DTO représentant les sujets dans le format d'export historique.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportSujets(
    List<String> fr,
    List<String> en,
    List<String> autre
) {}
