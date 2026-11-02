package com.hfstudio.guidenh.guide.siteexport.site;

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
            return "<span class=\"guide-export-error\">" + escape(exception.getMessage()) + "</span>";
        }
    }

    public static String wrap(String content, String title) {
        String explanation = "Open this guide in GuideNH to view NEI recipes.";
        return "<span class=\"guide-nei-link\" data-guide-nei-link data-guide-nei-title=\"" + escape(title)
            + "\" title=\""
            + escape(title == null || title.isBlank() ? explanation : title + "\n" + explanation)
            + "\">"
            + content
            + "</span>";
    }

    private static String escape(String value) {
        return value == null ? ""
            : value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
