package com.hfstudio.guidenh.guide.internal.binding;

import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.FlowTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BindingLinkCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("BindingLink", "ResearchLink");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields element) {
        try {
            var span = parent.appendText("");
            span.setStyleClass("Binding");
            span.setData("bindingSpec", BindingTagParser.parse(element));
        } catch (IllegalArgumentException | MdxAttrs.AttributeException exception) {
            parent.appendError(compiler, exception.getMessage(), element);
        }
    }
}
