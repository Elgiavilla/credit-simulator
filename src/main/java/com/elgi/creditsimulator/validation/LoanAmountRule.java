package com.elgi.creditsimulator.validation;

import com.elgi.creditsimulator.model.LoanRequest;

import java.math.BigDecimal;
import java.util.Optional;

public final class LoanAmountRule implements ValidationRule {

    /** Rp 1,000,000,000 — "1 miliyar". */
    static final BigDecimal MAXIMUM = new BigDecimal("1000000000");

    @Override
    public Optional<String> validate(LoanRequest request) {
        BigDecimal amount = request.totalLoanAmount();

        if (amount.signum() <= 0) {
            return Optional.of("Total loan amount must be greater than zero.");
        }
        if (amount.compareTo(MAXIMUM) > 0) {
            return Optional.of(String.format(
                    "Total loan amount must not exceed Rp %s (1 billion), but was Rp %s.",
                    MAXIMUM.toPlainString(), amount.toPlainString()));
        }
        return Optional.empty();
    }
}
