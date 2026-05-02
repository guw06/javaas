package com.assistant.services;

import java.sql.*;

public class DatabaseService {
    private static final String DB_URL = "jdbc:sqlite:jarvis.db";
    private Connection connection;
    
    public DatabaseService() {
        try {
            // Подключаемся к базе данных (создается автоматически если не существует)
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("✅ Подключение к базе данных установлено: jarvis.db");
            
            // Создаем таблицу memory если она не существует
            createTable();
            
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при подключении к базе данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Создает таблицу memory если она не существует
     */
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS memory (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "key_name TEXT UNIQUE NOT NULL, " +
                     "value_text TEXT NOT NULL)";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Таблица memory готова к использованию");
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при создании таблицы: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Сохраняет пару ключ-значение в базу данных
     * Если ключ уже существует, значение обновляется
     * 
     * @param key ключ для сохранения
     * @param value значение для сохранения
     */
    public void save(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            System.err.println("⚠️ Ключ не может быть пустым");
            return;
        }
        
        if (value == null) {
            value = "";
        }
        
        String sql = "INSERT OR REPLACE INTO memory (key_name, value_text) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key.trim());
            pstmt.setString(2, value);
            pstmt.executeUpdate();
            System.out.println("💾 Сохранено: " + key + " = " + value);
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при сохранении данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Получает значение по ключу из базы данных
     * 
     * @param key ключ для поиска
     * @return значение или null если ключ не найден
     */
    public String get(String key) {
        if (key == null || key.trim().isEmpty()) {
            System.err.println("⚠️ Ключ не может быть пустым");
            return null;
        }
        
        String sql = "SELECT value_text FROM memory WHERE key_name = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key.trim());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String value = rs.getString("value_text");
                System.out.println("📖 Прочитано: " + key + " = " + value);
                return value;
            } else {
                System.out.println("🔍 Ключ не найден: " + key);
                return null;
            }
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при чтении данных: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Удаляет запись по ключу
     * 
     * @param key ключ для удаления
     * @return true если запись была удалена
     */
    public boolean delete(String key) {
        if (key == null || key.trim().isEmpty()) {
            System.err.println("⚠️ Ключ не может быть пустым");
            return false;
        }
        
        String sql = "DELETE FROM memory WHERE key_name = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key.trim());
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("🗑️ Удалено: " + key);
                return true;
            } else {
                System.out.println("🔍 Ключ не найден для удаления: " + key);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при удалении данных: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Получает все записи из базы данных
     * 
     * @return строка со всеми записями
     */
    public String getAll() {
        String sql = "SELECT key_name, value_text FROM memory ORDER BY id";
        StringBuilder result = new StringBuilder();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                String key = rs.getString("key_name");
                String value = rs.getString("value_text");
                result.append(key).append(": ").append(value).append("\n");
                count++;
            }
            
            if (count == 0) {
                return "База данных пуста";
            }
            
            System.out.println("📚 Прочитано записей: " + count);
            return result.toString().trim();
            
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при чтении всех данных: " + e.getMessage());
            e.printStackTrace();
            return "Ошибка при чтении базы данных";
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
