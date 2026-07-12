package com.elgi.creditsimulator.controller.command;

import com.elgi.creditsimulator.controller.Command;
import com.elgi.creditsimulator.controller.CommandOutcome;
import com.elgi.creditsimulator.exception.CreditSimulatorException;
import com.elgi.creditsimulator.session.SheetManager;
import com.elgi.creditsimulator.view.ConsoleView;

import java.util.Objects;
import java.util.Optional;

public class SaveCommand implements Command {

    private final ConsoleView view;
    private final SheetManager sheets;

    public SaveCommand(ConsoleView view, SheetManager sheets) {
        this.view = Objects.requireNonNull(view, "view");
        this.sheets = Objects.requireNonNull(sheets, "sheets");
    }

    @Override
    public String name() {
        return "save";
    }

    @Override
    public String description() {
        return "Save the current calculation as a named sheet.";
    }

    @Override
    public CommandOutcome execute() {
        if (!sheets.current().isPresent()) {
            view.print("There is no calculation to save yet. Run 'calculate' or 'load' first.");
            return CommandOutcome.CONTINUE;
        }

        Optional<String> name = view.readLine("Sheet name : ");
        if (!name.isPresent()) {
            return CommandOutcome.EXIT;
        }

        try {
            boolean replaced = sheets.save(name.get());

            view.print(replaced
                    ? "Sheet '" + name.get().trim() + "' has been overwritten."
                    : "Saved as sheet '" + name.get().trim() + "'.");

        } catch (CreditSimulatorException failure) {
            view.printError(failure);
        }
        return CommandOutcome.CONTINUE;
    }



}
