package com.elgi.creditsimulator.controller;

import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.factory.VehicleFactory;
import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.model.Vehicle;
import com.elgi.creditsimulator.model.VehicleCondition;
import com.elgi.creditsimulator.model.VehicleType;
import com.elgi.creditsimulator.view.ConsoleView;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public final class LoanRequestReader {

    private final ConsoleView view;

    public LoanRequestReader(ConsoleView view) {
        this.view = Objects.requireNonNull(view, "view");
    }

    public Optional<LoanRequest> read() {
        Optional<VehicleType> type = view.promptUntilValid(
                "Jenis Kendaraan (Motor|Mobil) : ", VehicleType::fromInput);
        if (!type.isPresent()) {
            return Optional.empty();
        }

        Optional<VehicleCondition> condition = view.promptUntilValid(
                "Kondisi (Baru|Bekas)         : ", VehicleCondition::fromInput);
        if (!condition.isPresent()) {
            return Optional.empty();
        }

        Optional<Integer> year = view.promptUntilValid(
                "Tahun Kendaraan (4 digit)    : ", LoanRequestReader::parseYear);
        if (!year.isPresent()) {
            return Optional.empty();
        }

        Optional<BigDecimal> loanAmount = view.promptUntilValid(
                "Jumlah Pinjaman Total        : ", LoanRequestReader::parseAmount);
        if (!loanAmount.isPresent()) {
            return Optional.empty();
        }

        Optional<Integer> tenure = view.promptUntilValid(
                "Tenor Pinjaman (1-6 tahun)   : ", LoanRequestReader::parseTenure);
        if (!tenure.isPresent()) {
            return Optional.empty();
        }

        Optional<BigDecimal> downPayment = view.promptUntilValid(
                "Jumlah DP                    : ", LoanRequestReader::parseAmount);
        if (!downPayment.isPresent()) {
            return Optional.empty();
        }

        Vehicle vehicle = VehicleFactory.create(type.get(), condition.get(), year.get());
        return Optional.of(
                new LoanRequest(vehicle, loanAmount.get(), downPayment.get(), tenure.get()));
    }

    static int parseYear(String raw) {
        String value = requireNonBlank(raw, "Vehicle year");
        if (!value.matches("\\d{4}")) {
            throw new InvalidInputException(
                    "Invalid year: '" + value + "'. Expected exactly 4 digits, e.g. 2025.");
        }
        return Integer.parseInt(value);
    }

    public static int parseTenure(String raw) {
        String value = requireNonBlank(raw, "Loan tenure");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException cause) {
            throw new InvalidInputException(
                    "Invalid tenure: '" + value + "'. Expected a whole number of years, e.g. 3.");
        }
    }

    public static BigDecimal parseAmount(String raw) {
        String value = requireNonBlank(raw, "Amount");
        String amount = value.replaceAll("(?i)^rp\\.?\\s*", "").trim();  // an optional "Rp" / "Rp."

        boolean plain = amount.matches("\\d+");
        boolean grouped = amount.matches("\\d{1,3}([.,]\\d{3})+");

        if (!plain && !grouped) {
            throw new InvalidInputException(
                    "Invalid amount: '" + value + "'. Expected a whole number of rupiah, "
                            + "e.g. 100000000 or 100.000.000.");
        }
        return new BigDecimal(amount.replaceAll("[.,]", ""));
    }

    private static String requireNonBlank(String raw, String field) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidInputException(field + " must not be empty.");
        }
        return raw.trim();
    }
}
