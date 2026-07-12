package com.elgi.creditsimulator.view;

import com.elgi.creditsimulator.exception.CreditSimulatorException;
import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.exception.ValidationException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class ConsoleView {

    private final BufferedReader input;
    private final PrintStream output;
    private final boolean echoInput;

    public ConsoleView(InputStream input, PrintStream output, boolean echoInput) {
        this.input = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(input, "input"), StandardCharsets.UTF_8));
        this.output = Objects.requireNonNull(output, "output");
        this.echoInput = echoInput;
    }

    /** The production wiring. */
    public static ConsoleView onSystemStreams() {
        return new ConsoleView(System.in, System.out, false);
    }

    public static ConsoleView onSystemStreams(InputStream scriptedInput) {
        return new ConsoleView(scriptedInput, System.out, true);
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
            Optional<String> line = Optional.ofNullable(input.readLine());
            if (echoInput) {
                // The prompt was printed without a newline, expecting the terminal to echo what the
                // user typed. Nothing is typing, so complete the line ourselves.
                output.println(line.orElse(""));
            }
            return line;
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

    public static ConsoleView quiet() {
        return new ConsoleView(
                new ByteArrayInputStream(new byte[0]),
                new PrintStream(OutputStream.nullOutputStream()),
                false);
    }

}
