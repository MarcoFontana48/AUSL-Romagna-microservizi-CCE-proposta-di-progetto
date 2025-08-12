FROM eclipse-temurin:23-jre
WORKDIR /app

COPY service/build/libs/service.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]