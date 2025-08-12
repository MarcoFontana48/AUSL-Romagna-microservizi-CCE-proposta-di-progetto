FROM eclipse-temurin:23-jre
WORKDIR /app

# Copy pre-built JAR (built by CI/CD pipeline)
COPY api_gateway/build/libs/api_gateway.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]