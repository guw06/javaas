package com.assistant.commands;

import com.assistant.services.SportsScoreService;

public class SportsScoreCommand implements Command {
    private final SportsScoreService sports = new SportsScoreService();

    @Override
    public String execute(String input) {
        return sports.getScores(input);
    }
}
