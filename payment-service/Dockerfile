FROM gradle:8.5-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY payment-service/build.gradle payment-service/
COPY openapi ./openapi
COPY payment-service/src ./payment-service/src

RUN gradle :payment-service:bootJar --no-daemon

FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

COPY --from=build /app/payment-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]