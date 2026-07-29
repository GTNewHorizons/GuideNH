package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * {@code guideme.scene.element.IsometricCameraElementCompiler}。
 */
public class IsometricCameraElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("IsometricCamera");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        boolean hasYaw = el.getAttribute("yaw") != null;
        boolean hasPitch = el.getAttribute("pitch") != null;
        boolean hasRoll = el.getAttribute("roll") != null;
        if (!hasYaw && !hasPitch && !hasRoll) {
            // No explicit attributes: keep whatever the camera already has (preset or previous config).
            return;
        }
        float yaw = hasYaw ? MdxAttrs.getFloat(compiler, errorSink, el, "yaw", 0.0f) : camera.getRotationY();
        float pitch = hasPitch ? MdxAttrs.getFloat(compiler, errorSink, el, "pitch", 0.0f) : camera.getRotationX();
        float roll = hasRoll ? MdxAttrs.getFloat(compiler, errorSink, el, "roll", 0.0f) : camera.getRotationZ();
        camera.setIsometricYawPitchRoll(yaw, pitch, roll);
    }
}
