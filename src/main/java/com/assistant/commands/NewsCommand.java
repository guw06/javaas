package com.assistant.commands;

import com.assistant.services.NewsService;

public class NewsCommand implements Command {
    private final NewsService news = new NewsService();

    @Override
    public String execute(String input) {
        return news.getNews(input);
    }
}
