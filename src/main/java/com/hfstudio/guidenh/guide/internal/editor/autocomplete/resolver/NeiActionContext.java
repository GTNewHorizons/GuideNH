package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;

public record NeiActionContext(int replaceStart, int replaceEnd, String partialText,
    @Nullable SyntaxValueKind valueKind, List<String> choices) implements AutocompleteContext {

    private static final Pattern ACTION = Pattern.compile("&\\[[^\\]\\r\\n]*\\]\\(");

    @Override
    public String getPartialText() {
        return partialText;
    }

    @Nullable
    public static NeiActionContext resolve(String text, int offset) {
        int lineStart = text.lastIndexOf('\n', offset - 1) + 1;
        Matcher matcher = ACTION.matcher(text.substring(lineStart, offset));
        int start = -1;
        while (matcher.find()) {
            int actionStart = lineStart + matcher.start();
            int escapes = 0;
            for (int index = actionStart - 1; index >= 0 && text.charAt(index) == '\\'; index--) escapes++;
            if (escapes % 2 == 0) start = lineStart + matcher.end();
        }
        if (start < 0) return null;
        int end = start;
        while (end < text.length() && text.charAt(end) != ')' && !Character.isWhitespace(text.charAt(end))) end++;
        if (offset > end) return null;
        String uri = text.substring(start, end);
        String typed = text.substring(start, offset);
        String normalized = typed.toLowerCase(Locale.ROOT);
        if (!normalized.isEmpty() && !"nei:".startsWith(normalized) && !normalized.startsWith("nei:")) return null;
        int query = uri.indexOf('?');
        if (query < 0 || offset <= start + query) {
            return new NeiActionContext(
                start,
                query < 0 ? end : start + query,
                typed,
                null,
                List.of("nei:handler", "nei:recipes", "nei:usage")
                    .stream()
                    .map(mode -> mode + (query < 0 ? "?" : ""))
                    .toList());
        }
        String mode = uri.substring(4, query)
            .toLowerCase(Locale.ROOT);
        int segmentStart = Math.max(start + query, text.lastIndexOf('&', offset - 1)) + 1;
        int segmentEnd = segmentStart;
        while (segmentEnd < end && text.charAt(segmentEnd) != '&') segmentEnd++;
        int equals = text.indexOf('=', segmentStart);
        if (equals < segmentEnd && equals >= segmentStart && offset > equals) {
            String key = text.substring(segmentStart, equals);
            SyntaxValueKind kind = key.equals("handlerId") ? SyntaxValueKind.RECIPE_HANDLER_ID
                : key.equals("id") && !mode.equals("handler") ? SyntaxValueKind.ITEM_ID : null;
            String prefix;
            try {
                prefix = URLDecoder.decode(
                    text.substring(equals + 1, offset)
                        .replace("+", "%2B"),
                    StandardCharsets.UTF_8);
            } catch (IllegalArgumentException exception) {
                prefix = text.substring(equals + 1, offset);
            }
            return new NeiActionContext(equals + 1, segmentEnd, prefix, kind, List.of());
        }
        Set<String> existing = new HashSet<>();
        int currentSegment = segmentStart - start;
        int relativeStart = query + 1;
        for (String segment : uri.substring(query + 1)
            .split("&", -1)) {
            if (relativeStart != currentSegment) existing.add(segment.split("=", 2)[0]);
            relativeStart += segment.length() + 1;
        }
        List<String> choices = new ArrayList<>();
        for (String key : mode.equals("handler") ? List.of("handlerId") : List.of("id", "handlerId")) {
            if (!existing.contains(key)) choices.add(key + (equals >= segmentStart && equals < segmentEnd ? "" : "="));
        }
        return new NeiActionContext(
            segmentStart,
            equals >= segmentStart && equals < segmentEnd ? equals : segmentEnd,
            text.substring(segmentStart, offset),
            null,
            choices);
    }
}
