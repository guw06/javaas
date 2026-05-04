package com.assistant.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static final String DB_URL = "jdbc:sqlite:jarvis.db";

    private Connection connection;

    public DatabaseService() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("Database ready: jarvis.db");
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
    }

    private void createTables() {
        String createMemoryTable = """
            CREATE TABLE IF NOT EXISTS memory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                note TEXT NOT NULL
            )
            """;

        String createHistoryTable = """
            CREATE TABLE IF NOT EXISTS history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_request TEXT NOT NULL,
                ai_response TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(createMemoryTable);
            statement.execute(createHistoryTable);
        } catch (SQLException e) {
            System.err.println("Database table error: " + e.getMessage());
        }
    }

    public void saveNote(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        String sql = "INSERT INTO memory (note) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, text.trim());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Note save error: " + e.getMessage());
        }
    }

    public List<String> getAllNotes() {
        List<String> notes = new ArrayList<>();
        String sql = "SELECT note FROM memory ORDER BY id";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                notes.add(resultSet.getString("note"));
            }
        } catch (SQLException e) {
            System.err.println("Note read error: " + e.getMessage());
        }

        return notes;
    }

    public void saveHistory(String userRequest, String aiResponse) {
        if (userRequest == null || aiResponse == null) {
            return;
        }

        String sql = "INSERT INTO history (user_request, ai_response) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userRequest);
            statement.setString(2, aiResponse);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("History save error: " + e.getMessage());
        }
    }

    public void logInteraction(String userRequest, String aiResponse) {
        saveHistory(userRequest, aiResponse);
    }

    public List<String> getHistory(int limit) {
        List<String> history = new ArrayList<>();
        String sql = """
            SELECT user_request, ai_response, timestamp
            FROM history
            ORDER BY id DESC
            LIMIT ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(String.format(
                        "[%s]%nВы: %s%nАссистент: %s",
                        resultSet.getString("timestamp"),
                        resultSet.getString("user_request"),
                        resultSet.getString("ai_response")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("History read error: " + e.getMessage());
        }

        return history;
    }

    public void clearNotes() {
        executeDelete("DELETE FROM memory", "Note clear error");
    }

    public void clearHistory() {
        executeDelete("DELETE FROM history", "History clear error");
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Database close error: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private void executeDelete(String sql, String errorPrefix) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println(errorPrefix + ": " + e.getMessage());
        }
    }
}
