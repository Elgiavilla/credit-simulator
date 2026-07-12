package com.elgi.creditsimulator.model;

import com.elgi.creditsimulator.exception.InvalidInputException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum VehicleCondition {

    BARU("Baru", new BigDecimal("0.35"), List.of("baru", "new")),
    BEKAS("Bekas", new BigDecimal("0.25"), List.of("bekas", "lama", "used"));

    private final String displayName;
    private final BigDecimal minimumDownPaymentRatio;
    private final List<String> aliases;

    VehicleCondition(String displayName, BigDecimal minimumDownPaymentRatio, List<String> aliases) {
        this.displayName = displayName;
        this.minimumDownPaymentRatio = minimumDownPaymentRatio;
        this.aliases = aliases;
    }

    public String displayName() {
        return displayName;
    }

    /** The minimum down payment as a fraction of the total loan amount, e.g. {@code 0.35}. */
    public BigDecimal minimumDownPaymentRatio() {
        return minimumDownPaymentRatio;
    }

    public boolean isNew() {
        return this == BARU;
    }

    /**
     * Parses free-form input into a condition. Case-insensitive and whitespace-tolerant.
     *
     * @throws InvalidInputException if the input matches no known condition
     */
    public static VehicleCondition fromInput(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidInputException(
                    "Vehicle condition must not be empty. Expected one of: " + options());
        }
        String normalised = input.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(condition -> condition.aliases.contains(normalised))
                .findFirst()
                .orElseThrow(() -> new InvalidInputException(
                        "Unknown vehicle condition: '" + input.trim() + "'. Expected one of: " + options()));
    }

    public static String options() {
        return Arrays.stream(values())
                .map(VehicleCondition::displayName)
                .collect(Collectors.joining(" | "));
    }
}
