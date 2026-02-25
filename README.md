# Bom bom Market Application

Веб-приложение "Витрина интернет-магазина" на Spring Boot.

## Функциональность

- Просмотр товаров с пагинацией, поиском и сортировкой (с кэшированием в Redis)
- Добавление товаров в корзину
- Оформление заказов с проверкой баланса через сервис платежей
- Просмотр истории заказов

## Технологии

- Java 21
- Spring Boot 3.5.9
- Webflux
- R2DBC
- H2 Database
- Redis
- Thymeleaf
- Gradle
- Docker

## Запуск приложения

### Локально с H2 и Redis

```bash
 
 docker run -d --name redis -p 6379:6379 redis:7-alpine

./gradlew :payment-service:bootRun

./gradlew :market-app:bootRun

./gradlew clean build
```

### Запуск в Docker

```bash
docker compose up -d

```

### Доступ к приложениям

- **Основное приложение**: http://localhost:8080
- **Платежный сервис**: http://localhost:8082/api/v1/balance
- **H2 Console**: http://localhost:8080/h2-console
    - JDBC URL: `jdbc:h2:mem:marketdb`
    - User: `sa`
    - Password: нихт