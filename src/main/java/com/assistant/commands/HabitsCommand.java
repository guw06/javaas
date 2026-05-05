package com.assistant.commands;

import com.assistant.services.DatabaseService;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HabitsCommand implements Command {
    private final DatabaseService database;

    public HabitsCommand(DatabaseService database) {
        this.database = database;
    }

    @Override
    public String execute(String input) {
        String text = normalize(input);

        if (containsAny(text, "покажи", "список", "какие привычки")) {
            return showHabits();
        }

        ParsedHabit habit = parse(input);
        if (habit.trigger().isBlank() || habit.action().isBlank()) {
            return """
                Привычку можно задать так:
                привычка "доброе утро" = сводка дня
                или:
                когда слышишь "начинаем учебу", включай режим учебы
                """.trim();
        }

        database.saveHabit(habit.trigger(), habit.action());
        return "Запомнила привычку: если услышу «" + habit.trigger() + "», предложу/выполню «" + habit.action() + "».";
    }

    private String showHabits() {
        Map<String, String> habits = database.getHabits();
        if (habits.isEmpty()) {
            return "Привычек пока нет. Можно создать: привычка \"доброе утро\" = сводка дня.";
        }

        StringBuilder builder = new StringBuilder("Мои привычки:\n");
        habits.forEach((trigger, action) -> builder.append("- «").append(trigger).append("» -> ").append(action).append("\n"));
        return builder.toString().trim();
    }

    private ParsedHabit parse(String input) {
        String source = input == null ? "" : input.trim();
        Matcher quoted = Pattern.compile("(?iu)(?:привычка|когда\\s+слышишь)\\s+[\"']([^\"']+)[\"']\\s*(?:=|,|то)?\\s*(.+)$").matcher(source);
        if (quoted.find()) {
            return new ParsedHabit(cleanup(quoted.group(1)), cleanup(quoted.group(2)));
        }
        return new ParsedHabit("", "");
    }

    private String cleanup(String value) {
        return value == null ? "" : value
            .replaceFirst("(?iu)^(включай|выполняй|делай|то)\\s+", "")
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

    private record ParsedHabit(String trigger, String action) {
    }
}
