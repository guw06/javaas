package com.assistant.commands;

import com.assistant.utils.HttpClientUtil;
import com.google.gson.JsonObject;

public class JokeCommand implements Command {
    @Override
    public String execute(String input) {
        String response = HttpClientUtil.get("https://api.chucknorris.io/jokes/random");
        if (response != null && !response.isEmpty()) {
            JsonObject json = HttpClientUtil.gson.fromJson(response, JsonObject.class);
            return json.get("value").getAsString();
        }
        return "Не удалось получить шутку";
    }
}
