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
- отправка `OTP` через `Telegram`
- логирование запросов

## Структура проекта

- `src/main/java/org/example/api` — HTTP-обработчики
- `src/main/java/org/example/service` — бизнес-логика
- `src/main/java/org/example/dao` — работа с БД через `JDBC`
- `src/main/java/org/example/security` — пароли и токены
- `src/main/java/org/example/notification` — каналы доставки кодов
- `src/main/resources/application.properties` — настройки приложения
- `src/main/resources/db/schema.sql` — SQL-схема БД
- `src/main/resources/db/seed.sql` — стартовые данные БД

## Таблицы

Приложение работает в отдельной схеме `otp_service`, чтобы не конфликтовать с уже существующими таблицами в базе.

- `otp_service.users`
- `otp_service.otp_config`
- `otp_service.otp_codes`

## База данных

В проекте есть два способа подготовки БД:

1. Автоматически при старте приложения
2. Вручную через SQL-файлы

SQL-файлы:

- [schema.sql](src/main/resources/db/schema.sql)
- [seed.sql](src/main/resources/db/seed.sql)

### Вариант 1. Автоматически

При запуске приложение само:

- создаёт схему `otp_service`
- создаёт таблицы
- добавляет стартовую запись в `otp_config`

### Вариант 2. Вручную через DBeaver или psql

Сначала выполнить `schema.sql`, потом `seed.sql`.

Пример для `psql`:

```bash
psql -U postgres -d postgres -f src/main/resources/db/schema.sql
psql -U postgres -d postgres -f src/main/resources/db/seed.sql
```

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

Сейчас доступны каналы `FILE`, `EMAIL`, `SMS` и `TELEGRAM`.

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

Пример отправки в `Telegram`:

```bash
curl -X POST http://localhost:8080/otp/generate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"operationId":"telegram-test-001","channel":"TELEGRAM","destination":"Nikolaj"}'
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
smpp.host=localhost
smpp.port=2775
smpp.system_id=smppclient1
smpp.password=password
smpp.system_type=OTP
smpp.source_addr=OTPService
```

Для теста нужен запущенный `SMPPsim` или другой совместимый `SMPP`-эмулятор. Если файл не заполнен или эмулятор не запущен, отправка по `SMS` вернёт ошибку конфигурации или соединения.

## Настройка Telegram

Перед использованием канала `TELEGRAM` заполнить файл [telegram.properties](/Users/nikolaj/IdeaProjects/specialized_tools_java/src/main/resources/telegram.properties):

```properties
telegram.bot.token=YOUR_BOT_TOKEN
telegram.chat.id=YOUR_CHAT_ID
telegram.api.base-url=https://api.telegram.org
```

Чтобы получить `chat.id`:

1. Создать бота через `@BotFather`
2. Начать с ботом диалог
3. Открыть:

```text
https://api.telegram.org/botYOUR_BOT_TOKEN/getUpdates
```

4. Найти значение `message.chat.id`

Если файл не заполнен, приложение запустится, но отправка по `TELEGRAM` вернёт ошибку конфигурации.
