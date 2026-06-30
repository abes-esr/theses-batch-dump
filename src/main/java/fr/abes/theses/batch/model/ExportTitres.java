package fr.abes.theses.batch.model;

import java.util.List;

/**
 * DTO représentant les titres dans le format d'export historique.
 */
public record ExportTitres(
    String fr,
    String en,
    List<String> autre
) {}
