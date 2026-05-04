package com.assistant.commands;

import com.assistant.services.WindowsAutomationService;

public class TaskManagerCommand implements Command {
    private final WindowsAutomationService windows = new WindowsAutomationService();

    @Override
    public String execute(String input) {
        return windows.openTaskManager();
    }
}
