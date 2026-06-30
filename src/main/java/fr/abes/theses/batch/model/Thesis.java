package fr.abes.theses.batch.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/**
 * Représente les métadonnées d'une thèse récupérées depuis l'API theses.fr.
 * Utilise des alias Jackson pour gérer les variations de noms de propriétés
 * existantes dans les réponses JSON de l'API.
 */
public record Thesis(
    String id,
    String nnt,
    String doi,
    String numSujet,
    List<Person> auteurs,
    String titrePrincipal,
    String titreEN,
    List<Person> directeurs,
    String discipline,
    String dateSoutenance,
    String datePremiereInscriptionDoctorat,
    String etabSoutenanceN,
    String etabSoutenancePpn,
    String codeEtab,
    List<Organization> etabCotutelle,
    
    @JsonAlias({"ecolesDoctorale", "ecolesDoctorales"})
    List<Organization> ecolesDoctorales,
    
    @JsonAlias({"partenairesDeRecherche", "partenairesRecherche"})
    List<Organization> partenairesDeRecherche,
    
    @JsonAlias({"president", "presidentJury"})
    Person president,
    
    List<Person> rapporteurs,
    
    @JsonAlias({"examinateurs", "membresJury"})
    List<Person> examinateurs,
    
    List<Subject> sujets,
    List<Subject> sujetsRameau,
    List<String> langues,
    String langue,
    String source,
    String status,
    String accessible
) {}
