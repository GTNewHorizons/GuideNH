package com.hfstudio.guidenh.guide.document.block.recipes;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBox;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.document.block.LytSlotGrid;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

public class LytStandardRecipeBox extends LytBox {

    public static final ResourceLocation CRAFTING_TEXTURE = new ResourceLocation(
        "minecraft",
        "textures/gui/container/crafting_table.png");

    public static final int ARROW_U = 90;
    public static final int ARROW_V = 35;
    public static final int ARROW_W = 22;
    public static final int ARROW_H = 15;
    public static final int GAP = 4;

    private final LytSlotGrid inputs;
    private final LytSlot output;
    @Getter
    private final boolean shapeless;

    public LytStandardRecipeBox(LytSlotGrid inputs, ItemStack resultStack, boolean shapeless) {
        this.inputs = inputs;
        this.output = new LytSlot(resultStack);
        this.output.setLargeSlot(true);
        // Reserve the crafting-arrow gap as the output slot's own left margin,
        // so the flex-row layout (Rust) leaves room for the arrow between the
        // inputs grid and the output — declared by the block, not the compiler.
        this.output.setMarginLeft(GAP * 2 + ARROW_W);
        this.shapeless = shapeless;
        append(inputs);
        append(output);
    }

    private static int getGlTextureId(ResourceLocation res) {
        try {
            var tex = Minecraft.getMinecraft()
                .getTextureManager()
                .getTexture(res);
            return tex != null ? tex.getGlTextureId() : -1;
        } catch (Throwable t) {
            // Headless (unit tests) or texture unavailable: skip drawing.
            return -1;
        }
    }

    public static LytStandardRecipeBox shaped3x3(List<ItemStack> stacks, ItemStack result) {
        var grid = new LytSlotGrid(3, 3);
        int n = Math.min(9, stacks == null ? 0 : stacks.size());
        for (int i = 0; i < n; i++) {
            ItemStack s = stacks.get(i);
            if (s != null && s.stackSize > 0) {
                grid.setItem(i % 3, i / 3, s);
            }
        }
        return new LytStandardRecipeBox(grid, result, false);
    }

    public static LytStandardRecipeBox shapeless(List<ItemStack> stacks, ItemStack result) {
        var nonEmpty = stacks == null ? Collections.<ItemStack>emptyList() : stacks;
        var grid = new LytSlotGrid(3, 3);
        int n = Math.min(9, nonEmpty.size());
        for (int i = 0; i < n; i++) {
            ItemStack s = nonEmpty.get(i);
            if (s != null && s.stackSize > 0) {
                grid.setItem(i % 3, i / 3, s);
            }
        }
        return new LytStandardRecipeBox(grid, result, true);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int inW = inputs.getWidth() * LytSlot.OUTER_SIZE;
        int inH = inputs.getHeight() * LytSlot.OUTER_SIZE;
        inputs.layout(context, x, y, availableWidth);

        int arrowX = x + inW + GAP;
        int arrowY = y + (inH - ARROW_H) / 2;

        int outSize = LytSlot.OUTER_SIZE_LARGE;
        int outX = arrowX + ARROW_W + GAP;
        int outY = y + (inH - outSize) / 2;
        output.layout(context, outX, outY, availableWidth);

        int totalW = (outX + outSize) - x;
        int totalH = inH;
        return new LytRect(x, y, totalW, totalH);
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        super.computePrimitives(c);

        int inW = inputs.getWidth() * LytSlot.OUTER_SIZE;
        int inH = inputs.getHeight() * LytSlot.OUTER_SIZE;
        int arrowX = bounds.x() + inW + GAP;
        int arrowY = bounds.y() + (inH - ARROW_H) / 2;
        int texId = getGlTextureId(CRAFTING_TEXTURE);
        if (texId >= 0) {
            // Vanilla container GUI textures are 256x256 (same assumption as
            // VanillaRenderContext.blitTexture).
            c.emit(
                new GuideRenderPrimitive.BlitTexture(
                    texId,
                    arrowX,
                    arrowY,
                    ARROW_W,
                    ARROW_H,
                    ARROW_U / 256f,
                    ARROW_V / 256f,
                    (ARROW_U + ARROW_W) / 256f,
                    (ARROW_V + ARROW_H) / 256f));
        }
    }

    @Override
    public void render(RenderContext context) {
        super.render(context);

        int inW = inputs.getWidth() * LytSlot.OUTER_SIZE;
        int inH = inputs.getHeight() * LytSlot.OUTER_SIZE;
        int arrowX = bounds.x() + inW + GAP;
        int arrowY = bounds.y() + (inH - ARROW_H) / 2;
        context.blitTexture(CRAFTING_TEXTURE, arrowX, arrowY, ARROW_U, ARROW_V, ARROW_W, ARROW_H);
    }
}
