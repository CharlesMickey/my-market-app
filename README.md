# Bom bom Market Application

Веб-приложение "Витрина интернет-магазина" на Spring Boot.

## Функциональность

- Просмотр товаров с пагинацией, поиском и сортировкой
- Добавление товаров в корзину
- Оформление заказов
- Просмотр истории заказов

## Технологии

- Java 21
- Spring Boot 3.5.9
- Spring Data JPA
- H2 Database / PostgreSQL
- Thymeleaf
- Gradle
- Docker

## Запуск приложения

### Локально с H2

```bash
## Run locally
./gradlew bootRun

## Run tests
./gradlew test

## Build JAR
./gradlew clean build

## Run with Docker
docker compose up -d

```
