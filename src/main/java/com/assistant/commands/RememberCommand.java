package com.assistant.commands;

import com.assistant.services.DatabaseService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RememberCommand implements Command {
    private static DatabaseService database;
    
    // Статический метод для установки DatabaseService из Main
    public static void setDatabase(DatabaseService db) {
        database = db;
    }
    
    @Override
    public String execute(String input) {
        if (database == null) {
            return "Ошибка: база данных не инициализирована";
        }
        
        String text = input.toLowerCase().replaceFirst(".*запомни\\s+", "");
        
        if (text.trim().isEmpty()) {
            return "Что мне запомнить?";
        }
        
        // Генерируем уникальный ключ с временной меткой
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String key = "note_" + System.currentTimeMillis();
        String valueWithTimestamp = "[" + timestamp + "] " + text;
        
        // Сохраняем в базу данных
        database.save(key, valueWithTimestamp);
        
        return "Я запомнил: " + text;
    }
}
