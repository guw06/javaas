import io.javalin.Javalin;
import com.google.gson.Gson;
import com.assistant.models.RequestDto;
import com.assistant.models.ResponseDto;
import com.assistant.CommandManager;
import com.assistant.commands.HelloCommand;
import com.assistant.commands.TimeCommand;
import com.assistant.commands.DateCommand;

public class Main {
    private static final CommandManager commandManager = new CommandManager();

    public static void main(String[] args) {
        commandManager.register("привет", new HelloCommand());
        commandManager.register("время", new TimeCommand());
        commandManager.register("дата", new DateCommand());
        
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
