package com.assistant;

import com.assistant.commands.Command;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private Map<String, Command> commands = new HashMap<>();

    public void register(String keyword, Command command) {
        commands.put(keyword, command);
    }

    public String process(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Вы ничего не сказали";
        }
        String lowerInput = input.toLowerCase();
        for (String keyword : commands.keySet()) {
            if (lowerInput.contains(keyword)) {
                return commands.get(keyword).execute(input);
            }
        }
        return "Я не понимаю эту команду";
    }
}
