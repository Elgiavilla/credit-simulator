package com.elgi.creditsimulator.io;


import com.elgi.creditsimulator.exception.InputFileException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class InputScript {

    private static final String COMMENT_PREFIX = "#";
    private static final String IMPLICIT_COMMAND = "calculate";

    private final List<String> lines;

    private InputScript(List<String> lines) {
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public static InputScript load(Path path, Set<String> knownCommands) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(knownCommands, "knownCommands");

        if (!Files.exists(path)) {
            throw new InputFileException("Input file not found: " + path.toAbsolutePath());
        }
        if (!Files.isReadable(path)) {
            throw new InputFileException("Input file cannot be read: " + path.toAbsolutePath());
        }

        List<String> rawLines;
        try {
            rawLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException cause) {
            throw new InputFileException("Could not read input file: " + path.toAbsolutePath(), cause);
        }

        return parse(rawLines, knownCommands);
    }

    public static InputScript parse(List<String> rawLines, Set<String> knownCommands) {
        List<String> meaningful = new ArrayList<>();

        for (String raw : rawLines) {
            String line = stripComment(raw).trim();
            if (!line.isEmpty()) {
                meaningful.add(line);
            }
        }

        if (meaningful.isEmpty()) {
            throw new InputFileException(
                    "The input file has no instructions in it (only blanks and comments).");
        }

        if (!isCommand(meaningful.get(0), knownCommands)) {
            // A bare loan form. The user wrote the six answers and expected the obvious question.
            meaningful.add(0, IMPLICIT_COMMAND);
        }

        return new InputScript(meaningful);
    }

    public List<String> lines() {
        return lines;
    }

    public InputStream toInputStream() {
        String joined = String.join(System.lineSeparator(), lines) + System.lineSeparator();
        return new ByteArrayInputStream(joined.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isCommand(String line, Set<String> knownCommands) {
        return knownCommands.contains(line.toLowerCase());
    }

    private static String stripComment(String line) {
        int hash = line.indexOf(COMMENT_PREFIX);
        return hash < 0 ? line : line.substring(0, hash);
    }

}
