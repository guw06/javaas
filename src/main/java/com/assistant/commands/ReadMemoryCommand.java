package com.assistant.commands;

import com.assistant.utils.MemoryUtil;

public class ReadMemoryCommand implements Command {
    @Override
    public String execute(String input) {
        return MemoryUtil.readAllNotes();
    }
}
