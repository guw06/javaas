package com.assistant.commands;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GoogleSearchCommand implements Command {
    @Override
    public String execute(String input) {
        try {
            String query = input.toLowerCase().replaceFirst(".*найди\\s+", "");
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            Desktop.getDesktop().browse(new URI("https://www.google.com/search?q=" + encodedQuery));
            return "Ищу информацию";
        } catch (Exception e) {
            return "Не удалось выполнить поиск: " + e.getMessage();
        }
    }
}
