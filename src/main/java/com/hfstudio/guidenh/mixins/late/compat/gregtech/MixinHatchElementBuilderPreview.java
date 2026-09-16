package com.hfstudio.guidenh.mixins.late.compat.gregtech;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.hfstudio.guidenh.integration.gregtech.GregTechPreviewHatchCandidates;
import com.hfstudio.guidenh.integration.structurelib.MinimumHatchStructureElement;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.util.HatchElementBuilder;

@Mixin(value = HatchElementBuilder.class, remap = false)
public abstract class MixinHatchElementBuilderPreview<T> {

    @Shadow
    private BiFunction<? super T, ItemStack, ? extends Predicate<ItemStack>> mHatchItemFilter;

    @Inject(method = "atLeast(Ljava/util/Map;)Lgregtech/api/util/HatchElementBuilder;", at = @At("RETURN"))
    private void guidenh$preserveDeclaredCandidates(Map<IHatchElement<? super T>, ? extends Number> elements,
        CallbackInfoReturnable<HatchElementBuilder<T>> callback) {
        // atLeast's placement filter only reports unmet counts, excluding even zero-minimum optional types.
        mHatchItemFilter = GregTechPreviewHatchCandidates.withDeclaredCandidates(mHatchItemFilter, elements.keySet());
    }

    @Inject(method = "build", at = @At("RETURN"), cancellable = true)
    private void guidenh$preserveOptionalChainFallbacks(CallbackInfoReturnable<IStructureElement<?>> callback) {
        IStructureElement<?> element = callback.getReturnValue();
        if (element == null) {
            return;
        }
        boolean hasMinimumRequirement = ((AccessorHatchElementBuilder) this).guidenh$getReject() != null;
        int casingIndex = ((AccessorHatchElementBuilder) this).guidenh$getCasingIndex();
        callback.setReturnValue(new MinimumHatchStructureElement<>(element, hasMinimumRequirement, casingIndex));
    }
}
