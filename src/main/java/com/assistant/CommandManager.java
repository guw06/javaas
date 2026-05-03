package com.assistant;

import com.assistant.commands.Command;
import com.assistant.services.GeminiService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommandManager {
    private Map<String, Command> commands = new HashMap<>();
    private GeminiService geminiService;

    public CommandManager() {
        this.geminiService = new GeminiService();
    }

    public void register(String keyword, Command command) {
        commands.put(keyword, command);
    }

    /**
     * Возвращает количество зарегистрированных команд
     */
    public int getCommandCount() {
        return commands.size();
    }

    /**
     * Возвращает список зарегистрированных ключевых слов
     */
    public Set<String> getKeywords() {
        return commands.keySet();
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
        try {
            return geminiService.ask(input);
        } catch (Exception e) {
            return "Команда не распознана. Доступные команды: привет, время, дата, погода, шутка, факт, новости, валюта, браузер, найди, блокнот, запомни что, вспомни, скриншот, буфер, статистика";
        }
    }
}
