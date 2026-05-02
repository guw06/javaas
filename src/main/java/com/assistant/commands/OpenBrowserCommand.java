package com.assistant.commands;

import java.awt.Desktop;
import java.net.URI;

public class OpenBrowserCommand implements Command {
    @Override
    public String execute(String input) {
        try {
            Desktop.getDesktop().browse(new URI("https://google.com"));
            return "Браузер открыт";
        } catch (Exception e) {
            return "Не удалось открыть браузер: " + e.getMessage();
        }
    }
}
