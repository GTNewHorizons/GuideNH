package com.hfstudio.guidenh.guide.layout;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytImageBlock;
import com.hfstudio.guidenh.guide.document.block.LytLatexBlock;
import com.hfstudio.guidenh.guide.document.block.LytLatexDisplayBlock;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.document.block.LytStructureView;
import com.hfstudio.guidenh.guide.document.block.LytThematicBreak;
import com.hfstudio.guidenh.guide.document.block.chart.LytBarChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytChartBase;
import com.hfstudio.guidenh.guide.document.block.chart.LytColumnChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytLineChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytPieChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytScatterChart;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.flow.LytFlowBreak;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.internal.recipe.LytNeiRecipeBox;
import com.hfstudio.guidenh.guide.layout.flatbuffers.ChartData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FlatNode;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FunctionGraphData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.GuidebookSceneData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.ImageData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LatexDisplayData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.ListMarkerData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.MediaWikiGeneratedListData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.MediaWikiSpecialGeneratedData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.PieChartData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.RecipeBoxData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.SlotData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.StructureViewData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextSpan;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextStyle;
import com.hfstudio.guidenh.guide.layout.flatbuffers.ThematicBreakData;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiGeneratedListBlock;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialGeneratedBlock;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Determines node_type and builds FlatNode with the appropriate sub-data table.
 */
public final class LayoutNodeSerializer {

    private LayoutNodeSerializer() {}

    /**
     * One inline block paired with its paragraph's U+FFFC placeholder: flat index
     * plus the vertical alignment request consumed by the Rust inline post-pass
     * (see InlineBlockRef in the schema: 0=bottom 2px below baseline, 1=baseline
     * ascent, 2=center on line plus offset, retired in favor of anchoring icon
     * atoms to the bottom edge of their ink box) and the atom ink box (floats in
     * the 16-unit icon space, matching IconMetrics; all zero means undeclared,
     * which keeps the legacy behavior).
     */
    public record InlineRef(int flatIndex, int align, float param, int inkLeft, int inkRight, int inkBottom) {

        /** No ink box (legacy constructor; all ink values zero means undeclared). */
        public InlineRef(int flatIndex, int align, float param) {
            this(flatIndex, align, param, 0, 0, 0);
        }
    }

    /** Extract the full text of a paragraph (with U+FFFC placeholders for inline blocks). */
    static String paragraphText(LytParagraph par) {
        StringBuilder sb = new StringBuilder();
        for (LytFlowContent fc : par.getContent()) {
            extractFlowText(fc, sb);
        }
        return sb.toString();
    }

    public static int build(FlatBufferBuilder fbb, LytBlock block, int styleOff, List<Integer> childIndices,
        List<InlineRef> inlineRefs) {
        byte nodeType = resolveNodeType(block);
        int textOff = nodeType == 1 ? buildTextData(fbb, block, inlineRefs) : 0;
        int imageOff = nodeType == 2 ? buildImageData(fbb, block) : 0;
        int slotOff = nodeType == 3 ? buildSlotData(fbb, block) : 0;
        int breakOff = nodeType == 4 ? buildThematicBreakData(fbb) : 0;
        int latexOff = nodeType == 8 ? buildLatexData(fbb, block) : 0;
        int recipeBoxOff = nodeType == 20 ? buildRecipeBoxData(fbb, block) : 0;
        int pieChartOff = nodeType == 21 ? buildPieChartData(fbb, block) : 0;
        int chartDataOff = (nodeType == 22 || nodeType == 23 || nodeType == 24 || nodeType == 25)
            ? buildChartData(fbb, block)
            : 0;
        int structureViewDataOff = nodeType == 26 ? buildStructureViewData(fbb, block) : 0;
        int guidebookSceneDataOff = nodeType == 27 ? buildGuidebookSceneData(fbb, block) : 0;
        int functionGraphDataOff = nodeType == 28 ? buildFunctionGraphData(fbb, block) : 0;
        int mediawikiGeneratedListDataOff = nodeType == 29 ? buildMediaWikiGeneratedListData(fbb, block) : 0;
        int mediawikiSpecialGeneratedDataOff = nodeType == 30 ? buildMediaWikiSpecialGeneratedData(fbb, block) : 0;
        byte customLayout = 0;
        // List-item shared gutter marker declaration: Java declares it, Rust shapes
        // and positions it, and Java has no dedicated rendering path. Non-list items
        // and subclasses that own their marker (task checkboxes and similar) write
        // nothing here (0 = no declaration).
        int listMarkerOff = buildListMarkerData(fbb, block);

        int childrenVec = buildChildrenVector(fbb, childIndices);
        return FlatNode.createFlatNode(
            fbb,
            styleOff,
            nodeType,
            textOff,
            imageOff,
            slotOff,
            breakOff,
            0,
            latexOff,
            customLayout,
            childrenVec,
            recipeBoxOff,
            pieChartOff,
            chartDataOff,
            structureViewDataOff,
            guidebookSceneDataOff,
            functionGraphDataOff,
            mediawikiGeneratedListDataOff,
            mediawikiSpecialGeneratedDataOff,
            listMarkerOff);
    }

    static byte resolveNodeType(LytBlock block) {
        if (block instanceof MediaWikiSpecialGeneratedBlock) return 30;
        if (block instanceof MediaWikiGeneratedListBlock) return 29;
        if (block instanceof LytFunctionGraph) return 28;
        if (block instanceof LytGuidebookScene) return 27;
        if (block instanceof LytStructureView) return 26;
        if (block instanceof LytPieChart) return 21;
        if (block instanceof LytBarChart) return 22;
        if (block instanceof LytColumnChart) return 23;
        if (block instanceof LytLineChart) return 24;
        if (block instanceof LytScatterChart) return 25;
        if (block instanceof LytNeiRecipeBox) return 20;
        if (block instanceof LytThematicBreak) return 4;
        if (block instanceof LytImage || block instanceof LytImageBlock) return 2;
        if (block instanceof LytSlot) return 3;
        if (block instanceof LytLatexBlock || block instanceof LytLatexDisplayBlock) return 8;
        if (block instanceof LytTable) return 7;
        if (block instanceof LytParagraph par && isOpaqueText(par)) return 0; // opaque leaf: no glyph run
        if (block instanceof LytParagraph) return 1; // Text: contains LytFlowText children
        return 0; // Container
    }

