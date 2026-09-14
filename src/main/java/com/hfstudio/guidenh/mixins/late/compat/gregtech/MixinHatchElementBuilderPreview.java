package com.hfstudio.guidenh.mixins.late.compat.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.hfstudio.guidenh.integration.structurelib.MinimumHatchStructureElement;

import gregtech.api.util.HatchElementBuilder;

@Mixin(value = HatchElementBuilder.class, remap = false)
public abstract class MixinHatchElementBuilderPreview {

    @Inject(method = "build", at = @At("RETURN"), cancellable = true)
    private void guidenh$preserveOptionalChainFallbacks(CallbackInfoReturnable<IStructureElement<?>> callback) {
        IStructureElement<?> element = callback.getReturnValue();
        if (element == null) {
            return;
        }
        boolean hasMinimumRequirement = ((AccessorHatchElementBuilder) (Object) this).guidenh$getReject() != null;
        int casingIndex = ((AccessorHatchElementBuilder) (Object) this).guidenh$getCasingIndex();
        callback.setReturnValue(new MinimumHatchStructureElement<>(element, hasMinimumRequirement, casingIndex));
    }
}
