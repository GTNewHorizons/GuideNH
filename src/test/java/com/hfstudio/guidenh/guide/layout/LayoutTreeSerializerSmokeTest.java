package com.hfstudio.guidenh.guide.layout;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBox;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytSlotGrid;
import com.hfstudio.guidenh.guide.document.block.LytThematicBreak;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FlatLayout;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FlatNode;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutResult;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

/**
 * Contract tests for the layout compiler's lowering rules (no JNI dependency):
 * how high-level Lyt blocks are translated into Taffy-visible semantics.
 */
class LayoutTreeSerializerSmokeTest {

    /** Plain childless container with preset bounds. */
    private static class PlainLeafBox extends LytBox {

        PlainLeafBox(int x, int y, int w, int h) {
            this.bounds = new LytRect(x, y, w, h);
        }

        @Override
        protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
            return bounds;
        }
    }

    /** Child fixture that honors the layout position, with fixed size. */
    private static class FixedChild extends LytBox {

        FixedChild() {
            this(0, 0, 60, 6);
        }

        FixedChild(int x, int y, int w, int h) {
            this.bounds = new LytRect(x, y, w, h);
        }

        @Override
        protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
            return new LytRect(x, y, 60, 6);
        }
    }

    @Test
    void childlessContainerNoLongerGetsJavaBoundsSize() {
        // SIZE_FROM_JAVA_BOUNDS was removed with the Java layout pre-pass.
        // Childless opaque containers no longer receive an explicit size from
        // Java-computed bounds; size is now declared through explicit dimensions
        // or Rust-side measure functions.
        var leaf = new PlainLeafBox(0, 0, 77, 33);

        byte[] output = new LayoutTreeSerializer().serialize(leaf, 800f, 1.0f, 1.0f);
        LayoutInput input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));

        assertEquals(1, input.nodesLength());
        FlatNode node = input.nodes(0);
        assertEquals(0, node.nodeType(), "fixture must be a Container node");
        assertNull(
            node.style()
                .sizeW(),
            "childless container no longer gets an explicit width from Java bounds");
        assertNull(
            node.style()
                .sizeH(),
            "childless container no longer gets an explicit height from Java bounds");
    }

    @Test
    void normalBlockChildrenAreStillSerialized() {
        var root = new LytVBox();
        root.append(new LytThematicBreak());
        root.append(new LytThematicBreak());

        byte[] output = new LayoutTreeSerializer().serialize(root, 800f, 1.0f, 1.0f);
        LayoutInput input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));

        assertEquals(3, input.nodesLength(), "normal container keeps children in the flat tree");
        assertEquals(
            2,
            input.nodes(0)
                .childrenLength());
    }

    @Test
    void paragraphSizeIsNotForcedFromBounds() {
        var par = new LytParagraph();
        par.appendText("hello");
        // Simulate bounds from the Java layout pass: Rust text measurement
        // must stay authoritative for text nodes.
        par.applyExternalLayout(new LytRect(0, 0, 200, 20));

        byte[] output = new LayoutTreeSerializer().serialize(par, 800f, 1.0f, 1.0f);
        LayoutInput input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));

        FlatNode node = input.nodes(0);
        assertEquals(1, node.nodeType(), "paragraph must serialize as Text");
        assertNull(
            node.style()
                .sizeW(),
            "text nodes must not get a px width from Java bounds");
        assertNull(
            node.style()
                .sizeH(),
            "text nodes must not get a px height from Java bounds");
    }

    @Test
    void hboxWrapSerializesAsFlexWrap() {
        var row = new LytHBox(); // wrap defaults to true
        row.append(new LytThematicBreak());

        byte[] output = new LayoutTreeSerializer().serialize(row, 800f, 1.0f, 1.0f);
        LayoutInput input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));

        assertEquals(
            0,
            input.nodes(0)
                .style()
                .flexDirection(),
            "HBox must serialize as Row");
        assertEquals(
            1,
            input.nodes(0)
                .style()
                .flexWrap(),
            "wrapping HBox must serialize flex_wrap=Wrap");

        row.setWrap(false);
        output = new LayoutTreeSerializer().serialize(row, 800f, 1.0f, 1.0f);
        input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));
        assertEquals(
            0,
            input.nodes(0)
                .style()
                .flexWrap(),
            "non-wrapping HBox must serialize flex_wrap=NoWrap");
    }

    @Test
    void slotGridLowersToWrappingRowWithExplicitWidth() {
        var grid = new LytSlotGrid(3, 3);

        byte[] output = new LayoutTreeSerializer().serialize(grid, 800f, 1.0f, 1.0f);
        LayoutInput input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));

        FlatNode node = input.nodes(0);
        assertEquals(
            0,
            node.style()
                .flexDirection(),
            "slot grid must lower to a Row container");
        assertEquals(
            1,
            node.style()
                .flexWrap(),
            "slot grid must lower to a wrapping row");
        assertNotNull(
            node.style()
                .sizeW(),
            "fixed grid must carry an explicit width");
        assertEquals(
            54f,
            node.style()
                .sizeW()
                .value(),
            0.001f,
            "3 columns x 18px");
    }

    @Test
    void documentFloatEliminatedInnerCarriesRealFloat() {
        var root = new LytVBox();
        root.applyExternalLayout(new LytRect(0, 0, 800, 600)); // give the root flow bounds
        var floatWrapper = new LytDocumentFloat(new FixedChild(), true);
        // Lay out so the inner has bounds; right float in 200px at x=5 → inner at (145,10).
        floatWrapper.layout(new LayoutContext(null), 5, 10, 200);
        root.append(floatWrapper);
        root.append(new LytThematicBreak());

        byte[] output = new LayoutTreeSerializer().serialize(root, 800f, 1.0f, 1.0f);
        LayoutInput input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));

        assertEquals(
            3,
            input.nodesLength(),
            "wrapper is eliminated: root + floated inner + sibling (no zero-height anchor node)");
        FlatNode inner = input.nodes(1);
        assertEquals(
            2,
            inner.style()
                .float_(),
            "the inner carries a real right-float for the Rust pusher");
        assertEquals(
            0,
            inner.style()
                .position(),
            "the inner is in-flow (a float), not position:absolute");
        assertEquals(
            LytDocumentFloat.FLOAT_GAP,
            inner.style()
                .marginLeft(),
            0.001f,
            "right-float gap rides the inner's left margin (text-facing side)");
        // SIZE_FROM_JAVA_BOUNDS was removed with the Java layout pre-pass;
        // the float inner (a plain LytBox fixture with no explicit dimensions)
        // no longer carries a serialized size: Rust sizes it from its content.
        assertNull(
            inner.style()
                .sizeW(),
            "float inner no longer gets explicit width from Java bounds");
        assertNull(
            inner.style()
                .sizeH(),
            "float inner no longer gets explicit height from Java bounds");
    }

    @Test
    void floatAdjacentBlockIsNotLanePinned() {
        var root = new LytVBox();
        root.applyExternalLayout(new LytRect(0, 0, 800, 600)); // give the root flow bounds
        var floatWrapper = new LytDocumentFloat(new FixedChild(), false); // left float at (5,10,60,6)
        floatWrapper.layout(new LayoutContext(null), 5, 10, 200);
        root.append(floatWrapper);
        // Non-text block beside the float: no longer lane-pinned (CSS: a block
        // box is not narrowed by a float; only line boxes wrap).
        root.append(new FixedChild(70, 12, 100, 6));

        byte[] output = new LayoutTreeSerializer().serialize(root, 800f, 1.0f, 1.0f);
        LayoutInput input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));

        assertEquals(3, input.nodesLength());
        FlatNode inner = input.nodes(1);
        assertEquals(
            1,
            inner.style()
                .float_(),
            "the inner carries a real left-float");
        FlatNode adjacent = input.nodes(2);
        assertEquals(
            0f,
            adjacent.style()
                .marginLeft(),
            0.001f,
            "no lane displacement on margin-left");
    }

    @Test
    void textParagraphEmitsNoFloatClipReplay() {
        var root = new LytVBox();
        root.applyExternalLayout(new LytRect(0, 0, 800, 600));
        var floatWrapper = new LytDocumentFloat(new FixedChild(), true); // right float: inner at (145,10,60,6)
        floatWrapper.layout(new LayoutContext(null), 5, 10, 200);
        root.append(floatWrapper);
        // A text paragraph in the float's band: no precomputed clip is emitted;
        // the Rust pusher wraps it against its own live float table.
        var par = new LytParagraph();
        par.appendText("hello");
        par.applyExternalLayout(new LytRect(5, 12, 100, 6));
        root.append(par);

        byte[] output = new LayoutTreeSerializer().serialize(root, 800f, 1.0f, 1.0f);
        LayoutInput input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));

        assertEquals(3, input.nodesLength());
        FlatNode para = input.nodes(2);
        var text = para.text();
        assertNotNull(text, "paragraph must carry text data");
        assertEquals(
            0,
            text.floatClipsLength(),
            "no clip replay: wrapping is owned by the Rust pusher's live float table");
    }

    @Test
    void preWrapParagraphIsTextLeaf() {
        var par = new LytParagraph();
        par.modifyStyle(style -> style.whiteSpace(WhiteSpaceMode.PRE_WRAP));
        par.appendText("code");

        byte[] output = new LayoutTreeSerializer().serialize(par, 800f, 1.0f, 1.0f);
        LayoutInput input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));

        assertEquals(
            1,
            input.nodes(0)
                .nodeType(),
            "PRE_WRAP paragraphs serialize as Text (parley preserves spaces and \n natively)");
    }

    @Test
    void testDimHelpers() {
        // Verify dimension helpers produce valid offsets
        com.google.flatbuffers.FlatBufferBuilder fbb = new com.google.flatbuffers.FlatBufferBuilder(64);
        int auto = LayoutStyleExtractor.dimAuto(fbb);
        assertEquals(0, auto, "dimAuto should return 0");
    }

    @Test
    void testStyleBuilderProducesValidStyle() {
        // Minimal LytBlock: LayoutStyleExtractor must handle it.
        LytThematicBreak tb = new LytThematicBreak();
        com.google.flatbuffers.FlatBufferBuilder fbb = new com.google.flatbuffers.FlatBufferBuilder(256);
        int styleOff = LayoutStyleExtractor
            .build(fbb, tb, LayoutStyleExtractor.Flags.NONE, LayoutStyleExtractor.NodeAdjustments.ZERO);
        assertTrue(styleOff != 0, "build() should return non-zero Style offset");
    }

    @Test
    void testSerializerProducesValidFlatBuffer() {
        // Serialize a minimal tree through LayoutTreeSerializer.
        LytThematicBreak tb = new LytThematicBreak();
        LayoutTreeSerializer serializer = new LayoutTreeSerializer();

        byte[] output = serializer.serialize(tb, 800f, 1.0f, 1.0f);
        assertNotNull(output);
        assertTrue(output.length > 0, "serializer should produce non-empty output");

        // Verify we can parse the output as a LayoutInput
        LayoutInput input = LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));
        assertTrue(input.availableWidth() > 0);
        assertTrue(input.nodesLength() >= 1, "should have at least 1 node");
    }

    @Test
    void testFullPipelineWithJni() {
        // Full pipeline: Java LytBlock → Serializer → JNI → Rust → result
        String libPath = System.getProperty("guide.native.lib.path");
        if (libPath == null || libPath.isEmpty()) {
            return; // skip if JNI not available
        }
        System.load(libPath);

        LytThematicBreak tb = new LytThematicBreak();
        LayoutTreeSerializer serializer = new LayoutTreeSerializer();
        byte[] input = serializer.serialize(tb, 800f, 1.0f, 1.0f);

        long handle = LayoutBridge.init(new byte[0], "en-US");
        assertTrue(handle != 0);

        byte[] output = LayoutBridge.measureLayout(handle, input);
        assertNotNull(output);
        assertTrue(output.length > 0, "middle layer + JNI should produce non-empty result");

        LayoutResult result = LayoutResult.getRootAsLayoutResult(ByteBuffer.wrap(output));
        assertTrue(result.nodesLength() >= 1, "LayoutResult should have at least 1 node");

        FlatLayout fl = result.nodes(0);
        assertTrue(fl.x() >= 0);
        assertTrue(fl.y() >= 0);
        assertTrue(fl.w() >= 0);
        assertTrue(fl.h() >= 0, "ThematicBreak should have non-negative height");

        LayoutBridge.destroy(handle);
    }
}
