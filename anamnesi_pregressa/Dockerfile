FROM eclipse-temurin:23-jre
WORKDIR /app

COPY anamnesi_pregressa/build/libs/anamnesi_pregressa.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]