package fr.abes.theses.batch.processor;

import fr.abes.theses.batch.model.*;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Processeur chargé de nettoyer, harmoniser et transformer les données d'une thèse
 * du format API brute (Thesis) vers le format d'export historique (ExportThesis).
 */
public class ThesisProcessor implements ItemProcessor<Thesis, ExportThesis> {

    @Override
    public ExportThesis process(@NonNull Thesis t) {
        // 1. Valeurs par défaut simples et harmonisées
        String cleanSource = t.source() != null ? t.source() : "theses.fr";
        String cleanAccessible = t.accessible() != null ? t.accessible() : "non";
        String cleanStatus = "soutenue".equalsIgnoreCase(t.status()) ? "soutenue" : "en cours";

        // 2. Auteur unique (on prend le premier auteur s'il y en a)
        ExportPerson auteur = null;
        if (t.auteurs() != null && !t.auteurs().isEmpty()) {
            Person a = t.auteurs().get(0);
            auteur = new ExportPerson(a.nom(), a.prenom(), a.ppn());
        }

        // 3. Directeurs de thèse
        List<ExportPerson> directeurs = null;
        if (t.directeurs() != null) {
            directeurs = t.directeurs().stream()
                .map(d -> new ExportPerson(d.nom(), d.prenom(), d.ppn()))
                .toList();
        }

        // 4. Écoles doctorales
        List<ExportOrganization> ecoles = null;
        if (t.ecolesDoctorales() != null) {
            ecoles = t.ecolesDoctorales().stream()
                .map(e -> new ExportOrganization(e.nom(), e.ppn(), null))
                .toList();
        }

        // 5. Établissements de soutenance (création d'une liste avec l'établissement principal)
        List<ExportOrganization> etabs = null;
        if (t.etabSoutenanceN() != null || t.etabSoutenancePpn() != null) {
            etabs = List.of(new ExportOrganization(t.etabSoutenanceN(), t.etabSoutenancePpn(), null));
        }

        // 6. Langues (on enveloppe dans une liste)
        List<String> langues = t.langues();
        if ((langues == null || langues.isEmpty()) && t.langue() != null) {
            langues = List.of(t.langue());
        }

        // 7. Membres du jury (examinateurs)
        List<ExportPerson> membres = null;
        if (t.examinateurs() != null) {
            membres = t.examinateurs().stream()
                .map(m -> new ExportPerson(m.nom(), m.prenom(), m.ppn()))
                .toList();
        }

        // 8. Partenaires de recherche
        List<ExportOrganization> partenaires = null;
        if (t.partenairesDeRecherche() != null) {
            partenaires = t.partenairesDeRecherche().stream()
                .map(p -> new ExportOrganization(p.nom(), p.ppn(), null))
                .toList();
        }

        // 9. Président du jury
        ExportPerson president = null;
        if (t.president() != null && t.president().nom() != null && !t.president().nom().trim().isEmpty()) {
            president = new ExportPerson(t.president().nom(), t.president().prenom(), t.president().ppn());
        }

        // 10. Rapporteurs
        List<ExportPerson> rapporteurs = null;
        if (t.rapporteurs() != null) {
            rapporteurs = t.rapporteurs().stream()
                .map(r -> new ExportPerson(r.nom(), r.prenom(), r.ppn()))
                .toList();
        }

        // 11. Sujets dispatchés par langue (fr/en/autre)
        String sujetFr = null;
        String sujetEn = null;
        List<String> sujetAutre = new ArrayList<>();
        if (t.sujets() != null) {
            for (Subject s : t.sujets()) {
                if (s.libelle() != null) {
                    if (s.langue() == null || "fr".equalsIgnoreCase(s.langue())) {
                        sujetFr = cleanText(s.libelle());
                    } else if ("en".equalsIgnoreCase(s.langue())) {
                        sujetEn = cleanText(s.libelle());
                    } else {
                        sujetAutre.add(cleanText(s.libelle()));
                    }
                }
            }
        }
        ExportSujets sujets = (sujetFr != null || sujetEn != null || !sujetAutre.isEmpty())
            ? new ExportSujets(sujetFr, sujetEn, sujetAutre.isEmpty() ? null : sujetAutre)
            : null;

        // 12. Sujets Rameau (uniquement les libellés sous forme de liste de chaînes)
        List<String> rameaux = null;
        if (t.sujetsRameau() != null) {
            rameaux = t.sujetsRameau().stream()
                .map(Subject::libelle)
                .map(this::cleanText)
                .toList();
        }

        // 13. Titres (fr/en)
        ExportTitres titres = new ExportTitres(cleanText(t.titrePrincipal()), cleanText(t.titreEN()), null);

        // 14. OAI Set Specs (par exemple ddc:540 si chimie, sinon déduction ou vide)
        List<String> oaiSpecs = null;
        if (t.discipline() != null && "chimie".equalsIgnoreCase(t.discipline().trim())) {
            oaiSpecs = List.of("ddc:540");
        }

        // Reformatage de la date de soutenance (dd/MM/yyyy -> yyyy-MM-dd)
        String dateSoutenanceClean = reformatDate(t.dateSoutenance());

        // Retourne le DTO d'export historique
        return new ExportThesis(
            cleanAccessible,
            auteur,
            null, // cas
            t.codeEtab(),
            dateSoutenanceClean,
            directeurs,
            t.discipline(),
            ecoles,
            null, // embargo
            etabs,
            langues,
            membres,
            t.nnt() != null ? t.nnt() : t.id(),
            oaiSpecs,
            partenaires,
            president,
            rapporteurs,
            null, // resumes
            cleanSource,
            cleanStatus,
            sujets,
            rameaux,
            "non", // these_sur_travaux
            titres
        );
    }

    private String reformatDate(String dateStr) {
        if (dateStr == null) {
            return null;
        }
        try {
            if (dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
                String[] parts = dateStr.split("/");
                return parts[2] + "-" + parts[1] + "-" + parts[0];
            }
        } catch (Exception e) {
            // Ignorer et retourner la valeur brute
        }
        return dateStr;
    }

    private String cleanText(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("\\\\+", "").trim();
    }
}
