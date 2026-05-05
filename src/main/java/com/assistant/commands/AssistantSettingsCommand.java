package com.assistant.commands;

import com.assistant.services.DatabaseService;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssistantSettingsCommand implements Command {
    private final DatabaseService database;

    public AssistantSettingsCommand(DatabaseService database) {
        this.database = database;
    }

    @Override
    public String execute(String input) {
        String text = normalize(input);

        if (containsAny(text, "покажи", "какие", "список") || text.equals("настройки")) {
            return showSettings();
        }

        if (containsAny(text, "коротко", "кратко")) {
            database.saveSetting("answer_style", "коротко");
            return "Хорошо, буду отвечать короче и без лишней воды.";
        }
        if (containsAny(text, "подробно", "объясняй")) {
            database.saveSetting("answer_style", "подробно");
            return "Приняла. Буду объяснять подробнее, когда это полезно.";
        }
        if (containsAny(text, "дружелюб", "по человечески", "как человек")) {
            database.saveSetting("personality", "живая, дружелюбная, спокойная");
            return "Готово. Буду звучать теплее и живее.";
        }
        if (containsAny(text, "строго", "серьезно")) {
            database.saveSetting("personality", "собранная, короткая, деловая");
            return "Поняла. Переключаюсь на более собранный стиль.";
        }

        String city = extractAfter(input, "(?iu)(мой\\s+город|город|я\\s+живу\\s+в)\\s+");
        if (!city.isBlank()) {
            database.saveSetting("city", city);
            database.saveProfileValue("city", city);
            return "Запомнила город: " + city + ". Буду учитывать его в сводках и погоде.";
        }

        String personality = extractQuoted(input);
        if (!personality.isBlank() && containsAny(text, "характер", "стиль", "режим общения")) {
            database.saveSetting("personality", personality);
            return "Запомнила стиль общения: " + personality + ".";
        }

        return """
            Настройки можно менять так:
            - отвечай коротко
            - отвечай подробно
            - общайся по-человечески
            - поставь город Алматы
            - покажи настройки
            """.trim();
    }

    private String showSettings() {
        Map<String, String> settings = database.getSettings();
        if (settings.isEmpty()) {
            return "Настройки пока почти стандартные: теплый стиль, короткие ответы, голос из браузера.";
        }

        StringBuilder builder = new StringBuilder("Текущие настройки AURA:\n");
        settings.forEach((key, value) -> builder.append("- ").append(label(key)).append(": ").append(value).append("\n"));
        return builder.toString().trim();
    }

    private String label(String key) {
        return switch (key) {
            case "answer_style" -> "стиль ответов";
            case "personality" -> "характер";
            case "city" -> "город";
            default -> key;
        };
    }

    private String extractAfter(String input, String regex) {
        Matcher matcher = Pattern.compile(regex + "(.+)$").matcher(input == null ? "" : input.trim());
        if (!matcher.find()) {
            return "";
        }
        return cleanupValue(matcher.group(matcher.groupCount()));
    }

    private String extractQuoted(String input) {
        Matcher matcher = Pattern.compile("\"([^\"]+)\"|'([^']+)'").matcher(input == null ? "" : input);
        if (!matcher.find()) {
            return "";
        }
        return cleanupValue(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
    }

    private String cleanupValue(String value) {
        return value == null ? "" : value
            .replaceAll("(?iu)\\b(пожалуйста|теперь|для aura|для ауры)\\b", " ")
            .replaceAll("\\s+", " ")
            .trim();
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
        return input == null ? "" : input.toLowerCase().replace('ё', 'е').replaceAll("\\s+", " ").trim();
    }
}
