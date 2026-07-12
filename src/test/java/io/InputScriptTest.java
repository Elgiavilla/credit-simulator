package io;

import com.elgi.creditsimulator.exception.InputFileException;
import com.elgi.creditsimulator.io.InputScript;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputScriptTest {

    private static final Set<String> VOCABULARY =
            new HashSet<>(Arrays.asList("calculate", "show", "exit"));

    private static List<String> linesOf(String... rawLines) {
        return InputScript.parse(Arrays.asList(rawLines), VOCABULARY).lines();
    }

    @Nested
    @DisplayName("telling a script from a form")
    class ScriptOrForm {

        @Test
        @DisplayName("a file starting with a command is taken as a script, unchanged")
        void commandFirstMeansScript() {
            assertEquals(Arrays.asList("show", "exit"), linesOf("show", "exit"));
        }

        @Test
        @DisplayName("a file starting with a loan field is taken as a form, and 'calculate' is implied")
        void fieldFirstMeansForm() {
            List<String> lines =
                    linesOf("Mobil", "Bekas", "2020", "100000000", "3", "25000000");

            assertEquals("calculate", lines.get(0));
            assertEquals(7, lines.size());
            assertEquals("Mobil", lines.get(1));
        }

        @Test
        @DisplayName("the command check is case-insensitive, like the command loop itself")
        void commandDetectionIgnoresCase() {
            assertEquals("SHOW", linesOf("SHOW", "exit").get(0),
                    "a recognised command must not have 'calculate' prepended to it");
        }

        @Test
        @DisplayName("the vocabulary is supplied, not assumed -- an unknown word is treated as a field")
        void vocabularyIsSupplied() {
            // 'show' is a command to the real application, but not to a caller that says otherwise.
            List<String> lines =
                    InputScript.parse(Collections.singletonList("show"), Collections.emptySet()).lines();

            assertEquals(Arrays.asList("calculate", "show"), lines);
        }
    }

    @Nested
    @DisplayName("cleaning the file")
    class Cleaning {

        @Test
        @DisplayName("blank lines are dropped, so a stray newline cannot shift every answer down a field")
        void blankLinesAreDropped() {
            assertEquals(
                    Arrays.asList("calculate", "Mobil", "Bekas"),
                    linesOf("calculate", "", "Mobil", "   ", "Bekas"));
        }

        @Test
        @DisplayName("comments are stripped, whole-line and trailing")
        void commentsAreStripped() {
            assertEquals(
                    Arrays.asList("calculate", "Mobil", "Bekas"),
                    linesOf("# a whole-line comment", "calculate", "Mobil     # jenis kendaraan",
                            "Bekas#kondisi"));
        }

        @Test
        @DisplayName("surrounding whitespace is trimmed")
        void whitespaceIsTrimmed() {
            assertEquals(Arrays.asList("calculate", "Mobil"), linesOf("  calculate  ", "\tMobil  "));
        }

        @Test
        @DisplayName("a file with nothing but blanks and comments is rejected, not silently ignored")
        void emptyFileIsRejected() {
            InputFileException failure = assertThrows(InputFileException.class,
                    () -> linesOf("# nothing", "   ", ""));

            assertTrue(failure.getMessage().contains("no instructions"), failure.getMessage());
        }
    }

    @Nested
    @DisplayName("loading from disk")
    class Loading {

        @Test
        @DisplayName("a real file is read, cleaned, and turned into a stream")
        void readsARealFile(@TempDir Path directory) throws IOException {
            Path file = directory.resolve("file_inputs.txt");
            Files.write(file, Arrays.asList(
                    "# the golden case",
                    "Mobil", "Bekas", "2020", "100000000", "3", "25000000",
                    "",
                    "exit"), StandardCharsets.UTF_8);

            InputScript script = InputScript.load(file, VOCABULARY);

            assertEquals(
                    Arrays.asList("calculate", "Mobil", "Bekas", "2020", "100000000", "3",
                            "25000000", "exit"),
                    script.lines());
        }

        @Test
        @DisplayName("a missing file fails with a message about the path, not about vehicles")
        void missingFileIsReported(@TempDir Path directory) {
            InputFileException failure = assertThrows(InputFileException.class,
                    () -> InputScript.load(directory.resolve("nope.txt"), VOCABULARY));

            assertTrue(failure.getMessage().contains("not found"), failure.getMessage());
            assertTrue(failure.getMessage().contains("nope.txt"), failure.getMessage());
        }

        @Test
        @DisplayName("the stream is terminated, so the last line is readable rather than swallowed")
        void streamIsNewlineTerminated(@TempDir Path directory) throws IOException {
            Path file = directory.resolve("one_line.txt");
            // No trailing newline in the file itself -- a very common way for files to be saved.
            Files.write(file, "show".getBytes(StandardCharsets.UTF_8));

            InputScript script = InputScript.load(file, VOCABULARY);

            String streamed = new String(readAll(script), StandardCharsets.UTF_8);
            assertTrue(streamed.endsWith(System.lineSeparator()),
                    "an unterminated last line would be lost by BufferedReader: " + streamed);
        }

        private byte[] readAll(InputScript script) throws IOException {
            java.io.InputStream stream = script.toInputStream();
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[256];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

}
