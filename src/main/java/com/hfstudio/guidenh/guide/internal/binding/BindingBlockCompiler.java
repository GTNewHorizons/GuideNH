package com.hfstudio.guidenh.guide.internal.binding;

import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BindingBlockCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("BindingCard", "ResearchCard", "BindingIcon", "ResearchIcon");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields element) {
        try {
            parent.append(
                new BindingPlaceholder(
                    BindingTagParser.parse(element),
                    element.name()
                        .endsWith("Icon") ? "icon" : "card"));
        } catch (IllegalArgumentException | MdxAttrs.AttributeException exception) {
            parent.appendError(compiler, exception.getMessage(), element);
        }
    }
}
