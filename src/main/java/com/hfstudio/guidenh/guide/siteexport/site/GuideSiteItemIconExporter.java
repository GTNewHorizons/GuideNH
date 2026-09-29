package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideSiteItemIconExporter implements GuideSiteItemIconResolver, AutoCloseable {

    /**
     * Raster size for exported `item-icons/*.png` (vanilla item GUI draws a
     * 16 x 16 logical tile, scaled to this).
     */
    private static final int ICON_SIZE = 96;

    private final GuideSiteTextureAnimations animations;
    private final Map<String, String> exportedIcons = new LinkedHashMap<>();
    @Nullable
    private Framebuffer framebuffer;
    @Nullable
    private ByteBuffer pixelBuffer;

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

        int previousDisplayWidth = minecraft.displayWidth;
        int previousDisplayHeight = minecraft.displayHeight;
        int previousGuiScale = minecraft.gameSettings.guiScale;
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousTextureUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        boolean previousLightmapTexture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(previousTextureUnit);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        RenderItem itemRenderer = RenderItem.getInstance();
        float previousZLevel = itemRenderer.zLevel;

        boolean projectionPushed = false;
        boolean modelViewPushed = false;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            Framebuffer framebuffer = framebuffer();
            minecraft.displayWidth = ICON_SIZE;
            minecraft.displayHeight = ICON_SIZE;
            minecraft.gameSettings.guiScale = 1;

            framebuffer.bindFramebuffer(true);
            GL11.glViewport(0, 0, ICON_SIZE, ICON_SIZE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            projectionPushed = true;
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, ICON_SIZE, ICON_SIZE, 0.0D, 1000.0D, 3000.0D);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            modelViewPushed = true;
            GL11.glLoadIdentity();
            GL11.glTranslatef(0.0F, 0.0F, -2000.0F);

            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());

            // Set GUI lights before raster scaling so item normals receive standard GUI lighting.
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            float scale = ICON_SIZE / 16f;
            float origin = 0f;
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(origin, origin, 0f);
                GL11.glScalef(scale, scale, 1f);

                itemRenderer.zLevel = 100f;
                itemRenderer
                    .renderItemAndEffectIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
                itemRenderer
                    .renderItemOverlayIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
            } finally {
                GL11.glPopMatrix();
                RenderHelper.disableStandardItemLighting();
            }

            return readPixels();
        } finally {
            if (modelViewPushed) {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            }
            if (projectionPushed) {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
            }

            itemRenderer.zLevel = previousZLevel;
            minecraft.displayWidth = previousDisplayWidth;
            minecraft.displayHeight = previousDisplayHeight;
            minecraft.gameSettings.guiScale = previousGuiScale;
            OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            if (previousLightmapTexture) {
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            } else {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }
            OpenGlHelper.setActiveTexture(previousTextureUnit);
            GL11.glMatrixMode(previousMatrixMode);
        }
    }

    private Framebuffer framebuffer() {
        if (framebuffer == null) {
            framebuffer = new Framebuffer(ICON_SIZE, ICON_SIZE, true);
            framebuffer.setFramebufferColor(0f, 0f, 0f, 0f);
        }
        return framebuffer;
    }

    private BufferedImage readPixels() {
        if (pixelBuffer == null) {
            pixelBuffer = BufferUtils.createByteBuffer(ICON_SIZE * ICON_SIZE * 4);
        }
        ByteBuffer buffer = pixelBuffer;
        buffer.clear();
        GL11.glReadPixels(0, 0, ICON_SIZE, ICON_SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) image.getRaster()
            .getDataBuffer()).getData();
        for (int y = 0; y < ICON_SIZE; y++) {
            int flippedY = ICON_SIZE - 1 - y;
            for (int x = 0; x < ICON_SIZE; x++) {
                int index = (x + y * ICON_SIZE) * 4;
                int red = buffer.get(index) & 0xFF;
                int green = buffer.get(index + 1) & 0xFF;
                int blue = buffer.get(index + 2) & 0xFF;
                int alpha = buffer.get(index + 3) & 0xFF;
                pixels[x + flippedY * ICON_SIZE] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        }
        return image;
    }

    @Override
    public synchronized void close() {
        if (framebuffer != null) {
            framebuffer.deleteFramebuffer();
            framebuffer = null;
        }
        pixelBuffer = null;
    }
}
