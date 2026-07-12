package com.elgi.creditsimulator.controller.command;

import com.elgi.creditsimulator.controller.Command;
import com.elgi.creditsimulator.controller.CommandOutcome;
import com.elgi.creditsimulator.exception.CreditSimulatorException;
import com.elgi.creditsimulator.model.InstallmentPlan;
import com.elgi.creditsimulator.session.SheetManager;
import com.elgi.creditsimulator.view.ConsoleView;
import com.elgi.creditsimulator.view.InstallmentPlanFormatter;

import java.util.Objects;
import java.util.Optional;

public class SwitchCommand implements Command {

    private final ConsoleView view;
    private final SheetManager sheets;
    private final InstallmentPlanFormatter formatter;

    public SwitchCommand(ConsoleView view, SheetManager sheets, InstallmentPlanFormatter formatter) {
        this.view = Objects.requireNonNull(view, "view");
        this.sheets = Objects.requireNonNull(sheets, "sheets");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    @Override
    public String name() {
        return "switch";
    }

    @Override
    public String description() {
        return "Switch to a saved sheet and show its calculation again.";
    }

    @Override
    public CommandOutcome execute() {
        if (sheets.isEmpty()) {
            view.print("No sheets saved yet. Run 'calculate', then 'save'.");
            return CommandOutcome.CONTINUE;
        }

        view.print("Available sheets: " + String.join(", ", sheets.names()));

        Optional<String> name = view.readLine("Switch to  : ");
        if (!name.isPresent()) {
            return CommandOutcome.EXIT;
        }

        try {
            InstallmentPlan plan = sheets.switchTo(name.get());

            view.printBlankLine();
            view.print("--- sheet: " + name.get().trim() + " ---");
            view.print(formatter.format(plan));

        } catch (CreditSimulatorException failure) {
            view.printError(failure);
        }
        return CommandOutcome.CONTINUE;
    }
}
