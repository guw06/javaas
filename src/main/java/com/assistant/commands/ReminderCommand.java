package com.assistant.commands;

import com.assistant.services.ReminderService;

public class ReminderCommand implements Command {
    private final ReminderService reminderService;

    public ReminderCommand(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Override
    public String execute(String input) {
        return reminderService.handle(input);
    }
}
