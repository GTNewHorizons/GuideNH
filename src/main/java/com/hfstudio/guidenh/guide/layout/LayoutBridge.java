package com.hfstudio.guidenh.guide.layout;

/** Java layout facade backed exclusively by taffy-java. */
public class LayoutBridge {

    private static final TaffyJavaLayoutEngine ENGINE = new TaffyJavaLayoutEngine();
    private static volatile long globalFontHandle;

    /** Set the global font handle (called once after init()). */
    public static void setFontHandle(long handle) {
        globalFontHandle = handle;
    }

    /** Get the global font handle. Returns 0 if not initialized. */
    public static long getFontHandle() {
        return globalFontHandle;
    }

    /** Initializes the Java layout session. Font bytes are accepted for API compatibility. */
    public static long init(byte[] fontTtfData, String locale) {
        long handle = 1L;
        setFontHandle(handle);
        return handle;
    }

    /**
     * Register fallback symbol-font data (e.g. seguisym.ttf) into the existing
     * FontSystem and append it to the Han fallback key. Best-effort: a no-op on
     * empty data; callers should pass {@code new byte[0]} to skip.
     *
     * @param handle       FontSystem handle from init()
     * @param fallbackData font file bytes, or empty array to skip
     */
    public static void loadFallbackFont(long handle, byte[] fallbackData) {}

    /**
     * Measure the entire layout tree.
     * 
     * @param handle FontSystem handle from init()
     * @param input  FlatBuffer-encoded LayoutInput bytes
     * @return FlatBuffer-encoded LayoutResult bytes, or empty byte[] on failure
     */
    public static byte[] measureLayout(long handle, byte[] input) {
        if (handle == 0L || handle != globalFontHandle) return new byte[0];
        try {
            return ENGINE.compute(input);
        } catch (RuntimeException ignored) {
            return new byte[0];
        }
    }

    /**
     * Shape + rasterize one styled text (unified text pipeline entry).
     *
     * @param handle FontSystem handle from init()
     * @param input  FlatBuffer-encoded ShapeTextInput bytes
     * @return FlatBuffer-encoded ShapeTextResult bytes (atlas-keyed quads +
     *         metrics), or empty byte[] on failure
     */
    public static byte[] shapeText(long handle, byte[] input) {
        return new byte[0];
    }

    /** Destroy the FontSystem and free native memory. */
    public static void destroy(long handle) {
        if (handle == globalFontHandle) globalFontHandle = 0L;
    }

    private LayoutBridge() {}
}
