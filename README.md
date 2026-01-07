# Hotel Booking Microservices (Java 17, Spring Boot 3.4.x)

Учебный проект системы бронирования отелей на микросервисной архитектуре.

## Архитектура

Сервисы:
- `eureka-server` — Service Discovery (Eureka)
- `api-gateway` — API Gateway (Spring Cloud Gateway)
- `hotel-service` — управление отелями и номерами
- `booking-service` — бронирования + регистрация/аутентификация пользователей

Технологии:
- Java 17
- Spring Boot 3.4.x / Spring Cloud 2024.0.1
- Spring Data JPA + H2 (in-memory)
- Spring Security + JWT (Resource Server)
- Swagger (springdoc-openapi)

## Запуск

Сборка:
```bash
mvn -ntp clean package
```

Порядок старта (в отдельных терминалах):
```bash
mvn -ntp -pl eureka-server spring-boot:run
mvn -ntp -pl hotel-service spring-boot:run
mvn -ntp -pl booking-service spring-boot:run
mvn -ntp -pl api-gateway spring-boot:run
```

Порты:
- Eureka: `8761`
- Hotel service: `8081`
- Booking service: `8086`
- API Gateway: `8085`

H2 in-memory сбрасывается при каждом рестарте сервисов.

## Проверка gateway/actuator

- Health: `http://localhost:8085/actuator/health`
- Routes (если поддерживается): `http://localhost:8085/actuator/gateway/routes`

## Примеры curl.exe (через gateway)

Аутентификация:
```bash
curl.exe -X POST http://localhost:8085/api/bookings/user/auth \
  -H "Content-Type: application/json" \
  --data @auth.json
```

Бронирование (manual room):
```bash
curl.exe -X POST http://localhost:8085/api/bookings/booking \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  --data @booking.json
```

Бронирование (autoSelect):
```bash
curl.exe -X POST http://localhost:8085/api/bookings/booking \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  --data @bookingAuto.json
```

История бронирований:
```bash
curl.exe http://localhost:8085/api/bookings/bookings \
  -H "Authorization: Bearer <TOKEN>"
```

Список отелей:
```bash
curl.exe http://localhost:8085/api/hotels \
  -H "Authorization: Bearer <TOKEN>"
```

Список номеров:
```bash
curl.exe "http://localhost:8085/api/hotels/rooms?hotelId=1" \
  -H "Authorization: Bearer <TOKEN>"
```

Рекомендации номеров:
```bash
curl.exe "http://localhost:8085/api/hotels/rooms/recommend?startDate=2026-01-10&endDate=2026-01-12" \
  -H "Authorization: Bearer <TOKEN>"
```

Примечание: `requestId` обязателен в каждом бронировании. Для повторных запросов используйте новый UUID.

## Тестовые данные

- ADMIN: `admin / adminpass`
- USER: `user / userpass`
- 2 отеля, по 5 номеров, разные `timesBooked`

## Сага бронирования и компенсация

`POST /booking`:
1. `booking-service` создает запись `Booking` со статусом `PENDING`.
2. Вызывает `hotel-service`:
   `POST /api/rooms/{id}/confirm-availability` (startDate, endDate, requestId, bookingId).
3. Успех — `CONFIRMED`.
4. Ошибка/timeout — `CANCELLED`, компенсация:
   `POST /api/rooms/{id}/release`.

Идемпотентность:
- `requestId` обязателен для создания бронирования.
- `confirm-availability` и `release` идемпотентны по `requestId`.

## Внутренние эндпойнты hotel-service

- `POST /api/rooms/{id}/confirm-availability`
- `POST /api/rooms/{id}/release`

Не публикуются через gateway и защищены заголовком `X-Internal-Token`.

## Тестирование

```bash
mvn -ntp clean test
```

Покрыто тестами:
- успешное бронирование (PENDING -> CONFIRMED)
- конфликт по датам (409)
- timeout + retry -> CANCELLED + release
- идемпотентность requestId (hotel-service)
- параллельное бронирование
- autoSelect без свободных комнат -> 409 и без записи

## Проверка соответствия ТЗ

- gateway маршрутизирует только `/api/bookings/**` и `/api/hotels/**`
- saga с компенсацией и retry/backoff реализована в `booking-service`
- autoSelect выбирает комнаты по рекомендациям (timesBooked ASC, id ASC)
- конфликтные даты -> 409 без дублей
- роли USER/ADMIN и внутренние эндпойнты защищены
