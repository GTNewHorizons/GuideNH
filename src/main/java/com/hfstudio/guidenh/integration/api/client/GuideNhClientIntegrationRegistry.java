package com.hfstudio.guidenh.integration.api.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.MutableGuide;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuideNhClientIntegrationRegistry {

    private static final GuideNhClientIntegrationRegistry GLOBAL = new GuideNhClientIntegrationRegistry();
    // Providers are usually registered once during mod initialization but queried in render
    // loops. Publish an immutable replacement on writes so reads require neither a lock nor a
    // defensive List.copyOf allocation.
    private volatile List<PreviewPlayerSlimArmProvider> previewPlayerSlimArmProviders = List.of();
    private volatile List<PreviewPlayerModelProvider> previewPlayerModelProviders = List.of();
    private volatile List<PreviewPlayerElytraProvider> previewPlayerElytraProviders = List.of();
    private volatile List<PreviewBlockRenderProvider> previewBlockRenderProviders = List.of();
    private volatile List<QuestHoverProvider> questHoverProviders = List.of();

    public GuideNhClientIntegrationRegistry() {}

    public static GuideNhClientIntegrationRegistry global() {
        return GLOBAL;
    }

    public synchronized void registerPreviewPlayerSlimArmProvider(PreviewPlayerSlimArmProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!previewPlayerSlimArmProviders.contains(provider)) {
            previewPlayerSlimArmProviders = appendProvider(previewPlayerSlimArmProviders, provider);
        }
    }

    public List<PreviewPlayerSlimArmProvider> previewPlayerSlimArmProviders() {
        return previewPlayerSlimArmProviders;
    }

    public synchronized void registerPreviewPlayerModelProvider(PreviewPlayerModelProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!previewPlayerModelProviders.contains(provider)) {
            previewPlayerModelProviders = appendProvider(previewPlayerModelProviders, provider);
        }
    }

    public List<PreviewPlayerModelProvider> previewPlayerModelProviders() {
        return previewPlayerModelProviders;
    }

    public synchronized void registerPreviewPlayerElytraProvider(PreviewPlayerElytraProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!previewPlayerElytraProviders.contains(provider)) {
            previewPlayerElytraProviders = appendProvider(previewPlayerElytraProviders, provider);
        }
    }

    public List<PreviewPlayerElytraProvider> previewPlayerElytraProviders() {
        return previewPlayerElytraProviders;
    }

    public synchronized void registerPreviewBlockRenderProvider(PreviewBlockRenderProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!previewBlockRenderProviders.contains(provider)) {
            previewBlockRenderProviders = appendProvider(previewBlockRenderProviders, provider);
        }
    }

    public List<PreviewBlockRenderProvider> previewBlockRenderProviders() {
        return previewBlockRenderProviders;
    }

    public synchronized void registerQuestHoverProvider(QuestHoverProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!questHoverProviders.contains(provider)) {
            questHoverProviders = appendProvider(questHoverProviders, provider);
        }
    }

    public List<QuestHoverProvider> questHoverProviders() {
        return questHoverProviders;
    }

    private static <T> List<T> appendProvider(List<T> currentProviders, T provider) {
        List<T> updatedProviders = new ArrayList<>(currentProviders.size() + 1);
        updatedProviders.addAll(currentProviders);
        updatedProviders.add(provider);
        return List.copyOf(updatedProviders);
    }

    @Nullable
    public Boolean resolveSlimArms(@Nullable AbstractClientPlayer player) {
        for (PreviewPlayerSlimArmProvider provider : previewPlayerSlimArmProviders()) {
            Boolean slimArms = provider.resolveSlimArms(player);
            if (slimArms != null) {
                return slimArms;
            }
        }
        return null;
    }

    public boolean isPreviewPlayerModelProvided() {
        for (PreviewPlayerModelProvider provider : previewPlayerModelProviders()) {
            if (provider.isModelProvided()) {
                return true;
            }
        }
        return false;
    }

    public boolean tryInitializePreviewPlayerModel(Object model) {
        if (model == null) {
            return false;
        }
        for (PreviewPlayerModelProvider provider : previewPlayerModelProviders()) {
            if (provider.tryInitializeModel(model)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPreviewPlayerElytraStack(@Nullable ItemStack stack) {
        if (stack == null) {
            return false;
        }
        for (PreviewPlayerElytraProvider provider : previewPlayerElytraProviders()) {
            if (provider.isElytraStack(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean tryRenderPreviewPlayerElytraLayer(@Nullable EntityLivingBase entity, float limbSwing,
        float limbSwingAmount, float partialTicks, float ageInTicks, float scale) {
        if (entity == null) {
            return false;
        }
        for (PreviewPlayerElytraProvider provider : previewPlayerElytraProviders()) {
            if (provider.tryRenderElytraLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, scale)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public TileEntity promotePreviewBlockTileEntity(@Nullable Block block, @Nullable TileEntity tileEntity) {
        if (block == null || tileEntity == null) {
            return tileEntity;
        }
        TileEntity current = tileEntity;
        for (PreviewBlockRenderProvider provider : previewBlockRenderProviders()) {
            TileEntity promoted = provider.promoteTileEntity(block, current);
            if (promoted != null) {
                return promoted;
            }
        }
        return current;
    }

    public boolean tryRenderPreviewWorldBlock(@Nullable RenderBlocks renderBlocks, @Nullable IBlockAccess blockAccess,
        @Nullable Block block, int x, int y, int z) {
        if (renderBlocks == null || blockAccess == null || block == null) {
            return false;
        }
        for (PreviewBlockRenderProvider provider : previewBlockRenderProviders()) {
            if (provider.tryRenderWorldBlock(renderBlocks, blockAccess, block, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public boolean isQuestHoverAvailable() {
        for (QuestHoverProvider provider : questHoverProviders()) {
            if (provider.isQuestHoverAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public UUID currentHoveredQuestId() {
        for (QuestHoverProvider provider : questHoverProviders()) {
            if (!provider.isQuestHoverAvailable()) {
                continue;
            }
            UUID questId = provider.currentHoveredQuestId();
            if (questId != null) {
                return questId;
            }
        }
        return null;
    }

    @Nullable
    public PageAnchor findQuestHoverPage(@Nullable MutableGuide guide, @Nullable UUID questId) {
        if (guide == null || questId == null) {
            return null;
        }
        for (QuestHoverProvider provider : questHoverProviders()) {
            if (!provider.isQuestHoverAvailable()) {
                continue;
            }
            PageAnchor pageAnchor = provider.findQuestHoverPage(guide, questId);
            if (pageAnchor != null) {
                return pageAnchor;
            }
        }
        return null;
    }
}
