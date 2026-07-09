package com.hfstudio.guidenh.guide.scene.cache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureCache;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneNodeModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneNodeType;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxExpressionAttribute;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public class GuideSceneStructureFingerprintResolver {

    public static final Comparator<String> NULL_SAFE_STRING_COMPARATOR = Comparator.nullsFirst(String::compareTo);

    @Nullable
    public GuideSceneStructureCacheKey buildForGameScene(PageCompiler compiler,
        List<? extends MdAstAnyContent> children) {
        GuideSceneStructureFingerprintBuilder builder = new GuideSceneStructureFingerprintBuilder();
        int structuralIndex = 0;
        for (MdAstAnyContent child : children) {
            MdxJsxElementFields element = unwrapSceneElement(child);
            if (element == null || !isStructuralSceneElement(element.name())) {
                continue;
            }
            appendGameSceneElement(builder, compiler, element, structuralIndex++);
        }
        return builder.isEmpty() ? null : builder.build();
    }

    @Nullable
    public GuideSceneStructureCacheKey buildForPreview(SceneEditorSession session, Path workingRoot) {
        SceneEditorSceneModel model = session.getSceneModel();
        GuideSceneStructureFingerprintBuilder builder = new GuideSceneStructureFingerprintBuilder();
        if (model.getSceneNodes()
            .isEmpty()) {
            appendLegacyPreviewStructure(builder, session, workingRoot, model);
            return builder.isEmpty() ? null : builder.build();
        }
        int structuralIndex = 0;
        for (SceneEditorSceneNodeModel node : model.getSceneNodes()) {
            if (!isStructuralPreviewNode(node.getType())) {
                continue;
            }
            appendPreviewNode(builder, session, workingRoot, node, structuralIndex++);
        }
        return builder.isEmpty() ? null : builder.build();
    }

    public boolean isStructuralSceneElement(@Nullable String name) {
        return "Block".equals(name) || "Entity".equals(name)
            || "ImportStructure".equals(name)
            || "PlaceBlock".equals(name)
            || "RemoveBlocks".equals(name)
            || "ReplaceBlock".equals(name);
    }

    public boolean isStructuralPreviewNode(SceneEditorSceneNodeType type) {
        return type == SceneEditorSceneNodeType.IMPORT_STRUCTURE || type == SceneEditorSceneNodeType.REMOVE_BLOCKS
            || type == SceneEditorSceneNodeType.OPAQUE;
    }

    @Nullable
    private static MdxJsxElementFields unwrapSceneElement(UnistNode node) {
        return SceneTagCompiler.unwrapSceneElement(node);
    }

    private void appendGameSceneElement(GuideSceneStructureFingerprintBuilder builder, PageCompiler compiler,
        MdxJsxElementFields element, int structuralIndex) {
        String name = element.name();
        if (name == null) {
            return;
        }
        String prefix = structuralIndex + ":" + name;
        appendAttributes(builder, prefix, element.attributes());
        if ("ImportStructure".equals(name)) {
            appendImportedStructureAsset(builder, prefix, compiler, element.getAttributeString("src", null));
        }
    }

    private void appendLegacyPreviewStructure(GuideSceneStructureFingerprintBuilder builder, SceneEditorSession session,
        Path workingRoot, SceneEditorSceneModel model) {
        String structureText = resolvePreviewStructureText(session, workingRoot, model.getStructureSource());
        if (structureText != null) {
            builder.addHashedText("legacy:structure", structureText);
        }
    }

    private void appendPreviewNode(GuideSceneStructureFingerprintBuilder builder, SceneEditorSession session,
        Path workingRoot, SceneEditorSceneNodeModel node, int structuralIndex) {
        String prefix = structuralIndex + ":"
            + node.getType()
                .name();
        if (node.getType() == SceneEditorSceneNodeType.OPAQUE) {
            String opaqueText = node.getOpaqueText();
            if (opaqueText != null && !opaqueText.isEmpty()) {
                builder.addHashedText(prefix + ":opaque", opaqueText);
            }
            return;
        }
        appendAttributes(builder, prefix, node.getType(), node.getAttributes());
        if (node.getType() == SceneEditorSceneNodeType.IMPORT_STRUCTURE) {
            appendPreviewStructureText(builder, prefix, session, workingRoot, node.getAttribute("src"));
        }
    }

    private void appendImportedStructureAsset(GuideSceneStructureFingerprintBuilder builder, String prefix,
        PageCompiler compiler, @Nullable String src) {
        if (src == null || src.trim()
            .isEmpty()) {
            builder.add(prefix + ":structure:missing", "missing-src");
            return;
        }
        builder.add(prefix + ":structure:src", src.trim());
        try {
            byte[] data = compiler.loadAsset(IdUtils.resolveLink(src, compiler.getPageId()));
            if (data != null) {
                builder.addHashedBytes(prefix + ":structure:bytes", data);
            } else {
                builder.add(prefix + ":structure:bytes", "missing-asset");
            }
        } catch (Exception e) {
            builder.add(
                prefix + ":structure:error",
                e.getClass()
                    .getName() + ":"
                    + e.getMessage());
        }
    }

    private void appendPreviewStructureText(GuideSceneStructureFingerprintBuilder builder, String prefix,
        SceneEditorSession session, Path workingRoot, @Nullable String structureSource) {
        String structureText = resolvePreviewStructureText(session, workingRoot, structureSource);
        if (structureText != null) {
            builder.addHashedText(prefix + ":structure:text", structureText);
        } else if (structureSource != null && !structureSource.trim()
            .isEmpty()) {
                builder.add(prefix + ":structure:src", structureSource.trim());
            }
    }

    private void appendAttributes(GuideSceneStructureFingerprintBuilder builder, String prefix,
        List<MdxJsxAttributeNode> attributes) {
        List<String> rendered = new ArrayList<>();
        for (MdxJsxAttributeNode attributeNode : attributes) {
            rendered.add(renderAttribute(attributeNode));
        }
        rendered.sort(NULL_SAFE_STRING_COMPARATOR);
        for (int i = 0; i < rendered.size(); i++) {
            builder.add(prefix + ":attr:" + i, rendered.get(i));
        }
    }

    private void appendAttributes(GuideSceneStructureFingerprintBuilder builder, String prefix,
        SceneEditorSceneNodeType nodeType, Map<String, String> attributes) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(attributes.entrySet());
        entries.sort(Map.Entry.comparingByKey(NULL_SAFE_STRING_COMPARATOR));
        for (Map.Entry<String, String> entry : entries) {
            builder.add(prefix + ":attr:" + entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private String renderAttribute(MdxJsxAttributeNode attributeNode) {
        if (attributeNode instanceof MdxJsxAttribute attribute) {
            if (attribute.hasStringValue()) {
                return attribute.name + "=\"" + attribute.getStringValue() + "\"";
            }
            if (attribute.hasExpressionValue()) {
                return attribute.name + "={" + attribute.getExpressionValue() + "}";
            }
            return attribute.name;
        }
        if (attributeNode instanceof MdxJsxExpressionAttribute expressionAttribute) {
            return "..." + expressionAttribute.value;
        }
        return attributeNode.type();
    }

    @Nullable
    private String resolvePreviewStructureText(SceneEditorSession session, Path workingRoot,
        @Nullable String structureSource) {
        String normalizedSource = normalize(structureSource);
        String importedStructureSnbt = session.getImportedStructureSnbt();
        if (importedStructureSnbt != null && !importedStructureSnbt.trim()
            .isEmpty()) {
            String modelStructureSource = normalize(
                session.getSceneModel()
                    .getStructureSource());
            if (modelStructureSource == null || modelStructureSource.equals(normalizedSource)) {
                return importedStructureSnbt;
            }
        }
        if (normalizedSource == null) {
            return null;
        }
        try {
            Path path = SceneEditorStructureCache.resolveSceneStructurePath(workingRoot, normalizedSource)
                .orElse(null);
            if (path == null || !Files.exists(path)) {
                return null;
            }
            return Files.readString(path);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
