package com.hfstudio.guidenh.guide.layout;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FlatNode;
import com.hfstudio.guidenh.guide.layout.flatbuffers.GlyphBitmap;
import com.hfstudio.guidenh.guide.layout.flatbuffers.GlyphRun;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutResult;
import com.hfstudio.guidenh.guide.layout.flatbuffers.PlacedGlyph;
import com.hfstudio.guidenh.guide.layout.flatbuffers.Style;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextStyle;

/**
 * Headless diagnostic: prints glyph data from the Rust layout pipeline to console.
 * Bitmaps arrive inside LayoutResult (rasterized at render_scale); quads carry
 * document-space top-left positions. No window: runs and exits.
 *
 * Run: ./gradlew runGlyphDiag
 */
public class GlyphDiag {

    public static void main(String[] args) throws Exception {
        byte[] fontData = loadFont();
        long handle = LayoutBridge.init(fontData, "en-US");
        if (handle == 0) {
            System.err.println("FATAL: init returned 0");
            System.exit(1);
        }
        System.out.println("font: " + fontPath + "  bytes=" + fontData.length);

        String text = "Hello World! The quick brown fox jumps over the lazy dog. ABCDEFGH abcdefgh 0123456789";

        diag(text, 24f, 1.0f, handle);
        diag(text, 24f, 2.0f, handle);

        LayoutBridge.destroy(handle);
        System.out.println("DONE");
    }

    static void diag(String text, float fontSize, float renderScale, long handle) {
        System.out.println("\n=== fontSize=" + fontSize + " renderScale=" + renderScale + " text=\"" + text + "\"");

        byte[] lo = LayoutBridge.measureLayout(handle, buildLayoutInput(text, fontSize, renderScale));
        if (lo.length == 0) {
            System.out.println("  measureLayout: EMPTY");
            return;
        }
        LayoutResult lr = LayoutResult.getRootAsLayoutResult(ByteBuffer.wrap(lo));
        int totalGlyphs = 0;
        for (int r = 0; r < lr.glyphRunsLength(); r++) totalGlyphs += lr.glyphRuns(r)
            .glyphsLength();
        System.out.println("  layout: " + lr.debugInfo());
        System.out.println(
            "  glyphRuns=" + lr.glyphRunsLength() + " totalGlyphs=" + totalGlyphs + " bitmaps=" + lr.bitmapsLength());

        // Bitmap stats keyed by bitmap key
        Map<Long, BmpEntry> bmpMap = new HashMap<>();
        for (int i = 0; i < lr.bitmapsLength(); i++) {
            GlyphBitmap bmp = lr.bitmaps(i);
            int w = (int) bmp.w();
            int h = (int) bmp.h();
            ByteBuffer rgba = bmp.rgbaAsByteBuffer();
            int baseOff = rgba.position();
            int nonZero = 0;
            boolean allWhite = true;
            for (int p = 0; p < w * h; p++) {
                int off = baseOff + p * 4;
                if ((rgba.get(off) & 0xFF) != 255 || (rgba.get(off + 1) & 0xFF) != 255
                    || (rgba.get(off + 2) & 0xFF) != 255) {
                    allWhite = false;
                }
                if ((rgba.get(off + 3) & 0xFF) > 0) nonZero++;
            }
            bmpMap.put(bmp.key(), new BmpEntry(w, h, nonZero, allWhite));
        }

        // Print glyph table: quads in run order
        System.out.printf(
            "  %-4s %-20s %-10s %-10s %-8s %-8s %-8s %-8s %-6s %-6s%n",
            "#",
            "bitmapKey",
            "docX",
            "docY",
            "docW",
            "docH",
            "bmpW",
            "bmpH",
            "ink%",
            "ok?");
        int printed = 0;
        int missing = 0;
        for (int r = 0; r < lr.glyphRunsLength() && printed < 30; r++) {
            GlyphRun run = lr.glyphRuns(r);
            for (int g = 0; g < run.glyphsLength() && printed < 30; g++) {
                PlacedGlyph pg = run.glyphs(g);
                BmpEntry be = bmpMap.get(pg.bitmapKey());
                if (be == null) missing++;
                System.out.printf(
                    "  %-4d %-20d %-10.2f %-10.2f %-8.2f %-8.2f %-8d %-8d %-6d %-6s%n",
                    printed,
                    pg.bitmapKey(),
                    pg.x(),
                    pg.y(),
                    pg.w(),
                    pg.h(),
                    be != null ? be.w : 0,
                    be != null ? be.h : 0,
                    be != null ? be.nonZero * 100 / Math.max(1, be.w * be.h) : 0,
                    be == null ? "MISS" : be.allWhite ? "OK" : "!RGB");
                printed++;
            }
        }
        System.out.println(
            "  SUMMARY: unique bitmaps=" + bmpMap.size()
                + " quadsMissingBitmap="
                + missing
                + " (renderScale="
                + renderScale
                + ", bitmap px should scale with it)");
    }

