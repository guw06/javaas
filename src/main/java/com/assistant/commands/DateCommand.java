package com.assistant.commands;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateCommand implements Command {
    @Override
    public String execute(String input) {
        LocalDate today = LocalDate.now();
        String date = today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        return "Сегодня " + date;
    }
}
