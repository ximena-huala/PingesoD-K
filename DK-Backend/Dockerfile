# syntax=docker/dockerfile:1

# ── Build: compila el jar ejecutable con Gradle ─────────────────────────────
FROM gradle:8.14.4-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon -x test

# ── Runtime: solo el JRE + el jar ───────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
