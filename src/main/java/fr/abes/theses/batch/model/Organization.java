package fr.abes.theses.batch.model;

/**
 * Représente un établissement ou une organisation (cotutelle, école doctorale, partenaire de recherche) dans l'API theses.fr.
 * 
 * @param nom Nom de l'établissement ou de l'organisation.
 * @param ppn Identifiant PPN (IdRef) de l'établissement ou de l'organisation.
 */
public record Organization(
    String nom,
    String ppn
) {}
