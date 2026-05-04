FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties ./
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies || true

COPY src src

RUN ./gradlew --no-daemon bootJar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "build/libs/be-management-kebun-0.0.1-SNAPSHOT.jar"]
