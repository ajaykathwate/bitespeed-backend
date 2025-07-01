# Stage 1: Build the Spring Boot app using Maven and Java 21
FROM eclipse-temurin:21 AS build
WORKDIR /app

# Copy necessary files
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src src

# Make the Maven wrapper executable
RUN chmod +x ./mvnw

# Build the project without running tests
RUN ./mvnw clean package -DskipTests

# Stage 2: Create final image
FROM eclipse-temurin:21
WORKDIR /app

# Copy the built JAR file
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
