FROM eclipse-temurin:23-jre
WORKDIR /app

COPY terapia/build/libs/terapia.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]