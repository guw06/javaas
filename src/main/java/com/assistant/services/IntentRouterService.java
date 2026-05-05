package com.assistant.services;

import java.util.Locale;

public class IntentRouterService {
    public enum Intent {
        PROFILE,
        REMINDER,
        TRANSLATION,
        COMPUTER_ACTION,
        MEDIA,
        MEMORY,
        KNOWLEDGE,
        MATH,
        CHAT
    }

    public record IntentDecision(Intent intent, double confidence, String reason) {
    }

    public IntentDecision classify(String input) {
        String text = normalize(input);
        if (text.isBlank()) {
            return new IntentDecision(Intent.CHAT, 0.1, "empty");
        }

        if (containsAny(text, "меня зовут", "мое имя", "моё имя", "зови меня", "мой профиль", "что ты знаешь обо мне", "как меня зовут", "я живу в")) {
            return new IntentDecision(Intent.PROFILE, 0.94, "profile phrase");
        }

        if (containsAny(text, "напомни", "напоминание", "напоминания", "через минут", "через час", "завтра в", "сегодня в")) {
            return new IntentDecision(Intent.REMINDER, 0.93, "reminder phrase");
        }

        if (containsAny(text, "переведи", "перевод", "как будет", "как сказать", "на япон", "на каз", "на рус", "на англ", "қазақша", "казакша")) {
            return new IntentDecision(Intent.TRANSLATION, 0.92, "translation phrase");
        }

        if (containsAny(text, "ютуб", "youtube", "песня", "трек", "музыку", "вруби", "поставь")) {
            return new IntentDecision(Intent.MEDIA, 0.84, "media phrase");
        }

        if (containsAny(text, "открой", "запусти", "закрой", "создай файл", "удали файл", "перемести", "wi fi", "wifi", "bluetooth", "диспетчер задач")) {
            return new IntentDecision(Intent.COMPUTER_ACTION, 0.86, "computer action phrase");
        }

        if (containsAny(text, "запомни", "сохрани заметку", "покажи заметки", "что помнишь", "история")) {
            return new IntentDecision(Intent.MEMORY, 0.82, "memory phrase");
        }

        if (containsAny(text, "посчитай", "вычисли", "сколько будет", "процент") || text.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*")) {
            return new IntentDecision(Intent.MATH, 0.80, "math phrase");
        }

        if (containsAny(text, "что такое", "что значит", "объясни", "расскажи про", "какая столица", "кто такой", "кто такая", "зачем нужен")) {
            return new IntentDecision(Intent.KNOWLEDGE, 0.78, "knowledge question");
        }

        return new IntentDecision(Intent.CHAT, 0.45, "fallback chat");
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
