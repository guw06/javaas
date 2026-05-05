package com.assistant.commands;

import com.assistant.services.DatabaseService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TasksCommand implements Command {
    private final DatabaseService database;

    public TasksCommand(DatabaseService database) {
        this.database = database;
    }

    @Override
    public String execute(String input) {
        String text = normalize(input);

        if (containsAny(text, "покажи", "список", "какие", "что по задач")) {
            return listTasks();
        }

        if (containsAny(text, "готово", "выполнено", "закрой задачу", "отметь")) {
            int id = extractNumber(text);
            boolean completed = id > 0 ? database.completeTask(id) : database.completeFirstOpenTask();
            return completed
                ? "Отметила задачу выполненной."
                : "Не нашла открытую задачу с таким номером.";
        }

        String task = input.replaceFirst("(?iu)^.*?(добавь|создай|запиши|поставь)\\s+(задачу|дело|todo)?\\s*", "").trim();
        if (task.isBlank() || task.equals(input.trim())) {
            task = input.replaceFirst("(?iu)^.*?(задача|дело|todo)\\s*", "").trim();
        }

        if (task.isBlank()) {
            return "Скажи задачу. Например: добавь задачу закончить проект.";
        }

        int id = database.addTask(task);
        return id > 0
            ? "Добавила задачу #" + id + ": " + task
            : "Не смогла сохранить задачу.";
    }

    private String listTasks() {
        List<String> tasks = database.getTasks(false, 10);
        if (tasks.isEmpty()) {
            return "Открытых задач нет. Красиво, чисто.";
        }

        StringBuilder builder = new StringBuilder("Открытые задачи:\n");
        for (String task : tasks) {
            builder.append("- ").append(task).append("\n");
        }
        return builder.toString().trim();
    }

    private int extractNumber(String text) {
        Matcher matcher = Pattern.compile("\\d+").matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group()) : -1;
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
