FROM openjdk:21-jdk-slim

LABEL maintainer="SecureTransfer <support@securetransfer.com>"
LABEL description="SecureTransfer Signaling Server"
LABEL version="1.0.0"

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Build the application
RUN mvn clean package -DskipTests

# Create non-root user
RUN groupadd -r securetransfer && useradd -r -g securetransfer securetransfer

# Copy the built jar
RUN cp target/*.jar app.jar

# Change ownership
RUN chown -R securetransfer:securetransfer /app

# Switch to non-root user
USER securetransfer

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
