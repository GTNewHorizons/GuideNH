package com.hfstudio.guidenh.guide.units;

import java.math.BigDecimal;

/** Formats an amount for one registered unit family. */
public interface AmountUnitFormatter {

    String format(BigDecimal value, String format, Integer decimalPlaces);
}
