# AURA - Personal AI Assistant

AURA - персональный голосовой и текстовый ассистент на Java. Проект помогает управлять компьютером, работать с файлами, открывать программы и сайты, вести задачи, напоминания, сценарии и обращаться к AI через Gemini/OpenAI.

## Возможности

- Голосовой ввод через Web Speech API.
- Озвучивание ответов через Speech Synthesis API.
- Wake words: `аура`, `aura`, `ассистент`.
- Управление программами, браузером, вкладками, Wi-Fi/Bluetooth, диспетчером задач.
- Работа с файлами: создать, открыть, переместить, безопасно удалить.
- Создание и открытие Word-документов.
- Поиск файлов по рабочему столу, Документам и Загрузкам.
- Память, профиль пользователя, задачи, напоминания, привычки и сценарии.
- AI fallback: Gemini -> OpenAI при режиме `ai.provider=auto`.
- Диагностика AI без вывода API-ключей.
- Журнал действий и безопасные подтверждения опасных команд.

## Технологии

- Java 21
- Maven
- Javalin
- SQLite
- Gson
- OkHttp
- HTML, CSS, JavaScript

## Запуск

```powershell
.\start.bat
```

После запуска откройте:

```text
http://localhost:8080
```

Проверка backend:

```text
http://localhost:8080/ping
```

Ответ должен быть:

```text
pong
```

## Конфигурация AI

Локальные ключи хранятся в `aura.properties`. Этот файл не должен попадать в Git.

Пример:

```properties
ai.provider=auto
gemini.api.key=YOUR_GEMINI_API_KEY
gemini.model=gemini-2.5-flash-lite
openai.api.key=YOUR_OPENAI_API_KEY
openai.model=gpt-5.4-mini
```

## Полезные команды для демонстрации

```text
привет
помощь
сводка дня
добавь задачу сдать проект
покажи задачи
найди последний Word документ
режим учебы
проверь нейросеть
что ты сегодня делала
```

## Структура

```text
src/main/java/Main.java                     - запуск сервера
src/main/java/com/assistant/CommandManager.java
src/main/java/com/assistant/commands/       - команды AURA
src/main/java/com/assistant/services/       - сервисы, база, AI, файлы
frontend/                                   - веб-интерфейс
knowledge/aura-knowledge.tsv                - локальные знания
docs/                                       - ТЗ проекта
```

## Безопасность

- `aura.properties`, `.env`, `*.db`, `target/` игнорируются Git.
- Удаление важных данных требует подтверждения: `да, подтверждаю`.
- Системные папки Windows защищены от файловых команд.
- Локальная база проекта называется `aura.db`.
