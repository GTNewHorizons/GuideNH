package com.hfstudio.guidenh.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.hfstudio.guidenh.integration.Mods;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Mixins implements IMixins {

    EARLY(Side.CLIENT, "forge.AccessorForgeHooksClient", "forge.AccessorGuiIngameForge", "fml.AccessorFMLClientHandler",
        "minecraft.AccessorAbstractResourcePack", "minecraft.AccessorFallbackResourceManager",
        "minecraft.AccessorSimpleReloadableResourceManager", "forge.AccessorShapedOreRecipe",
        "forge.AccessorShapelessOreRecipe", "minecraft.AccessorTextureManager", "minecraft.AccessorTextureMap",
        "minecraft.AccessorTextureAtlasSprite", "minecraft.MixinTessellatorSceneExportCapture",
        "minecraft.MixinModelRendererSceneExportCapture"),

    AE2_EXTERNAL_CABLE_PARTS(Side.CLIENT, Phase.LATE, Mods.AE2, "compat.ae2.MixinPartQuartzFiber",
        "compat.ae2.MixinPartP2PTunnelME", "compat.ae2.MixinPartToggleBus"),

    ANGELICA_SCENE_EXPORT(Side.CLIENT, Phase.LATE, Mods.Angelica,
        "compat.angelica.MixinVanillaModelMeshesSceneExportCapture"),

    GREGTECH_HATCH_BUILDER(Side.CLIENT, Phase.LATE, Mods.GregTech, "compat.gregtech.MixinHatchElementBuilderPreview",
        "compat.gregtech.MixinHatchElementPlacementPreview"),

    STRUCTURELIB_MINIMUM_HATCHES(Side.CLIENT, Phase.LATE, Mods.StructureLib,
        "compat.structurelib.MixinSurvivalBuildStructureWalker"),

    ;

    private final MixinBuilder builder;

    Mixins(Side side, String... mixins) {
        this.builder = new MixinBuilder().addSidedMixins(side, mixins)
            .setPhase(Phase.EARLY);
    }

    Mixins(Side side, Phase phase, ITargetMod requiredMod, String... mixins) {
        this.builder = new MixinBuilder().addSidedMixins(side, mixins)
            .setPhase(phase)
            .addRequiredMod(requiredMod);
    }

    @Override
    public @Nonnull MixinBuilder getBuilder() {
        return builder;
    }
}
