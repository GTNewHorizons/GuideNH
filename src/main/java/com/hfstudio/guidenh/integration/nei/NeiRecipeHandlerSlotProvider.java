package com.hfstudio.guidenh.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.fluid.FluidStackResolver;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.integration.api.RecipeHandlerSlotProvider;
import com.hfstudio.guidenh.integration.api.RecipeSlot;

public class NeiRecipeHandlerSlotProvider implements RecipeHandlerSlotProvider {

    @Override
    public List<RecipeSlot> readIngredientSlots(Object handler, int recipeIndex) {
        return convertSlots(NeiRecipeLookup.readIngredientSlots(handler, recipeIndex));
    }

    @Override
    public List<RecipeSlot> readOtherSlots(Object handler, int recipeIndex) {
        return convertSlots(NeiRecipeLookup.readOtherSlots(handler, recipeIndex));
    }

    @Override
    public @Nullable RecipeSlot readResultSlot(Object handler, int recipeIndex) {
        return convertSlot(NeiRecipeLookup.readResultSlot(handler, recipeIndex));
    }

    public static List<RecipeSlot> convertSlots(List<NeiRecipeLookup.Slot> slots) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        ArrayList<RecipeSlot> converted = new ArrayList<>(slots.size());
        for (NeiRecipeLookup.Slot slot : slots) {
            RecipeSlot convertedSlot = convertSlot(slot);
            if (convertedSlot != null) {
                converted.add(convertedSlot);
            }
        }
        return converted;
    }

    @Nullable
    public static RecipeSlot convertSlot(@Nullable NeiRecipeLookup.Slot slot) {
        if (slot == null) {
            return null;
        }
        if (!slot.fluids.isEmpty())
            return new RecipeSlot(slot.relx, slot.rely, slot.stacks, slot.current, slot.fluids, slot.currentFluid);
        var fluids = new ArrayList<FluidStackSpec>();
        for (var stack : slot.stacks == null ? List.<ItemStack>of() : slot.stacks) {
            var fluid = FluidStackResolver.getDisplayedFluid(stack);
            if (fluid != null) fluids.add(FluidStackSpec.fromStack(fluid, true));
        }
        var current = FluidStackResolver.getDisplayedFluid(slot.current);
        return new RecipeSlot(
            slot.relx,
            slot.rely,
            slot.stacks,
            slot.current,
            fluids,
            current == null ? null : FluidStackSpec.fromStack(current, true));
    }
}
