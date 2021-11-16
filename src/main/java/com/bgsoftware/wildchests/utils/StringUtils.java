package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.Locale;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public final class StringUtils {

    private static DecimalFormat numberFormatter = new DecimalFormat("###,###,###,###,###,###,###,###,###,##0.00");
    private static final long Q = 1_000_000_000_000_000L, T = 1_000_000_000_000L, B = 1_000_000_000L, M = 1_000_000L, K = 1_000L;

    public static String format(BigDecimal bigDecimal){
        String s = numberFormatter.format(Double.parseDouble(bigDecimal.toString()));
        return s.endsWith(".00") ? s.replace(".00", "") : s;
    }

    public static String format(double d)
    {
        return format(BigDecimal.valueOf(d));
    }

    public static String fancyFormat(BigDecimal bigDecimal){
        double d = bigDecimal.longValue();
        if(d >= Q)
            return format((d / Q)) + Locale.FORMAT_QUAD.getMessage();
        else if(d >= T)
            return format((d / T)) + Locale.FORMAT_TRILLION.getMessage();
        else if(d >= B)
            return format((d / B)) + Locale.FORMAT_BILLION.getMessage();
        else if(d >= M)
            return format((d / M)) + Locale.FORMAT_MILLION.getMessage();
        else if(d >= K)
            return format((d / K)) + Locale.FORMAT_THOUSANDS.getMessage();
        else
            return format(d);
    }
}
