package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeAttribute;
import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeHtml;

import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.compiler.tags.NeiRecipeLinkCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class GuideSiteNeiLinkRenderer {

    private GuideSiteNeiLinkRenderer() {}

    public static String render(MdxJsxElementFields element, String defaultNamespace, String content) {
        try {
            NeiRecipeLinkCompiler.parseTarget(element, defaultNamespace);
            return wrap(content, MdxAttrs.getString(element, "title", ""));
        } catch (IllegalArgumentException | MdxAttrs.AttributeException exception) {
            return "<span class=\"guide-export-error\">" + escapeHtml(exception.getMessage()) + "</span>";
        }
    }

    public static String wrap(String content, String title) {
        String explanation = "Open this guide in GuideNH to view NEI recipes.";
        return "<span class=\"guide-nei-link\" data-guide-nei-link data-guide-nei-title=\"" + escapeAttribute(title)
            + "\" title=\""
            + escapeAttribute(title == null || title.isBlank() ? explanation : title + "\n" + explanation)
            + "\">"
            + content
            + "</span>";
    }

}
