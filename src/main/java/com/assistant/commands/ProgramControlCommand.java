package com.assistant.commands;

import com.assistant.services.WindowsAutomationService;

import java.util.Locale;

public class ProgramControlCommand implements Command {
    private final WindowsAutomationService windows = new WindowsAutomationService();

    @Override
    public String execute(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);

        if (lower.contains("закрой") || lower.contains("закрыть") || lower.contains("останови") || lower.contains("close")) {
            return windows.closeProgram(input);
        }

        return windows.openProgram(input);
    }
}
