package com.hfstudio.guidenh.guide.internal.mermaid;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDirection;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutMode;

/**
 * Recognized frontmatter configuration keys for Mermaid flowcharts.
 * <pre>{@code
 * ---
 * direction: LR
 * layout: elk
 * ---
 * }</pre>
 */
public enum FrontmatterKey {

    DIRECTION("direction") {
        @Override
        public Object parse(String value) {
            return FlowchartDirection.fromString(value);
        }
    },
    LAYOUT("layout") {
        @Override
        public Object parse(String value) {
            return FlowchartLayoutMode.fromConfigValue(value);
        }
    };

    private final String key;

    FrontmatterKey(String key) {
        this.key = key;
    }

    public String key() { return key; }

    /** Parse the given string value into the typed configuration object. */
    @Nullable
    public abstract Object parse(String value);

    /** Look up a {@link FrontmatterKey} by its YAML key name, case-insensitive. */
    @Nullable
    public static FrontmatterKey byKey(String key) {
        if (key == null) return null;
        for (FrontmatterKey fk : values()) {
            if (fk.key.equalsIgnoreCase(key.trim())) return fk;
        }
        return null;
    }
}
