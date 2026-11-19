package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.ScalarNode;

public class SyntaxContextValues {

    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w.-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    private static final Pattern YAML_PAIR = Pattern.compile("(?m)^\\s*(?:-\\s*)?([\\w.-]+):[ \\t]*(.*)$");
    private static final Pattern BINDING_ENTRY = Pattern.compile("(?m)^[ \\t]+-[ \\t]+");
    private static final Pattern BINDING_FLOW = Pattern.compile("^[ \\t]*-[ \\t]+\\{");
    private static final Pattern TOP_LEVEL = Pattern.compile("(?m)^\\S");
    private static final Pattern FLOW_PAIR = Pattern.compile(
        "(?:[\\{,])\\s*(type|id|anchor)\\s*:\\s*(\"(?:\\\\.|[^\"\\\\\\r\\n])*\"?|'(?:''|[^'\\r\\n])*'?|[^,}\\r\\n]*)");

    public record BindingField(String name, String prefix, int start, int end, String quote,
        Map<String, String> contextValues) {}

    protected SyntaxContextValues() {}

    public static Map<String, String> attributes(String text, int cursor) {
        int start = text.lastIndexOf('<', cursor);
        if (start < 0) return Map.of();
        int end = text.indexOf('>', cursor);
        var result = new LinkedHashMap<String, String>();
        var matcher = ATTRIBUTE.matcher(text.substring(start, end < 0 ? text.length() : end));
        while (matcher.find())
            result.put(matcher.group(1), matcher.group(2) != null ? matcher.group(2) : matcher.group(3));
        return Map.copyOf(result);
    }

    public static Map<String, String> bindingEntry(String text, int cursor) {
        int section = text.lastIndexOf("\nbindings:", cursor);
        if (section < 0) return Map.of();
        int closing = text.indexOf("\n---", section);
        if (closing < cursor) return Map.of();
        int sectionEnd = closing + 1;
        var topLevel = TOP_LEVEL.matcher(text);
        if (topLevel.find(section + 2)) sectionEnd = Math.min(sectionEnd, topLevel.start());
        if (cursor >= sectionEnd) return Map.of();
        var entries = BINDING_ENTRY.matcher(text);
        entries.region(section, sectionEnd);
        int start = -1, end = sectionEnd;
        while (entries.find()) {
            if (entries.start() > cursor) {
                end = entries.start();
                break;
            }
            start = entries.start();
        }
        if (start < 0) return Map.of();
        var result = new LinkedHashMap<String, String>();
        String entry = text.substring(start, end);
        if (BINDING_FLOW.matcher(entry)
            .find()) {
            var flow = FLOW_PAIR.matcher(entry);
            int previousEnd = 0;
            while (flow.find()) {
                if (flowBoundary(entry, previousEnd, flow.start())) break;
                result.put(flow.group(1), scalar(flow.group(2)));
                previousEnd = flow.end();
            }
            result.put("section", "bindings");
            return Map.copyOf(result);
        }
        var pairs = YAML_PAIR.matcher(entry);
        while (pairs.find()) {
            result.put(pairs.group(1), scalar(pairs.group(2)));
        }
        result.put("section", "bindings");
        return Map.copyOf(result);
    }

    public static BindingField bindingField(String text, int cursor) {
        var context = bindingEntry(text, cursor);
        if (context.isEmpty()) return null;
        int lineStart = text.lastIndexOf('\n', cursor - 1) + 1;
        int lineEnd = text.indexOf('\n', cursor);
        if (lineEnd < 0) lineEnd = text.length();
        String line = text.substring(lineStart, lineEnd);
        if (!BINDING_FLOW.matcher(line)
            .find()) return null;
        var fields = FLOW_PAIR.matcher(line);
        int previousEnd = 0;
        while (fields.find()) {
            if (flowBoundary(line, previousEnd, fields.start())) break;
            previousEnd = fields.end();
            int start = lineStart + fields.start(2), end = lineStart + fields.end(2);
            String quote = "";
            if (start < end && (text.charAt(start) == '"' || text.charAt(start) == '\'')) {
                quote = text.substring(start, start + 1);
                start++;
                if (end > start && text.substring(end - 1, end)
                    .equals(quote)) end--;
            }
            if (cursor >= start && cursor <= end) return new BindingField(
                fields.group(1),
                quote.isEmpty() ? text.substring(start, cursor) : scalar(quote + text.substring(start, cursor) + quote),
                start,
                end,
                quote,
                context);
        }
        return null;
    }

    private static boolean flowBoundary(String text, int start, int end) {
        for (int index = start; index < end; index++) {
            char character = text.charAt(index);
            if (character == '}' || character == '#') return true;
        }
        return false;
    }

    private static String scalar(String text) {
        try {
            var node = new Yaml().compose(new StringReader(text));
            return node instanceof ScalarNode value ? value.getValue() : text.trim();
        } catch (YAMLException exception) {
            return text.trim()
                .replaceFirst("^[\"']", "");
        }
    }
}
