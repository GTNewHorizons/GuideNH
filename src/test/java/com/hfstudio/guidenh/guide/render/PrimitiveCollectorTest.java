package com.hfstudio.guidenh.guide.render;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBox;
import com.hfstudio.guidenh.guide.layout.LayoutContext;

/**
 * Contract tests for {@link PrimitiveCollector}: traversal order, culling,
 * legacy fallback, scissor pairing and transform-stack symmetry. Pure Java:
 * no GL, no Minecraft.
 */
class PrimitiveCollectorTest {

    /**
     * Minimal block: fixed bounds, can hold children, configurable
     * primitive/legacy path and children clip rect.
     */
    private static class TestBlock extends LytBox {

        boolean usePrimitives = true;
        LytRect childrenClip;

        TestBlock(int x, int y, int w, int h) {
            this.bounds = new LytRect(x, y, w, h);
        }

        @Override
        public boolean usePrimitives() {
            return usePrimitives;
        }

        @Override
        public void computePrimitives(PrimitiveCollector c) {
            c.emit(
                new GuideRenderPrimitive.FillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF112233));
        }

        @Override
        public LytRect getChildrenClipRect() {
            return childrenClip;
        }

        @Override
        public void emitDecorations(PrimitiveCollector c) {
            c.emit(
                new GuideRenderPrimitive.DrawBorder(
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    bounds.height(),
                    1,
                    1,
                    1,
                    1,
                    0xFF445566));
        }

        @Override
        protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
            return bounds;
        }

        @Override
        protected void onLayoutMoved(int deltaX, int deltaY) {}
    }

    private static long count(List<GuideRenderPrimitive> prims, Class<?> type) {
        return prims.stream()
            .filter(type::isInstance)
            .count();
    }

    private static int indexOf(List<GuideRenderPrimitive> prims, Class<?> type, int fromIndex) {
        for (int i = fromIndex; i < prims.size(); i++) {
            if (type.isInstance(prims.get(i))) return i;
        }
        return -1;
    }

    @Test
    void culledSubtreeEmitsNothing() {
        var pc = new PrimitiveCollector(new LytRect(0, 0, 200, 200), null);
        pc.pushTransform(0, -100, 1.0f); // scroll 100: doc y=500 -> screen y=400, outside

        TestBlock parent = new TestBlock(0, 500, 50, 50);
        parent.append(new TestBlock(0, 510, 10, 10));
        pc.collectFrom(parent);

        var prims = pc.result();
        assertEquals(0, count(prims, GuideRenderPrimitive.FillRect.class), "culled subtree emits no draws");
        assertEquals(0, count(prims, GuideRenderPrimitive.HostDraw.class), "culled legacy subtree emits no HostDraw");
        assertEquals(0, count(prims, GuideRenderPrimitive.DrawBorder.class), "culled subtree emits no decorations");
    }

    @Test
    void partiallyVisibleLegacyBlockEmitsOneHostDrawAndDoesNotRecurse() {
        var pc = new PrimitiveCollector(new LytRect(0, 0, 200, 200), null);
        pc.pushTransform(0, -100, 1.0f);

        TestBlock legacy = new TestBlock(0, 190, 50, 50); // screen y=90..140: visible
        legacy.usePrimitives = false;
        legacy.append(new TestBlock(0, 200, 10, 10)); // must NOT be visited

        pc.collectFrom(legacy);

        var prims = pc.result();
        assertEquals(1, count(prims, GuideRenderPrimitive.HostDraw.class), "legacy block must emit one HostDraw");
        assertEquals(0, count(prims, GuideRenderPrimitive.FillRect.class), "legacy subtree must not be recursed into");
        assertEquals(0, count(prims, GuideRenderPrimitive.DrawBorder.class), "legacy decorations must not be emitted");
    }

    @Test
    void clipRectWrapsChildrenAndDecorationsStayOutside() {
        var pc = new PrimitiveCollector(new LytRect(0, 0, 500, 500), null);

        TestBlock clipper = new TestBlock(0, 0, 100, 100);
        clipper.childrenClip = new LytRect(0, 0, 50, 50);
        clipper.append(new TestBlock(10, 10, 10, 10));

        pc.collectFrom(clipper);
        var prims = pc.result();

        int ownFill = indexOf(prims, GuideRenderPrimitive.FillRect.class, 0);
        int pushSc = indexOf(prims, GuideRenderPrimitive.PushScissor.class, 0);
        int childFill = indexOf(prims, GuideRenderPrimitive.FillRect.class, ownFill + 1);
        int popSc = indexOf(prims, GuideRenderPrimitive.PopScissor.class, 0);
        int ownBorder = indexOf(prims, GuideRenderPrimitive.DrawBorder.class, popSc);

        assertTrue(ownFill >= 0 && ownFill < pushSc, "own primitives before PushScissor");
        assertTrue(pushSc >= 0 && pushSc < childFill, "PushScissor before child primitives");
        assertTrue(childFill > 0 && childFill < popSc, "child primitives before PopScissor");
        assertTrue(ownBorder > popSc, "decorations after PopScissor (outside clip)");
    }

    @Test
    void transformPushPopStaysSymmetric() {
        var pc = new PrimitiveCollector(new LytRect(0, 0, 100, 100), null);
        pc.pushTransform(10, 20, 2.0f);
        pc.pushTransform(5, 5, 1.5f);
        pc.popTransform();
        pc.popTransform();
        pc.popTransform(); // no-op: root frame must be kept
        pc.popTransform(); // no-op

        var prims = pc.result();
        assertEquals(2, count(prims, GuideRenderPrimitive.PushTransform.class));
        assertEquals(
            2,
            count(prims, GuideRenderPrimitive.PopTransform.class),
            "pops beyond the root frame must not be emitted");
    }

    @Test
    void cullAccountsForTransformScale() {
        // zoom 2 + scroll: doc y=90 -> screen y = 90*2 - 150 = 30 (visible);
        // without the transform it would look outside the 200px viewport.
        var pc = new PrimitiveCollector(new LytRect(0, 0, 200, 200), null);
        pc.pushTransform(0, -150, 2.0f);

        pc.collectFrom(new TestBlock(0, 90, 10, 10));

        assertFalse(
            pc.result()
                .isEmpty(),
            "block visible under scale transform must be collected");
    }

    @Test
    void overflowingChildKeepsCollapsedParentVisible() {
        // Regression: a container whose own rect leaves the viewport must not take
        // an overflowing child (e.g. a floated graph) with it. The collector culls
        // by subtree-union bounds (LytBlock.getCullBounds).
        var pc = new PrimitiveCollector(new LytRect(0, 0, 200, 200), null);
        pc.pushTransform(0, -100, 1.0f);

        TestBlock parent = new TestBlock(0, 95, 50, 5); // own rect: screen y=-5..0, outside
        TestBlock child = new TestBlock(0, 95, 50, 150); // overflows to screen y=145, visible
        parent.append(child);
        // LytDocument.computeCullBounds computes this union after each layout pass.
        parent.setCullBounds(new LytRect(0, 95, 50, 150));

        pc.collectFrom(parent);

        assertEquals(
            2,
            count(pc.result(), GuideRenderPrimitive.FillRect.class),
            "parent and overflowing child must both be collected");
    }
}
