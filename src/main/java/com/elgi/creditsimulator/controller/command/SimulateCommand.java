package com.elgi.creditsimulator.controller.command;

import com.elgi.creditsimulator.controller.Command;
import com.elgi.creditsimulator.controller.CommandOutcome;
import com.elgi.creditsimulator.controller.LoanRequestReader;
import com.elgi.creditsimulator.exception.CreditSimulatorException;
import com.elgi.creditsimulator.model.InstallmentPlan;
import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.service.InstallmentCalculator;
import com.elgi.creditsimulator.session.SheetManager;
import com.elgi.creditsimulator.validation.LoanValidator;
import com.elgi.creditsimulator.view.ConsoleView;
import com.elgi.creditsimulator.view.InstallmentPlanFormatter;

import java.util.Objects;
import java.util.Optional;

public class SimulateCommand implements Command {

    private final ConsoleView view;
    private final LoanRequestReader reader;
    private final LoanValidator validator;
    private final InstallmentCalculator calculator;
    private final InstallmentPlanFormatter formatter;

    public SimulateCommand(
            ConsoleView view,
            LoanRequestReader reader,
            LoanValidator validator,
            InstallmentCalculator calculator,
            InstallmentPlanFormatter formatter) {

        this.view = Objects.requireNonNull(view, "view");
        this.reader = Objects.requireNonNull(reader, "reader");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    @Override
    public String name() {
        return "calculate";
    }

    @Override
    public String description() {
        return "Simulate a new vehicle loan and show the monthly installments.";
    }

    @Override
    public CommandOutcome execute() {
        Optional<LoanRequest> request = reader.read();
        if (!request.isPresent()) {
            // Input ended mid-form (Ctrl-D, or a file that ran out of lines). Not an error.
            return CommandOutcome.EXIT;
        }

        try {
            simulate(request.get());
        } catch (CreditSimulatorException failure) {
            view.printBlankLine();
            view.printError(failure);
        }
        return CommandOutcome.CONTINUE;
    }
    
    public void simulate(LoanRequest request) {
        validator.validate(request);
        SheetManager sheets = new SheetManager();
        InstallmentPlan plan = calculator.calculate(request);
        sheets.setCurrent(plan);
        view.printBlankLine();
        view.print(formatter.format(plan));
    }
}
