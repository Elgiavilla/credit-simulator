package com.elgi.creditsimulator.model;

import java.math.BigDecimal;
import java.util.Objects;

public abstract class Vehicle {

    private final VehicleCondition condition;
    private final int year;

    protected Vehicle(VehicleCondition condition, int year) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.year = year;
    }

    public abstract VehicleType type();

    public abstract BigDecimal baseInterestRate();

    public VehicleCondition condition() {
        return condition;
    }

    public int year() {
        return year;
    }

    public BigDecimal minimumDownPaymentRatio() {
        return condition.minimumDownPaymentRatio();
    }

    public boolean isNew() {
        return condition.isNew();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Vehicle)) {
            return false;
        }

        Vehicle vehicle = (Vehicle) other;

        return year == vehicle.year
                && condition == vehicle.condition
                && type() == vehicle.type();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), condition, year);
    }

}
