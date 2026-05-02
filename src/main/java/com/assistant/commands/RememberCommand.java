package com.assistant.commands;

import com.assistant.utils.MemoryUtil;

public class RememberCommand implements Command {
    @Override
    public String execute(String input) {
        String text = input.toLowerCase().replaceFirst(".*запомни\\s+", "");
        MemoryUtil.saveNote(text);
        return "Я запомнил";
    }
}
