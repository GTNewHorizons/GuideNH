package com.hfstudio.guidenh.guide.scene.outline;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldOutlineAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public final class GuidebookOutlineRenderer {

    private final OutlineFrame frame = new OutlineFrame();
    private final OutlineRenderBackend boxBackend = new BoxOutlineBackend();
    @Nullable
    private OutlineRenderBackend screenSpaceBackend;
    private boolean screenSpaceFailed;

    public void render(GuidebookLevel level, Iterable<? extends InWorldAnnotation> annotations,
        @Nullable GuidebookSceneLayerSelection layers, OutlineTargetDrawer drawer, Matrix4fc viewProjection,
        float partialTicks) {
        if (!containsOutline(annotations)) {
            return;
        }
        OutlineTargetResolver.collect(level, annotations, layers, Minecraft.getSystemTime(), frame);
        try {
            if (frame.isEmpty()) {
                return;
            }
            OutlineRenderBackend screenSpace = screenSpaceBackend();
            if (screenSpace == null) {
                boxBackend.render(level, frame, drawer, viewProjection, partialTicks);
                return;
            }
            try {
                screenSpace.render(level, frame, drawer, viewProjection, partialTicks);
            } catch (RuntimeException e) {
                screenSpaceFailed = true;
                screenSpace.release();
                screenSpaceBackend = null;
                GuideDebugLog
                    .warnAlways("[GuideNH] Screen-space outlines are unavailable, using boxes: {}", e.toString());
                boxBackend.render(level, frame, drawer, viewProjection, partialTicks);
            }
        } finally {
            frame.clear();
        }
    }

    public void release() {
        if (screenSpaceBackend != null) {
            screenSpaceBackend.release();
            screenSpaceBackend = null;
        }
        frame.clear();
    }

    @Nullable
    private OutlineRenderBackend screenSpaceBackend() {
        if (screenSpaceBackend == null && !screenSpaceFailed) {
            if (ScreenSpaceOutlineBackend.isSupported()) {
                screenSpaceBackend = new ScreenSpaceOutlineBackend();
            } else {
                screenSpaceFailed = true;
            }
        }
        return screenSpaceBackend;
    }

    private static boolean containsOutline(Iterable<? extends InWorldAnnotation> annotations) {
        for (InWorldAnnotation annotation : annotations) {
            if (annotation instanceof InWorldOutlineAnnotation) {
                return true;
            }
        }
        return false;
    }
}
