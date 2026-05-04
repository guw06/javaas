package com.assistant.commands;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class GoogleSearchCommand implements Command {
    @Override
    public String execute(String input) {
        try {
            String query = extractQuery(input);
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            Desktop.getDesktop().browse(new URI("https://www.google.com/search?q=" + encodedQuery));
            return "Ищу информацию: " + query;
        } catch (Exception e) {
            return "Не смогла открыть поиск. Проверь, пожалуйста, браузер.";
        }
    }

    private String extractQuery(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String query = input.trim();
        String lower = query.toLowerCase(Locale.ROOT);
        String[] markers = {"найди", "найти", "ищи", "поищи", "поиск", "загугли", "google", "search"};

        for (String marker : markers) {
            int index = lower.indexOf(marker);
            if (index >= 0) {
                query = query.substring(index + marker.length()).trim();
                break;
            }
        }

        return query.isBlank() ? input.trim() : query;
    }
}
