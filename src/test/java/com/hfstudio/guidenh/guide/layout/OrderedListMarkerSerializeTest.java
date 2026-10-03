package com.hfstudio.guidenh.guide.layout;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.block.LytList;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput;

/**
 * Ordered-list numbering is serialized on the first layout pass.
 *
 * <p>
 * Serialization runs before {@code afterExternalLayout()} inside
 * {@code LytDocument.createLayout}, so on the first pass
 * {@code LytListItem.cachedOrderedNumber} is still -1. Marker serialization
 * must not depend on that cache: if it did, every ordered list would render
 * as a bullet on its first pass. This test serializes a tree that has not
 * been through {@code afterExternalLayout} and asserts that the serialized
 * {@code ListMarkerData.text} already carries "N." with the list start
 * semantics applied.
 */
class OrderedListMarkerSerializeTest {

    private static LayoutInput serialize(LytList root) {
        byte[] output = new LayoutTreeSerializer().serialize(root, 800f, 1.0f, 1.0f);
        return LayoutInput.getRootAsLayoutInput(ByteBuffer.wrap(output));
    }

    @Test
    void orderedListItemsSerializeLiveNumbersOnFirstPass() {
        var list = new LytList(true, 3);
        list.append(new LytListItem());
        list.append(new LytListItem());
        list.append(new LytListItem());
        // NOTE: no applyExternalLayout / afterExternalLayout; serialize IS the
        // first pass here.
        LayoutInput input = serialize(list);
        assertEquals(4, input.nodesLength(), "list + 3 items");
        assertEquals(
            "3.",
            input.nodes(1)
                .listMarker()
                .text(),
            "first item must start at list start");
        assertEquals(
            "4.",
            input.nodes(2)
                .listMarker()
                .text(),
            "second item must count up");
        assertEquals(
            "5.",
            input.nodes(3)
                .listMarker()
                .text(),
            "third item must count up");
    }

    @Test
    void orderedListStartOneSerializesFromOne() {
        var list = new LytList(true, 1);
        list.append(new LytListItem());
        LayoutInput input = serialize(list);
        assertEquals(
            "1.",
            input.nodes(1)
                .listMarker()
                .text());
    }

    @Test
    void unorderedListItemsSerializeBulletOnFirstPass() {
        var list = new LytList(false, 1);
        list.append(new LytListItem());
        LayoutInput input = serialize(list);
        assertEquals(
            "\u2022",
            input.nodes(1)
                .listMarker()
                .text());
    }
}
