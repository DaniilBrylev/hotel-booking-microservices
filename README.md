# Hotel Booking Microservices — REST API (Java 17, Spring Boot 3.4.x)

Учебный проект: **REST API системы бронирования отелей** на **микросервисной архитектуре** (Eureka + API Gateway).
Ключевые требования ТЗ покрываются: **JWT + роли USER/ADMIN**, **согласованность через двухшаговое подтверждение (Saga)**,
**идемпотентность по `requestId`**, **autoSelect** (подбор номера по статистике `timesBooked`).

> Важно: сервисы используют **H2 in-memory**, данные сбрасываются после перезапуска.

---

## 1) Состав проекта

- **eureka-server** — Service Discovery (Eureka)
- **api-gateway** — маршрутизация `/api/**` (Spring Cloud Gateway), проксирование запросов в сервисы
- **hotel-service** — управление отелями/номерами, выдача доступных/рекомендованных номеров, внутренние эндпойнты подтверждения/компенсации
- **booking-service** — регистрация/аутентификация (JWT), CRUD бронирований, интеграция с `hotel-service`, Saga

---

## 2) Технологии

- Java 17
- Spring Boot 3.4.x
- Spring Cloud 2024.0.1
- Spring Data JPA + H2 (in-memory)
- Spring Security + JWT (Resource Server в сервисах)
- Springdoc OpenAPI (Swagger UI)
- JUnit 5

---

## 3) Порты

- Eureka Server: `8761`
- Hotel Service: `8081`
- Booking Service: `8086`
- API Gateway: `8085`

---

## 4) Сборка

```bash
mvn -ntp clean package
```

---

## 5) Запуск сервисов (локально)

Рекомендуемый порядок запуска:

```bash
mvn -ntp -pl eureka-server spring-boot:run
mvn -ntp -pl hotel-service spring-boot:run
mvn -ntp -pl booking-service spring-boot:run
mvn -ntp -pl api-gateway spring-boot:run --% -Dspring-boot.run.arguments=--server.port=8085
```

Проверка, что gateway поднялся:

```bash
curl.exe -i http://localhost:8085/actuator/health
```

---

## 6) Gateway Actuator (для проверки маршрутов)

- Health: `http://localhost:8085/actuator/health`
- Routes: `http://localhost:8085/actuator/gateway/routes`

> Если `/actuator/gateway/routes` возвращает 404 — в `api-gateway` включён флаг `spring.cloud.gateway.actuator.verbose.enabled=true`.

---

## 7) Swagger / OpenAPI

Swagger UI:

- Booking service: `http://localhost:8086/swagger-ui/index.html`
- Hotel service: `http://localhost:8081/swagger-ui/index.html`

---

## 8) Доступы и роли (тестовые пользователи)

- **ADMIN**: `admin / adminpass`
- **USER**: `user / userpass`

Ограничения:
- **USER** — операции со своими бронированиями + просмотр отелей/номеров
- **ADMIN** — CRUD отелей/номеров/пользователей (если включено контроллерами)

---

## 9) Маршруты через Gateway (публичные)

Все публичные запросы идут **только через gateway** `http://localhost:8085/api/**`:

- `/api/bookings/**` → `booking-service`
- `/api/hotels/**` → `hotel-service`

Внутренние эндпойнты **не должны быть доступны через gateway** (см. раздел 11).

---

## 10) Основные эндпойнты (по ТЗ)

### Booking service (через gateway)
- `POST /api/bookings/user/register` — регистрация (USER)
- `POST /api/bookings/user/auth` — аутентификация, JWT (USER)
- `POST /api/bookings/booking` — создать бронирование (USER)
- `GET  /api/bookings/bookings` — история бронирований текущего пользователя (USER)
- `GET  /api/bookings/booking/{id}` — получить бронирование (USER)
- `DELETE /api/bookings/booking/{id}` — отменить бронирование (USER)

> В запросе на создание бронирования **обязателен `requestId`** (UUID).

### Hotel service (через gateway)
- `GET /api/hotels` — список отелей (USER)
- `GET /api/hotels/rooms` — список доступных номеров (USER)  
  Параметры: `startDate`, `endDate` (опционально), `hotelId` (опционально)
- `GET /api/hotels/rooms/recommend` — рекомендованные доступные номера (USER)  
  Параметры: `startDate`, `endDate` (обязательные), опционально `hotelId`

