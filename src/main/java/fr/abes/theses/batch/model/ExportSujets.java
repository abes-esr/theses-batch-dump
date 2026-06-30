package fr.abes.theses.batch.model;

import java.util.List;

/**
 * DTO représentant les sujets dans le format d'export historique.
 */
public record ExportSujets(
    String fr,
    String en,
    List<String> autre
) {}
