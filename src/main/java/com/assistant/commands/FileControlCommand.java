package com.assistant.commands;

import com.assistant.services.FileAutomationService;

public class FileControlCommand implements Command {
    private final FileAutomationService files = new FileAutomationService();

    @Override
    public String execute(String input) {
        return files.execute(input);
    }
}
