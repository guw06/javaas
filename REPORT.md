# AURA Project Report

GitHub: https://github.com/guw06/javaas

## Описание

AURA - персональный Java-ассистент с веб-интерфейсом, REST backend, SQLite-базой, голосовым вводом, системными командами Windows, памятью, задачами, напоминаниями и AI-интеграцией через локальный конфиг `aura.properties`.

## Рубрика

| Критерий | Балл | Что реализовано |
| --- | ---: | --- |
| Фронтенд | 20 | Полноэкранный веб-интерфейс, чат, голосовые настройки, быстрые команды, статус backend, runtime-блок, CRUD-панель, demo mode, live log, панель 100/100, GitHub-ссылка. |
| Бекэнд | 50 | Java 21 + Javalin REST API, SQLite, маршрутизация команд, системные сервисы, AI provider auto mode, настройки, история, задачи, напоминания, action log, project items API. |
| Добавит/изменит/заменит/удалит | 10 | REST CRUD для `/api/project-items`: POST add, PUT update/replace status, DELETE remove, GET list. |
| Безопасность | 10 | `aura.properties` игнорируется Git, ключи не выводятся в UI, prepared statements для SQL, request body limits, status validation, safe file walker, security headers. |
| GitHub link | 5 | Ссылка указана в UI и отчете: https://github.com/guw06/javaas |
| Отчет | 5 | Этот файл фиксирует функционал, критерии и итоговую оценку. |
| Total | 100 | Проект закрывает все пункты рубрики. |

## Проверка

1. Запуск: `.\start.bat`
2. Backend health: `http://localhost:8081/ping` или порт, который AURA покажет при старте.
3. Frontend: открыть URL из консоли запуска.
4. CRUD: в правой панели добавить пункт, изменить, отметить `Готово`, удалить.
5. Demo Mode: нажать `Показать add/edit/done/delete`, чтобы автоматически показать полный CRUD-сценарий.

## Безопасность

- API-ключи хранятся только локально в `aura.properties`.
- Файл `aura.properties`, база `*.db`, логи `*.log` и `target/` находятся в `.gitignore`.
- SQL-запросы с пользовательским вводом используют `PreparedStatement`.
- Файловый поиск пропускает закрытые Windows-папки и не роняет backend.
- REST endpoints ограничивают размер тела запроса и валидируют статус проектного пункта.
