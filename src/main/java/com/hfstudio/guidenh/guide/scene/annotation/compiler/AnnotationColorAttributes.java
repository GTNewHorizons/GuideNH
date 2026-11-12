package com.hfstudio.guidenh.guide.scene.annotation.compiler;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.annotation.AnnotationColorStyle;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/** Parses the common animated border color contract. */
public class AnnotationColorAttributes {

    private AnnotationColorAttributes() {}

    public static AnnotationColorStyle parse(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        var mode = MdxAttrs.getEnum(compiler, errorSink, el, "colorMode", AnnotationColorStyle.Mode.SOLID);
        float period = MdxAttrs.getFloat(compiler, errorSink, el, "colorPeriod", AnnotationColorStyle.DEFAULT_PERIOD);
        try {
            return new AnnotationColorStyle(mode, period);
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return AnnotationColorStyle.SOLID;
        }
    }
}
