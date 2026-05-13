FROM gradle:8.14.3-jdk21-alpine AS build

WORKDIR /app

COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src

RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S tactical && adduser -S tactical -G tactical

COPY --from=build /app/build/libs/*.jar app.jar

USER tactical

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
