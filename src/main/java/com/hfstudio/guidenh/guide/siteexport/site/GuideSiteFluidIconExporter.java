package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hfstudio.guidenh.client.GuideClientTasks;
import com.hfstudio.guidenh.guide.internal.fluid.FluidCatalog;
import com.hfstudio.guidenh.guide.internal.fluid.FluidIconRenderer;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;
import com.hfstudio.guidenh.guide.render.GuideIconCapture;

public class GuideSiteFluidIconExporter implements GuideSiteFluidIconResolver, AutoCloseable {

    private static final int ICON_SIZE = 96;
    private static final int CACHE_LIMIT = 512;
    private final GuideSiteAssetRegistry assets;
    private final GuideIconCapture capture = new GuideIconCapture();
    private final Map<FluidStackSpec, String> icons = new LinkedHashMap<>(16, 0.75f, true);
    private long revision = Long.MIN_VALUE;

    public GuideSiteFluidIconExporter(GuideSiteAssetRegistry assets) {
        this.assets = assets;
    }

    @Override
    public String exportFluidIcon(FluidStackSpec fluid) {
        return GuideClientTasks.call(() -> exportOnClient(fluid));
    }

    private String exportOnClient(FluidStackSpec fluid) {
        long currentRevision = FluidCatalog.INSTANCE.resourceRevision();
        if (currentRevision != revision) {
            icons.clear();
            capture.close();
            revision = currentRevision;
        }
        String path = icons.get(fluid);
        if (path != null) return path;
        var stack = FluidStacks.resolve(fluid);
        var image = capture.capture(ICON_SIZE, () -> FluidIconRenderer.draw(stack, 0, 0, ICON_SIZE, false));
        try {
            path = GuideSitePageAssetExporter.ROOT_PREFIX + assets.writePngAsync("fluid-icons", image);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to export fluid icon: " + fluid.id(), exception);
        }
        icons.put(fluid, path);
        if (icons.size() > CACHE_LIMIT) icons.remove(
            icons.keySet()
                .iterator()
                .next());
        return path;
    }

    @Override
    public void close() {
        GuideClientTasks.call(() -> {
            capture.close();
            icons.clear();
            return null;
        });
    }
}
