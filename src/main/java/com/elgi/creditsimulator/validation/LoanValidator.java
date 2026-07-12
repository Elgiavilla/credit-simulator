package com.elgi.creditsimulator.validation;

import com.elgi.creditsimulator.exception.ValidationException;
import com.elgi.creditsimulator.model.LoanRequest;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class LoanValidator {

    private final List<ValidationRule> rules;

    public LoanValidator(List<ValidationRule> rules) {
        Objects.requireNonNull(rules, "rules");
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
    }

    public static LoanValidator withDefaults(Clock clock) {
        return new LoanValidator(Arrays.asList(
                new VehicleYearRule(clock),   // spec rule 1
                new TenureRule(),             // spec rule 2
                new LoanAmountRule(),         // spec: "Numeric <= 1 miliyar"
                new DownPaymentRule()));      // spec rules 3 and 4
    }

    public static LoanValidator withDefaults() {
        return withDefaults(Clock.systemDefaultZone());
    }

    public void validate(LoanRequest request) {
        Objects.requireNonNull(request, "request");

        List<String> violations = new ArrayList<>();
        for (ValidationRule rule : rules) {
            Optional<String> violation = rule.validate(request);
            violation.ifPresent(violations::add);
        }

        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
    }

    public List<String> findViolations(LoanRequest request) {
        Objects.requireNonNull(request, "request");

        List<String> violations = new ArrayList<>();
        for (ValidationRule rule : rules) {
            rule.validate(request).ifPresent(violations::add);
        }
        return Collections.unmodifiableList(violations);
    }
}
