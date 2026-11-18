package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFluidLink;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class FluidLinkCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("FluidLink");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields element) {
        try {
            LytFluidLink link = new LytFluidLink(FluidTagAttributes.readFluid(element));
            link.setStyleClass("FluidLink");
            link.setData("showTooltip", FluidTagAttributes.showTooltip(element));
            link.setData("showText", MdxAttrs.getBoolean(element, "showText", true));
            link.setData(
                "showIcon",
                ItemImageCompiler.resolveLabelPosition(element.getAttributeString("showIcon", null)));
            link.setData("scale", FluidTagAttributes.scale(element));
            link.setData("linksTo", element.getAttributeString("linksTo", null));
            link.setData(
                "guideId",
                compiler.getGuideId()
                    .toString());
            link.setData(
                "pageId",
                compiler.getPageId()
                    .toString());
            compiler.compileFlowContext(element.children(), link);
            parent.append(link);
        } catch (IllegalArgumentException exception) {
            parent.appendError(compiler, exception.getMessage(), element);
        }
    }
}
