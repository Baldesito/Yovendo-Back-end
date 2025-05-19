# Usa una immagine con Maven per il build
FROM maven:3.9.4-openjdk-17 AS build

# Copia i file di configurazione Maven
COPY pom.xml .
COPY src ./src

# Compila l'applicazione
RUN mvn clean package -DskipTests

# Usa una immagine più leggera per l'esecuzione
FROM openjdk:17-jdk-slim

# Crea directory dell'app
WORKDIR /app

# Copia il JAR dall'immagine di build
COPY --from=build target/*.jar app.jar

# Crea directory per uploads
RUN mkdir -p /tmp/uploads

# Espone la porta
EXPOSE 8080

# Comando per avviare l'applicazione
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]