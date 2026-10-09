package com.smartmess.android.utils;

import java.text.DecimalFormat;
import java.util.Locale;

public class CurrencyUtils {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");
    public static final String SYMBOL = "৳ ";

    public static String format(double amount) {
        return SYMBOL + FORMAT.format(amount);
    }

    public static String formatWithoutSymbol(double amount) {
        return FORMAT.format(amount);
    }
}
