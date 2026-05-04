package com.assistant;

import com.assistant.commands.Command;
import com.assistant.services.GeminiService;
import com.assistant.services.SmartAssistantService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CommandManager {
    private static class CommandEntry {
        final String primaryKeyword;
        final List<String> aliases;
        final Command command;

        CommandEntry(String primaryKeyword, List<String> aliases, Command command) {
            this.primaryKeyword = primaryKeyword;
            this.aliases = aliases;
            this.command = command;
        }
    }

    private final List<CommandEntry> entries = new ArrayList<>();
    private final GeminiService geminiService;
    private final SmartAssistantService smartAssistantService;

    public CommandManager() {
        this.geminiService = new GeminiService();
        this.smartAssistantService = new SmartAssistantService();
    }

    public void register(String keyword, Command command) {
        register(keyword, List.of(), command);
    }

    public void register(String keyword, List<String> aliases, Command command) {
        List<String> normalizedAliases = aliases.stream()
            .map(this::normalizeText)
            .toList();
        entries.add(new CommandEntry(normalizeText(keyword), normalizedAliases, command));
    }

    public int getCommandCount() {
        return entries.size();
    }

    public Set<String> getKeywords() {
        Set<String> keywords = new LinkedHashSet<>();
        for (CommandEntry entry : entries) {
            keywords.add(entry.primaryKeyword);
        }
        return keywords;
    }

    public String process(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Я слушаю. Напишите или скажите команду.";
        }

        String normalizedInput = normalizeText(input);

        Optional<String> smartResult = smartAssistantService.process(input);
        if (smartResult.isPresent()) {
            System.out.println("AURA smart intent matched");
            return smartResult.get();
        }

        for (CommandEntry entry : entries) {
            if (normalizedInput.contains(entry.primaryKeyword)) {
                System.out.println("System command matched: " + entry.primaryKeyword);
                return entry.command.execute(input);
            }
        }

        for (CommandEntry entry : entries) {
            for (String alias : entry.aliases) {
                if (normalizedInput.contains(alias)) {
                    System.out.println("System command alias matched: " + alias + " -> " + entry.primaryKeyword);
                    return entry.command.execute(input);
                }
            }
        }

        try {
            return geminiService.ask(smartAssistantService.buildFallbackPrompt(input));
        } catch (Exception e) {
            System.err.println("Gemini error: " + e.getMessage());
            return "Я не смогла разобрать запрос. Попробуйте сказать проще или напишите: помощь.";
        }
    }

    private String normalizeText(String text) {
        return text == null ? "" : text
            .toLowerCase()
            .replace('\u0451', '\u0435')
            .replaceAll("[?!.,;:\"'()\\[\\]{}]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
