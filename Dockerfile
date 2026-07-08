# ===================================================================
# Étape 1 : Construction de l'application (Maven Build)
# ===================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copie du fichier pom.xml et téléchargement des dépendances hors-ligne pour mise en cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copie du code source et compilation du Jar exécutable (sans exécuter les tests)
COPY src ./src
RUN mvn clean package -DskipTests

# ===================================================================
# Étape 2 : Image d'exécution (Runtime JRE)
# ===================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Création d'un groupe et utilisateur système "spring" pour des raisons de sécurité (non-root execution)
RUN addgroup -S spring && adduser -S spring -G spring

# Copie du fichier jar généré dans l'étape précédente avec les bons droits d'accès
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Changement d'utilisateur pour exécuter en tant que "spring"
USER spring:spring

# Téléchargement d'une version fixe de l'agent OpenTelemetry pour la reproductibilité
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.3.0/opentelemetry-javaagent.jar /app/opentelemetry.jar


# Point d'entrée de l'application
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry.jar", "-jar", "app.jar"]
