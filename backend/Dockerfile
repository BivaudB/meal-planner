# Image du backend Spring Boot (multi-architecture : amd64 et arm64, ex. VM Oracle Ampere).
# Construction : docker compose -f docker-compose.prod.yml build backend

# --- Compilation ---------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
# Dépendances d'abord : mises en cache tant que pom.xml ne change pas
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests \
    && mkdir app \
    && cd app \
    && java -Djarmode=tools -jar ../target/*.jar extract --layers --launcher --destination .

# --- Exécution -----------------------------------------------------------
FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --home /app app
WORKDIR /app
# Couches Spring Boot, de la moins à la plus souvent modifiée
COPY --from=build /build/app/dependencies/ ./
COPY --from=build /build/app/spring-boot-loader/ ./
COPY --from=build /build/app/snapshot-dependencies/ ./
COPY --from=build /build/app/application/ ./
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
