package com.elgi.creditsimulator.validation;

import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.model.Vehicle;

import java.time.Clock;
import java.time.Year;
import java.util.Objects;
import java.util.Optional;

public final class VehicleYearRule implements ValidationRule {

    private final Clock clock;

    public VehicleYearRule(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public VehicleYearRule() {
        this(Clock.systemDefaultZone());
    }

    @Override
    public Optional<String> validate(LoanRequest request) {
        Vehicle vehicle = request.vehicle();

        if (!vehicle.isNew()) {
            return Optional.empty();
        }

        int currentYear = Year.now(clock).getValue();
        int earliestAllowed = currentYear - 1;

        if (vehicle.year() < earliestAllowed) {
            return Optional.of(String.format(
                    "A new vehicle cannot have a model year earlier than %d (current year - 1), "
                            + "but was %d. If this vehicle is second-hand, enter its condition as "
                            + "'Bekas'.",
                    earliestAllowed, vehicle.year()));
        }
        return Optional.empty();
    }
}
