package com.elgi.creditsimulator.view;

import com.elgi.creditsimulator.exception.CreditSimulatorException;
import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.exception.ValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class ConsoleView {

    private final BufferedReader input;
    private final PrintStream output;

    public ConsoleView(InputStream input, PrintStream output) {
        this.input = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(input, "input"), StandardCharsets.UTF_8));
        this.output = Objects.requireNonNull(output, "output");
    }

    /** The production wiring. */
    public static ConsoleView onSystemStreams() {
        return new ConsoleView(System.in, System.out);
    }

    // ---------------------------------------------------------------- output

    public void print(String message) {
        output.println(message);
    }

    public void printBlankLine() {
        output.println();
    }

    public void printBanner() {
        print("=====================================");
        print("     BCAD CREDIT SIMULATOR");
        print("=====================================");
        print("Type 'show' to list the available commands.");
    }

    public void printError(CreditSimulatorException failure) {
        if (failure instanceof ValidationException) {
            List<String> violations = ((ValidationException) failure).violations();
            print(violations.size() == 1
                    ? "Cannot proceed: " + violations.get(0)
                    : "Cannot proceed. " + violations.size() + " problems with this request:");
            if (violations.size() > 1) {
                for (String violation : violations) {
                    print("  - " + violation);
                }
            }
        } else {
            print("Cannot proceed: " + failure.getMessage());
        }
    }

    public Optional<String> readLine(String prompt) {
        output.print(prompt);
        output.flush();
        try {
            return Optional.ofNullable(input.readLine());
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not read from input", cause);
        }
    }

    public <T> Optional<T> promptUntilValid(String prompt, Function<String, T> parser) {
        while (true) {
            Optional<String> line = readLine(prompt);
            if (!line.isPresent()) {
                return Optional.empty();
            }
            try {
                return Optional.of(parser.apply(line.get()));
            } catch (InvalidInputException invalid) {
                print("  " + invalid.getMessage());
            }
        }
    }

}
