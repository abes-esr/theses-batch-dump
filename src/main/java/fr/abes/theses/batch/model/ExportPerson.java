package fr.abes.theses.batch.model;

/**
 * DTO représentant une personne dans le format d'export historique.
 */
public record ExportPerson(
    String nom,
    String prenom,
    String idref
) {}
