package com.assistant.commands;

import com.assistant.services.WindowsAutomationService;

import java.util.Locale;

public class OpenNotepadCommand implements Command {
    private final WindowsAutomationService windows = new WindowsAutomationService();

    @Override
    public String execute(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        if (lower.contains("закрой") || lower.contains("закрыть") || lower.contains("останови") || lower.contains("close")) {
            return windows.closeProgram("закрой блокнот");
        }

        try {
            Runtime.getRuntime().exec("notepad.exe");
            return "Открываю Блокнот.";
        } catch (Exception e) {
            return "Не смогла открыть Блокнот.";
        }
    }
}
