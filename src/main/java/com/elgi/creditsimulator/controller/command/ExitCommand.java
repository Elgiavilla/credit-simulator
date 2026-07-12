package com.elgi.creditsimulator.controller.command;

import com.elgi.creditsimulator.controller.Command;
import com.elgi.creditsimulator.controller.CommandOutcome;
import com.elgi.creditsimulator.view.ConsoleView;

import java.util.Objects;

public class ExitCommand implements Command {

    private final ConsoleView view;

    public ExitCommand(ConsoleView view) {
        this.view = Objects.requireNonNull(view, "view");
    }

    @Override
    public String name() {
        return "exit";
    }

    @Override
    public String description() {
        return "Leave the credit simulator.";
    }

    @Override
    public CommandOutcome execute() {
        view.print("Terima kasih. Sampai jumpa!");
        return CommandOutcome.EXIT;
    }
}
