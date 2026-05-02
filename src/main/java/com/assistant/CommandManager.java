package com.assistant;

import com.assistant.commands.Command;
import com.assistant.services.GeminiService;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private Map<String, Command> commands = new HashMap<>();
    private GeminiService geminiService;

    public CommandManager() {
        this.geminiService = new GeminiService();
    }

    public void register(String keyword, Command command) {
        commands.put(keyword, command);
    }

    public String process(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Вы ничего не сказали";
        }
        
        String lowerInput = input.toLowerCase();
        
        // Сначала проверяем системные команды
        for (String keyword : commands.keySet()) {
            if (lowerInput.contains(keyword)) {
                System.out.println("Системная команда найдена: " + keyword);
                return commands.get(keyword).execute(input);
            }
        }
        
        // Если системная команда не найдена - отправляем в Gemini
        System.out.println("Системная команда не найдена, отправляем в Gemini: " + input);
        return geminiService.ask(input);
    }
}
