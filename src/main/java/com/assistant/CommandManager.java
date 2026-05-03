package com.assistant;

import com.assistant.commands.Command;
import com.assistant.services.GeminiService;
import java.util.*;

/**
 * Умный менеджер команд с поддержкой алиасов и синонимов.
 * Каждая команда может иметь множество ключевых слов для распознавания
 * естественной речи на русском языке.
 */
public class CommandManager {
    
    /**
     * Запись команды с основным ключевым словом и алиасами
     */
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

    public CommandManager() {
        this.geminiService = new GeminiService();
    }

    /**
     * Регистрирует команду с одним ключевым словом (обратная совместимость)
     */
    public void register(String keyword, Command command) {
        register(keyword, List.of(), command);
    }

    /**
     * Регистрирует команду с основным словом и списком алиасов/синонимов
     */
    public void register(String keyword, List<String> aliases, Command command) {
        entries.add(new CommandEntry(keyword.toLowerCase(), aliases, command));
    }

    /**
     * Возвращает количество зарегистрированных команд
     */
    public int getCommandCount() {
        return entries.size();
    }

    /**
     * Возвращает список зарегистрированных ключевых слов
     */
    public Set<String> getKeywords() {
        Set<String> keywords = new LinkedHashSet<>();
        for (CommandEntry entry : entries) {
            keywords.add(entry.primaryKeyword);
        }
        return keywords;
    }

    /**
     * Обрабатывает пользовательский ввод.
     * 
     * Порядок обработки:
     * 1. Точное совпадение с основным ключевым словом
     * 2. Совпадение по алиасам/синонимам
     * 3. Gemini AI как умный fallback
     */
    public String process(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Вы ничего не сказали";
        }
        
        String lowerInput = input.toLowerCase().trim();
        
        // Убираем лишние пробелы и знаки пунктуации для лучшего матчинга
        String normalizedInput = normalizeText(lowerInput);
        
        // Шаг 1: Проверяем основные ключевые слова
        for (CommandEntry entry : entries) {
            if (normalizedInput.contains(entry.primaryKeyword)) {
                System.out.println("✅ Системная команда (основное слово): " + entry.primaryKeyword);
                return entry.command.execute(input);
            }
        }
        
        // Шаг 2: Проверяем алиасы/синонимы
        for (CommandEntry entry : entries) {
            for (String alias : entry.aliases) {
                if (normalizedInput.contains(alias)) {
                    System.out.println("✅ Системная команда (алиас '" + alias + "' → " + entry.primaryKeyword + ")");
                    return entry.command.execute(input);
                }
            }
        }
        
        // Шаг 3: Отправляем в Gemini AI как умный fallback
        System.out.println("🤖 Системная команда не найдена, отправляем в Gemini: " + input);
        try {
            return geminiService.ask(input);
        } catch (Exception e) {
            System.err.println("❌ Ошибка Gemini: " + e.getMessage());
            return "Извините, не удалось обработать запрос. Попробуйте одну из команд: " +
                   "привет, время, дата, погода, шутка, факт, новости, валюта, " +
                   "найди, блокнот, запомни, вспомни, скриншот, буфер, статистика, помощь";
        }
    }
    
    /**
     * Нормализация текста для лучшего распознавания.
     * Убирает лишние пробелы, знаки препинания и приводит к нижнему регистру.
     */
    private String normalizeText(String text) {
        return text
            .replaceAll("[?!.,;:\"'()\\[\\]{}]", " ")  // Убираем пунктуацию
            .replaceAll("\\s+", " ")                      // Убираем дубли пробелов
            .trim();
    }
}
