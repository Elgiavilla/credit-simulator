package com.elgi.creditsimulator.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class InstallmentPlan {

    private final LoanRequest request;
    private final List<YearlyInstallment> yearlyInstallments;

    public InstallmentPlan(LoanRequest request, List<YearlyInstallment> yearlyInstallments) {
        this.request = Objects.requireNonNull(request, "request");
        Objects.requireNonNull(yearlyInstallments, "yearlyInstallments");
        this.yearlyInstallments =
                Collections.unmodifiableList(new ArrayList<>(yearlyInstallments));
    }

    public LoanRequest request() {
        return request;
    }

    public List<YearlyInstallment> yearlyInstallments() {
        return yearlyInstallments;
    }

    public BigDecimal totalRepayment() {
        return yearlyInstallments.stream()
                .map(YearlyInstallment::yearlyInstallment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalInterest() {
        return totalRepayment().subtract(request.principal());
    }

    public BigDecimal averageMonthlyInstallment() {
        return totalRepayment()
                .divide(BigDecimal.valueOf(request.tenureMonths()), MathContext.DECIMAL64)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public YearlyInstallment forYear(int year) {
        return yearlyInstallments.stream()
                .filter(installment -> installment.year() == year)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No installment for year " + year));
    }
}
