package com.assistant.commands;

import com.assistant.services.WindowsAutomationService;

import java.util.Locale;

public class OpenBrowserCommand implements Command {
    private final WindowsAutomationService windows = new WindowsAutomationService();

    @Override
    public String execute(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);

        if (lower.contains("закрой") || lower.contains("закрыть") || lower.contains("останови") || lower.contains("close")) {
            if (lower.contains("edge") || lower.contains("эдж")) {
                return windows.closeProgram("закрой edge");
            }
            if (lower.contains("chrome") || lower.contains("хром")) {
                return windows.closeProgram("закрой chrome");
            }
            return "Уточните браузер: закрой chrome или закрой edge.";
        }

        return windows.openBrowserTarget(input);
    }
}
