package utils;

import com.elgi.creditsimulator.utils.MoneyFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyFormatTest {

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource({
            "3500000,      'Rp. 3,500,000.00'",
            "2641423.50,   'Rp. 2,641,423.50'",
            "1000000000,   'Rp. 1,000,000,000.00'",
            "0,            'Rp. 0.00'",
            "999,          'Rp. 999.00'",
            "1000,         'Rp. 1,000.00'"
    })
    @DisplayName("money is grouped with commas and always shows two decimals")
    void rupiah(BigDecimal amount, String expected) {
        assertEquals(expected, MoneyFormat.rupiah(amount));
    }

    @Test
    @DisplayName("money is rounded half-up to two decimals, never truncated")
    void rupiahRoundsHalfUp() {
        assertEquals("Rp. 2,641,423.50", MoneyFormat.rupiah(new BigDecimal("2641423.495")));
        assertEquals("Rp. 1.00", MoneyFormat.rupiah(new BigDecimal("0.995")));
    }

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource({
            "8.0,   8",
            "8.1,   '8,1'",
            "8.6,   '8,6'",
            "9.0,   9",
            "10.2,  '10,2'",
            "10.30, '10,3'"
    })
    @DisplayName("rates drop trailing zeros and use a comma for the decimal, matching the spec sample")
    void percent(BigDecimal rate, String expected) {
        assertEquals(expected, MoneyFormat.percent(rate));
    }

    @Test
    @DisplayName("the spec's sample line is reproducible from these two formatters")
    void reproducesSpecSampleLine() {
        // "tahun 2 : Rp. 3,501,000.00/bln , Suku Bunga : 8,1%"
        String line = String.format("tahun %d : %s/bln , Suku Bunga : %s%%",
                2,
                MoneyFormat.rupiah(new BigDecimal("3501000")),
                MoneyFormat.percent(new BigDecimal("8.1")));

        assertEquals("tahun 2 : Rp. 3,501,000.00/bln , Suku Bunga : 8,1%", line);
    }
}
