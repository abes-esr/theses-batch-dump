# 🛠️ Documentation développeur - theses-batch-dump

Ce projet est un traitement par lots (Batch) développé en **Java 21** à l'aide du framework **Spring Boot** (v3.3.1) et de **Spring Batch** (v5.1).

---

## 🏗️ Architecture du Batch

L'application est structurée autour d'un unique Job Spring Batch `thesesUpdateJob` composé de deux étapes principales :

1. **`fetchAndWriteStep`** :
   - **Reader** (`ThesesItemReader`) : Interroge l'API REST de theses.fr de façon paginée.
   - **Processor** (`ThesisProcessor`) : Nettoie, filtre et structure les données récupérées.
   - **Writer** (`CompositeItemWriter` composé de 3 sous-writers) : Génère simultanément les fichiers d'exports dans trois formats (CSV avec BOM UTF-8, JSON, et NDJSON).
2. **`uploadStep`** :
   - **Tasklet** (`UploadTasklet`) : Téléverse les fichiers générés vers data.gouv.fr à l'aide de l'API de data.gouv.fr (si le mode associé est activé).

---

## 🚦 Modes d'Exécution & Variables de Configuration

Le comportement de l'application est piloté par des variables d'environnement, configurables dans un fichier `.env` ou transmises lors du lancement de la JVM.

### 1. Le Mode Test (`MODE_TEST`)

Ce mode permet de tester l'intégration de bout en bout et les formats d'écriture sans avoir à charger l'intégralité du catalogue des thèses.

- **Variable** : `MODE_TEST` (dans `.env`) ou `-Dapp.mode-test` (propriété système)
- **Impacts** :
  - **Limitation des données** : Le Batch s'arrête après avoir récupéré un maximum de **50 thèses** depuis l'API theses.fr.
  - **Contournement de la planification** : Il ignore la règle d'exécution des 6 mois. Le job s'exécute systématiquement au démarrage.

### 2. Le Mode data.gouv.fr (`DATA_GOUV_UPLOAD_ENABLED`)

Permet de contrôler si les fichiers générés doivent être poussés sur la plateforme nationale data.gouv.fr.

- **Variable** : `DATA_GOUV_UPLOAD_ENABLED` (dans `.env`) ou `-Dapp.datagouv.upload-enabled`
- **Impacts** :
  - **Si `true`** : L'étape `uploadStep` est exécutée et envoie les fichiers d'export générés vers les ressources configurées.
  - **Si `false` (par défaut)** : Le téléversement est désactivé. Les fichiers sont uniquement générés localement dans le dossier `OUTPUT_DIR`.
- **Variables dépendantes obligatoires si activé** :
  - `DATA_GOUV_API_KEY` : Clé d'API data.gouv.fr
  - `DATA_GOUV_DATASET_ID` : ID du jeu de données cible
  - `DATA_GOUV_CSV_RESOURCE_ID` : ID de la ressource pour le fichier CSV
  - `DATA_GOUV_JSON_RESOURCE_ID` : ID de la ressource pour le fichier JSON
  - `DATA_GOUV_NDJSON_RESOURCE_ID` : ID de la ressource pour le fichier NDJSON

### 3. Règle de planification des 6 mois & Forçage (`FORCE_RUN`)

Hors mode test, l'application applique une règle métier visant à ne lancer l'export que si le dernier export réussi (`COMPLETED`) remonte à plus de 6 mois.

- Pour forcer l'exécution manuellement en mode normal, vous pouvez positionner la variable `FORCE_RUN=true` (ou `-Dapp.force-run=true`).

---

## 💻 Développement et Lancement Local

### Prérequis

- Java 21 installé
- Maven 3.9+ installé

### Configuration locale

1. Copiez le fichier `.env-dist` en `.env` (si non présent) et ajustez les variables.
2. Modifiez le fichier de propriétés spécifique au développement local : application-localhost.properties

### Commandes Maven

- **Lancer l'application en mode Spring Boot** (charge le profil `localhost` par défaut) :

  ```bash
  mvn spring-boot:run
  ```

- **Lancer en spécifiant des arguments système (ex: activer le mode test)** :

  ```bash
  mvn spring-boot:run -Dspring-boot.run.arguments="--app.mode-test=true"
  ```

- **Compiler et packager un JAR exécutable** (sans exécuter les tests unitaires) :

  ```bash
  mvn clean package -DskipTests
  ```

- **Lancer le JAR produit** :
  ```bash
  java -jar target/theses-batch-dump-1.0.0.jar
  ```
