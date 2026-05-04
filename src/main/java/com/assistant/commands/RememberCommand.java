package com.assistant.commands;

import com.assistant.services.DatabaseService;

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
        
        // Сохраняем заметку в базу данных
        database.saveNote(text);
        
        return "Запомнила: " + text;
    }
}
