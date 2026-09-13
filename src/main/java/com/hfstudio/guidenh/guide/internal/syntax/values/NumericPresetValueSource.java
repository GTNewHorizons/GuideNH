package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/** Suggests common numeric values for number attributes, keyed by attribute name so that a width offers widths. */
public class NumericPresetValueSource implements SyntaxValueSource {

    private static final Map<String, String[]> PRESETS = createPresets();

    private static Map<String, String[]> createPresets() {
        Map<String, String[]> presets = new LinkedHashMap<>();
        presets.put("width", new String[] { "100", "200", "256", "300", "400", "512" });
        presets.put("height", new String[] { "100", "200", "256", "300", "400" });
        presets.put("scale", new String[] { "0.5", "1.0", "1.5", "2.0", "3.0" });
        presets.put("zoom", new String[] { "0.5", "1.0", "1.5", "2.0", "3.0" });
        presets.put("gap", new String[] { "0", "5", "10", "15", "20" });
        presets.put("x", new String[] { "0" });
        presets.put("y", new String[] { "0" });
        presets.put("z", new String[] { "0" });
        presets.put("startAngle", new String[] { "-90", "0", "90", "180" });
        presets.put("barWidthRatio", new String[] { "0.5", "0.7", "0.9" });
        return Collections.unmodifiableMap(presets);
    }

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.INT, SyntaxValueKind.FLOAT);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        String[] values = PRESETS.get(request.attributeName());
        if (values == null) {
            return List.of();
        }
        String partial = request.partialText();
        String typed = partial != null ? partial : "";
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (String value : values) {
            if (results.size() >= limit) {
                break;
            }
            if (typed.isEmpty() || value.startsWith(typed)) {
                results.add(SyntaxSuggestion.of(value));
            }
        }
        return results;
    }
}
