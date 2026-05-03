import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import com.google.gson.Gson;
import com.assistant.models.RequestDto;
import com.assistant.models.ResponseDto;
import com.assistant.CommandManager;
import com.assistant.commands.*;
import com.assistant.services.SystemMonitorService;
import com.assistant.services.DatabaseService;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class Main {
    private static final CommandManager commandManager = new CommandManager();

    public static void main(String[] args) {
        // Инициализируем базу данных
        DatabaseService database = new DatabaseService();
        
        // Передаем DatabaseService в команды памяти
        RememberCommand.setDatabase(database);
        ReadMemoryCommand.setDatabase(database);
        
        // Запускаем мониторинг системы
        SystemMonitorService systemMonitor = new SystemMonitorService();
        systemMonitor.startMonitoring();

        // Регистрируем команды
        commandManager.register("привет", new HelloCommand());
        commandManager.register("время", new TimeCommand());
        commandManager.register("дата", new DateCommand());
        commandManager.register("погода", new WeatherCommand());
        commandManager.register("шутка", new JokeCommand());
        commandManager.register("факт", new CatFactCommand());
        commandManager.register("новости", new NewsCommand());
        commandManager.register("валюта", new CurrencyCommand());
        commandManager.register("браузер", new OpenBrowserCommand());
        commandManager.register("найди", new GoogleSearchCommand());
        commandManager.register("блокнот", new OpenNotepadCommand());
        commandManager.register("запомни", new RememberCommand());
        commandManager.register("вспомни", new ReadMemoryCommand());
        commandManager.register("скриншот", new ScreenshotCommand());
        commandManager.register("буфер", new ClipboardCommand());
        commandManager.register("статистика", new SystemStatsCommand());
        commandManager.register("система", new SystemStatsCommand());
        
        Gson gson = new Gson();
        
        // Создаем Javalin с обслуживанием статических файлов
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            // Обслуживаем frontend из папки frontend/
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.directory = "frontend";
                staticFileConfig.location = Location.EXTERNAL;
                staticFileConfig.hostedPath = "/";
            });
        }).start(8080);
        
        System.out.println("\n" +
            "╔═══════════════════════════════════════════════════════════╗\n" +
            "║  🤖 J.A.R.V.I.S. — Персональный ИИ-Ассистент           ║\n" +
            "║  🚀 Сервер запущен на порту 8080                        ║\n" +
            "║  ⚡ Java 21 | Virtual Threads | Gemini AI               ║\n" +
            "║  🔗 http://localhost:8080                                ║\n" +
            "╚═══════════════════════════════════════════════════════════╝\n"
        );

        // Автоматически открываем браузер
        openBrowser("http://localhost:8080");

        // === REST API ===

        // Health check
        app.get("/ping", ctx -> ctx.result("pong"));
        
        // Основная команда
        app.post("/api/command", ctx -> {
            RequestDto request = gson.fromJson(ctx.body(), RequestDto.class);
            String userText = request.getText();
            
            // Обрабатываем команду
            String result = commandManager.process(userText);
            
            // Сохраняем взаимодействие в историю
            database.logInteraction(userText, result);
            
            ResponseDto response = new ResponseDto(result);
            ctx.json(response);
        });
        
        // История диалогов
        app.get("/api/history", ctx -> {
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(50);
            List<String> history = database.getHistory(limit);
            ctx.json(Map.of("history", history));
        });
        
        // Статус системы
        app.get("/api/status", ctx -> {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            ctx.json(Map.of(
                "status", "online",
                "uptime", System.currentTimeMillis(),
                "memory", Map.of(
                    "used", usedMemory / (1024 * 1024),
                    "total", totalMemory / (1024 * 1024),
                    "max", maxMemory / (1024 * 1024),
                    "percentage", Math.round((double) usedMemory / totalMemory * 100)
                ),
                "processors", runtime.availableProcessors(),
                "javaVersion", System.getProperty("java.version"),
                "os", System.getProperty("os.name"),
                "dbConnected", database.isConnected(),
                "commands", commandManager.getCommandCount()
            ));
        });
        
        // Очистка памяти
        app.delete("/api/notes", ctx -> {
            database.clearNotes();
            ctx.json(Map.of("message", "Память очищена"));
        });
        
        // Очистка истории
        app.delete("/api/history", ctx -> {
            database.clearHistory();
            ctx.json(Map.of("message", "История очищена"));
        });
        
        // Graceful Shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Завершение работы...");
            systemMonitor.stopMonitoring();
            database.close();
            app.stop();
            System.out.println("✅ Все ресурсы освобождены. До свидания!");
        }));
    }
    
    /**
     * Открывает браузер с указанным URL
     */
    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("🌐 Браузер открыт: " + url);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Не удалось открыть браузер автоматически. Откройте вручную: " + url);
        }
    }
}
