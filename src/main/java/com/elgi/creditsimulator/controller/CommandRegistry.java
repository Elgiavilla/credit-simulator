package com.elgi.creditsimulator.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CommandRegistry {

    private final Map<String, Command> commands = new LinkedHashMap<>();

    public CommandRegistry register(Command command) {
        Objects.requireNonNull(command, "command");
        String key = command.name().toLowerCase();

        if (commands.containsKey(key)) {
            throw new IllegalStateException("A command named '" + key + "' is already registered.");
        }
        commands.put(key, command);
        return this;
    }

    public Optional<Command> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(commands.get(name.trim().toLowerCase()));
    }

    public List<Command> all() {
        return Collections.unmodifiableList(new ArrayList<>(commands.values()));
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

}
