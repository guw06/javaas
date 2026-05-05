package com.assistant.services;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class LocalBrainService {
    private final Map<String, String> knowledge = new LinkedHashMap<>();

    public LocalBrainService() {
        knowledge.put("java", "Java — язык программирования. На нём удобно делать настольные приложения, серверы, Android-часть и такие проекты, как AURA. В твоём проекте Java запускает backend, команды, базу SQLite и локальный сервер.");
        knowledge.put("maven", "Maven — сборщик Java-проектов. Он скачивает зависимости из `pom.xml`, компилирует код и собирает JAR-файл для запуска.");
        knowledge.put("api ключ", "API-ключ — секретная строка, по которой сервис понимает, что запрос идёт от твоего аккаунта. Его нельзя выкладывать в GitHub, скриншоты и общий доступ.");
        knowledge.put("api", "API — способ, по которому программы общаются друг с другом. Например, AURA отправляет вопрос в AI API и получает ответ.");
        knowledge.put("нейросеть", "Нейросеть — модель, которая учится на больших данных и умеет находить закономерности: отвечать на вопросы, переводить, писать текст, анализировать команды.");
        knowledge.put("искусственный интеллект", "Искусственный интеллект — набор технологий, которые позволяют программе понимать текст, принимать решения, искать ответы и выполнять задачи почти как помощник.");
        knowledge.put("sqlite", "SQLite — лёгкая база данных в одном файле. В AURA она хранит историю диалогов и заметки в `jarvis.db`.");
        knowledge.put("git", "Git — система контроля версий. Она помогает сохранять историю изменений проекта, делать коммиты и откатываться к рабочему состоянию.");
        knowledge.put("github", "GitHub — сервис для хранения Git-проектов онлайн, совместной разработки и публикации кода.");
        knowledge.put("wi fi", "Wi-Fi — беспроводная сеть для подключения к интернету или локальной сети.");
        knowledge.put("wifi", "Wi-Fi — беспроводная сеть для подключения к интернету или локальной сети.");
        knowledge.put("bluetooth", "Bluetooth — беспроводная связь на небольшом расстоянии: наушники, мышки, клавиатуры, телефоны и другие устройства.");
        knowledge.put("казахстан", "Казахстан — страна в Центральной Азии. Столица — Астана. Государственный язык — казахский, русский широко используется в общении.");
        knowledge.put("япония", "Япония — островное государство в Восточной Азии. Столица — Токио. Официальный язык — японский.");
    }

    public Optional<String> process(String input) {
        String text = normalize(input);
        if (text.isBlank()) {
            return Optional.empty();
        }

        Optional<String> identity = answerIdentity(text);
        if (identity.isPresent()) {
            return identity;
        }

        Optional<String> date = answerDate(text);
        if (date.isPresent()) {
            return date;
        }

        Optional<String> capital = answerCapital(text);
        if (capital.isPresent()) {
            return capital;
        }

        Optional<String> definition = answerDefinition(text);
        if (definition.isPresent()) {
            return definition;
        }

        Optional<String> advice = answerProjectAdvice(text);
        if (advice.isPresent()) {
            return advice;
        }

        return Optional.empty();
    }

    private Optional<String> answerIdentity(String text) {
        if (containsAny(text, "кто ты", "как тебя зовут", "ты кто", "что ты такое")) {
            return Optional.of("Я AURA, твоя персональная помощница. Могу отвечать на вопросы, переводить, считать, работать с файлами, открывать программы, браузер, YouTube, новости, спорт и помнить важные заметки.");
        }

        if (containsAny(text, "что ты умеешь", "твои возможности", "что можешь", "список возможностей")) {
            return Optional.of("""
                Я умею отвечать на обычные вопросы, переводить фразы, считать примеры, решать задачи, создавать Word-документы, искать новости и счёт матчей, открывать программы, управлять браузером, Wi-Fi/Bluetooth, файлами и работать в agent mode.
                Для подробного списка напиши: помощь.
                """.trim());
        }

        return Optional.empty();
    }

    private Optional<String> answerDate(String text) {
        if (containsAny(text, "какой сейчас год", "какой год", "текущий год")) {
            return Optional.of("Сейчас " + LocalDate.now().getYear() + " год.");
        }
        return Optional.empty();
    }

    private Optional<String> answerCapital(String text) {
        if (containsAny(text, "столица казахстана", "какая столица казахстана")) {
            return Optional.of("Столица Казахстана — Астана.");
        }
        if (containsAny(text, "столица японии", "какая столица японии")) {
            return Optional.of("Столица Японии — Токио.");
        }
        if (containsAny(text, "столица россии", "какая столица россии")) {
            return Optional.of("Столица России — Москва.");
        }
        if (containsAny(text, "столица сша", "какая столица сша", "столица америки")) {
            return Optional.of("Столица США — Вашингтон.");
        }
        return Optional.empty();
    }

    private Optional<String> answerDefinition(String text) {
        if (!looksLikeDefinitionQuestion(text)) {
            return Optional.empty();
        }

        for (Map.Entry<String, String> entry : knowledge.entrySet()) {
            if (text.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    private Optional<String> answerProjectAdvice(String text) {
        if (containsAny(text, "как улучшить ауру", "как улучшить aura", "как сделать ауру умнее", "как сделать ассистента умнее")) {
            return Optional.of("""
                Чтобы AURA стала умнее, нужны три слоя: быстрые локальные команды для действий, локальный мозг для частых вопросов и AI fallback для сложных рассуждений. Я уже двигаю проект именно туда: сначала намерение, потом инструмент, потом нейросеть, если своих знаний мало.
                """.trim());
        }

        if (containsAny(text, "почему не отвечает", "почему слабый ответ", "ai не отвечает", "нейросеть не отвечает")) {
            return Optional.of("Если AI отвечает слабо или не отвечает, обычно причина в ключе, лимите, модели или интернете. Локальные команды AURA работают без API, а сложные вопросы требуют рабочего Gemini или OpenAI ключа.");
        }

        return Optional.empty();
    }

    private boolean looksLikeDefinitionQuestion(String text) {
        return containsAny(text,
            "что такое", "что значит", "что означает", "объясни", "расскажи про",
            "для чего нужен", "зачем нужен", "что это"
        );
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String input) {
        return input == null
            ? ""
            : input.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replace("-", " ")
                .replaceAll("[?!.,;:\"'()\\[\\]{}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
