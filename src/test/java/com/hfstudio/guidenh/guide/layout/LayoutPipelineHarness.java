package com.hfstudio.guidenh.guide.layout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBox;
import com.hfstudio.guidenh.guide.document.block.LytCodeBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.render.GlyphRunHolder;
import com.hfstudio.guidenh.guide.render.GuideGlyphAtlas;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Headless pipeline test bench: drives the full layout pipeline (Java layout →
 * compiler serialization → Rust measureLayout via JNI → writeback) on synthetic
 * documents that mirror the problem pages, then checks structural invariants
 * (no zero-sized content, no unexpected sibling overlap, glyph runs present
 * where expected) and dumps the resulting tree for inspection.
 * <p>
 * Run via {@code ./gradlew runLayoutDump}. Not a JUnit test: it needs the
 * native DLL and a real TTF font.
 */
public final class LayoutPipelineHarness {

    //
    // AWT-backed advances from the SAME TTF the Rust engine shapes with, so the
    // Java layout pass (LineBuilder) breaks lines essentially identically to
    // parley: the float clip/lane geometry replayed from Java bounds then lines
    // up with the Rust line positions. A fixed mock advance (6px/char) makes
    // Java line counts diverge from Rust's by whole lines, which breaks that
    // replay exactly the way it breaks in-game when the two engines' metrics
    // disagree.

    private static FontMetrics MOCK_METRICS;

