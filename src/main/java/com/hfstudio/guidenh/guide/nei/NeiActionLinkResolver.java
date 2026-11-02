package com.hfstudio.guidenh.guide.nei;

import java.util.ArrayList;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.ActionLinkResolver;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownActionLink;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

public class NeiActionLinkResolver implements ActionLinkResolver {

    @Override
    public Set<String> getSchemes() {
        return Set.of("nei");
    }

    @Override
    public MdxJsxTextElement resolve(String label, String destination, String defaultNamespace) {
        NeiLinkTarget target = NeiLinkParser.parseUri(destination, defaultNamespace);
        MdxJsxTextElement element = new MdxJsxTextElement("NeiRecipeLink", new ArrayList<>());
        element.addAttribute("mode", target.action());
        if (target.itemId() != null) element.addAttribute("id", target.itemId());
        if (target.handlerId() != null) element.addAttribute("handlerId", target.handlerId());
        element.addChild(MarkdownActionLink.plainLabel(label));
        return element;
    }
}
