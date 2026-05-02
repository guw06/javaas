package com.assistant.commands;

import com.assistant.utils.HttpClientUtil;
import com.google.gson.JsonObject;

public class CurrencyCommand implements Command {
    @Override
    public String execute(String input) {
        String response = HttpClientUtil.get("https://api.exchangerate-api.com/v4/latest/USD");
        if (response != null && !response.isEmpty()) {
            JsonObject json = HttpClientUtil.gson.fromJson(response, JsonObject.class);
            JsonObject rates = json.getAsJsonObject("rates");
            double rub = rates.get("RUB").getAsDouble();
            double eur = rates.get("EUR").getAsDouble();
            return String.format("Курс USD: RUB %.2f, EUR %.2f", rub, eur);
        }
        return "Не удалось получить курс валют";
    }
}
