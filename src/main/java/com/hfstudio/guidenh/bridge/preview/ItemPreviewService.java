package com.hfstudio.guidenh.bridge.preview;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.bridge.protocol.BridgeProtocolLimits;
import com.hfstudio.guidenh.client.GuideClientTasks;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipLines;
import com.hfstudio.guidenh.guide.render.GuideIconCapture;
import com.hfstudio.guidenh.guide.scene.ponder.PonderNbtPath;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteItemSupport;

public class ItemPreviewService implements AutoCloseable {

    private static final int DEFAULT_ICON_SIZE = 64;
    private static final int LARGE_ICON_SIZE = 128;
    private static final int RENDER_TIMEOUT_SECONDS = 5;

    private final GuideIconCapture capture = new GuideIconCapture();
    private final ItemPreviewCache cache;
    private final BridgeProtocolLimits limits;

    public ItemPreviewService(ItemPreviewCache cache, BridgeProtocolLimits limits) {
        this.cache = cache;
        this.limits = limits;
    }

    public PreviewResolveResult resolve(PreviewResolveQuery query) {
        if (!"items".equals(query.getCapability())) {
            throw new IllegalArgumentException("Unknown preview capability");
        }

        ItemStack stack = resolveItemStack(query);
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("Unknown item id");
        }

        ItemPreviewCacheKey cacheKey = new ItemPreviewCacheKey(
            query.getCapability(),
            query.getId(),
            stack.getItemDamage(),
            Math.max(1, query.getCount()),
            normalizedNbtText(stack.stackTagCompound),
            query.getRenderVariant());
        ItemPreviewPayload cached = cache.get(cacheKey);
        if (cached != null) {
            return cached.toResult(query.getCapability());
        }

        ItemPreviewPayload payload = renderOnClientThread(cacheKey, stack.copy());
        cache.put(cacheKey, payload);
        return payload.toResult(query.getCapability());
    }

    private ItemStack resolveItemStack(PreviewResolveQuery query) {
        ItemStack stack = IdUtils.resolveItemStack(query.getId(), "minecraft");
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        stack.stackSize = Math.max(1, query.getCount());
        String nbt = query.getNbt();
        if (!nbt.isEmpty()) {
            try {
                NBTTagCompound parsedNbt = PonderNbtPath.parseCompound(nbt);
                stack.stackTagCompound = parsedNbt;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid preview NBT", e);
            }
        }
        return stack;
    }

    private ItemPreviewPayload renderOnClientThread(ItemPreviewCacheKey cacheKey, ItemStack stack) {
        return GuideClientTasks.call(() -> createPayload(cacheKey, stack, Minecraft.getMinecraft()));
    }

    private ItemPreviewPayload createPayload(ItemPreviewCacheKey cacheKey, ItemStack stack, Minecraft minecraft) {
        int iconSize = resolveIconSize(cacheKey.getRenderVariant());
        byte[] iconBytes = renderPng(stack, iconSize, minecraft);
        String iconPngBase64 = Base64.getEncoder()
            .encodeToString(iconBytes);
        List<String> tooltipLines = sanitizeTooltipLines(
            GuideItemTooltipLines.build(new ItemTooltip(stack), minecraft));
        String displayName = GuideSiteItemSupport.displayName(stack);
        String resolvedId = GuideSiteItemSupport.itemId(stack);
        String detail = buildDetail(resolvedId, stack.getItemDamage());
        return new ItemPreviewPayload(
            cacheKey.toPreviewKey(),
            cacheKey.getId(),
            displayName,
            detail,
            stack.getItemDamage(),
            stack.stackSize,
            normalizedNbtText(stack.stackTagCompound),
            tooltipLines,
            iconPngBase64,
            iconSize,
            iconSize);
    }

    private int resolveIconSize(String renderVariant) {
        int requestedSize = "picker".equalsIgnoreCase(renderVariant) ? LARGE_ICON_SIZE : DEFAULT_ICON_SIZE;
        int maxEdge = (int) Math.sqrt(Math.max(16, limits.getMaxPreviewIconPixels()));
        return Math.clamp(requestedSize, 16, maxEdge);
    }

    private byte[] renderPng(ItemStack stack, int iconSize, Minecraft minecraft) {
        BufferedImage image = capture.capture(iconSize, () -> {
            RenderItem renderer = RenderItem.getInstance();
            float previousZ = renderer.zLevel;
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glPushMatrix();
            try {
                float scale = (iconSize - 2f) / 16f;
                GL11.glTranslatef(1, 1, 0);
                GL11.glScalef(scale, scale, 1);
                renderer.zLevel = 100;
                renderer.renderItemAndEffectIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
                renderer.renderItemOverlayIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
            } finally {
                renderer.zLevel = previousZ;
                GL11.glPopMatrix();
                RenderHelper.disableStandardItemLighting();
            }
        });
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode item preview", exception);
        }
    }

    @Override
    public void close() {
        GuideClientTasks.call(() -> {
            capture.close();
            return null;
        });
    }

    private List<String> sanitizeTooltipLines(List<String> tooltipLines) {
        List<String> sanitizedLines = new ArrayList<>();
        int maxLines = limits.getMaxPreviewTooltipLines();
        for (String tooltipLine : tooltipLines) {
            if (sanitizedLines.size() >= maxLines) {
                break;
            }
            sanitizedLines.add(tooltipLine == null ? "" : tooltipLine);
        }
        if (sanitizedLines.isEmpty()) {
            sanitizedLines.add(EnumChatFormatting.WHITE + "Unknown item");
        }
        return sanitizedLines;
    }

    private String buildDetail(String resolvedId, int meta) {
        if (resolvedId == null || resolvedId.isEmpty()) {
            return "";
        }
        return resolvedId + ":" + Math.max(meta, 0);
    }

    private String normalizedNbtText(NBTTagCompound tagCompound) {
        return tagCompound == null ? "" : tagCompound.toString();
    }
}
