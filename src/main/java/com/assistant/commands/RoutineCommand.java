package com.assistant.commands;

import com.assistant.services.DatabaseService;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutineCommand implements Command {
    private final DatabaseService database;

    public RoutineCommand(DatabaseService database) {
        this.database = database;
    }

    @Override
    public String execute(String input) {
        String text = normalize(input);

        if (containsAny(text, "покажи", "список", "какие сценарии", "режимы")) {
            return showRoutines();
        }

        ParsedRoutine parsed = parse(input);
        if (!parsed.name().isBlank() && !parsed.commands().isBlank()) {
            database.saveRoutine(parsed.name(), parsed.commands());
            return "Сценарий «" + parsed.name() + "» сохранен.";
        }

        if (containsAny(text, "учеб", "study")) {
            openUrl("https://docs.google.com/document/u/0/");
            openUrl("https://translate.google.com/");
            database.logAction("routine", "режим учебы");
            return "Включила режим учебы: открыла документы и переводчик.";
        }
        if (containsAny(text, "работ", "work", "кодинг", "программ")) {
            openUrl("https://github.com/");
            openUrl("https://calendar.google.com/");
            database.logAction("routine", "режим работы");
            return "Включила рабочий режим: открыла GitHub и календарь.";
        }
        if (containsAny(text, "отдых", "музык", "relax")) {
            openUrl("https://www.youtube.com/results?search_query=" + encode("relax music"));
            database.logAction("routine", "режим отдыха");
            return "Включила режим отдыха: открыла спокойную музыку на YouTube.";
        }
        if (containsAny(text, "утро", "morning")) {
            database.logAction("routine", "утренний режим");
            return new DailySummaryCommand(database).execute(input);
        }

        String customName = extractCustomName(text);
        if (!customName.isBlank()) {
            String commands = database.getRoutine(customName);
            if (!commands.isBlank()) {
                database.logAction("routine", customName);
                runSimpleCommands(commands);
                return "Запустила сценарий «" + customName + "». Команды: " + commands;
            }
        }

        return "Сценарии: режим учебы, режим работы, режим отдыха, утренний режим. Можно создать: сценарий \"кодинг\" = открой сайт github.com; открой калькулятор.";
    }

    private String showRoutines() {
        Map<String, String> routines = new LinkedHashMap<>();
        routines.put("учеба", "документы + переводчик");
        routines.put("работа", "GitHub + календарь");
        routines.put("отдых", "YouTube с музыкой");
        routines.putAll(database.getRoutines());

        StringBuilder builder = new StringBuilder("Доступные сценарии:\n");
        routines.forEach((name, commands) -> builder.append("- ").append(name).append(": ").append(commands).append("\n"));
        return builder.toString().trim();
    }

    private ParsedRoutine parse(String input) {
        Matcher matcher = Pattern.compile("(?iu)(?:создай\\s+)?(?:сценарий|режим)\\s+[\"']([^\"']+)[\"']\\s*(?:=|:|->)\\s*(.+)$").matcher(input == null ? "" : input.trim());
        if (matcher.find()) {
            return new ParsedRoutine(cleanup(matcher.group(1)), cleanup(matcher.group(2)));
        }
        return new ParsedRoutine("", "");
    }

    private String extractCustomName(String text) {
        String value = text
            .replaceFirst("(?iu)^.*?(запусти|включи|открой)?\\s*(сценарий|режим)\\s*", "")
            .replaceAll("\\s+", " ")
            .trim();
        return value.equals(text) ? "" : value;
    }

    private void runSimpleCommands(String commands) {
        for (String command : commands.split("[;\\n]+")) {
            String text = normalize(command);
            if (text.startsWith("открой сайт ")) {
                openUrl("https://" + text.replaceFirst("открой сайт\\s+", "").replaceFirst("^https?://", ""));
            } else if (text.startsWith("открой ")) {
                String target = text.replaceFirst("открой\\s+", "");
                if (target.contains(".")) {
                    openUrl("https://" + target.replaceFirst("^https?://", ""));
                }
            }
        }
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String cleanup(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
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

    private record ParsedRoutine(String name, String commands) {
    }
}
