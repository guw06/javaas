package com.assistant.commands;

import com.assistant.services.GeminiService;

public class ProblemSolverCommand implements Command {
    private final GeminiService geminiService = new GeminiService();

    @Override
    public String execute(String input) {
        String prompt = """
            Ты AURA, персональный ассистент. Реши задачу пользователя.
            Правила:
            - если это математика, покажи краткий ход решения и итог;
            - если данных не хватает, скажи каких именно;
            - отвечай по-русски, ясно и без лишней воды.

            Задача пользователя:
            """ + input;

        return geminiService.ask(prompt);
    }
}
