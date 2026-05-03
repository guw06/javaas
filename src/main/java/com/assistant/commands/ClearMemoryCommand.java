package com.assistant.commands;

import com.assistant.services.DatabaseService;

/**
 * Команда "очисти" — очищает заметки из памяти
 */
public class ClearMemoryCommand implements Command {
    private static DatabaseService database;
    
    public static void setDatabase(DatabaseService db) {
        database = db;
    }
    
    @Override
    public String execute(String input) {
        if (database == null) {
            return "Ошибка: база данных не инициализирована";
        }
        
        database.clearNotes();
        return "🗑️ Память очищена! Все заметки удалены.";
    }
}
