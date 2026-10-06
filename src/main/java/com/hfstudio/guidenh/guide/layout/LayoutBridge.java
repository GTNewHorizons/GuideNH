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
     * Register fallback symbol-font data (e.g. seguisym.ttf) into the existing
     * FontSystem and append it to the Han fallback key. Best-effort: a no-op on
     * empty data; callers should pass {@code new byte[0]} to skip.
     *
     * @param handle       FontSystem handle from init()
     * @param fallbackData font file bytes, or empty array to skip
     */
    public static native void loadFallbackFont(long handle, byte[] fallbackData);

    /**
     * Measure the entire layout tree.
     * 
     * @param handle FontSystem handle from init()
     * @param input  FlatBuffer-encoded LayoutInput bytes
     * @return FlatBuffer-encoded LayoutResult bytes, or empty byte[] on failure
     */
    public static native byte[] measureLayout(long handle, byte[] input);

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
