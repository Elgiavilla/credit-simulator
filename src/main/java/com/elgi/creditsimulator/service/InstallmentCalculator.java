package com.elgi.creditsimulator.service;

import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.model.InstallmentPlan;
import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.model.YearlyInstallment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InstallmentCalculator {

    private static final MathContext PRECISION = new MathContext(34, RoundingMode.HALF_UP);

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final InterestRateCalculator interestRateCalculator;

    public InstallmentCalculator(InterestRateCalculator interestRateCalculator) {
        this.interestRateCalculator =
                Objects.requireNonNull(interestRateCalculator, "interestRateCalculator");
    }

    public InstallmentCalculator() {
        this(new InterestRateCalculator());
    }

    public InstallmentPlan calculate(LoanRequest request) {
        Objects.requireNonNull(request, "request");

        BigDecimal principal = request.principal();
        if (principal.signum() <= 0) {
            throw new InvalidInputException(
                    "Nothing to finance: the down payment covers the full loan amount.");
        }

        int tenureYears = request.tenureYears();
        List<BigDecimal> rates = interestRateCalculator.ratesFor(request.vehicle(), tenureYears);

        List<YearlyInstallment> schedule = new ArrayList<>(tenureYears);
        BigDecimal openingPrincipal = principal;

        for (int year = 1; year <= tenureYears; year++) {
            BigDecimal rate = rates.get(year - 1);

            BigDecimal totalDue =
                    openingPrincipal.add(openingPrincipal.multiply(asFraction(rate), PRECISION));

            int remainingMonths = request.tenureMonths() - ((year - 1) * 12);
            BigDecimal monthlyInstallment =
                    totalDue.divide(BigDecimal.valueOf(remainingMonths), PRECISION);
            BigDecimal yearlyInstallment = monthlyInstallment.multiply(MONTHS_PER_YEAR);

            schedule.add(new YearlyInstallment(
                    year, rate, openingPrincipal, totalDue, monthlyInstallment, yearlyInstallment));

            // The next year opens on whatever of this year's total was not actually paid off.
            openingPrincipal = totalDue.subtract(yearlyInstallment);
        }

        return new InstallmentPlan(request, schedule);
    }

    private BigDecimal asFraction(BigDecimal ratePercent) {
        return ratePercent.divide(ONE_HUNDRED, PRECISION);
    }
}
