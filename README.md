# Spring Security Authorization (Reactive)

Демонстрационное приложение на **Spring Boot 3.4.1**, показывающее механизмы **авторизации**
в реактивном стеке (**Spring WebFlux**) с использованием **Spring Security**.

## Обзор и назначение

Проект — учебный пример настройки авторизации в реактивном Spring-приложении.
Он демонстрирует:

- **URL-уровень**: настройку цепочки фильтров безопасности (`SecurityWebFilterChain`) с правилами доступа для различных
  путей.
- **Метод-уровень**: защиту бизнес-методов аннотацией `@PreAuthorize` в реактивном контексте.
- **Иерархию ролей** (`RoleHierarchy`): роль `MANAGER` автоматически включает полномочия `VERIFIED_USER`.
- **Иерархию полномочий**: `delete_posts > create_posts > view_posts`.
- **AOP Advisor**: альтернативный (закомментированный) способ защиты методов без аннотаций — через
  `AuthorizationManagerBeforeReactiveMethodInterceptor`.
- **Тестирование**: проверку авторизации в реактивном контексте с помощью `@WithMockUser`, ручной установки
  `ReactiveSecurityContextHolder` и `StepVerifier`.

Бизнес-логика минимальна — есть единственный сервис `TodoService`, который создаёт задачу (`Todo`).
Основной фокус — на **конфигурации безопасности и тестах**.

## Стек технологий

| Компонент      | Версия / Описание                           |
|----------------|---------------------------------------------|
| Язык           | Java 21                                     |
| Фреймворк      | Spring Boot 3.4.1                           |
| Реактивный веб | Spring WebFlux                              |
| Безопасность   | Spring Security                             |
| Сборщик        | Maven 3.9.9 (через Maven Wrapper 3.3.2)     |
| Тестирование   | JUnit 5, Reactor Test, Spring Security Test |

## Требования

- **JDK 21** или выше
- **Maven** 3.9+ (или используйте встроенный Maven Wrapper — `mvnw`)

## Настройка и запуск

### Сборка проекта

```bash
./mvnw clean package
```

### Запуск приложения

```bash
./mvnw spring-boot:run
```

По умолчанию приложение запускается на порту **8080**.

> **Примечание:** В проекте не настроен `ReactiveUserDetailsService`, поэтому Spring Security
> автоматически сгенерирует пароль для пользователя `user` (выведется в лог при запуске).

## Скрипты (Maven)

| Команда                  | Описание                                   |
|--------------------------|--------------------------------------------|
| `./mvnw clean package`   | Сборка проекта                             |
| `./mvnw spring-boot:run` | Запуск приложения                          |
| `./mvnw test`            | Запуск тестов                              |
| `./mvnw clean install`   | Сборка и установка в локальный репозиторий |

Для Windows используйте `mvnw.cmd` вместо `./mvnw`.

## Переменные окружения

На данный момент проект не использует специфичных переменных окружения.

| Файл                                        | Описание                                       |
|---------------------------------------------|------------------------------------------------|
| `src/main/resources/application.properties` | Имя приложения (`spring.application.name`)     |
| `src/test/resources/application.yml`        | Уровень логирования для тестов (`root: DEBUG`) |

## Описание настроенных бинов

Все бины определены в классе `ApplicationConfiguration`.

### 1. SecurityWebFilterChain

Определяет правила доступа на уровне HTTP-запросов. Порядок правил имеет значение — проверка останавливается на первом
подходящем.

| Путь / Правило        | Политика доступа                          |
|-----------------------|-------------------------------------------|
| `HEAD *`              | Запрещён (`denyAll`)                      |
| `/api/orders/{id:\d}` | Требуется аутентификация                  |
| `/api/orders?search`  | Требуется аутентификация                  |
| `/api/**`             | Требуется аутентификация                  |
| `GET /permit-all`     | Доступно всем (`permitAll`)               |
| `/deny-all`           | Запрещён (`denyAll`)                      |
| `/anonymous`          | Только неаутентифицированные пользователи |
| `/authenticated`      | Требуется аутентификация                  |
| `/has-authority`      | Требуется полномочие `view`               |
| `/has-any-authority`  | Требуется `view` или `edit`               |
| `/has-admin-role`     | Требуется роль `admin`                    |
| `/has-any-role`       | Требуется роль `admin` или `user`         |

