package com.elgi.creditsimulator.validation;

import com.elgi.creditsimulator.model.LoanRequest;

import java.util.Optional;

public interface ValidationRule {
    Optional<String> validate(LoanRequest request);
}
