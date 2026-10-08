package com.hfstudio.guidenh.guide.units;

import java.math.BigDecimal;
import java.util.Locale;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.CompactOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;

public class AmountFormatter {

    public static String format(String rawValue, String unit, String format, Integer decimalPlaces) {
        BigDecimal value = parse(rawValue);
        return AmountUnitRegistry.require(unit)
            .format(value, normalizeFormat(format), decimalPlaces);
    }

    public static BigDecimal parse(String rawValue) {
        if (rawValue == null || rawValue.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Amount value must not be empty");
        }
        try {
            return new BigDecimal(rawValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Amount value must be a finite decimal number: " + rawValue);
        }
    }

    public static String formatNumber(BigDecimal value, String format, Integer decimalPlaces) {
        String mode = normalizeFormat(format);
        if ("compact".equals(mode)) {
            return decimalPlaces == null ? NumberFormatUtil.formatNumberCompact(value)
                : NumberFormatUtil.formatNumberCompact(value, compactOptions(decimalPlaces));
        }
        FormatOptions options = formatOptions(mode, decimalPlaces);
        return "default".equals(mode) && decimalPlaces == null ? NumberFormatUtil.formatNumber(value)
            : NumberFormatUtil.formatNumber(value, options);
    }

    public static String formatFluid(BigDecimal value, String format, Integer decimalPlaces) {
        String mode = normalizeFormat(format);
        if ("compact".equals(mode)) {
            return decimalPlaces == null ? NumberFormatUtil.formatFluidCompact(value)
                : NumberFormatUtil.formatFluidCompact(value, compactOptions(decimalPlaces));
        }
        FormatOptions options = formatOptions(mode, decimalPlaces);
        return "default".equals(mode) && decimalPlaces == null ? NumberFormatUtil.formatFluid(value)
            : NumberFormatUtil.formatFluid(value, options);
    }

    public static String normalizeFormat(String format) {
        String normalized = format == null || format.trim()
            .isEmpty() ? "default"
                : format.trim()
                    .toLowerCase(Locale.ROOT);
        if (!normalized.equals("default") && !normalized.equals("plain")
            && !normalized.equals("compact")
            && !normalized.equals("scientific")) {
            throw new IllegalArgumentException("Unknown amount format: " + format);
        }
        return normalized;
    }

    private static FormatOptions formatOptions(String mode, Integer decimalPlaces) {
        FormatOptions options = new FormatOptions();
        if (decimalPlaces != null) {
            options.setDecimalPlaces(decimalPlaces);
        }
        if ("plain".equals(mode)) {
            options.disableExponentialFormatting();
        } else if ("scientific".equals(mode)) {
            options.setExponentialThreshold(BigDecimal.ZERO);
        }
        return options;
    }

    private static CompactOptions compactOptions(Integer decimalPlaces) {
        CompactOptions options = new CompactOptions();
        options.setDecimalPlaces(decimalPlaces);
        return options;
    }
}
