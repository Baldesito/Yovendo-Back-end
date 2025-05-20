# Build stage - Updated to Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Set encoding for Maven build
ENV MAVEN_OPTS="-Dfile.encoding=UTF-8"

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

# Build con UTF-8
RUN mvn clean package -DskipTests -B -Dfile.encoding=UTF-8 -Dproject.build.sourceEncoding=UTF-8

# Runtime stage - Updated to Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia il JAR
COPY --from=builder /app/target/*.jar app.jar

# Imposta solo variabili non sensibili
ENV SPRING_PROFILES_ACTIVE=prod
ENV SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
ENV SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect

# Crea la directory uploads
RUN mkdir -p /app/uploads

# Esponi la porta
EXPOSE 8080

# Avvia l'applicazione con debug
CMD ["java", "-Dfile.encoding=UTF-8", "-Dspring.profiles.active=prod", "-jar", "app.jar"]