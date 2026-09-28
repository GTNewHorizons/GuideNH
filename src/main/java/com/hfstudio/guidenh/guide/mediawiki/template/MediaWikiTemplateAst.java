package com.hfstudio.guidenh.guide.mediawiki.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

/** Copies template AST nodes so each call site can safely substitute its own arguments. */
public final class MediaWikiTemplateAst {

    private static final Map<MdAstNode, JsonObject> SHELL_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private MediaWikiTemplateAst() {}

    public static MdAstAnyContent copy(MdAstAnyContent node) {
        MdAstAnyContent content = copyShell(node);
        if (content == null) {
            return text(flatten(node));
        }
        List<? extends MdAstAnyContent> sourceChildren = childrenOf(node);
        if (!sourceChildren.isEmpty()) {
            List<MdAstAnyContent> copies = new ArrayList<>(sourceChildren.size());
            for (MdAstAnyContent child : sourceChildren) {
                copies.add(copy(child));
            }
            replaceChildren(content, copies);
        }
        return content;
    }

    /**
     * Creates a node without children, for callers which provide a filtered child list themselves. Source
     * positions intentionally remain absent: they point at the template file, not its expanded call site.
     */
    static @Nullable MdAstAnyContent copyShell(MdAstAnyContent node) {
        if (!(node instanceof MdAstNode astNode)) {
            return null;
        }
        try {
            MdAstNode shell = MdAstNode.fromJson(shellJson(astNode));
            return shell instanceof MdAstAnyContent content ? content : null;
        } catch (IOException | RuntimeException failed) {
            MediaWikiTemplateDiagnostics.reportCopyFailure(node.type(), failed);
            return null;
        }
    }

    private static JsonObject shellJson(MdAstNode node) throws IOException {
        synchronized (SHELL_CACHE) {
            JsonObject cached = SHELL_CACHE.get(node);
            if (cached != null) {
                return cached;
            }
            StringWriter buffer = new StringWriter();
            try (JsonWriter jsonWriter = new JsonWriter(buffer)) {
                node.toJson(jsonWriter);
            }
            JsonObject tree = new JsonParser().parse(buffer.toString())
                .getAsJsonObject();
            indexShells(node, tree);
            return SHELL_CACHE.get(node);
        }
    }

    private static void indexShells(MdAstNode node, JsonObject tree) {
        JsonArray childJson = tree.has("children") ? tree.getAsJsonArray("children") : null;
        List<? extends MdAstAnyContent> children = childrenOf((MdAstAnyContent) node);
        if (childJson != null && childJson.size() == children.size()) {
            for (int index = 0; index < children.size(); index++) {
                if (children.get(index) instanceof MdAstNode child && childJson.get(index)
                    .isJsonObject()) {
                    indexShells(
                        child,
                        childJson.get(index)
                            .getAsJsonObject());
                }
            }
        }
        tree.remove("position");
        tree.add("children", new JsonArray());
        SHELL_CACHE.put(node, tree);
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
