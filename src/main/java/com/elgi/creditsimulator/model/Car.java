package com.elgi.creditsimulator.model;

import java.math.BigDecimal;

public final class Car extends Vehicle {

    private static final BigDecimal BASE_INTEREST_RATE = new BigDecimal("8.0");

    public Car(VehicleCondition condition, int year) {
        super(condition, year);
    }

    @Override
    public VehicleType type() {
        return VehicleType.MOBIL;
    }

    @Override
    public BigDecimal baseInterestRate() {
        return BASE_INTEREST_RATE;
    }
}
