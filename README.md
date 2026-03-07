# Bom bom Market Application

Веб-приложение "Витрина интернет-магазина" на Spring Boot.

## Функциональность

- Просмотр товаров с пагинацией, поиском и сортировкой (с кэшированием в Redis)
- Добавление товаров в корзину (только для авторизованных пользователей)
- Оформление заказов с проверкой баланса через сервис платежей
- Просмотр истории заказов (привязана к конкретному пользователю)
- Авторизация пользователей по логину/паролю

## Технологии

- Java 21
- Spring Boot 3.5.9
- Webflux
- R2DBC
- H2 Database
- Redisа
- Thymeleaf
- Keycloak
- Gradle
- Docker

## Запуск приложения

### Запуск в Docker

```bash
# Шаг 1: Запуск Redis и Keycloak
docker compose up -d redis keycloak

# Шаг 2: Настройка Keycloak (один раз)
# http://localhost:8085 (admin/admin)
# Создать realm: bom-bom-market
# Создайте клиента market-app:
# Client ID: market-app
# Client authentication: ON
# Service accounts roles: ON
# Valid redirect URIs: http://localhost:8080/*
# Создайте клиента payment-service (также)
# Перейдите в Clients → market-app → Credentials
# Скопируйте Client secret (при start-dev не требуется)

# Шаг 3: Создать .env файл с секретом (главное без BOM иначе не взлетит)
echo "KEYCLOAK_CLIENT_SECRET=скопированный_секрет" > .env 

# Шаг 4: Запуск всех сервисов
docker compose up -d

# Просмотр логов
docker compose logs -f

# Остановка
docker compose down
```

### Доступ к приложениям

- **Основное приложение**: http://localhost:8080
- **Keycloak**: http://localhost:8085 (admin/admin)

### Тестовые пользователи

| Логин | Пароль | Баланс          |
|-------|--------|-----------------|
| user1 | 1234 | 150000.00 Денег |
| user2 | 1234 | 50000.00  Денег |_