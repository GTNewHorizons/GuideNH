package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class FluidImageCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("FluidImage");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields element) {
        try {
            parent.append(LytFlowInlineBlock.of(new FluidImagePlaceholder(FluidImageOptions.read(element))));
        } catch (IllegalArgumentException exception) {
            parent.appendError(compiler, exception.getMessage(), element);
        }
    }

    public static class FluidImagePlaceholder extends LytParagraph {

        private final FluidImageOptions options;

        public FluidImagePlaceholder(FluidImageOptions options) {
            this.options = options;
            setStyleClass("FluidImage");
        }

        public FluidImageOptions getOptions() {
            return options;
        }
    }
}
