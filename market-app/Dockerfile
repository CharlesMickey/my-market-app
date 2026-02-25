FROM gradle:8.5-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY market-app/build.gradle market-app/
COPY openapi ./openapi
COPY market-app/src ./market-app/src


RUN gradle :market-app:bootJar --no-daemon

FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

COPY --from=build /app/market-app/build/libs/*.jar app.jar
COPY market-app/src/main/resources/application*.yml ./
COPY market-app/src/main/resources/templates ./templates
COPY market-app/src/main/resources/static ./static

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]