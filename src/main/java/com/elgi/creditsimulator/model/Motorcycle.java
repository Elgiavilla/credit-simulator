package com.elgi.creditsimulator.model;

import java.math.BigDecimal;

public final class Motorcycle extends Vehicle {

    private static final BigDecimal BASE_INTEREST_RATE = new BigDecimal("9.0");

    public Motorcycle(VehicleCondition condition, int year) {
        super(condition, year);
    }

    @Override
    public VehicleType type() {
        return VehicleType.MOTOR;
    }

    @Override
    public BigDecimal baseInterestRate() {
        return BASE_INTEREST_RATE;
    }
}
