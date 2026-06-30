package fr.abes.theses.batch.model;

/**
 * DTO représentant une organisation dans le format d'export historique.
 */
public record ExportOrganization(
    String nom,
    String idref,
    String type
) {}
