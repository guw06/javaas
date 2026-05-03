package com.assistant.commands;

import com.assistant.services.DatabaseService;
import java.util.List;

/**
 * Команда "история" — показывает историю диалогов из БД
 */
public class HistoryCommand implements Command {
    private static DatabaseService database;
    
    public static void setDatabase(DatabaseService db) {
        database = db;
    }
    
    @Override
    public String execute(String input) {
        if (database == null) {
            return "Ошибка: база данных не инициализирована";
        }
        
        List<String> history = database.getHistory(10);
        
        if (history == null || history.isEmpty()) {
            return "📖 История пуста. Начните общение!";
        }
        
        StringBuilder sb = new StringBuilder("📖 Последние диалоги:\n\n");
        for (int i = 0; i < history.size(); i++) {
            sb.append(history.get(i)).append("\n\n");
        }
        
        return sb.toString().trim();
    }
}
