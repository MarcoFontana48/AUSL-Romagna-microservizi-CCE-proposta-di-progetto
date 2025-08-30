FROM eclipse-temurin:23-jre
WORKDIR /app

COPY diario_clinico/build/libs/diario_clinico.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]