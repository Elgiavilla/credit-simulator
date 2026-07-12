package com.elgi.creditsimulator.controller.command;

import com.elgi.creditsimulator.controller.Command;
import com.elgi.creditsimulator.controller.CommandOutcome;
import com.elgi.creditsimulator.controller.CommandRegistry;
import com.elgi.creditsimulator.view.ConsoleView;

import java.util.Objects;

public class ShowCommand implements Command {

    private final CommandRegistry registry;
    private final ConsoleView view;

    public ShowCommand(CommandRegistry registry, ConsoleView view) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.view = Objects.requireNonNull(view, "view");
    }

    @Override
    public String name() {
        return "show";
    }

    @Override
    public String description() {
        return "List every available command.";
    }

    @Override
    public CommandOutcome execute() {
        view.print("Available commands:");

        int widest = registry.all().stream()
                .mapToInt(command -> command.name().length())
                .max()
                .orElse(0);

        for (Command command : registry.all()) {
            view.print(String.format("  %-" + Math.max(widest, 1) + "s  %s",
                    command.name(), command.description()));
        }
        return CommandOutcome.CONTINUE;
    }
}
