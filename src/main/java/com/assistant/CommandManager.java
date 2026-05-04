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
    private String lastUserInput = "";
    private String lastAssistantResponse = "";
    private String lastActionableInput = "";

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
        Optional<String> followUpResult = processFollowUp(normalizedInput);
        if (followUpResult.isPresent()) {
            String result = followUpResult.get();
            rememberConversation(input, result);
            return result;
        }

        String result = processCurrent(input, normalizedInput);
        rememberConversation(input, result);
        return result;
    }

    private String processCurrent(String input, String normalizedInput) {
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
            return geminiService.ask(smartAssistantService.buildFallbackPrompt(input, lastUserInput, lastAssistantResponse));
        } catch (Exception e) {
            System.err.println("Gemini error: " + e.getMessage());
            return "Я не смогла разобрать запрос. Попробуйте сказать проще или напишите: помощь.";
        }
    }

    private Optional<String> processFollowUp(String normalizedInput) {
        if (!isAffirmativeFollowUp(normalizedInput) || lastActionableInput.isBlank()) {
            return Optional.empty();
        }

        String previousResponse = normalizeText(lastAssistantResponse);
        boolean previousAskedToAdd = previousResponse.contains("подключ")
            || previousResponse.contains("добав")
            || previousResponse.contains("инструмент")
            || normalizedInput.contains("добав");

        if (previousAskedToAdd || containsAny(normalizedInput, "повтори", "снова", "еще раз", "еще")) {
            Optional<String> smartResult = smartAssistantService.process(lastActionableInput);
            String replayResult = smartResult.orElseGet(() -> processCurrent(lastActionableInput, normalizeText(lastActionableInput)));
            return Optional.of("Поняла, продолжаю прошлый запрос. " + replayResult);
        }

        return Optional.of("Я помню предыдущий запрос: «" + compact(lastActionableInput) + "». Если нужно выполнить его снова, скажи: повтори.");
    }

    private void rememberConversation(String input, String response) {
        lastUserInput = input == null ? "" : input.trim();
        lastAssistantResponse = response == null ? "" : response.trim();

        String normalizedInput = normalizeText(input);
        if (isActionLike(normalizedInput)) {
            lastActionableInput = input == null ? "" : input.trim();
        }
    }

    private boolean isAffirmativeFollowUp(String normalizedInput) {
        if (normalizedInput.length() > 60) {
            return false;
        }

        String padded = " " + normalizedInput + " ";
        return padded.contains(" да ")
            || padded.contains(" конечно ")
            || padded.contains(" хорошо ")
            || padded.contains(" ок ")
            || padded.contains(" окей ")
            || padded.contains(" давай ")
            || padded.contains(" согласен ")
            || padded.contains(" согласна ")
            || padded.contains(" добавляй ")
            || padded.contains(" продолжай ")
            || padded.contains(" сделай ");
    }

    private boolean isActionLike(String normalizedInput) {
        return containsAny(normalizedInput,
            "открой", "запусти", "включи", "поставь", "проиграй", "найди", "поищи",
            "создай", "удали", "перемести", "переименуй", "разложи", "агент",
            "посчитай", "реши", "запомни", "сделай");
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String compact(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 77) + "...";
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
