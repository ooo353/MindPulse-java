# Use OpenJDK 21 slim image as base
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the JAR file from target directory
COPY target/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]