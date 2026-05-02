# 🤖 Голосовой Ассистент (Voice Assistant)

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Javalin](https://img.shields.io/badge/Javalin-6.1.3-blue?style=for-the-badge)
![JavaScript](https://img.shields.io/badge/JavaScript-ES6-yellow?style=for-the-badge&logo=javascript)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**Персональный голосовой ассистент с искусственным интеллектом в стиле Джарвис**

[Возможности](#-возможности) • [Установка](#-установка) • [Запуск](#-запуск) • [Команды](#-доступные-команды) • [Технологии](#-технологии)

</div>

---

## 📋 Описание

Голосовой ассистент — это интеллектуальная система, способная распознавать голосовые команды, обрабатывать их и отвечать голосом. Проект построен на архитектуре клиент-сервер с использованием Java backend и современного веб-интерфейса.

### ✨ Возможности

#### 🎤 Голосовое управление
- **Распознавание речи** — Web Speech API для русского языка
- **Синтез речи** — Text-to-Speech для голосовых ответов
- **Wake Word активация** — "Джарвис", "Алиса", "Ассистент"
- **Continuous режим** — постоянная прослушка с wake word

#### 🧠 Интеллектуальные команды

#### 🤖 Умный Fallback с Gemini AI
**Джарвис теперь бесконечно умный!** Если вы спросите что-то, что не является системной командой, ваш вопрос автоматически отправится в Gemini AI. Больше никаких "Команда не распознана"!

**Примеры:**
- "Расскажи про квантовую физику" → Gemini ответит
- "Как приготовить пиццу?" → Gemini ответит
- "Объясни что такое блокчейн" → Gemini ответит
- "Погода" → Системная команда (быстрый ответ)
- "Открой блокнот" → Системная команда (мгновенное выполнение)

#### 📋 Системные команды (приоритет)
- ⏰ **Время и дата** — текущее время и дата
- 🌤️ **Погода** — актуальная погода в Астане
- 😂 **Шутки** — случайные шутки от Chuck Norris API
- 🐱 **Факты о котах** — интересные факты
- 📰 **Новости** — последние новости
- 💱 **Курсы валют** — актуальные курсы USD
- 🌐 **Поиск в Google** — открытие поиска в браузере
- 🗒️ **Блокнот** — запуск приложения
- 💾 **Память** — сохранение и чтение заметок
- 🤖 **Gemini AI** — интеграция с нейросетью Google

#### 🎨 Современный UI
- **Тема Джарвис** — неоновый голубой дизайн
- **Анимации** — плавные переходы и эффекты
- **Индикаторы** — статус подключения и режима
- **Адаптивность** — работает на всех устройствах
- **Чат-интерфейс** — красивые пузырьки сообщений

---

## 🚀 Установка

### Требования

- **Java 21** или выше
- **Maven 3.8+**
- **Современный браузер** (Chrome, Edge, Safari)
- **Интернет-соединение** для API запросов

### Клонирование репозитория

```bash
git clone https://github.com/yourusername/voice-assistant.git
cd voice-assistant
```

### Установка зависимостей

Maven автоматически загрузит все зависимости при первой сборке:

```bash
mvn clean install
```

---

## 🎯 Запуск

### 1. Запуск Backend (Java)

#### Вариант A: Через Maven

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="Main"
```

#### Вариант B: Через IDE

1. Откройте проект в IntelliJ IDEA / Eclipse / VS Code
2. Найдите файл `Main.java`
3. Запустите метод `main()`

#### Вариант C: Через JAR

```bash
mvn clean package
java -jar target/javaas-1.0-SNAPSHOT.jar
```

**Сервер запустится на:** `http://localhost:8080`

### 2. Запуск Frontend (UI)

#### Вариант A: Прямое открытие

1. Перейдите в папку `frontend/`
2. Откройте файл `index.html` в браузере

#### Вариант B: Через Live Server (рекомендуется)

```bash
cd frontend
# Если установлен Python
python -m http.server 3000

# Если установлен Node.js
npx http-server -p 3000
```

Откройте: `http://localhost:3000`

### 3. Проверка работы

1. Откройте браузер и перейдите к `index.html`
2. Проверьте индикатор подключения (должен быть зеленым)
3. Нажмите на кнопку микрофона 🎤
4. Скажите: **"Привет"**
5. Ассистент должен ответить голосом!

---

## 📝 Доступные команды

### 🗣️ Голосовые команды

| Команда | Описание | Пример |
|---------|----------|--------|
| `привет` | Приветствие | "Привет" |
| `время` | Текущее время | "Какое время?" |
| `дата` | Текущая дата | "Какая дата?" |
| `погода` | Погода в Астане | "Какая погода?" |
| `шутка` | Случайная шутка | "Расскажи шутку" |
| `факт` | Факт о котах | "Расскажи факт" |
| `новости` | Последние новости | "Какие новости?" |
| `валюта` | Курсы валют | "Курс доллара" |
| `браузер` | Открыть Google | "Открой браузер" |
| `найди [запрос]` | Поиск в Google | "Найди рецепт пиццы" |
| `блокнот` | Запустить блокнот | "Открой блокнот" |
| `запомни [текст]` | Сохранить заметку | "Запомни купить молоко" |
| `вспомни` | Прочитать заметки | "Что ты помнишь?" |

### 🤖 Gemini AI (Автоматический Fallback)

Теперь **любой вопрос**, который не является системной командой, автоматически отправляется в Gemini AI!

**Вам больше не нужно говорить "спроси" или "нейросеть"** - просто задавайте вопрос:

| Вопрос | Результат |
|--------|----------|
| "Что такое Java?" | Gemini объяснит |
| "Расскажи про космос" | Gemini расскажет |
| "Как работает интернет?" | Gemini ответит |
| "Напиши стих про осень" | Gemini напишет |
| "Объясни теорию относительности" | Gemini объяснит |

**Системные команды имеют приоритет!** Если вы скажете "погода" или "блокнот" - выполнится быстрая системная команда.

### 🎯 Wake Word режим

**Активация:** Двойной клик по кнопке микрофона

В этом режиме ассистент постоянно слушает и реагирует только на команды с wake word:

```
"Джарвис, какая погода?"
"Алиса, расскажи шутку"
"Ассистент, открой браузер"
```

---

## 🛠️ Технологии

### Backend

- **Java 21** — современная версия Java
- **Javalin 6.1.3** — легковесный веб-фреймворк
- **Gson 2.10.1** — парсинг JSON
- **OkHttp 4.12.0** — HTTP клиент для API запросов
- **SLF4J 2.0.12** — логирование

### Frontend

- **HTML5** — структура
- **CSS3** — стилизация с анимациями
- **JavaScript ES6** — логика приложения
- **Web Speech API** — распознавание речи
- **Speech Synthesis API** — синтез речи
- **Fetch API** — HTTP запросы

### Внешние API

- [wttr.in](https://wttr.in) — погода
- [Chuck Norris API](https://api.chucknorris.io) — шутки
- [Cat Facts API](https://catfact.ninja) — факты о котах
- [News API](https://newsapi.org) — новости
- [Exchange Rate API](https://exchangerate-api.com) — курсы валют

---

## 📁 Структура проекта

```
voice-assistant/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── assistant/
│                   ├── commands/          # Команды ассистента
│                   │   ├── Command.java
│                   │   ├── HelloCommand.java
│                   │   ├── TimeCommand.java
│                   │   ├── DateCommand.java
│                   │   ├── WeatherCommand.java
│                   │   ├── JokeCommand.java
│                   │   ├── CatFactCommand.java
│                   │   ├── NewsCommand.java
│                   │   ├── CurrencyCommand.java
│                   │   ├── OpenBrowserCommand.java
│                   │   ├── GoogleSearchCommand.java
│                   │   ├── OpenNotepadCommand.java
│                   │   ├── RememberCommand.java
│                   │   └── ReadMemoryCommand.java
│                   ├── models/            # Модели данных
│                   │   ├── RequestDto.java
│                   │   └── ResponseDto.java
│                   ├── utils/             # Утилиты
│                   │   ├── HttpClientUtil.java
│                   │   └── MemoryUtil.java
│                   └── CommandManager.java # Менеджер команд
├── frontend/
│   ├── index.html                         # Главная страница
│   ├── style.css                          # Стили
│   ├── app.js                             # Логика Speech API
│   └── script.js                          # Логика UI
├── Main.java                              # Точка входа
├── pom.xml                                # Maven конфигурация
├── memory.txt                             # Файл памяти
└── README.md                              # Документация
```

---

## ⚙️ Конфигурация

### Изменение порта сервера

В файле `Main.java`:

```java
Javalin app = Javalin.create(config -> {
    config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
}).start(8080); // Измените порт здесь
```

### Добавление новой команды

1. Создайте класс в `src/main/java/com/assistant/commands/`:

```java
package com.assistant.commands;

public class MyCommand implements Command {
    @Override
    public String execute(String input) {
        return "Ответ команды";
    }
}
```

2. Зарегистрируйте в `Main.java`:

```java
commandManager.register("ключевое_слово", new MyCommand());
```

### Настройка Wake Words

В файле `frontend/app.js`:

```javascript
const wakeWords = ['джарвис', 'алиса', 'ассистент', 'мое_слово'];
```

---

## 🐛 Решение проблем

### Сервер не запускается

- Проверьте, что порт 8080 свободен
- Убедитесь, что Java 21 установлена: `java -version`
- Проверьте Maven: `mvn -version`

### Микрофон не работает

- Разрешите доступ к микрофону в браузере
- Используйте HTTPS или localhost
- Проверьте поддержку Web Speech API в браузере

### Нет ответа от сервера

- Убедитесь, что backend запущен
- Проверьте индикатор подключения (зеленый = OK)
- Откройте консоль браузера (F12) для ошибок
- Проверьте CORS настройки

### Голосовой ответ не работает

- Проверьте, что озвучивание включено
- Убедитесь, что громкость не на нуле
- Проверьте поддержку Speech Synthesis API

---

## 🎨 Кастомизация UI

### Изменение цветовой схемы

В файле `frontend/style.css`:

```css
:root {
    --primary-color: #00f3ff;      /* Основной цвет */
    --bg-dark: #000000;            /* Фон */
    --text-primary: #00f3ff;       /* Текст */
    --error-color: #ff0066;        /* Ошибки */
    --success-color: #00ff88;      /* Успех */
}
```

### Изменение шрифтов

Замените в `index.html`:

```html
<link href="https://fonts.googleapis.com/css2?family=YourFont&display=swap" rel="stylesheet">
```

---

## 📊 API Endpoints

### Backend REST API

| Метод | Endpoint | Описание |
|-------|----------|----------|
| `GET` | `/ping` | Проверка работы сервера |
| `POST` | `/api/command` | Отправка команды |

#### POST /api/command

**Request:**
```json
{
  "text": "привет"
}
```

**Response:**
```json
{
  "response": "Здравствуйте! Чем могу помочь?"
}
```

---

## 🤝 Вклад в проект

Мы приветствуем вклад в развитие проекта!

1. Fork репозитория
2. Создайте ветку: `git checkout -b feature/amazing-feature`
3. Commit изменения: `git commit -m 'feat: add amazing feature'`
4. Push в ветку: `git push origin feature/amazing-feature`
5. Откройте Pull Request

---

## 📄 Лицензия

Этот проект распространяется под лицензией MIT. См. файл `LICENSE` для деталей.

---

## 👨‍💻 Автор

**Ваше Имя**

- GitHub: [@yourusername](https://github.com/yourusername)
- Email: your.email@example.com

---

## 🙏 Благодарности

- [Javalin](https://javalin.io/) — за отличный веб-фреймворк
- [Web Speech API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Speech_API) — за возможности распознавания речи
- Всем разработчикам открытых API

---

## 📸 Скриншоты

### Главный экран
![Main Screen](screenshots/main.png)

### Голосовое управление
![Voice Control](screenshots/voice.png)

### Continuous режим
![Continuous Mode](screenshots/continuous.png)

---

## 🔮 Планы развития

- [ ] Интеграция с ChatGPT API
- [ ] Поддержка английского языка
- [ ] Мобильное приложение
- [ ] Управление умным домом
- [ ] История диалогов
- [ ] Настройки голоса
- [ ] Темная/светлая тема
- [ ] Плагины и расширения

---

<div align="center">

**⭐ Если проект понравился, поставьте звезду! ⭐**

Made with ❤️ and ☕

</div>
