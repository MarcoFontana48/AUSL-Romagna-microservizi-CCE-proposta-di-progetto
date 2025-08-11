FROM eclipse-temurin:21-jre
WORKDIR /app

COPY api_gateway/build/libs/api_gateway.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]