package com.assistant.commands;

import com.assistant.services.GeminiService;

public class GeminiCommand implements Command {
    private final GeminiService geminiService;
    
    public GeminiCommand() {
        this.geminiService = new GeminiService();
    }
    
    @Override
    public String execute(String input) {
        String question = extractQuestion(input);
        
        if (question.isEmpty()) {
            return "Пожалуйста, задайте вопрос. Например: 'спроси что такое Java'";
        }
        
        return geminiService.ask(question);
    }

    private String extractQuestion(String input) {
        String lowerInput = input.toLowerCase();
        
        String[] triggers = {"спроси", "спрашивай", "задай вопрос", "нейросеть", "gemini", "джемини"};
        
        for (String trigger : triggers) {
            if (lowerInput.contains(trigger)) {
                int index = lowerInput.indexOf(trigger);
                String result = input.substring(index + trigger.length()).trim();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        
        return input.trim();
    }
}
