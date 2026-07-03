package com.hfstudio.guidenh.guide.internal.mermaid.mindmap;

import java.util.List;

import com.hfstudio.guidenh.guide.internal.mermaid.MermaidSourceExtractor;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.MdxBlockTagSourceExtractor;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

public class MindmapNodeContentExtractor {

    private MindmapNodeContentExtractor() {}

    public static @Nullable String extractDiagramSource(MdxJsxElementFields element, @Nullable String pageSource) {
        String source = extractBlockTagChildrenSource(element, pageSource);
        if (source != null && !source.trim()
            .isEmpty()) {
            return MermaidSourceExtractor.stripExplicitNodeContentBlocks(source);
        }

        source = extractChildrenSource(element, pageSource);
        if (source != null && !source.trim()
            .isEmpty()) {
            return MermaidSourceExtractor.stripExplicitNodeContentBlocks(source);
        }
        return null;
    }

    private static @Nullable String extractBlockTagChildrenSource(MdxJsxElementFields element,
        @Nullable String pageSource) {
        if (element == null || pageSource == null || pageSource.isEmpty()) {
            return null;
        }

        String body = MdxBlockTagSourceExtractor.extractRawBody(element, pageSource);
        if (body == null) {
            return null;
        }

        return dedentBlockTagBody(body);
    }

    private static @Nullable String extractChildrenSource(MdxJsxElementFields element, @Nullable String pageSource) {
        if (element == null || pageSource == null || pageSource.isEmpty()) {
            return null;
        }

        List<? extends MdAstAnyContent> children = element.children();
        if (children == null || children.isEmpty()) {
            return null;
        }

        UnistPosition firstPosition = null;
        UnistPosition lastPosition = null;
        for (MdAstAnyContent child : children) {
            UnistPosition position = child.position();
            if (position == null || position.start() == null || position.end() == null) {
                return null;
            }
            if (firstPosition == null) {
                firstPosition = position;
            }
            lastPosition = position;
        }

        int sourceStart = firstPosition.start()
            .offset();
        int sourceEnd = lastPosition.end()
            .offset();
        if (sourceStart < 0 || sourceEnd <= sourceStart || sourceEnd > pageSource.length()) {
            return null;
        }
        return dedentBlockTagBody(pageSource.substring(sourceStart, sourceEnd));
    }

    private static String dedentBlockTagBody(String body) {
        String normalized = GuideStringLines.normalizeLineEndings(body);
        if (normalized.isEmpty()) {
            return normalized;
        }

        List<String> lines = GuideStringLines.splitLines(normalized);
        int firstContentLine = 0;
        while (firstContentLine < lines.size() && lines.get(firstContentLine)
            .trim()
            .isEmpty()) {
            firstContentLine++;
        }

        int minIndent = Integer.MAX_VALUE;
        for (int index = firstContentLine; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.trim()
                .isEmpty()) {
                continue;
            }
            minIndent = Math.min(minIndent, leadingWhitespaceWidth(line));
        }
        if (minIndent == Integer.MAX_VALUE) {
            minIndent = 0;
        }

        StringBuilder result = new StringBuilder(normalized.length());
        for (int index = firstContentLine; index < lines.size(); index++) {
            if (index > firstContentLine) {
                result.append('\n');
            }
            result.append(removeLeadingWhitespace(lines.get(index), minIndent));
        }

        while (!result.isEmpty() && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        return result.toString();
    }

    private static int leadingWhitespaceWidth(String line) {
        int width = 0;
        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (value == ' ') {
                width++;
            } else if (value == '\t') {
                width += 4;
            } else {
                break;
            }
        }
        return width;
    }

    private static String removeLeadingWhitespace(String line, int width) {
        if (width <= 0 || line.isEmpty()) {
            return line;
        }

        int index = 0;
        int remaining = width;
        while (index < line.length() && remaining > 0) {
            char value = line.charAt(index);
            if (value == ' ') {
                remaining--;
                index++;
            } else if (value == '\t') {
                remaining -= Math.min(remaining, 4);
                index++;
            } else {
                break;
            }
        }
        return line.substring(index);
    }
}
