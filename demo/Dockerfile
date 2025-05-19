# Usa Maven per compilare
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /tmp/uploads
EXPOSE 8080
CMD ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
