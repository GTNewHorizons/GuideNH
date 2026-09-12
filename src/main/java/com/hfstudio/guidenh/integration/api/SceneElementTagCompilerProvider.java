package com.hfstudio.guidenh.integration.api;

import java.util.List;

import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;

/** Lets a mod add scene element compilers to every guide, rather than to one guide. */
public interface SceneElementTagCompilerProvider {

    void appendSceneElementTagCompilers(List<SceneElementTagCompiler> compilers);
}
