package com.hfstudio.guidenh.guide.document.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class LytDocumentLayoutTest {

    @Test
    public void floatingVisualsResizeWhenScaleChangesAtTheSameDocumentWidth() {
        var texture = new ResourceLocation("guidenh", "layout-test.png");
        var image = new LytImage();
        image.setTexture(texture, new GuidePageTexture(texture, 200, 100));
        image.setDisplaySize(200, 100);
        var document = new LytDocument();
        document.append(new LytDocumentFloat(image, true));
        document.append(LytParagraph.of("Text alongside the floating visual. ".repeat(20)));
        var metrics = new FontMetrics() {

            @Override
            public float getAdvance(int codePoint, ResolvedTextStyle style) {
                return 6;
            }

            @Override
            public int getLineHeight(ResolvedTextStyle style) {
                return 10;
            }
        };
        document.updateLayout(new LayoutContext(metrics).withVisualScale(0.5f), 400);
        assertEquals(
            100,
            image.getBounds()
                .width());
        document.updateLayout(new LayoutContext(metrics), 400);
        assertEquals(
            200,
            image.getBounds()
                .width());
    }
}
