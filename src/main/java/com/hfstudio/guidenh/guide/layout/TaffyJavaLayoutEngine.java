package com.hfstudio.guidenh.guide.layout;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.layout.flatbuffers.Dimension;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FlatLayout;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FlatNode;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutResult;
import com.hfstudio.guidenh.guide.layout.flatbuffers.Style;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;

/** Pure Java adapter from GuideNH's FlatBuffer protocol to taffy-java. */
public class TaffyJavaLayoutEngine {

    public byte[] compute(byte[] input) {
        if (input == null || input.length == 0) return new byte[0];
        LayoutInput layoutInput = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(input));
        int count = layoutInput.nodesLength();
        if (count == 0) return emptyResult();

        FlatNode[] flatNodes = new FlatNode[count];
        int[] parentIndices = new int[count];
        Arrays.fill(parentIndices, -1);
        for (int i = 0; i < count; i++) flatNodes[i] = layoutInput.nodes(i);

        TaffyTree tree = new TaffyTree();
        NodeId[] ids = new NodeId[count];
        for (int i = 0; i < count; i++) {
            TaffyStyle style = toStyle(flatNodes[i] == null ? null : flatNodes[i].style());
            ids[i] = tree.newLeafWithContext(style, flatNodes[i]);
        }
        for (int i = count - 1; i >= 0; i--) {
            FlatNode node = flatNodes[i];
            if (node == null || node.childrenLength() == 0) continue;
            List<NodeId> children = new ArrayList<>(node.childrenLength());
            for (int j = 0; j < node.childrenLength(); j++) {
                int child = (int) node.children(j);
                if (child >= 0 && child < count) {
                    parentIndices[child] = i;
                    children.add(ids[child]);
                }
            }
            if (!children.isEmpty()) {
                NodeId parent = tree.newWithChildren(tree.getStyle(ids[i]), children);
                ids[i] = parent;
            }
        }

        float width = layoutInput.availableWidth();
        if (!(width > 0f)) width = 0f;
        tree.computeLayoutWithMeasure(
            ids[0],
            TaffySize.of(AvailableSpace.definite(width), AvailableSpace.maxContent()),
            this::measure);

