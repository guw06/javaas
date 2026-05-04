package com.assistant.commands;

import com.assistant.services.WindowsAutomationService;

import java.util.Locale;

public class BrowserTabsCommand implements Command {
    private final WindowsAutomationService windows = new WindowsAutomationService();

    @Override
    public String execute(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);

        if (lower.contains("закрой") || lower.contains("закрыть") || lower.contains("close")) {
            return windows.browserCloseTab();
        }
        if (lower.contains("следующ") || lower.contains("next")) {
            return windows.browserNextTab();
        }
        if (lower.contains("предыдущ") || lower.contains("назад") || lower.contains("previous")) {
            return windows.browserPreviousTab();
        }
        if (lower.contains("обнов") || lower.contains("refresh") || lower.contains("reload")) {
            return windows.browserRefreshTab();
        }
        if (lower.contains("адрес") || lower.contains("address")) {
            return windows.browserFocusAddress();
        }
        if (hasTarget(lower)) {
            return windows.openBrowserTarget(input);
        }

        return windows.browserNewTab();
    }

    private boolean hasTarget(String lower) {
        return lower.contains("http")
            || lower.contains("www.")
            || lower.contains(".com")
            || lower.contains(".ru")
            || lower.contains(".org")
            || lower.contains(".net")
            || lower.contains(".kz")
            || lower.contains("сайт")
            || lower.contains("перейди")
            || lower.contains("найди")
            || lower.contains("поиск");
    }
}
