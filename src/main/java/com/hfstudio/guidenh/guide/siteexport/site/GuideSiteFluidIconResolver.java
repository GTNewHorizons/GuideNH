package com.hfstudio.guidenh.guide.siteexport.site;

import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;

@FunctionalInterface
public interface GuideSiteFluidIconResolver {

    GuideSiteFluidIconResolver NONE = fluid -> "";

    String exportFluidIcon(FluidStackSpec fluid);

    static GuideSiteFluidIconResolver from(GuideSiteItemIconResolver resolver) {
        return resolver instanceof GuideSiteFluidIconResolver fluids ? fluids : NONE;
    }
}
