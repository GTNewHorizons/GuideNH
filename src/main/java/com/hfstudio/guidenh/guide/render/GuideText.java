package com.hfstudio.guidenh.guide.render;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.layout.LayoutBridge;
import com.hfstudio.guidenh.guide.layout.flatbuffers.ShapeTextInput;
import com.hfstudio.guidenh.guide.layout.flatbuffers.ShapeTextResult;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextStyle;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Unified text pipeline entry — one font, one entry point, one coordinate
 * system (一个字体、一个入口、一个坐标系).
 *
 * <p>
 * All measurement (width / line height / baseline) and all text emission in
 * the primitive pipeline go through this class, backed by the single Rust
 * font system (cosmic-text over the system TTF). No block may reach for
 * {@code Minecraft.getMinecraft().fontRenderer} or stash a LayoutContext to
 * do text work.
 *
 * <p>
 * <b>Coordinate contract.</b> Text coordinates are document-space with the
 * origin at the <em>line top</em>. The text baseline is
 * {@code lineTop + ascent(style)}; {@link #ascent()} is the single baseline
 * authority (cosmic font metrics at the base em size). Minecraft's legacy
 * "top + 7" convention exists only inside HostDraw legacy rendering, never
 * here.
 */
public final class GuideText {

    /** Base em size for guide body text (typography P1: 9 → 11). */
    public static final float BASE_FONT_SIZE = 11f;
    /** Line height at scale 1 = BASE_FONT_SIZE × 1.55 (round(17.05) = 17;
     * mirrored by parley_text.rs push_defaults FontSizeRelative(1.55)). */
    public static final int BASE_LINE_HEIGHT = 17;

    /**
     * Cache key includes the display pixel ratio: bitmaps are rasterized per
     * render scale, so a GUI-scale change must not hit stale entries (C-5).
     */
    private record ShapeKey(String text, boolean bold, boolean italic, float fontScale, int renderScale) {}

    private record AdvanceKey(int codePoint, boolean bold, boolean italic, float fontScale, int renderScale) {}

    private static final int CACHE_LIMIT = 4096;
    private static final Map<ShapeKey, ShapeTextResult> shapeCache = new ConcurrentHashMap<>();
    private static final Map<AdvanceKey, Float> advanceCache = new ConcurrentHashMap<>();
    private static volatile Float cachedBaseAscent;

    private GuideText() {}

    /** True when the Rust font system is initialized (in-game; false in some tests). */
    public static boolean isAvailable() {
        return LayoutBridge.getFontHandle() != 0;
    }

    /** Measured advance width of {@code text} at the given style (cached). */
    public static int measureWidth(@Nullable String text, @Nullable ResolvedTextStyle style) {
        if (text == null || text.isEmpty() || !isAvailable()) {
            return 0;
        }
        return Math.round(shape(text, style).width());
    }

    /** Line height: 17 × fontScale (11px base × 1.55 line-height ratio). */
    public static int lineHeight(@Nullable ResolvedTextStyle style) {
        float scale = style != null ? style.fontScale() : 1f;
        return Math.max(1, Math.round(BASE_LINE_HEIGHT * scale));
    }

    /**
     * Baseline offset below the line top at scale 1 (cosmic font metrics at
     * {@link #BASE_FONT_SIZE}). Multiply by fontScale for scaled styles.
     */
    public static float ascent() {
        Float cached = cachedBaseAscent;
        if (cached != null) {
            return cached;
        }
        if (!isAvailable()) {
            // Same convention as the legacy MC cell: baseline ≈ top + 7 of 9.
            return 7f;
        }
        float a = shape("x", null).ascent();
        cachedBaseAscent = a;
        return a;
    }

    /**
     * Body x-height at scale 1, read from the Rust shape pipeline (T4): the
     * first run's skrifa OS/2 {@code sxHeight} at the base em size. Unlike
     * {@link #ascent()} (the font ascent, ≈0.75-0.85em — the top of tall
     * letters/ascenders), this measures a lower-case "x", i.e. the height
     * lowercase body letters actually occupy. Multiply by fontScale for scaled
     * styles.
     */
    public static float xHeight() {
        return shape("x", null).xHeight();
    }

    /** Baseline Y for a line whose top is {@code lineTop} at the given style. */
    public static float baselineOf(float lineTop, @Nullable ResolvedTextStyle style) {
        float scale = style != null ? style.fontScale() : 1f;
        return lineTop + ascent() * scale;
    }

    /**
     * Emit {@code text} as an atlas-backed glyph run at document position
     * {@code (x, y)} (line-top origin), tinted with the style's color.
     * <p>
     * Shaping results are cached by (text, style); atlas bitmaps dedupe by
     * content key, so per-frame emission is cheap. Decorations (underline /
     * strikethrough / backgrounds) are NOT emitted here — rich-text spans are
     * handled by the span pipeline (phase 3).
     */
    public static void emitText(PrimitiveCollector c, String text, int x, int y, @Nullable ResolvedTextStyle style) {
        if (text == null || text.isEmpty() || !isAvailable()) {
            return;
        }
        ShapeTextResult shaped = shape(text, style);
        var atlas = GuideGlyphAtlas.instance();
        for (int i = 0; i < shaped.bitmapsLength(); i++) {
            var bmp = shaped.bitmaps(i);
            if (bmp == null || bmp.rgbaLength() == 0) continue;
            byte[] rgba = new byte[bmp.rgbaLength()];
            for (int j = 0; j < rgba.length; j++) {
                rgba[j] = (byte) bmp.rgba(j);
            }
            atlas.upload(bmp.key(), rgba, (int) bmp.w(), (int) bmp.h());
        }
        int tex = atlas.getTextureId();
        int n = shaped.glyphsLength();
        if (n == 0) {
            return;
        }
        List<GuideRenderPrimitive.PlacedGlyph> glyphs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            var g = shaped.glyphs(i);
            if (g == null) continue;
            glyphs.add(
                new GuideRenderPrimitive.PlacedGlyph(
                    g.bitmapKey(),
                    x + g.x(),
                    y + g.y(),
                    g.w(),
                    g.h(),
                    (int) g.lineIndex()));
        }
        c.emit(
            new GuideRenderPrimitive.DrawGlyphRun(
                tex,
                glyphs,
                resolveColor(style),
                false,
                style != null && style.dropShadow()));
    }

    /** Per-codepoint advance (cached; used by RustFontMetrics). */
    public static float advanceOf(int codePoint, @Nullable ResolvedTextStyle style) {
        if (!isAvailable()) {
            return 0f;
        }
        boolean bold = style != null && style.bold();
        boolean italic = style != null && style.italic();
        float scale = style != null ? style.fontScale() : 1f;
        AdvanceKey key = new AdvanceKey(codePoint, bold, italic, scale, DisplayScale.scaleFactor());
        Float cached = advanceCache.get(key);
        if (cached != null) {
            return cached;
        }
        String s = new String(Character.toChars(codePoint));
        float w = shape(s, style).width();
        if (advanceCache.size() > CACHE_LIMIT) {
            advanceCache.clear();
        }
        advanceCache.put(key, w);
        return w;
    }

    /** Truncation suffix policy for {@link #clipToWidth} and {@link #clipToChars}. */
    public enum ClipSuffix {
        /** Hard truncation, no suffix appended. */
        NONE,
        /** ASCII three dots {@code "..."}. */
        DOTS3,
        /** Typographic ellipsis {@code "…"} (U+2026). */
        UNICODE_ELLIPSIS
    }

    /**
     * Word-first line wrapping: splits {@code text} on whitespace (line
     * breaks preserved first via {@link GuideStringLines#splitLines}, then
     * words within each line) and packs words into lines of at most
     * {@code maxWidth} pixels (measured with {@link #measureWidth}). A word
     * that does not fit is broken at codepoint granularity via
     * {@link #advanceOf} — codepoint-aware, so surrogate pairs are never split
     * (no {@code charAt} scanning). Output lines carry no leading or trailing
     * whitespace; empty input lines are dropped.
     *
     * <p><b>Degradation semantics:</b> when {@link #isAvailable()} is false no
     * measurement is possible, so the text is returned untouched as a single
     * line (no wrapping). Returns an empty list for {@code null} / empty input.
     */
    public static List<String> wrap(String text, int maxWidth, @Nullable ResolvedTextStyle style) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        if (!isAvailable()) {
            return List.of(text);
        }
        int budget = Math.max(1, maxWidth);
        List<String> lines = new ArrayList<>();
        for (String rawLine : GuideStringLines.splitLines(text)) {
            String line = rawLine != null ? rawLine.trim() : "";
            if (line.isEmpty()) {
                continue;
            }
            if (measureWidth(line, style) <= budget) {
                lines.add(line);
                continue;
            }
            String[] words = line.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (measureWidth(candidate, style) <= budget) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                appendBrokenWord(word, budget, style, lines);
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    /**
     * Pixel-level truncation: returns the longest codepoint prefix of
     * {@code text} whose rendered width, together with {@code suffix}, does
     * not exceed {@code maxWidth}.
     *
     * <p><b>Semantic invariant: the result's rendered width is
     * {@code <= maxWidth}.</b> The suffix width counts against the budget; when
     * the suffix alone does not fit ({@code suffixWidth > maxWidth}), or when
     * even a single codepoint plus the suffix overflows, the empty string is
     * returned (宁空勿溢 — rather empty than overflowing). When the full text
     * fits, it is returned unchanged.
     *
     * <p>Implementation: codepoints are accumulated via
     * {@link #advanceOf(int, ResolvedTextStyle)} — never {@code substring} +
     * {@link #measureWidth} shrink loops, which would create one unique shape
     * cache key per prefix and blow up shaping to O(n²). The final result is
     * then re-verified with {@link #measureWidth} and codepoints are backed off
     * until it fits, because the per-codepoint advance sum differs from the
     * shaped run by subpixels.
     *
     * <p><b>Degradation semantics:</b> when {@link #isAvailable()} is false no
     * measurement is possible, so the original text is returned untouched.
     */
    public static String clipToWidth(String text, int maxWidth, @Nullable ResolvedTextStyle style,
        ClipSuffix suffix) {
        if (!isAvailable()) {
            return text;
        }
        if (text == null || text.isEmpty()) {
            return "";
        }
        ClipSuffix eff = suffix != null ? suffix : ClipSuffix.NONE;
        String suffixText = suffixText(eff);
        int budget = Math.max(0, maxWidth);
        int suffixWidth = suffixText.isEmpty() ? 0 : measureWidth(suffixText, style);
        if (suffixWidth > budget) {
            return "";
        }
        if (measureWidth(text, style) <= budget) {
            return text;
        }
        int contentBudget = budget - suffixWidth;
        StringBuilder sb = new StringBuilder();
        float accumulated = 0f;
        int offset = 0;
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            accumulated += advanceOf(codePoint, style);
            if (accumulated > contentBudget) {
                break;
            }
            sb.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
        }
        if (sb.length() == 0) {
            // Even a single codepoint plus the suffix cannot fit.
            return "";
        }
        // Conservative backoff: advanceOf sums and shaped runs differ by
        // subpixels; drop codepoints until the measured width fits.
        while (sb.length() > 0 && measureWidth(sb.toString() + suffixText, style) > budget) {
            int last = sb.codePointBefore(sb.length());
            sb.delete(sb.length() - Character.charCount(last), sb.length());
        }
        return sb.length() == 0 ? "" : sb + suffixText;
    }

    /**
     * Character-level truncation by codepoint count (codepoint-aware; never
     * splits a surrogate pair). The suffix is counted against
     * {@code maxChars}: when the full text fits within the <em>complete</em>
     * budget ({@code maxChars}) it is returned unchanged (no suffix appended);
     * otherwise it is truncated to {@code maxChars - suffixCodepoints}
     * codepoints and the suffix is appended. When the text does not fit and
     * {@code maxChars <= suffix length} (the suffix alone consumes the budget),
     * or {@code maxChars <= 0}, the empty string is returned (宁空勿溢).
     *
     * <p>Semantic invariant: the result's codepoint count is
     * {@code <= maxChars}. Mirrors {@link #clipToWidth}'s
     * fits-in-full-budget semantics — the fits check uses the complete budget,
     * so a text that fits is never truncated to make room for the suffix.
     *
     * <p>Pure string logic — no font measurement involved, so it is unaffected
     * by {@link #isAvailable()}.
     */
    public static String clipToChars(String text, int maxChars, ClipSuffix suffix) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        ClipSuffix eff = suffix != null ? suffix : ClipSuffix.NONE;
        String suffixText = suffixText(eff);
        int suffixCount = suffixText.codePointCount(0, suffixText.length());
        if (maxChars <= 0) {
            return "";
        }
        // Fits-in-full-budget (mirrors clipToWidth's ④ full-fits check): the
        // complete text fits within maxChars, so it is returned unchanged. This
        // must precede the suffix-budget guard — a fitting text must not be
        // truncated just to reserve space for the suffix.
        if (text.codePointCount(0, text.length()) <= maxChars) {
            return text;
        }
        if (suffixCount >= maxChars) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int remaining = maxChars - suffixCount;
        int offset = 0;
        int taken = 0;
        while (offset < text.length() && taken < remaining) {
            int codePoint = text.codePointAt(offset);
            sb.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
            taken++;
        }
        return sb + suffixText;
    }

    /** Resolve the style's color to ARGB (default opaque white). */
    public static int resolveColor(@Nullable ResolvedTextStyle style) {
        int color = style != null && style.color() != null ? style.color()
            .resolve(LightDarkMode.current()) : 0xFFFFFFFF;
        if ((color >>> 24) == 0) {
            color |= 0xFF000000;
        }
        return color;
    }

    // ---- internals -----------------------------------------------------------

    private static String suffixText(ClipSuffix suffix) {
        return switch (suffix) {
            case NONE -> "";
            case DOTS3 -> "...";
            case UNICODE_ELLIPSIS -> "\u2026";
        };
    }

    /**
     * Codepoint-level line breaking for a single word that does not fit on the
     * current line. Accumulates per-codepoint advances via
     * {@link #advanceOf} (no substring re-measure loops, no {@code charAt}
     * surrogate-pair splitting); a single codepoint wider than the budget is
     * emitted on its own line (a glyph cannot be split).
     */
    private static void appendBrokenWord(String word, int maxWidth, @Nullable ResolvedTextStyle style,
        List<String> output) {
        if (measureWidth(word, style) <= maxWidth) {
            output.add(word);
            return;
        }
        StringBuilder current = new StringBuilder();
        float accumulated = 0f;
        int offset = 0;
        while (offset < word.length()) {
            int codePoint = word.codePointAt(offset);
            float advance = advanceOf(codePoint, style);
            if (current.length() > 0 && accumulated + advance > maxWidth) {
                output.add(current.toString());
                current.setLength(0);
                accumulated = 0f;
            }
            current.appendCodePoint(codePoint);
            accumulated += advance;
            offset += Character.charCount(codePoint);
        }
        if (current.length() > 0) {
            output.add(current.toString());
        }
    }

    private static ShapeTextResult shape(String text, @Nullable ResolvedTextStyle style) {
        boolean bold = style != null && style.bold();
        boolean italic = style != null && style.italic();
        float scale = style != null ? style.fontScale() : 1f;
        int renderScale = DisplayScale.scaleFactor();
        ShapeKey key = new ShapeKey(text, bold, italic, scale, renderScale);
        ShapeTextResult cached = shapeCache.get(key);
        if (cached != null) {
            return cached;
        }
        ShapeTextResult shaped = shapeUncached(text, bold, italic, scale, renderScale);
        if (shapeCache.size() > CACHE_LIMIT) {
            shapeCache.clear();
        }
        shapeCache.put(key, shaped);
        return shaped;
    }

    private static ShapeTextResult shapeUncached(String text, boolean bold, boolean italic, float fontScale,
        int renderScale) {
        FlatBufferBuilder fbb = new FlatBufferBuilder(1024);
        int strOff = fbb.createString(text);
        int styleOff = TextStyle
            .createTextStyle(fbb, BASE_FONT_SIZE, bold, italic, fontScale, 0xFFFFFFFFL, 0L, false, false, 0L, false, 0.0f, false, false);
        int inputOff = ShapeTextInput.createShapeTextInput(fbb, strOff, styleOff, -1.0f, renderScale);
        fbb.finish(inputOff);
        byte[] result = LayoutBridge.shapeText(LayoutBridge.getFontHandle(), fbb.sizedByteArray());
        if (result == null || result.length == 0) {
            // Degrade instead of throwing (C-7): one bad string must not take
            // down the whole layout/render frame.
            com.hfstudio.guidenh.guide.scene.support.GuideDebugLog
                .warnAlways("GuideText: shapeText failed, degrading to empty (len={})", text.length());
            FlatBufferBuilder empty = new FlatBufferBuilder(64);
            int off = com.hfstudio.guidenh.guide.layout.flatbuffers.ShapeTextResult
                .createShapeTextResult(empty, 0f, BASE_LINE_HEIGHT * fontScale, 0f, BASE_LINE_HEIGHT * fontScale, 0, 0, 0f, 0f);
            empty.finish(off);
            return com.hfstudio.guidenh.guide.layout.flatbuffers.ShapeTextResult
                .getRootAsShapeTextResult(ByteBuffer.wrap(empty.sizedByteArray()));
        }
        return ShapeTextResult.getRootAsShapeTextResult(ByteBuffer.wrap(result));
    }
}
