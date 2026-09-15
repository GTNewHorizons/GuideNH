package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * Applies the include-control tags when a template is transcluded. The tags are ordinary MDX elements, so
 * their bodies are already parsed and this only chooses which bodies survive.
 *
 * <p>
 * {@code <NoInclude>} is documentation that belongs on the template page, {@code <IncludeOnly>} is output
 * that belongs only where the template is called, and {@code <OnlyInclude>} narrows transclusion to its own
 * body when a template contains one.
 */
public class MediaWikiIncludeControl {

    public static final String NO_INCLUDE = "NoInclude";
    public static final String INCLUDE_ONLY = "IncludeOnly";
    public static final String ONLY_INCLUDE = "OnlyInclude";

    private MediaWikiIncludeControl() {}

    public static List<MdAstAnyContent> filterForTransclusion(List<MdAstAnyContent> body) {
        List<MdAstAnyContent> onlyInclude = collectOnlyInclude(body);
        if (onlyInclude != null) {
            return onlyInclude;
        }
        return filterNodes(body);
    }

    private static List<MdAstAnyContent> filterNodes(List<MdAstAnyContent> body) {
        List<MdAstAnyContent> result = new ArrayList<>(body.size());
        for (MdAstAnyContent node : body) {
            if (!(node instanceof MdxJsxElementFields element)) {
                result.add(node);
                continue;
            }
            String tag = element.name();
            if (NO_INCLUDE.equals(tag)) {
                continue;
            }
            if (INCLUDE_ONLY.equals(tag)) {
                result.addAll(MediaWikiTemplateValue.copyNodes(element.children()));
                continue;
            }
            // Any other tag keeps itself but has its own children filtered, so a control tag nested inside
            // an ordinary tag still works.
            MediaWikiTemplateAst.replaceChildren(node, filterNodes(new ArrayList<>(element.children())));
            result.add(node);
        }
        return result;
    }

    private static List<MdAstAnyContent> collectOnlyInclude(List<MdAstAnyContent> body) {
        List<MdAstAnyContent> collected = null;
        for (MdAstAnyContent node : body) {
            if (!(node instanceof MdxJsxElementFields element)) {
                continue;
            }
            if (ONLY_INCLUDE.equals(element.name())) {
                if (collected == null) {
                    collected = new ArrayList<>();
                }
                collected.addAll(MediaWikiTemplateValue.copyNodes(element.children()));
                continue;
            }
            List<MdAstAnyContent> nested = collectOnlyInclude(new ArrayList<>(element.children()));
            if (nested != null) {
                if (collected == null) {
                    collected = new ArrayList<>();
                }
                collected.addAll(nested);
            }
        }
        return collected;
    }
}
