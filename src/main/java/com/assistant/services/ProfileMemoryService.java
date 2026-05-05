package com.assistant.services;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileMemoryService {
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "(?:меня\\s+зовут|мое\\s+имя|моё\\s+имя)\\s+([\\p{L}][\\p{L}\\s-]{1,40})",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "(?:зови\\s+меня|называй\\s+меня|обращайся\\s+ко\\s+мне\\s+как)\\s+([\\p{L}][\\p{L}\\s-]{1,60})",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern CITY_PATTERN = Pattern.compile(
        "(?:я\\s+живу\\s+в|живу\\s+в|я\\s+из|мой\\s+город)\\s+([\\p{L}][\\p{L}\\s-]{1,50})",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    private final DatabaseService database;

    public ProfileMemoryService(DatabaseService database) {
        this.database = database;
    }

    public Optional<String> process(String input) {
        String text = normalize(input);
        if (text.isBlank()) {
            return Optional.empty();
        }

        if (containsAny(text, "очисти профиль", "забудь профиль", "удали профиль")) {
            database.clearProfile();
            return Optional.of("Готово, очистила профиль. Заметки и история при этом остались.");
        }

        Optional<String> profileQuestion = answerProfileQuestion(text);
        if (profileQuestion.isPresent()) {
            return profileQuestion;
        }

        Optional<String> learned = learnFrom(input);
        return learned.map(value -> "Запомнила. " + value);
    }

    public void captureSilently(String input) {
        learnFrom(input);
    }

    public String buildContext() {
        Map<String, String> profile = database.getProfileValues();
        if (profile.isEmpty()) {
            return "Профиль пользователя пока пуст.";
        }

        StringBuilder builder = new StringBuilder("Профиль пользователя:\n");
        for (Map.Entry<String, String> entry : profile.entrySet()) {
            builder.append("- ")
                .append(label(entry.getKey()))
                .append(": ")
                .append(entry.getValue())
                .append('\n');
        }
        return builder.toString().trim();
    }

    private Optional<String> learnFrom(String input) {
        String normalized = normalize(input);
        Map<String, String> learned = new LinkedHashMap<>();

        Matcher addressMatcher = ADDRESS_PATTERN.matcher(normalized);
        if (addressMatcher.find()) {
            String value = cleanValue(addressMatcher.group(1));
            if (!value.isBlank()) {
                learned.put("preferred_address", value);
            }
        }

        Matcher nameMatcher = NAME_PATTERN.matcher(normalized);
        if (nameMatcher.find() && !normalized.contains("кто я")) {
            String value = cleanValue(nameMatcher.group(1));
            if (!value.isBlank() && value.length() <= 40 && !looksLikeCommandTail(value)) {
                learned.put("name", capitalize(value));
            }
        }

        Matcher cityMatcher = CITY_PATTERN.matcher(normalized);
        if (cityMatcher.find()) {
            String value = cleanValue(cityMatcher.group(1));
            if (!value.isBlank()) {
                learned.put("city", capitalize(value));
            }
        }

        if (containsAny(normalized, "говори со мной на казахском", "отвечай на казахском", "казахский язык")) {
            learned.put("language", "казахский");
        } else if (containsAny(normalized, "говори со мной на русском", "отвечай на русском", "русский язык")) {
            learned.put("language", "русский");
        } else if (containsAny(normalized, "говори со мной на английском", "отвечай на английском", "английский язык")) {
            learned.put("language", "английский");
        }

        if (containsAny(normalized, "отвечай коротко", "говори коротко", "короткие ответы")) {
            learned.put("answer_style", "коротко");
        } else if (containsAny(normalized, "объясняй подробно", "отвечай подробно", "подробные ответы")) {
            learned.put("answer_style", "подробно");
        }

        if (learned.isEmpty()) {
            return Optional.empty();
        }

        learned.forEach(database::saveProfileValue);
        return Optional.of(formatLearned(learned));
    }

    private Optional<String> answerProfileQuestion(String text) {
        if (containsAny(text, "что ты знаешь обо мне", "мой профиль", "покажи профиль", "что ты помнишь обо мне")) {
            Map<String, String> profile = database.getProfileValues();
            if (profile.isEmpty()) {
                return Optional.of("Пока почти ничего личного не знаю. Можешь сказать: «меня зовут ...», «я живу в ...», «отвечай коротко».");
            }

            StringBuilder builder = new StringBuilder("Вот что я помню о тебе:\n");
            profile.forEach((key, value) -> builder.append("- ").append(label(key)).append(": ").append(value).append('\n'));
            return Optional.of(builder.toString().trim());
        }

        if (containsAny(text, "как меня зовут", "помнишь мое имя", "помнишь моё имя")) {
            String name = database.getProfileValue("name");
            return Optional.of(name.isBlank() ? "Пока не знаю твоё имя. Скажи: «меня зовут ...»." : "Тебя зовут " + name + ".");
        }

        if (containsAny(text, "как ко мне обращаться", "как ты меня называешь")) {
            String address = database.getProfileValue("preferred_address");
            return Optional.of(address.isBlank() ? "Пока обращаюсь просто по-дружески. Можешь сказать: «называй меня ...»." : "Ты просил обращаться к тебе: " + address + ".");
        }

        if (containsAny(text, "где я живу", "мой город")) {
            String city = database.getProfileValue("city");
            return Optional.of(city.isBlank() ? "Пока не знаю твой город. Скажи: «я живу в ...»." : "Ты живёшь в городе " + city + ".");
        }

        return Optional.empty();
    }

    private String formatLearned(Map<String, String> learned) {
        if (learned.size() == 1) {
            Map.Entry<String, String> entry = learned.entrySet().iterator().next();
            return label(entry.getKey()) + ": " + entry.getValue() + ".";
        }

        StringBuilder builder = new StringBuilder("Обновила профиль: ");
        boolean first = true;
        for (Map.Entry<String, String> entry : learned.entrySet()) {
            if (!first) {
                builder.append("; ");
            }
            builder.append(label(entry.getKey())).append(": ").append(entry.getValue());
            first = false;
        }
        return builder.append('.').toString();
    }

    private String label(String key) {
        return switch (key) {
            case "name" -> "имя";
            case "preferred_address" -> "обращение";
            case "city" -> "город";
            case "language" -> "язык";
            case "answer_style" -> "стиль ответов";
            default -> key;
        };
    }

    private boolean looksLikeCommandTail(String value) {
        return containsAny(value, "запомни", "открой", "создай", "удали", "переведи", "поставь", "включи");
    }

    private String cleanValue(String value) {
        return value == null ? "" : value
            .replaceAll("\\b(и|а|но|пожалуйста|теперь|всегда)\\b.*$", "")
            .replaceAll("^['\"«»\\s]+|['\"«»\\s]+$", "")
            .trim();
    }

    private String capitalize(String value) {
        if (value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
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
                .replaceAll("[?!.,;:\"'()\\[\\]{}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