    static byte[] buildLayoutInput(String text, float fontSize, float renderScale) {
        FlatBufferBuilder fbb = new FlatBufferBuilder(4096);
        int to = fbb.createString(text);
        int tso = TextStyle.createTextStyle(
            fbb,
            fontSize,
            false,
            false,
            1.0f,
            0xFFFFFFFFL,
            0,
            false,
            false,
            0L,
            false,
            0.0f,
            false,
            false);
        int tdo = TextData.createTextData(fbb, to, tso, (byte) 1, 0, 0, 0, 0, 0, 0, false, (byte) 0);
        int so = Style.createStyle(
            fbb,
            (byte) 0,
            (byte) 1,
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0f,
            0f,
            0f,
            0f,
            0f,
            false,
            false,
            false,
            false,
            0f,
            0f,
            0f,
            0f,
            0f,
            0f,
            0f,
            0f,
            (byte) 0,
            0f,
            1f,
            0,
            (byte) 0,
            (byte) 0,
            (byte) 0,
            0,
            0,
            0,
            0);
        int cv = createIntVec(fbb, new int[0]);
        int no = FlatNode
            .createFlatNode(fbb, so, (byte) 1, tdo, 0, 0, 0, 0, 0, (byte) 0, cv, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        int nv = fbb.createVectorOfTables(new int[] { no });
        fbb.finish(LayoutInput.createLayoutInput(fbb, 800f, 1.0f, renderScale, (byte) 1, nv, 0.25f)); // Shear factor,
                                                                                                      // formerly
                                                                                                      // LayoutTreeSerializer.SHEAR_K;
                                                                                                      // the Rust engine
                                                                                                      // owns SHEAR_K
                                                                                                      // now.
        return fbb.sizedByteArray();
    }

    static int createIntVec(FlatBufferBuilder fbb, int[] data) {
        fbb.startVector(4, data.length, 4);
        for (int i = data.length - 1; i >= 0; i--) fbb.addInt(data[i]);
        return fbb.endVector();
    }

    static String fontPath = "none";

    static byte[] loadFont() {
        Path p = resolveFontPath();
        if (p == null) return new byte[0];
        try {
            fontPath = p.toAbsolutePath()
                .toString();
            return Files.readAllBytes(p);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    static Path resolveFontPath() {
        String windir = System.getenv("WINDIR");
        if (windir == null) windir = "C:\\Windows";
        Path base = Paths.get(windir, "Fonts");
        Path[] cands = { base.resolve("msyh.ttc"), base.resolve("msyh.ttf"), base.resolve("simsun.ttc"),
            base.resolve("segoeui.ttf"), base.resolve("arial.ttf"), base.resolve("consola.ttf"),
            base.resolve("times.ttf"), base.resolve("cour.ttf"), };
        for (Path c : cands) {
            if (Files.exists(c)) return c;
        }
        return null;
    }

    record BmpEntry(int w, int h, int nonZero, boolean allWhite) {}
}
