package com.assistant.commands;

public class OpenNotepadCommand implements Command {
    @Override
    public String execute(String input) {
        try {
            Runtime.getRuntime().exec("notepad.exe");
            return "Блокнот запущен";
        } catch (Exception e) {
            return "Не удалось запустить блокнот: " + e.getMessage();
        }
    }
}