> `AccessDeniedException` перехватывается `ExceptionTranslationWebFilter`.

### 2. RoleHierarchy

Настраивает иерархию ролей и полномочий:

```text
ROLE_MANAGER > ROLE_VERIFIED_USER
delete_posts > create_posts
create_posts > view_posts
```

Это означает, что пользователь с ролью `MANAGER` автоматически получает все права `VERIFIED_USER`,
а обладатель `delete_posts` может также `create_posts` и `view_posts`.

### 3. Реактивная безопасность методов (@EnableReactiveMethodSecurity)

Включена поддержка аннотаций безопасности в реактивных методах.

- **`TodoService.create(String)`** — защищён `@PreAuthorize("hasRole('VERIFIED_USER')")`.
  Только пользователи с ролью `VERIFIED_USER` (или `MANAGER` — благодаря иерархии) могут создавать задачи.

### 4. Advisor (закомментирован) — protectCreateTodoMethodPointcut

Альтернативный способ настройки безопасности через AOP без аннотаций в сервисе.
Использует `JdkRegexpMethodPointcut` + `AuthorizationManagerBeforeReactiveMethodInterceptor`.

> ⚠️ **TODO:** Возможно, не поддерживает иерархию ролей.

### 5. MethodSecurityExpressionHandler (закомментирован)

Настраивает поддержку иерархии ролей в SpEL-выражениях безопасности.
Актуален только при использовании AOP Advisor вместо аннотаций.

> ⚠️ **TODO:** Исследовать корректность работы в контексте WebFlux.

## Тестирование

```bash
./mvnw test
```

### TodoServiceSecurityTests

Проверяет работу `@PreAuthorize` в реактивном контексте:

| Тест                                        | Что проверяет                                                       |
|---------------------------------------------|---------------------------------------------------------------------|
| `testCreateWithMockUser_ReturnsCreatedTodo` | Успешное создание задачи с `@WithMockUser(roles = "VERIFIED_USER")` |
| `testCreateWithValidRole`                   | Успешное создание через ручную установку `SecurityContext`          |
| `testCreateWithInvalidRole`                 | `AccessDeniedException` при неверной роли (`USER`)                  |
| `testCreateWithoutAuthentication`           | `AccessDeniedException` без аутентификации                          |

## Структура проекта

```text
spring-security-authorization/
├── .mvn/wrapper/                               # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/olga/springsecurity/authorization/
│   │   │   ├── SpringSecurityAuthorizationApplication.java  # Точка входа (main)
│   │   │   ├── ApplicationConfiguration.java                # Конфигурация безопасности
│   │   │   ├── Todo.java                                    # Record: name, id, owner
│   │   │   └── TodoService.java                             # Сервис с @PreAuthorize
│   │   └── resources/
│   │       └── application.properties                       # Настройки приложения
│   └── test/
│       ├── java/olga/springsecurity/authorization/
│       │   ├── SpringSecurityAuthorizationApplicationTests.java  # Тест контекста
│       │   └── TodoServiceSecurityTests.java                    # Тесты авторизации
│       └── resources/
│           └── application.yml                              # Настройки тестов (DEBUG логи)
├── mvnw / mvnw.cmd                            # Maven Wrapper скрипты
├── pom.xml                                     # Конфигурация сборки
└── README.md
```

## TODO

- [ ] Добавить `ReactiveUserDetailsService` для полноценного запуска (сейчас используется автогенерация пароля или моки
  в тестах).
- [ ] Протестировать поддержку иерархии ролей в закомментированном `Advisor`.
- [ ] Исследовать корректность `MethodSecurityExpressionHandler` в контексте WebFlux.
- [ ] Добавить endpoint-контроллер (сейчас настроены только правила, но нет `@RestController`).
- [ ] Раскомментировать и настроить `spring-boot-starter-data-r2dbc` для хранения данных.

## Лицензия

Лицензия не указана. Смотрите `pom.xml` (секция `<licenses>` пуста).

```