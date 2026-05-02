import io.javalin.Javalin;
import com.google.gson.Gson;
import com.assistant.models.RequestDto;
import com.assistant.models.ResponseDto;
import com.assistant.CommandManager;
import com.assistant.commands.*;
import com.assistant.services.SystemMonitorService;
import com.assistant.services.DatabaseService;

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
        // GeminiCommand больше не нужен - Gemini теперь встроен в CommandManager
        
        Gson gson = new Gson();
        
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(8080);

        app.get("/ping", ctx -> ctx.result("pong"));
        
        app.post("/api/command", ctx -> {
            RequestDto request = gson.fromJson(ctx.body(), RequestDto.class);
            String result = commandManager.process(request.getText());
            ResponseDto response = new ResponseDto(result);
            ctx.json(response);
        });
    }
}
