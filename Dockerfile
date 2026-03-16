# Paso 1: Construir el jar con maven
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Paso 2: Crear la imagen final con el jar construido
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/microspringboot-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
