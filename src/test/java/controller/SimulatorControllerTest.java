package controller;

import com.elgi.creditsimulator.controller.Command;
import com.elgi.creditsimulator.controller.CommandOutcome;
import com.elgi.creditsimulator.controller.CommandRegistry;
import com.elgi.creditsimulator.controller.SimulatorController;
import com.elgi.creditsimulator.controller.LoanRequestReader;
import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.view.ConsoleView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the whole application through its front door, with a string of keystrokes standing in for a
 * user and a byte buffer standing in for a terminal. No mocks, no stdout capture, no global state --
 * only possible because ConsoleView takes its streams as constructor arguments.
 */
class SimulatorControllerTest {

    private static final Clock CLOCK_2026 =
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    /** Feeds the given lines to the application and returns everything it printed. */
    private static String run(String... lines) {
        String keystrokes = String.join("\n", lines) + "\n";

        ByteArrayInputStream input =
                new ByteArrayInputStream(keystrokes.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(captured, true, StandardCharsets.UTF_8);

        ConsoleView view = new ConsoleView(input, output);
        SimulatorController.createDefault(view, CLOCK_2026).run();

        return new String(captured.toByteArray(), StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("the show menu")
    class ShowMenu {

        @Test
        @DisplayName("lists every registered command")
        void listsEveryCommand() {
            String output = run("show", "exit");

            assertTrue(output.contains("calculate"), output);
            assertTrue(output.contains("show"), output);
            assertTrue(output.contains("exit"), output);
        }

        @Test
        @DisplayName("describes each command, not just names it")
        void describesEachCommand() {
            String output = run("show", "exit");

            assertTrue(output.contains("List every available command."), output);
            assertTrue(output.contains("Leave the credit simulator."), output);
        }
    }

    @Nested
    @DisplayName("the calculate flow")
    class CalculateFlow {

        @Test
        @DisplayName("a valid loan prints one line per year, in the spec's format")
        void validLoanPrintsSchedule() {
            String output = run(
                    "calculate",
                    "Mobil", "Bekas", "2020", "100000000", "3", "25000000",
                    "exit");

            // The golden case from Rumus.xlsx, all the way through the console.
            assertTrue(output.contains("tahun 1 : Rp. 2,250,000.00/bln , Suku Bunga : 8%"), output);
            assertTrue(output.contains("tahun 2 : Rp. 2,432,250.00/bln , Suku Bunga : 8,1%"), output);
            assertTrue(output.contains("tahun 3 : Rp. 2,641,423.50/bln , Suku Bunga : 8,6%"), output);
        }

        @Test
        @DisplayName("a mistyped field is re-prompted, and the fields already entered are kept")
        void mistypedFieldIsRePrompted() {
            String output = run(
                    "calculate",
                    "mobli",        // typo
                    "Mobil",        // corrected -- the other five fields are NOT re-asked
                    "Bekas", "2020", "100000000", "3", "25000000",
                    "exit");

            assertTrue(output.contains("Unknown vehicle type: 'mobli'"), output);
            assertTrue(output.contains("tahun 1 : Rp. 2,250,000.00/bln"), output);
        }

        @Test
        @DisplayName("a loan that breaks the rules is reported, and the session continues")
        void invalidLoanIsReportedWithoutCrashing() {
            String output = run(
                    "calculate",
                    "Mobil", "Baru", "2026", "100000000", "3", "10000000",  // 10% DP on a new car
                    "show",                                                  // still alive?
                    "exit");

            assertTrue(output.contains("Cannot proceed"), output);
            assertTrue(output.contains("at least 35%"), output);
            assertTrue(output.contains("Available commands:"), output);
            assertFalse(output.contains("tahun 1"), "no schedule should be printed: " + output);
        }

        @Test
        @DisplayName("every broken rule is listed at once")
        void allViolationsAreListed() {
            String output = run(
                    "calculate",
                    "Mobil", "Baru", "2020", "1500000000", "7", "150000000",
                    "exit");

            assertTrue(output.contains("4 problems"), output);
        }
    }

    @Nested
    @DisplayName("the loop")
    class Loop {

        @Test
        @DisplayName("an unknown command is reported and the session continues")
        void unknownCommandIsSurvivable() {
            String output = run("frobnicate", "exit");

            assertTrue(output.contains("Unknown command: 'frobnicate'"), output);
            assertTrue(output.contains("Sampai jumpa"), output);
        }

        @Test
        @DisplayName("commands are matched case-insensitively")
        void commandsAreCaseInsensitive() {
            assertTrue(run("SHOW", "exit").contains("Available commands:"));
            assertTrue(run("  ShOw  ", "exit").contains("Available commands:"));
        }

        @Test
        @DisplayName("a blank line is ignored rather than treated as an unknown command")
        void blankLinesAreIgnored() {
            String output = run("", "   ", "exit");

            assertFalse(output.contains("Unknown command"), output);
        }

        @Test
        @DisplayName("running out of input ends the session cleanly, without an exception")
        void endOfInputEndsCleanly() {
            String output = run("show");   // no 'exit' -- the stream simply stops

            assertTrue(output.contains("Available commands:"), output);
        }

        @Test
        @DisplayName("input ending mid-form ends the session rather than building a half request")
        void endOfInputMidFormIsSafe() {
            String output = run("calculate", "Mobil", "Baru");   // three fields short

            assertFalse(output.contains("tahun 1"), output);
        }
    }

    @Nested
    @DisplayName("input parsing")
    class InputParsing {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "100000000,      100000000",
                "100.000.000,    100000000",
                "'100,000,000',  100000000",
                "Rp 100000000,   100000000",
                "'Rp. 100.000.000', 100000000",
                "'  40000000  ', 40000000"
        })
        @DisplayName("rupiah is accepted however people actually type it")
        void amountIsTolerant(String raw, BigDecimal expected) {
            assertEquals(0, expected.compareTo(LoanRequestReader.parseAmount(raw)));
        }

        @ParameterizedTest(name = "\"{0}\" is rejected")
        @ValueSource(strings = {"abc", "100k", "-5000", "", "   ", "1.5"})
        @DisplayName("a non-numeric amount is rejected")
        void amountRejectsNonsense(String raw) {
            assertThrows(InvalidInputException.class, () -> LoanRequestReader.parseAmount(raw));
        }

        @ParameterizedTest(name = "tenure \"{0}\" is rejected as unparseable")
        @ValueSource(strings = {"three", "3.5", "", "  "})
        @DisplayName("a tenure that is not a whole number is rejected at parse time")
        void tenureRejectsNonIntegers(String raw) {
            assertThrows(InvalidInputException.class, () -> LoanRequestReader.parseTenure(raw));
        }

        @Test
        @DisplayName("a tenure of 7 parses fine -- the 1-6 bound is a lending rule, not a parse rule")
        void tenureRangeIsNotAParseConcern() {
            assertEquals(7, LoanRequestReader.parseTenure("7"));
        }
    }

    @Nested
    @DisplayName("the command registry")
    class Registry {

        @Test
        @DisplayName("registering the same name twice is a programming error, not a silent overwrite")
        void duplicateRegistrationFails() {
            CommandRegistry registry = new CommandRegistry();
            Command command = new Command() {
                @Override
                public String name() {
                    return "dup";
                }

                @Override
                public String description() {
                    return "";
                }

                @Override
                public CommandOutcome execute() {
                    return CommandOutcome.CONTINUE;
                }
            };

            registry.register(command);
            assertThrows(IllegalStateException.class, () -> registry.register(command));
        }
    }
}
