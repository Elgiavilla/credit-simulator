package com.elgi.creditsimulator.view;

import com.elgi.creditsimulator.utils.MoneyFormat;
import com.elgi.creditsimulator.model.InstallmentPlan;
import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.model.YearlyInstallment;

public class InstallmentPlanFormatter {

    private static final String NEWLINE = System.lineSeparator();

    public String format(InstallmentPlan plan) {
        StringBuilder output = new StringBuilder();

        output.append(formatSummary(plan.request())).append(NEWLINE);
        output.append(NEWLINE);
        output.append(formatInstallments(plan));

        return output.toString();
    }

    public String formatInstallments(InstallmentPlan plan) {
        StringBuilder output = new StringBuilder();

        for (YearlyInstallment installment : plan.yearlyInstallments()) {
            if (output.length() > 0) {
                output.append(NEWLINE);
            }
            output.append(formatYear(installment));
        }
        return output.toString();
    }

    public String formatYear(YearlyInstallment installment) {
        return String.format(
                "tahun %d : %s/bln , Suku Bunga : %s%%",
                installment.year(),
                MoneyFormat.rupiah(installment.monthlyInstallment()),
                MoneyFormat.percent(installment.interestRate()));
    }

    private String formatSummary(LoanRequest request) {
        return new StringBuilder()
                .append("Jenis Kendaraan   : ").append(request.vehicle().type().displayName())
                .append(NEWLINE)
                .append("Kondisi           : ").append(request.vehicle().condition().displayName())
                .append(NEWLINE)
                .append("Tahun Kendaraan   : ").append(request.vehicle().year())
                .append(NEWLINE)
                .append("Jumlah Pinjaman   : ").append(MoneyFormat.rupiah(request.totalLoanAmount()))
                .append(NEWLINE)
                .append("Jumlah DP         : ").append(MoneyFormat.rupiah(request.downPayment()))
                .append(NEWLINE)
                .append("Pokok Pinjaman    : ").append(MoneyFormat.rupiah(request.principal()))
                .append(NEWLINE)
                .append("Tenor             : ").append(request.tenureYears()).append(" tahun")
                .toString();
    }

}
