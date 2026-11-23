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
        "minecraft.AccessorSimpleReloadableResourceManager", "minecraft.MixinResourcePackReload",
        "forge.AccessorShapedOreRecipe", "forge.AccessorShapelessOreRecipe", "minecraft.AccessorTextureManager",
        "minecraft.AccessorTextureMap", "minecraft.AccessorTextureAtlasSprite",
        "minecraft.MixinTessellatorSceneExportCapture", "minecraft.MixinModelRendererSceneExportCapture",
        "minecraft.AccessorGuiCaptureScreen", "minecraft.AccessorGuiCaptureContainer", "minecraft.MixinGuiCaptureInput",
        "minecraft.MixinGuiCaptureRender", "minecraft.MixinGuiCaptureLayer", "minecraft.MixinGuiCaptureBackground",
        "minecraft.MixinGuiCaptureAlpha", "minecraft.MixinGuiCaptureButtonAlpha",
        "minecraft.MixinGuiCaptureFramebuffer", "minecraft.MixinGuiCaptureMainMenu",
        "minecraft.MixinStandaloneContainerScreen", "minecraft.MixinCaptureContainerSynchronization"),

    AE2_EXTERNAL_CABLE_PARTS(Side.CLIENT, Phase.LATE, Mods.AE2, "compat.ae2.MixinPartQuartzFiber",
        "compat.ae2.MixinPartP2PTunnelME", "compat.ae2.MixinPartToggleBus"),

    LWJGL_CAPTURE_INPUT(Side.CLIENT, Phase.EARLY, Mods.Lwjgl3ify, "compat.lwjgl3ify.MixinCaptureInputEvents",
        "compat.lwjgl3ify.AccessorCaptureTextInput"),

    MUI_CAPTURE_GEOMETRY(Side.CLIENT, Phase.LATE, Mods.ModularUI, "compat.mui.AccessorCaptureModularWindow"),

    SERVER_CAPTURE_CONTAINER(new MixinBuilder()
        .addCommonMixins("minecraft.MixinCaptureServerContainer", "minecraft.AccessorCaptureEnderInventory")
        .setPhase(Phase.EARLY)),

    MUI_CAPTURE_FACTORY(new MixinBuilder().addCommonMixins("compat.mui.MixinCaptureMuiFactory")
        .setPhase(Phase.LATE)
        .addRequiredMod(Mods.ModularUI)),

    MUI2_CAPTURE_FACTORY(new MixinBuilder().addCommonMixins("compat.mui2.MixinCaptureMui2Factory")
        .setPhase(Phase.LATE)
        .addRequiredMod(Mods.ModularUI2)),

    NEI_CAPTURE_GUI(Side.CLIENT, Phase.LATE, Mods.NotEnoughItems, "compat.nei.AccessorCaptureNeiLayout",
        "compat.nei.AccessorCaptureNeiRecipe", "compat.nei.AccessorCaptureNeiTask", "compat.nei.AccessorCaptureNeiText",
        "compat.nei.MixinCaptureNeiLayers"),

    BQ_CAPTURE_GUI(Side.CLIENT, Phase.LATE, Mods.BetterQuesting, "compat.betterquesting.AccessorCaptureBqCanvas",
        "compat.betterquesting.AccessorCaptureBqQuest", "compat.betterquesting.AccessorCaptureBqQuestLines",
        "compat.betterquesting.MixinCaptureBqIconFramebuffer"),

    ANGELICA_SCENE_EXPORT(Side.CLIENT, Phase.LATE, Mods.Angelica,
        "compat.angelica.MixinVanillaModelMeshesSceneExportCapture"),

    GREGTECH_HATCH_BUILDER(Side.CLIENT, Phase.LATE, Mods.GregTech, "compat.gregtech.MixinHatchElementBuilderPreview",
        "compat.gregtech.MixinHatchElementPlacementPreview", "compat.gregtech.MixinItemMachinesPreview"),

    ARI_RESEARCH(Side.CLIENT, Phase.LATE, Mods.AspectRecipeIndex, "compat.aspectrecipeindex.AccessorCachedThaumRecipe",
        "compat.aspectrecipeindex.AccessorResearchInfo", "compat.aspectrecipeindex.MixinAriResearchTooltip"),

    THAUMCRAFT_RESEARCH(Side.CLIENT, Phase.LATE, Mods.Thaumcraft, "compat.thaumcraft.MixinResearchBrowserHover",
        "compat.thaumcraft.AccessorGuiResearchBrowser", "compat.thaumcraft.AccessorGuiResearchRecipe",
        "compat.thaumcraft.MixinGuiResearchBrowser", "compat.thaumcraft.MixinResearchRecipeGuide"),

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
