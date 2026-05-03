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
        HistoryCommand.setDatabase(database);
        
        // Запускаем мониторинг системы
        SystemMonitorService systemMonitor = new SystemMonitorService();
        systemMonitor.startMonitoring();

        // ═══════════════════════════════════════════════════
        // Регистрация команд с алиасами (синонимами)
        // Теперь ассистент понимает естественную речь!
        // ═══════════════════════════════════════════════════
        
        commandManager.register("привет", List.of(
            "здравствуй", "здравствуйте", "добрый день", "добрый вечер",
            "доброе утро", "добрая ночь", "хай", "хеллоу", "hello", "hi",
            "салам", "приветствую", "здарова", "хей", "йо"
        ), new HelloCommand());
        
        commandManager.register("время", List.of(
            "который час", "сколько время", "сколько времени", "какое время",
            "текущее время", "часы", "подскажи время", "скажи время",
            "скажи который час", "what time"
        ), new TimeCommand());
        
        commandManager.register("дата", List.of(
            "какая дата", "какое сегодня число", "число", "какой день",
            "сегодняшняя дата", "какое число", "день недели", "сегодня",
            "подскажи дату", "скажи дату", "what date"
        ), new DateCommand());
        
        commandManager.register("погода", List.of(
            "какая погода", "прогноз погоды", "прогноз", "температура",
            "сколько градусов", "на улице", "за окном", "weather",
            "погоду", "погоды", "тепло", "холодно", "какая температура"
        ), new WeatherCommand());
        
        commandManager.register("шутка", List.of(
            "расскажи шутку", "анекдот", "рассмеши", "смешное",
            "пошути", "расскажи анекдот", "joke", "рассмеши меня",
            "что-нибудь смешное", "юмор", "прикол"
        ), new JokeCommand());
        
        commandManager.register("факт", List.of(
            "расскажи факт", "интересный факт", "факты",
            "расскажи что-нибудь", "расскажи интересное", "fun fact",
            "факт о котах", "что нибудь интересное", "удиви меня"
        ), new CatFactCommand());
        
        commandManager.register("новости", List.of(
            "последние новости", "что нового", "новость", "news",
            "что в мире", "что происходит", "свежие новости",
            "какие новости", "расскажи новости", "покажи новости"
        ), new NewsCommand());
        
        commandManager.register("валюта", List.of(
            "курс", "курс доллара", "курс валют", "доллар", "евро",
            "обменный курс", "currency", "тенге", "рубль",
            "сколько стоит доллар", "курс валюты", "exchange rate"
        ), new CurrencyCommand());
        
        commandManager.register("браузер", List.of(
            "открой браузер", "открой интернет", "интернет", "гугл",
            "хром", "chrome", "browser", "открой гугл", "запусти браузер",
            "открой google", "открой chrome"
        ), new OpenBrowserCommand());
        
        commandManager.register("найди", List.of(
            "загугли", "поищи", "поиск", "search", "google",
            "найти", "ищи", "погугли", "покажи"
        ), new GoogleSearchCommand());
        
        commandManager.register("блокнот", List.of(
            "открой блокнот", "notepad", "открой notepad",
            "текстовый редактор", "запусти блокнот", "открой редактор"
        ), new OpenNotepadCommand());
        
        commandManager.register("запомни", List.of(
            "сохрани", "записи", "запиши", "добавь заметку",
            "сохрани заметку", "remember", "заметка", "записать",
            "не забудь", "save"
        ), new RememberCommand());
        
        commandManager.register("вспомни", List.of(
            "что помнишь", "что ты помнишь", "что запомнил",
            "покажи заметки", "заметки", "мои записи", "мои заметки",
            "что сохранено", "напомни", "remind", "memories"
        ), new ReadMemoryCommand());
        
        commandManager.register("скриншот", List.of(
            "сделай скриншот", "screenshot", "снимок экрана",
            "скрин", "сфоткай экран", "захват экрана", "скриншотик",
            "сделай снимок", "сделай скрин"
        ), new ScreenshotCommand());
        
        commandManager.register("буфер", List.of(
            "буфер обмена", "clipboard", "что в буфере",
            "что скопировано", "покажи буфер", "прочитай буфер",
            "содержимое буфера", "что я скопировал"
        ), new ClipboardCommand());
        
        commandManager.register("статистика", List.of(
            "система", "системная информация", "system", "stats",
            "ресурсы", "память", "процессор", "загрузка",
            "покажи статистику", "информация о системе", "системные ресурсы",
            "сколько памяти", "сколько оперативки", "cpu"
        ), new SystemStatsCommand());
        
        commandManager.register("помощь", List.of(
            "помоги", "help", "что умеешь", "что ты умеешь",
            "команды", "список команд", "подсказка", "справка",
            "что ты можешь", "твои возможности", "возможности",
            "как пользоваться", "инструкция", "меню"
        ), new HelpCommand());
        
        commandManager.register("история", List.of(
            "покажи историю", "история диалогов", "история чата",
            "предыдущие запросы", "что спрашивал", "history",
            "лог", "логи", "покажи лог"
        ), new HistoryCommand());
        
        commandManager.register("посчитай", List.of(
            "калькулятор", "вычисли", "сколько будет", "calculate",
            "calc", "математика", "math", "считай", "calculator",
            "посчитать", "рассчитай"
        ), new CalculatorCommand());
        
        commandManager.register("очисти", List.of(
            "очисти память", "удали заметки", "забудь все",
            "очистить", "сброс", "удалить", "clear", "reset",
            "забудь", "очисти заметки"
        ), new ClearMemoryCommand());
        
        Gson gson = new Gson();
        
        // Создаем Javalin с обслуживанием статических файлов
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
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
            "║  📝 Команд: " + commandManager.getCommandCount() + " | Алиасов: 150+                       ║\n" +
            "║  🔗 http://localhost:8080                                ║\n" +
            "╚═══════════════════════════════════════════════════════════╝\n"
        );

        // Автоматически открываем браузер
        openBrowser("http://localhost:8080");

        // === REST API ===
        app.get("/ping", ctx -> ctx.result("pong"));
        
        app.post("/api/command", ctx -> {
            RequestDto request = gson.fromJson(ctx.body(), RequestDto.class);
            String userText = request.getText();
            String result = commandManager.process(userText);
            database.logInteraction(userText, result);
            ResponseDto response = new ResponseDto(result);
            ctx.json(response);
        });
        
        app.get("/api/history", ctx -> {
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(50);
            List<String> history = database.getHistory(limit);
            ctx.json(Map.of("history", history));
        });
        
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
        
        app.delete("/api/notes", ctx -> {
            database.clearNotes();
            ctx.json(Map.of("message", "Память очищена"));
        });
        
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
    
    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("🌐 Браузер открыт: " + url);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Не удалось открыть браузер: " + url);
        }
    }
}