        FlatBufferBuilder builder = new FlatBufferBuilder(Math.max(4096, count * 48));
        int[] offsets = new int[count];
        float contentHeight = 0f;
        float[] absoluteX = new float[count];
        float[] absoluteY = new float[count];
        for (int i = 0; i < count; i++) {
            Layout layout = tree.getLayout(ids[i]);
            if (layout == null) layout = new Layout();
            int parent = parentIndices[i];
            absoluteX[i] = layout.location().x + (parent >= 0 ? absoluteX[parent] : 0f);
            absoluteY[i] = layout.location().y + (parent >= 0 ? absoluteY[parent] : 0f);
            offsets[i] = FlatLayout.createFlatLayout(
                builder,
                absoluteX[i],
                absoluteY[i],
                Math.max(0f, layout.size().width),
                Math.max(0f, layout.size().height),
                i);
            if (i == 0) contentHeight = Math.max(0f, layout.size().height);
        }
        int nodes = LayoutResult.createNodesVector(builder, offsets);
        int result = LayoutResult.createLayoutResult(builder, nodes, 0, 0, 0, contentHeight, 0);
        builder.finish(result);
        return builder.sizedByteArray();
    }

    private FloatSize measure(FloatSize known, TaffySize<AvailableSpace> available, NodeId node, FlatNode context,
        TaffyStyle style) {
        if (context == null) return FloatSize.zero();
        if (context.text() != null) {
            String text = context.text()
                .text();
            if (text == null || text.isEmpty()) return FloatSize.zero();
            float maxWidth = 0f;
            float lineWidth = 0f;
            int lines = 1;
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '\n') {
                    maxWidth = Math.max(maxWidth, lineWidth);
                    lineWidth = 0f;
                    lines++;
                } else {
                    lineWidth += ch == '\t' ? 4f * 7f : 7f;
                }
            }
            maxWidth = Math.max(maxWidth, lineWidth);
            float availableWidth = available.width != null ? available.width.intoOption() : Float.NaN;
            if (!Float.isNaN(availableWidth) && availableWidth > 0f) {
                lines = Math.max(lines, (int) Math.ceil(maxWidth / availableWidth));
                maxWidth = Math.min(maxWidth, availableWidth);
            }
            return FloatSize.of(maxWidth, lines * 10f);
        }
        return FloatSize.zero();
    }

    private TaffyStyle toStyle(Style source) {
        TaffyStyle style = new TaffyStyle();
        if (source == null) return style;
        style.display = switch (source.display()) {
            case 1 -> TaffyDisplay.BLOCK;
            case 2 -> TaffyDisplay.GRID;
            case 3 -> TaffyDisplay.NONE;
            case 4 -> TaffyDisplay.FLOW_ROOT;
            default -> TaffyDisplay.FLEX;
        };
        style.flexDirection = source.flexDirection() == 0 ? FlexDirection.ROW : FlexDirection.COLUMN;
        style.flexWrap = switch (source.flexWrap()) {
            case 1 -> FlexWrap.WRAP;
            case 2 -> FlexWrap.WRAP_REVERSE;
            case 3 -> FlexWrap.BALANCE;
            case 4 -> FlexWrap.BALANCE_REVERSE;
            default -> FlexWrap.NO_WRAP;
        };
        style.alignItems = align(source.alignItems());
        style.alignSelf = align(source.alignSelf());
        style.justifyContent = alignContent(source.justifyContent());
        style.size = TaffySize.of(dimension(source.sizeW()), dimension(source.sizeH()));
        style.minSize = TaffySize.of(lengthAuto(source.minW()), lengthAuto(source.minH()));
        style.maxSize = TaffySize.of(lengthAuto(source.maxW()), lengthAuto(source.maxH()));
        style.margin = TaffyRect.of(
            LengthPercentageAuto.length(source.marginLeft()),
            LengthPercentageAuto.length(source.marginRight()),
            LengthPercentageAuto.length(source.marginTop()),
            LengthPercentageAuto.length(source.marginBottom()));
        style.padding = TaffyRect.of(
            LengthPercentage.length(source.paddingLeft()),
            LengthPercentage.length(source.paddingRight()),
            LengthPercentage.length(source.paddingTop()),
            LengthPercentage.length(source.paddingBottom()));
        style.border = TaffyRect.of(
            LengthPercentage.length(source.borderLeft()),
            LengthPercentage.length(source.borderRight()),
            LengthPercentage.length(source.borderTop()),
            LengthPercentage.length(source.borderBottom()));
        style.gap = TaffySize.of(length(source.gapW()), length(source.gapH()));
        style.flexGrow = source.flexGrow();
        style.flexShrink = source.flexShrink();
        style.floatMode = switch (source.float_()) {
            case 1 -> TaffyFloat.LEFT;
            case 2 -> TaffyFloat.RIGHT;
            default -> TaffyFloat.NONE;
        };
        style.clear = switch (source.clear()) {
            case 1 -> Clear.LEFT;
            case 2 -> Clear.RIGHT;
            case 3 -> Clear.BOTH;
            default -> Clear.NONE;
        };
        style.position = source.position() == 1 ? TaffyPosition.ABSOLUTE : TaffyPosition.RELATIVE;
        style.overflow = new TaffyPoint<>(overflow(source.overflow()), overflow(source.overflow()));
        return style;
    }

    private static AlignItems align(byte value) {
        return switch (value) {
            case 1 -> AlignItems.CENTER;
            case 2 -> AlignItems.END;
            case 3, 4 -> AlignItems.STRETCH;
            default -> AlignItems.AUTO;
        };
    }

    private static AlignContent alignContent(byte value) {
        return switch (value) {
            case 1 -> AlignContent.CENTER;
            case 2 -> AlignContent.END;
            case 3, 4 -> AlignContent.STRETCH;
            default -> AlignContent.START;
        };
    }

    private static Overflow overflow(byte value) {
        return switch (value) {
            case 1 -> Overflow.CLIP;
            case 2 -> Overflow.SCROLL;
            case 3 -> Overflow.HIDDEN;
            default -> Overflow.VISIBLE;
        };
    }

    private static TaffyDimension dimension(Dimension value) {
        if (value == null) return TaffyDimension.AUTO;
        return value.unit() == 2 ? TaffyDimension.percent(value.value() / 100f) : TaffyDimension.length(value.value());
    }

    private static LengthPercentageAuto lengthAuto(Dimension value) {
        if (value == null) return LengthPercentageAuto.AUTO;
        return value.unit() == 2 ? LengthPercentageAuto.percent(value.value() / 100f)
            : LengthPercentageAuto.length(value.value());
    }

    private static LengthPercentage length(Dimension value) {
        if (value == null) return LengthPercentage.ZERO;
        return value.unit() == 2 ? LengthPercentage.percent(value.value() / 100f)
            : LengthPercentage.length(value.value());
    }

    private static byte[] emptyResult() {
        FlatBufferBuilder builder = new FlatBufferBuilder(64);
        int result = LayoutResult.createLayoutResult(builder, 0, 0, 0, 0, 0, 0);
        builder.finish(result);
        return builder.sizedByteArray();
    }
}
