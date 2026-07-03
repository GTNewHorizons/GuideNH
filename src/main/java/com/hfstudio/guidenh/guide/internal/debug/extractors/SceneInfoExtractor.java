package com.hfstudio.guidenh.guide.internal.debug.extractors;

import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.HoveredElementInfo;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

public class SceneInfoExtractor implements DebugInfoExtractor {

    @Override
    public boolean canHandle(LytNode node) {
        return node instanceof LytGuidebookScene;
    }

    @Override
    public void extract(LytNode node, HoveredElementInfo info) {
        LytGuidebookScene scene = (LytGuidebookScene) node;
        info.addExtraInfo("Type: 3D Scene");

        int sceneWidth = scene.getSceneWidth();
        int sceneHeight = scene.getSceneHeight();
        info.addExtraInfo("Size: " + sceneWidth + "x" + sceneHeight);

        var annotations = scene.getAnnotations();
        if (annotations != null && !annotations.isEmpty()) {
            info.addExtraInfo("Annotations: " + annotations.size());
        }

        int childElements = scene.getChildren()
            .size();
        if (childElements > 0) {
            info.addExtraInfo("UI Children: " + childElements);
        }
    }
}
