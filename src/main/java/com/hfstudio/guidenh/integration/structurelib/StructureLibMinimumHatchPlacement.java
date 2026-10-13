package com.hfstudio.guidenh.integration.structurelib;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElementChain;

/**
 * Carries GuideNH preview-only placement state without changing elements exposed by GregTech.
 */
public class StructureLibMinimumHatchPlacement {

    private static final Map<IStructureElement<?>, PlacementMetadata> METADATA = Collections
        .synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<IStructureElement<?>> CURRENT_ELEMENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PREVIEW_BUILD = new ThreadLocal<>();

    private StructureLibMinimumHatchPlacement() {}

    public static void register(IStructureElement<?> element, boolean hasMinimumRequirement, boolean exclusive,
        int casingIndex) {
        METADATA.put(element, new PlacementMetadata(hasMinimumRequirement, exclusive, casingIndex));
    }

    public static void setCurrentElement(IStructureElement<?> element) {
        CURRENT_ELEMENT.set(element);
    }

    public static void clearCurrentElement() {
        CURRENT_ELEMENT.remove();
    }

    public static void beginPreviewBuild() {
        PREVIEW_BUILD.set(Boolean.TRUE);
    }

    public static void endPreviewBuild() {
        PREVIEW_BUILD.remove();
        CURRENT_ELEMENT.remove();
    }

    public static boolean shouldUseFallback(IStructureElement<?> element) {
        PlacementMetadata metadata = METADATA.get(element);
        return PREVIEW_BUILD.get() == Boolean.TRUE && metadata != null
            && !metadata.hasMinimumRequirement()
            && !metadata.exclusive()
            && CURRENT_ELEMENT.get() instanceof IStructureElementChain<?>;
    }

    public static int getCasingIndex(IStructureElement<?> element) {
        PlacementMetadata metadata = METADATA.get(element);
        return metadata != null ? metadata.casingIndex() : -1;
    }

    private record PlacementMetadata(boolean hasMinimumRequirement, boolean exclusive, int casingIndex) {}
}
