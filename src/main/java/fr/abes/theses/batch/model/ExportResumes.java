package fr.abes.theses.batch.model;

import java.util.List;

/**
 * DTO représentant les résumés dans le format d'export historique.
 */
public record ExportResumes(
    String fr,
    String en,
    List<String> autre
) {}
