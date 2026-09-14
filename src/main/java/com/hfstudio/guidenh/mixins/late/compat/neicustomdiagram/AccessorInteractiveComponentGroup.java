package com.hfstudio.guidenh.mixins.late.compat.neicustomdiagram;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.InteractiveComponentGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;

@Mixin(value = InteractiveComponentGroup.class, remap = false)
public interface AccessorInteractiveComponentGroup {

    @Accessor("slotTooltip")
    Tooltip getSlotTooltip();
}
