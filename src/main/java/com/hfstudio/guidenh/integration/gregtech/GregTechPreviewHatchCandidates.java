package com.hfstudio.guidenh.integration.gregtech;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewMetadataScope;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

public final class GregTechPreviewHatchCandidates {

    private GregTechPreviewHatchCandidates() {}

    public static <T> BiFunction<? super T, ItemStack, ? extends Predicate<ItemStack>> withDeclaredCandidates(
        BiFunction<? super T, ItemStack, ? extends Predicate<ItemStack>> placementFilter,
        Collection<IHatchElement<? super T>> elements) {
        List<IHatchElement<? super T>> declared = List.copyOf(elements);
        Set<Class<? extends IMetaTileEntity>> blacklist = new HashSet<>();
        for (IHatchElement<?> element : declared) {
            if (element != null && element.mteBlacklist() != null) {
                blacklist.addAll(element.mteBlacklist());
            }
        }
        Predicate<ItemStack> candidates = stack -> {
            if (stack == null || stack.getItem() == null) {
                return false;
            }
            IMetaTileEntity machine = StructureLibPreviewMetadataScope.resolveMachine(stack);
            if (machine == null || blacklist.contains(machine.getClass())) {
                return false;
            }
            for (IHatchElement<?> element : declared) {
                if (element != null && element.matchesHatch(machine)) {
                    return true;
                }
            }
            return false;
        };
        // Later hatchItemFilterAnd calls compose with this factory, retaining tier, channel and custom restrictions.
        return (context, trigger) -> StructureLibPreviewMetadataScope.isActive() ? candidates
            : placementFilter.apply(context, trigger);
    }
}
