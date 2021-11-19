package com.bgsoftware.wildchests.utils;

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
            return format(bigDecimal.divide(Q, 2, RoundingMode.HALF_UP));

        if (bigDecimal.compareTo(T) >= 0)
            return format(bigDecimal.divide(T, 2, RoundingMode.HALF_UP));

        if (bigDecimal.compareTo(B) >= 0)
            return format(bigDecimal.divide(B, 2, RoundingMode.HALF_UP));

        if (bigDecimal.compareTo(M) >= 0)
            return format(bigDecimal.divide(M, 2, RoundingMode.HALF_UP));

        if (bigDecimal.compareTo(K) >= 0)
            return format(bigDecimal.divide(K, 2, RoundingMode.HALF_UP));

        return format(bigDecimal);
    }

}
