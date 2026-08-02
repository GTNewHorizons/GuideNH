package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public final class MermaidNodeRenderer {

    private MermaidNodeRenderer() {}

    public static final int DEFAULT_ACCENT = 0xFF7AA2F7;
    public static final int ACCENT_DANGER = 0xFFF7768E;
    public static final int ACCENT_SUCCESS = 0xFF9ECE6A;
    public static final int ACCENT_WARN = 0xFFE0AF68;
    public static final int ACCENT_MUTED = 0xFF8B949E;
    public static final int ACCENT_CIRCLE = 0xFF7DCFFF;
    public static final int ACCENT_CLOUD = 0xFF73DACA;

    public static final int DEFAULT_BACKGROUND = 0xFF1F2A38;
    public static final int ALT_BACKGROUND = 0xFF111922;

    public static final int BADGE_BACKGROUND = 0x262A3340;
    public static final int BADGE_BORDER = 0x66434C57;

    public record NodeColors(int background, int border, int accent) {}

    public static int resolveAccentColor(List<String> classes, MermaidNodeShape shape) {
        int accent = DEFAULT_ACCENT;
        for (String className : classes) {
            String lower = className.toLowerCase();
            if (lower.contains("danger") || lower.contains("error")
                || lower.contains("urgent")
                || lower.contains("red")) {
                accent = ACCENT_DANGER;
                break;
            }
            if (lower.contains("success") || lower.contains("green") || lower.contains("done")) {
                accent = ACCENT_SUCCESS;
                break;
            }
            if (lower.contains("warn") || lower.contains("yellow") || lower.contains("amber")) {
                accent = ACCENT_WARN;
                break;
            }
            if (lower.contains("muted") || lower.contains("gray") || lower.contains("grey")) {
                accent = ACCENT_MUTED;
            }
        }

        accent = switch (shape) {
            case CIRCLE, DOUBLE_CIRCLE -> ACCENT_CIRCLE;
            case HEXAGON -> ACCENT_WARN;
            case CLOUD -> ACCENT_CLOUD;
            case BANG -> ACCENT_DANGER;
            default -> accent;
        };

        return accent;
    }

    public static NodeColors resolveNodeColors(List<String> classes, MermaidNodeShape shape, boolean isRoot) {
        int accent = resolveAccentColor(classes, shape);
        int background = isRoot ? DEFAULT_BACKGROUND : ALT_BACKGROUND;
        return new NodeColors(background, accent, accent);
    }

    public static ResolvedTextStyle scaleTextStyle(ResolvedTextStyle base, float zoom) {
        return new ResolvedTextStyle(
            base.fontScale() * zoom,
            base.bold(),
            base.italic(),
            base.underlined(),
            base.wavyUnderline(),
            base.dottedUnderline(),
            base.strikethrough(),
            base.obfuscated(),
            base.font(),
            base.color(),
            base.whiteSpace(),
            base.alignment(),
            base.dropShadow(),
            base.backgroundColor(),
            base.inlineCode(),
            base.baselineShift());
    }

    public static ResolvedTextStyle getOrScaleStyle(Map<ResolvedTextStyle, ResolvedTextStyle> cache,
        ResolvedTextStyle base, float zoom) {
        return cache.computeIfAbsent(base, key -> scaleTextStyle(key, zoom));
    }

    public static String simplifyIcon(String icon) {
        if (icon == null || icon.trim()
            .isEmpty()) {
            return null;
        }

        String trimmed = icon.trim();
        String leaf = trimmed.substring(lastWhitespaceSeparatedTokenStart(trimmed));
        if (leaf.startsWith("fa-")) {
            leaf = leaf.substring(3);
        }
        leaf = leaf.replace('-', ' ')
            .trim();
        return leaf.isEmpty() ? trimmed : leaf;
    }

    private static int lastWhitespaceSeparatedTokenStart(String text) {
        int index = text.length() - 1;
        while (index >= 0 && !Character.isWhitespace(text.charAt(index))) {
            index--;
        }
        return index + 1;
    }

    @FunctionalInterface
    public interface AdvanceFunction {

        float getAdvance(int codePoint, ResolvedTextStyle style);
    }

    public static int measureText(LayoutContext context, ResolvedTextStyle style, String text) {
        return measureTextInternal(style, text, context::getAdvance);
    }

    public static int measureText(RenderContext context, ResolvedTextStyle style, String text) {
        return context.getStringWidth(text, style);
    }

    public static int measureTextInternal(ResolvedTextStyle style, String text, AdvanceFunction advance) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float width = 0f;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            width += advance.getAdvance(codePoint, style);
            offset += Character.charCount(codePoint);
        }
        return Math.round(width);
    }

    public static List<String> wrapText(LayoutContext context, ResolvedTextStyle style, String text, int maxWidth) {
        // A4 unified text pipeline: word-first wrapping + codepoint-level
        // breaking of overlong words, measured by GuideText (Rust font system).
        // LayoutContext is no longer used for measurement — GuideText.wrap
        // measures with its own GuideText adapters, which are the same source
        // as the former LayoutContext-based measurement (measurement-neutral).
        return GuideText.wrap(text, maxWidth, style);
    }
}
