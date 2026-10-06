package com.hfstudio.guidenh.guide.render;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.layout.LayoutBridge;
import com.hfstudio.guidenh.guide.layout.flatbuffers.ShapeTextInput;
import com.hfstudio.guidenh.guide.layout.flatbuffers.ShapeTextResult;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextStyle;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Unified text pipeline entry: one font, one entry point, one coordinate
 * system.
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

    /**
     * Base em size for guide body text, raised from 9 to 11 by the typography
     * pass. This is the value {@link #resolveBaseFontSize()} resolves to when
     * the config {@code readerFontSize} is 0 (follow system) -- the default, so
     * the default wire bytes are unchanged from earlier builds.
     */
    public static final float BASE_FONT_SIZE = 11f;
    /**
     * Line height at scale 1 for the DEFAULT base font size (readerFontSize = 0):
     * round(BASE_FONT_SIZE × 1.55) = round(17.05) = 17 (mirrored by
     * parley_text.rs push_defaults FontSizeRelative(1.55)). Kept as a constant
     * because {@code RustFontMetrics} references it, and it equals
     * {@link #baseLineHeight()} at the default config -- so the default wire
     * bytes are unchanged; a configured readerFontSize derives the runtime
     * estimate via {@link #baseLineHeight()}.
     */
    public static final int BASE_LINE_HEIGHT = 17;

    /**
     * Single source of truth for the base em size written to the text wire.
     * {@code readerFontSize <= 0} ("follow system") resolves to the built-in
     * {@link #BASE_FONT_SIZE} (11f); a positive configured value is used as-is.
     * With the default config this returns exactly {@link #BASE_FONT_SIZE}, so
     * every wire consumer of the font size is byte-for-byte identical to
     * earlier builds.
     */
    public static float resolveBaseFontSize() {
        int configured = ModConfig.reader.readerFontSize;
        return configured > 0 ? (float) configured : BASE_FONT_SIZE;
    }

    /**
     * Single source of truth for the reader body font family: the
     * config-to-wire mapping. {@code "system"} / null / blank resolves to
     * {@code null} (= undeclared), and the Rust {@code resolve_family} in
     * parley_text.rs:570-576 then falls back to the SansSerif generic, leaving
     * the wire bytes unchanged. Any other string passes through as a named
     * family. When a passed-through name is absent from the fontique
     * collection, parley skips it silently and falls back to the script
     * fallback chain (the platform default font, not a hard failure):
     * parley-0.11.0 resolve/mod.rs:233-237 (a Named family that is not in the
     * collection is never pushed onto the stack), shape/mod.rs:586 (the empty
     * stack handled by {@code unwrap_or(&[])}), and fontique query.rs:122-126
     * (the family chain continues into {@code fallback_families}). The default
     * "system" resolves to null, which is wire-neutral.
     */
    public static @Nullable String resolveBaseFontFamily() {
        String configured = ModConfig.reader.readerFontFamily;
        if (configured == null || configured.isBlank() || "system".equals(configured)) {
            return null;
        }
        return configured;
    }

    /**
     * Single source of truth for the reader inline-code font family: the
     * config-to-wire mapping. {@code "mono"} is the config-level meaning of
     * "platform monospace default", but the Rust {@code resolve_family} only
     * recognises the reserved word {@code "monospace"} (the
     * {@code Some("monospace")} branch at parley_text.rs:572; {@code "mono"}
     * takes the {@code Some(name) => FontFamily::named("mono")} branch, whose
     * collection lookup misses). {@code "mono"} is therefore mapped here to
     * {@link #MONO_FONT_FAMILY} ("monospace"), so under the default config the
     * wire matches the inline_code literal byte for byte. null / blank is
     * treated as the default ("monospace"); any other string passes through as
     * a named family with the same fallback semantics as the base family.
     */
    public static String resolveCodeFontFamily() {
        String configured = ModConfig.reader.readerCodeFontFamily;
        if (configured == null || configured.isBlank() || "mono".equals(configured)) {
            return MONO_FONT_FAMILY;
        }
        return configured;
    }

    /**
     * Line height at scale 1 for the CURRENT base font size -- the Java-side
     * mirror of Rust's FontSizeRelative(1.55): round(resolveBaseFontSize() ×
     * 1.55). At the default config this equals the historical constant
     * {@link #BASE_LINE_HEIGHT} (17), so default-mode estimates are unchanged;
     * a configured readerFontSize derives proportionally.
     */
    public static int baseLineHeight() {
        return Math.max(1, Math.round(resolveBaseFontSize() * 1.55f));
    }

    /**
     * The inline_code monospace family written on the wire: the CSS generic
     * name {@code "monospace"}. The Rust {@code resolve_family} maps it to
     * GenericFamily::Monospace, which each fontique backend resolves to that
     * platform's monospace default (dwrite to Consolas, and so on). Shared by
     * the Java-side serialization (LayoutNodeSerializer) and by the cache key
     * ({@link #fontFamilyOf} in this class); it is the default wire value of
     * {@link #resolveCodeFontFamily()} (the config value {@code "mono"} maps to
     * this constant), so the cache key, the wire family and the family actually
     * shaped are always the same.
     */
    public static final String MONO_FONT_FAMILY = "monospace";

    /**
     * Cache key includes the display pixel ratio: bitmaps are rasterized per
     * render scale, so a GUI-scale change must not hit stale entries.
     * <p>
     * The family component is {@link #fontFamilyOf}, the family the style
     * actually writes to the wire (inline_code resolves to
     * {@link #resolveCodeFontFamily()}, body text to
     * {@link #resolveBaseFontFamily()}), so different families never cross-talk
     * and entries of a replaced family stop matching after a config change. The
     * {@code baseFontSize} component is {@link #resolveBaseFontSize()}, the same
     * local variable that {@code shapeUncached} writes to the wire as the font
     * size, so key and wire share one source and entries of a replaced size
     * stop matching after a config change.
     */
    private record ShapeKey(String text, boolean bold, boolean italic, float fontScale, int renderScale,
        @Nullable String family, float baseFontSize) {}

    private record AdvanceKey(int codePoint, boolean bold, boolean italic, float fontScale, int renderScale,
        @Nullable String family, float baseFontSize) {}

    private static final int CACHE_LIMIT = 4096;
    private static final Map<ShapeKey, ShapeTextResult> shapeCache = new ConcurrentHashMap<>();
    private static final Map<AdvanceKey, Float> advanceCache = new ConcurrentHashMap<>();

    /**
     * Per-family baseline cache key: the composite (family, baseFontSize).
     * Indexing by family alone would return a stale baseline after a font size
     * change, which is how a cache gets polluted by a key that describes one
     * font while the wire carries another. The family component is still
     * normalized by {@link #familyKey} (null/blank = body SansSerif generic);
     * body metrics ({@code ascentFor} / {@code xHeightFor} with a null family)
     * resolve the family to {@link #resolveBaseFontFamily()} first, so the key
     * shares one source with the wire and a base-family config change stops
     * matching the old family's baseline entries. The {@code baseFontSize}
     * component is {@link #resolveBaseFontSize()}, the same source as the value
     * {@code shape("x", ...)} writes to the wire in the same frame.
     */
    private record FamilyMetricKey(@Nullable String family, float baseFontSize) {}

    /**
     * Per-family baseline cache, replacing the earlier global single value
     * ({@code cachedBaseAscent}). The key is the family normalized by
     * {@link #familyKey} (null/blank = body font SansSerif generic, which
     * reproduces the behaviour of the global single value). Distinct families
     * such as the monospace family are cached independently and never pollute
     * each other. The key is {@link FamilyMetricKey} (family + baseFontSize),
     * so a font size change cannot return a stale value, and body metrics are
     * dimensioned by {@link #resolveBaseFontFamily()}, so a base-family change
     * cannot return a stale baseline either.
     */
    private static final Map<FamilyMetricKey, Float> baseAscentByFamily = new ConcurrentHashMap<>();
    private static final Map<FamilyMetricKey, Float> xHeightByFamily = new ConcurrentHashMap<>();

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

    /**
     * Line height at the current base font size × fontScale (11px base × 1.55
     * line-height ratio at the default config; see {@link #baseLineHeight()}).
     */
    public static int lineHeight(@Nullable ResolvedTextStyle style) {
        float scale = style != null ? style.fontScale() : 1f;
        return Math.max(1, Math.round(baseLineHeight() * scale));
    }

    /**
     * Baseline offset below the line top at scale 1 (cosmic font metrics at
     * {@link #BASE_FONT_SIZE}). Multiply by fontScale for scaled styles.
     * <p>
     * Body-font (SansSerif generic) accessor: equivalent to
     * {@code ascentFor(null)}. The downstream consumers ({@code baselineOf},
     * {@code LytItemImage}, {@code GuideNavBar}) all measure the body font, so
     * the semantics are unchanged when several families are in play.
     */
    public static float ascent() {
        return ascentFor(null);
    }

    /**
     * Per-family ascent. A null or blank family falls back to the body font,
     * which resolves to {@link #resolveBaseFontFamily()} (the default resolves
     * to null, i.e. the SansSerif generic, matching the behaviour of the
     * earlier global single-value cache), keeping the metric key and the wire
     * on one source. Each family has its own cache
     * ({@link #baseAscentByFamily}) and never pollutes another.
     */
    public static float ascentFor(@Nullable String family) {
        // An explicit family wins; a null family means body text, resolved through resolveBaseFontFamily().
        String effFamily = family != null ? family : resolveBaseFontFamily();
        // Key = (family, baseFontSize). baseFontSize shares one source with the
        // value shape("x", ...) writes to the wire in the same frame
        // (resolveBaseFontSize only reads config, so it is constant within a
        // frame), and effFamily shares one source with the family that
        // familyStyle(effFamily) writes to the wire (fontFamilyOf passes a
        // non-null style.font() through unchanged).
        FamilyMetricKey key = new FamilyMetricKey(familyKey(effFamily), resolveBaseFontSize());
        Float cached = baseAscentByFamily.get(key);
        if (cached != null) {
            return cached;
        }
        if (!isAvailable()) {
            // Same convention as the legacy MC cell: baseline ≈ top + 7 of 9.
            return 7f;
        }
        float a = shape("x", familyStyle(effFamily)).ascent();
        baseAscentByFamily.put(key, a);
        return a;
    }

    /**
     * Body x-height at scale 1, read from the Rust shape pipeline: the
     * first run's skrifa OS/2 {@code sxHeight} at the base em size. Unlike
     * {@link #ascent()} (the font ascent, ≈0.75-0.85em -- the top of tall
     * letters/ascenders), this measures a lower-case "x", i.e. the height
     * lowercase body letters actually occupy. Multiply by fontScale for scaled
     * styles. Body-font (SansSerif generic) accessor: equivalent to
     * {@code xHeightFor(null)}.
     */
    public static float xHeight() {
        return xHeightFor(null);
    }

    /**
     * Per-family x-height. A null or blank family falls back to the body font,
     * which resolves to {@link #resolveBaseFontFamily()} (the default resolves
     * to null, i.e. the SansSerif generic), keeping the metric key and the wire
     * on one source. Each family has its own cache
     * ({@link #xHeightByFamily}) and never pollutes another.
     */
    public static float xHeightFor(@Nullable String family) {
        // An explicit family wins; a null family means body text, resolved through resolveBaseFontFamily().
        String effFamily = family != null ? family : resolveBaseFontFamily();
        // Key = (family, baseFontSize); the same-source argument as ascentFor.
        FamilyMetricKey key = new FamilyMetricKey(familyKey(effFamily), resolveBaseFontSize());
        Float cached = xHeightByFamily.get(key);
        if (cached != null) {
            return cached;
        }
        float h = shape("x", familyStyle(effFamily)).xHeight();
        xHeightByFamily.put(key, h);
        return h;
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
     * strikethrough / backgrounds) are NOT emitted here; rich-text spans are
     * handled by the span pipeline.
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
                glyphs,
                resolveColor(style),
                // Declare the style's italic: it is already declared into
                // shaping (shapeUncached writes TextStyle.italic), and a
                // hardcoded false here used to silence the MC §o synthetic
                // italic slant for labels. The renderer now shears italic runs
                // (per-span runs carry their own shear via GlyphRun.shear).
                style != null && style.italic(),
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
        // The baseFontSize component shares one source with the value
        // shape(s, style) writes to the wire as the font size in the same frame
        // (resolveBaseFontSize only reads config, so it is constant within a
        // frame).
        float baseFontSize = resolveBaseFontSize();
        AdvanceKey key = new AdvanceKey(
            codePoint,
            bold,
            italic,
            scale,
            DisplayScale.scaleFactor(),
            fontFamilyOf(style),
            baseFontSize);
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
     * {@link #advanceOf} -- codepoint-aware, so surrogate pairs are never split
     * (no {@code charAt} scanning). Output lines carry no leading or trailing
     * whitespace; empty input lines are dropped.
     *
     * <p>
     * <b>Degradation semantics:</b> when {@link #isAvailable()} is false no
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
     * <p>
     * <b>Semantic invariant: the result's rendered width is
     * {@code <= maxWidth}.</b> The suffix width counts against the budget; when
     * the suffix alone does not fit ({@code suffixWidth > maxWidth}), or when
     * even a single codepoint plus the suffix overflows, the empty string is
     * returned (rather empty than overflowing). When the full text fits, it is
     * returned unchanged.
     *
     * <p>
     * Implementation: codepoints are accumulated via
     * {@link #advanceOf(int, ResolvedTextStyle)} -- never {@code substring} +
     * {@link #measureWidth} shrink loops, which would create one unique shape
     * cache key per prefix and blow up shaping to O(n²). The final result is
     * then re-verified with {@link #measureWidth} and codepoints are backed off
     * until it fits, because the per-codepoint advance sum differs from the
     * shaped run by subpixels.
     *
     * <p>
     * <b>Degradation semantics:</b> when {@link #isAvailable()} is false no
     * measurement is possible, so the original text is returned untouched.
     */
    public static String clipToWidth(String text, int maxWidth, @Nullable ResolvedTextStyle style, ClipSuffix suffix) {
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
     * or {@code maxChars <= 0}, the empty string is returned (rather empty than
     * overflowing).
     *
     * <p>
     * Semantic invariant: the result's codepoint count is
     * {@code <= maxChars}. Mirrors {@link #clipToWidth}'s
     * fits-in-full-budget semantics -- the fits check uses the complete budget,
     * so a text that fits is never truncated to make room for the suffix.
     *
     * <p>
     * Corner case: when the full text fits ({@code codePointCount <=
     * maxChars}) it is returned unchanged (no suffix appended) even if the
     * suffix alone would not fit the budget -- the fits-in-full-budget check
     * precedes the suffix-space reservation.
     *
     * <p>
     * Pure string logic -- no font measurement involved, so it is unaffected
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
        // Fits-in-full-budget (mirrors clipToWidth's full-fits check): the
        // complete text fits within maxChars, so it is returned unchanged. This
        // must precede the suffix-budget guard -- a fitting text must not be
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
            .resolve() : 0xFFFFFFFF;
        if ((color >>> 24) == 0) {
            color |= 0xFF000000;
        }
        return color;
    }

    // Internal helpers: shaping, wire values and the metric/shape caches.

    private static String suffixText(ClipSuffix suffix) {
        return switch (suffix) {
            case NONE -> "";
            case DOTS3 -> "...";
            case UNICODE_ELLIPSIS -> "\u2026";
        };
    }

    /**
     * The font family the style actually writes to the wire, shared by the
     * cache key and by serialization (LayoutNodeSerializer.buildFbTextStyle),
     * so the cache key, the wire family and the family that is actually shaped
     * are always the same. Inline code resolves to
     * {@link #resolveCodeFontFamily()} (monospace is what inline code means;
     * the default "mono" to "monospace" mapping keeps the earlier literal byte
     * for byte); otherwise a non-null {@code style.font()} passes through as a
     * named family and null falls back to {@link #resolveBaseFontFamily()} (the
     * default "system" resolves to null, so non-code pages keep their earlier
     * bytes).
     */
    @Nullable
    static String fontFamilyOf(@Nullable ResolvedTextStyle style) {
        if (style == null) {
            return null;
        }
        if (style.inlineCode()) {
            return resolveCodeFontFamily();
        }
        return style.font() != null ? style.font() : resolveBaseFontFamily();
    }

    /**
     * Normalizes a family for the per-family cache keys: null or blank becomes
     * null (= body font).
     */
    @Nullable
    private static String familyKey(@Nullable String family) {
        if (family == null || family.isBlank()) {
            return null;
        }
        return family;
    }

    /**
     * Minimal style carrying the given family, so the shape pipeline resolves
     * that family; null falls back to body text, which makes
     * {@code shape("x", null)} byte-for-byte identical to the earlier output.
     */
    @Nullable
    private static ResolvedTextStyle familyStyle(@Nullable String family) {
        if (family == null) {
            return null;
        }
        return new ResolvedTextStyle(
            1f,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            family,
            null,
            null,
            null,
            false,
            null,
            false,
            0f);
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
        // The family dimension is the family the style actually writes to the
        // wire (inline_code resolves to resolveCodeFontFamily, body text to
        // resolveBaseFontFamily); sharing fontFamilyOf with serialization keeps
        // the cache from conflating the monospace and body fonts on the wire,
        // and entries of a replaced family stop matching after a config change.
        String fontFamily = fontFamilyOf(style);
        // baseFontSize is captured once and used both in the key and on the
        // wire (passed straight to shapeUncached), so key and wire share one
        // source and entries of a replaced size stop matching after a config
        // change.
        float baseFontSize = resolveBaseFontSize();
        ShapeKey key = new ShapeKey(text, bold, italic, scale, renderScale, fontFamily, baseFontSize);
        ShapeTextResult cached = shapeCache.get(key);
        if (cached != null) {
            return cached;
        }
        ShapeTextResult shaped = shapeUncached(text, bold, italic, scale, renderScale, fontFamily, baseFontSize);
        if (shapeCache.size() > CACHE_LIMIT) {
            shapeCache.clear();
        }
        shapeCache.put(key, shaped);
        return shaped;
    }

    private static ShapeTextResult shapeUncached(String text, boolean bold, boolean italic, float fontScale,
        int renderScale, @Nullable String fontFamily, float baseFontSize) {
        FlatBufferBuilder fbb = new FlatBufferBuilder(1024);
        int strOff = fbb.createString(text);
        // The wire family is the fontFamilyOf(style) captured once in shape()
        // (the same local variable is passed through), so the key and the wire
        // agree; an empty string is avoided to prevent a zero offset (the Rust
        // clean_family also trims and filters empty names).
        int familyOff = fontFamily != null && !fontFamily.isEmpty() ? fbb.createString(fontFamily) : 0;
        // The wire font size is the baseFontSize component of the cache key
        // (the same local variable is passed through), so the key and the wire
        // agree and there is no cache pollution path.
        int styleOff = TextStyle.createTextStyle(
            fbb,
            baseFontSize,
            bold,
            italic,
            fontScale,
            0xFFFFFFFFL,
            familyOff,
            false,
            false,
            0L,
            false,
            0.0f,
            false,
            false);
        int inputOff = ShapeTextInput.createShapeTextInput(fbb, strOff, styleOff, -1.0f, renderScale);
        fbb.finish(inputOff);
        byte[] result = LayoutBridge.shapeText(LayoutBridge.getFontHandle(), fbb.sizedByteArray());
        if (result == null || result.length == 0) {
            // Degrade instead of throwing: one bad string must not take
            // down the whole layout/render frame.
            com.hfstudio.guidenh.guide.scene.support.GuideDebugLog
                .warnAlways("GuideText: shapeText failed, degrading to empty (len={})", text.length());
            FlatBufferBuilder empty = new FlatBufferBuilder(64);
            // The line height is derived from the current baseFontSize (the
            // default gives BASE_LINE_HEIGHT = 17, matching the earlier bytes).
            int off = com.hfstudio.guidenh.guide.layout.flatbuffers.ShapeTextResult.createShapeTextResult(
                empty,
                0f,
                baseLineHeight() * fontScale,
                0f,
                baseLineHeight() * fontScale,
                0,
                0,
                0f,
                0f);
            empty.finish(off);
            return com.hfstudio.guidenh.guide.layout.flatbuffers.ShapeTextResult
                .getRootAsShapeTextResult(ByteBuffer.wrap(empty.sizedByteArray()));
        }
        return ShapeTextResult.getRootAsShapeTextResult(ByteBuffer.wrap(result));
    }
}
