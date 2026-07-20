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

    /** Base em size matching the legacy MC font cell (FONT_HEIGHT = 9). */
    public static final float BASE_FONT_SIZE = 9f;
    /** Line height at scale 1: FONT_HEIGHT + 1 = 10 (mirrored by text.rs). */
    public static final int BASE_LINE_HEIGHT = 10;

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

    /** Line height: 10 × fontScale (matches the legacy guide model). */
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
            glyphs.add(new GuideRenderPrimitive.PlacedGlyph(g.bitmapKey(), x + g.x(), y + g.y(), g.w(), g.h()));
        }
        c.emit(new GuideRenderPrimitive.DrawGlyphRun(tex, glyphs, resolveColor(style)));
    }

    /**
     * Oracle for the compiler's float-wrap band splitting: shape {@code text}
     * at {@code maxWidth} and return per-visual-line records as
     * {@code [startByte, endByte, lineTop, lineBottom]} (document units).
     * Same engine, same width → deterministic break points.
     */
    public static List<int[]> shapeLineBands(String text, @Nullable ResolvedTextStyle style, int maxWidth) {
        if (!isAvailable() || text == null || text.isEmpty() || maxWidth <= 0) {
            return List.of();
        }
        boolean bold = style != null && style.bold();
        boolean italic = style != null && style.italic();
        float scale = style != null ? style.fontScale() : 1f;
        FlatBufferBuilder fbb = new FlatBufferBuilder(1024);
        int strOff = fbb.createString(text);
        int styleOff = TextStyle
            .createTextStyle(fbb, BASE_FONT_SIZE, bold, italic, scale, 0xFFFFFFFFL, 0L, false, false, 0L, false);
        int inputOff = ShapeTextInput.createShapeTextInput(fbb, strOff, styleOff, maxWidth, DisplayScale.scaleFactor());
        fbb.finish(inputOff);
        byte[] result = LayoutBridge.shapeText(LayoutBridge.getFontHandle(), fbb.sizedByteArray());
        if (result == null || result.length == 0) {
            return List.of();
        }
        ShapeTextResult shaped = ShapeTextResult.getRootAsShapeTextResult(ByteBuffer.wrap(result));
        int n = shaped.glyphsLength();
        List<int[]> lines = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            var g = shaped.glyphs(i);
            if (g == null) continue;
            int li = (int) g.lineIndex();
            while (lines.size() <= li) {
                lines.add(new int[] { Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 0 });
            }
            int[] line = lines.get(li);
            line[0] = Math.min(line[0], (int) g.start());
            line[1] = Math.max(line[1], (int) g.end());
            line[2] = Math.min(line[2], Math.round(g.y()));
            line[3] = Math.max(line[3], Math.round(g.y() + g.h()));
        }
        lines.removeIf(l -> l[0] == Integer.MAX_VALUE);
        return lines;
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
            .createTextStyle(fbb, BASE_FONT_SIZE, bold, italic, fontScale, 0xFFFFFFFFL, 0L, false, false, 0L, false);
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
                .createShapeTextResult(empty, 0f, BASE_LINE_HEIGHT * fontScale, 0f, BASE_LINE_HEIGHT * fontScale, 0, 0);
            empty.finish(off);
            return com.hfstudio.guidenh.guide.layout.flatbuffers.ShapeTextResult
                .getRootAsShapeTextResult(ByteBuffer.wrap(empty.sizedByteArray()));
        }
        return ShapeTextResult.getRootAsShapeTextResult(ByteBuffer.wrap(result));
    }
}
