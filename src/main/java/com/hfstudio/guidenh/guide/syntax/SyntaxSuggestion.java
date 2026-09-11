package com.hfstudio.guidenh.guide.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * A value offered for an attribute or frontmatter key.
 *
 * @param value    text written into the page
 * @param label    text shown in the popup; falls back to {@code value} when null
 * @param subtitle dimmed text shown after the label, for example a display name
 * @param icon     item rendered next to the label, or null
 */
public record SyntaxSuggestion(String value, @Nullable String label, @Nullable String subtitle,
    @Nullable ItemStack icon) {

    public static SyntaxSuggestion of(String value) {
        return new SyntaxSuggestion(value, null, null, null);
    }

    public static SyntaxSuggestion of(String value, @Nullable String subtitle) {
        return new SyntaxSuggestion(value, null, subtitle, null);
    }

    public static SyntaxSuggestion withValue(String value, @Nullable String label, @Nullable String subtitle) {
        return new SyntaxSuggestion(value, label, subtitle, null);
    }

    public static SyntaxSuggestion withIcon(String value, @Nullable ItemStack icon) {
        return new SyntaxSuggestion(value, null, null, icon);
    }

    /** Text shown in the popup. */
    public String displayText() {
        return label != null ? label : value;
    }

    /** All suggestions in {@code suggestions} whose value contains {@code partial}, case-insensitively. */
    public static List<SyntaxSuggestion> containing(List<SyntaxSuggestion> suggestions, String partial, int limit) {
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (SyntaxSuggestion suggestion : suggestions) {
            if (results.size() >= Math.max(0, limit)) {
                break;
            }
            if (lower.isEmpty() || suggestion.value()
                .toLowerCase(Locale.ROOT)
                .contains(lower)) {
                results.add(suggestion);
            }
        }
        return results;
    }
}
