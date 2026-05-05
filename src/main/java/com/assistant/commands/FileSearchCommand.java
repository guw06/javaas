package com.assistant.commands;

import com.assistant.services.FileSearchService;

public class FileSearchCommand implements Command {
    private final FileSearchService fileSearchService = new FileSearchService();

    @Override
    public String execute(String input) {
        return fileSearchService.handle(input);
    }
}
