package com.assistant.commands;

import com.assistant.services.DatabaseService;
import java.util.List;

public class ReadMemoryCommand implements Command {
    private static DatabaseService database;
    
    public static void setDatabase(DatabaseService db) {
        database = db;
    }
    
    @Override
    public String execute(String input) {
        if (database == null) {
            return "Ошибка: база данных не инициализирована";
        }
        
        List<String> notes = database.getAllNotes();
        
        if (notes == null || notes.isEmpty()) {
            return "Моя память пуста";
        }
        
        StringBuilder result = new StringBuilder("📝 Вот что я помню:\n\n");
        
        for (int i = 0; i < notes.size(); i++) {
            result.append(i + 1).append(". ").append(notes.get(i)).append("\n");
        }
        
        return result.toString().trim();
    }
}
