package com.elgi.creditsimulator.validation;

import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.utils.MoneyFormat;

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
                    "Total loan amount must not exceed %s (1 billion), but was %s.",
                    MoneyFormat.rupiah(MAXIMUM), MoneyFormat.rupiah(amount)));
        }
        return Optional.empty();
    }
}
