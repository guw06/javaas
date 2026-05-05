package com.assistant.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReminderService {
    private static final Pattern RELATIVE_PATTERN = Pattern.compile(
        "через\\s+(\\d+)\\s*(секунд[уы]?|сек|минут[уы]?|мин|час[аов]?|дн(?:я|ей)?|день)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern DAY_TIME_PATTERN = Pattern.compile(
        "(сегодня|завтра|послезавтра)\\s+в\\s+(\\d{1,2})(?::(\\d{2}))?",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(?:^|\\s)в\\s+(\\d{1,2})(?::(\\d{2}))?(?=\\s|$)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    private final DatabaseService database;

    private record ReminderPlan(String text, LocalDateTime remindAt) {
    }

    public ReminderService(DatabaseService database) {
        this.database = database;
    }

    public String handle(String input) {
        String normalized = normalize(input);
        if (normalized.isBlank()) {
            return "Скажи, что и когда напомнить. Например: «напомни через 10 минут проверить чай».";
        }

        if (containsAny(normalized, "покажи напоминания", "список напоминаний", "какие напоминания", "мои напоминания")) {
            return listUpcoming();
        }

        if (containsAny(normalized, "очисти напоминания", "удали напоминания")) {
            return "Пока я не удаляю все напоминания одной командой, чтобы случайно не стереть важное. Можно добавить точечное удаление по номеру следующим шагом.";
        }

        Optional<ReminderPlan> plan = parse(input);
        if (plan.isEmpty()) {
            return """
                Я поняла, что нужно напоминание, но не увидела время.
                Примеры:
                - напомни через 10 минут проверить чай
                - напомни завтра в 18:00 сделать домашку
                - напомни сегодня в 21:30 лечь спать
                """.trim();
        }

        ReminderPlan reminder = plan.get();
        int id = database.addReminder(reminder.text(), reminder.remindAt());
        if (id < 0) {
            return "Не смогла сохранить напоминание. Проверь базу данных и попробуй ещё раз.";
        }

        return "Готово, напомню " + formatDateTime(reminder.remindAt()) + ": " + reminder.text() + ".";
    }

    public List<String> pollDueReminders() {
        return database.getDueReminders(LocalDateTime.now(), true);
    }

    private String listUpcoming() {
        List<String> reminders = database.getUpcomingReminders(10);
        if (reminders.isEmpty()) {
            return "Активных напоминаний нет.";
        }

        return "Ближайшие напоминания:\n" + String.join("\n", reminders);
    }

    private Optional<ReminderPlan> parse(String input) {
        String normalized = normalize(input);
        LocalDateTime now = LocalDateTime.now();

        Matcher relativeMatcher = RELATIVE_PATTERN.matcher(normalized);
        if (relativeMatcher.find()) {
            int amount = Integer.parseInt(relativeMatcher.group(1));
            String unit = relativeMatcher.group(2);
            LocalDateTime remindAt = addRelative(now, amount, unit);
            String text = cleanupReminderText(normalized, relativeMatcher.group());
            return text.isBlank() ? Optional.empty() : Optional.of(new ReminderPlan(text, remindAt));
        }

        Matcher dayTimeMatcher = DAY_TIME_PATTERN.matcher(normalized);
        if (dayTimeMatcher.find()) {
            LocalDate date = switch (dayTimeMatcher.group(1)) {
                case "завтра" -> LocalDate.now().plusDays(1);
                case "послезавтра" -> LocalDate.now().plusDays(2);
                default -> LocalDate.now();
            };
            LocalDateTime remindAt = LocalDateTime.of(date, parseTime(dayTimeMatcher.group(2), dayTimeMatcher.group(3)));
            String text = cleanupReminderText(normalized, dayTimeMatcher.group());
            return text.isBlank() ? Optional.empty() : Optional.of(new ReminderPlan(text, remindAt));
        }

        Matcher timeMatcher = TIME_PATTERN.matcher(normalized);
        if (timeMatcher.find()) {
            LocalDateTime remindAt = LocalDateTime.of(LocalDate.now(), parseTime(timeMatcher.group(1), timeMatcher.group(2)));
            if (remindAt.isBefore(now)) {
                remindAt = remindAt.plusDays(1);
            }

            String text = cleanupReminderText(normalized, timeMatcher.group());
            return text.isBlank() ? Optional.empty() : Optional.of(new ReminderPlan(text, remindAt));
        }

        return Optional.empty();
    }

    private LocalDateTime addRelative(LocalDateTime now, int amount, String unit) {
        if (unit.startsWith("сек")) {
            return now.plusSeconds(amount);
        }
        if (unit.startsWith("мин")) {
            return now.plusMinutes(amount);
        }
        if (unit.startsWith("час")) {
            return now.plusHours(amount);
        }
        return now.plusDays(amount);
    }

    private LocalTime parseTime(String hourText, String minuteText) {
        int hour = Math.max(0, Math.min(23, Integer.parseInt(hourText)));
        int minute = minuteText == null || minuteText.isBlank() ? 0 : Math.max(0, Math.min(59, Integer.parseInt(minuteText)));
        return LocalTime.of(hour, minute);
    }

    private String cleanupReminderText(String normalizedInput, String timePhrase) {
        return normalizedInput
            .replace(timePhrase, " ")
            .replaceAll("\\b(аура|напомни|напомнить|напоминание|мне|пожалуйста|что|о том что|про)\\b", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String formatDateTime(LocalDateTime value) {
        return value.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
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