    /**
     * Paragraphs whose text cannot be represented by the glyph-run path are
     * lowered to opaque Container leaves: Rust reserves their box from the
     * Java-computed bounds, and rendering falls back to the legacy path
     * (HostDraw) automatically since no glyph run is produced.
     * <p>
     * All paragraphs use Rust text shaping for layout; float-aligned inline
     * blocks are serialized with align=3/4 and positioned inline by the Rust
     * post-pass (paragraph-level float support deferred to a later step).
     */
    private static boolean isOpaqueText(LytParagraph par) {
        return false;
    }

    /**
     * List-item shared gutter marker declaration: Java declares it, Rust shapes the
     * marker string with the same shaping pipeline used for body text, right-aligns
     * it against the list item's own left edge (the document text line of that
     * nesting level), and emits the glyphs as ordinary glyph quads into the page
     * output stream. Java has no dedicated rendering path for it; the old DrawText
     * emit, measureWidth positioning, and bullet-square drawing were removed from
     * LytListItem.
     *
     * <p>
     * Only {@link LytListItem} instances that use the shared marker write this
     * field (ordered "N." including the start semantics, unordered "•", see
     * {@link LytListItem#getSharedMarkerText()}). Subclasses with their own gutter
     * marker (task checkboxes and similar, hasOwnMarker=true) return null and
     * declare nothing, because a custom marker uses its own drawing path and the
     * shared slot must not declare it twice. The style is body regular
     * (BASE_FONT_SIZE) in the {@link LytListItem#COL_MARKER} gray.
     *
     * <p>
     * Ordered list numbering timing fix: ordered numbers are computed on the spot
     * while serializing. {@link LytListItem#getSharedMarkerText()} scans the sibling
     * items of the parent {@link LytList} to derive the number (including the start
     * semantics). serialize (this class) runs before
     * {@code afterExternalLayout()} (LytDocument.createLayout), so any number cached
     * during layout is unavailable on the first serialization pass. This is O(N) and
     * runs once per layout pass, which is acceptable.
     */
    private static int buildListMarkerData(FlatBufferBuilder fbb, LytBlock block) {
        if (!(block instanceof LytListItem item)) {
            return 0;
        }
        String markerText = item.getSharedMarkerText();
        if (markerText == null) {
            return 0;
        }
        int textOff = fbb.createString(markerText);
        long markerArgb = LytListItem.COL_MARKER.resolve() & 0xFFFFFFFFL;
        // Real font_family: the marker declares the body font family. The resolved
        // style font wins; a null font falls back to
        // GuideText.resolveBaseFontFamily(), whose default "system" maps to null, so
        // nothing is written and the wire stays byte-identical to the state before
        // this field was activated. The pipeline only needs to carry it.
        var markerStyle = item.resolveStyle();
        String markerFamily = markerStyle != null && markerStyle.font() != null ? markerStyle.font()
            : GuideText.resolveBaseFontFamily();
        int markerFamilyOff = markerFamily != null ? fbb.createString(markerFamily) : 0;
        int styleOff = TextStyle.createTextStyle(
            fbb,
            GuideText.BASE_FONT_SIZE,
            false, // bold: the body font is regular
            false, // italic
            1f, // fontScale
            markerArgb,
            markerFamilyOff,
            false, // underlined
            false, // strikethrough
            0L, // highlightArgb
            false, // inlineCode
            0.0f, // baselineShift
            false, // wavyUnderline
            false); // dottedUnderline
        return ListMarkerData.createListMarkerData(fbb, textOff, styleOff);
    }

