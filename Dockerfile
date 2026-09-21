# ------------------------------
# Stage 1: Build the application
# ------------------------------
FROM amazoncorretto:21-alpine3.24-jdk AS builder
WORKDIR /app

# Copy Maven wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure wrapper script has execute permissions
RUN chmod +x mvnw

# Download dependencies (this layer is cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy application source code and package the JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ------------------------------
# Stage 2: Runtime Image
# ------------------------------
# FROM amazoncorretto:21-alpine
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Expose port
EXPOSE 8080

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Define default Java options
ENV JAVA_OPTS=""

# Use shell form to allow JAVA_OPTS variable expansion at runtime
CMD sh -c "java $JAVA_OPTS -jar app.jar --spring.profiles.active=dev"