package com.assistant.commands;

import com.assistant.utils.HttpClientUtil;

public class WeatherCommand implements Command {
    @Override
    public String execute(String input) {
        String response = HttpClientUtil.get("https://wttr.in/Astana?format=3");
        if (response != null && !response.isEmpty()) {
            return response;
        }
        return "Не смогла узнать погоду. Проверь интернет и попробуй еще раз.";
    }
}
