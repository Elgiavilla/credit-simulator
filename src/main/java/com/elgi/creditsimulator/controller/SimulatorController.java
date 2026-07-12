package com.elgi.creditsimulator.controller;

import com.elgi.creditsimulator.controller.command.ExitCommand;
import com.elgi.creditsimulator.controller.command.ShowCommand;
import com.elgi.creditsimulator.controller.command.SimulateCommand;
import com.elgi.creditsimulator.exception.CreditSimulatorException;
import com.elgi.creditsimulator.service.InstallmentCalculator;
import com.elgi.creditsimulator.validation.LoanValidator;
import com.elgi.creditsimulator.view.ConsoleView;
import com.elgi.creditsimulator.view.InstallmentPlanFormatter;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SimulatorController {

    private final ConsoleView view;
    private final CommandRegistry registry;

    public SimulatorController(ConsoleView view, CommandRegistry registry) {
        this.view = Objects.requireNonNull(view, "view");
        this.registry = Objects.requireNonNull(registry, "registry");

        if (registry.isEmpty()) {
            throw new IllegalArgumentException("A controller with no commands can do nothing.");
        }
    }

    public static SimulatorController createDefault(ConsoleView view, Clock clock) {
        Objects.requireNonNull(view, "view");

        LoanRequestReader reader = new LoanRequestReader(view);
        LoanValidator validator = LoanValidator.withDefaults(clock);
        InstallmentCalculator calculator = new InstallmentCalculator();
        InstallmentPlanFormatter formatter = new InstallmentPlanFormatter();

        CommandRegistry registry = new CommandRegistry();
        registry.register(new SimulateCommand(view, reader, validator, calculator, formatter));
        // 'load' arrives in M6, 'save' and 'switch' in M7. Each is one line here and the
        // 'show' menu picks them up for free.
        registry.register(new ShowCommand(registry, view));
        registry.register(new ExitCommand(view));

        return new SimulatorController(view, registry);
    }

    /** Runs until the user exits or the input ends. */
    public void run() {
        view.printBanner();

        while (true) {
            view.printBlankLine();
            Optional<String> line = view.readLine("> ");

            if (!line.isPresent()) {
                // End of input. A piped script that simply stops is not an error.
                return;
            }

            String word = line.get().trim();
            if (word.isEmpty()) {
                continue;
            }

            Optional<Command> command = registry.find(word);
            if (!command.isPresent()) {
                view.print("Unknown command: '" + word + "'. Type 'show' to see what is available.");
                continue;
            }

            if (dispatch(command.get()) == CommandOutcome.EXIT) {
                return;
            }
        }
    }

    private CommandOutcome dispatch(Command command) {
        try {
            return command.execute();
        } catch (CreditSimulatorException failure) {
            // A command that let one of ours escape. Report it and keep the session alive.
            view.printError(failure);
            return CommandOutcome.CONTINUE;
        }
    }

    public Set<String> commandNames() {
        return registry.names();
    }

}
