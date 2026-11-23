package com.hfstudio.guidenh.mixins.late.compat.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchBrowser;

@Mixin(value = GuiResearchBrowser.class, remap = false)
public interface AccessorGuiResearchBrowser {

    @Accessor("currentHighlight")
    ResearchItem guidenh$getHighlightedResearch();

    @Accessor("paneWidth")
    int guidenh$getCapturePaneWidth();

    @Accessor("paneHeight")
    int guidenh$getCapturePaneHeight();

    @Accessor("selectedCategory")
    static void guidenh$setSelectedCategory(String category) {
        throw new AssertionError();
    }

    @Accessor("selectedCategory")
    static String guidenh$getCaptureCategory() {
        throw new AssertionError();
    }

    @Accessor("guiMapX")
    double guidenh$getCaptureMapX();

    @Accessor("guiMapX")
    void guidenh$setCaptureMapX(double value);

    @Accessor("guiMapY")
    double guidenh$getCaptureMapY();

    @Accessor("guiMapY")
    void guidenh$setCaptureMapY(double value);

    @Accessor("field_74117_m")
    double guidenh$getCapturePreviousX();

    @Accessor("field_74117_m")
    void guidenh$setCapturePreviousX(double value);

    @Accessor("field_74115_n")
    double guidenh$getCapturePreviousY();

    @Accessor("field_74115_n")
    void guidenh$setCapturePreviousY(double value);

    @Accessor("field_74124_q")
    double guidenh$getCaptureTargetX();

    @Accessor("field_74124_q")
    void guidenh$setCaptureTargetX(double value);

    @Accessor("field_74123_r")
    double guidenh$getCaptureTargetY();

    @Accessor("field_74123_r")
    void guidenh$setCaptureTargetY(double value);
}
