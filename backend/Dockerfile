# Étape 1 : Build Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copier le pom.xml seul d'abord 
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier les sources et builder
COPY src src
RUN mvn -B package -DskipTests

# Étape 2 : Runtime JRE
FROM eclipse-temurin:21-jre

# Installer wget pour le healthcheck Docker
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Créer le répertoire des logs avec les bons droits
RUN mkdir -p /app/logs/archived && chmod -R 755 /app/logs

EXPOSE 8080

# JVM tuning pour container
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
