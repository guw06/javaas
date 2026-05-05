package com.assistant.commands;

import com.assistant.services.DatabaseService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DailySummaryCommand implements Command {
    private final DatabaseService database;

    public DailySummaryCommand(DatabaseService database) {
        this.database = database;
    }

    @Override
    public String execute(String input) {
        String city = database.getSetting("city", database.getProfileValue("city"));
        List<String> reminders = database.getUpcomingReminders(3);
        List<String> tasks = database.getTasks(false, 5);
        int openTasks = database.countOpenTasks();

        StringBuilder builder = new StringBuilder();
        builder.append("Сводка на ")
            .append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append(":\n");
        builder.append("- город: ").append(city == null || city.isBlank() ? "не задан" : city).append("\n");
        builder.append("- открытых задач: ").append(openTasks).append("\n");

        if (!tasks.isEmpty()) {
            builder.append("\nГлавные задачи:\n");
            tasks.forEach(task -> builder.append("- ").append(task).append("\n"));
        }

        if (!reminders.isEmpty()) {
            builder.append("\nБлижайшие напоминания:\n");
            reminders.forEach(reminder -> builder.append("- ").append(reminder).append("\n"));
        }

        builder.append("\nКоманды для утра: новости, погода, покажи задачи, проверь нейросеть.");
        return builder.toString().trim();
    }
}