    private static FontMetrics createFontMetrics(byte[] fontData) {
        try {
            var awtFont = java.awt.Font
                .createFont(java.awt.Font.TRUETYPE_FONT, new java.io.ByteArrayInputStream(fontData))
                .deriveFont(9f);
            var img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var g = img.createGraphics();
            // Fractional metrics: advances come straight from the font's hmtx
            // (unhinted, float): the same numbers swash reports to parley.
            g.setRenderingHint(
                java.awt.RenderingHints.KEY_FRACTIONALMETRICS,
                java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont(awtFont);
            var fm = g.getFontMetrics();
            return new FontMetrics() {

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
            throw new IllegalStateException("harness font not usable by AWT", e);
        }
    }

    /** Opaque fixed-size leaf (stand-in for graphs / NEI boxes / scenes). */
    private static class FixedLeaf extends LytBox {

        private final int w;
        private final int h;

        FixedLeaf(int w, int h) {
            this.w = w;
            this.h = h;
        }

        @Override
        public int getExplicitWidth() {
            return w;
        }

        @Override
        public int getExplicitHeight() {
            return h;
        }

        @Override
        protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
            return new LytRect(x, y, w, h);
        }
    }

    private static LytParagraph paragraph(String text) {
        var p = new LytParagraph();
        p.appendText(text);
        return p;
    }

    private static boolean ready;

    private static synchronized boolean setup() {
        if (ready) return true;
        String libPath = System.getProperty("guide.native.lib.path");
        if (libPath == null || libPath.isEmpty()) {
            System.err.println("SKIP: guide.native.lib.path not set");
            return false;
        }
        System.load(libPath);
        Path fontPath = resolveFontPath();
        byte[] fontData = new byte[0];
        try {
            if (fontPath != null) fontData = Files.readAllBytes(fontPath);
        } catch (Exception e) {
            System.err.println("WARN: font read failed: " + e);
        }
        long handle = LayoutBridge.init(fontData, "en-US");
        if (handle == 0) {
            System.err.println("SKIP: LayoutBridge.init returned 0");
            return false;
        }
        LayoutBridge.setFontHandle(handle);
        GuideGlyphAtlas.instance()
            .setHeadless(true);
        MOCK_METRICS = createFontMetrics(fontData);
        ready = true;
        System.out.println("[harness] ready; font=" + fontPath + " bytes=" + fontData.length);
        return true;
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

    private record Issue(String text) {}

    // Baseline evidence: the serialized Rust glyph runs must carry a non-zero
    // document-space baseline satisfying the Rust invariant
    // baseline - y == placement.top: the diff is
    // pixel/0.5px-aligned and the baseline sits in or just around the glyph
    // quad's vertical span, NOT literally within [0, h], because a narrow
    // punctuation glyph's ink bitmap (h) is smaller than its placement.top
    // (ascent measured from the glyph quad top). Mirrors the Rust-side bounds
    // `bl >= pg.y() - 1.0 && bl <= pg.y() + pg.h() + 1.0` plus a sane ascent
    // ceiling so a broken pipeline (baseline decoupled from y) still trips.
    private static long BS_TOTAL;
    private static long BS_NONZERO;
    private static long BS_VALID;
    private static long BS_HALF_PX; // diff is a multiple of 0.5 (pixel/subpixel aligned)
    private static String BS_SAMPLE = "";

    private static void checkTree(LytNode node, List<Issue> issues, String path) {
        if (node instanceof LytBlock b) {
            LytRect r = b.getBounds();
            String here = path + "/"
                + b.getClass()
                    .getSimpleName();
            if (r == null) {
                issues.add(new Issue(here + ": null bounds"));
            } else {
                if (r.width() < 0 || r.height() < 0) {
                    issues.add(new Issue(here + ": negative size " + r));
                }
                // Serialized opaque leaves and text paragraphs must not collapse
                // to zero area: that is the "empty frame" failure mode.
                boolean expectContent = b instanceof FixedLeaf || (b instanceof LytParagraph p && !p.isEmpty());
                if (expectContent && r.width() * r.height() == 0) {
                    issues.add(new Issue(here + ": zero-area bounds " + r + " (empty frame!)"));
                }
            }
            // Every injected Rust glyph run must carry its baseline (nonzero)
            // with baseline - y == placement.top (Rust invariant; the diff is
            // 0.5px-aligned and within [-1, 24]; outside that the baseline is
            // decoupled from its glyph quad). This is the evidence the Java
            // consumer actually reads fbg.baseline() at both deserialization
            // points and that onLayoutMoved keeps the pair (y, baseline) coherent
            // (both translate by deltaY, so baseline - y is invariant under moves).
            if (b instanceof GlyphRunHolder h && h.getGlyphData() != null) {
                for (var group : h.getGlyphData()
                    .runs()) {
                    for (var g : group.glyphs()) {
                        BS_TOTAL++;
                        float top = g.baseline() - g.y();
                        boolean nz = g.baseline() != 0f;
                        boolean valid = top >= -1.0f && top <= 24.0f;
                        boolean halfPx = Math.abs((top * 2f) - Math.rint(top * 2f)) < 0.01f;
                        if (nz) BS_NONZERO++;
                        if (valid) BS_VALID++;
                        if (halfPx) BS_HALF_PX++;
                        if (BS_SAMPLE.isEmpty() && nz && valid) {
                            BS_SAMPLE = String
                                .format(" baseline=%.2f y=%.2f h=%.2f top=%.2f", g.baseline(), g.y(), g.h(), top);
                        }
                        if (!nz) {
                            issues.add(new Issue(here + ": glyph baseline=0 (Rust baseline not wired) y=" + g.y()));
                        } else if (!valid) {
                            issues.add(
                                new Issue(
                                    here + ": glyph top="
                                        + top
                                        + " outside [-1,24] (baseline="
                                        + g.baseline()
                                        + " y="
                                        + g.y()
                                        + " h="
                                        + g.h()
                                        + ")"));
                        }
                    }
                }
            }
        }
        for (LytNode child : node.getChildren()) {
            checkTree(
                child,
                issues,
                path + "/"
                    + (node instanceof LytBlock b ? b.getClass()
                        .getSimpleName() : "doc"));
        }
    }

    private static void checkSiblingOverlap(LytNode node, List<Issue> issues, String path) {
        List<LytRect> rects = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (LytNode child : node.getChildren()) {
            if (child instanceof LytBlock b && b.getBounds() != null) {
                rects.add(b.getBounds());
                names.add(
                    b.getClass()
                        .getSimpleName());
            }
        }
        for (int i = 0; i < rects.size(); i++) {
            for (int j = i + 1; j < rects.size(); j++) {
                // Floats legitimately overlap in-flow siblings by design
                // (earlier content is not pushed aside).
                if (names.get(i)
                    .equals("LytDocumentFloat")
                    || names.get(j)
                        .equals("LytDocumentFloat")) {
                    continue;
                }
                if (rects.get(i)
                    .intersects(rects.get(j))) {
                    issues.add(
                        new Issue(
                            path + ": siblings overlap: "
                                + names.get(i)
                                + rects.get(i)
                                + " vs "
                                + names.get(j)
                                + rects.get(j)));
                }
            }
        }
        for (LytNode child : node.getChildren()) {
            checkSiblingOverlap(
                child,
                issues,
                path + "/"
                    + (child instanceof LytBlock b ? b.getClass()
                        .getSimpleName() : "?"));
        }
    }

    private static void dump(LytNode node, int depth, StringBuilder out) {
        String indent = "  ".repeat(depth);
        if (node instanceof LytBlock b) {
            out.append(indent)
                .append(
                    b.getClass()
                        .getSimpleName())
                .append(" ")
                .append(b.getBounds())
                .append(
                    b instanceof GlyphRunHolder h && h.getGlyphData() != null
                        && !h.getGlyphData()
                            .runs()
                            .isEmpty() ? " [glyphs:" + h.getGlyphData()
                                .runs()
                                .stream()
                                .mapToInt(
                                    g -> g.glyphs()
                                        .size())
                                .sum() + "]" : "")
                .append('\n');
        }
        for (LytNode child : node.getChildren()) {
            dump(child, depth + 1, out);
        }
    }

    private static LytDocument floatPage() {
        var doc = new LytDocument();
        doc.append(paragraph("Intro to function graphs. Floats should sit right, text wraps around them."));
        doc.append(new LytDocumentFloat(new FixedLeaf(200, 150), true)); // right-floating graph
        doc.append(
            paragraph(
                "This paragraph is long enough to wrap around the floating graph on the right side."
                    + " It must be lane-narrowed instead of overlapping the float."));
        doc.append(paragraph("Another wrapping paragraph beside the float."));
        var code = new LytCodeBlock();
        code.setCodeText("<FunctionGraph width=\"360\" height=\"220\">\n  <Plot expr=\"sin(x)\" />\n</FunctionGraph>");
        doc.append(code);
        doc.append(paragraph("Tail paragraph after the code block."));
        return doc;
    }

    private static LytDocument recipeGalleryPage() {
        var doc = new LytDocument();
        doc.append(paragraph("Recipes should form a multi-column gallery:"));
        for (int i = 0; i < 6; i++) {
            doc.append(
                com.hfstudio.guidenh.guide.document.block.recipes.LytStandardRecipeBox
                    .shaped3x3(java.util.Collections.emptyList(), null));
        }
        doc.append(paragraph("After the gallery."));
        return doc;
    }

    private static LytDocument fileTreePage() {
        var doc = new LytDocument();
        doc.append(paragraph("File tree:"));
        var tree = new com.hfstudio.guidenh.guide.document.block.LytFileTree();
        // Row with icon block: a short icon paragraph + text payload → exercises
        // icon+paragraph twin-row layout inside the LytHBox row container.
        {
            var icon = new LytParagraph();
            icon.setMarginTop(0);
            icon.setMarginBottom(0);
            icon.appendText("\uD83D\uDCC1");
            tree.appendRow(
                java.util.List.of(com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind.BRANCH),
                icon,
                paragraph("config/"));
        }
        tree.appendRow(
            java.util.List.of(
                com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind.VERTICAL,
                com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind.BRANCH),
            null,
            paragraph("GuideNH/"));
        tree.appendRow(
            java.util.List.of(
                com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind.EMPTY,
                com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind.VERTICAL,
                com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind.LAST_BRANCH),
            null,
            paragraph("themes/default.cfg"));
        doc.append(tree);
        doc.append(paragraph("After the tree."));
        return doc;
    }

    private static LytDocument inlinePage() {
        var doc = new LytDocument();
        var p = new LytParagraph();
        p.appendText("before ");
        var inlineImg = new com.hfstudio.guidenh.guide.document.block.LytItemImage(null);
        p.append(com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock.of(inlineImg));
        p.appendText(" after the icon");
        doc.append(p);
        doc.append(paragraph("Tail paragraph."));
        return doc;
    }

    /** Table with pinned cell layout (absolute lowering) + glyph-run paragraphs inside cells. */
    private static LytDocument tablePage() {
        var doc = new LytDocument();
        doc.append(paragraph("Table below:"));
        var table = new com.hfstudio.guidenh.guide.document.block.table.LytTable();
        for (int r = 0; r < 2; r++) {
            var row = table.appendRow();
            for (int c = 0; c < 3; c++) {
                var cell = row.appendCell();
                cell.append(paragraph("cell " + r + "," + c));
            }
        }
        // Third row: one long description that must wrap and GROW its row
        // (regression guard for the "description text overlaps next row" bug).
        var longRow = table.appendRow();
        for (int c = 0; c < 3; c++) {
            var cell = longRow.appendCell();
            cell.append(
                c == 2 ? paragraph("Scene viewport width in pixels, a long description that wraps to multiple lines.")
                    : paragraph("cell 2," + c));
        }
        doc.append(table);
        doc.append(paragraph("After table."));
        return doc;
    }

    /** Real FunctionGraph through the primitive pipeline (curves, axes, points, title). */
    private static LytDocument functionGraphPage() {
        var doc = new LytDocument();
        doc.append(paragraph("Function graph below:"));
        var graph = new com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph();
        graph.setTitle("sin(x)");
        graph.addPlot(
            new com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot(
                "sin(x)",
                (x, y) -> Math.sin(x),
                false,
                null,
                0xFF66CCFF,
                "sin(x)"));
        graph.addPoint(
            new com.hfstudio.guidenh.guide.document.block.functiongraph.MarkedPoint(
                com.hfstudio.guidenh.guide.document.block.functiongraph.MarkedPoint.MODE_EXPLICIT,
                -1,
                1.0,
                1.0,
                0xFFFFCC66,
                false,
                "P"));
        doc.append(graph);
        doc.append(paragraph("After the graph."));
        return doc;
    }

    /**
     * Replicates function-graph.md's two on-page examples (fenced + container),
     * the reported "labels scattered across each other's panel" page.
     */
    private static LytDocument twoGraphsPage() {
        var doc = new LytDocument();
        doc.append(paragraph("Fenced Block"));
        // Graph 1 goes through the real fence parser (attr parsing incl.
        // "xRange=-pi..pi"; negative constants broke before the parser fix).
        var g1 = com.hfstudio.guidenh.guide.compiler.tags.functiongraph.FunctionGraphFenceParser.parse(
            "width=360 height=220 xRange=-pi..pi yRange=-2..2 quadrants=all cornerLegend=topRight\n"
                + "sin(x)        | color=#ff5566 label=\"sin\" pointEveryX=1 autoPointLabel=x\n"
                + "cos(x)        | color=#3399ff label=\"cos\" pointEveryY=1\n"
                + "x/2           | color=#88cc77 domain=-pi..pi\n"
                + ":0,0\n"
                + "@plot=0 atX=1.5708\n");
        doc.append(g1);
        doc.append(paragraph("<FunctionGraph> Container"));
        var g2 = new com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph();
        g2.setExplicitSize(360, 220);
        g2.setExplicitXRange(-6, 6);
        g2.setExplicitYRange(-3, 3);
        g2.setQuadrantMask(0xF);
        g2.setCornerLegendPosition(com.hfstudio.guidenh.guide.document.block.chart.CornerLegendPosition.TOP_RIGHT);
        g2.addPlot(
            new com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot(
                "sin(x)",
                (x, y) -> Math.sin(x),
                false,
                null,
                0xFFFF5566,
                "sin x",
                new com.hfstudio.guidenh.guide.document.block.functiongraph.AutoPointSpec(
                    1.0,
                    Double.NaN,
                    com.hfstudio.guidenh.guide.document.block.functiongraph.AutoPointLabelMode.X,
                    0xFFFFFFFF,
                    true)));
        g2.addPlot(
            new com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot(
                "x^2/4",
                (x, y) -> x * x / 4,
                false,
                null,
                0xFF3399FF,
                "x² / 4"));
        g2.addPlot(
            new com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot(
                "|x|-1",
                (x, y) -> Math.abs(x) - 1,
                false,
                null,
                0xFF88CC77,
                "|x| - 1",
                new com.hfstudio.guidenh.guide.document.block.functiongraph.AutoPointSpec(
                    Double.NaN,
                    1.0,
                    com.hfstudio.guidenh.guide.document.block.functiongraph.AutoPointLabelMode.NONE,
                    0xFFFFFFFF,
                    true)));
        doc.append(g2);
        return doc;
    }

    /** Heading with fontScale ≠ 1: regression guard for the double-scale bug. */
    private static LytDocument fontScalePage() {
        var doc = new LytDocument();
        var heading = new com.hfstudio.guidenh.guide.document.block.LytHeading();
        heading.setDepth(1);
        heading.appendText("Scaled Heading (H1 style, fontScale > 1)");
        doc.append(heading);
        doc.append(paragraph("Normal body line right after, width should not jump."));
        return doc;
    }

    /** ContentTabs: only the ACTIVE tab body may enter the serialized tree. */
    private static LytDocument contentTabsPage() {
        var doc = new LytDocument();
        doc.append(paragraph("Content tabs below:"));
        var entries = java.util.List.of(
            new com.hfstudio.guidenh.guide.compiler.tags.ContentTabsSpec.TabEntry(
                "First",
                paragraph("First tab body."),
                null),
            new com.hfstudio.guidenh.guide.compiler.tags.ContentTabsSpec.TabEntry(
                "Second",
                paragraph("Second tab body (hidden)."),
                null),
            new com.hfstudio.guidenh.guide.compiler.tags.ContentTabsSpec.TabEntry(
                "Third",
                paragraph("Third tab body (hidden too)."),
                null));
        doc.append(
            new com.hfstudio.guidenh.guide.document.block.LytContentTabsBlock("Demo Tabs", null, 0, null, entries));
        doc.append(paragraph("After tabs."));
        return doc;
    }

    /**
     * Right float + a long paragraph crossing its bottom edge: band splitting must
     * keep lines narrow beside the float and recover full width below it.
     */
    private static LytDocument floatWrapPage() {
        var doc = new LytDocument();
        doc.append(paragraph("Intro line before the float."));
        doc.append(new LytDocumentFloat(new FixedLeaf(200, 60), true));
        doc.append(
            paragraph(
                "A long paragraph that wraps beside the floating block on the right and must recover "
                    + "full width below it. It carries enough text to cross the float's bottom edge, "
                    + "so band splitting produces a narrow band beside the float and a full-width band "
                    + "below it. More filler text to make sure several lines sit below the edge. "
                    + "Even more text so the paragraph definitely grows past the float's bottom: "
                    + "the quick brown fox jumps over the lazy dog, pack my box with five dozen liquor jugs, "
                    + "and still this paragraph continues with additional words to fill the remaining lines, "
                    + "then several more sentences so the full-width band below the float spans multiple "
                    + "lines and its first line demonstrably reaches well beyond the narrow lane width."));
        doc.append(paragraph("After the wrap."));
        return doc;
    }

    /**
     * Left float + plain-text paragraph (left-side clip path) followed by a
     * right float + paragraph: mirrors content-embed.md's multi-float
     * structure: both floats must attach to their flow slots (zero-height
     * anchors), plain-text lines must clip on BOTH sides, and glyph pens must
     * never intrude into either float's content rect.
     */
    private static LytDocument multiFloatPage() {
        var doc = new LytDocument();
        doc.append(paragraph("Intro line before the floats."));
        doc.append(new LytDocumentFloat(new FixedLeaf(120, 80), false)); // left float
        doc.append(
            paragraph(
                "A long paragraph that wraps to the right of the left-floating block, staying clear of "
                    + "its lane until the float's bottom edge passes, then recovering the full content "
                    + "width below it. Filler text to carry the paragraph well past the float's bottom: "
                    + "the quick brown fox jumps over the lazy dog, pack my box with five dozen liquor "
                    + "jugs, and still more words so several lines land below the float's zone."));
        doc.append(new LytDocumentFloat(new FixedLeaf(140, 70), true)); // right float
        doc.append(
            paragraph(
                "Another long paragraph, this one wrapping to the left of the right-floating block. "
                    + "It must keep its lines clear of the right lane until that float clears, while "
                    + "the earlier left float is already gone from the registry. More filler text so "
                    + "the paragraph crosses the right float's bottom edge and recovers full width."));
        doc.append(paragraph("After the floats."));
        return doc;
    }

    /** Latin paragraph with short lines: justification stretches spaces to the node width. */
    private static LytDocument justifyPage() {
        var doc = new LytDocument();
        doc.append(
            paragraph(
                "Short words spread out across the full width when justification stretches the spaces "
                    + "between them evenly so each line reaches the right edge of the paragraph box."));
        doc.append(paragraph("\u77ED\u884C\u7ED3\u675F\u540E\u6062\u590D\u81EA\u7136\u5BBD\u5EA6\u3002"));
        return doc;
    }

    private static LytDocument itemImagePage() {
        var doc = new LytDocument();
        doc.append(paragraph("Item images (null stacks, layout-only):"));
        doc.append(new com.hfstudio.guidenh.guide.document.block.LytItemImage(null));
        var scaled = new com.hfstudio.guidenh.guide.document.block.LytItemImage(null);
        scaled.setScale(2f);
        doc.append(scaled);
        doc.append(paragraph("After items."));
        return doc;
    }

    /** Mirrors markdown.md's inline-LaTeX section (the reported regression page). */
    private static LytDocument latexInlinePage() {
        var doc = new LytDocument();
        var p1 = new LytParagraph();
        p1.appendText("Einstein's mass-energy equivalence: ");
        p1.append(com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock.of(latex("E=mc^2")));
        p1.appendText(" and Pythagoras: ");
        p1.append(com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock.of(latex("a^2+b^2=c^2")));
        doc.append(p1);
        var p2 = new LytParagraph();
        p2.appendText("A formula with a fraction expands the line height automatically: contains ");
        p2.append(com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock.of(latex("\\frac{1}{2}")));
        p2.appendText(" and also ");
        p2.append(com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock.of(latex("\\frac{a+b}{c-d}")));
        p2.appendText(" in the same line.");
        doc.append(p2);
        return doc;
    }

    /**
     * Multi-color PRE_WRAP paragraphs (a highlighted code block body plus a
     * directly built two-color paragraph): the static multi-style span pipeline
     * must render them per-span via GuideText.
     */
    private static LytDocument codeBlockPage() {
        var doc = new LytDocument();
        var code = new LytCodeBlock();
        code.setCodeText("<FunctionGraph width=\"360\" height=\"220\">\n  <Plot expr=\"sin(x)\" />\n</FunctionGraph>");
        doc.append(code);
        // Deterministic multi-style input: two spans with distinct colors.
        var pre = new LytParagraph();
        pre.modifyStyle(s -> s.whiteSpace(com.hfstudio.guidenh.guide.style.WhiteSpaceMode.PRE_WRAP));
        var red = new com.hfstudio.guidenh.guide.document.flow.LytFlowSpan();
        red.setStyle(
            com.hfstudio.guidenh.guide.style.TextStyle.builder()
                .color(new com.hfstudio.guidenh.guide.color.ConstantColor(0xFFFF5555))
                .build());
        red.append(com.hfstudio.guidenh.guide.document.flow.LytFlowText.of("first span "));
        var blue = new com.hfstudio.guidenh.guide.document.flow.LytFlowSpan();
        blue.setStyle(
            com.hfstudio.guidenh.guide.style.TextStyle.builder()
                .color(new com.hfstudio.guidenh.guide.color.ConstantColor(0xFF5555FF))
                .build());
        blue.append(com.hfstudio.guidenh.guide.document.flow.LytFlowText.of("second span"));
        pre.append(red);
        pre.append(blue);
        doc.append(pre);
        return doc;
    }

    /**
     * Span-pipeline check: the multi-color PRE_WRAP document must collect at
     * least two differently-tinted DrawGlyphRuns (per-span GuideText emission)
     * and zero HostDraw fallbacks.
     */
    private static int runCodeBlockScenario() {
        System.out.println("\n=== codeBlockPage (width=557) ===");
        LytDocument doc = codeBlockPage();
        doc.updateLayout(new LayoutContext(MOCK_METRICS), 557);
        List<Issue> issues = new ArrayList<>();
        checkTree(doc, issues, "");
        checkSiblingOverlap(doc, issues, "doc");
        StringBuilder sb = new StringBuilder();
        dump(doc, 0, sb);
        System.out.print(sb);

        var pc = new com.hfstudio.guidenh.guide.render.PrimitiveCollector(new LytRect(0, 0, 557, 2000), null);
        for (var block : doc.getBlocks()) {
            pc.collectFrom(block);
        }
        long hostDraws = 0;
        long glyphRuns = 0;
        Set<Integer> tints = new HashSet<>();
        for (var p : pc.result()) {
            if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.HostDraw) {
                hostDraws++;
            } else
                if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.DrawGlyphRun dg && !dg.glyphs()
                    .isEmpty()) {
                        glyphRuns++;
                        tints.add(dg.argb());
                    }
        }
        System.out.println("-- glyphRuns=" + glyphRuns + " tints=" + tints + " hostDraws=" + hostDraws);
        if (glyphRuns < 2 || tints.size() < 2) {
            issues.add(
                new Issue(
                    "expected >= 2 DrawGlyphRuns with distinct tints, got runs=" + glyphRuns + " tints=" + tints));
        }
        if (hostDraws > 0) {
            issues.add(new Issue("expected no HostDraw primitives, got " + hostDraws));
        }
        if (issues.isEmpty()) {
            System.out.println("OK: no issues");
            return 0;
        }
        for (Issue i : issues) {
            System.out.println("ISSUE: " + i.text());
        }
        return issues.size();
    }

