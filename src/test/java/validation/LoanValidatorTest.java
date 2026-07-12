package validation;

import com.elgi.creditsimulator.exception.ValidationException;
import com.elgi.creditsimulator.factory.VehicleFactory;
import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.validation.LoanValidator;
import com.elgi.creditsimulator.validation.TenureRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanValidatorTest {

    private static final Clock CLOCK_2026 =
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final LoanValidator validator = LoanValidator.withDefaults(CLOCK_2026);

    private static LoanRequest request(
            String type, String condition, String year, String amount, String downPayment, int tenure) {
        return new LoanRequest(
                VehicleFactory.create(type, condition, year),
                new BigDecimal(amount),
                new BigDecimal(downPayment),
                tenure);
    }

    private static LoanRequest validRequest() {
        return request("Mobil", "Baru", "2026", "100000000", "40000000", 3);
    }

    @Test
    @DisplayName("a request satisfying every rule passes")
    void validRequestPasses() {
        assertDoesNotThrow(() -> validator.validate(validRequest()));
        assertTrue(validator.findViolations(validRequest()).isEmpty());
    }

    @Nested
    @DisplayName("model year (spec rule 1)")
    class ModelYear {

        @ParameterizedTest(name = "a new vehicle from {0} is accepted in 2026")
        @ValueSource(strings = {"2025", "2026"})
        @DisplayName("a new vehicle may be this year's model or last year's")
        void newVehicleAtOrAfterFloor(String year) {
            assertDoesNotThrow(() -> validator.validate(
                    request("Mobil", "Baru", year, "100000000", "40000000", 3)));
        }

        @Test
        @DisplayName("a new vehicle from currentYear - 2 is rejected")
        void newVehicleBelowFloor() {
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> validator.validate(
                            request("Mobil", "Baru", "2024", "100000000", "40000000", 3)));

            assertEquals(1, exception.violations().size());
            assertTrue(exception.getMessage().contains("2025"),
                    "the message should name the earliest allowed year");
        }

        @ParameterizedTest(name = "a used vehicle from {0} is accepted")
        @ValueSource(strings = {"1990", "2010", "2024", "2026"})
        @DisplayName("a used vehicle has no model-year floor")
        void usedVehicleHasNoFloor(String year) {
            assertDoesNotThrow(() -> validator.validate(
                    request("Motor", "Bekas", year, "100000000", "30000000", 3)));
        }

        @Test
        @DisplayName("the spec's own REST sample (new car, 2025) is valid under a 2026 clock")
        void specSamplePayloadIsValidToday() {
            // vehicleType Mobil, vehicleCondition Baru, vehicleYear 2025,
            // totalLoanAmount 1000000000, loanTenure 6, downPayment 500000000
            assertDoesNotThrow(() -> validator.validate(
                    request("Mobil", "Baru", "2025", "1000000000", "500000000", 6)));
        }

        @Test
        @DisplayName("that same sample becomes invalid in 2027 -- which is why the clock is injected")
        void specSamplePayloadExpires() {
            LoanValidator validatorIn2027 = LoanValidator.withDefaults(
                    Clock.fixed(Instant.parse("2027-01-01T00:00:00Z"), ZoneOffset.UTC));

            assertThrows(ValidationException.class, () -> validatorIn2027.validate(
                    request("Mobil", "Baru", "2025", "1000000000", "500000000", 6)));
        }
    }

    @Nested
    @DisplayName("tenure (spec rule 2)")
    class Tenure {

        @ParameterizedTest(name = "a {0}-year tenure is accepted")
        @ValueSource(ints = {1, 2, 3, 4, 5, 6})
        @DisplayName("1 to 6 years inclusive")
        void tenureWithinRange(int tenure) {
            assertDoesNotThrow(() -> validator.validate(
                    request("Mobil", "Baru", "2026", "100000000", "40000000", tenure)));
        }

        @ParameterizedTest(name = "a {0}-year tenure is rejected")
        @ValueSource(ints = {7, 10, 100})
        @DisplayName("more than 6 years is rejected")
        void tenureAboveCeiling(int tenure) {
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> validator.validate(
                            request("Mobil", "Baru", "2026", "100000000", "40000000", tenure)));

            assertTrue(exception.getMessage().contains("6"),
                    "the message should name the ceiling");
        }
    }

    @Nested
    @DisplayName("loan amount (<= 1 miliar)")
    class LoanAmount {

        @Test
        @DisplayName("exactly one billion is accepted -- the bound is inclusive")
        void exactlyOneBillionPasses() {
            assertDoesNotThrow(() -> validator.validate(
                    request("Mobil", "Baru", "2026", "1000000000", "400000000", 3)));
        }

        @Test
        @DisplayName("one rupiah over one billion is rejected")
        void oneRupiahOverIsRejected() {
            assertThrows(ValidationException.class, () -> validator.validate(
                    request("Mobil", "Baru", "2026", "1000000001", "400000000", 3)));
        }

        @Test
        @DisplayName("a zero loan amount is rejected")
        void zeroAmountIsRejected() {
            assertThrows(ValidationException.class, () -> validator.validate(
                    request("Mobil", "Baru", "2026", "0", "0", 3)));
        }

        @Test
        @DisplayName("an invalid amount does not also trigger a meaningless down-payment percentage")
        void invalidAmountSuppressesDownPaymentNoise() {
            List<String> violations = validator.findViolations(
                    request("Mobil", "Baru", "2026", "0", "0", 3));

            assertEquals(1, violations.size(),
                    "a zero loan amount should report once, not once per dependent rule: " + violations);
        }
    }

    @Nested
    @DisplayName("down payment (spec rules 3 and 4)")
    class DownPayment {

        @ParameterizedTest(name = "new vehicle, DP {0} of 100,000,000 -> valid={1}")
        @CsvSource({
                "34999999, false",  // a whisker under 35%
                "35000000, true",   // exactly 35% -- the bound is inclusive
                "35000001, true",
                "99999999, true"
        })
        @DisplayName("a new vehicle needs at least 35%")
        void newVehicleNeeds35Percent(String downPayment, boolean expectedValid) {
            LoanRequest request =
                    request("Mobil", "Baru", "2026", "100000000", downPayment, 3);

            assertEquals(expectedValid, validator.findViolations(request).isEmpty(),
                    "DP " + downPayment + " -> " + validator.findViolations(request));
        }

        @ParameterizedTest(name = "used vehicle, DP {0} of 100,000,000 -> valid={1}")
        @CsvSource({
                "24999999, false",
                "25000000, true",   // exactly 25% -- the bound is inclusive
                "25000001, true"
        })
        @DisplayName("a used vehicle needs at least 25%")
        void usedVehicleNeeds25Percent(String downPayment, boolean expectedValid) {
            LoanRequest request =
                    request("Motor", "Bekas", "2018", "100000000", downPayment, 3);

            assertEquals(expectedValid, validator.findViolations(request).isEmpty());
        }

        @Test
        @DisplayName("the 25% minimum applies to a used vehicle that a new one would fail")
        void thirtyPercentPassesUsedButFailsNew() {
            String thirtyPercent = "30000000";

            assertTrue(validator.findViolations(
                    request("Mobil", "Bekas", "2018", "100000000", thirtyPercent, 3)).isEmpty());
            assertTrue(!validator.findViolations(
                    request("Mobil", "Baru", "2026", "100000000", thirtyPercent, 3)).isEmpty());
        }

        @Test
        @DisplayName("a down payment covering the whole price is rejected as a validation error")
        void fullDownPaymentIsRejected() {
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> validator.validate(
                            request("Mobil", "Baru", "2026", "100000000", "100000000", 3)));

            assertTrue(exception.getMessage().toLowerCase().contains("nothing left to finance"));
        }

        @Test
        @DisplayName("the violation message names both the required percentage and the required amount")
        void messageIsActionable() {
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> validator.validate(
                            request("Mobil", "Baru", "2026", "100000000", "30000000", 3)));

            String message = exception.getMessage();
            assertTrue(message.contains("35"), "should name the required percentage: " + message);
            assertTrue(message.contains("35000000.00"), "should name the required amount: " + message);
        }
    }

    @Nested
    @DisplayName("accumulation")
    class Accumulation {

        @Test
        @DisplayName("every broken rule is reported, not just the first")
        void reportsAllViolationsAtOnce() {
            // A new 2020 car (too old), 7-year tenure (too long), 1.5 billion (too much),
            // 10% down payment (too little). Four rules, four violations.
            LoanRequest request =
                    request("Mobil", "Baru", "2020", "1500000000", "150000000", 7);

            ValidationException exception =
                    assertThrows(ValidationException.class, () -> validator.validate(request));

            assertEquals(4, exception.violations().size(),
                    "expected all four rules to fire: " + exception.violations());
        }

        @Test
        @DisplayName("the message lists each violation on its own line when there is more than one")
        void multipleViolationsAreListed() {
            LoanRequest request =
                    request("Mobil", "Baru", "2026", "100000000", "10000000", 7);

            ValidationException exception =
                    assertThrows(ValidationException.class, () -> validator.validate(request));

            assertEquals(2, exception.violations().size());
            assertTrue(exception.getMessage().startsWith("2 problems"),
                    "got: " + exception.getMessage());
        }

        @Test
        @DisplayName("a validator can be assembled with a single rule, for isolated testing")
        void singleRuleValidator() {
            LoanValidator tenureOnly =
                    new LoanValidator(java.util.Collections.singletonList(new TenureRule()));

            // A 2020 'new' car would fail the year rule, but that rule is not in force here.
            LoanRequest request = request("Mobil", "Baru", "2020", "100000000", "40000000", 3);

            assertTrue(tenureOnly.findViolations(request).isEmpty());
        }
    }
}
