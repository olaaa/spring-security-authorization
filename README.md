# Spring Security Authorization (Reactive)

Этот проект представляет собой демонстрационное приложение на **Spring Boot**, демонстрирующее механизмы **авторизации**
в реактивном стеке (**Spring WebFlux**) с использованием **Spring Security**.

## Основное назначение

Проект служит примером настройки прав доступа на уровне веб-запросов и на уровне методов в реактивном приложении. В нем
реализованы:

- Настройка цепочки фильтров безопасности (`SecurityWebFilterChain`).
- Иерархия ролей (Role Hierarchy).
- Защита методов с использованием аннотаций (`@PreAuthorize`).
- Тестирование безопасности реактивных сервисов.

## Стек технологий

- **Язык**: Java 21
- **Фреймворк**: Spring Boot 3.4.1
- **Модули**: Spring WebFlux, Spring Security
- **Сборщик**: Maven
- **Библиотеки для тестирования**: JUnit 5, Reactor Test, Spring Security Test

## Требования

Для запуска проекта вам потребуются:

- JDK 21 или выше.
- Maven (или использование `mvnw`, входящего в проект).

## Настройка и запуск

### Сборка проекта

```bash
./mvnw clean package
```

### Запуск приложения

```bash
./mvnw spring-boot:run
```

По умолчанию приложение запускается на порту `8080`.

## Описание настроенных бинов и безопасности

### 1. `SecurityWebFilterChain`

Настроен в `ApplicationConfiguration`. Определяет правила доступа к различным URL:

- `/api/orders/**` — требуется аутентификация.
- `/permit-all` — доступно всем.
- `/deny-all` — закрыто для всех.
- `/has-authority`, `/has-admin-role` и др. — примеры проверок прав и ролей.
- `/anonymous` — доступ только для неаутентифицированных пользователей.

### 2. `RoleHierarchy` (Иерархия ролей)

Определяет подчиненность ролей и полномочий:

- `ROLE_MANAGER > ROLE_VERIFIED_USER` (Менеджер наследует права верифицированного пользователя).
- `delete_posts > create_posts > view_posts` (Иерархия полномочий).

### 3. Защита методов

В проекте включена поддержка реактивной безопасности методов (`@EnableReactiveMethodSecurity`).

- **`TodoService.create`**: защищен аннотацией `@PreAuthorize("hasRole('VERIFIED_USER')")`.

### 4. `Advisor` (Инфраструктурный компонент)

В `ApplicationConfiguration` есть закомментированный бин `protectCreateTodoMethodPointcut`, который показывает
альтернативный способ настройки безопасности через AOP Advisor без использования аннотаций в сервисе.

## Скрипты

В корне проекта доступны стандартные скрипты Maven Wrapper:

- `mvnw` / `mvnw.cmd` — запуск команд Maven без необходимости предустановки Maven в системе.

## Переменные окружения

На данный момент проект не использует специфичных переменных окружения. Все настройки находятся в
`src/main/resources/application.properties`.

## Тестирование

Для запуска тестов используйте команду:

```bash
./mvnw test
```

Основные тесты:

- `TodoServiceSecurityTests`: Проверяет работу `@PreAuthorize` в реактивном контексте с использованием `@WithMockUser` и
  ручной настройки `SecurityContext`.

## Структура проекта

```text
src/main/java/olga/springsecurity/authorization/
├── ApplicationConfiguration.java       # Конфигурация безопасности и бинов
├── SpringSecurityAuthorizationApplication.java # Точка входа
├── Todo.java                           # Модель данных (Record)
└── TodoService.java                    # Бизнес-логика с защитой методов

src/test/java/olga/springsecurity/authorization/
├── SpringSecurityAuthorizationApplicationTests.java
└── TodoServiceSecurityTests.java       # Тесты авторизации
```

## TODO

- [ ] Доработать и протестировать поддержку иерархии ролей в закомментированном `Advisor`.
- [ ] Добавить настройку `ReactiveUserDetailsService` для полноценного запуска в рантайме (сейчас используется
  конфигурация по умолчанию или моки в тестах).
- [ ] Исследовать использование `MethodSecurityExpressionHandler` в контексте WebFlux.

## Лицензия

Информация о лицензии не указана (см. `pom.xml`).
