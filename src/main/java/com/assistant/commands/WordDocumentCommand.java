package com.assistant.commands;

import com.assistant.services.WordDocumentService;

public class WordDocumentCommand implements Command {
    private final WordDocumentService word = new WordDocumentService();

    @Override
    public String execute(String input) {
        return word.createFromCommand(input);
    }
}