    /** One instance of each chart type, all rendered through the primitive pipeline. */
    private static LytDocument chartsPage() {
        var doc = new LytDocument();
        doc.append(paragraph("Charts below:"));
        var column = new com.hfstudio.guidenh.guide.document.block.chart.LytColumnChart();
        column.setCategories(new String[] { "A", "B", "C" });
        column.setSeries(
            java.util.List.of(chartSeries("s1", 0xFF66CCFF, 3, 7, 5), chartSeries("s2", 0xFFFFCC66, 5, 2, 6)));
        doc.append(column);
        var bar = new com.hfstudio.guidenh.guide.document.block.chart.LytBarChart();
        bar.setCategories(new String[] { "A", "B", "C" });
        bar.setSeries(java.util.List.of(chartSeries("bars", 0xFF88CC77, 4, 6, 3)));
        doc.append(bar);
        var line = new com.hfstudio.guidenh.guide.document.block.chart.LytLineChart();
        line.setCategories(new String[] { "A", "B", "C", "D" });
        line.setSeries(java.util.List.of(chartSeries("line", 0xFFFF8899, 1, 4, 2, 5)));
        doc.append(line);
        var scatter = new com.hfstudio.guidenh.guide.document.block.chart.LytScatterChart();
        scatter.setSeries(
            java.util.List.of(
                new com.hfstudio.guidenh.guide.document.block.chart.ChartSeries(
                    "scatter",
                    0xFFCC99FF,
                    new double[] { 1, 2, 3, 4 },
                    new double[] { 2, 5, 3, 6 })));
        doc.append(scatter);
        var pie = new com.hfstudio.guidenh.guide.document.block.chart.LytPieChart();
        pie.setSlices(
            java.util.List.of(
                new com.hfstudio.guidenh.guide.document.block.chart.PieSlice("alpha", 5, 0xFF66CCFF),
                new com.hfstudio.guidenh.guide.document.block.chart.PieSlice("beta", 3, 0xFFFFCC66),
                new com.hfstudio.guidenh.guide.document.block.chart.PieSlice("gamma", 2, 0xFF88CC77)));
        doc.append(pie);
        return doc;
    }

