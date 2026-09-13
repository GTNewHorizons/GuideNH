package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

public class AttributePresetValueSource implements SyntaxValueSource {

    private static final Set<SyntaxValueKind> KINDS = Set
        .of(SyntaxValueKind.STRING, SyntaxValueKind.ENUM, SyntaxValueKind.INT, SyntaxValueKind.FLOAT);

    private static final Map<String, String[]> VALUES = buildValues();

    private static Map<String, String[]> buildValues() {
        Map<String, String[]> values = new LinkedHashMap<>();
        values.put("showIcon", new String[] { "left", "right", "true", "false" });
        values.put("showTooltip", new String[] { "true", "false" });
        values.put("showText", new String[] { "true", "false" });
        values.put("noTooltip", new String[] { "true", "false" });
        values.put("wrap", new String[] { "inline", "square", "tight", "through" });
        values.put("align", new String[] { "left", "center", "right" });
        values.put("float", new String[] { "none", "left", "right" });
        values.put("clear", new String[] { "none", "left", "right", "all" });
        values.put("valign", new String[] { "baseline", "top", "middle", "bottom" });
        values.put("position", new String[] { "topLeft", "topRight", "bottomLeft", "bottomRight", "center" });
        values.put("direction", new String[] { "clockwise", "counterclockwise" });
        values.put("perspective", new String[] { "isometric", "front", "back", "left", "right", "top", "bottom" });
        values.put("facing", new String[] { "north", "south", "west", "east", "up", "down" });
        values.put("rotation", new String[] { "normal", "clockwise_90", "clockwise_180", "counterclockwise_90" });
        values.put("flip", new String[] { "none", "front_back", "left_right" });
        values.put("background", new String[] { "transparent", "checker", "solid" });
        values.put("trigger", new String[] { "click", "hover" });
        values.put("target", new String[] { "self", "blank" });
        values.put("quadrants", new String[] { "all", "positive", "top", "right" });
        return Collections.unmodifiableMap(values);
    }

    @Override
    public Set<SyntaxValueKind> kinds() {
        return KINDS;
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String attribute = request.attributeName();
        if (attribute == null) {
            return Collections.emptyList();
        }
        String[] declared = VALUES.get(attribute);
        if (declared == null) {
            return Collections.emptyList();
        }
        String lower = request.partialText()
            .toLowerCase(Locale.ROOT);
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (String value : declared) {
            if (results.size() >= limit) {
                break;
            }
            if (lower.isEmpty() || value.toLowerCase(Locale.ROOT)
                .startsWith(lower)) {
                results.add(SyntaxSuggestion.of(value));
            }
        }
        return results;
    }
}
