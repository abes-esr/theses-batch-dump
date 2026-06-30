package fr.abes.theses.batch.model;

/**
 * Représente un sujet ou mot-clé associé à une thèse dans l'API theses.fr.
 * 
 * @param langue Langue du libellé (ex: "fr", "en"). Peut être null.
 * @param libelle Valeur textuelle du mot-clé.
 */
public record Subject(
    String langue,
    String libelle
) {}
