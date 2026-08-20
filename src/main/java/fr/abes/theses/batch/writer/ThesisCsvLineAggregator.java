package fr.abes.theses.batch.writer;

import fr.abes.theses.batch.model.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.lang.NonNull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Agrégateur de ligne CSV générant les colonnes plates dans l'ordre exact de old.csv.
 */
public class ThesisCsvLineAggregator implements LineAggregator<ExportThesis> {

    // Délimiteur virgule comme dans old.csv
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(',')
            .build();

    @Override
    @NonNull
    @SuppressWarnings("null")
    public String aggregate(@NonNull ExportThesis t) {
        List<String> row = new ArrayList<>();

        // 1. accessible
        row.add(t.accessible() != null ? t.accessible() : "");

        // 2. auteur
        if (t.auteur() != null) {
            row.add(t.auteur().idref() != null ? t.auteur().idref() : "");
            row.add(t.auteur().nom() != null ? t.auteur().nom() : "");
            row.add(t.auteur().prenom() != null ? t.auteur().prenom() : "");
        } else {
            row.add(""); row.add(""); row.add("");
        }

        // 3. cas
        row.add(t.cas() != null ? t.cas() : "");

        // 4. code_etab
        row.add(t.code_etab() != null ? t.code_etab() : "");

        // 5. date_soutenance
        row.add(t.date_soutenance() != null ? t.date_soutenance() : "");

        // 6. directeurs_these (0 à 6)
        for (int i = 0; i <= 6; i++) {
            addPerson(row, t.directeurs_these(), i);
        }

        // 7. discipline
        row.add(t.discipline() != null ? t.discipline() : "");

        // 8. ecoles_doctorales (0 à 1)
        for (int i = 0; i <= 1; i++) {
            addOrg(row, t.ecoles_doctorales(), i, false);
        }

        // 9. embargo
        row.add(t.embargo() != null ? t.embargo() : "");

        // 10. etablissements_soutenance (0 à 4)
        for (int i = 0; i <= 4; i++) {
            addOrg(row, t.etablissements_soutenance(), i, false);
        }

        // 11. langues (0 à 3)
        for (int i = 0; i <= 3; i++) {
            addString(row, t.langues(), i);
        }

        // 12. membres_jury (lexicographique : 0, 10, 11, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        int[] membresIndices = {0, 10, 11, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int idx : membresIndices) {
            addPerson(row, t.membres_jury(), idx);
        }

        // 13. nnt
        row.add(t.nnt() != null ? t.nnt() : "");

        // 14. oai_set_specs
        row.add(t.oai_set_specs() != null && !t.oai_set_specs().isEmpty() ? t.oai_set_specs().get(0) : "");

        // 15. partenaires_recherche (0 à 7, avec type)
        for (int i = 0; i <= 7; i++) {
            addOrg(row, t.partenaires_recherche(), i, true);
        }

        // 16. president_jury
        if (t.president_jury() != null) {
            row.add(t.president_jury().idref() != null ? t.president_jury().idref() : "");
            row.add(t.president_jury().nom() != null ? t.president_jury().nom() : "");
            row.add(t.president_jury().prenom() != null ? t.president_jury().prenom() : "");
        } else {
            row.add(""); row.add(""); row.add("");
        }

        // 17. rapporteurs (0 à 5)
        for (int i = 0; i <= 5; i++) {
            addPerson(row, t.rapporteurs(), i);
        }

        // 18. resumes
        List<String> resumesAutre = t.resumes() != null ? t.resumes().autre() : null;
        for (int i = 0; i <= 5; i++) {
            addString(row, resumesAutre, i);
        }
        row.add(t.resumes() != null && t.resumes().en() != null ? t.resumes().en() : "");
        row.add(t.resumes() != null && t.resumes().fr() != null ? t.resumes().fr() : "");

        // 19. source
        row.add(t.source() != null ? t.source() : "");

        // 20. status
        row.add(t.status() != null ? t.status() : "");

        // 21. sujets
        List<String> sujetsAutre = t.sujets() != null ? t.sujets().autre() : null;
        for (int i = 0; i <= 7; i++) {
            addString(row, sujetsAutre, i);
        }
        row.add(t.sujets() != null && t.sujets().en() != null ? String.join("||", t.sujets().en()) : "");
        row.add(t.sujets() != null && t.sujets().fr() != null ? String.join("||", t.sujets().fr()) : "");

        // 22. sujets_rameau (lexicographique : 0, 1, 10..19, 2, 20..29, 3, 30..39, 4, 40..49, 5, 50..54, 6, 7, 8, 9)
        int[] rameauIndices = {
            0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            2, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            3, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            4, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            5, 50, 51, 52, 53, 54,
            6, 7, 8, 9
        };
        for (int idx : rameauIndices) {
            addString(row, t.sujets_rameau(), idx);
        }

        // 23. these_sur_travaux
        row.add(t.these_sur_travaux() != null ? t.these_sur_travaux() : "");

        // 24. titres
        List<String> titresAutre = t.titres() != null ? t.titres().autre() : null;
        for (int i = 0; i <= 3; i++) {
            addString(row, titresAutre, i);
        }
        row.add(t.titres() != null && t.titres().en() != null ? t.titres().en() : "");
        row.add(t.titres() != null && t.titres().fr() != null ? t.titres().fr() : "");

        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSV_FORMAT)) {
            printer.printRecord(row);
            return sw.toString().trim();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la sérialisation CSV de la thèse", e);
        }
    }

    private void addPerson(List<String> row, List<ExportPerson> list, int index) {
        if (list != null && index < list.size() && list.get(index) != null) {
            ExportPerson p = list.get(index);
            row.add(p.idref() != null ? p.idref() : "");
            row.add(p.nom() != null ? p.nom() : "");
            row.add(p.prenom() != null ? p.prenom() : "");
        } else {
            row.add(""); row.add(""); row.add("");
        }
    }

    private void addOrg(List<String> row, List<ExportOrganization> list, int index, boolean includeType) {
        if (list != null && index < list.size() && list.get(index) != null) {
            ExportOrganization o = list.get(index);
            row.add(o.idref() != null ? o.idref() : "");
            row.add(o.nom() != null ? o.nom() : "");
            if (includeType) {
                row.add(o.type() != null ? o.type() : "");
            }
        } else {
            row.add(""); row.add("");
            if (includeType) {
                row.add("");
            }
        }
    }

    private void addString(List<String> row, List<String> list, int index) {
        if (list != null && index < list.size() && list.get(index) != null) {
            row.add(list.get(index));
        } else {
            row.add("");
        }
    }
}
