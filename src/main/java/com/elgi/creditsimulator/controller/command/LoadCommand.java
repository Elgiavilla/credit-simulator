package com.elgi.creditsimulator.controller.command;

import com.elgi.creditsimulator.controller.Command;
import com.elgi.creditsimulator.controller.CommandOutcome;
import com.elgi.creditsimulator.exception.CreditSimulatorException;
import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.remote.LoanApiClient;
import com.elgi.creditsimulator.view.ConsoleView;

import java.util.Objects;

public class LoadCommand implements Command {

    private final ConsoleView view;
    private final LoanApiClient client;
    private final SimulateCommand simulator;

    public LoadCommand(ConsoleView view, LoanApiClient client, SimulateCommand simulator) {
        this.view = Objects.requireNonNull(view, "view");
        this.client = Objects.requireNonNull(client, "client");
        this.simulator = Objects.requireNonNull(simulator, "simulator");
    }

    @Override
    public String name() {
        return "load";
    }

    @Override
    public String description() {
        return "Load an existing calculation from the web service and show its installments.";
    }

    @Override
    public CommandOutcome execute() {
        view.print("Loading existing calculation from " + client.endpoint() + " ...");

        try {
            LoanRequest request = client.loadExistingCalculation();
            simulator.simulate(request);

        } catch (CreditSimulatorException failure) {
            view.printBlankLine();
            view.printError(failure);
        }
        return CommandOutcome.CONTINUE;
    }
}
