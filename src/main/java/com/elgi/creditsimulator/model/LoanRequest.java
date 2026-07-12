package com.elgi.creditsimulator.model;

import com.elgi.creditsimulator.exception.InvalidInputException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

public final class LoanRequest {

    private final Vehicle vehicle;
    private final BigDecimal totalLoanAmount;
    private final BigDecimal downPayment;
    private final int tenureYears;

    public LoanRequest(
            Vehicle vehicle,
            BigDecimal totalLoanAmount,
            BigDecimal downPayment,
            int tenureYears) {

        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.totalLoanAmount = Objects.requireNonNull(totalLoanAmount, "totalLoanAmount");
        this.downPayment = Objects.requireNonNull(downPayment, "downPayment");

        if (totalLoanAmount.signum() < 0) {
            throw new InvalidInputException("Total loan amount must not be negative.");
        }
        if (downPayment.signum() < 0) {
            throw new InvalidInputException("Down payment must not be negative.");
        }
        if (tenureYears < 1) {
            throw new InvalidInputException(
                    "Loan tenure must be at least 1 year, but was " + tenureYears + ".");
        }
        this.tenureYears = tenureYears;
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    public BigDecimal totalLoanAmount() {
        return totalLoanAmount;
    }

    public BigDecimal downPayment() {
        return downPayment;
    }

    public int tenureYears() {
        return tenureYears;
    }

    /**
     * The amount actually financed: the price less the down payment.
     *
     * <p>This is the "Pokok Pinjaman" of the reference spreadsheet, and the figure the first year's
     * interest is charged on.
     */
    public BigDecimal principal() {
        return totalLoanAmount.subtract(downPayment);
    }

    /** The loan term expressed in months. */
    public int tenureMonths() {
        return tenureYears * 12;
    }

    /** The down payment as a fraction of the total loan amount, e.g. {@code 0.35}. */
    public BigDecimal downPaymentRatio() {
        if (totalLoanAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return downPayment.divide(totalLoanAmount, MathContext.DECIMAL64);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LoanRequest)) {
            return false;
        }
        LoanRequest request = (LoanRequest) other;
        return tenureYears == request.tenureYears
                && vehicle.equals(request.vehicle)
                // compareTo, not equals: BigDecimal("100") is not equals() to BigDecimal("100.00"),
                // and two requests for the same money are the same request.
                && totalLoanAmount.compareTo(request.totalLoanAmount) == 0
                && downPayment.compareTo(request.downPayment) == 0;
    }

    @Override
    public int hashCode() {
        // stripTrailingZeros so that equal-by-compareTo amounts hash alike, matching equals above.
        return Objects.hash(
                vehicle,
                totalLoanAmount.stripTrailingZeros(),
                downPayment.stripTrailingZeros(),
                tenureYears);
    }

    @Override
    public String toString() {
        return String.format(
                "LoanRequest{vehicle=%s, totalLoanAmount=%s, downPayment=%s, tenureYears=%d}",
                vehicle, totalLoanAmount.toPlainString(), downPayment.toPlainString(), tenureYears);
    }
}
