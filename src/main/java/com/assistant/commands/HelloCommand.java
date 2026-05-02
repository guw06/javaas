package com.assistant.commands;

public class HelloCommand implements Command {
    @Override
    public String execute(String input) {
        return "Здравствуйте! Чем могу помочь?";
    }
}
