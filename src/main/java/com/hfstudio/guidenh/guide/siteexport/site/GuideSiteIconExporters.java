package com.hfstudio.guidenh.guide.siteexport.site;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;

public class GuideSiteIconExporters implements GuideSiteItemIconResolver, GuideSiteFluidIconResolver, AutoCloseable {

    private final GuideSiteItemIconExporter items;
    private final GuideSiteFluidIconExporter fluids;

    public GuideSiteIconExporters(GuideSiteAssetRegistry assets, GuideSiteTextureAnimations animations) {
        items = new GuideSiteItemIconExporter(assets, animations);
        fluids = new GuideSiteFluidIconExporter(assets);
    }

    @Override
    public String exportIcon(@Nullable ItemStack stack) {
        return items.exportIcon(stack);
    }

    @Override
    public String exportFluidIcon(FluidStackSpec fluid) {
        return fluids.exportFluidIcon(fluid);
    }

    @Override
    public void close() {
        try {
            items.close();
        } finally {
            fluids.close();
        }
    }
}
