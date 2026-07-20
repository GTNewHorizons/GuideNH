package com.hfstudio.guidenh.guide.layout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytFileTree;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytImageBlock;
import com.hfstudio.guidenh.guide.document.block.LytLatexBlock;
import com.hfstudio.guidenh.guide.document.block.LytLatexDisplayBlock;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.document.block.LytThematicBreak;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FlatNode;
import com.hfstudio.guidenh.guide.layout.flatbuffers.ImageData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LatexDisplayData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.SlotData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextData;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextSpan;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextStyle;
import com.hfstudio.guidenh.guide.layout.flatbuffers.ThematicBreakData;
import com.hfstudio.guidenh.guide.layout.flow.LineTextRun;
import com.hfstudio.guidenh.guide.render.GuideText;
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
     * ascent, 2=center on line + offset).
     */
    public record InlineRef(int flatIndex, int align, float param) {}

    /**
     * One float-wrap band (schema TextBand): the paragraph's text is shaped in
     * sequential bands at per-band widths, mirroring CSS float wrapping
     * (narrow beside the float, full width below it).
     */
    public record BandSpec(int splitByte, float width, float marginLeft) {}

    /** Extract the full text of a paragraph (with U+FFFC placeholders for inline blocks). */
    static String paragraphText(LytParagraph par) {
        StringBuilder sb = new StringBuilder();
        for (LytFlowContent fc : par.getContent()) {
            extractFlowText(fc, sb);
        }
        return sb.toString();
    }

    public static int build(FlatBufferBuilder fbb, LytBlock block, int styleOff, List<Integer> childIndices,
        List<InlineRef> inlineRefs, List<BandSpec> bandSpecs) {
        byte nodeType = resolveNodeType(block);
        int textOff = nodeType == 1 ? buildTextData(fbb, block, inlineRefs, bandSpecs) : 0;
        int imageOff = nodeType == 2 ? buildImageData(fbb, block) : 0;
        int slotOff = nodeType == 3 ? buildSlotData(fbb, block) : 0;
        int breakOff = nodeType == 4 ? buildThematicBreakData(fbb) : 0;
        int latexOff = nodeType == 8 ? buildLatexData(fbb, block) : 0;
        byte customLayout = 0;

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
            childrenVec);
    }

    static byte resolveNodeType(LytBlock block) {
        if (block instanceof LytThematicBreak) return 4;
        if (block instanceof LytImage || block instanceof LytImageBlock) return 2;
        if (block instanceof LytSlot) return 3;
        if (block instanceof LytLatexBlock || block instanceof LytLatexDisplayBlock) return 8;
        if (block instanceof LytTable) return 7;
        if (block instanceof LytParagraph par && isOpaqueText(par)) return 0; // opaque leaf — no glyph run
        if (block instanceof LytParagraph) return 1; // Text — contains LytFlowText children
        return 0; // Container
    }

    /**
     * Blocks whose internal subtree structure the compiler keeps as opaque
     * leaves: children are not serialized (the Java layout positions them), and
     * the block reserves its box from the Java-computed flow bounds. Currently:
     * {@code LytFileTree} (icon+payload rows with connector lines are a custom
     * internal arrangement).
     */
    static boolean isOpaqueSubtree(LytBlock block) {
        return block instanceof LytFileTree;
    }

    /**
     * Paragraphs whose text cannot be represented by the glyph-run path are
     * lowered to opaque Container leaves: Rust reserves their box from the
     * Java-computed bounds, and rendering falls back to the legacy path
     * (HostDraw) automatically since no glyph run is produced.
     * <p>
     * Currently: PRE_WRAP paragraphs (code blocks, preformatted text),
     * float-aligned inline-block paragraphs, and dynamic-style paragraphs
     * (spoiler reveal, {@code §k}/obfuscated — see
     * {@link LytParagraph#hasDynamicStyles}). Static multi-style paragraphs
     * are serialized as rich Text with {@code TextData.spans} instead.
     */
    private static boolean isOpaqueText(LytParagraph par) {
        var resolved = par.resolveStyle();
        if (resolved != null && resolved.whiteSpace() == com.hfstudio.guidenh.guide.style.WhiteSpaceMode.PRE_WRAP) {
            return true;
        }
        // Paragraphs with float-aligned inline blocks keep the legacy path
        // (inline float layout is not part of the inline-block pipeline yet).
        for (LytFlowContent fc : par.getContent()) {
            if (hasFloatAlignedInlineBlock(fc)) return true;
        }
        // Dynamic styles (spoiler reveal, §k/obfuscated) cannot be baked into
        // a glyph run — it would render spoilers in plain text and draw §k
        // literally. Those paragraphs must stay on the legacy path.
        if (LytParagraph.hasDynamicStyles(par.getContent())) {
            return true;
        }
        // Static multi-style paragraphs are NOT opaque: they serialize as Text
        // with rich spans (TextData.spans) and get per-span glyph runs from
        // Rust. Mode-2 per-span emission remains as the fallback when no glyph
        // run is produced (Rust pipeline unavailable).
        return false;
    }

    private static boolean hasFloatAlignedInlineBlock(LytFlowContent fc) {
        if (fc instanceof LytFlowInlineBlock ib) {
            return ib.getAlignment() != com.hfstudio.guidenh.guide.document.flow.InlineBlockAlignment.INLINE;
        }
        if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                if (hasFloatAlignedInlineBlock(child)) return true;
            }
        }
        return false;
    }

    private static int buildTextData(FlatBufferBuilder fbb, LytBlock block, List<InlineRef> inlineRefs,
        List<BandSpec> bandSpecs) {
        String text = "";
        // Base em size matches the legacy MC font cell (FONT_HEIGHT = 9); the
        // guide's line height is FONT_HEIGHT+1 = 10, which text.rs mirrors as
        // size × 10/9. fontScale (headings etc.) scales both proportionally.
        float fontSize = 9f;
        boolean bold = false;
        boolean italic = false;
        float fontScale = 1f;
        long baseColor = 0xFFFFFFFFL;
        List<SpanPart> spanParts = new ArrayList<>();

        if (block instanceof LytParagraph par) {
            // Collect all text recursively — text can be nested inside LytFlowSpan wrappers
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
                // For now the Rust side uses font_size * font_scale * 1.4 fallback.
                bold = resolved.bold();
                italic = resolved.italic();
                if (resolved.fontScale() != 1f) {
                    fontScale = resolved.fontScale();
                }
                // Base run tint (single-style runs fall back to this color).
                baseColor = GuideText.resolveColor(resolved) & 0xFFFFFFFFL;
            }

            // Rich spans: per-leaf resolved styles in document order (concatenated
            // span texts equal the extracted text, U+FFFC placeholders included).
            for (LytFlowContent fc : par.getContent()) {
                collectSpanParts(fc, resolved, spanParts);
            }
        }

        int strOff = fbb.createString(text);
        int styleOff = TextStyle
            .createTextStyle(fbb, fontSize, bold, italic, fontScale, baseColor, 0L, false, false, 0L, false);
        int inlineBlocksVec = 0;
        if (!inlineRefs.isEmpty()) {
            int[] refs = new int[inlineRefs.size()];
            for (int i = 0; i < refs.length; i++) {
                var r = inlineRefs.get(i);
                refs[i] = com.hfstudio.guidenh.guide.layout.flatbuffers.InlineBlockRef
                    .createInlineBlockRef(fbb, r.flatIndex(), (byte) r.align(), r.param());
            }
            inlineBlocksVec = TextData.createInlineBlocksVector(fbb, refs);
        }
        int bandsVec = 0;
        if (!bandSpecs.isEmpty()) {
            int[] bands = new int[bandSpecs.size()];
            for (int i = 0; i < bands.length; i++) {
                var b = bandSpecs.get(i);
                bands[i] = com.hfstudio.guidenh.guide.layout.flatbuffers.TextBand
                    .createTextBand(fbb, b.splitByte(), b.width(), b.marginLeft());
            }
            bandsVec = TextData.createBandsVector(fbb, bands);
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
        return TextData.createTextData(fbb, strOff, styleOff, (byte) 0, inlineBlocksVec, bandsVec, spansVec);
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
            highlight = LightDarkMode.current() == LightDarkMode.DARK_MODE
                ? LineTextRun.INLINE_CODE_BACKGROUND_DARK & 0xFFFFFFFFL
                : LineTextRun.INLINE_CODE_BACKGROUND_LIGHT & 0xFFFFFFFFL;
        } else if (style.backgroundColor() != null) {
            highlight = style.backgroundColor()
                .resolve(LightDarkMode.current()) & 0xFFFFFFFFL;
        }
        return TextStyle.createTextStyle(
            fbb,
            fontSize,
            style.bold(),
            style.italic(),
            style.fontScale(),
            argb,
            0L,
            style.underlined(),
            style.strikethrough(),
            highlight,
            inlineCode);
    }

    /**
     * Recursively extract all text from a flow content tree.
     * Handles {@link LytFlowText} (direct text) and {@link LytFlowSpan}
     * (wrapper with nested children).
     */
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
            // We can't access private explicitWidth/explicitHeight from here,
            // but the explicit fields from schema default to -1 so Rust can
            // detect "not set" and fall through to naturalW * scaleX.
            // Read them via the setter's backing concept — if the image has
            // explicit dimensions they are encoded in the LytImage's layout output.
            // For now, naturalW/naturalH give the Rust side usable dimensions.
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
        float sourceScale = 100f;
        float userScale = 1f;
        int offsetX = 0;
        int offsetY = 0;
        float rawW = 0;
        float rawH = 0;
        float refH = 0;

        if (block instanceof LytLatexBlock lb) {
            formula = lb.getFormula();
            fillColorArgb = lb.getFillColorArgb();
            sourceScale = lb.getSourceScale();
            userScale = lb.getUserScale();
            offsetX = lb.getOffsetX();
            offsetY = lb.getOffsetY();
            var bds = lb.getBounds();
            if (bds != null) {
                rawW = bds.width();
                rawH = bds.height();
            }
        } else if (block instanceof LytLatexDisplayBlock ldb) {
            var bds = ldb.getBounds();
            if (bds != null) {
                rawW = bds.width();
                rawH = bds.height();
            }
        }

        int formulaOff = fbb.createString(formula);
        return LatexDisplayData.createLatexDisplayData(
            fbb,
            formulaOff,
            fillColorArgb,
            sourceScale,
            userScale,
            offsetX,
            offsetY,
            rawW,
            rawH,
            refH);
    }

    private static int buildChildrenVector(FlatBufferBuilder fbb, List<Integer> indices) {
        fbb.startVector(4, indices.size(), 4);
        for (int i = indices.size() - 1; i >= 0; i--) {
            fbb.addInt(indices.get(i));
        }
        return fbb.endVector();
    }
}
