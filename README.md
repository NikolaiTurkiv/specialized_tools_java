# OTP Service

Учебный backend-проект по работе с OTP.

## Реализовано

- HTTP API на `com.sun.net.httpserver`
- `Gradle`-сборка
- `PostgreSQL + JDBC`
- регистрация и логин пользователей
- роли `ADMIN` и `USER`
- ограничение: в системе может быть только один администратор
- настройка длины `OTP` и времени жизни кода
- генерация `OTP` для операции
- валидация `OTP`
- перевод просроченных кодов в статус `EXPIRED` по расписанию
- сохранение сгенерированного кода в файл `otp-codes.txt`
- отправка `OTP` по `Email`
- отправка `OTP` по `SMS` через `SMPP`
- логирование запросов

## Структура проекта

- `src/main/java/org/example/api` — HTTP-обработчики
- `src/main/java/org/example/service` — бизнес-логика
- `src/main/java/org/example/dao` — работа с БД через `JDBC`
- `src/main/java/org/example/security` — пароли и токены
- `src/main/java/org/example/notification` — каналы доставки кодов
- `src/main/resources/application.properties` — настройки приложения

## Таблицы

Приложение работает в отдельной схеме `otp_service`, чтобы не конфликтовать с уже существующими таблицами в базе.

- `otp_service.users`
- `otp_service.otp_config`
- `otp_service.otp_codes`

## Запуск

```bash
./gradlew run
```

Проверка доступности:

```bash
curl http://localhost:8080/health
```

## Основные эндпоинты

### 1. Регистрация

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","role":"ADMIN"}'
```

### 2. Логин

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 3. Получить OTP-конфиг администратора

```bash
curl http://localhost:8080/admin/config \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. Изменить OTP-конфиг администратора

```bash
curl -X PUT http://localhost:8080/admin/config \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"codeLength":4,"ttlSeconds":120}'
```

### 5. Получить список обычных пользователей

```bash
curl http://localhost:8080/admin/users \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 6. Удалить пользователя

```bash
curl -X DELETE http://localhost:8080/admin/users/2 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 7. Сгенерировать OTP

Сейчас доступны каналы `FILE`, `EMAIL` и `SMS`.

```bash
curl -X POST http://localhost:8080/otp/generate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"operationId":"payment-001","channel":"FILE"}'
```

После этого код появится в файле `otp-codes.txt` в корне проекта.

Пример отправки по `Email`:

```bash
curl -X POST http://localhost:8080/otp/generate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"operationId":"payment-002","channel":"EMAIL","destination":"user@example.com"}'
```

Пример отправки по `SMS`:

```bash
curl -X POST http://localhost:8080/otp/generate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"operationId":"sms-test-001","channel":"SMS","destination":"+79991234567"}'
```

### 8. Валидировать OTP

```bash
curl -X POST http://localhost:8080/otp/validate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"operationId":"payment-001","code":"1234"}'
```

## Как проверить проект

1. Запустить приложение.
2. Зарегистрировать администратора.
3. Выполнить логин и сохранить токен.
4. Изменить `OTP`-конфигурацию через admin API.
5. Зарегистрировать обычного пользователя.
6. Выполнить логин обычного пользователя.
7. Сгенерировать `OTP` через канал `FILE`.
8. Открыть `otp-codes.txt` и взять код.
9. Отправить код на валидацию.

## Сборка

```bash
./gradlew build
```

## Настройка Email

Перед использованием канала `EMAIL` заполнить файл [email.properties](/Users/nikolaj/IdeaProjects/specialized_tools_java/src/main/resources/email.properties):

у меня отрабатывает только на мобильном интернете

```properties
email.username=your_email@example.com
email.password=your_app_password
email.from=your_email@example.com
mail.smtp.host=smtp.example.com
mail.smtp.port=587
mail.smtp.auth=true
mail.smtp.starttls.enable=true
```

Если файл не заполнен, приложение запустится, но отправка по `EMAIL` вернёт ошибку конфигурации.

## Настройка SMS

Перед использованием канала `SMS` заполнить файл [sms.properties](/Users/nikolaj/IdeaProjects/specialized_tools_java/src/main/resources/sms.properties):

```properties
smpp.host=
smpp.port=
smpp.system_id=
smpp.password=
smpp.system_type=OTP
smpp.source_addr=OTPService
```

Для теста нужен запущенный `SMPPsim` или другой совместимый `SMPP`-эмулятор. Если файл не заполнен или эмулятор не запущен, отправка по `SMS` вернёт ошибку конфигурации или соединения.
