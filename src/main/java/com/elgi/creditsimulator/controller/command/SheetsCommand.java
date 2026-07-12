package com.elgi.creditsimulator.controller.command;

import com.elgi.creditsimulator.controller.Command;
import com.elgi.creditsimulator.controller.CommandOutcome;
import com.elgi.creditsimulator.model.InstallmentPlan;
import com.elgi.creditsimulator.session.SheetManager;
import com.elgi.creditsimulator.view.ConsoleView;

import java.util.Objects;
import java.util.Optional;

public class SheetsCommand implements Command {

    private final ConsoleView view;
    private final SheetManager sheets;

    public SheetsCommand(ConsoleView view, SheetManager sheets) {
        this.view = Objects.requireNonNull(view, "view");
        this.sheets = Objects.requireNonNull(sheets, "sheets");
    }

    @Override
    public String name() {
        return "sheets";
    }

    @Override
    public String description() {
        return "List the saved sheets.";
    }

    @Override
    public CommandOutcome execute() {
        if (sheets.isEmpty()) {
            view.print("No sheets saved yet. Run 'calculate', then 'save'.");
            return CommandOutcome.CONTINUE;
        }

        Optional<String> active = sheets.activeName();
        view.print("Saved sheets (" + sheets.size() + "):");

        for (String name : sheets.names()) {
            boolean isActive = active.isPresent() && active.get().equals(name);
            String marker = isActive ? "*" : " ";

            // A one-line summary per sheet, so the list is enough to choose from without switching
            // to each one in turn just to remember what it was.
            InstallmentPlan plan = sheets.get(name).orElseThrow(IllegalStateException::new);
            view.print(String.format("  %s %-20s %s, %s, tenor %d thn",
                    marker,
                    name,
                    plan.request().vehicle().type().displayName(),
                    plan.request().vehicle().condition().displayName(),
                    plan.request().tenureYears()));
        }

        if (active.isPresent()) {
            view.print("  (* = currently active)");
        }
        return CommandOutcome.CONTINUE;
    }
}
