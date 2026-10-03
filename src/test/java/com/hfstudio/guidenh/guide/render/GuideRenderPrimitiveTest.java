package com.hfstudio.guidenh.guide.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Contract tests for {@link GuideRenderPrimitive} record shapes. */
class GuideRenderPrimitiveTest {

    @Test
    void blitTextureNineArgConstructorDefaultsToWhiteTint() {
        var bt = new GuideRenderPrimitive.BlitTexture(1, 2, 3, 4, 5, 0f, 0f, 1f, 1f);
        assertEquals(0xFFFFFFFF, bt.argb(), "9-arg form must default to untinted (white)");
    }

    @Test
    void blitTextureTintIsStored() {
        var bt = new GuideRenderPrimitive.BlitTexture(1, 2, 3, 4, 5, 0f, 0f, 1f, 1f, 0x80FF0000);
        assertEquals(0x80FF0000, bt.argb());
    }
}
