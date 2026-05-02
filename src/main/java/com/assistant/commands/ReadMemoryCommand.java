package com.assistant.commands;

import com.assistant.services.DatabaseService;

public class ReadMemoryCommand implements Command {
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
        
        // Получаем все заметки из базы данных
        String allNotes = database.getAll();
        
        if (allNotes == null || allNotes.trim().isEmpty() || allNotes.equals("База данных пуста")) {
            return "Я ничего не помню. Скажите 'запомни что-то', чтобы я сохранил";
        }
        
        // Форматируем вывод
        StringBuilder result = new StringBuilder("📝 Вот что я помню:\n\n");
        
        String[] lines = allNotes.split("\n");
        int noteNumber = 1;
        
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            // Убираем ключ (note_xxxxx:) и оставляем только значение
            if (line.contains(": ")) {
                String value = line.substring(line.indexOf(": ") + 2);
                result.append(noteNumber).append(". ").append(value).append("\n");
                noteNumber++;
            }
        }
        
        return result.toString().trim();
    }
}
