package com.hfstudio.guidenh.mixins.late.compat.structurelib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.hfstudio.guidenh.integration.structurelib.StructureLibMinimumHatchPlacement;

/** Supplies the currently visited element without depending on optional instrumentation. */
@Mixin(targets = "com.gtnewhorizon.structurelib.structure.SurvivalBuildStructureWalker", remap = false)
public abstract class MixinSurvivalBuildStructureWalker {

    @Inject(method = "visit", at = @At("HEAD"))
    private void guidenh$setCurrentPreviewElement(IStructureElement<?> element, World world, int x, int y, int z,
        int a, int b, int c, CallbackInfoReturnable<Boolean> callback) {
        StructureLibMinimumHatchPlacement.setCurrentElement(element);
    }

    @Inject(method = "visit", at = @At("RETURN"))
    private void guidenh$clearCurrentPreviewElement(IStructureElement<?> element, World world, int x, int y, int z,
        int a, int b, int c, CallbackInfoReturnable<Boolean> callback) {
        StructureLibMinimumHatchPlacement.clearCurrentElement();
    }
}