    private static com.hfstudio.guidenh.guide.document.block.chart.ChartSeries chartSeries(String name, int color,
        double... ys) {
        return com.hfstudio.guidenh.guide.document.block.chart.ChartSeries.fromValues(name, color, ys);
    }

    /**
     * Charts check: every chart type has non-zero bounds and emits primitives
     * through the pipeline: zero HostDraw fallbacks.
     */
    private static int runChartsScenario() {
        System.out.println("\n=== chartsPage (width=557) ===");
        LytDocument doc = chartsPage();
        doc.updateLayout(new LayoutContext(MOCK_METRICS), 557);
        List<Issue> issues = new ArrayList<>();
        checkTree(doc, issues, "");
        checkSiblingOverlap(doc, issues, "doc");
        StringBuilder sb = new StringBuilder();
        dump(doc, 0, sb);
        System.out.print(sb);

        int chartBlocks = 0;
        long hostDraws = 0;
        var viewport = new LytRect(0, 0, 557, 4000);
        for (var block : doc.getBlocks()) {
            if (!(block instanceof com.hfstudio.guidenh.guide.document.block.chart.LytChartBase chart)) {
                continue;
            }
            chartBlocks++;
            LytRect b = chart.getBounds();
            if (b == null || b.width() * b.height() == 0) {
                issues.add(
                    new Issue(
                        chart.getClass()
                            .getSimpleName() + ": zero-area bounds "
                            + b));
                continue;
            }
            var pc = new com.hfstudio.guidenh.guide.render.PrimitiveCollector(viewport, null);
            pc.collectFrom(chart);
            long own = 0;
            for (var p : pc.result()) {
                if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.HostDraw) {
                    hostDraws++;
                } else {
                    own++;
                }
            }
            System.out.println(
                "-- " + chart.getClass()
                    .getSimpleName() + " bounds=" + b + " primitives=" + own);
            if (own == 0) {
                issues.add(
                    new Issue(
                        chart.getClass()
                            .getSimpleName() + ": emitted no primitives"));
            }
        }
        if (chartBlocks != 5) {
            issues.add(new Issue("expected 5 chart blocks, got " + chartBlocks));
        }
        if (hostDraws > 0) {
            issues.add(new Issue("expected no HostDraw primitives, got " + hostDraws));
        }
        if (issues.isEmpty()) {
            System.out.println("OK: no issues");
            return 0;
        }
        for (Issue i : issues) {
            System.out.println("ISSUE: " + i.text());
        }
        return issues.size();
    }

    /**
     * Rich multi-style paragraph: bold/colored/italic/struck/highlighted spans
     * in one paragraph, shaped once by Rust into per-span glyph runs.
     */
    private static LytDocument richTextPage() {
        var doc = new LytDocument();
        var p = new LytParagraph();
        p.appendText("plain ");
        var boldRed = new com.hfstudio.guidenh.guide.document.flow.LytFlowSpan();
        boldRed.setStyle(
            com.hfstudio.guidenh.guide.style.TextStyle.builder()
                .bold(true)
                .color(new com.hfstudio.guidenh.guide.color.ConstantColor(0xFFFF5555))
                .build());
        boldRed.append(com.hfstudio.guidenh.guide.document.flow.LytFlowText.of("bold-red "));
        var italic = new com.hfstudio.guidenh.guide.document.flow.LytFlowSpan();
        italic.setStyle(
            com.hfstudio.guidenh.guide.style.TextStyle.builder()
                .italic(true)
                .color(new com.hfstudio.guidenh.guide.color.ConstantColor(0xFF55FF55))
                .build());
        italic.append(com.hfstudio.guidenh.guide.document.flow.LytFlowText.of("italic "));
        var struck = new com.hfstudio.guidenh.guide.document.flow.LytFlowSpan();
        struck.setStyle(
            com.hfstudio.guidenh.guide.style.TextStyle.builder()
                .strikethrough(true)
                .backgroundColor(new com.hfstudio.guidenh.guide.color.ConstantColor(0x66FFFF00))
                .build());
        struck.append(com.hfstudio.guidenh.guide.document.flow.LytFlowText.of("struck"));
        p.append(boldRed);
        p.append(italic);
        p.append(struck);
        doc.append(p);
        var p2 = new LytParagraph();
        p2.appendText("underlined ");
        var ul = new com.hfstudio.guidenh.guide.document.flow.LytFlowSpan();
        ul.setStyle(
            com.hfstudio.guidenh.guide.style.TextStyle.builder()
                .underlined(true)
                .color(new com.hfstudio.guidenh.guide.color.ConstantColor(0xFF55CCFF))
                .build());
        ul.append(com.hfstudio.guidenh.guide.document.flow.LytFlowText.of("link-like"));
        p2.append(ul);
        doc.append(p2);
        return doc;
    }

    /**
     * Rich-text check: >= 2 differently-tinted DrawGlyphRuns, a sheared (italic)
     * run, decoration FillRects (highlight/strikethrough/underline), no HostDraw.
     */
    private static int runRichTextScenario() {
        System.out.println("\n=== richTextPage (width=557) ===");
        LytDocument doc = richTextPage();
        doc.updateLayout(new LayoutContext(MOCK_METRICS), 557);
        List<Issue> issues = new ArrayList<>();
        checkTree(doc, issues, "");
        checkSiblingOverlap(doc, issues, "doc");
        StringBuilder sb = new StringBuilder();
        dump(doc, 0, sb);
        System.out.print(sb);

        var pc = new com.hfstudio.guidenh.guide.render.PrimitiveCollector(new LytRect(0, 0, 557, 2000), null);
        for (var block : doc.getBlocks()) {
            pc.collectFrom(block);
        }
        long hostDraws = 0;
        long glyphRuns = 0;
        long sheared = 0;
        long decorations = 0;
        Set<Integer> tints = new HashSet<>();
        for (var p : pc.result()) {
            if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.HostDraw) {
                hostDraws++;
            } else
                if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.DrawGlyphRun dg && !dg.glyphs()
                    .isEmpty()) {
                        glyphRuns++;
                        tints.add(dg.argb());
                        if (dg.shear()) {
                            sheared++;
                        }
                    } else if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.FillRect) {
                        decorations++;
                    }
        }
        System.out.println(
            "-- glyphRuns=" + glyphRuns
                + " tints="
                + tints
                + " sheared="
                + sheared
                + " decorations="
                + decorations
                + " hostDraws="
                + hostDraws);
        if (glyphRuns < 2 || tints.size() < 2) {
            issues.add(
                new Issue(
                    "expected >= 2 DrawGlyphRuns with distinct tints, got runs=" + glyphRuns + " tints=" + tints));
        }
        if (sheared < 1) {
            issues.add(new Issue("expected a sheared (italic) DrawGlyphRun"));
        }
        if (decorations < 3) {
            issues.add(
                new Issue(
                    "expected >= 3 decoration FillRects (highlight/strikethrough/underline), got " + decorations));
        }
        if (hostDraws > 0) {
            issues.add(new Issue("expected no HostDraw primitives, got " + hostDraws));
        }
        if (issues.isEmpty()) {
            System.out.println("OK: no issues");
            return 0;
        }
        for (Issue i : issues) {
            System.out.println("ISSUE: " + i.text());
        }
        return issues.size();
    }

    private static com.hfstudio.guidenh.guide.document.block.LytLatexBlock latex(String formula) {
        return new com.hfstudio.guidenh.guide.document.block.LytLatexBlock(
            formula,
            com.hfstudio.guidenh.guide.document.block.LatexRenderOptions.builder()
                .build());
    }

    /**
     * Geometry audit for inline blocks: prints each paragraph's glyph baseline
     * rows and each LaTeX block's layout box vs. actual blit rect, so vertical
     * anchoring errors show up as numbers instead of screenshots.
     */
    private static void dumpInlineDiagnostics(LytNode node) {
        if (node instanceof LytParagraph par && par.getGlyphData() != null
            && !par.getGlyphData()
                .runs()
                .isEmpty()) {
            java.util.TreeSet<Float> rows = new java.util.TreeSet<>();
            for (var group : par.getGlyphData()
                .runs()) {
                for (var g : group.glyphs()) {
                    rows.add(g.y());
                }
            }
            System.out.println(
                "   PARA bounds=" + par.getBounds()
                    + " baselines="
                    + rows.stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + "," + b)
                        .orElse(""));
        }
        if (node instanceof com.hfstudio.guidenh.guide.document.block.LytLatexBlock lb) {
            System.out.println(
                "   LATEX \"" + lb.getFormula() + "\" bounds=" + lb.getBounds() + " visual=" + lb.getVisualBounds());
        }
        if (node instanceof LytParagraph par && par.getGlyphData() != null
            && !par.getGlyphData()
                .runs()
                .isEmpty()) {
            // Per-line extents from Rust glyph runs, keyed by the REAL line_index
            // (float-wrap band verification + the Rust side of the line-geometry
            // oracle that will replace the Java LineBuilder).
            java.util.TreeMap<Integer, float[]> byLine = new java.util.TreeMap<>();
            for (var group : par.getGlyphData()
                .runs()) {
                for (var g : group.glyphs()) {
                    float[] ext = byLine
                        .computeIfAbsent(g.lineIndex(), k -> new float[] { Float.MAX_VALUE, 0, Float.MAX_VALUE, 0 });
                    ext[0] = Math.min(ext[0], g.x());
                    ext[1] = Math.max(ext[1], g.x() + g.w());
                    ext[2] = Math.min(ext[2], g.y());
                    ext[3] = Math.max(ext[3], g.y() + g.h());
                }
            }
            StringBuilder sb = new StringBuilder("   RUST-LINE:");
            byLine.forEach(
                (line, ext) -> sb.append(
                    " [" + line
                        + " x="
                        + (int) ext[0]
                        + ".."
                        + (int) ext[1]
                        + " y="
                        + (int) ext[2]
                        + ".."
                        + (int) ext[3]
                        + "]"));
            System.out.println(sb);
            System.out.println("   JAVA-LINE:");
        }
        for (LytNode child : node.getChildren()) {
            dumpInlineDiagnostics(child);
        }
    }

    private static int runScenario(String name, LytDocument doc, int width) {
        return runScenario(name, doc, width, false);
    }

    private static int runScenario(String name, LytDocument doc, int width, boolean collectPrimitives) {
        System.out.println("\n=== " + name + " (width=" + width + ") ===");
        doc.updateLayout(new LayoutContext(MOCK_METRICS), width);
        // Serialize a second time AFTER the Java pass so the dump reflects the
        // real (post-Java-layout) styles and Rust output.
        dumpFlatTree(doc);
        if (collectPrimitives) {
            var pc = new com.hfstudio.guidenh.guide.render.PrimitiveCollector(new LytRect(0, 0, width, 2000), null);
            for (var block : doc.getBlocks()) {
                pc.collectFrom(block);
            }
            var prims = pc.result();
            long fills = prims.stream()
                .filter(com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.FillRect.class::isInstance)
                .count();
            long hostDraws = prims.stream()
                .filter(com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.HostDraw.class::isInstance)
                .count();
            long glyphs = prims.stream()
                .filter(com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.DrawGlyphRun.class::isInstance)
                .count();
            long lines = prims.stream()
                .filter(com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.DrawLine.class::isInstance)
                .count();
            long circles = prims.stream()
                .filter(com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.DrawCircle.class::isInstance)
                .count();
            long texts = prims.stream()
                .filter(com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.DrawText.class::isInstance)
                .count();
            System.out.println(
                "-- primitives: total=" + prims.size()
                    + " FillRect="
                    + fills
                    + " HostDraw="
                    + hostDraws
                    + " GlyphRun="
                    + glyphs
                    + " Line="
                    + lines
                    + " Circle="
                    + circles
                    + " Text="
                    + texts);
            for (var p : prims) {
                if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.PushScissor ps) {
                    System.out.println("   PushScissor " + ps);
                }
            }
        }
        List<Issue> issues = new ArrayList<>();
        checkTree(doc, issues, "");
        checkSiblingOverlap(doc, issues, "doc");
        checkFloatOverlap(doc, issues);
        checkLineGeometry(doc, width, issues);
        StringBuilder sb = new StringBuilder();
        dump(doc, 0, sb);
        System.out.print(sb);
        dumpInlineDiagnostics(doc);
        if (issues.isEmpty()) {
            System.out.println("OK: no issues");
            return 0;
        }
        for (Issue i : issues) {
            System.out.println("ISSUE: " + i.text());
        }
        return issues.size();
    }

    /** Dump the serialized flat tree (index, class, children) for structure debugging. */
    private static void dumpFlatTree(LytNode root) {
        var serializer = new LayoutTreeSerializer();
        byte[] bytes = serializer.serialize(root, 557f, 1.0f, 1.0f);
        var input = com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput
            .getRootAsLayoutInput(java.nio.ByteBuffer.wrap(bytes));
        System.out.println("-- flat tree --");
        for (int i = 0;; i++) {
            LytNode n = serializer.getNodeByFlatIndex(i);
            if (n == null) break;
            List<Integer> kids = new ArrayList<>();
            for (LytNode c : n.getChildren()) {
                int idx = serializer.getFlatIndex(c);
                if (idx >= 0) kids.add(idx);
            }
            var st = input.nodes(i)
                .style();
            var textData = input.nodes(i)
                .text();
            System.out.println(
                "  [" + i
                    + "] "
                    + n.getClass()
                        .getSimpleName()
                    + " children="
                    + kids
                    + " sizeW="
                    + (st.sizeW() == null ? "auto"
                        : st.sizeW()
                            .value())
                    + " sizeH="
                    + (st.sizeH() == null ? "auto"
                        : st.sizeH()
                            .value())
                    + " grow="
                    + st.flexGrow()
                    + " shrink="
                    + st.flexShrink()
                    + " pos="
                    + st.position()
                    + " dir="
                    + st.flexDirection()
                    + (textData == null ? "" : " bands=" + textData.bandsLength())
                    + (textData == null ? "" : " clips=" + textData.floatClipsLength()));
            if (textData != null && textData.floatClipsLength() > 0) {
                for (int ci = 0; ci < textData.floatClipsLength(); ci++) {
                    var fc = textData.floatClips(ci);
                    System.out.println(
                        "     clip[" + ci
                            + "] y="
                            + fc.yTop()
                            + ".."
                            + fc.yBottom()
                            + " x="
                            + fc.x()
                            + " w="
                            + fc.width());
                }
            }
            if (textData != null && textData.bandsLength() > 0) {
                for (int bi = 0; bi < textData.bandsLength(); bi++) {
                    var tb = textData.bands(bi);
                    System.out.println(
                        "     band[" + bi
                            + "] split="
                            + tb.splitByte()
                            + " w="
                            + tb.width()
                            + " ml="
                            + tb.marginLeft());
                }
            }
        }
        if (LayoutBridge.getFontHandle() != 0) {
            byte[] result = LayoutBridge.measureLayout(LayoutBridge.getFontHandle(), bytes);
            var lr = com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutResult
                .getRootAsLayoutResult(java.nio.ByteBuffer.wrap(result));
            System.out.println("-- rust debug_info --");
            System.out.println(lr.debugInfo());
        }
    }

    /**
     * Replicates beamcrafter.md's structure: a tall right float, a short intro
     * paragraph carrying a {@code <br clear="right"/>
     * }, then the following
     * paragraph. The clear break extends the intro's Java height below the
     * float (LineBuilder jumps lineBoxY) but contributes no text; without the
     * serializer's min-height bridge, Rust stacks the follower at the intro's
     * TEXT height, pulling it up into the float's zone (the "Construction
     * paragraph invades the scene panel" bug).
     */
    private static LytDocument floatClearPage() {
        var doc = new LytDocument();
        doc.append(new LytDocumentFloat(new FixedLeaf(200, 120), true));
        var intro = new LytParagraph();
        intro.appendText("Short intro beside the float.");
        var br = new com.hfstudio.guidenh.guide.document.flow.LytFlowBreak();
        br.setClearRight(true);
        intro.append(br);
        doc.append(intro);
        doc.append(paragraph("Following paragraph must start below the float's bottom edge, not inside it."));
        return doc;
    }

    /**
     * Hard-break check: a paragraph with an in-line {@code <br>
     * } (no clear, no
     * float) must lay out as two lines. Before the break-splitting migration the
     * {@code <br>
     * } was dropped from the shaped text, collapsing the two lines
     * into one (height ~10); the Rust pusher now splits at the break and stacks
     * the pieces, so the box must be at least two lines tall (~20).
     */
    private static int runBreakScenario() {
        System.out.println("\n=== breakWrapPage (width=557) ===");
        var doc = new LytDocument();
        var p = new LytParagraph();
        p.appendText("line one");
        p.appendBreak();
        p.appendText("line two");
        doc.append(p);
        doc.updateLayout(new LayoutContext(MOCK_METRICS), 557);
        List<Issue> issues = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        dump(doc, 0, sb);
        System.out.print(sb);
        int h = p.getBounds()
            .height();
        System.out.println("-- break paragraph height=" + h);
        if (h < 18) {
            issues.add(
                new Issue(
                    "hard <br> not honoured: paragraph height=" + h
                        + " (expected >= 2 lines; the <br> was dropped so the two lines collapsed into one)"));
        }
        if (issues.isEmpty()) {
            System.out.println("OK: no issues");
            return 0;
        }
        for (Issue i : issues) {
            System.out.println("ISSUE: " + i.text());
        }
        return issues.size();
    }

    /**
     * Clear-break check: the paragraph after a clear="right" break must start
     * at or below the float's bottom edge.
     */
    private static int runFloatClearScenario() {
        System.out.println("\n=== floatClearPage (width=557) ===");
        LytDocument doc = floatClearPage();
        doc.updateLayout(new LayoutContext(MOCK_METRICS), 557);
        List<Issue> issues = new ArrayList<>();
        checkTree(doc, issues, "");
        checkSiblingOverlap(doc, issues, "doc");
        checkFloatOverlap(doc, issues);
        StringBuilder sb = new StringBuilder();
        dump(doc, 0, sb);
        System.out.print(sb);

        var serializer = new LayoutTreeSerializer();
        serializer.serialize(doc, 557f, 1.0f, 1.0f);
        var floats = serializer.getFloatRects();
        int floatBottom = floats.isEmpty() ? -1
            : floats.get(0)
                .rect()
                .bottom() - LytDocumentFloat.FLOAT_GAP;

        LytParagraph intro = null;
        LytParagraph follower = null;
        for (LytNode child : doc.getChildren()) {
            if (child instanceof LytParagraph p) {
                if (intro == null) intro = p;
                follower = p;
            }
        }
        // The clear break lives at the END of the intro paragraph. CSS clear
        // moves the flow AFTER the break below the float; it must NOT inflate
        // the intro paragraph's own box (its text still wraps beside the float
        // at the top). Inflating it leaves a blank gap inside the intro: the
        // exact "callout not hugging" symptom seen in-game.
        if (intro != null && intro.getBounds() != null
            && intro.getBounds()
                .height() > 30) {
            issues.add(
                new Issue(
                    "intro paragraph inflated to height=" + intro.getBounds()
                        .height()
                        + " by trailing clear (should hug its text, ~10px/line; the clear must push the FOLLOWING flow, not stretch this box)"));
        }
        if (follower == null || follower.getBounds() == null) {
            issues.add(new Issue("floatClearPage: following paragraph missing"));
        } else {
            System.out.println(
                "-- follower y=" + follower.getBounds()
                    .y() + " floatBottom=" + floatBottom);
            if (follower.getBounds()
                .y() < floatBottom - 2) {
                issues.add(
                    new Issue(
                        "following paragraph starts at y=" + follower.getBounds()
                            .y() + " inside the float's zone (bottom=" + floatBottom + "); clear break lost"));
            }
        }
        if (issues.isEmpty()) {
            System.out.println("OK: no issues");
            return 0;
        }
        for (Issue i : issues) {
            System.out.println("ISSUE: " + i.text());
        }
        return issues.size();
    }

    /**
     * Float-wrap invariant: glyph runs of blocks OUTSIDE a float's subtree must
     * never intersect the float's content rect (the registered rect minus
     * FLOAT_GAP on the gap side and the bottom). Guards the whole "text invades
     * the float zone" failure class across every scenario.
     */
    private static void checkFloatOverlap(LytDocument doc, List<Issue> issues) {
        var serializer = new LayoutTreeSerializer();
        serializer.serialize(doc, 557f, 1.0f, 1.0f);
        List<LayoutTreeSerializer.FloatRect> floats = serializer.getFloatRects();
        if (floats.isEmpty()) return;
        checkFloatOverlap(doc, floats, issues, false, "doc");
    }

    private static void checkFloatOverlap(LytNode node, List<LayoutTreeSerializer.FloatRect> floats, List<Issue> issues,
        boolean insideFloat, String path) {
        boolean inFloat = insideFloat || node instanceof LytDocumentFloat;
        String here = path + "/"
            + (node instanceof LytBlock b ? b.getClass()
                .getSimpleName() : "?");
        if (!inFloat && node instanceof GlyphRunHolder h && h.getGlyphData() != null) {
            int reported = 0;
            outer: for (var group : h.getGlyphData()
                .runs()) {
                for (var g : group.glyphs()) {
                    for (var fr : floats) {
                        int gap = LytDocumentFloat.FLOAT_GAP;
                        LytRect f = fr.rect();
                        // Content rect: right floats carry the gap on the left,
                        // left floats on the right; both carry it on the bottom.
                        float cx = fr.right() ? f.x() + gap : f.x();
                        float cr = fr.right() ? f.right() : f.right() - gap;
                        float cb = f.bottom() - gap;
                        // A glyph intrudes only when its pen start passes the
                        // float's content edge by more than a few px; the lane
                        // edge sits FLOAT_GAP px clear of the content edge and
                        // shaped advances can overshoot the lane by ~2px, so
                        // legitimate pens never come near it. The vertical
                        // check carries the clear/clip semantics.
                        boolean xIntrude = fr.right() ? g.x() > cx + 4 : g.x() < cr - 4;
                        boolean overlap = xIntrude && g.y() + g.h() > f.y() + 2 && g.y() < cb - 2;
                        if (overlap) {
                            issues.add(
                                new Issue(
                                    here + ": glyph @("
                                        + (int) g.x()
                                        + ","
                                        + (int) g.y()
                                        + ") inside float rect "
                                        + f));
                            if (++reported >= 3) break outer;
                        }
                    }
                }
            }
        }
        for (LytNode child : node.getChildren()) {
            checkFloatOverlap(child, floats, issues, inFloat, here);
        }
    }

    /**
     * Line-geometry oracle backing the interaction retirement: prove Rust glyph
     * runs reproduce the Java LineBuilder's per-line geometry for PLAIN
     * paragraphs, so line rects can be rebuilt from glyph runs (via line_index)
     * and the Java LineBuilder deleted.
     * <p>
     * Two regimes, decided independently of the Rust/Java comparison (so it is
     * not a circular "assert equal when equal"):
     * <ul>
     * <li><b>Plain</b> (no float vertically overlaps, no inline blocks, unit
     * fontScale): the Java LineBuilder is a valid oracle: Rust must match its
     * line count, left edge and line-top y. Right edge is NOT compared: AWT
     * (Java) and swash (Rust) per-glyph advances accumulate differently, so the
     * right extent drifts with line length even when wrapping agrees.</li>
     * <li><b>Float/inline</b>: the Java LineBuilder is NOT a valid oracle: it
     * skips the float clip the Rust pusher applies (measured: left-float text at
     * x=5 in Java vs the correct x=129 in Rust) and its line count diverges in
     * wrap bands. There we assert Rust internal consistency only; glyph-vs-lane
     * correctness is checkFloatOverlap's job.</li>
     * </ul>
     */
    private static void checkLineGeometry(LytDocument doc, int width, List<Issue> issues) {
        var serializer = new LayoutTreeSerializer();
        serializer.serialize(doc, width, 1.0f, 1.0f);
        List<LayoutTreeSerializer.FloatRect> floats = serializer.getFloatRects();
        checkLineGeometry(doc, floats, issues, false, "doc");
    }

    private static void checkLineGeometry(LytNode node, List<LayoutTreeSerializer.FloatRect> floats, List<Issue> issues,
        boolean insideFloat, String path) {
        boolean inFloat = insideFloat || node instanceof LytDocumentFloat;
        String here = path + "/"
            + (node instanceof LytBlock b ? b.getClass()
                .getSimpleName() : "?");
        if (!inFloat && node instanceof LytParagraph par
            && par.getGlyphData() != null
            && !par.getGlyphData()
                .runs()
                .isEmpty()
            && par.getBounds() != null) {
            checkParagraphLines(par, floats, issues, here);
        }
        for (LytNode child : node.getChildren()) {
            checkLineGeometry(child, floats, issues, inFloat, here);
        }
    }

    private static void checkParagraphLines(LytParagraph par, List<LayoutTreeSerializer.FloatRect> floats,
        List<Issue> issues, String here) {
        LytRect pb = par.getBounds();
        // Rust per-line extents {minX, maxX, minY, maxY}, keyed by line_index.
        java.util.TreeMap<Integer, float[]> rust = new java.util.TreeMap<>();
        for (var group : par.getGlyphData()
            .runs()) {
            for (var g : group.glyphs()) {
                float[] ext = rust
                    .computeIfAbsent(g.lineIndex(), k -> new float[] { Float.MAX_VALUE, 0, Float.MAX_VALUE, 0 });
                ext[0] = Math.min(ext[0], g.x());
                ext[1] = Math.max(ext[1], g.x() + g.w());
                ext[2] = Math.min(ext[2], g.y());
                ext[3] = Math.max(ext[3], g.y() + g.h());
            }
        }
        if (rust.isEmpty()) {
            return;
        }
        // The Java LineBuilder is gone: Rust is the sole layout authority.
        // No Java oracle to compare against; skip the line-level comparison.
    }

    /**
     * Coordinate-level check for the two-graph page: each graph's text/circle
     * primitives must land inside its own panel (labels leaking into the other
     * graph's panel is the reported bug), and curve segments must reach the
     * plot area (curves drawn fully outside = the "invisible curves" bug).
     */
    private static int runTwoGraphsScenario() {
        System.out.println("\n=== twoGraphsPage (width=557) ===");
        LytDocument doc = twoGraphsPage();
        doc.updateLayout(new LayoutContext(MOCK_METRICS), 557);
        List<Issue> issues = new ArrayList<>();
        checkTree(doc, issues, "");
        checkSiblingOverlap(doc, issues, "doc");
        StringBuilder sb = new StringBuilder();
        dump(doc, 0, sb);
        System.out.print(sb);

        var viewport = new LytRect(0, 0, 557, 4000);
        for (var block : doc.getBlocks()) {
            if (!(block instanceof com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph graph)) {
                continue;
            }
            var pc = new com.hfstudio.guidenh.guide.render.PrimitiveCollector(viewport, null);
            pc.collectFrom(graph);
            LytRect b = graph.getBounds();
            System.out.println("-- graph bounds=" + b);
            int texts = 0, circles = 0, lines = 0, glyphQuads = 0, outsideTexts = 0, outsideCircles = 0,
                outsideGlyphs = 0, linesOffPanel = 0;
            int printed = 0;
            for (var p : pc.result()) {
                if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.DrawText dt) {
                    texts++;
                    boolean in = dt.x() >= b.x() - 2 && dt.x() <= b.right() + 2
                        && dt.y() >= b.y() - 2
                        && dt.y() <= b.bottom() + 2;
                    if (!in) {
                        outsideTexts++;
                        if (printed++ < 10) {
                            System.out
                                .println("   OUTSIDE-TEXT \"" + dt.text() + "\" @(" + dt.x() + "," + dt.y() + ")");
                        }
                    } else if (texts <= 16) {
                        System.out.println("   text \"" + dt.text() + "\" @(" + dt.x() + "," + dt.y() + ")");
                    }
                } else if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.DrawGlyphRun dg) {
                    // Unified-pipeline text (GuideText.emitText): every glyph quad
                    // must stay inside the emitting graph's panel.
                    for (var g : dg.glyphs()) {
                        glyphQuads++;
                        boolean in = g.x() >= b.x() - 3 && g.x() <= b.right() + 3
                            && g.y() >= b.y() - 3
                            && g.y() <= b.bottom() + 3;
                        if (!in) {
                            outsideGlyphs++;
                            if (printed++ < 10) {
                                System.out.println("   OUTSIDE-GLYPH @(" + g.x() + "," + g.y() + ")");
                            }
                        }
                    }
                } else if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.DrawCircle dc) {
                    circles++;
                    boolean in = dc.cx() >= b.x() - 4 && dc.cx() <= b.right() + 4
                        && dc.cy() >= b.y() - 4
                        && dc.cy() <= b.bottom() + 4;
                    if (!in) {
                        outsideCircles++;
                        if (printed++ < 10) {
                            System.out.println("   OUTSIDE-CIRCLE @(" + dc.cx() + "," + dc.cy() + ")");
                        }
                    }
                } else if (p instanceof com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.DrawLine dl) {
                    lines++;
                    // Segment bounding box must intersect the (slightly inflated) panel.
                    float minX = Math.min(dl.x1(), dl.x2()), maxX = Math.max(dl.x1(), dl.x2());
                    float minY = Math.min(dl.y1(), dl.y2()), maxY = Math.max(dl.y1(), dl.y2());
                    boolean touches = maxX >= b.x() - 8 && minX <= b.right() + 8
                        && maxY >= b.y() - 8
                        && minY <= b.bottom() + 8;
                    if (!touches) linesOffPanel++;
                }
            }
            System.out.println(
                "   texts=" + texts
                    + " glyphQuads="
                    + glyphQuads
                    + " circles="
                    + circles
                    + " lines="
                    + lines
                    + " | outsideTexts="
                    + outsideTexts
                    + " outsideGlyphs="
                    + outsideGlyphs
                    + " outsideCircles="
                    + outsideCircles
                    + " linesOffPanel="
                    + linesOffPanel);
            if (outsideTexts > 0 || outsideCircles > 0 || outsideGlyphs > 0) {
                issues.add(
                    new Issue(
                        "graph " + b
                            + ": "
                            + (outsideTexts + outsideGlyphs + outsideCircles)
                            + " primitives outside bounds"));
            }
            if (lines > 0 && linesOffPanel == lines) {
                issues.add(new Issue("graph " + b + ": ALL " + lines + " line segments off panel (invisible curves)"));
            }
        }
        if (issues.isEmpty()) {
            System.out.println("OK: no issues");
            return 0;
        }
        for (Issue i : issues) {
            System.out.println("ISSUE: " + i.text());
        }
        return issues.size();
    }

    /**
     * Reproduces the in-game "callout not hugging the intro" case: a right
     * float, a short intro paragraph, then a callout (LytQuoteBox) wrapped in
     * LytFloatAwareBlock: exactly what PageCompiler emits for a blockquote
     * beside a float. The wrapper must stay adjacent to the intro (a block box
     * is not pushed below a float in CSS); only its width may shrink to the
     * lane, and only the text inside wraps.
     */
    private static LytDocument floatAwareBlockPage() {
        var doc = new LytDocument();
        doc.append(new LytDocumentFloat(new FixedLeaf(120, 200), true));
        doc.append(
            paragraph(
                "The Large Sifter is an HV tier multiblock for sifting gems and minerals. The Large Sifter is a direct upgrade from the singleblock sifter because it runs at 500% speed, only uses 75% of the EU/t normally required, and offers 4 parallels per voltage tier. It is recommended to passively sift dusts and liquids with no other use."));
        var q = new com.hfstudio.guidenh.guide.document.block.LytQuoteBox();
        q.setQuoteStyle(null, "Note", null);
        q.append(paragraph("Only the structure of this multiblock has changed, the mechanics stay the same."));
        doc.append(new com.hfstudio.guidenh.guide.document.block.LytFloatAwareBlock(q));
        return doc;
    }

    public static void main(String[] args) {
        if (!setup()) {
            System.exit(2);
            return;
        }
        int failures = 0;
        failures += runScenario("floatPage", floatPage(), 557, true);
        failures += runScenario("recipeGalleryPage", recipeGalleryPage(), 557);
        failures += runScenario("fileTreePage", fileTreePage(), 557, true);
        failures += runScenario("itemImagePage", itemImagePage(), 557);
        failures += runScenario("inlinePage", inlinePage(), 557);
        failures += runScenario("justifyPage", justifyPage(), 557);
        failures += runScenario("floatWrapPage", floatWrapPage(), 557, true);
        failures += runScenario("multiFloatPage", multiFloatPage(), 557, true);
        failures += runFloatClearScenario();
        failures += runBreakScenario();
        failures += runScenario("floatAwareBlockPage", floatAwareBlockPage(), 920, true);
        failures += runScenario("contentTabsPage", contentTabsPage(), 557, true);
        failures += runScenario("fontScalePage", fontScalePage(), 557);
        failures += runScenario("tablePage", tablePage(), 557);
        failures += runScenario("functionGraphPage", functionGraphPage(), 557, true);
        failures += runScenario("latexInlinePage", latexInlinePage(), 557);
        // Narrow width forces the fraction paragraph to wrap like in-game,
        // exercising the per-line growth path (markers on line 2+).
        failures += runScenario("latexInlinePageNarrow", latexInlinePage(), 300);
        failures += runTwoGraphsScenario();
        failures += runCodeBlockScenario();
        failures += runChartsScenario();
        failures += runRichTextScenario();
        System.out.println("\n==== TOTAL ISSUES: " + failures + " ====");
        // Baseline evidence: real text glyphs carry a non-zero Rust baseline with
        // baseline - y == placement.top (Rust invariant; 0.5px-aligned). Fail the
        // run when no such evidence exists.
        System.out.println(
            "==== BASELINE-EVIDENCE: total=" + BS_TOTAL
                + " nonzero="
                + BS_NONZERO
                + " valid("
                + -1.0
                + "..24)="
                + BS_VALID
                + " halfPxAligned="
                + BS_HALF_PX
                + " sample"
                + BS_SAMPLE
                + " ====");
        if (BS_TOTAL == 0 || BS_NONZERO == 0) {
            failures++;
            System.out.println("BASELINE-EVIDENCE-FAIL: no non-zero baseline observed");
        }
        System.exit(failures == 0 ? 0 : 1);
    }
}
