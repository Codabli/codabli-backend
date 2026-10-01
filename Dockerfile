# Backend Codabli (Spring Boot 4 / Java 25) — image de production.
# Build multi-stage : compilation Maven puis exécution sur un JRE léger.

# --- Étape build ---
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
# Dépendances d'abord (cache de couche tant que le pom ne change pas).
# NB : l'image maven: fournit déjà Maven 3.9 ; on utilise `mvn` directement.
# (Ne pas appeler ./mvnw ici : l'image définit MAVEN_CONFIG=/root/.m2, que le
#  wrapper passe comme argument à Maven → "Unknown lifecycle phase /root/.m2".)
COPY pom.xml ./
RUN mvn -B dependency:go-offline
# Puis le code.
COPY src src
RUN mvn -B clean package -DskipTests

# --- Étape run ---
FROM eclipse-temurin:25-jre
WORKDIR /app
# Le jar Spring Boot repackagé (le *.jar.original est ignoré par le motif).
COPY --from=build /app/target/codabli-backend-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
