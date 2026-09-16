package com.hfstudio.guidenh.integration.structurelib;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElementChain;

/**
 * Carries the element currently visited by StructureLib on the client thread.
 * HatchElementBuilder wrappers use it to leave an alternative chain position to
 * its casing fallback when the hatch is not needed to meet a minimum.
 */
public final class StructureLibMinimumHatchPlacement {

    private static final ThreadLocal<IStructureElement<?>> CURRENT_ELEMENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CREATIVE_CONSTRUCT = new ThreadLocal<>();

    private StructureLibMinimumHatchPlacement() {}

    public static void setCurrentElement(IStructureElement<?> element) {
        CURRENT_ELEMENT.set(element);
    }

    public static void clearCurrentElement() {
        CURRENT_ELEMENT.remove();
    }

    public static void beginCreativeConstruct() {
        CREATIVE_CONSTRUCT.set(Boolean.TRUE);
    }

    public static void endCreativeConstruct() {
        CREATIVE_CONSTRUCT.remove();
    }

    public static boolean shouldUseFallback(boolean hasMinimumRequirement) {
        return !hasMinimumRequirement
            && (CREATIVE_CONSTRUCT.get() == Boolean.TRUE || CURRENT_ELEMENT.get() instanceof IStructureElementChain<?>);
    }
}
