import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import com.google.gson.Gson;
import com.assistant.models.RequestDto;
import com.assistant.models.ResponseDto;
import com.assistant.CommandManager;
import com.assistant.commands.*;
import com.assistant.services.SystemMonitorService;
import com.assistant.services.DatabaseService;
import com.assistant.services.ReminderService;
import com.assistant.services.SystemTrayService;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class Main {
    private static final CommandManager commandManager = new CommandManager();

    public static void main(String[] args) {
        DatabaseService database = new DatabaseService();
        commandManager.setDatabase(database);
        ReminderService reminderService = new ReminderService(database);
        SystemTrayService trayService = new SystemTrayService();
        
        RememberCommand.setDatabase(database);
        ReadMemoryCommand.setDatabase(database);
        HistoryCommand.setDatabase(database);
        
        SystemMonitorService systemMonitor = new SystemMonitorService();
        systemMonitor.startMonitoring();

        commandManager.register("переведи", List.of(
            "перевод", "перевести", "как будет", "как сказать", "что значит", "что означает",
            "translate", "translation", "на японском", "на русском", "на английском", "на казахском",
            "на казхсский", "казакша", "қазақша"
        ), new TranslationCommand());
        
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
            "какие новости", "расскажи новости", "покажи новости", "новостная сводка",
            "новости про", "новости о"
        ), new NewsCommand());

        commandManager.register("спорт", List.of(
            "счет матча", "счет игры", "результат матча", "как сыграли",
            "футбол", "баскетбол", "хоккей", "nba", "нба", "nhl", "нхл",
            "апл", "лига чемпионов", "real madrid", "реал", "барселона"
        ), new SportsScoreCommand());
        
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

        commandManager.register("программа", List.of(
            "приложение", "открой программу", "запусти программу", "закрой программу",
            "открой приложение", "запусти приложение", "закрой приложение",
            "калькулятор", "paint", "пэйнт", "проводник", "терминал", "cmd",
            "командная строка", "powershell", "chrome", "хром", "edge", "эдж"
        ), new ProgramControlCommand());

        commandManager.register("диспетчер задач", List.of(
            "task manager", "taskmgr", "открой диспетчер задач", "запусти диспетчер задач"
        ), new TaskManagerCommand());

        commandManager.register("вкладк", List.of(
            "новая вкладка", "открой вкладку", "закрой вкладку", "следующая вкладка",
            "предыдущая вкладка", "обнови вкладку", "адресная строка", "открой сайт",
            "перейди на сайт", "сайт", "browser tab"
        ), new BrowserTabsCommand());

        commandManager.register("wi-fi", List.of(
            "wifi", "вайфай", "вай-фай", "bluetooth", "блютуз", "блютус",
            "включи wi-fi", "выключи wi-fi", "включи bluetooth", "выключи bluetooth",
            "настройки wi-fi", "настройки bluetooth"
        ), new NetworkControlCommand());

        commandManager.register("файл", List.of(
            "файлы", "папк", "создай файл", "создай папку", "удали файл", "удали папку",
            "перемести файл", "перенеси файл", "перемести папку", "перенеси папку",
            "move file", "create file", "delete file"
        ), new FileControlCommand());

        commandManager.register("word", List.of(
            "ворд", "docx", "документ word", "вордовский документ", "создай документ",
            "создай word", "создай ворд", "подготовь документ", "сформируй документ"
        ), new WordDocumentCommand());

        commandManager.register("youtube", List.of(
            "ютуб", "ютьюб", "открой youtube", "включи песню", "поставь песню",
            "включи музыку", "поставь музыку", "включи трек", "включи клип"
        ), new YouTubeCommand());

        commandManager.register("агент", List.of(
            "agent", "agent mode", "ai agent", "компьютерный агент", "автоматизируй",
            "сделай сама", "самостоятельно", "найди все pdf", "переименуй по дате",
            "разложи загрузки", "организуй загрузки"
        ), new ComputerAgentCommand());

        commandManager.register("напомни", List.of(
            "напоминание", "напоминания", "reminder", "remind me", "покажи напоминания",
            "список напоминаний", "через минут", "через час", "завтра в", "сегодня в"
        ), new ReminderCommand(reminderService));

        commandManager.register("настройки", List.of(
            "покажи настройки", "ответь коротко", "отвечай коротко", "отвечай подробно",
            "общайся как человек", "по человечески", "поставь город", "мой город",
            "характер", "стиль общения", "режим общения"
        ), new AssistantSettingsCommand(database));

        commandManager.register("проверь ai", List.of(
            "проверь нейросеть", "диагностика ai", "диагностика ии", "проверь openai",
            "проверь gemini", "проверка ключа", "ai status", "статус нейросети"
        ), new AiDiagnosticsCommand());

        commandManager.register("задача", List.of(
            "задачи", "todo", "дело", "дела", "добавь задачу", "создай задачу",
            "покажи задачи", "список задач", "готово задача", "отметь задачу"
        ), new TasksCommand(database));

        commandManager.register("сценарий", List.of(
            "сценарии", "режим учебы", "режим работы", "режим отдыха", "режим кодинга",
            "утренний режим", "запусти сценарий", "включи сценарий", "покажи сценарии"
        ), new RoutineCommand(database));

        commandManager.register("привычка", List.of(
            "привычки", "покажи привычки", "когда слышишь", "запомни привычку"
        ), new HabitsCommand(database));

        commandManager.register("научи команду", List.of(
            "когда я скажу", "обучи команду", "обученная команда", "обученные команды",
            "покажи обученные команды", "команда"
        ), new TeachCommand(database));

        commandManager.register("поиск файлов", List.of(
            "найди файл", "найди документ", "найди последний", "найди последнюю",
            "семантический поиск", "поиск по файлам", "открой найденный файл",
            "открой последний документ", "открой последний word", "последний ворд"
        ), new FileSearchCommand());

        commandManager.register("сводка дня", List.of(
            "утренняя сводка", "мой день", "план дня", "что сегодня", "начни день",
            "ежедневная сводка"
        ), new DailySummaryCommand(database));

        commandManager.register("журнал действий", List.of(
            "что ты сегодня делала", "что ты сегодня сделала", "что ты делала", "что ты сделала", "лог действий", "логи действий",
            "покажи журнал", "покажи действия", "история действий"
        ), new ActionLogCommand(database));
        
        commandManager.register("запомни", List.of(
            "сохрани", "записи", "запиши", "добавь заметку",
            "сохрани заметку", "remember", "заметка", "записать",
            "не забудь", "save"
        ), new RememberCommand());
        
        commandManager.register("вспомни", List.of(
            "что помнишь", "что ты помнишь", "что запомнил",
            "покажи заметки", "заметки", "мои записи", "мои заметки",
            "что сохранено", "remind", "memories"
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

        commandManager.register("реши задачу", List.of(
            "решить задачу", "помоги решить", "объясни решение", "найди решение",
            "реши пример", "объясни задачу"
        ), new ProblemSolverCommand());
        
        commandManager.register("очисти", List.of(
            "очисти память", "удали заметки", "забудь все",
            "очистить", "сброс", "удалить", "clear", "reset",
            "забудь", "очисти заметки"
        ), new ClearMemoryCommand());
        
        Gson gson = new Gson();
        
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
            "║  ⚡ Java 21 | Gemini/OpenAI | Local Brain               ║\n" +
            "║  📝 Команд: " + commandManager.getCommandCount() + " | Алиасов: 150+                       ║\n" +
            "║  🔗 http://localhost:8080                                ║\n" +
            "╚═══════════════════════════════════════════════════════════╝\n"
        );

        openBrowser("http://localhost:8080");
        trayService.install("http://localhost:8080");

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

        app.get("/api/settings", ctx -> ctx.json(database.getSettings()));

        app.post("/api/settings", ctx -> {
            Map<?, ?> payload = gson.fromJson(ctx.body(), Map.class);
            if (payload != null) {
                payload.forEach((key, value) -> {
                    if (key != null && value != null && !String.valueOf(value).isBlank()) {
                        database.saveSetting(String.valueOf(key), String.valueOf(value));
                    }
                });
            }
            ctx.json(Map.of("settings", database.getSettings()));
        });

        app.get("/api/tasks", ctx -> ctx.json(Map.of("tasks", database.getTasks(false, 50))));

        app.get("/api/actions", ctx -> {
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(50);
            ctx.json(Map.of("actions", database.getActionLog(limit)));
        });

        app.get("/api/reminders/due", ctx -> ctx.json(Map.of("reminders", reminderService.pollDueReminders())));
        
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
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Завершение работы...");
            systemMonitor.stopMonitoring();
            trayService.remove();
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
