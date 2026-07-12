package com.elgi.creditsimulator.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyFormat {

    private static final String CURRENCY_PREFIX = "Rp. ";

    private MoneyFormat() {
        throw new AssertionError("MoneyFormat is a utility class and must not be instantiated");
    }

    public static String rupiah(BigDecimal amount) {
        DecimalFormat format =
                new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return CURRENCY_PREFIX + format.format(amount);
    }

    public static String percent(BigDecimal rate) {
        String plain = rate.stripTrailingZeros().toPlainString();
        return plain.replace('.', ',');
    }
}
