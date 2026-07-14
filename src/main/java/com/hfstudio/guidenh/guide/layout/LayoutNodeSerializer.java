package com.hfstudio.guidenh.guide.layout;

import java.util.List;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.block.*;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.layout.flatbuffers.*;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Determines node_type and builds FlatNode with the appropriate sub-data table.
 */
public final class LayoutNodeSerializer {

    private LayoutNodeSerializer() {}

    public static int build(FlatBufferBuilder fbb, LytBlock block, int styleOff, List<Integer> childIndices) {
        byte nodeType = resolveNodeType(block);
        int textOff = nodeType == 1 ? buildTextData(fbb, block) : 0;
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

    private static byte resolveNodeType(LytBlock block) {
        if (block instanceof LytThematicBreak) return 4;
        if (block instanceof LytImage || block instanceof LytImageBlock) return 2;
        if (block instanceof LytSlot) return 3;
        if (block instanceof LytLatexBlock || block instanceof LytLatexDisplayBlock) return 8;
        if (block instanceof LytTable) return 7;
        if (block instanceof LytFileTree) return 6;
        if (block instanceof LytParagraph) return 1; // Text — contains LytFlowText children
        return 0; // Container
    }

    private static int buildTextData(FlatBufferBuilder fbb, LytBlock block) {
        String text = "";
        float fontSize = 14f;
        boolean bold = false;
        boolean italic = false;
        float fontScale = 1f;

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
                GuideDebugLog.warnAlways("Layout: para text(len={}) preview=\"{}\"", text.length(), text.substring(0, 80));
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
            }
        }

        int strOff = fbb.createString(text);
        int styleOff = TextStyle.createTextStyle(fbb, fontSize, bold, italic, fontScale, 0xFFFFFFFF, 0);
        return TextData.createTextData(fbb, strOff, styleOff, (byte) 0);
    }

    /**
     * Recursively extract all text from a flow content tree.
     * Handles {@link LytFlowText} (direct text) and {@link LytFlowSpan}
     * (wrapper with nested children).
     */
    private static void extractFlowText(LytFlowContent fc, StringBuilder out) {
        if (fc instanceof LytFlowText ft) {
            out.append(ft.getText());
        } else if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                extractFlowText(child, out);
            }
        }
        // Other flow types (LytFlowInlineBlock, LytFlowBreak, etc.) contribute no text
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

        return ImageData.createImageData(fbb, naturalW, naturalH, cropX, cropY, cropW, cropH, scaleX, scaleY,
            explicitW, explicitH);
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
        return LatexDisplayData.createLatexDisplayData(fbb, formulaOff, fillColorArgb, sourceScale, userScale,
            offsetX, offsetY, rawW, rawH, refH);
    }

    private static int buildChildrenVector(FlatBufferBuilder fbb, List<Integer> indices) {
        fbb.startVector(4, indices.size(), 4);
        for (int i = indices.size() - 1; i >= 0; i--) {
            fbb.addInt(indices.get(i));
        }
        return fbb.endVector();
    }
}
