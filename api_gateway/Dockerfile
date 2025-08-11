# Multi-stage build for CI/CD
FROM eclipse-temurin:23-jdk AS build
WORKDIR /build

# Copy the entire project context
COPY . .

# Make gradlew executable (if it exists at root level)
RUN if [ -f ./gradlew ]; then chmod +x ./gradlew; fi

# Install gradle if no gradlew is found
RUN if [ ! -f ./gradlew ]; then \
        apt-get update && \
        apt-get install -y wget unzip && \
        wget https://services.gradle.org/distributions/gradle-8.5-bin.zip && \
        unzip gradle-8.5-bin.zip && \
        mv gradle-8.5 /opt/gradle && \
        ln -s /opt/gradle/bin/gradle /usr/local/bin/gradle && \
        rm gradle-8.5-bin.zip; \
    fi

# Build the application
RUN if [ -f ./gradlew ]; then \
        ./gradlew :api_gateway:shadowJar --no-daemon; \
    else \
        gradle :api_gateway:shadowJar --no-daemon; \
    fi

# Runtime stage
FROM eclipse-temurin:23-jre AS runtime
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /build/api_gateway/build/libs/api_gateway.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]