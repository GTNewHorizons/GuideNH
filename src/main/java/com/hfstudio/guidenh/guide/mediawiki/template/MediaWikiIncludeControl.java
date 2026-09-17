package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/** Applies transclusion-only include-control tags. */
public final class MediaWikiIncludeControl {

    public static final String NO_INCLUDE = "NoInclude";
    public static final String INCLUDE_ONLY = "IncludeOnly";
    public static final String ONLY_INCLUDE = "OnlyInclude";

    private MediaWikiIncludeControl() {}

    /** Filters and clones in one pass, avoiding a discarded deep copy of excluded nodes. */
    public static List<MdAstAnyContent> copyForTransclusion(List<? extends MdAstAnyContent> body) {
        List<? extends MdAstAnyContent> selected = findOnlyInclude(body);
        if (selected != null) {
            // Keep the established OnlyInclude semantics: once it selects a body, nested include-control
            // tags are content, not another filtering pass.
            return copySelectedNodes(selected);
        }
        return copyFilteredNodes(body, true);
    }

    private static List<MdAstAnyContent> copySelectedNodes(List<? extends MdAstAnyContent> nodes) {
        List<MdAstAnyContent> copied = new ArrayList<>(nodes.size());
        for (MdAstAnyContent node : nodes) {
            copied.add(MediaWikiTemplateAst.copy(node));
        }
        return copied;
    }

    private static List<MdAstAnyContent> copyFilteredNodes(List<? extends MdAstAnyContent> nodes,
        boolean skipFrontmatter) {
        List<MdAstAnyContent> copied = new ArrayList<>(nodes.size());
        for (MdAstAnyContent node : nodes) {
            if (skipFrontmatter && node instanceof MdAstYamlFrontmatter) {
                continue;
            }
            if (node instanceof MdxJsxElementFields element) {
                String tag = element.name();
                if (NO_INCLUDE.equals(tag)) {
                    continue;
                }
                if (INCLUDE_ONLY.equals(tag)) {
                    copied.addAll(copySelectedNodes(element.children()));
                    continue;
                }
                MdAstAnyContent shell = MediaWikiTemplateAst.copyShell(node);
                if (shell == null) {
                    copied.add(MediaWikiTemplateAst.text(MediaWikiTemplateAst.flatten(node)));
                    continue;
                }
                MediaWikiTemplateAst.replaceChildren(shell, copyFilteredNodes(element.children(), false));
                copied.add(shell);
            } else {
                copied.add(MediaWikiTemplateAst.copy(node));
            }
        }
        return copied;
    }

    private static List<? extends MdAstAnyContent> findOnlyInclude(List<? extends MdAstAnyContent> body) {
        List<MdAstAnyContent> collected = null;
        for (MdAstAnyContent node : body) {
            if (!(node instanceof MdxJsxElementFields element)) {
                continue;
            }
            if (ONLY_INCLUDE.equals(element.name())) {
                if (collected == null) {
                    collected = new ArrayList<>();
                }
                collected.addAll(element.children());
                continue;
            }
            List<? extends MdAstAnyContent> nested = findOnlyInclude(element.children());
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
