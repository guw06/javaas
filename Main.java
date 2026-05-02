import io.javalin.Javalin;
import com.google.gson.Gson;
import com.assistant.models.RequestDto;
import com.assistant.models.ResponseDto;

public class Main {
    public static void main(String[] args) {
        Gson gson = new Gson();
        
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(8080);

        app.get("/ping", ctx -> ctx.result("pong"));
        
        app.post("/api/command", ctx -> {
            RequestDto request = gson.fromJson(ctx.body(), RequestDto.class);
            ResponseDto response = new ResponseDto("Команда получена");
            ctx.json(response);
        });
    }
}
