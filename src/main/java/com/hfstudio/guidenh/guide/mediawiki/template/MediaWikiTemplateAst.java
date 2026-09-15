package com.hfstudio.guidenh.guide.mediawiki.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

/**
 * Copies template AST nodes so each call site gets its own tree. The JSON form written by
 * {@link MdAstNode#toJson} is the only complete representation of every node field, and
 * {@link MdAstNode#fromJson} reconstructs every node type, which makes a round-trip the reliable clone.
 */
public class MediaWikiTemplateAst {

    private MediaWikiTemplateAst() {}

    /**
     * A copy of a node, or a text node holding its text when it cannot be copied.
     *
     * <p>
     * A shared node is never returned. The copy is what keeps a template's body pristine while each call
     * site substitutes its own arguments, so returning the original would let the first call rewrite the
     * template for every later one; degrading to text keeps a node the copier does not understand from
     * corrupting the others.
     */
    public static MdAstAnyContent copy(MdAstAnyContent node) {
        if (!(node instanceof MdAstNode astNode)) {
            return text(flatten(node));
        }
        try {
            // A parent's addChild rejects children that do not match its declared element type, and an MDX
            // element declares a narrower type than the phrasing content it actually holds, so children are
            // rebuilt separately instead of being handed to readJson.
            JsonObject json = toJsonObject(astNode);
            List<? extends MdAstAnyContent> sourceChildren = childrenOf(node);
            json.add("children", new JsonArray());
            MdAstNode shell = MdAstNode.fromJson(json);
            if (!(shell instanceof MdAstAnyContent content)) {
                return text(flatten(node));
            }
            // Positions are deliberately not carried over. They point into the template page's own text, and a
            // tag that reads its body from source would then slice unsubstituted text and lose the parameter
            // expansion; leaving them unset makes such a tag use its already-expanded parsed children instead.
            if (!sourceChildren.isEmpty()) {
                List<MdAstAnyContent> copies = new ArrayList<>(sourceChildren.size());
                for (MdAstAnyContent child : sourceChildren) {
                    copies.add(copy(child));
                }
                replaceChildren(content, copies);
            }
            return content;
        } catch (IOException | RuntimeException failed) {
            MediaWikiTemplateDiagnostics.reportCopyFailure(node.type(), failed);
            return text(flatten(node));
        }
    }

    private static JsonObject toJsonObject(MdAstNode node) throws IOException {
        StringWriter buffer = new StringWriter();
        try (JsonWriter jsonWriter = new JsonWriter(buffer)) {
            node.toJson(jsonWriter);
        }
        return new JsonParser().parse(buffer.toString())
            .getAsJsonObject();
    }

    public static MdAstText text(String value) {
        MdAstText node = new MdAstText();
        node.value = value != null ? value : "";
        return node;
    }

    public static String flatten(@Nullable MdAstAnyContent node) {
        if (node == null) {
            return "";
        }
        if (node instanceof MdAstText textNode) {
            return textNode.value;
        }
        return node instanceof MdAstNode astNode ? astNode.toText() : "";
    }

    public static boolean isBlank(@Nullable MdAstAnyContent node) {
        return flatten(node).trim()
            .isEmpty();
    }

    public static List<? extends MdAstAnyContent> childrenOf(@Nullable MdAstAnyContent node) {
        return node instanceof MdAstParent<?>parent ? parent.children() : List.of();
    }

    /**
     * Replaces a node's children in place. A parent's declared element type is narrower than the nodes a
     * template may place there, so the list is reached through its own mutable view.
     */
    @SuppressWarnings("unchecked")
    public static void replaceChildren(MdAstAnyContent node, List<MdAstAnyContent> children) {
        if (!(node instanceof MdAstParent<?>parent)) {
            return;
        }
        List<MdAstAnyContent> live = (List<MdAstAnyContent>) parent.children();
        live.clear();
        live.addAll(children);
    }
}
