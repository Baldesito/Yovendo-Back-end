# Build stage - Updated to Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Set encoding for Maven build to UTF-8
ENV MAVEN_OPTS="-Dfile.encoding=UTF-8"

# Copy pom.xml first for better caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application with explicit UTF-8 encoding
RUN mvn clean package -DskipTests -B -Dfile.encoding=UTF-8 -Dproject.build.sourceEncoding=UTF-8

# Runtime stage - Updated to Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create upload directory
RUN mkdir -p /tmp/uploads

# Set environment variables for runtime
ENV JAVA_OPTS="-Dfile.encoding=UTF-8"
ENV SPRING_PROFILES_ACTIVE=prod

# Expose port
EXPOSE 8080

# Run the application with UTF-8 encoding
CMD ["java", "-Dfile.encoding=UTF-8", "-Dspring.profiles.active=prod", "-jar", "app.jar"]