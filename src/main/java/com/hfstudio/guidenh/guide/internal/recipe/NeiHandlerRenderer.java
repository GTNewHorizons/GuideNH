package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.RecipeSlot;

/**
 * Invokes registered recipe handlers to render a recipe in-place. Relies on provider-side
 * drawBackground/drawForeground/drawExtras calls being independent of GUI state. All calls are
 * wrapped in try-catch so a misbehaving third-party handler cannot crash the guide UI.
 */
public class NeiHandlerRenderer {

    public static final RenderItem ITEM_RENDERER = new RenderItem();

    private NeiHandlerRenderer() {}

    /**
     * Render the background / foreground / extras for {@code handler} at screen offset
     * {@code (screenX, screenY)} and draw every positioned stack as a 16x16 item on top. Returns
     * the stack under {@code (mouseX, mouseY)} if any, else {@code null}.
     */
    public static @Nullable ItemStack render(Object handler, int recipeIndex, int screenX, int screenY, int clipX,
        int clipY, int clipWidth, int clipHeight, int mouseX, int mouseY) {
        return render(
            handler,
            recipeIndex,
            screenX,
            screenY,
            clipX,
            clipY,
            clipWidth,
            clipHeight,
            mouseX,
            mouseY,
            false);
    }

    /**
     * @param skipForeground when {@code true}, skips {@code drawForeground} and {@code drawExtras}.
     *                       Pass {@code true} for handlers whose {@code getOtherStacks} is known to
     *                       throw; those methods call GTNH-NEI's safe-wrapper internally, which
     *                       would log "Error in getOtherStacks" spam on every rendered frame.
     */
    public static @Nullable ItemStack render(Object handler, int recipeIndex, int screenX, int screenY, int clipX,
        int clipY, int clipWidth, int clipHeight, int mouseX, int mouseY, boolean skipForeground) {
        return render(
            handler,
            recipeIndex,
            screenX,
            screenY,
            clipX,
            clipY,
            clipWidth,
            clipHeight,
            mouseX,
            mouseY,
            skipForeground,
            null,
            null,
            null,
            false);
    }

