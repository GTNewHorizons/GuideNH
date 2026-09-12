package com.hfstudio.guidenh.integration.api;

import java.util.List;

import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;

/**
 * Lets a mod add scene element compilers to every guide: the global counterpart of
 * {@code GuideBuilder.extension(SceneElementTagCompiler.EXTENSION_POINT, compiler)}. Register through
 * {@code GuideNhIntegrationRegistry.registerSceneElementTagCompilerProvider(provider)}.
 */
public interface SceneElementTagCompilerProvider {

    void appendSceneElementTagCompilers(List<SceneElementTagCompiler> compilers);
}
