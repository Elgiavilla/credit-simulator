package com.elgi.creditsimulator.validation;

import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.model.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public final class DownPaymentRule implements ValidationRule {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    public Optional<String> validate(LoanRequest request) {
        BigDecimal downPayment = request.downPayment();
        BigDecimal totalLoanAmount = request.totalLoanAmount();

        if (totalLoanAmount.signum() <= 0) {
            // The loan amount is already invalid; LoanAmountRule will report it. Reporting a
            // meaningless percentage on top of that would only add noise.
            return Optional.empty();
        }

        Vehicle vehicle = request.vehicle();
        BigDecimal minimumRatio = vehicle.minimumDownPaymentRatio();
        BigDecimal minimumAmount = totalLoanAmount.multiply(minimumRatio);

        if (downPayment.compareTo(minimumAmount) < 0) {
            return Optional.of(String.format(
                    "Down payment for a %s %s must be at least %s%% of the loan amount "
                            + "(Rp %s), but was Rp %s (%s%%).",
                    vehicle.type().displayName(),
                    vehicle.condition().displayName(),
                    asPercent(minimumRatio),
                    minimumAmount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    downPayment.toPlainString(),
                    asPercent(request.downPaymentRatio())));
        }

        if (downPayment.compareTo(totalLoanAmount) >= 0) {
            return Optional.of(String.format(
                    "Down payment (Rp %s) covers the entire loan amount (Rp %s). "
                            + "There is nothing left to finance.",
                    downPayment.toPlainString(), totalLoanAmount.toPlainString()));
        }

        return Optional.empty();
    }

    /** {@code 0.35} -> {@code "35"}, {@code 0.3} -> {@code "30"}. */
    private String asPercent(BigDecimal ratio) {
        return ratio.multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
