package service;

import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.factory.VehicleFactory;
import com.elgi.creditsimulator.model.Vehicle;
import com.elgi.creditsimulator.service.InterestRateCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterestRateCalculatorTest {

    private static final BigDecimal CAR_BASE = new BigDecimal("8.0");
    private static final BigDecimal MOTORCYCLE_BASE = new BigDecimal("9.0");

    private InterestRateCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new InterestRateCalculator();
    }

    /**
     * The tiebreaker test. The spec's two increment rules are contradictory when stacked; this
     * sequence is what the worked example in the problem statement actually shows, and it is the
     * reason the increments alternate rather than accumulate. If this ever goes red, the
     * interpretation of the spec has changed, not merely the implementation.
     */
    @ParameterizedTest(name = "car, year {0} -> {1}%")
    @CsvSource({
            "1, 8.0",
            "2, 8.1",   // +0.1 across an odd boundary  -- matches the spec sample
            "3, 8.6",   // +0.5 across an even boundary -- matches the spec sample
            "4, 8.7",
            "5, 9.2",
            "6, 9.3"
    })
    @DisplayName("car rates follow the alternating +0.1 / +0.5 progression")
    void carRateProgression(int year, BigDecimal expected) {
        BigDecimal actual = calculator.rateFor(CAR_BASE, year);

        assertEquals(0, expected.compareTo(actual),
                "year " + year + " expected " + expected + " but was " + actual);
    }

    @ParameterizedTest(name = "motorcycle, year {0} -> {1}%")
    @CsvSource({
            "1,  9.0",
            "2,  9.1",
            "3,  9.6",
            "4,  9.7",
            "5, 10.2",
            "6, 10.3"
    })
    @DisplayName("motorcycle rates follow the same progression from a 9% base")
    void motorcycleRateProgression(int year, BigDecimal expected) {
        assertEquals(0, expected.compareTo(calculator.rateFor(MOTORCYCLE_BASE, year)));
    }

    @Test
    @DisplayName("the increments do NOT accumulate: year 3 is 8.6, not 8.7")
    void incrementsDoNotStack() {
        BigDecimal yearThree = calculator.rateFor(CAR_BASE, 3);

        assertEquals(0, new BigDecimal("8.6").compareTo(yearThree));
        assertEquals(1, new BigDecimal("8.7").compareTo(yearThree),
                "8.7 would mean the +0.1 and +0.5 rules had been applied cumulatively");
    }

    @Test
    @DisplayName("ratesFor returns the whole schedule in order")
    void producesFullSchedule() {
        List<BigDecimal> rates = calculator.ratesFor(CAR_BASE, 6);

        assertEquals(6, rates.size());
        List<String> asText = rates.stream()
                .map(BigDecimal::toPlainString)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("8.0", "8.1", "8.6", "8.7", "9.2", "9.3"), asText);
    }

    @Test
    @DisplayName("the vehicle overload picks up the vehicle's own base rate")
    void vehicleOverloadUsesOwnBaseRate() {
        Vehicle car = VehicleFactory.create("Mobil", "Baru", "2025");
        Vehicle motorcycle = VehicleFactory.create("Motor", "Baru", "2025");

        assertEquals(0, new BigDecimal("8.6").compareTo(calculator.ratesFor(car, 3).get(2)));
        assertEquals(0, new BigDecimal("9.6").compareTo(calculator.ratesFor(motorcycle, 3).get(2)));
    }

    @Test
    @DisplayName("a one-year loan never leaves the base rate")
    void singleYearLoanStaysAtBase() {
        List<BigDecimal> rates = calculator.ratesFor(CAR_BASE, 1);

        assertEquals(1, rates.size());
        assertEquals(0, CAR_BASE.compareTo(rates.get(0)));
    }

    @ParameterizedTest(name = "year {0} is rejected")
    @ValueSource(ints = {0, -1})
    @DisplayName("a non-positive year is rejected")
    void rejectsNonPositiveYear(int year) {
        assertThrows(InvalidInputException.class, () -> calculator.rateFor(CAR_BASE, year));
    }

    @Test
    @DisplayName("a non-positive tenure is rejected")
    void rejectsNonPositiveTenure() {
        assertThrows(InvalidInputException.class, () -> calculator.ratesFor(CAR_BASE, 0));
    }

    @Test
    @DisplayName("a null base rate is rejected")
    void rejectsNullBaseRate() {
        assertThrows(InvalidInputException.class, () -> calculator.rateFor(null, 1));
    }
}
