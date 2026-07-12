package com.elgi.creditsimulator.service;

import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.model.Vehicle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InterestRateCalculator {

    private static final BigDecimal ANNUAL_INCREMENT = new BigDecimal("0.1");

    private static final BigDecimal BIENNIAL_INCREMENT = new BigDecimal("0.5");

    public BigDecimal rateFor(BigDecimal baseRate, int year) {
        if (baseRate == null) {
            throw new InvalidInputException("Base interest rate must not be null.");
        }
        if (year < 1) {
            throw new InvalidInputException("Loan year must be 1 or greater, but was " + year + ".");
        }

        BigDecimal rate = baseRate;
        for (int transition = 1; transition < year; transition++) {
            rate = rate.add(incrementForTransition(transition));
        }
        return rate;
    }

    public List<BigDecimal> ratesFor(BigDecimal baseRate, int tenureYears) {
        if (tenureYears < 1) {
            throw new InvalidInputException(
                    "Loan tenure must be at least 1 year, but was " + tenureYears + ".");
        }

        List<BigDecimal> rates = new ArrayList<>(tenureYears);
        for (int year = 1; year <= tenureYears; year++) {
            rates.add(rateFor(baseRate, year));
        }
        return Collections.unmodifiableList(rates);
    }

    public List<BigDecimal> ratesFor(Vehicle vehicle, int tenureYears) {
        if (vehicle == null) {
            throw new InvalidInputException("Vehicle must not be null.");
        }
        return ratesFor(vehicle.baseInterestRate(), tenureYears);
    }

    private BigDecimal incrementForTransition(int transition) {
        boolean isEvenBoundary = transition % 2 == 0;
        return isEvenBoundary ? BIENNIAL_INCREMENT : ANNUAL_INCREMENT;
    }
}
