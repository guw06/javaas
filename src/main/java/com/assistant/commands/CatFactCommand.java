package com.assistant.commands;

import com.assistant.utils.HttpClientUtil;
import com.google.gson.JsonObject;

public class CatFactCommand implements Command {
    @Override
    public String execute(String input) {
        String response = HttpClientUtil.get("https://catfact.ninja/fact");
        if (response != null && !response.isEmpty()) {
            JsonObject json = HttpClientUtil.gson.fromJson(response, JsonObject.class);
            return json.get("fact").getAsString();
        }
        return "Не удалось получить факт о котах";
    }
}
