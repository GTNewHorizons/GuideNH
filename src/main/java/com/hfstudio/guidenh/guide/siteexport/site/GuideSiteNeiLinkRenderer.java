package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeAttribute;
import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeHtml;

import java.util.regex.Pattern;

import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.compiler.tags.NeiRecipeLinkCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class GuideSiteNeiLinkRenderer {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern VISUAL_ELEMENT = Pattern.compile("<(?:img|svg)\\b", Pattern.CASE_INSENSITIVE);

    private GuideSiteNeiLinkRenderer() {}

    public static String render(MdxJsxElementFields element, String defaultNamespace, String content) {
        try {
            NeiRecipeLinkCompiler.parseTarget(element, defaultNamespace);
            if (HTML_TAG.matcher(content)
                .replaceAll("")
                .isBlank()
                && !VISUAL_ELEMENT.matcher(content)
                    .find()) {
                throw new IllegalArgumentException("NeiRecipeLink requires visible inline content.");
            }
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
