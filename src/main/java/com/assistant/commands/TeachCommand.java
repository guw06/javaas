package com.assistant.commands;

import com.assistant.services.DatabaseService;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TeachCommand implements Command {
    private final DatabaseService database;

    public TeachCommand(DatabaseService database) {
        this.database = database;
    }

    @Override
    public String execute(String input) {
        String text = normalize(input);

        if (containsAny(text, "покажи", "список", "чему я тебя научил", "обученные")) {
            return showCommands();
        }

        ParsedCommand parsed = parse(input);
        if (parsed.phrase().isBlank() || parsed.action().isBlank()) {
            return """
                Научи меня так:
                когда я скажу "режим кодинга", открой браузер
                или:
                команда "домашка" = открой последний Word документ
                """.trim();
        }

        database.saveCustomCommand(parsed.phrase(), parsed.action());
        return "Запомнила. Когда ты скажешь «" + parsed.phrase() + "», я выполню: " + parsed.action();
    }

    private String showCommands() {
        Map<String, String> commands = database.getCustomCommands();
        if (commands.isEmpty()) {
            return "Обученных команд пока нет.";
        }

        StringBuilder builder = new StringBuilder("Обученные команды:\n");
        commands.forEach((phrase, action) -> builder.append("- «").append(phrase).append("» -> ").append(action).append("\n"));
        return builder.toString().trim();
    }

    private ParsedCommand parse(String input) {
        String source = input == null ? "" : input.trim();
        Matcher quoted = Pattern.compile("(?iu)(?:когда\\s+я\\s+скажу|команда|научи\\s+команду)\\s+[\"']([^\"']+)[\"']\\s*(?:,|=|->|то)?\\s*(.+)$").matcher(source);
        if (quoted.find()) {
            return new ParsedCommand(cleanup(quoted.group(1)), cleanup(quoted.group(2)));
        }

        Matcher plain = Pattern.compile("(?iu)когда\\s+я\\s+скажу\\s+(.+?)\\s+(?:то\\s+)?(открой|запусти|найди|покажи|создай|включи|поставь|сводка|погода|новости).+$").matcher(source);
        if (plain.find()) {
            String action = source.substring(plain.start(2)).trim();
            return new ParsedCommand(cleanup(plain.group(1)), cleanup(action));
        }

        return new ParsedCommand("", "");
    }

    private String cleanup(String value) {
        return value == null ? "" : value
            .replaceAll("(?iu)\\b(пожалуйста|теперь)\\b", " ")
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

    private record ParsedCommand(String phrase, String action) {
    }
}
