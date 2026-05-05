package com.assistant.commands;

import com.assistant.services.GeminiService;

public class AiDiagnosticsCommand implements Command {
    private final GeminiService geminiService = new GeminiService();

    @Override
    public String execute(String input) {
        return geminiService.diagnose();
    }
}
