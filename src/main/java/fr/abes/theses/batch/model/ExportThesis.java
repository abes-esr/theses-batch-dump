package fr.abes.theses.batch.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO principal représentant le schéma de thèse complet à exporter en JSON,
 * NDJSON et CSV.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportThesis(
        String accessible,
        ExportPerson auteur,
        String cas,
        String code_etab,
        String date_soutenance,
        List<ExportPerson> directeurs_these,
        String discipline,
        List<ExportOrganization> ecoles_doctorales,
        String embargo,
        List<ExportOrganization> etablissements_soutenance,
        List<String> langues,
        List<ExportPerson> membres_jury,
        String nnt,
        List<String> oai_set_specs,
        List<ExportOrganization> partenaires_recherche,
        ExportPerson president_jury,
        List<ExportPerson> rapporteurs,
        ExportResumes resumes,
        String source,
        String status,
        ExportSujets sujets,
        List<String> sujets_rameau,
        String these_sur_travaux,
        ExportTitres titres,
        String date_cines,
        String num_sujet_sans_s) {
}
