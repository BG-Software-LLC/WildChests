package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.Locale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class StringUtils {

    private static final DecimalFormat numberFormatter = new DecimalFormat("###,###,###,###,###,###,###,###,###,##0.00");
    private static final BigDecimal K = BigDecimal.valueOf(1000), M = K.multiply(K), B = M.multiply(K),
            T = B.multiply(K), Q = T.multiply(K);

    public static String format(BigDecimal bigDecimal) {
        String s = numberFormatter.format(Double.parseDouble(bigDecimal.toString()));
        return s.endsWith(".00") ? s.replace(".00", "") : s;
    }

    public static String fancyFormat(BigDecimal bigDecimal) {
        if (bigDecimal.compareTo(Q) >= 0)
            return format(bigDecimal.divide(Q, 2, RoundingMode.HALF_UP)) + Locale.FORMAT_QUAD.getMessage();

        if (bigDecimal.compareTo(T) >= 0)
            return format(bigDecimal.divide(T, 2, RoundingMode.HALF_UP)) + Locale.FORMAT_TRILLION.getMessage();

        if (bigDecimal.compareTo(B) >= 0)
            return format(bigDecimal.divide(B, 2, RoundingMode.HALF_UP)) + Locale.FORMAT_BILLION.getMessage();

        if (bigDecimal.compareTo(M) >= 0)
            return format(bigDecimal.divide(M, 2, RoundingMode.HALF_UP)) + Locale.FORMAT_MILLION.getMessage();

        if (bigDecimal.compareTo(K) >= 0)
            return format(bigDecimal.divide(K, 2, RoundingMode.HALF_UP)) + Locale.FORMAT_THOUSANDS.getMessage();

        return format(bigDecimal);
    }

    public static String format(String format) {
        String[] words = format.split(" ");
        String[] formattedWords = new String[words.length];

        for (int i = 0; i < words.length; ++i) {
            String curr = words[i];
            formattedWords[i] = Character.toUpperCase(curr.charAt(0)) + curr.substring(1).toLowerCase(java.util.Locale.ENGLISH);
        }

        return String.join(" ", formattedWords);
    }

}
