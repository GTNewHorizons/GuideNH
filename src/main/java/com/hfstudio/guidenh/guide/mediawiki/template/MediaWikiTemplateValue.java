package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

/**
 * One argument value. Rich values keep their MDX nodes so an argument may itself be a tag, and a plain
 * value keeps its text so it can be substituted into an attribute, which cannot hold block content.
 */
public final class MediaWikiTemplateValue {

    private final List<MdAstAnyContent> nodes;
    private final String text;
    private final boolean rich;

    private MediaWikiTemplateValue(List<MdAstAnyContent> nodes, String text, boolean rich) {
        this.nodes = nodes;
        this.text = text;
        this.rich = rich;
    }

    public static MediaWikiTemplateValue ofText(String text) {
        String safe = text != null ? text : "";
        MdAstText node = new MdAstText();
        node.value = safe;
        return new MediaWikiTemplateValue(List.of(node), safe, false);
    }

    public static MediaWikiTemplateValue ofNodes(List<? extends MdAstAnyContent> nodes) {
        StringBuilder builder = new StringBuilder();
        for (MdAstAnyContent node : nodes) {
            if (node instanceof MdAstText textNode) {
                builder.append(textNode.value);
            } else {
                builder.append(node instanceof MdAstNode astNode ? astNode.toText() : "");
            }
        }
        return new MediaWikiTemplateValue(List.copyOf(nodes), builder.toString(), true);
    }

    public List<MdAstAnyContent> nodes() {
        return nodes;
    }

    public String text() {
        return text;
    }

    public boolean isRich() {
        return rich;
    }

    public boolean isBlank() {
        return text.trim()
            .isEmpty();
    }

    public MediaWikiTemplateValue copy() {
        return rich ? new MediaWikiTemplateValue(copyNodes(nodes), text, true) : ofText(text);
    }

    static List<MdAstAnyContent> copyNodes(List<? extends MdAstAnyContent> source) {
        List<MdAstAnyContent> copies = new ArrayList<>(source.size());
        for (MdAstAnyContent node : source) {
            copies.add(MediaWikiTemplateAst.copy(node));
        }
        return List.copyOf(copies);
    }
}
