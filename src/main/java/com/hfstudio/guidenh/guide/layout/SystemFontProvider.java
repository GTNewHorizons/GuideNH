package com.hfstudio.guidenh.guide.layout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * System-font-based {@link FontProvider} that resolves the default CJK-
 * supporting font from well-known OS-specific paths.
 * <p>
 * <table>
 * <caption>Resolved paths by OS</caption>
 * <tr>
 * <th>OS</th>
 * <th>Font</th>
 * <th>Path</th>
 * </tr>
 * <tr>
 * <td>Windows</td>
 * <td>Microsoft YaHei</td>
 * <td>{@code C:\Windows\Fonts\msyh.ttc}</td>
 * </tr>
 * <tr>
 * <td>Linux</td>
 * <td>Noto Sans CJK SC</td>
 * <td>{@code /usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc}</td>
 * </tr>
 * <tr>
 * <td>macOS</td>
 * <td>PingFang SC</td>
 * <td>{@code /System/Library/Fonts/PingFang.ttc}</td>
 * </tr>
 * </table>
 * </p>
 * <p>
 * If none of the well-known paths exist the provider falls back to an empty
 * byte array so the existing no-font fallback path continues to work.
 * </p>
 */
public final class SystemFontProvider implements FontProvider {

    private String resolvedPath = "none";

    @Override
    public byte[] getFontData(String locale) {
        Path fontPath = resolveFontPath();
        if (fontPath == null) {
            GuideDebugLog
                .warnAlways("SystemFontProvider: no font file found for locale={}, using empty fallback", locale);
            return new byte[0];
        }
        try {
            byte[] data = Files.readAllBytes(fontPath);
            resolvedPath = fontPath.toAbsolutePath()
                .toString();
            GuideDebugLog.warnAlways("SystemFontProvider: loaded {} bytes from {}", data.length, resolvedPath);
            return data;
        } catch (IOException e) {
            GuideDebugLog.warnAlways("SystemFontProvider: failed to read {}: {}", fontPath, e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public String getFontPath() {
        return resolvedPath;
    }

    /**
     * Read fallback symbol-font bytes: Segoe UI Symbol (seguisym.ttf) on
     * Windows — covers the callout icons ⓘ ✦ ➤ ⚠ ☢ (U+24D8/2726/27A4/26A0/2622)
     * that msyh.ttc lacks. Other OSes get best-effort DejaVu / Apple Symbols
     * candidates. Returns an empty array when nothing is found (callers skip).
     */
    @Override
    public byte[] getFallbackFontData(String locale) {
        Path fontPath = resolveFallbackSymbolFontPath();
        if (fontPath == null) {
            GuideDebugLog
                .warnAlways("SystemFontProvider: no fallback symbol font found for locale={}, skipping", locale);
            return new byte[0];
        }
        try {
            byte[] data = Files.readAllBytes(fontPath);
            GuideDebugLog
                .warnAlways("SystemFontProvider: loaded fallback symbol font {} bytes from {}", data.length, fontPath);
            return data;
        } catch (IOException e) {
            GuideDebugLog
                .warnAlways("SystemFontProvider: failed to read fallback symbol font {}: {}", fontPath, e.getMessage());
            return new byte[0];
        }
    }

    // ---- platform detection ----

    private static Path resolveFontPath() {
        String os = System.getProperty("os.name")
            .toLowerCase();
        if (os.contains("win")) {
            return resolveWindowsFont();
        } else if (os.contains("mac")) {
            return resolveMacFont();
        } else {
            return resolveLinuxFont();
        }
    }

    private static Path resolveFallbackSymbolFontPath() {
        String os = System.getProperty("os.name")
            .toLowerCase();
        if (os.contains("win")) {
            // Segoe UI Symbol — Windows always ships it; covers ⓘ ✦ ➤ ⚠ ☢
            String windir = System.getenv("WINDIR");
            if (windir == null) {
                windir = "C:\\Windows";
            }
            Path seguisym = Paths.get(windir, "Fonts", "seguisym.ttf");
            if (Files.exists(seguisym)) return seguisym;
            // Fallback: Segoe UI Emoji (also carries the pictographs)
            Path segoeemoji = Paths.get(windir, "Fonts", "seguiemj.ttf");
            if (Files.exists(segoeemoji)) return segoeemoji;
            return null;
        } else if (os.contains("mac")) {
            // Apple Symbols — covers common dingbats/symbols
            Path appleSymbols = Paths.get("/System/Library/Fonts/Apple Symbols.ttf");
            if (Files.exists(appleSymbols)) return appleSymbols;
            return null;
        }
        // Linux best-effort: DejaVu Sans covers ⓘ ✦ ➤ ⚠ ☢
        Path[] candidates = { Paths.get("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
            Paths.get("/usr/share/fonts/dejavu/DejaVuSans.ttf"),
            Paths.get("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf") };
        for (Path p : candidates) {
            if (Files.exists(p)) return p;
        }
        return null;
    }

    private static Path resolveWindowsFont() {
        String windir = System.getenv("WINDIR");
        if (windir == null) {
            windir = "C:\\Windows";
        }
        Path base = Paths.get(windir, "Fonts");

        // Microsoft YaHei (msyh.ttc) — good CJK coverage
        Path msyh = base.resolve("msyh.ttc");
        if (Files.exists(msyh)) return msyh;
        // Fallback: SimSun
        Path simsun = base.resolve("simsun.ttc");
        if (Files.exists(simsun)) return simsun;
        // Fallback: any .ttf / .ttc in Fonts dir (last resort — check a few)
        Path segoeui = base.resolve("segoeui.ttf");
        if (Files.exists(segoeui)) return segoeui;
        // Fallback: Arial
        Path arial = base.resolve("arial.ttf");
        if (Files.exists(arial)) return arial;

        return null;
    }

    private static Path resolveLinuxFont() {
        // Noto Sans CJK — common locations
        Path[] candidates = { Paths.get("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
            Paths.get("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"),
            Paths.get("/usr/share/fonts/noto/NotoSansCJK-Regular.ttc"),
            Paths.get("/usr/share/fonts/opentype/noto/NotoSansSC-Regular.otf"),
            Paths.get("/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"),
            // Debian/Ubuntu package fonts-noto-cjk
            Paths.get("/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf"), };
        for (Path p : candidates) {
            if (Files.exists(p)) return p;
        }
        return null;
    }

    private static Path resolveMacFont() {
        Path pingfang = Paths.get("/System/Library/Fonts/PingFang.ttc");
        if (Files.exists(pingfang)) return pingfang;
        // macOS 10.11+ also has PingFang SC
        Path pingfangSc = Paths.get("/System/Library/Fonts/PingFang.ttc");
        if (Files.exists(pingfangSc)) return pingfangSc;
        // Fallback: Hiragino Sans
        Path hiragino = Paths.get("/System/Library/Fonts/Hiragino Sans GB.ttc");
        if (Files.exists(hiragino)) return hiragino;
        return null;
    }
}
