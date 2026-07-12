package com.elgi.creditsimulator.session;

import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.model.InstallmentPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SheetManager {

    private final Map<String, InstallmentPlan> sheets = new LinkedHashMap<>();

    private InstallmentPlan current;
    private String activeName;

    public void setCurrent(InstallmentPlan plan) {
        this.current = Objects.requireNonNull(plan, "plan");
        this.activeName = null;
    }

    public Optional<InstallmentPlan> current() {
        return Optional.ofNullable(current);
    }

    public Optional<String> activeName() {
        return Optional.ofNullable(activeName);
    }

    public boolean save(String rawName) {
        if (current == null) {
            throw new InvalidInputException(
                    "There is no calculation to save yet. Run 'calculate' or 'load' first.");
        }

        String name = normalise(rawName);
        boolean replaced = sheets.containsKey(name);

        sheets.put(name, current);
        activeName = name;

        return replaced;
    }

    public InstallmentPlan switchTo(String rawName) {
        String name = normalise(rawName);

        InstallmentPlan plan = sheets.get(name);
        if (plan == null) {
            throw new InvalidInputException(
                    "No sheet named '" + name + "'. " + describeAvailable());
        }

        current = plan;
        activeName = name;

        return plan;
    }

    public List<String> names() {
        return Collections.unmodifiableList(new ArrayList<>(sheets.keySet()));
    }

    public Optional<InstallmentPlan> get(String rawName) {
        return Optional.ofNullable(sheets.get(normalise(rawName)));
    }

    public boolean isEmpty() {
        return sheets.isEmpty();
    }

    public int size() {
        return sheets.size();
    }

    private String normalise(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new InvalidInputException("A sheet name must not be empty.");
        }
        return rawName.trim();
    }

    private String describeAvailable() {
        return sheets.isEmpty()
                ? "No sheets have been saved yet."
                : "Saved sheets: " + String.join(", ", sheets.keySet()) + ".";
    }

}
