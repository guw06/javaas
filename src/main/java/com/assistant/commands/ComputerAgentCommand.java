package com.assistant.commands;

import com.assistant.services.ComputerAgentService;

public class ComputerAgentCommand implements Command {
    private final ComputerAgentService agent = new ComputerAgentService();

    @Override
    public String execute(String input) {
        return agent.execute(input);
    }
}
