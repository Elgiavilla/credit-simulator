package com.elgi.creditsimulator.factory;

import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.model.Vehicle;
import com.elgi.creditsimulator.model.VehicleCondition;
import com.elgi.creditsimulator.model.VehicleType;
import com.elgi.creditsimulator.model.Car;
import com.elgi.creditsimulator.model.Motorcycle;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class VehicleFactory {

    private static final Map<VehicleType, BiFunction<VehicleCondition, Integer, Vehicle>> REGISTRY =
            new EnumMap<>(VehicleType.class);

    static {
        REGISTRY.put(VehicleType.MOBIL, Car::new);
        REGISTRY.put(VehicleType.MOTOR, Motorcycle::new);
    }

    private VehicleFactory() {
        throw new AssertionError("VehicleFactory is a static factory and must not be instantiated");
    }

    /**
     * Creates a vehicle from already-parsed domain values.
     *
     * @throws InvalidInputException if the type has no registered implementation
     */
    public static Vehicle create(VehicleType type, VehicleCondition condition, int year) {
        if (type == null) {
            throw new InvalidInputException("Vehicle type must not be empty. Expected one of: "
                    + VehicleType.options());
        }
        if (condition == null) {
            throw new InvalidInputException("Vehicle condition must not be empty. Expected one of: "
                    + VehicleCondition.options());
        }

        BiFunction<VehicleCondition, Integer, Vehicle> constructor = REGISTRY.get(type);
        if (constructor == null) {
            // Unreachable while the registry covers the enum, but a loud failure here beats a
            // silent null if someone adds an enum constant and forgets to register it.
            throw new InvalidInputException(
                    "No vehicle implementation registered for type: " + type.displayName());
        }
        return constructor.apply(condition, year);
    }

    /**
     * Creates a vehicle from raw user input, parsing each field on the way in.
     *
     * @throws InvalidInputException if any field cannot be parsed
     */
    public static Vehicle create(String type, String condition, String year) {
        return create(
                VehicleType.fromInput(type),
                VehicleCondition.fromInput(condition),
                parseYear(year));
    }

    private static int parseYear(String year) {
        if (year == null || year.isBlank()) {
            throw new InvalidInputException("Vehicle year must not be empty. Expected a 4-digit year.");
        }
        String normalised = year.trim();
        if (!normalised.matches("\\d{4}")) {
            throw new InvalidInputException(
                    "Invalid vehicle year: '" + normalised + "'. Expected exactly 4 digits, e.g. 2025.");
        }
        return Integer.parseInt(normalised);
    }
}
