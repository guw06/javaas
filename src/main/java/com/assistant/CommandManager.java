package com.assistant;

import com.assistant.commands.Command;
import com.assistant.services.DatabaseService;
import com.assistant.services.GeminiService;
import com.assistant.services.IntentRouterService;
import com.assistant.services.IntentRouterService.Intent;
import com.assistant.services.LocalBrainService;
import com.assistant.services.ProfileMemoryService;
import com.assistant.services.SmartAssistantService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CommandManager {
    private static final Set<String> STEM_PHRASES = Set.of("вкладк", "папк", "директор", "загрузк");

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

    private record CommandMatch(CommandEntry entry, String phrase, int score) {
    }

    private final List<CommandEntry> entries = new ArrayList<>();
    private final GeminiService geminiService;
    private final IntentRouterService intentRouterService;
    private final LocalBrainService localBrainService;
    private final SmartAssistantService smartAssistantService;
    private ProfileMemoryService profileMemoryService;
    private String lastUserInput = "";
    private String lastAssistantResponse = "";
    private String lastActionableInput = "";

    public CommandManager() {
        this.geminiService = new GeminiService();
        this.intentRouterService = new IntentRouterService();
        this.localBrainService = new LocalBrainService();
        this.smartAssistantService = new SmartAssistantService();
    }

    public void setDatabase(DatabaseService database) {
        if (database != null) {
            this.profileMemoryService = new ProfileMemoryService(database);
        }
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
        IntentRouterService.IntentDecision intent = intentRouterService.classify(input);
        System.out.printf("AURA intent: %s %.2f (%s)%n", intent.intent(), intent.confidence(), intent.reason());

        if (profileMemoryService != null) {
            Optional<String> profileResult = profileMemoryService.process(input);
            if (profileResult.isPresent()) {
                System.out.println("AURA profile memory matched");
                return profileResult.get();
            }
        }

        Optional<String> smartResult = smartAssistantService.process(input);
        if (smartResult.isPresent()) {
            System.out.println("AURA smart intent matched");
            return smartResult.get();
        }

        if (intent.intent() == Intent.KNOWLEDGE && intent.confidence() >= 0.70) {
            Optional<String> localBrainResult = localBrainService.process(input);
            if (localBrainResult.isPresent()) {
                System.out.println("AURA local brain matched");
                return localBrainResult.get();
            }
        }

        Optional<CommandMatch> commandMatch = findBestCommand(normalizedInput);
        if (commandMatch.isPresent()) {
            CommandMatch match = commandMatch.get();
            System.out.println("System command matched: " + match.phrase() + " -> " + match.entry().primaryKeyword);
            return match.entry().command.execute(input);
        }

        Optional<String> localBrainResult = localBrainService.process(input);
        if (localBrainResult.isPresent()) {
            System.out.println("AURA local brain matched");
            return localBrainResult.get();
        }

        try {
            String profileContext = profileMemoryService == null
                ? "Профиль пользователя недоступен."
                : profileMemoryService.buildContext();
            return geminiService.ask(smartAssistantService.buildFallbackPrompt(input, lastUserInput, lastAssistantResponse, profileContext));
        } catch (Exception e) {
            System.err.println("Gemini error: " + e.getMessage());
            return "Я не смогла разобрать запрос. Попробуйте сказать проще или напишите: помощь.";
        }
    }

    private Optional<CommandMatch> findBestCommand(String normalizedInput) {
        CommandMatch best = null;

        for (CommandEntry entry : entries) {
            List<String> phrases = new ArrayList<>();
            phrases.add(entry.primaryKeyword);
            phrases.addAll(entry.aliases);

            for (String phrase : phrases) {
                if (!matchesCommandPhrase(normalizedInput, entry.primaryKeyword, phrase)) {
                    continue;
                }

                int score = scorePhrase(phrase);
                if (best == null || score > best.score()) {
                    best = new CommandMatch(entry, phrase, score);
                }
            }
        }

        return Optional.ofNullable(best);
    }

    private boolean matchesCommandPhrase(String normalizedInput, String primaryKeyword, String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return false;
        }

        if ("привет".equals(primaryKeyword) && !isGreetingIntent(normalizedInput)) {
            return false;
        }

        if (phrase.contains(" ")) {
            return containsPhrase(normalizedInput, phrase);
        }

        if (STEM_PHRASES.contains(phrase)) {
            return hasTokenStartingWith(normalizedInput, phrase);
        }

        return hasExactToken(normalizedInput, phrase);
    }

    private int scorePhrase(String phrase) {
        int score = phrase.length();
        if (phrase.contains(" ")) {
            score += 30;
        }
        if (STEM_PHRASES.contains(phrase)) {
            score -= 5;
        }
        return score;
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

        if (profileMemoryService != null) {
            profileMemoryService.captureSilently(input);
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
            "посчитай", "реши", "запомни", "сделай", "переведи", "перевод");
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsPhrase(String text, String phrase) {
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private boolean hasExactToken(String text, String token) {
        for (String part : text.split("\\s+")) {
            if (part.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTokenStartingWith(String text, String prefix) {
        for (String part : text.split("\\s+")) {
            if (part.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGreetingIntent(String normalizedInput) {
        if (looksLikeLanguageRequest(normalizedInput)) {
            return false;
        }

        String[] tokens = normalizedInput.split("\\s+");
        if (tokens.length > 5) {
            return false;
        }

        return hasExactToken(normalizedInput, "привет")
            || hasExactToken(normalizedInput, "здравствуй")
            || hasExactToken(normalizedInput, "здравствуйте")
            || containsPhrase(normalizedInput, "добрый день")
            || containsPhrase(normalizedInput, "добрый вечер")
            || containsPhrase(normalizedInput, "доброе утро")
            || hasExactToken(normalizedInput, "хай")
            || hasExactToken(normalizedInput, "hello")
            || hasExactToken(normalizedInput, "hi")
            || hasExactToken(normalizedInput, "салам");
    }

    private boolean looksLikeLanguageRequest(String normalizedInput) {
        return containsAny(normalizedInput,
            "перевод", "переведи", "перевести", "как будет", "как сказать",
            "что значит", "что означает", "на япон", "на рус", "на англ", "на каз",
            "японский", "русский", "английский", "казахский", "казакша", "қазақ",
            "слово", "фраза"
        );
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
