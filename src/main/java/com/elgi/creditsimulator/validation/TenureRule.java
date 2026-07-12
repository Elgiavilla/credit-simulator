package com.elgi.creditsimulator.validation;

import com.elgi.creditsimulator.model.LoanRequest;

import java.util.Optional;

public final class TenureRule implements ValidationRule {

    static final int MINIMUM_YEARS = 1;
    static final int MAXIMUM_YEARS = 6;

    @Override
    public Optional<String> validate(LoanRequest request) {
        int tenure = request.tenureYears();

        if (tenure < MINIMUM_YEARS || tenure > MAXIMUM_YEARS) {
            return Optional.of(String.format(
                    "Loan tenure must be between %d and %d years, but was %d.",
                    MINIMUM_YEARS, MAXIMUM_YEARS, tenure));
        }
        return Optional.empty();
    }
}
