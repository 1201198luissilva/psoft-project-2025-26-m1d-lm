# Use the official Eclipse Temurin 21 JDK image
FROM eclipse-temurin:21-jdk-jammy

# Set a working directory inside the container
WORKDIR /app

# Copy the JAR file into the container
COPY target/LMSAuthors-0.0.1-SNAPSHOT.jar /app/app.jar

# Informative only; it has no influence in the port effectively exposed by the container.
#EXPOSE 8080

# Set the command to run the JAR file
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
