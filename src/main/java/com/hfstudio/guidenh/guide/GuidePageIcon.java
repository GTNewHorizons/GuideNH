package com.hfstudio.guidenh.guide;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.fluid.FluidIconRenderer;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;
import com.hfstudio.guidenh.guide.render.RenderContext;

public record GuidePageIcon(@Nullable ItemStack itemStack, @Nullable ResourceLocation textureId,
    @Nullable GuidePageTexture texture, @Nullable List<ItemStack> cycleItemStacks,
    @Nullable List<GuidePageTexture> cycleTextures, @Nullable List<ResourceLocation> cycleTextureIds,
    List<FluidStackSpec> fluidSpecs, List<FluidStack> fluidStacks) {

    public GuidePageIcon {
        fluidSpecs = List.copyOf(fluidSpecs);
        fluidStacks = fluidStacks.stream()
            .map(FluidStack::copy)
            .toList();
        if (fluidSpecs.size() != fluidStacks.size())
            throw new IllegalArgumentException("Fluid icon descriptions and stacks must match");
    }

    public GuidePageIcon(@Nullable ItemStack itemStack, @Nullable ResourceLocation textureId,
        @Nullable GuidePageTexture texture, @Nullable List<ItemStack> cycleItemStacks,
        @Nullable List<GuidePageTexture> cycleTextures, @Nullable List<ResourceLocation> cycleTextureIds) {
        this(itemStack, textureId, texture, cycleItemStacks, cycleTextures, cycleTextureIds, List.of(), List.of());
    }

    public static GuidePageIcon fluid(FluidStackSpec spec) {
        return cycleFluids(List.of(spec));
    }

    public static GuidePageIcon cycleFluids(List<FluidStackSpec> specs) {
        if (specs.isEmpty()) throw new IllegalArgumentException("Fluid icon list must not be empty");
        return new GuidePageIcon(
            null,
            null,
            null,
            null,
            null,
            null,
            specs,
            specs.stream()
                .map(FluidStacks::resolve)
                .toList());
    }

    public boolean isFluidIcon() {
        return !fluidSpecs.isEmpty();
    }

    public List<FluidStack> fluidStacks() {
        return fluidStacks.stream()
            .map(FluidStack::copy)
            .toList();
    }

    public @Nullable FluidStackSpec resolveCurrentFluidSpec() {
        return isFluidIcon() ? fluidSpecs.get(fluidIndex()) : null;
    }

    public void renderCurrentFluid(RenderContext context, int x, int y, int size) {
        if (isFluidIcon()) FluidIconRenderer.draw(context, fluidStacks.get(fluidIndex()), x, y, size, false);
    }

    public void renderCurrentFluid(int x, int y, int size) {
        if (isFluidIcon()) FluidIconRenderer.draw(fluidStacks.get(fluidIndex()), x, y, size, false);
    }

    private int fluidIndex() {
        return (int) ((System.nanoTime() / 1_000_000_000L) % fluidSpecs.size());
    }

    public static GuidePageIcon item(ItemStack itemStack) {
        return new GuidePageIcon(itemStack, null, null, null, null, null);
    }

    public static GuidePageIcon textureId(ResourceLocation textureId) {
        return new GuidePageIcon(null, textureId, null, null, null, null);
    }

    public static GuidePageIcon texture(ResourceLocation textureId, GuidePageTexture texture) {
        return new GuidePageIcon(null, textureId, texture, null, null, null);
    }

    public static GuidePageIcon cycleItems(List<ItemStack> itemStacks) {
        if (itemStacks.isEmpty()) {
            throw new IllegalArgumentException("cycle item icon list must not be empty");
        }
        var copiedStacks = copy(itemStacks);
        return new GuidePageIcon(copiedStacks.getFirst(), null, null, copiedStacks, null, null);
    }

    public static GuidePageIcon cycleTextureIds(List<ResourceLocation> textureIds) {
        if (textureIds.isEmpty()) {
            throw new IllegalArgumentException("cycle texture icon list must not be empty");
        }
        var copiedTextureIds = copy(textureIds);
        return new GuidePageIcon(null, copiedTextureIds.getFirst(), null, null, null, copiedTextureIds);
    }

    public static GuidePageIcon cycleTextures(List<ResourceLocation> textureIds, List<GuidePageTexture> textures) {
        if (textureIds.isEmpty()) {
            throw new IllegalArgumentException("cycle texture icon id list must not be empty");
        }
        var copiedTextureIds = copy(textureIds);
        var copiedTextures = textures.isEmpty() ? null : copy(textures);
        return new GuidePageIcon(
            null,
            copiedTextureIds.getFirst(),
            copiedTextures == null ? null : copiedTextures.getFirst(),
            null,
            copiedTextures,
            copiedTextureIds);
    }

    private static <T> List<T> copy(List<T> values) {
        return List.copyOf(values);
    }

    public boolean isItemIcon() {
        return itemStack != null || (cycleItemStacks != null && !cycleItemStacks.isEmpty());
    }

    public boolean isTextureIcon() {
        return texture != null || (cycleTextures != null && !cycleTextures.isEmpty());
    }

    @Nullable
    public ItemStack resolveCurrentItemStack() {
        if (cycleItemStacks != null && !cycleItemStacks.isEmpty()) {
            if (cycleItemStacks.size() == 1) return cycleItemStacks.getFirst();
            int idx = (int) ((System.currentTimeMillis() / 1000L) % cycleItemStacks.size());
            return cycleItemStacks.get(idx);
        }
        return itemStack;
    }

    @Nullable
    public GuidePageTexture resolveCurrentTexture() {
        if (cycleTextures != null && !cycleTextures.isEmpty()) {
            if (cycleTextures.size() == 1) return cycleTextures.getFirst();
            int idx = (int) ((System.currentTimeMillis() / 1000L) % cycleTextures.size());
            return cycleTextures.get(idx);
        }
        return texture;
    }

    @Nullable
    public ResourceLocation resolveCurrentTextureId() {
        if (cycleTextureIds != null && !cycleTextureIds.isEmpty()) {
            if (cycleTextureIds.size() == 1) return cycleTextureIds.getFirst();
            int idx = (int) ((System.currentTimeMillis() / 1000L) % cycleTextureIds.size());
            return cycleTextureIds.get(idx);
        }
        return textureId;
    }
}
