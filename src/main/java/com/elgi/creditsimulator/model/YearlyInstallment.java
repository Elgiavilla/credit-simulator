package com.elgi.creditsimulator.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class YearlyInstallment {

    private final int year;
    private final BigDecimal interestRate;
    private final BigDecimal openingPrincipal;
    private final BigDecimal totalDue;
    private final BigDecimal monthlyInstallment;
    private final BigDecimal yearlyInstallment;

    /**
     * @param year               1-based year of the loan
     * @param interestRate       the annual rate for this year, as a percentage (e.g. {@code 8.1})
     * @param openingPrincipal   the outstanding principal at the start of this year
     * @param totalDue           {@code openingPrincipal} plus this year's interest
     * @param monthlyInstallment the monthly payment charged during this year
     * @param yearlyInstallment  twelve monthly payments
     */
    public YearlyInstallment(
            int year,
            BigDecimal interestRate,
            BigDecimal openingPrincipal,
            BigDecimal totalDue,
            BigDecimal monthlyInstallment,
            BigDecimal yearlyInstallment) {

        this.year = year;
        this.interestRate = Objects.requireNonNull(interestRate, "interestRate");
        this.openingPrincipal = Objects.requireNonNull(openingPrincipal, "openingPrincipal");
        this.totalDue = Objects.requireNonNull(totalDue, "totalDue");
        this.monthlyInstallment = Objects.requireNonNull(monthlyInstallment, "monthlyInstallment");
        this.yearlyInstallment = Objects.requireNonNull(yearlyInstallment, "yearlyInstallment");
    }

    public int year() {
        return year;
    }

    public BigDecimal interestRate() {
        return interestRate;
    }

    public BigDecimal openingPrincipal() {
        return openingPrincipal;
    }

    public BigDecimal totalDue() {
        return totalDue;
    }

    public BigDecimal monthlyInstallment() {
        return monthlyInstallment;
    }

    public BigDecimal yearlyInstallment() {
        return yearlyInstallment;
    }

    /** The monthly payment rounded to whole cents, for display and money-equality assertions. */
    public BigDecimal monthlyInstallmentRounded() {
        return monthlyInstallment.setScale(2, RoundingMode.HALF_UP);
    }

    /** The interest charged in this year alone: {@code totalDue - openingPrincipal}. */
    public BigDecimal interestCharged() {
        return totalDue.subtract(openingPrincipal);
    }

    @Override
    public String toString() {
        return String.format(
                "Year %d: %s%% on %s -> %s/month",
                year,
                interestRate.toPlainString(),
                openingPrincipal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                monthlyInstallmentRounded().toPlainString());
    }
}
