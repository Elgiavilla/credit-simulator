package com.elgi.creditsimulator.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ValidationException extends CreditSimulatorException {

    private static final long serialVersionUID = 1L;

    private final ArrayList<String> violations;

    public ValidationException(List<String> violations) {
        super(buildMessage(violations));
        this.violations = new ArrayList<>(violations);
    }

    public List<String> violations() {
        return Collections.unmodifiableList(violations);
    }

    private static String buildMessage(List<String> violations) {
        Objects.requireNonNull(violations, "violations");
        if (violations.isEmpty()) {
            throw new IllegalArgumentException(
                    "A ValidationException must carry at least one violation.");
        }
        if (violations.size() == 1) {
            return violations.get(0);
        }

        StringBuilder message = new StringBuilder(violations.size() + " problems with this request:");
        for (String violation : violations) {
            message.append(System.lineSeparator()).append("  - ").append(violation);
        }
        return message.toString();
    }
}
