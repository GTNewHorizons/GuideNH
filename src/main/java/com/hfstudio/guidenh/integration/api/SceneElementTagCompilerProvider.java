package com.hfstudio.guidenh.integration.api;

import java.util.List;

import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;

public interface SceneElementTagCompilerProvider {

    void appendSceneElementTagCompilers(List<SceneElementTagCompiler> compilers);
}
