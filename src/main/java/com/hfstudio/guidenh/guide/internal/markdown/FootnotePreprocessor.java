package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public class FootnotePreprocessor {

    private static final Pattern DEFINITION_START = Pattern.compile("^\\[\\^([^\\]]+)]:(.*)$");
    private static final Pattern REFERENCE = Pattern.compile("\\[\\^([^\\]]+)]");

    protected FootnotePreprocessor() {}

    public static String preprocess(String markdown) {
        if (markdown == null || !markdown.contains("[^")) {
            return markdown;
        }

        List<String> lines = GuideStringLines.splitLines(markdown);
        Map<String, String> definitions = new LinkedHashMap<>();
        StringBuilder body = new StringBuilder(markdown.length());

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = DEFINITION_START.matcher(line);
            if (!matcher.matches()) {
                appendLine(body, line);
                continue;
            }

            String id = matcher.group(1)
                .trim();
            StringBuilder definition = new StringBuilder(
                matcher.group(2)
                    .trim());
            while (i + 1 < lines.size()) {
                String next = lines.get(i + 1);
                if (next.startsWith("    ") || next.startsWith("\t")) {
                    if (!definition.isEmpty()) {
                        definition.append('\n');
                    }
                    definition.append(trimDefinitionIndent(next));
                    i++;
                    continue;
                }
                if (next.isEmpty()) {
                    if (i + 2 < lines.size()) {
                        String afterBlank = lines.get(i + 2);
                        if (afterBlank.startsWith("    ") || afterBlank.startsWith("\t")) {
                            definition.append("\n\n");
                            i += 2;
                            definition.append(trimDefinitionIndent(afterBlank));
                            continue;
                        }
                    }
                }
                break;
            }
            // Keep first definition; ignore subsequent definitions with the same id
            definitions.putIfAbsent(id, definition.toString());
        }

        String transformedBody = replaceReferences(body.toString(), definitions);
        if (definitions.isEmpty()) {
            return transformedBody;
        }

        StringBuilder result = new StringBuilder(transformedBody.length() + 64);
        result.append(transformedBody);
        if (!result.isEmpty() && result.charAt(result.length() - 1) != '\n') {
            result.append('\n');
        }
        if (!result.isEmpty()) {
            result.append('\n');
        }
        result.append("<FootnoteList width=\"0\">\n\n");
        result.append("## Footnotes\n\n");
        int index = 1;
        for (var entry : definitions.entrySet()) {
            result.append(index)
                .append(". ")
                .append(entry.getValue())
                .append('\n');
            index++;
        }
        result.append('\n');
        result.append("</FootnoteList>\n");
        return result.toString();
    }

    private static String replaceReferences(String body, Map<String, String> definitions) {
        if (body.isEmpty()) {
            return body;
        }

        List<String> lines = GuideStringLines.splitLines(body);
        StringBuilder result = new StringBuilder(body.length());
        int nextNumber = 1;
        Map<String, Integer> numbers = new LinkedHashMap<>();

        for (String line : lines) {
            String trimmed = line.trim();
            // Skip Expected: and INVARIANTS lines — they are test/spec documentation, not page content
            if (trimmed.startsWith("Expected:") || trimmed.startsWith("INVARIANTS")) {
                appendLine(result, line);
                continue;
            }

            Matcher matcher = REFERENCE.matcher(line);
            StringBuilder sb = new StringBuilder(line.length());
            while (matcher.find()) {
                String id = matcher.group(1)
                    .trim();

                // Undefined references: leave as-is, consume no number
                if (!definitions.containsKey(id)) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                    continue;
                }

                Integer number = numbers.get(id);
                if (number == null) {
                    number = nextNumber++;
                    numbers.put(id, number);
                }

                String replacement = "<sup>[" + number + "]</sup>";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            appendLine(result, sb.toString());
        }
        return result.toString();
    }

    private static String trimDefinitionIndent(String line) {
        if (line.startsWith("\t")) {
            return line.substring(1);
        }
        if (line.startsWith("    ")) {
            return line.substring(4);
        }
        return line;
    }

    private static void appendLine(StringBuilder builder, String line) {
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(line);
    }
}
