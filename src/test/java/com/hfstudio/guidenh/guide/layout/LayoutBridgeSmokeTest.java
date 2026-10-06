package com.hfstudio.guidenh.guide.layout;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FlatLayout;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FlatNode;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutResult;
import com.hfstudio.guidenh.guide.layout.flatbuffers.Style;

/**
 * Smoke test for the Rust JNI bridge (LayoutBridge).
 * <p>
 * Run with:
 * {@code ./gradlew test --tests "*LayoutBridgeSmokeTest" -Dguide.native.lib.path=src/rust/layout-engine/target/release/guide_layout_engine.dll}
 */
@EnabledIfSystemProperty(named = "guide.native.lib.path", matches = ".+")
class LayoutBridgeSmokeTest {

    @BeforeAll
    static void checkLib() {
        String p = System.getProperty("guide.native.lib.path");
        assertNotNull(p);
        assertTrue(new java.io.File(p).exists(), "Native library not found: " + p);
    }

    @Test
    void testInitReturnsNonZero() {
        long handle = LayoutBridge.init(new byte[0], "en-US");
        assertTrue(handle != 0, "init must return non-zero handle");
        LayoutBridge.destroy(handle);
    }

    @Test
    void testDestroyZeroIsNoop() {
        LayoutBridge.destroy(0); // should not crash
    }

    @Test
    void testInitDestroyRoundTrip() {
        long handle = LayoutBridge.init(new byte[0], "en-US");
        assertTrue(handle != 0);
        LayoutBridge.destroy(handle);
    }

    @Test
    void testMeasureLayoutEmptyInput() {
        long handle = LayoutBridge.init(new byte[0], "en-US");
        byte[] result = LayoutBridge.measureLayout(handle, new byte[0]);
        assertNotNull(result);
        assertEquals(0, result.length, "empty input should produce empty output");
        LayoutBridge.destroy(handle);
    }

    @Test
    void testMeasureLayoutWithRootNode() {
        long handle = LayoutBridge.init(new byte[0], "en-US");

        byte[] input = buildMinimalLayoutInput(800f);
        byte[] output = LayoutBridge.measureLayout(handle, input);
        assertNotNull(output);
        assertTrue(output.length > 0, "valid FlatBuffer input should produce non-empty output");

        // Parse the result
        LayoutResult result = LayoutResult.getRootAsLayoutResult(ByteBuffer.wrap(output));
        assertTrue(result.nodesLength() == 1, "should have exactly 1 layout node");

        // Container node with no children → (0,0,0,0), content_height=0
        // (Taffy computes no intrinsic size for an empty flex container)
        FlatLayout fl = result.nodes(0);
        assertTrue(fl.x() >= 0);
        assertTrue(fl.y() >= 0);
        assertTrue(fl.w() >= 0);
        assertTrue(fl.h() >= 0);

        LayoutBridge.destroy(handle);
    }

    /** Build a FlatBuffer int vector. */
    private static int createIntVec(FlatBufferBuilder fbb, int[] data) {
        fbb.startVector(4, data.length, 4);
        for (int i = data.length - 1; i >= 0; i--) {
            fbb.addInt(data[i]);
        }
        return fbb.endVector();
    }

    private byte[] buildMinimalLayoutInput(float availableWidth) {
        FlatBufferBuilder fbb = new FlatBufferBuilder(256);

        int styleOff = Style.createStyle(
            fbb,
            (byte) 0, // display Flex
            (byte) 1, // flex_direction Column
            (byte) 0, // flex_wrap
            (byte) 0, // align_items
            (byte) 0, // align_self
            (byte) 0, // justify_content
            0,
            0, // gap_w, gap_h
            0,
            0, // size_w, size_h
            0,
            0, // min_w, min_h
            0,
            0, // max_w, max_h
            0f, // aspect_ratio
            0f,
            0f,
            0f,
            0f, // margin
            false,
            false,
            false,
            false, // margin_auto
            0f,
            0f,
            0f,
            0f, // padding
            0f,
            0f,
            0f,
            0f, // border
            (byte) 0, // overflow
            0f, // flex_grow
            1f, // flex_shrink
            0, // flex_basis
            (byte) 0, // float
            (byte) 0, // clear
            (byte) 0, // position
            0,
            0,
            0,
            0 // inset
        );

        int childrenVec = createIntVec(fbb, new int[0]);
        int nodeOff = FlatNode.createFlatNode(
            fbb,
            styleOff,
            (byte) 0,
            0,
            0,
            0,
            0,
            0,
            0,
            (byte) 0,
            childrenVec,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0);

        int nodesVec = fbb.createVectorOfTables(new int[] { nodeOff });
        int inputOff = LayoutInput.createLayoutInput(fbb, availableWidth, 1.0f, 1.0f, (byte) 1, nodesVec, 0.25f); // Shear
                                                                                                                  // factor,
                                                                                                                  // formerly
                                                                                                                  // LayoutTreeSerializer.SHEAR_K;
                                                                                                                  // the
                                                                                                                  // Rust
                                                                                                                  // engine
                                                                                                                  // owns
                                                                                                                  // SHEAR_K
                                                                                                                  // now.
        fbb.finish(inputOff);

        return fbb.sizedByteArray();
    }
}
