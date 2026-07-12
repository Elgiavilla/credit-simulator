package service;

import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.factory.VehicleFactory;
import com.elgi.creditsimulator.model.InstallmentPlan;
import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.model.YearlyInstallment;
import com.elgi.creditsimulator.service.InstallmentCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallmentCalculatorTest {

    private InstallmentCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new InstallmentCalculator();
    }

    /** Compares money at two decimal places, so 100 and 100.00 are the same amount. */
    private static void assertMoney(String expected, BigDecimal actual) {
        BigDecimal rounded = actual.setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, new BigDecimal(expected).compareTo(rounded),
                "expected " + expected + " but was " + rounded.toPlainString());
    }

    private static void assertRate(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected rate " + expected + " but was " + actual);
    }

    @Nested
    @DisplayName("golden case from the reference spreadsheet (Rumus.xlsx)")
    class GoldenCase {

        /** Car, 100,000,000 price, 25% down payment, 3-year tenure — the worked case in the sheet. */
        private InstallmentPlan plan() {
            LoanRequest request = new LoanRequest(
                    VehicleFactory.create("Mobil", "Bekas", "2020"),
                    new BigDecimal("100000000"),
                    new BigDecimal("25000000"),
                    3);
            return calculator.calculate(request);
        }

        @Test
        @DisplayName("the financed principal is price less down payment")
        void principal() {
            assertMoney("75000000", plan().request().principal());
        }

        @Test
        @DisplayName("year 1: 75,000,000 @ 8.0% -> 81,000,000 -> 2,250,000.00/month")
        void yearOne() {
            YearlyInstallment year = plan().forYear(1);

            assertMoney("75000000", year.openingPrincipal());
            assertRate("8.0", year.interestRate());
            assertMoney("81000000", year.totalDue());
            assertMoney("2250000.00", year.monthlyInstallment());
        }

        @Test
        @DisplayName("year 2: 54,000,000 @ 8.1% -> 58,374,000 -> 2,432,250.00/month")
        void yearTwo() {
            YearlyInstallment year = plan().forYear(2);

            assertMoney("54000000", year.openingPrincipal());
            assertRate("8.1", year.interestRate());
            assertMoney("58374000", year.totalDue());
            assertMoney("2432250.00", year.monthlyInstallment());
        }

        @Test
        @DisplayName("year 3: 29,187,000 @ 8.6% -> 31,697,082 -> 2,641,423.50/month")
        void yearThree() {
            YearlyInstallment year = plan().forYear(3);

            assertMoney("29187000", year.openingPrincipal());
            assertRate("8.6", year.interestRate());
            assertMoney("31697082", year.totalDue());
            assertMoney("2641423.50", year.monthlyInstallment());
        }

        @Test
        @DisplayName("each year opens on the previous year's unpaid balance")
        void principalDeclinesByCarryOver() {
            InstallmentPlan plan = plan();

            for (int year = 2; year <= 3; year++) {
                YearlyInstallment previous = plan.forYear(year - 1);
                YearlyInstallment current = plan.forYear(year);

                BigDecimal expected = previous.totalDue()
                        .subtract(previous.yearlyInstallment())
                        .setScale(2, RoundingMode.HALF_UP);

                assertMoney(expected.toPlainString(), current.openingPrincipal());
            }
        }

        @Test
        @DisplayName("the average monthly divides by the real tenure, not the spreadsheet's hard-coded 60")
        void averageMonthlyFixesSpreadsheetDefect() {
            InstallmentPlan plan = plan();

            // 87,884,082 repaid in total, over 36 months.
            assertMoney("87884082", plan.totalRepayment());
            assertMoney("2441224.50", plan.averageMonthlyInstallment());

            // What cell D9 of the spreadsheet actually produces (sum / 60). Asserted here only to
            // document that the divergence is understood and deliberate, not accidental.
            BigDecimal spreadsheetD9 = plan.totalRepayment()
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            assertMoney("1464734.70", spreadsheetD9);
        }
    }

    @Nested
    @DisplayName("schedule shape")
    class ScheduleShape {

        private InstallmentPlan planFor(String type, String condition, int tenureYears) {
            return calculator.calculate(new LoanRequest(
                    VehicleFactory.create(type, condition, "2025"),
                    new BigDecimal("200000000"),
                    new BigDecimal("70000000"),
                    tenureYears));
        }

        @Test
        @DisplayName("one row per year of the tenure, numbered from 1")
        void oneRowPerYear() {
            InstallmentPlan plan = planFor("Mobil", "Baru", 6);

            assertEquals(6, plan.yearlyInstallments().size());
            for (int year = 1; year <= 6; year++) {
                assertEquals(year, plan.yearlyInstallments().get(year - 1).year());
            }
        }

        @Test
        @DisplayName("a motorcycle starts from a 9% base, so it costs more than an identical car")
        void motorcycleCostsMoreThanCar() {
            BigDecimal carMonthly = planFor("Mobil", "Baru", 3).forYear(1).monthlyInstallment();
            BigDecimal motorcycleMonthly = planFor("Motor", "Baru", 3).forYear(1).monthlyInstallment();

            assertTrue(motorcycleMonthly.compareTo(carMonthly) > 0,
                    "motorcycle (9%) should cost more than car (8%) on an identical loan");
        }

        @Test
        @DisplayName("the monthly payment rises year on year, because the divisor shrinks faster than the principal")
        void monthlyPaymentRises() {
            InstallmentPlan plan = planFor("Mobil", "Baru", 6);

            for (int year = 2; year <= 6; year++) {
                BigDecimal previous = plan.forYear(year - 1).monthlyInstallment();
                BigDecimal current = plan.forYear(year).monthlyInstallment();

                assertTrue(current.compareTo(previous) > 0,
                        "year " + year + " (" + current + ") should exceed year " + (year - 1)
                                + " (" + previous + ")");
            }
        }

        @Test
        @DisplayName("the outstanding principal falls year on year")
        void principalFalls() {
            InstallmentPlan plan = planFor("Mobil", "Baru", 6);

            for (int year = 2; year <= 6; year++) {
                BigDecimal previous = plan.forYear(year - 1).openingPrincipal();
                BigDecimal current = plan.forYear(year).openingPrincipal();

                assertTrue(current.compareTo(previous) < 0,
                        "principal should decline into year " + year);
            }
        }

        @Test
        @DisplayName("a one-year loan is a single row at the base rate")
        void singleYearLoan() {
            InstallmentPlan plan = planFor("Mobil", "Baru", 1);

            assertEquals(1, plan.yearlyInstallments().size());
            assertRate("8.0", plan.forYear(1).interestRate());
            // 130,000,000 financed @ 8% = 140,400,000, spread over 12 months.
            assertMoney("11700000.00", plan.forYear(1).monthlyInstallment());
        }

        @Test
        @DisplayName("total interest is total repayment less the financed principal")
        void totalInterestIsConsistent() {
            InstallmentPlan plan = planFor("Mobil", "Baru", 4);

            BigDecimal expected = plan.totalRepayment()
                    .subtract(plan.request().principal())
                    .setScale(2, RoundingMode.HALF_UP);

            assertMoney(expected.toPlainString(), plan.totalInterest());
        }
    }

    @Nested
    @DisplayName("degenerate inputs")
    class DegenerateInputs {

        @Test
        @DisplayName("a down payment covering the whole price leaves nothing to finance")
        void fullDownPaymentIsRejected() {
            LoanRequest request = new LoanRequest(
                    VehicleFactory.create("Mobil", "Baru", "2025"),
                    new BigDecimal("100000000"),
                    new BigDecimal("100000000"),
                    3);

            assertThrows(InvalidInputException.class, () -> calculator.calculate(request));
        }

        @Test
        @DisplayName("a null request is rejected")
        void nullRequestIsRejected() {
            assertThrows(NullPointerException.class, () -> calculator.calculate(null));
        }
    }
}
