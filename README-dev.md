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

## 📂 Rôle détaillé des Fichiers

L'application est structurée en packages clairs sous `fr.abes.theses.batch` :

### 🚀 Démarrage et Planification (`/scheduler`)

- **[ThesesBatchApplication.java](src/main/java/fr/abes/theses/batch/ThesesBatchApplication.java)** : Classe de démarrage Spring Boot classique.
- **[StartupJobRunner.java](src/main/java/fr/abes/theses/batch/scheduler/StartupJobRunner.java)** : Exécuté au démarrage si la planification est désactivée (`app.scheduler.enabled=false`). Il évalue les conditions, exécute le Batch si requis, puis arrête proprement la JVM.
- **[ScheduledJobRunner.java](src/main/java/fr/abes/theses/batch/scheduler/ScheduledJobRunner.java)** : S'active si le planificateur est activé (`app.scheduler.enabled=true`). Il laisse l'application s'exécuter en continu et planifie des exécutions régulières via une expression Cron.
- **[JobLauncherService.java](src/main/java/fr/abes/theses/batch/scheduler/JobLauncherService.java)** : Orchestrateur intermédiaire. Il génère les paramètres du job (comme la date d'export unique) et déclenche l'exécution du job.
- **[JobRunDecider.java](src/main/java/fr/abes/theses/batch/scheduler/JobRunDecider.java)** : Évalue si le job doit tourner (gestion du mode test, du forçage `FORCE_RUN` et de la règle de temps de 6 mois).

### ⚙️ Configuration globale (`/config`)

- **[BatchConfig.java](src/main/java/fr/abes/theses/batch/config/BatchConfig.java)** : Configuration générale du Job Spring Batch. Déclare le Job, ses étapes (`Step`), et l'ensemble des beans requis (`RestClient`, `Reader`, `Processor`, `Writers`, `Tasklet`).

### 📥 Lecture de données (`/reader`)

- **[ThesesItemReader.java](src/main/java/fr/abes/theses/batch/reader/ThesesItemReader.java)** : Reader paginé interrogeant l'API de theses.fr en effectuant des appels REST successifs via le `RestClient` de Spring 3.

### 🧹 Transformation des données (`/processor`)

- **[ThesisProcessor.java](src/main/java/fr/abes/theses/batch/processor/ThesisProcessor.java)** : Reçoit les objets bruts de l'API et les convertit en DTO d'export après nettoyage (reformatage de dates, gestion des listes de personnes ou d'organisations).

### 💾 Écriture et Structuration des fichiers (`/writer`)

- **[ThesisCsvLineAggregator.java](src/main/java/fr/abes/theses/batch/writer/ThesisCsvLineAggregator.java)** : Formate l'objet d'export en une ligne CSV plate en ordonnant les 243 colonnes requises.
- **[NdjsonLineAggregator.java](src/main/java/fr/abes/theses/batch/writer/NdjsonLineAggregator.java)** : Sérialise l'objet d'export en une ligne au format JSON standard.

### 📤 Téléversement (`/tasklet`)

- **[UploadTasklet.java](src/main/java/fr/abes/theses/batch/tasklet/UploadTasklet.java)** : Récupère les fichiers générés localement et les envoie par requêtes HTTP POST multipart à data.gouv.fr.

### 📦 Modèles (`/model`)

- Contient les structures de données brutes reçues de l'API (ex: `Thesis`, `Person`) et les structures cibles formatées (ex: `ExportThesis`, `ExportPerson`).

---

### 📡 Canaux de transmission des informations

- **Propriétés globales :** Via l'injection de valeurs par `@Value` de Spring, alimentée par le fichier `.env` ou `application.properties`.
- **Persistance historique :** Base de données H2 intégrée persistée localement dans `/data/h2db`. Elle permet à `JobRunDecider` (via `JobExplorer`) d'analyser l'historique des exécutions.
- **Paramètres de Job (JobParameters) :** `JobLauncherService` calcule et injecte la date du jour `exportDate` lors du lancement. Grâce au `@StepScope` Spring Batch, cette date est lue par les writers (pour nommer les fichiers d'exports) et par le `UploadTasklet` (pour retrouver les chemins des fichiers locaux à téléverser).
- **Flot orienté Chunk :** Le `ThesesItemReader` produit des objets `Thesis` (format API brute) qui transitent par le `ThesisProcessor` pour donner des objets `ExportThesis` (format DTO d'export), finalement sérialisés par les Writers sur le disque local.
- **Fichiers physiques :** Le disque local sert de point de passage entre la génération (`fetchAndWriteStep`) et le téléversement (`UploadTasklet`).

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
