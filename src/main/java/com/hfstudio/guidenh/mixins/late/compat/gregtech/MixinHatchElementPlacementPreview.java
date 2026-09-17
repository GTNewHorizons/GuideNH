package com.hfstudio.guidenh.mixins.late.compat.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;
import com.hfstudio.guidenh.integration.structurelib.StructureLibMinimumHatchPlacement;

/**
 * Applies GuideNH preview placement policy to GregTech's original hatch element.
 *
 * <p>
 * The builder deliberately continues to return its original anonymous element. Some consumers inspect that
 * implementation's enclosing builder, so replacing it with a delegating element is not binary compatible.
 */
@Mixin(targets = "gregtech.api.util.HatchElementBuilder$2", remap = false)
public abstract class MixinHatchElementPlacementPreview {

    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void guidenh$leaveOptionalCreativeHatchPositionForFallback(Object context, World world, int x, int y, int z,
        ItemStack trigger, CallbackInfoReturnable<Boolean> callback) {
        if (StructureLibMinimumHatchPlacement.shouldUseFallback((IStructureElement<?>) this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(
        method = "survivalPlaceBlock(Ljava/lang/Object;Lnet/minecraft/world/World;IIILnet/minecraft/item/ItemStack;Lcom/gtnewhorizon/structurelib/structure/AutoPlaceEnvironment;)Lcom/gtnewhorizon/structurelib/structure/IStructureElement$PlaceResult;",
        at = @At("HEAD"),
        cancellable = true)
    private void guidenh$leaveOptionalHatchPositionForFallback(Object context, World world, int x, int y, int z,
        ItemStack trigger, AutoPlaceEnvironment environment,
        CallbackInfoReturnable<IStructureElement.PlaceResult> callback) {
        if (StructureLibMinimumHatchPlacement.shouldUseFallback((IStructureElement<?>) this)) {
            callback.setReturnValue(IStructureElement.PlaceResult.REJECT);
        }
    }

    @Inject(
        method = "survivalPlaceBlock(Ljava/lang/Object;Lnet/minecraft/world/World;IIILnet/minecraft/item/ItemStack;Lcom/gtnewhorizon/structurelib/structure/AutoPlaceEnvironment;)Lcom/gtnewhorizon/structurelib/structure/IStructureElement$PlaceResult;",
        at = @At("RETURN"))
    private void guidenh$applyPreviewHatchTexture(Object context, World world, int x, int y, int z, ItemStack trigger,
        AutoPlaceEnvironment environment, CallbackInfoReturnable<IStructureElement.PlaceResult> callback) {
        IStructureElement.PlaceResult result = callback.getReturnValue();
        if (result != IStructureElement.PlaceResult.ACCEPT && result != IStructureElement.PlaceResult.ACCEPT_STOP) {
            return;
        }
        int casingIndex = StructureLibMinimumHatchPlacement.getCasingIndex((IStructureElement<?>) this);
        if (casingIndex >= 0) {
            GregTechHelpers.updatePreviewHatchTexture(world, x, y, z, casingIndex);
        }
    }
}