Алгоритм рекомендаций (ТЗ):
- сортировка по `timesBooked ASC`, при равенстве — по `id ASC`

---

## 11) Внутренние эндпойнты hotel-service (INTERNAL)

Используются только как часть Saga и **не должны быть доступны через gateway**:

- `POST /api/rooms/{id}/confirm-availability`
- `POST /api/rooms/{id}/release`

Через gateway такие запросы должны быть **403/404** (например, из-за удаления `X-Internal-Token`).

---

## 12) Saga: двухшаговое подтверждение + компенсация

При создании бронирования:

1) `booking-service` создаёт запись `Booking` со статусом `PENDING` (локальная транзакция)  
2) вызывает `hotel-service`:
   `POST /api/rooms/{id}/confirm-availability` (передаёт `startDate`, `endDate`, `requestId`, `bookingId`)  
3) при успехе — переводит в `CONFIRMED`  
4) при ошибке/тайм-ауте — переводит в `CANCELLED` и выполняет компенсацию:
   `POST /api/rooms/{id}/release`

Надёжность (ТЗ):
- для удалённого вызова заданы timeout + retry/backoff
- операции идемпотентны по `requestId` (повтор запроса не создаёт дубль)

---

## 13) Примеры запросов (PowerShell / curl.exe)

### 13.1 Получить JWT (через файл, надёжно для PowerShell)

Создать `auth.json` (UTF‑8 без BOM):

```powershell
@'
{"username":"user","password":"userpass"}
'@ | Set-Content -NoNewline -Encoding utf8 .\auth.json
```

Запросить токен:

```powershell
$auth = curl.exe -s -H "Content-Type: application/json" --data-binary "@auth.json" http://localhost:8085/api/bookings/user/auth
$token = ($auth | ConvertFrom-Json).token
$token
```

### 13.2 Создать бронирование (manual room)

Создать `booking.json` (обязателен requestId):

```powershell
@'
{
  "requestId": "11111111-1111-1111-1111-111111111111",
  "startDate": "2026-01-10",
  "endDate": "2026-01-12",
  "autoSelect": false,
  "roomId": 1
}
'@ | Set-Content -NoNewline -Encoding utf8 .\booking.json
```

Запрос:

```powershell
curl.exe -i http://localhost:8085/api/bookings/booking `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  --data-binary "@booking.json"
```

### 13.3 Создать бронирование (autoSelect)

```powershell
@'
{
  "requestId": "22222222-2222-2222-2222-222222222222",
  "startDate": "2026-01-10",
  "endDate": "2026-01-12",
  "autoSelect": true
}
'@ | Set-Content -NoNewline -Encoding utf8 .\bookingAuto.json
```

```powershell
curl.exe -i http://localhost:8085/api/bookings/booking `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  --data-binary "@bookingAuto.json"
```

### 13.4 Доступные номера и рекомендации

```powershell
curl.exe -i "http://localhost:8085/api/hotels/rooms?startDate=2026-01-10&endDate=2026-01-12" `
  -H "Authorization: Bearer $token"
```

```powershell
curl.exe -i "http://localhost:8085/api/hotels/rooms/recommend?startDate=2026-01-10&endDate=2026-01-12" `
  -H "Authorization: Bearer $token"
```

---

## 14) Тестирование

Запуск всех тестов:

```bash
mvn -ntp clean test
```

Примеры покрытых сценариев (по ТЗ):
- успешное бронирование (PENDING → CONFIRMED)
- конфликт дат/занятость номера → `409`
- тайм-аут/ошибка удалённого сервиса → `CANCELLED` + `release`
- идемпотентность по `requestId` (повтор не создаёт дублей)
- autoSelect без доступных комнат → `409` без “успешных CANCELLED”

---

## 15) Предзаполнение данных

При старте сервисов создаются тестовые данные:
- минимум 2 отеля и несколько номеров
- заполнение `timesBooked` для демонстрации алгоритма рекомендаций
- пользователи `admin/user` (см. раздел 8)

---

## 16) Типовые проблемы

### PowerShell и JSON
В PowerShell используйте **`curl.exe` + `--data-binary "@file.json"`**.  
Не используйте токен с угловыми скобками `<TOKEN>` — иначе будет `401 invalid_token`.

---

## Автор
Брылев Даниил Вячеславович
