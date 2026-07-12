package com.elgi.creditsimulator;

import com.elgi.creditsimulator.controller.SimulatorController;
import com.elgi.creditsimulator.view.ConsoleView;

import java.time.Clock;

public class Main {
    private Main() {
        throw new AssertionError("Main is an entry point and must not be instantiated");
    }

    public static void main(String[] args) {
        ConsoleView view = ConsoleView.onSystemStreams();

        if (args.length > 0) {
            view.print("File input is not wired up yet (arriving in M5). Starting interactive mode.");
        }

        SimulatorController.createDefault(view, Clock.systemDefaultZone()).run();
    }
}