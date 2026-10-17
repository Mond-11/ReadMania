# ReadMania

Вебзастосунок книжкового інтернет-магазину «ReadMania». Монорепозиторій: бекенд і фронтенд в одному репозиторії.

## Структура репозиторію

```
ReadMania/
├── backend/                        # серверна частина (Java, Spring Boot, Maven)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/ua/readmania/  # вихідний код (пакети за доменами)
│   │   │   │   ├── common/         # спільні компоненти, обробка помилок
│   │   │   │   └── security/       # налаштування Spring Security
│   │   │   └── resources/
│   │   │       ├── application.yml # конфігурація застосунку
│   │   │       └── db/migration/   # міграції бази даних (Flyway)
│   │   └── test/java/ua/readmania/ # тести
│   ├── pom.xml                     # опис збірки Maven
│   ├── mvnw, mvnw.cmd              # Maven Wrapper
│   └── .mvn/                       # налаштування Maven Wrapper
├── frontend/                       # клієнтська частина (React, Vite)
│   ├── src/                        # вихідний код
│   ├── public/                     # статичні файли
│   ├── index.html                  # точка входу
│   └── package.json                # залежності та скрипти npm
├── docs/                           # документація (ТЗ, SRS, діаграми)
├── docker-compose.yml              # локальна база даних PostgreSQL
├── .editorconfig                   # єдині правила форматування
├── .gitattributes                  # правила обробки кінців рядків
├── .gitignore                      # файли, що не потрапляють у git
└── README.md                       # цей файл
```
