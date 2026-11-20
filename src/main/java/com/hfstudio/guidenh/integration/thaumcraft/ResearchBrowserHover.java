package com.hfstudio.guidenh.integration.thaumcraft;

import org.jetbrains.annotations.Nullable;

import thaumcraft.api.research.ResearchItem;

public interface ResearchBrowserHover {

    @Nullable
    ResearchItem guidenh$researchAt(int x, int y);
}
