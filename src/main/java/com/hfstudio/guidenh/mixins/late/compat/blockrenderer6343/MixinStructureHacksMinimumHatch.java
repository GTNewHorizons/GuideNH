package com.hfstudio.guidenh.mixins.late.compat.blockrenderer6343;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.hfstudio.guidenh.integration.structurelib.MinimumHatchStructureElement;

import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.integration.nei.StructureHacks;

@Mixin(value = StructureHacks.class, remap = false)
public abstract class MixinStructureHacksMinimumHatch {

    @Inject(method = "getStacksForElement", at = @At("HEAD"), cancellable = true)
    private static <T> void guidenh$unwrapMinimumHatch(T context, IStructureElement<T> element, ConstructableData data,
        CallbackInfoReturnable<Iterable<ItemStack>> callback) {
        if (!(element instanceof MinimumHatchStructureElement<?>wrapped)) {
            return;
        }
        @SuppressWarnings("unchecked")
        IStructureElement<T> delegate = (IStructureElement<T>) wrapped.getDelegate();
        // Re-enter the transformed method with the original element. This preserves StructureHacks' own
        // channel-wrapper handling and avoids duplicating its private channel extraction rules here.
        callback.setReturnValue(StructureHacks.getStacksForElement(context, delegate, data));
    }
}
