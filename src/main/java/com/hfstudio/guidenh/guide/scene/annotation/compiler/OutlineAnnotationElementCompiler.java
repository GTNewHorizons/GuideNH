package com.hfstudio.guidenh.guide.scene.annotation.compiler;

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldOutlineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.outline.OutlineMode;
import com.hfstudio.guidenh.guide.scene.outline.OutlineRegion;
import com.hfstudio.guidenh.guide.scene.outline.OutlineTargetScope;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * {@code <OutlineAnnotation pos="x y z" | min="x y z" max="x y z" entity="sceneEntityId" target="blocks"
 * mode="auto" color="#AARRGGBB" thickness="1" pulse alwaysOnTop />}.
 */
public class OutlineAnnotationElementCompiler extends AnnotationTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("OutlineAnnotation");
    }

    @Override
    @Nullable
    protected SceneAnnotation createAnnotation(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        String sceneEntityId = trimToNull(MdxAttrs.getString(compiler, errorSink, el, "entity", null));
        OutlineTargetScope scope = MdxAttrs.getEnum(compiler, errorSink, el, "target", OutlineTargetScope.BLOCKS);
        OutlineRegion region = parseRegion(compiler, errorSink, el, sceneEntityId == null && scope.isBlocks());

        ColorValue color = el.hasAttribute("color") ? MdxAttrs.getColor(compiler, errorSink, el, "color", null) : null;
        float thickness = MdxAttrs
            .getFloat(compiler, errorSink, el, "thickness", InWorldOutlineAnnotation.DEFAULT_THICKNESS);
        OutlineMode mode = MdxAttrs.getEnum(compiler, errorSink, el, "mode", OutlineMode.AUTO);
        boolean pulse = MdxAttrs.getBoolean(compiler, errorSink, el, "pulse", false);
        float pulsePeriod = MdxAttrs
            .getFloat(compiler, errorSink, el, "pulsePeriod", InWorldOutlineAnnotation.DEFAULT_PULSE_PERIOD);

        var annotation = new InWorldOutlineAnnotation(
            region,
            sceneEntityId,
            scope,
            color,
            thickness,
            mode,
            pulse,
            pulsePeriod);
        annotation.setAlwaysOnTop(MdxAttrs.getBoolean(compiler, errorSink, el, "alwaysOnTop", false));
        return annotation;
    }

    @Nullable
    private static OutlineRegion parseRegion(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        boolean defaultToOrigin) {
        if (el.hasAttribute("min") || el.hasAttribute("max")) {
            Vector3f min = MdxAttrs.getVector3(compiler, errorSink, el, "min", new Vector3f());
            Vector3f max = MdxAttrs.getVector3(compiler, errorSink, el, "max", new Vector3f(min));
            return OutlineRegion.of(floor(min.x), floor(min.y), floor(min.z), floor(max.x), floor(max.y), floor(max.z));
        }
        if (el.hasAttribute("pos") || defaultToOrigin) {
            Vector3f pos = MdxAttrs.getVector3(compiler, errorSink, el, "pos", new Vector3f());
            return OutlineRegion.single(floor(pos.x), floor(pos.y), floor(pos.z));
        }
        return null;
    }

    private static int floor(float value) {
        return (int) Math.floor(value);
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
