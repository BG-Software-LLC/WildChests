package com.bgsoftware.wildchests.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public final class StringUtils {

    private static DecimalFormat numberFormatter = new DecimalFormat("###,###,###,###,###,###,###,###,###,##0.00");

    public static String format(BigDecimal bigDecimal){
        String s = numberFormatter.format(Double.parseDouble(bigDecimal.toString()));
        return s.endsWith(".00") ? s.replace(".00", "") : s;
    }

}
