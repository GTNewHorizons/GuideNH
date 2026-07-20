package com.hfstudio.guidenh.guide.layout;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * JNI bridge to the Rust guide_layout_engine native library.
 * <p>
 * All methods are static. FontSystem handle is the only persistent state.
 * The native library is extracted from the classpath on first load.
 */
public final class LayoutBridge {

    private static boolean loaded;

    static {
        loadNative();
    }

    private static void loadNative() {
        if (loaded) return;

        // Allow override via system property for development/testing
        String overridePath = System.getProperty("guide.native.lib.path");
        if (overridePath != null && !overridePath.isEmpty()) {
            System.load(overridePath);
            loaded = true;
            return;
        }

        // Extract native lib from classpath resources
        String os = System.getProperty("os.name")
            .toLowerCase();
        String libName;
        if (os.contains("win")) {
            libName = "guide_layout_engine.dll";
        } else if (os.contains("mac")) {
            libName = "guide_layout_engine.dylib";
        } else {
            libName = "libguide_layout_engine.so";
        }

        String resourcePath = "/natives/" + libName;
        try (InputStream is = LayoutBridge.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new UnsatisfiedLinkError("Native library not found in classpath: " + resourcePath);
            }
            Path tmpFile = Files.createTempFile("guide_layout_engine_", libName);
            tmpFile.toFile()
                .deleteOnExit();
            Files.copy(is, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            System.load(
                tmpFile.toAbsolutePath()
                    .toString());
            loaded = true;
        } catch (IOException e) {
            throw new UnsatisfiedLinkError("Failed to extract native library: " + e.getMessage());
        }
    }

    private static long globalFontHandle;

    /** Set the global font handle (called once after init()). */
    public static void setFontHandle(long handle) {
        globalFontHandle = handle;
    }

    /** Get the global font handle. Returns 0 if not initialized. */
    public static long getFontHandle() {
        return globalFontHandle;
    }

    /** Load TTF font data and create a FontSystem. Returns opaque handle (long). */
    public static native long init(byte[] fontTtfData, String locale);

    /**
     * Measure the entire layout tree.
     * 
     * @param handle FontSystem handle from init()
     * @param input  FlatBuffer-encoded LayoutInput bytes
     * @return FlatBuffer-encoded LayoutResult bytes, or empty byte[] on failure
     */
    public static native byte[] measureLayout(long handle, byte[] input);

    /**
     * Rasterize a batch of glyphs.
     * 
     * @param handle FontSystem handle from init()
     * @param input  FlatBuffer-encoded RasterInput bytes
     * @return FlatBuffer-encoded RasterResult bytes, or empty byte[] on failure
     */
    public static native byte[] rasterizeGlyphs(long handle, byte[] input);

    /**
     * Shape + rasterize text in one call. Returns RenderResult FlatBuffer bytes
     * with per-glyph (x, y, w, h, rgba) at SubpixelBin-corrected positions.
     * Java only needs to blit the returned glyphs — no rounding/position math.
     */
    public static native byte[] renderText(long handle, String text, float fontSize, float availWidth);

    /**
     * Shape + rasterize one styled text (unified text pipeline entry).
     *
     * @param handle FontSystem handle from init()
     * @param input  FlatBuffer-encoded ShapeTextInput bytes
     * @return FlatBuffer-encoded ShapeTextResult bytes (atlas-keyed quads +
     *         metrics), or empty byte[] on failure
     */
    public static native byte[] shapeText(long handle, byte[] input);

    /** Destroy the FontSystem and free native memory. */
    public static native void destroy(long handle);

    private LayoutBridge() {}
}
