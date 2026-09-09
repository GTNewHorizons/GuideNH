package com.hfstudio.guidenh.guide.document.interaction;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.MinecraftFontMetrics;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.siteexport.ExportableResourceProvider;
import com.hfstudio.guidenh.guide.siteexport.ResourceExporter;

import lombok.Getter;

public class ContentTooltip implements GuideTooltip {

    @Getter
    private final LytBlock content;

    private int lastMaxWidth = -1;
    @Getter
    private LytRect layoutBox = LytRect.empty();
    /**
     * The unnormalized bounds of all rendered tooltip content. This includes floating and inline
     * blocks that can extend beyond their paragraph's text bounds.
     */
    @Getter
    private LytRect contentBounds = LytRect.empty();

    public ContentTooltip(LytBlock content) {
        this.content = content;
        content.setDetachedLayoutInvalidator(this::invalidateLayout);
        prepareEmbeddedScenes(content);
    }

    public LytRect layout(int maxWidth) {
        if (maxWidth != lastMaxWidth) {
            var ctx = new LayoutContext(new MinecraftFontMetrics());
            LytRect rootBounds = content.layout(ctx, 0, 0, Math.max(20, maxWidth));
            contentBounds = collectContentBounds(rootBounds);
            layoutBox = new LytRect(0, 0, contentBounds.width(), contentBounds.height());
            lastMaxWidth = maxWidth;
        }
        return layoutBox;
    }

    /**
     * Invalidates cached bounds after a dynamic tooltip body changes.
     */
    public void invalidateLayout() {
        lastMaxWidth = -1;
        layoutBox = LytRect.empty();
        contentBounds = LytRect.empty();
    }

    private LytRect collectContentBounds(LytRect rootBounds) {
        LytRect[] visualBounds = { rootBounds };
        content.visit(new LytVisitor() {

            @Override
            public Result beforeNode(LytNode node) {
                if (node instanceof LytBlock block && block.getBounds() != null) {
                    visualBounds[0] = LytRect.union(visualBounds[0], block.getBounds());
                }
                return Result.CONTINUE;
            }
        });
        return visualBounds[0];
    }

    @Override
    public void exportResources(ResourceExporter exporter) {
        ExportableResourceProvider.visit(content, exporter);
    }

    public static void prepareEmbeddedScenes(LytNode node) {
        if (node == null) {
            return;
        }
        if (node instanceof LytGuidebookScene scene) {
            scene.setInteractive(false);
            scene.setSceneButtonsVisible(false);
            scene.setBottomControlsVisible(false);
        }
        for (LytNode child : node.getChildren()) {
            prepareEmbeddedScenes(child);
        }
    }
}
