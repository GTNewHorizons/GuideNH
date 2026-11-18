package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

public class LytItemImage extends LytIconImage {

    @Getter
    protected ItemStack stack;

    public LytItemImage(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    protected String getDisplayName() {
        return stack == null ? "" : stack.getDisplayName();
    }

    @Override
    protected boolean hasContent() {
        return stack != null;
    }

    @Override
    protected boolean isRenderable() {
        return stack != null && stack.stackSize != 0;
    }

    @Override
    protected void renderIcon(RenderContext context, int renderX, int renderY) {
        try {
            if (getScale() == 1f) {
                context.renderItem(stack, renderX, renderY);
            } else {
                GL11.glPushMatrix();
                try {
                    GL11.glTranslatef(renderX, renderY, 0);
                    GL11.glScalef(getScale(), getScale(), 1f);
                    context.renderItem(stack, 0, 0);
                } finally {
                    GL11.glPopMatrix();
                }
            }
        } catch (Throwable t) {
            GuideDisplayItemStacks.warnRenderFailure("LytItemImage", stack, t);
            context.restoreExternalRenderState();
        }
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        return isTooltipVisible() && isRenderable() ? Optional.of(new ItemTooltip(stack)) : Optional.empty();
    }

    public List<ItemStack> getStacks() {
        return stack == null ? List.of() : List.of(stack);
    }
}