    static @Nullable ItemStack render(Object handler, int recipeIndex, int screenX, int screenY, int clipX, int clipY,
        int clipWidth, int clipHeight, int mouseX, int mouseY, boolean skipForeground,
        @Nullable List<RecipeSlot> cachedIngredients, @Nullable List<RecipeSlot> cachedOthers,
        @Nullable RecipeSlot cachedResult, boolean cachedResultAvailable) {
        GuideNhIntegrationRegistry registry = GuideNhIntegrationRegistry.global();
        if (handler == null || !registry.canRenderRecipeHandler(handler)) return null;

        // DiagramGroup is rendered from LytNeiRecipeBox with absolute-GUI scissors (tooltip-safe).

        // Phase 1: NEI-native background + foreground + extras at translated origin.
        // drawForeground and drawExtras are skipped when getOtherStacks is broken, because
        // GTNH-NEI calls getOtherStacks inside its own safe-wrapper from those methods, which
        // would log errors on every render frame.
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_SCISSOR_BIT
                | GL11.GL_TRANSFORM_BIT
                | GL11.GL_VIEWPORT_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(screenX, screenY, 0f);
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());
            registry.renderRecipeHandler(handler, recipeIndex, skipForeground);
        } catch (Throwable ignored) {} finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        // Phase 2: draw every positioned stack on top.
        List<RecipeSlot> ingredients = cachedIngredients != null ? cachedIngredients
            : registry.readRecipeIngredientSlots(handler, recipeIndex);
        List<RecipeSlot> others = skipForeground ? List.of()
            : cachedOthers != null ? cachedOthers : registry.readRecipeOtherSlots(handler, recipeIndex);
        RecipeSlot result = cachedResultAvailable ? cachedResult : registry.readRecipeResultSlot(handler, recipeIndex);
        return drawSlotBatch(ingredients, others, result, screenX, screenY, mouseX, mouseY);
    }

    private static @Nullable ItemStack drawSlotBatch(List<RecipeSlot> ingredients, List<RecipeSlot> others,
        @Nullable RecipeSlot result, int screenX, int screenY, int mouseX, int mouseY) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        ItemStack hovered = null;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            GL11.glDisable(GL11.GL_BLEND);
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());
            RenderHelper.enableGUIStandardItemLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            ITEM_RENDERER.zLevel = 100f;
            hovered = drawSlotBatchGroup(ingredients, screenX, screenY, mouseX, mouseY, mc, hovered);
            hovered = drawSlotBatchGroup(others, screenX, screenY, mouseX, mouseY, mc, hovered);
            if (result != null) {
                hovered = drawSlotBatchSlot(result, screenX, screenY, mouseX, mouseY, mc, hovered);
            }
        } finally {
            ITEM_RENDERER.zLevel = 0f;
            RenderHelper.disableStandardItemLighting();
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());
        }
        return hovered;
    }

    private static @Nullable ItemStack drawSlotBatchGroup(List<RecipeSlot> slots, int screenX, int screenY, int mouseX,
        int mouseY, Minecraft mc, @Nullable ItemStack currentHovered) {
        ItemStack hovered = currentHovered;
        for (RecipeSlot slot : slots) {
            hovered = drawSlotBatchSlot(slot, screenX, screenY, mouseX, mouseY, mc, hovered);
        }
        return hovered;
    }

    private static @Nullable ItemStack drawSlotBatchSlot(RecipeSlot slot, int screenX, int screenY, int mouseX,
        int mouseY, Minecraft mc, @Nullable ItemStack currentHovered) {
        ItemStack shown = pickVisibleStack(slot);
        if (shown == null) return currentHovered;
        try {
            ITEM_RENDERER.renderItemAndEffectIntoGUI(
                mc.fontRenderer,
                mc.getTextureManager(),
                shown,
                screenX + slot.x(),
                screenY + slot.y());
            if (shown.stackSize > 0) {
                ITEM_RENDERER.renderItemOverlayIntoGUI(
                    mc.fontRenderer,
                    mc.getTextureManager(),
                    shown,
                    screenX + slot.x(),
                    screenY + slot.y());
            }
        } catch (Throwable t) {
            GuideDisplayItemStacks.warnRenderFailure("NeiHandlerRenderer", shown, t);
        }
        return isOver(screenX + slot.x(), screenY + slot.y(), mouseX, mouseY) ? shown : currentHovered;
    }

    public static @Nullable ItemStack pickVisibleStack(RecipeSlot s) {
        if (s == null) {
            return null;
        }
        ItemStack current = s.currentStack();
        if (current != null && current.stackSize > 0) {
            return current;
        }
        if (s.stacks() == null || s.stacks()
            .isEmpty()) return null;
        ItemStack zeroCountFallback = null;
        for (int i = 0, n = s.stacks()
            .size(); i < n; i++) {
            ItemStack st = s.stacks()
                .get(i);
            if (st == null) continue;
            if (st.stackSize > 0) return st;
            if (zeroCountFallback == null) zeroCountFallback = st;
        }
        return zeroCountFallback;
    }

    public static boolean isOver(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    /**
     * Draws item icon + count overlay. For stacks with count=0, skips the overlay to avoid
     * rendering an ugly "0" label; use {@link #drawItemIcon} to always suppress the overlay.
     */
    public static void drawItem(ItemStack stack, int x, int y) {
        drawItemInternal(stack, x, y, true);
    }

    /** Draws item icon + count overlay only when count > 0; icon-only for count=0 items. */
    public static void drawStackWithCount(ItemStack stack, int x, int y) {
        drawItemInternal(stack, x, y, stack.stackSize > 0);
    }

    public static void drawItemIcon(ItemStack stack, int x, int y) {
        drawItemInternal(stack, x, y, false);
    }

    private static void drawItemInternal(ItemStack stack, int x, int y, boolean drawOverlay) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        try {
            GL11.glDisable(GL11.GL_BLEND);
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());
            RenderHelper.enableGUIStandardItemLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            ITEM_RENDERER.zLevel = 100f;
            ITEM_RENDERER.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            if (drawOverlay) {
                ITEM_RENDERER.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            }
            ITEM_RENDERER.zLevel = 0f;
            RenderHelper.disableStandardItemLighting();
        } catch (Throwable t) {
            GuideDisplayItemStacks.warnRenderFailure("NeiHandlerRenderer", stack, t);
        } finally {
            ITEM_RENDERER.zLevel = 0f;
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());
        }
    }
}
