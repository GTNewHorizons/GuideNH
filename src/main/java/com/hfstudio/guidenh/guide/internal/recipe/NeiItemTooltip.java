package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltipAppender;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

import codechicken.nei.NEIClientUtils;

/**
 * An {@link ItemTooltip} that lets the NEI handler contribute extra lines via
 * {@code IRecipeHandler.handleItemTooltip}. The GuideScreen renderer checks for this subtype and
 * appends {@link #appendExtraLines(List)} output after the vanilla tooltip lines.
 */
public class NeiItemTooltip extends ItemTooltip implements ItemTooltipAppender {

    private final Object handler;
    private final int recipeIndex;
    private final List<String> positionedTooltip;
    private final List<ItemStack> permutations;
    private final int activePermutationIndex;
    private final String acceptsLabel;

    public NeiItemTooltip(ItemStack stack, Object handler, int recipeIndex) {
        this(stack, handler, recipeIndex, null);
    }

    public NeiItemTooltip(ItemStack stack, Object handler, int recipeIndex, List<String> positionedTooltip) {
        this(stack, handler, recipeIndex, positionedTooltip, List.of(), null);
    }

    public NeiItemTooltip(ItemStack stack, Object handler, int recipeIndex, List<String> positionedTooltip,
        List<ItemStack> permutations, String acceptsLabel) {
        super(stack);
        this.handler = handler;
        this.recipeIndex = recipeIndex;
        this.positionedTooltip = copyStrings(positionedTooltip);
        this.permutations = copyStacks(permutations);
        this.activePermutationIndex = findActivePermutationIndex(stack, this.permutations);
        this.acceptsLabel = acceptsLabel;
    }

    /**
     * Passes the already-built vanilla tooltip to the handler. NEI handlers can both append lines
     * and update existing indexed lines, so using an empty temporary list loses handler data.
     */
    public void appendExtraLines(List<String> base) {
        if (base == null) return;
        GuideNhIntegrationRegistry.global()
            .appendRecipeItemTooltip(handler, getStack(), base, recipeIndex);
        for (String line : positionedTooltip) {
            if (line != null && !line.isEmpty()) base.add(line);
        }
        if (permutations.size() > 1) {
            String label = acceptsLabel == null || acceptsLabel.isEmpty() ? NEIClientUtils.translate("recipe.accepts")
                : acceptsLabel;
            base.add(EnumChatFormatting.GRAY + label + ":");
        }
    }

    public List<ItemStack> getPermutations() {
        return permutations;
    }

    public int getActivePermutationIndex() {
        return activePermutationIndex;
    }

    private static int findActivePermutationIndex(ItemStack active, List<ItemStack> candidates) {
        if (active == null) return -1;
        for (int index = 0; index < candidates.size(); index++) {
            ItemStack candidate = candidates.get(index);
            if (candidate != null && NEIClientUtils.areStacksSameType(candidate, active)) return index;
        }
        return -1;
    }

    public Object getHandler() {
        return handler;
    }

    public int getRecipeIndex() {
        return recipeIndex;
    }

    public String getAcceptsLabel() {
        return acceptsLabel;
    }

    @Override
    public void appendTooltipLines(List<String> lines) {
        appendExtraLines(lines);
    }

    private static List<String> copyStrings(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        ArrayList<String> copy = new ArrayList<>(values.size());
        for (String value : values) {
            if (value != null && !value.isEmpty()) copy.add(value);
        }
        return copy.isEmpty() ? List.of() : List.copyOf(copy);
    }

    private static List<ItemStack> copyStacks(List<ItemStack> values) {
        if (values == null || values.isEmpty()) return List.of();
        ArrayList<ItemStack> copy = new ArrayList<>(values.size());
        for (ItemStack value : values) {
            if (value != null) copy.add(value);
        }
        return copy.isEmpty() ? List.of() : List.copyOf(copy);
    }
}
