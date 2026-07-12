package com.elgi.creditsimulator.model;

import com.elgi.creditsimulator.exception.InvalidInputException;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum VehicleType {

    MOBIL("Mobil"),
    MOTOR("Motor");

    private final String displayName;

    VehicleType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Parses free-form input into a type. Case-insensitive and whitespace-tolerant, as the spec
     * requires ("Alphabet, Ignore Cased").
     *
     * @throws InvalidInputException if the input matches no known type
     */
    public static VehicleType fromInput(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidInputException(
                    "Vehicle type must not be empty. Expected one of: " + options());
        }
        String normalised = input.trim();
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(normalised))
                .findFirst()
                .orElseThrow(() -> new InvalidInputException(
                        "Unknown vehicle type: '" + input.trim() + "'. Expected one of: " + options()));
    }

    public static String options() {
        return Arrays.stream(values())
                .map(VehicleType::displayName)
                .collect(Collectors.joining(" | "));
    }

}
