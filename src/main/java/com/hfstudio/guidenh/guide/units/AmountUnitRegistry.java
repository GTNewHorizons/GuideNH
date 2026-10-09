package com.hfstudio.guidenh.guide.units;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AmountUnitRegistry {

    private static final ConcurrentMap<String, AmountUnitFormatter> FORMATTERS = new ConcurrentHashMap<>();

    static {
        register("none", AmountFormatter::formatNumber);
        register("item", AmountFormatter::formatNumber);
        register("fluid", AmountFormatter::formatFluid);
    }

    private AmountUnitRegistry() {}

    public static void register(String unit, AmountUnitFormatter formatter) {
        String normalized = normalize(unit);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Unit name must not be empty");
        }
        if (formatter == null) {
            throw new IllegalArgumentException("Unit formatter must not be null");
        }
        FORMATTERS.put(normalized, formatter);
    }

    public static AmountUnitFormatter require(String unit) {
        AmountUnitFormatter formatter = FORMATTERS.get(normalize(unit));
        if (formatter == null) {
            throw new IllegalArgumentException("Unknown amount unit: " + unit);
        }
        return formatter;
    }

    public static Map<String, AmountUnitFormatter> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(FORMATTERS));
    }

    public static String normalize(String unit) {
        return unit == null || unit.trim()
            .isEmpty() ? "none"
                : unit.trim()
                    .toLowerCase(Locale.ROOT);
    }
}
