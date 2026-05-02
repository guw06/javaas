package com.assistant.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static final String DB_URL = "jdbc:sqlite:jarvis.db";
    private Connection connection;
    
    public DatabaseService() {
        try {
            // Подключаемся к базе данных (создается автоматически если не существует)
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("✅ Подключение к базе данных установлено: jarvis.db");
            
            // Создаем таблицы если они не существуют
            createTables();
            
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при подключении к базе данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Создает таблицы memory и history если они не существуют
     */
    private void createTables() {
        // Таблица для заметок
        String createMemoryTable = "CREATE TABLE IF NOT EXISTS memory (" +
                                   "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                   "note TEXT NOT NULL)";
        
        // Таблица для истории диалогов
        String createHistoryTable = "CREATE TABLE IF NOT EXISTS history (" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                    "user_request TEXT NOT NULL, " +
                                    "ai_response TEXT NOT NULL, " +
                                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createMemoryTable);
            System.out.println("✅ Таблица memory готова к использованию");
            
            stmt.execute(createHistoryTable);
            System.out.println("✅ Таблица history готова к использованию");
            
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при создании таблиц: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Сохраняет заметку в таблицу memory
     * 
     * @param text текст заметки
     */
    public void saveNote(String text) {
        if (text == null || text.trim().isEmpty()) {
            System.err.println("⚠️ Текст заметки не может быть пустым");
            return;
        }
        
        String sql = "INSERT INTO memory (note) VALUES (?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, text);
            pstmt.executeUpdate();
            System.out.println("💾 Заметка сохранена: " + text);
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при сохранении заметки: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Получает все заметки из таблицы memory
     * 
     * @return список всех заметок
     */
    public List<String> getAllNotes() {
        List<String> notes = new ArrayList<>();
        String sql = "SELECT note FROM memory ORDER BY id";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String note = rs.getString("note");
                notes.add(note);
            }
            
            System.out.println("📚 Прочитано заметок: " + notes.size());
            
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при чтении заметок: " + e.getMessage());
            e.printStackTrace();
        }
        
        return notes;
    }
    
    /**
     * Сохраняет запись в историю диалогов
     * 
     * @param userRequest запрос пользователя
     * @param aiResponse ответ ассистента
     */
    public void saveHistory(String userRequest, String aiResponse) {
        if (userRequest == null || aiResponse == null) {
            System.err.println("⚠️ Запрос и ответ не могут быть null");
            return;
        }
        
        String sql = "INSERT INTO history (user_request, ai_response) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userRequest);
            pstmt.setString(2, aiResponse);
            pstmt.executeUpdate();
            System.out.println("📝 История сохранена");
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при сохранении истории: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Получает последние N записей из истории
     * 
     * @param limit количество записей
     * @return список записей истории
     */
    public List<String> getHistory(int limit) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT user_request, ai_response, timestamp FROM history " +
                     "ORDER BY id DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String userRequest = rs.getString("user_request");
                String aiResponse = rs.getString("ai_response");
                String timestamp = rs.getString("timestamp");
                
                String record = String.format("[%s]\nВы: %s\nАссистент: %s",
                                            timestamp, userRequest, aiResponse);
                history.add(record);
            }
            
            System.out.println("📖 Прочитано записей истории: " + history.size());
            
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при чтении истории: " + e.getMessage());
            e.printStackTrace();
        }
        
        return history;
    }
    
    /**
     * Удаляет все заметки из таблицы memory
     */
    public void clearNotes() {
        String sql = "DELETE FROM memory";
        
        try (Statement stmt = connection.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            System.out.println("🗑️ Удалено заметок: " + deleted);
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при удалении заметок: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Удаляет всю историю диалогов
     */
    public void clearHistory() {
        String sql = "DELETE FROM history";
        
        try (Statement stmt = connection.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            System.out.println("🗑️ Удалено записей истории: " + deleted);
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при удалении истории: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Закрывает соединение с базой данных
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Соединение с базой данных закрыто");
            }
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при закрытии соединения: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Проверяет, установлено ли соединение с базой данных
     * 
     * @return true если соединение активно
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