    private static int buildTextData(FlatBufferBuilder fbb, LytBlock block, List<InlineRef> inlineRefs) {
        String text = "";
        // Base em size mirrors GuideText.resolveBaseFontSize() (= GuideText.BASE_FONT_SIZE
        // = 11 when readerFontSize = 0); line height is size × 1.55
        // (round(17.05) = GuideText.BASE_LINE_HEIGHT = 17 at scale 1) across every Rust
        // path (parley_text.rs shape_paragraph, layout.rs emitText, measure.rs
        // empty-paragraph fallback). fontScale (headings etc.) scales both
        // proportionally: Rust computes scaled = fontSize × fontScale.
        float fontSize = GuideText.resolveBaseFontSize();
        boolean bold = false;
        boolean italic = false;
        float fontScale = 1f;
        long baseColor = 0xFFFFFFFFL;
        // Real font_family of the paragraph base run: the resolved style font wins,
        // a null font falls back to GuideText.resolveBaseFontFamily() (its default
        // "system" maps to null, so nothing is written and the wire stays
        // byte-identical to the state before this field was activated). Span-level
        // families are handled separately by buildFbTextStyle.
        String baseFamily = null;
        byte wsByte = 0;
        List<SpanPart> spanParts = new ArrayList<>();
        // In-paragraph hard breaks (<br>): raw byte offsets in the break-free
        // text at which the Rust pusher splits the paragraph into independently
        // shaped pieces (a hard break, not a whitespace char).
        List<Integer> breaks = new ArrayList<>();
        // In-paragraph clear breaks with their raw (original-text) UTF-8 byte
        // offset; collected in document order alongside the text extraction.
        List<int[]> clears = new ArrayList<>();

        if (block instanceof LytParagraph par) {
            // Collect all text recursively: text can be nested inside LytFlowSpan wrappers
            // (e.g. bold/italic spans). The simple instanceof LytFlowText filter misses spans.
            StringBuilder sb = new StringBuilder();
            for (LytFlowContent fc : par.getContent()) {
                extractFlowText(fc, sb);
            }
            text = sb.toString();

            // DIAG: log extracted text (first 80 chars) to confirm recursive extraction works
            if (text.length() > 80) {
                GuideDebugLog
                    .warnAlways("Layout: para text(len={}) preview=\"{}\"", text.length(), text.substring(0, 80));
            } else if (!text.isEmpty()) {
                GuideDebugLog.warnAlways("Layout: para text(len={}) preview=\"{}\"", text.length(), text);
            }

            // Resolve resolved style from the paragraph (uses its own style + parent styles)
            var resolved = par.resolveStyle();
            if (resolved != null) {
                // The ResolvedTextStyle doesn't carry fontSize directly;
                // default to paragraph's font data or whatever is available.
                // The Rust side uses font_size * font_scale * 1.45 for the
                // empty-paragraph line-height fallback (see measure.rs).
                bold = resolved.bold();
                italic = resolved.italic();
                if (resolved.fontScale() != 1f) {
                    fontScale = resolved.fontScale();
                }
                // Base run tint (single-style runs fall back to this color).
                baseColor = GuideText.resolveColor(resolved) & 0xFFFFFFFFL;
                // Base run family: the style font wins, and a null font falls back to
                // the reader config (resolveBaseFontFamily; its default "system" maps
                // to null, so the wire stays neutral).
                baseFamily = resolved.font() != null ? resolved.font() : GuideText.resolveBaseFontFamily();
                var resolvedWs = resolved.whiteSpace();
                wsByte = resolvedWs == com.hfstudio.guidenh.guide.style.WhiteSpaceMode.PRE_WRAP ? (byte) 1
                    : resolvedWs == com.hfstudio.guidenh.guide.style.WhiteSpaceMode.PRE ? (byte) 2 : (byte) 0;
            }

            // Rich spans: per-leaf resolved styles in document order (concatenated
            // span texts equal the extracted text, U+FFFC placeholders included).
            for (LytFlowContent fc : par.getContent()) {
                collectSpanParts(fc, resolved, spanParts);
            }
            // Single-style override: if all content resolves to one distinct style
            // that differs from the paragraph's own resolved style, the base run
            // style must come from that single span style rather than the paragraph
            // style, so that the single span's appearance is preserved without
            // needing rich spans (needsRichSpans requires >=2 distinct styles).
            if (spanParts.size() == 1 && resolved != null && !spanParts.get(0).style.equals(resolved)) {
                var partStyle = spanParts.get(0).style;
                bold = partStyle.bold();
                italic = partStyle.italic();
                if (partStyle.fontScale() != 1f) {
                    fontScale = partStyle.fontScale();
                }
                baseColor = GuideText.resolveColor(partStyle) & 0xFFFFFFFFL;
                // The single-style override takes the same reader config fallback.
                baseFamily = partStyle.font() != null ? partStyle.font() : GuideText.resolveBaseFontFamily();
                var partWs = partStyle.whiteSpace();
                wsByte = partWs == com.hfstudio.guidenh.guide.style.WhiteSpaceMode.PRE_WRAP ? (byte) 1
                    : partWs == com.hfstudio.guidenh.guide.style.WhiteSpaceMode.PRE ? (byte) 2 : (byte) 0;
            }
            // Clear breaks: record each <br clear> at its raw byte offset so the
            // Rust pusher can drop the following lines below the cleared floats.
            int[] clearOff = new int[] { 0 };
            for (LytFlowContent fc : par.getContent()) {
                walkClears(fc, clearOff, clears);
            }
            // Hard breaks: record every <br> (including clear ones) so the Rust
            // pusher splits the paragraph into independently shaped pieces.
            int[] breakOff = new int[] { 0 };
            for (LytFlowContent fc : par.getContent()) {
                walkBreaks(fc, breakOff, breaks);
            }
        }

        int strOff = fbb.createString(text);
        // Wire base family = baseFamily (the resolved or part style font wins,
        // otherwise resolveBaseFontFamily; its default "system" maps to null, so
        // familyOff stays 0 and the wire is byte-neutral).
        int baseFamilyOff = baseFamily != null ? fbb.createString(baseFamily) : 0;
        int styleOff = TextStyle.createTextStyle(
            fbb,
            fontSize,
            bold,
            italic,
            fontScale,
            baseColor,
            baseFamilyOff,
            false,
            false,
            0L,
            false,
            0.0f,
            false,
            false);
        int inlineBlocksVec = 0;
        if (!inlineRefs.isEmpty()) {
            int[] refs = new int[inlineRefs.size()];
            for (int i = 0; i < refs.length; i++) {
                var r = inlineRefs.get(i);
                // Pass the atom ink box through unchanged: the IconMetrics
                // inkLeft/inkRight/inkBottom values (floats in the 16-unit icon
                // space) are written to the wire, and the Rust inline post-pass and
                // measurement layer consume them for advance/kerning/baseline. All
                // zero means undeclared (legacy data, or an icon whose metrics cannot
                // be measured falls back to the cell width). The units match
                // IconMetrics (16-unit icon space); Rust converts them to px as
                // declared cell height * ink/16.
                refs[i] = com.hfstudio.guidenh.guide.layout.flatbuffers.InlineBlockRef.createInlineBlockRef(
                    fbb,
                    r.flatIndex(),
                    (byte) r.align(),
                    r.param(),
                    r.inkLeft(),
                    r.inkRight(),
                    r.inkBottom());
            }
            inlineBlocksVec = TextData.createInlineBlocksVector(fbb, refs);
        }
        int spansVec = 0;
        if (needsRichSpans(spanParts)) {
            int[] spans = new int[spanParts.size()];
            for (int i = 0; i < spans.length; i++) {
                SpanPart part = spanParts.get(i);
                int spanTextOff = fbb.createString(part.text);
                int spanStyleOff = buildFbTextStyle(fbb, fontSize, part.style);
                spans[i] = TextSpan.createTextSpan(fbb, spanTextOff, spanStyleOff);
            }
            spansVec = TextData.createSpansVector(fbb, spans);
        }
        int clearsVec = 0;
        if (!clears.isEmpty()) {
            int[] cs = new int[clears.size()];
            for (int i = 0; i < cs.length; i++) {
                int[] c = clears.get(i);
                cs[i] = com.hfstudio.guidenh.guide.layout.flatbuffers.ClearBreak
                    .createClearBreak(fbb, c[0], (byte) c[1]);
            }
            clearsVec = TextData.createClearsVector(fbb, cs);
        }
        int breaksVec = 0;
        if (!breaks.isEmpty()) {
            long[] bs = new long[breaks.size()];
            for (int i = 0; i < bs.length; i++) {
                bs[i] = breaks.get(i) & 0xFFFFFFFFL;
            }
            breaksVec = TextData.createBreaksVector(fbb, bs);
        }
        boolean separator = (block instanceof LytHeading heading)
            && (heading.getDepth() == 1 || heading.getDepth() == 2);
        // Read the per-paragraph text alignment from the resolved style
        byte alignmentByte = 0; // default: Start(Left)
        if (block instanceof LytParagraph par) {
            var resolved = par.resolveStyle();
            if (resolved != null && resolved.alignment() != null) {
                alignmentByte = switch (resolved.alignment()) {
                    case CENTER -> 1;
                    case RIGHT -> 2;
                    default -> 0; // LEFT
                };
            }
        }
        // bands / float_clips are DEPRECATED slots (marked in the fbs schema, never
        // consumed by Rust) and are no longer written. An omitted slot falls back to
        // its default (a null vector), so the wire carries no bytes for them.
        TextData.startTextData(fbb);
        TextData.addBreaks(fbb, breaksVec);
        TextData.addClears(fbb, clearsVec);
        TextData.addSpans(fbb, spansVec);
        TextData.addInlineBlocks(fbb, inlineBlocksVec);
        TextData.addStyle(fbb, styleOff);
        TextData.addText(fbb, strOff);
        TextData.addAlignment(fbb, alignmentByte);
        TextData.addSeparator(fbb, separator);
        TextData.addWhiteSpace(fbb, wsByte);
        return TextData.endTextData(fbb);
    }

