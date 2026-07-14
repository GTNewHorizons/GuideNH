package com.hfstudio.guidenh.guide.layout;

/**
 * Provides TTF/OTC font data for the Rust layout engine's font system.
 * <p>
 * Implementations resolve font files from system paths, config files, or
 * bundled resources. The default implementation is {@link SystemFontProvider}.
 * </p>
 * <p>
 * This is a global configuration concern, not a CSS theme concern — font data
 * is loaded once at {@link LayoutBridge#init(byte[], String)} time and shared
 * across all documents.
 * </p>
 */
public interface FontProvider {

    /**
     * Read the font file bytes for the given locale.
     *
     * @param locale BCP 47 locale tag (e.g. {@code "zh_CN"}, {@code "en_US"})
     * @return font file bytes, or an empty array if no suitable font could be
     *         located
     */
    byte[] getFontData(String locale);

    /**
     * Return the resolved font file path (for diagnostics / logging).
     *
     * @return the absolute path to the font file, or {@code "none"} if no font
     *         was found
     */
    String getFontPath();
}
