package com.hfstudio.guidenh.guide.layout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.AlignItems;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytDetailsBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.render.GlyphRunHolder;
import com.hfstudio.guidenh.guide.render.GuideGlyphAtlas;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextStyle;

/**
 * TEMPORARY diagnostic (will be deleted): span-based baselineShift experiment
 * for the details summary marker glyph.
 */
public final class DetailsAlignDiag {

    private static com.hfstudio.guidenh.guide.layout.FontMetrics MOCK_METRICS;

    private static com.hfstudio.guidenh.guide.layout.FontMetrics createFontMetrics(byte[] fontData) {
        try {
            var awtFont = java.awt.Font
                .createFont(java.awt.Font.TRUETYPE_FONT, new java.io.ByteArrayInputStream(fontData))
                .deriveFont(9f);
            var img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var g = img.createGraphics();
            g.setRenderingHint(
                java.awt.RenderingHints.KEY_FRACTIONALMETRICS,
                java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont(awtFont);
            var fm = g.getFontMetrics();
            return new com.hfstudio.guidenh.guide.layout.FontMetrics() {

                @Override
                public float getAdvance(int codePoint, ResolvedTextStyle style) {
                    return (float) fm.getStringBounds(new String(Character.toChars(codePoint)), g)
                        .getWidth();
                }

                @Override
                public int getLineHeight(ResolvedTextStyle style) {
                    return 10;
                }
            };
        } catch (Exception e) {
            throw new IllegalStateException("diag font not usable by AWT", e);
        }
    }

    private static Path resolveFontPath() {
        String windir = System.getenv("WINDIR");
        if (windir == null) windir = "C:\\Windows";
        Path base = Paths.get(windir, "Fonts");
        Path[] cands = { base.resolve("msyh.ttc"), base.resolve("msyh.ttf"), base.resolve("segoeui.ttf"),
            base.resolve("arial.ttf"), base.resolve("consola.ttf"), base.resolve("cour.ttf") };
        for (Path c : cands) {
            if (Files.exists(c)) return c;
        }
        return null;
    }

    private static void dumpPara(String label, LytParagraph p) {
        LytRect r = p.getBounds();
        var gd = ((GlyphRunHolder) p).getGlyphData();
        if (gd == null || gd.runs()
            .isEmpty()) {
            System.out.printf("PARA[%s] bounds=%s no glyph data%n", label, r);
            return;
        }
        float minY = Float.MAX_VALUE, maxY = 0;
        int count = 0;
        for (var group : gd.runs()) {
            for (var g : group.glyphs()) {
                minY = Math.min(minY, g.y());
                maxY = Math.max(maxY, g.y() + g.h());
                count++;
            }
        }
        float inkCenterY = (minY + maxY) / 2f;
        float boxCenterY = (r.y() + (r.y() + r.height())) / 2f;
        System.out.printf(
            "PARA[%s] bounds=%s glyphs=%d inkY=[%.2f..%.2f] inkCenter=%.2f boxCenter=%.2f delta=%.2f runs=%d%n",
            label,
            r,
            count,
            minY,
            maxY,
            inkCenterY,
            boxCenterY,
            inkCenterY - boxCenterY,
            gd.runs()
                .size());
        for (var group : gd.runs()) {
            for (var g : group.glyphs()) {
                System.out.printf("    g x=%.2f y=%.2f w=%.2f h=%.2f%n", g.x(), g.y(), g.w(), g.h());
            }
        }
    }

    private static void runSpanCase(String label, float bs) {
        LytDocument doc = new LytDocument();
        var row = new LytHBox();
        row.setGap(4);
        row.setWrap(false);
        row.setFullWidth(true);
        row.setAlignItems(AlignItems.CENTER);

        var marker = new LytParagraph();
        marker.setMarginTop(0);
        marker.setMarginBottom(0);
        marker.modifyStyle(
            style -> style.bold(true)
                .color(new ConstantColor(0xFFE2E6ED)));
        var s1 = new LytFlowSpan();
        s1.setStyle(
            TextStyle.builder()
                .bold(true)
                .color(new ConstantColor(0xFFE2E6ED))
                .baselineShift(bs)
                .build());
        s1.append(LytFlowText.of(">"));
        marker.append(s1);
        var s2 = new LytFlowSpan();
        s2.setStyle(
            TextStyle.builder()
                .bold(true)
                .color(new ConstantColor(0xFFE2E6ED))
                .baselineShift(0f)
                .build());
        s2.append(LytFlowText.of("\u200B"));
        marker.append(s2);

        var text = new LytParagraph();
        text.setMarginTop(0);
        text.setMarginBottom(0);
        text.modifyStyle(
            style -> style.bold(true)
                .color(new ConstantColor(0xFFE2E6ED)));
        text.appendText("Closed Details (click to expand)");

        row.append(marker);
        row.append(text);
        doc.append(row);
        doc.updateLayout(new LayoutContext(MOCK_METRICS), 557);
        System.out.println("\n==== spanCase bs=" + bs + " ====");
        for (LytNode c : row.getChildren()) {
            if (c instanceof LytParagraph p) {
                dumpPara(
                    ((GlyphRunHolder) p).getGlyphData() != null && !((GlyphRunHolder) p).getGlyphData()
                        .runs()
                        .isEmpty()
                        && ((GlyphRunHolder) p).getGlyphData()
                            .runs()
                            .getFirst()
                            .glyphs()
                            .size() == 1 ? "> marker" : "text",
                    p);
            }
        }
    }

    private static void runRealDetails(String state) {
        LytDocument doc = new LytDocument();
        LytDetailsBlock d = new LytDetailsBlock();
        d.setMarginTop(0);
        d.setMarginBottom(0);
        boolean open = "open".equals(state);
        d.setOpen(open);
        d.getSummaryBox()
            .clearContent();
        d.getSummaryBox()
            .appendText(open ? "Open Details (click to toggle)" : "Closed Details (click to expand)");
        doc.append(d);
        doc.updateLayout(new LayoutContext(MOCK_METRICS), 557);
        System.out.println("\n==== REAL details " + state + " ====");
        List<? extends LytNode> summaryChildren = d.getChildren()
            .getFirst() instanceof LytBlock sb ? sb.getChildren() : List.of();
        for (LytNode c : summaryChildren) {
            if (c instanceof LytParagraph p) {
                int n = ((GlyphRunHolder) p).getGlyphData() != null && !((GlyphRunHolder) p).getGlyphData()
                    .runs()
                    .isEmpty() ? ((GlyphRunHolder) p).getGlyphData()
                        .runs()
                        .getFirst()
                        .glyphs()
                        .size() : 0;
                dumpPara(n == 1 ? "> marker" : "text", p);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String libPath = System.getProperty("guide.native.lib.path");
        System.out.println("libPath=" + libPath);
        if (libPath == null || libPath.isEmpty()) {
            System.err.println("SKIP: guide.native.lib.path not set");
            return;
        }
        System.load(libPath);
        Path fontPath = resolveFontPath();
        byte[] fontData = Files.readAllBytes(fontPath);
        long handle = LayoutBridge.init(fontData, "en-US");
        LayoutBridge.setFontHandle(handle);
        GuideGlyphAtlas.instance()
            .setHeadless(true);
        MOCK_METRICS = createFontMetrics(fontData);
        System.out.println("font=" + fontPath);

        runRealDetails("closed");
        runRealDetails("open");
        runSpanCase("control_bs0", 0f);
        runSpanCase("bs_plus0_07", 0.07f);
        runSpanCase("bs_plus0_09", 0.09f);

        LayoutBridge.destroy(handle);
        System.out.println("DONE");
    }
}