    /** One text run with its resolved style, in document order. */
    private static final class SpanPart {

        String text;
        final ResolvedTextStyle style;

        SpanPart(String text, ResolvedTextStyle style) {
            this.text = text;
            this.style = style;
        }
    }

    /**
     * Split a flow content tree into styled span parts (document order), merging
     * adjacent parts that share the same resolved style. Inline blocks become
     * U+FFFC placeholder parts in the paragraph's own style. Mirrors
     * {@link #extractFlowText} so concatenated part texts equal the full text.
     */
    private static void collectSpanParts(LytFlowContent fc, ResolvedTextStyle paragraphStyle, List<SpanPart> out) {
        if (fc instanceof LytFlowText ft) {
            appendSpanPart(out, ft.getText(), fc.resolveStyle());
        } else if (fc instanceof LytFlowInlineBlock) {
            appendSpanPart(out, "￼", paragraphStyle);
        } else if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                collectSpanParts(child, paragraphStyle, out);
            }
        }
        // Other flow types (LytFlowBreak, etc.) contribute no text
    }

    private static void appendSpanPart(List<SpanPart> out, String text, ResolvedTextStyle style) {
        if (text.isEmpty()) {
            return;
        }
        if (!out.isEmpty() && out.get(out.size() - 1).style.equals(style)) {
            out.get(out.size() - 1).text += text;
        } else {
            out.add(new SpanPart(text, style));
        }
    }

    /**
     * Whether the paragraph needs rich spans (TextData.spans): multiple distinct
     * resolved styles, or any decoration the single-style run cannot express.
     * Single-style paragraphs keep the legacy text+style fields (spans empty).
     */
    private static boolean needsRichSpans(List<SpanPart> parts) {
        Set<ResolvedTextStyle> distinct = new HashSet<>();
        for (SpanPart part : parts) {
            distinct.add(part.style);
            ResolvedTextStyle s = part.style;
            if (s.underlined() || s.strikethrough()
                || s.wavyUnderline()
                || s.dottedUnderline()
                || s.backgroundColor() != null
                || s.inlineCode()) {
                return true;
            }
        }
        return distinct.size() >= 2;
    }

    /** Build a schema TextStyle for one span: color/decorations resolved now. */
    private static int buildFbTextStyle(FlatBufferBuilder fbb, float fontSize, ResolvedTextStyle style) {
        long argb = GuideText.resolveColor(style) & 0xFFFFFFFFL;
        boolean inlineCode = style.inlineCode();
        long highlight = 0L;
        if (inlineCode) {
            highlight = 0x1A6FB6FFL;
        } else if (style.backgroundColor() != null) {
            highlight = style.backgroundColor()
                .resolve() & 0xFFFFFFFFL;
        }
        // An inline_code span declares a monospace family through
        // GuideText.resolveCodeFontFamily() (the config default "mono" maps to
        // "monospace"; Rust resolve_family maps it to GenericFamily::Monospace, and
        // each fontique backend resolves the platform monospace default), which is
        // byte-identical to the legacy literal. A non-code span keeps its real
        // font_family (style.font() wins, otherwise resolveBaseFontFamily(), whose
        // default null writes null, keeping non-code pages byte-identical). This
        // shares one predicate with GuideText.fontFamilyOf, so the cache key, the
        // wire family, and the family actually used for shaping always agree.
        // FlatBuffers strings must be created before the table that references them.
        String spanFamily = style.inlineCode() ? GuideText.resolveCodeFontFamily()
            : (style.font() != null ? style.font() : GuideText.resolveBaseFontFamily());
        int familyOff = spanFamily != null ? fbb.createString(spanFamily) : 0;
        return TextStyle.createTextStyle(
            fbb,
            fontSize,
            style.bold(),
            style.italic(),
            style.fontScale(),
            argb,
            familyOff,
            style.underlined(),
            style.strikethrough(),
            highlight,
            inlineCode,
            style.baselineShift(),
            style.wavyUnderline(),
            style.dottedUnderline());
    }

    /**
     * Recursively extract all text from a flow content tree.
     * Handles {@link LytFlowText} (direct text) and {@link LytFlowSpan}
     * (wrapper with nested children).
     */
    /**
     * Walk flow content in document order, tracking the running raw (original
     * text, U+FFFC included) UTF-8 byte offset and recording every clear break
     * at its offset. Mirrors {@link #extractFlowText}'s traversal so the offset
     * matches the byte position in TextData.text: text contributes its UTF-8
     * length, an inline block contributes the 3 bytes of its U+FFFC placeholder,
     * a break contributes nothing (it is not in the text).
     */
    private static void walkClears(LytFlowContent fc, int[] offset, List<int[]> out) {
        if (fc instanceof LytFlowText ft) {
            offset[0] += ft.getText()
                .getBytes(StandardCharsets.UTF_8).length;
        } else if (fc instanceof LytFlowInlineBlock) {
            offset[0] += 3; // U+FFFC placeholder = 3 UTF-8 bytes
        } else if (fc instanceof LytFlowBreak fb) {
            boolean left = fb.isClearLeft();
            boolean right = fb.isClearRight();
            if (left || right) {
                byte side = (byte) (left && right ? 3 : left ? 1 : 2);
                out.add(new int[] { offset[0], side });
            }
        } else if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                walkClears(child, offset, out);
            }
        }
    }

    /**
     * Record every in-paragraph {@code <br>
     * } (hard break) at its raw byte offset
     * in the break-free text. Mirrors {@link #walkClears}' traversal and offset
     * accounting (a break contributes no bytes to the text), but records ALL
     * breaks, not only the clearing ones. The Rust pusher splits the paragraph
     * at these offsets and shapes each piece independently.
     */
    private static void walkBreaks(LytFlowContent fc, int[] offset, List<Integer> out) {
        if (fc instanceof LytFlowText ft) {
            offset[0] += ft.getText()
                .getBytes(StandardCharsets.UTF_8).length;
        } else if (fc instanceof LytFlowInlineBlock) {
            offset[0] += 3; // U+FFFC placeholder = 3 UTF-8 bytes
        } else if (fc instanceof LytFlowBreak fb) {
            // A clearing break is handled by the clear-floor path (single-shape
            // trailing clear); recording it here as a hard split too would put it
            // exactly on a segment boundary and lose it. Only non-clearing <br>
            // are hard splits.
            if (!fb.isClearLeft() && !fb.isClearRight()) {
                out.add(offset[0]); // the break itself is not in the text
            }
        } else if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                walkBreaks(child, offset, out);
            }
        }
    }

    private static void extractFlowText(LytFlowContent fc, StringBuilder out) {
        if (fc instanceof LytFlowText ft) {
            out.append(ft.getText());
        } else if (fc instanceof LytFlowInlineBlock) {
            // Inline block placeholders: one OBJECT REPLACEMENT CHARACTER per
            // inline block, in order. The Rust side replaces the placeholder's
            // advance with the block's real width (kerning) and anchors the
            // block at the placeholder's pen position.
            out.append('￼');
        } else if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                extractFlowText(child, out);
            }
        }
        // Other flow types (LytFlowBreak, etc.) contribute no text
    }

    private static int buildImageData(FlatBufferBuilder fbb, LytBlock block) {
        float naturalW = 0;
        float naturalH = 0;
        float scaleX = 1f;
        float scaleY = 1f;
        float explicitW = -1f;
        float explicitH = -1f;
        int cropX = 0;
        int cropY = 0;
        int cropW = -1;
        int cropH = -1;

        if (block instanceof LytImage img) {
            var tex = img.getTexture();
            if (tex != null && !tex.isMissing()) {
                var size = tex.getSize();
                naturalW = size.width();
                naturalH = size.height();
            }
            // Pass crop / scale / explicit fields so the Rust ImageData path
            // receives the same crop information as the Java Blit path.
            cropX = img.getCropX();
            cropY = img.getCropY();
            cropW = img.getCropWidth();
            cropH = img.getCropHeight();
            scaleX = (float) img.getScaleX();
            scaleY = (float) img.getScaleY();
            explicitW = img.getExplicitWidth();
            explicitH = img.getExplicitHeight();
        }
        if (block instanceof LytImageBlock imb) {
            // LytImageBlock extends LytParagraph (not LytImage) so the
            // LytImage branch above is never entered. Read explicit
            // dimensions, scale, and crop so Rust measure_image can
            // size the image correctly. Without this, ImageData retains
            // all defaults (naturalW=0, naturalH=0, explicitW=-1f) and
            // measure_image returns 1x1, so the block-level image collapses and the
            // parent container (e.g. LytListItem as column flex) does not stretch to
            // include the visual image height.
            explicitW = imb.getExplicitWidth();
            explicitH = imb.getExplicitHeight();
            scaleX = (float) imb.getScaleX();
            scaleY = (float) imb.getScaleY();
            cropX = imb.getCropX();
            cropY = imb.getCropY();
            cropW = imb.getCropWidth();
            cropH = imb.getCropHeight();
        }

        return ImageData
            .createImageData(fbb, naturalW, naturalH, cropX, cropY, cropW, cropH, scaleX, scaleY, explicitW, explicitH);
    }

    private static int buildSlotData(FlatBufferBuilder fbb, LytBlock block) {
        return SlotData.createSlotData(fbb, 18f);
    }

    private static int buildThematicBreakData(FlatBufferBuilder fbb) {
        return ThematicBreakData.createThematicBreakData(fbb, 6f);
    }

    private static int buildLatexData(FlatBufferBuilder fbb, LytBlock block) {
        String formula = "";
        int fillColorArgb = 0xFFFFFFFF;
        float userScale = 1f;
        int offsetX = 0;
        int offsetY = 0;
        float rawW = 0;
        float rawH = 0;

        if (block instanceof LytLatexBlock lb) {
            formula = lb.getFormula();
            fillColorArgb = lb.getFillColorArgb();
            userScale = lb.getUserScale();
            offsetX = lb.getOffsetX();
            offsetY = lb.getOffsetY();
            rawW = lb.getFormulaDisplayW();
            rawH = lb.getFormulaDisplayH();
        } else if (block instanceof LytLatexDisplayBlock ldb) {
            rawW = ldb.getFormulaDisplayW();
            rawH = ldb.getFormulaDisplayH();
        }

        int formulaOff = fbb.createString(formula);
        // source_scale / ref_h are DEPRECATED slots (marked in the fbs schema, never
        // consumed by Rust) and are no longer written. An omitted slot falls back to
        // its default (100.0 / 0.0), so the wire carries no bytes for them.
        LatexDisplayData.startLatexDisplayData(fbb);
        LatexDisplayData.addRawH(fbb, rawH);
        LatexDisplayData.addRawW(fbb, rawW);
        LatexDisplayData.addOffsetY(fbb, offsetY);
        LatexDisplayData.addOffsetX(fbb, offsetX);
        LatexDisplayData.addUserScale(fbb, userScale);
        LatexDisplayData.addFillColorArgb(fbb, fillColorArgb);
        LatexDisplayData.addFormula(fbb, formulaOff);
        return LatexDisplayData.endLatexDisplayData(fbb);
    }

    private static int buildRecipeBoxData(FlatBufferBuilder fbb, LytBlock block) {
        float bodyWidth = 0;
        float bodyHeight = 0;
        float bodyTopInset = 0;
        float bodyYShift = 0;
        float titleTextWidth = 0;
        float iconSize = 0;
        boolean recipeJumpEnabled = false;
        float titleHeight = 0;

        if (block instanceof LytNeiRecipeBox box) {
            bodyWidth = box.getBodyWidth();
            bodyHeight = box.getBodyHeight();
            bodyTopInset = box.getBodyTopInset();
            bodyYShift = box.getBodyYShift();
            titleTextWidth = box.getTitleTextWidth();
            iconSize = box.getIconSizeResult();
            recipeJumpEnabled = box.isRecipeJumpEnabled();
            titleHeight = box.getTitleHeight();
        }

        return RecipeBoxData.createRecipeBoxData(
            fbb,
            bodyWidth,
            bodyHeight,
            bodyTopInset,
            bodyYShift,
            titleTextWidth,
            iconSize,
            recipeJumpEnabled,
            titleHeight);
    }

    private static int buildPieChartData(FlatBufferBuilder fbb, LytBlock block) {
        float preferredWidth = 0;
        float totalHeight = 0;
        float titleChrome = 0;
        byte legendPosition = 0;
        float legendRowHeight = 10f;
        float[] legendLabelWidths = new float[0];

        if (block instanceof LytPieChart chart) {
            // preferredWidth: mirrors LytChartBase.preferredWidth()
            int ew = chart.getExplicitWidth();
            preferredWidth = (ew > 0 ? ew : LytChartBase.DEFAULT_WIDTH) + chart.getExtraPlotWidth();
            // totalHeight: mirrors LytChartBase.computeLayout: explicitHeight or DEFAULT_HEIGHT
            int eh = chart.getExplicitHeight();
            totalHeight = eh > 0 ? eh : LytChartBase.DEFAULT_HEIGHT;
            // New fields for Rust-side chrome computation (width-independent):
            titleChrome = chart.getTitleChromeForRust();
            legendPosition = chart.getLegendPositionForRust();
            legendRowHeight = chart.getLegendRowHeightForRust();
            legendLabelWidths = chart.getLegendLabelWidthsForRust();
        }

        int widthsVec = PieChartData.createLegendLabelWidthsVector(fbb, legendLabelWidths);
        // chrome_height is a DEPRECATED slot (marked in the fbs schema, never
        // consumed by Rust) and is no longer written. An omitted slot falls back to
        // its default 0.0, so the wire carries no bytes for it.
        PieChartData.startPieChartData(fbb);
        PieChartData.addLegendLabelWidths(fbb, widthsVec);
        PieChartData.addLegendRowHeight(fbb, legendRowHeight);
        PieChartData.addTitleChrome(fbb, titleChrome);
        PieChartData.addTotalHeight(fbb, totalHeight);
        PieChartData.addPreferredWidth(fbb, preferredWidth);
        PieChartData.addLegendPosition(fbb, legendPosition);
        return PieChartData.endPieChartData(fbb);
    }

    private static int buildChartData(FlatBufferBuilder fbb, LytBlock block) {
        float preferredWidth = 0;
        float totalHeight = 0;
        float titleChrome = 0;
        byte legendPosition = 0;
        float legendRowHeight = 10f;
        float[] legendLabelWidths = new float[0];

        if (block instanceof LytChartBase chart) {
            // preferredWidth: mirrors LytChartBase.preferredWidth()
            int ew = chart.getExplicitWidth();
            preferredWidth = (ew > 0 ? ew : LytChartBase.DEFAULT_WIDTH) + chart.getExtraPlotWidth();
            // totalHeight: mirrors LytChartBase.computeLayout: explicitHeight or DEFAULT_HEIGHT
            int eh = chart.getExplicitHeight();
            totalHeight = eh > 0 ? eh : LytChartBase.DEFAULT_HEIGHT;
            // New fields for Rust-side chrome computation (width-independent):
            titleChrome = chart.getTitleChromeForRust();
            legendPosition = chart.getLegendPositionForRust();
            legendRowHeight = chart.getLegendRowHeightForRust();
            legendLabelWidths = chart.getLegendLabelWidthsForRust();
        }

        int widthsVec = ChartData.createLegendLabelWidthsVector(fbb, legendLabelWidths);
        // chrome_height is a DEPRECATED slot (marked in the fbs schema, never
        // consumed by Rust) and is no longer written. An omitted slot falls back to
        // its default 0.0, so the wire carries no bytes for it.
        ChartData.startChartData(fbb);
        ChartData.addLegendLabelWidths(fbb, widthsVec);
        ChartData.addLegendRowHeight(fbb, legendRowHeight);
        ChartData.addTitleChrome(fbb, titleChrome);
        ChartData.addTotalHeight(fbb, totalHeight);
        ChartData.addPreferredWidth(fbb, preferredWidth);
        ChartData.addLegendPosition(fbb, legendPosition);
        return ChartData.endChartData(fbb);
    }

    private static int buildStructureViewData(FlatBufferBuilder fbb, LytBlock block) {
        float viewWidth = LytStructureView.DEFAULT_WIDTH;
        float viewHeight = LytStructureView.DEFAULT_HEIGHT;

        if (block instanceof LytStructureView sv) {
            viewWidth = sv.getViewWidth();
            viewHeight = sv.getViewHeight();
        }

        return StructureViewData.createStructureViewData(fbb, viewWidth, viewHeight);
    }

    private static int buildGuidebookSceneData(FlatBufferBuilder fbb, LytBlock block) {
        float sceneWidth = LytGuidebookScene.DEFAULT_WIDTH;
        float sceneHeight = LytGuidebookScene.DEFAULT_HEIGHT;
        float buttonColumnReserve = 0;
        float buttonsTotalHeight = 0;
        float leftDock = 0;
        float rightDock = 0;
        float topDock = 0;
        float bottomDock = 0;
        float bottomControlAreaHeight = 0;
        boolean reserveBottomControl = false;

        if (block instanceof LytGuidebookScene scene) {
            sceneWidth = scene.getSceneWidth();
            sceneHeight = scene.getSceneHeight();
            buttonColumnReserve = scene.getSceneButtonColumnReserveForExport();
            buttonsTotalHeight = scene.getButtonsTotalHeightForExport();
            leftDock = scene.getLeftDockForExport();
            rightDock = scene.getRightDockForExport();
            topDock = scene.getTopDockForExport();
            bottomDock = scene.getBottomDockForExport();
            bottomControlAreaHeight = scene.getBottomControlAreaHeight();
            reserveBottomControl = scene.isReserveBottomControlArea();
        }

        return GuidebookSceneData.createGuidebookSceneData(
            fbb,
            sceneWidth,
            sceneHeight,
            buttonColumnReserve,
            buttonsTotalHeight,
            leftDock,
            rightDock,
            topDock,
            bottomDock,
            bottomControlAreaHeight,
            reserveBottomControl);
    }

    private static int buildFunctionGraphData(FlatBufferBuilder fbb, LytBlock block) {
        float baseWidth = LytFunctionGraph.DEFAULT_WIDTH;
        float baseHeight = LytFunctionGraph.DEFAULT_HEIGHT;
        float titleChrome = 0;
        float legendRowHeight = 0;
        float[] labelItemWidths = new float[0];

        if (block instanceof LytFunctionGraph graph) {
            int ew = graph.getExplicitWidth();
            int eh = graph.getExplicitHeight();
            baseWidth = ew > 0 ? ew : LytFunctionGraph.DEFAULT_WIDTH;
            baseHeight = eh > 0 ? eh : LytFunctionGraph.DEFAULT_HEIGHT;

            // titleChrome = lineHeight(TITLE_STYLE) + TITLE_GAP, or 0 if no title
            String title = graph.getTitle();
            if (title != null && !title.isEmpty()) {
                int titleLineHeight = GuideText.lineHeight(LytFunctionGraph.getTitleStyle());
                titleChrome = titleLineHeight + LytFunctionGraph.getTitleGapConstant();
            }

            // Legend item widths and row height
            List<FunctionPlot> plots = graph.getPlots();
            int swatchSize = LytFunctionGraph.getLegendSwatchSize();
            int swatchTextGap = LytFunctionGraph.getLegendSwatchTextGap();
            legendRowHeight = Math.max(swatchSize, GuideText.lineHeight(LytFunctionGraph.getLegendLabelStyle()));
            labelItemWidths = new float[plots.size()];
            boolean hasLabel = false;
            for (int i = 0; i < plots.size(); i++) {
                FunctionPlot plot = plots.get(i);
                String label = plot.getLabel();
                if (label != null && !label.isEmpty()) {
                    int labelW = GuideText.measureWidth(label, LytFunctionGraph.getLegendLabelStyle());
                    labelItemWidths[i] = swatchSize + swatchTextGap + labelW;
                    hasLabel = true;
                }
            }
            if (!hasLabel) {
                legendRowHeight = 0; // suppress the legend entirely: there are no labels
            }
        }

        int labelWidthsVec = FunctionGraphData.createLabelItemWidthsVector(fbb, labelItemWidths);
        return FunctionGraphData
            .createFunctionGraphData(fbb, baseWidth, baseHeight, titleChrome, legendRowHeight, labelWidthsVec);
    }

    private static int buildMediaWikiGeneratedListData(FlatBufferBuilder fbb, LytBlock block) {
        float maxContentHeight = 0;

        if (block instanceof MediaWikiGeneratedListBlock mw) {
            maxContentHeight = mw.getMaxPrecomputedContentHeight();
        }

        return MediaWikiGeneratedListData.createMediaWikiGeneratedListData(fbb, maxContentHeight);
    }

    private static int buildMediaWikiSpecialGeneratedData(FlatBufferBuilder fbb, LytBlock block) {
        if (block instanceof MediaWikiSpecialGeneratedBlock mw) {
            // Collect font facts from the block (uses Minecraft font metrics,
            // width-independent). The serializer does NOT have a LayoutContext,
            // so fonts facts are collected via computeLayout context which is
            // already complete by the time serialization runs. We pass null
            // context here: font facts were already captured during layout.
            var facts = mw.getCollectedFontFacts();
            if (facts != null) {
                var impl = facts.impl();
                int groupTitleWidthsOff = impl.groupTitleWidths().length > 0
                    ? MediaWikiSpecialGeneratedData.createGroupTitleWidthsVector(fbb, impl.groupTitleWidths())
                    : 0;
                int groupEntryCountsOff = impl.groupEntryCounts().length > 0
                    ? MediaWikiSpecialGeneratedData.createGroupEntryCountsVector(fbb, impl.groupEntryCounts())
                    : 0;
                int groupEstimatedHeightsOff = impl.groupEstimatedHeights().length > 0
                    ? MediaWikiSpecialGeneratedData.createGroupEstimatedHeightsVector(fbb, impl.groupEstimatedHeights())
                    : 0;
                int entryTitleWidthsOff = impl.entryTitleWidths().length > 0
                    ? MediaWikiSpecialGeneratedData.createEntryTitleWidthsVector(fbb, impl.entryTitleWidths())
                    : 0;
                int entryHasIconOff = impl.entryHasIcon().length > 0
                    ? MediaWikiSpecialGeneratedData.createEntryHasIconVector(fbb, impl.entryHasIcon())
                    : 0;
                int entryEstimatedHeightsOff = impl.entryEstimatedHeights().length > 0
                    ? MediaWikiSpecialGeneratedData.createEntryEstimatedHeightsVector(fbb, impl.entryEstimatedHeights())
                    : 0;
                int entrySubtitleLineCountsOff = impl.entrySubtitleLineCounts().length > 0
                    ? MediaWikiSpecialGeneratedData
                        .createEntrySubtitleLineCountsVector(fbb, impl.entrySubtitleLineCounts())
                    : 0;
                int subtitleLineWordCountsOff = impl.subtitleLineWordCounts().length > 0 ? MediaWikiSpecialGeneratedData
                    .createSubtitleLineWordCountsVector(fbb, impl.subtitleLineWordCounts()) : 0;
                int subtitleWordWidthsOff = impl.subtitleWordWidths().length > 0
                    ? MediaWikiSpecialGeneratedData.createSubtitleWordWidthsVector(fbb, impl.subtitleWordWidths())
                    : 0;

                // max_content_height is a DEPRECATED slot (marked in the fbs schema,
                // never consumed by Rust) and is no longer written. An omitted slot
                // falls back to its default 0.0, so the wire carries no bytes for it.
                MediaWikiSpecialGeneratedData.startMediaWikiSpecialGeneratedData(fbb);
                MediaWikiSpecialGeneratedData.addSubtitleLineHeight(fbb, impl.subtitleLineHeight());
                MediaWikiSpecialGeneratedData.addLinkLineHeight(fbb, impl.linkLineHeight());
                MediaWikiSpecialGeneratedData.addSubtitleSpaceWidth(fbb, impl.subtitleSpaceWidth());
                MediaWikiSpecialGeneratedData.addSubtitleWordWidths(fbb, subtitleWordWidthsOff);
                MediaWikiSpecialGeneratedData.addSubtitleLineWordCounts(fbb, subtitleLineWordCountsOff);
                MediaWikiSpecialGeneratedData.addEntrySubtitleLineCounts(fbb, entrySubtitleLineCountsOff);
                MediaWikiSpecialGeneratedData.addEntryEstimatedHeights(fbb, entryEstimatedHeightsOff);
                MediaWikiSpecialGeneratedData.addEntryHasIcon(fbb, entryHasIconOff);
                MediaWikiSpecialGeneratedData.addEntryTitleWidths(fbb, entryTitleWidthsOff);
                MediaWikiSpecialGeneratedData.addTotalEntryCount(fbb, impl.totalEntryCount());
                MediaWikiSpecialGeneratedData.addGroupEstimatedHeights(fbb, groupEstimatedHeightsOff);
                MediaWikiSpecialGeneratedData.addGroupEntryCounts(fbb, groupEntryCountsOff);
                MediaWikiSpecialGeneratedData.addGroupTitleWidths(fbb, groupTitleWidthsOff);
                MediaWikiSpecialGeneratedData.addGroupCount(fbb, impl.groupCount());
                MediaWikiSpecialGeneratedData.addColumnCount(fbb, impl.columnCount());
                MediaWikiSpecialGeneratedData.addHasMore(fbb, impl.hasMore());
                return MediaWikiSpecialGeneratedData.endMediaWikiSpecialGeneratedData(fbb);
            }
        }

        // Fallback: no block data or no font facts (should not happen in normal flow).
        // All slot values equal their fbs defaults → empty table, wire-identical
        // to the previous create() call (max_content_height DEPRECATED, unwritten).
        MediaWikiSpecialGeneratedData.startMediaWikiSpecialGeneratedData(fbb);
        MediaWikiSpecialGeneratedData.addSubtitleLineHeight(fbb, 10.0f);
        MediaWikiSpecialGeneratedData.addLinkLineHeight(fbb, 10.0f);
        MediaWikiSpecialGeneratedData.addSubtitleSpaceWidth(fbb, 4.0f);
        MediaWikiSpecialGeneratedData.addColumnCount(fbb, 1);
        MediaWikiSpecialGeneratedData.addHasMore(fbb, false);
        return MediaWikiSpecialGeneratedData.endMediaWikiSpecialGeneratedData(fbb);
    }

    private static int buildChildrenVector(FlatBufferBuilder fbb, List<Integer> indices) {
        fbb.startVector(4, indices.size(), 4);
        for (int i = indices.size() - 1; i >= 0; i--) {
            fbb.addInt(indices.get(i));
        }
        return fbb.endVector();
    }
}
