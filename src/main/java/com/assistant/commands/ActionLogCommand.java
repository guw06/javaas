package com.assistant.commands;

import com.assistant.services.DatabaseService;

import java.util.List;

public class ActionLogCommand implements Command {
    private final DatabaseService database;

    public ActionLogCommand(DatabaseService database) {
        this.database = database;
    }

    @Override
    public String execute(String input) {
        String text = normalize(input);
        List<String> actions = containsAny(text, "сегодня", "день")
            ? database.getTodayActionLog(12)
            : database.getActionLog(12);

        if (actions.isEmpty()) {
            return "В журнале пока пусто. Я начну записывать важные действия с этого запуска.";
        }

        StringBuilder builder = new StringBuilder("Вот что я делала:\n");
        for (String action : actions) {
            builder.append("- ").append(action).append("\n");
        }
        return builder.toString().trim();
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
