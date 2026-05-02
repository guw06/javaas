package com.assistant.commands;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeCommand implements Command {
    @Override
    public String execute(String input) {
        LocalTime now = LocalTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        return "Сейчас " + time;
    }
}
