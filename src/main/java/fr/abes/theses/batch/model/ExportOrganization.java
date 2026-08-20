package fr.abes.theses.batch.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO représentant une organisation dans le format d'export historique.
 */
public record ExportOrganization(
    String nom,
    String idref,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String type
) {}
