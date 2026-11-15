package com.hfstudio.guidenh.guide.internal.search;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.GuideME;

public class GuideFluidLinksPage {

    public static final ResourceLocation PAGE_ID = GuideME.makeId("fluid_links");

    protected GuideFluidLinksPage() {}

    public static boolean isFluidLinksAnchor(@Nullable PageAnchor anchor) {
        return anchor != null && PAGE_ID.equals(anchor.pageId());
    }

    public static PageAnchor anchorForFluid(String id) {
        return new PageAnchor(PAGE_ID, id);
    }
}
