# ---------- Étape 1 : compilation et tests (dont la vérification des modules Spring Modulith)
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B package

# ---------- Étape 2 : image d'exécution (JRE seul, utilisateur non root)
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system ycyw && useradd --system --gid ycyw ycyw
COPY --from=build /app/target/ycyw-tchat.jar app.jar
USER ycyw
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
