package com.elgi.creditsimulator;

import com.elgi.creditsimulator.controller.SimulatorController;
import com.elgi.creditsimulator.exception.CreditSimulatorException;
import com.elgi.creditsimulator.io.InputScript;
import com.elgi.creditsimulator.view.ConsoleView;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Set;

public class Main {
    private static final int EXIT_OK = 0;
    private static final int EXIT_FAILURE = 1;

    private Main() {
        throw new AssertionError("Main is an entry point and must not be instantiated");
    }

    public static void main(String[] args) {
        System.exit(run(args, Clock.systemDefaultZone()));
    }

    static int run(String[] args, Clock clock) {
        if (args.length > 1) {
            System.err.println("Usage: credit_simulator [file_inputs.txt]");
            return EXIT_FAILURE;
        }

        try {
            if (args.length == 1) {
                return runScript(Paths.get(args[0]), clock);
            }
            return runInteractive(clock);
        } catch (CreditSimulatorException failure) {
            // Anything that got past the controller's own net. Still no stack trace for the user.
            System.err.println(failure.getMessage());
            return EXIT_FAILURE;
        }
    }

    private static int runInteractive(Clock clock) {
        SimulatorController.createDefault(ConsoleView.onSystemStreams(), clock).run();
        return EXIT_OK;
    }

    private static int runScript(Path path, Clock clock) {
        Set<String> vocabulary =
                SimulatorController.createDefault(ConsoleView.quiet(), clock).commandNames();

        InputScript script = InputScript.load(path, vocabulary);

        ConsoleView view = ConsoleView.onSystemStreams(script.toInputStream());
        SimulatorController.createDefault(view, clock).run();
        return EXIT_OK;
    }
}