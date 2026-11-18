package com.hfstudio.guidenh.guide.internal.editor;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.GuideItemReferenceResolver;
import com.hfstudio.guidenh.guide.compiler.GuideItemReferenceResolver.ResolvedBlockReference;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneNodeModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneNodeType;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.element.BlockElementCompiler;

/**
 * One instance per editor retains the last imported structure's parsed BlockImage data.
 */
public class SceneEditorBlockImageExporter {

    @Nullable
    private String cachedImportedBlockImageSource;
    @Nullable
    private BlockImageExportData cachedImportedBlockImage;

    @Nullable
    public String createText(SceneEditorSceneModel model, @Nullable String importedStructure) {
        String id;
        String ore = null;
        String meta;
        String nbt;
        if (model.getSceneNodes()
            .size() == 1
            && model.getSceneNodes()
                .get(0)
                .getType() == SceneEditorSceneNodeType.BLOCK) {
            SceneEditorSceneNodeModel node = model.getSceneNodes()
                .get(0);
            id = node.getAttribute("id");
            ore = node.getAttribute("ore");
            if ((id == null || id.isEmpty()) && (ore == null || ore.isEmpty())) {
                return null;
            }
            meta = resolveBlockImageMeta(node, id, ore);
            nbt = node.getAttribute("nbt");
        } else {
            BlockImageExportData importedBlock = resolveImportedBlockImage(importedStructure);
            if (importedBlock == null) {
                return null;
            }
            id = importedBlock.id();
            meta = Integer.toString(importedBlock.meta());
            nbt = importedBlock.nbt();
        }

        StringBuilder builder = new StringBuilder("<BlockImage");
        appendBlockImageAttribute(builder, "id", id, false);
        appendBlockImageAttribute(builder, "ore", ore, false);
        appendBlockImageAttribute(builder, "meta", meta, false);
        appendBlockImageAttribute(builder, "nbt", nbt, true);
        appendBlockImageAttribute(builder, "perspective", model.getPerspectivePreset(), false);
        if (model.hasExplicitZoom()) {
            builder.append(" scale={")
                .append(model.getZoom())
                .append('}');
        }
        return builder.append(" />")
            .toString();
    }

    @Nullable
    private String resolveBlockImageMeta(SceneEditorSceneNodeModel node, @Nullable String id, @Nullable String ore) {
        String explicitMeta = node.getAttribute("meta");
        if (explicitMeta != null || node.getAttribute("facing") == null) {
            return explicitMeta;
        }
        ResolvedBlockReference reference = GuideItemReferenceResolver.resolveBlockReference("minecraft", id, ore);
        return reference != null
            ? Integer.toString(BlockElementCompiler.resolvePlacementMeta(reference, node.getAttribute("facing")))
            : null;
    }

    private void appendBlockImageAttribute(StringBuilder builder, String name, @Nullable String value,
        boolean singleQuoted) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (singleQuoted) {
            builder.append(' ')
                .append(name)
                .append("='")
                .append(value.replace("'", "\\'"))
                .append('\'');
            return;
        }
        builder.append(' ')
            .append(name)
            .append("=\"")
            .append(
                value.replace("&", "&amp;")
                    .replace("\"", "&quot;"))
            .append('"');
    }

    public boolean canExport(SceneEditorSceneModel model, @Nullable String importedStructure) {
        return model.getSceneNodes()
            .size() == 1
            && model.getSceneNodes()
                .get(0)
                .getType() == SceneEditorSceneNodeType.BLOCK
            || resolveImportedBlockImage(importedStructure) != null;
    }

    @Nullable
    private BlockImageExportData resolveImportedBlockImage(@Nullable String source) {
        if (Objects.equals(source, cachedImportedBlockImageSource)) {
            return cachedImportedBlockImage;
        }
        cachedImportedBlockImageSource = source;
        cachedImportedBlockImage = parseImportedBlockImage(source);
        return cachedImportedBlockImage;
    }

    @Nullable
    private BlockImageExportData parseImportedBlockImage(@Nullable String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            NBTTagCompound root = GuideTextNbtCodec.readStructureNbt(source.getBytes(StandardCharsets.UTF_8));
            NBTTagList palette = root.getTagList("palette", 10);
            NBTTagList blocks = root.getTagList("blocks", 10);
            if (blocks.tagCount() != 1) {
                return null;
            }
            NBTTagCompound blockTag = blocks.getCompoundTagAt(0);
            int state = blockTag.getInteger("state");
            if (state < 0 || state >= palette.tagCount()) {
                return null;
            }
            String id = palette.getCompoundTagAt(state)
                .getString("Name");
            if (id.isEmpty() || "minecraft:air".equals(id)) {
                return null;
            }
            String nbt = null;
            if (blockTag.hasKey("nbt", 10)) {
                nbt = GuideTextNbtCodec.writeTextSafeCompound(blockTag.getCompoundTag("nbt"));
            }
            return new BlockImageExportData(id, blockTag.getInteger("meta"), nbt);
        } catch (Exception ignored) {
            return null;
        }
    }

    private record BlockImageExportData(String id, int meta, @Nullable String nbt) {}
}
