package com.assistant.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseService {
    private static final String DB_FILE = "aura.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Connection connection;

    public DatabaseService() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("Database ready: " + DB_FILE);
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

        String createProfileTable = """
            CREATE TABLE IF NOT EXISTS profile (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createRemindersTable = """
            CREATE TABLE IF NOT EXISTS reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text TEXT NOT NULL,
                remind_at TEXT NOT NULL,
                done INTEGER DEFAULT 0,
                delivered INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createSettingsTable = """
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createTasksTable = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text TEXT NOT NULL,
                done INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                completed_at DATETIME
            )
            """;

        String createRoutinesTable = """
            CREATE TABLE IF NOT EXISTS routines (
                name TEXT PRIMARY KEY,
                commands TEXT NOT NULL,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createHabitsTable = """
            CREATE TABLE IF NOT EXISTS habits (
                trigger TEXT PRIMARY KEY,
                action TEXT NOT NULL,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createCustomCommandsTable = """
            CREATE TABLE IF NOT EXISTS custom_commands (
                phrase TEXT PRIMARY KEY,
                action TEXT NOT NULL,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createActionLogTable = """
            CREATE TABLE IF NOT EXISTS action_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                action TEXT NOT NULL,
                detail TEXT,
                created_at DATETIME DEFAULT (datetime('now','localtime'))
            )
            """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(createMemoryTable);
            statement.execute(createHistoryTable);
            statement.execute(createProfileTable);
            statement.execute(createRemindersTable);
            statement.execute(createSettingsTable);
            statement.execute(createTasksTable);
            statement.execute(createRoutinesTable);
            statement.execute(createHabitsTable);
            statement.execute(createCustomCommandsTable);
            statement.execute(createActionLogTable);
        } catch (SQLException e) {
            System.err.println("Database table error: " + e.getMessage());
        }
    }

    public synchronized void saveNote(String text) {
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

    public synchronized List<String> getAllNotes() {
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

    public synchronized void saveHistory(String userRequest, String aiResponse) {
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

    public synchronized void logInteraction(String userRequest, String aiResponse) {
        saveHistory(userRequest, aiResponse);
    }

    public synchronized List<String> getHistory(int limit) {
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

    public synchronized void clearNotes() {
        executeDelete("DELETE FROM memory", "Note clear error");
    }

    public synchronized void clearHistory() {
        executeDelete("DELETE FROM history", "History clear error");
    }

    public synchronized void saveProfileValue(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return;
        }

        String sql = """
            INSERT INTO profile (key, value, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(key) DO UPDATE SET
                value = excluded.value,
                updated_at = CURRENT_TIMESTAMP
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.trim());
            statement.setString(2, value.trim());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Profile save error: " + e.getMessage());
        }
    }

    public synchronized String getProfileValue(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }

        String sql = "SELECT value FROM profile WHERE key = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("value") : "";
            }
        } catch (SQLException e) {
            System.err.println("Profile read error: " + e.getMessage());
            return "";
        }
    }

    public synchronized Map<String, String> getProfileValues() {
        Map<String, String> profile = new LinkedHashMap<>();
        String sql = "SELECT key, value FROM profile ORDER BY key";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                profile.put(resultSet.getString("key"), resultSet.getString("value"));
            }
        } catch (SQLException e) {
            System.err.println("Profile read error: " + e.getMessage());
        }

        return profile;
    }

    public synchronized void clearProfile() {
        executeDelete("DELETE FROM profile", "Profile clear error");
    }

    public synchronized void saveSetting(String key, String value) {
        upsertKeyValue("settings", "key", "value", key, value, "Settings save error");
    }

    public synchronized String getSetting(String key, String defaultValue) {
        String value = getKeyValue("settings", "key", "value", key, "Settings read error");
        return value.isBlank() ? defaultValue : value;
    }

    public synchronized Map<String, String> getSettings() {
        return getKeyValueMap("settings", "key", "value", "Settings read error");
    }

    public synchronized int addTask(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }

        String sql = "INSERT INTO tasks (text) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, text.trim());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            System.err.println("Task save error: " + e.getMessage());
            return -1;
        }
    }

    public synchronized List<String> getTasks(boolean includeDone, int limit) {
        List<String> tasks = new ArrayList<>();
        String sql = includeDone
            ? """
                SELECT id, text, done, created_at
                FROM tasks
                ORDER BY done, id DESC
                LIMIT ?
                """
            : """
                SELECT id, text, done, created_at
                FROM tasks
                WHERE done = 0
                ORDER BY id DESC
                LIMIT ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String marker = resultSet.getInt("done") == 1 ? "готово" : "в работе";
                    tasks.add(String.format("#%d — %s — %s",
                        resultSet.getInt("id"),
                        resultSet.getString("text"),
                        marker
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Task read error: " + e.getMessage());
        }

        return tasks;
    }

    public synchronized boolean completeTask(int id) {
        String sql = "UPDATE tasks SET done = 1, completed_at = CURRENT_TIMESTAMP WHERE id = ? AND done = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Task complete error: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean completeFirstOpenTask() {
        String sql = """
            SELECT id
            FROM tasks
            WHERE done = 0
            ORDER BY id
            LIMIT 1
            """;

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() && completeTask(resultSet.getInt("id"));
        } catch (SQLException e) {
            System.err.println("Task complete error: " + e.getMessage());
            return false;
        }
    }

    public synchronized int countOpenTasks() {
        String sql = "SELECT COUNT(*) AS total FROM tasks WHERE done = 0";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt("total") : 0;
        } catch (SQLException e) {
            System.err.println("Task count error: " + e.getMessage());
            return 0;
        }
    }

    public synchronized void saveRoutine(String name, String commands) {
        upsertKeyValue("routines", "name", "commands", name, commands, "Routine save error");
    }

    public synchronized String getRoutine(String name) {
        return getKeyValue("routines", "name", "commands", name, "Routine read error");
    }

    public synchronized Map<String, String> getRoutines() {
        return getKeyValueMap("routines", "name", "commands", "Routine read error");
    }

    public synchronized void saveHabit(String trigger, String action) {
        upsertKeyValue("habits", "trigger", "action", trigger, action, "Habit save error");
    }

    public synchronized Map<String, String> getHabits() {
        return getKeyValueMap("habits", "trigger", "action", "Habit read error");
    }

    public synchronized void saveCustomCommand(String phrase, String action) {
        upsertKeyValue("custom_commands", "phrase", "action", phrase, action, "Custom command save error");
    }

    public synchronized Map<String, String> getCustomCommands() {
        return getKeyValueMap("custom_commands", "phrase", "action", "Custom command read error");
    }

    public synchronized void logAction(String action, String detail) {
        if (action == null || action.isBlank()) {
            return;
        }

        String sql = "INSERT INTO action_log (action, detail, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, action.trim());
            statement.setString(2, detail == null ? "" : detail.trim());
            statement.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Action log error: " + e.getMessage());
        }
    }

    public synchronized List<String> getActionLog(int limit) {
        return readActionLog("""
            SELECT action, detail, created_at
            FROM action_log
            ORDER BY id DESC
            LIMIT ?
            """, limit);
    }

    public synchronized List<String> getTodayActionLog(int limit) {
        return readActionLog("""
            SELECT action, detail, created_at
            FROM action_log
            WHERE date(created_at, 'localtime') = ?
            ORDER BY id DESC
            LIMIT ?
            """, LocalDate.now().toString(), limit);
    }

    public synchronized int addReminder(String text, LocalDateTime remindAt) {
        if (text == null || text.isBlank() || remindAt == null) {
            return -1;
        }

        String sql = "INSERT INTO reminders (text, remind_at) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, text.trim());
            statement.setString(2, remindAt.format(DATE_TIME_FORMATTER));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            System.err.println("Reminder save error: " + e.getMessage());
            return -1;
        }
    }

    public synchronized List<String> getUpcomingReminders(int limit) {
        List<String> reminders = new ArrayList<>();
        String sql = """
            SELECT id, text, remind_at
            FROM reminders
            WHERE done = 0
            ORDER BY remind_at
            LIMIT ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reminders.add(String.format(
                        "#%d — %s — %s",
                        resultSet.getInt("id"),
                        resultSet.getString("text"),
                        formatStoredDateTime(resultSet.getString("remind_at"))
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Reminder read error: " + e.getMessage());
        }

        return reminders;
    }

    public synchronized List<String> getDueReminders(LocalDateTime now, boolean markDelivered) {
        List<String> reminders = new ArrayList<>();
        List<Integer> deliveredIds = new ArrayList<>();
        String sql = """
            SELECT id, text, remind_at
            FROM reminders
            WHERE done = 0 AND delivered = 0 AND remind_at <= ?
            ORDER BY remind_at
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, now.format(DATE_TIME_FORMATTER));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    reminders.add(String.format("Напоминаю: %s", resultSet.getString("text")));
                    if (markDelivered) {
                        deliveredIds.add(id);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Reminder read error: " + e.getMessage());
        }

        deliveredIds.forEach(this::markReminderDelivered);
        return reminders;
    }

    public synchronized void markReminderDone(int id) {
        updateReminderFlag(id, "done");
    }

    public synchronized void markReminderDelivered(int id) {
        updateReminderFlag(id, "delivered");
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Database close error: " + e.getMessage());
        }
    }

    public synchronized boolean isConnected() {
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

    private void updateReminderFlag(int id, String column) {
        if (!"done".equals(column) && !"delivered".equals(column)) {
            return;
        }

        String sql = "UPDATE reminders SET " + column + " = 1 WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Reminder update error: " + e.getMessage());
        }
    }

    private void upsertKeyValue(String table, String keyColumn, String valueColumn, String key, String value, String errorPrefix) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return;
        }

        String sql = """
            INSERT INTO %s (%s, %s, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(%s) DO UPDATE SET
                %s = excluded.%s,
                updated_at = CURRENT_TIMESTAMP
            """.formatted(table, keyColumn, valueColumn, keyColumn, valueColumn, valueColumn);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.trim());
            statement.setString(2, value.trim());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println(errorPrefix + ": " + e.getMessage());
        }
    }

    private String getKeyValue(String table, String keyColumn, String valueColumn, String key, String errorPrefix) {
        if (key == null || key.isBlank()) {
            return "";
        }

        String sql = "SELECT " + valueColumn + " FROM " + table + " WHERE " + keyColumn + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(valueColumn) : "";
            }
        } catch (SQLException e) {
            System.err.println(errorPrefix + ": " + e.getMessage());
            return "";
        }
    }

    private Map<String, String> getKeyValueMap(String table, String keyColumn, String valueColumn, String errorPrefix) {
        Map<String, String> values = new LinkedHashMap<>();
        String sql = "SELECT " + keyColumn + ", " + valueColumn + " FROM " + table + " ORDER BY " + keyColumn;

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                values.put(resultSet.getString(keyColumn), resultSet.getString(valueColumn));
            }
        } catch (SQLException e) {
            System.err.println(errorPrefix + ": " + e.getMessage());
        }

        return values;
    }

    private List<String> readActionLog(String sql, int limit) {
        List<String> logs = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(formatActionLogRow(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Action log read error: " + e.getMessage());
        }
        return logs;
    }

    private List<String> readActionLog(String sql, String date, int limit) {
        List<String> logs = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(formatActionLogRow(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Action log read error: " + e.getMessage());
        }
        return logs;
    }

    private String formatActionLogRow(ResultSet resultSet) throws SQLException {
        String detail = resultSet.getString("detail");
        String timestamp = resultSet.getString("created_at");
        if (detail == null || detail.isBlank()) {
            return timestamp + " — " + resultSet.getString("action");
        }
        return timestamp + " — " + resultSet.getString("action") + ": " + detail;
    }

    private String formatStoredDateTime(String value) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value, DATE_TIME_FORMATTER);
            return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        } catch (Exception e) {
            return value;
        }
    }
}
