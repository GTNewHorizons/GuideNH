package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.render.GuideIconCapture;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideSiteItemIconExporter implements GuideSiteItemIconResolver, AutoCloseable {

    /**
     * Raster size for exported `item-icons/*.png` (vanilla item GUI draws a
     * 16 x 16 logical tile, scaled to this).
     */
    private static final int ICON_SIZE = 96;

    private final GuideSiteTextureAnimations animations;
    private final Map<String, String> exportedIcons = new LinkedHashMap<>();
    private final GuideIconCapture capture = new GuideIconCapture();

    public GuideSiteItemIconExporter(GuideSiteAssetRegistry assets) {
        this(assets, new GuideSiteTextureAnimations(assets));
    }

    public GuideSiteItemIconExporter(GuideSiteAssetRegistry assets, GuideSiteTextureAnimations animations) {
        this.animations = animations;
    }

    @Override
    public synchronized String exportIcon(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "";
        }

        String cacheKey = cacheKey(stack);
        String cached = exportedIcons.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            String exportedPath = GuideSitePageAssetExporter.ROOT_PREFIX
                + animations.exportRendered("item-icons", () -> renderImage(stack.copy()));
            exportedIcons.put(cacheKey, exportedPath);
            return exportedPath;
        } catch (Throwable t) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [GuideSiteItemIconExporter] Failed to export offline icon for {}",
                GuideSiteItemSupport.itemId(stack),
                t);
            exportedIcons.put(cacheKey, "");
            return "";
        }
    }

    private String cacheKey(ItemStack stack) {
        StringBuilder key = new StringBuilder();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.getLanguageManager() != null
            && minecraft.getLanguageManager()
                .getCurrentLanguage() != null) {
            key.append(
                minecraft.getLanguageManager()
                    .getCurrentLanguage()
                    .getLanguageCode())
                .append('|');
        }
        key.append(GuideSiteItemSupport.itemId(stack))
            .append('#')
            .append(stack.getItemDamage())
            .append('#')
            .append(stack.stackSize);
        if (stack.getTagCompound() != null) {
            key.append('#')
                .append(stack.getTagCompound());
        }
        return key.toString();
    }

    private BufferedImage renderImage(ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null || minecraft.fontRenderer == null) {
            throw new IllegalStateException("Minecraft client is not ready for item icon export.");
        }
        return capture.capture(ICON_SIZE, () -> {
            RenderItem itemRenderer = RenderItem.getInstance();
            float previousZLevel = itemRenderer.zLevel;
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glPushMatrix();
            try {
                float scale = ICON_SIZE / 16f;
                GL11.glScalef(scale, scale, 1f);
                itemRenderer.zLevel = 100f;
                itemRenderer
                    .renderItemAndEffectIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
                itemRenderer
                    .renderItemOverlayIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0, "");
            } finally {
                GL11.glPopMatrix();
                itemRenderer.zLevel = previousZLevel;
                RenderHelper.disableStandardItemLighting();
            }
        });
    }

    @Override
    public synchronized void close() {
        capture.close();
    }
}
