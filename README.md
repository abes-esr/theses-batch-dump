# theses-batch-dump

[![Docker Pulls](https://img.shields.io/docker/pulls/abesesr/theses-batch-dump.svg)](https://hub.docker.com/r/abesesr/theses-batch-dump/)

Vous êtes sur le README usager. Si vous souhaitez accéder au README de développement, veuillez suivre ce lien : [README-dev](README-dev.md)

Ce dépôt héberge le code source de l'application **theses-batch-dump**.
C'est un traitement planifié (Batch) qui extrait, transforme et exporte les thèses soutenues en France.

**theses-batch-dump** permet de :

1. Récupérer de façon paginée les données des thèses de doctorat soutenues auprès de l'API de [theses.fr](https://theses.fr).
2. Traiter et restructurer ces données au format attendu pour l'ouverture des données publiques.
3. Générer localement des fichiers d'exports complets sous trois formats différents :
   - Un fichier **CSV** (avec BOM UTF-8 pour un import Excel direct sans problème d'accents).
   - Un fichier **JSON** (tableau structuré).
   - Un fichier **NDJSON** (format JSON délimité par des retours à la ligne, optimisé pour le traitement de flux).
4. Téléverser automatiquement ces fichiers vers les ressources associées de la plateforme nationale [data.gouv.fr](https://data.gouv.fr) (si configuré et activé).

---

## 🚀 Lancement avec Docker

L'application peut être packagée et démarrée dans un conteneur Docker.

### 1. Préparation de la configuration

Copiez le fichier de distribution des variables d'environnement `.env-dist` pour créer votre fichier `.env` de production/test :

```bash
cp .env-dist .env
# Modifiez ensuite les valeurs dans le fichier .env (clés API data.gouv.fr, répertoire de sortie, modes, etc.)
```

### 2. Démarrage et exécution

Pour exécuter le conteneur en utilisant vos variables d'environnement configurées :

```bash
# Construction de l'image
docker build -t abesesr/theses-batch-dump .

# Lancement de l'image Docker (lie le répertoire local "./data" pour récupérer les fichiers d'exports)
docker run --env-file .env -v %cd%/data:/app/data abesesr/theses-batch-dump
```

_(Sur Linux/macOS, remplacez `%cd%` par `$(pwd)`)_

Le conteneur va exécuter le Batch et s'arrêter après la fin de la génération des exports (et du téléversement si ce dernier est activé).
Si la planification en arrière-plan est activée (`SCHEDULER_ENABLED=true`), le cont�cuter les tests unitaires) :

```bash
mvn clean package -DskipTests
```

- **Lancer le JAR produit** :
  ```bash
  java -jar target/theses-batch-dump-1.0.0.jar
  ```
