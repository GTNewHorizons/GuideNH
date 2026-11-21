package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.HashSet;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.nei.NeiLinkParser;
import com.hfstudio.guidenh.guide.nei.NeiLinkTarget;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class NeiRecipeLinkCompiler extends FlowTagCompiler {

    private static final Set<String> ATTRIBUTES = Set.of("id", "handlerId", "mode", "title");

    @Override
    public Set<String> getTagNames() {
        return Set.of("NeiRecipeLink");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields element) {
        try {
            NeiLinkTarget target = parseTarget(
                element,
                compiler.getPageId()
                    .getResourceDomain());
            LytFlowLink link = createLink(target);
            String title = MdxAttrs.getString(element, "title", null);
            if (title != null && !title.isBlank()) link.setTooltip(new TextTooltip(title));
            compiler.compileInlineFragment(element.children(), link);
            if (link.getChildren()
                .stream()
                .noneMatch(NeiRecipeLinkCompiler::hasVisibleContent)) {
                throw new IllegalArgumentException("NeiRecipeLink requires visible inline content.");
            }
            parent.append(link);
        } catch (IllegalArgumentException | MdxAttrs.AttributeException exception) {
            parent.appendError(compiler, exception.getMessage(), element);
        }
    }

    public static NeiLinkTarget parseTarget(MdxJsxElementFields element, String defaultNamespace) {
        Set<String> names = new HashSet<>();
        for (var attribute : element.attributes()) {
            if (!(attribute instanceof MdxJsxAttribute named) || !named.hasStringValue()
                || !ATTRIBUTES.contains(named.name)
                || !names.add(named.name)) {
                throw new IllegalArgumentException(
                    "NeiRecipeLink accepts unique id, handlerId, mode, and title attributes.");
            }
        }
        return NeiLinkParser.parseAttributes(
            MdxAttrs.getString(element, "mode", null),
            MdxAttrs.getString(element, "id", null),
            MdxAttrs.getString(element, "handlerId", null),
            defaultNamespace);
    }

    private static boolean hasVisibleContent(LytFlowContent content) {
        if (content instanceof LytFlowText text) return !text.getText()
            .isBlank();
        if (content instanceof LytFlowSpan span) {
            return span.getChildren()
                .stream()
                .anyMatch(NeiRecipeLinkCompiler::hasVisibleContent);
        }
        return content instanceof LytFlowInlineBlock;
    }

    public static LytFlowLink createLink(NeiLinkTarget target) {
        LytFlowLink link = new LytFlowLink();
        link.setStyleClass("NeiRecipeLink");
        link.setData("neiLinkTarget", target);
        return link;
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields element, IndexingSink sink) {
        indexer.indexContent(element.children(), sink);
    }
}
