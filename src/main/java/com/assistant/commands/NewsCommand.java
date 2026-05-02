package com.assistant.commands;

import com.assistant.utils.HttpClientUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class NewsCommand implements Command {
    @Override
    public String execute(String input) {
        String response = HttpClientUtil.get("https://newsapi.org/v2/top-headlines?country=us&apiKey=demo");
        if (response != null && !response.isEmpty()) {
            JsonObject json = HttpClientUtil.gson.fromJson(response, JsonObject.class);
            JsonArray articles = json.getAsJsonArray("articles");
            if (articles != null && articles.size() > 0) {
                JsonObject article = articles.get(0).getAsJsonObject();
                String title = article.get("title").getAsString();
                return "Новость: " + title;
            }
        }
        return "Не удалось получить новости";
    }
}
