package com.hfstudio.guidenh.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.blockpos.IBlockPos;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.item.RegionWandExporter;
import com.hfstudio.guidenh.guide.internal.item.RegionWandExporter.SelectionAction;
import com.hfstudio.guidenh.guide.internal.item.RegionWandSelection;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RegionWandRenderer {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) return;
        ItemStack heldWand = getHeldRegionWand(player);
        boolean exportEnabled = ModConfig.ui.sceneExportEnabled;
        boolean holdingWand = heldWand != null && exportEnabled;
        IBlockPos p1 = RegionWandSelection.getPosition(1);
        IBlockPos p2 = RegionWandSelection.getPosition(2);
        boolean renderSelection = (p1 != null || p2 != null) && exportEnabled
            && (holdingWand || ModConfig.ui.regionWandPersistentSelectionRender);
        int[] target = holdingWand ? resolveTarget(mc, player) : null;
        if (!renderSelection && target == null) return;

        float partialTicks = event.partialTicks;
        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(-camX, -camY, -camZ);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2f);

        if (renderSelection && p1 != null) {
            drawBox(p1.getX(), p1.getY(), p1.getZ(), p1.getX() + 1, p1.getY() + 1, p1.getZ() + 1, 1f, 0.2f, 0.2f, 1f);
        }
        if (renderSelection && p2 != null) {
            drawBox(p2.getX(), p2.getY(), p2.getZ(), p2.getX() + 1, p2.getY() + 1, p2.getZ() + 1, 0.2f, 0.4f, 1f, 1f);
        }
        if (renderSelection && p1 != null && p2 != null) {
            int minX = Math.min(p1.getX(), p2.getX());
            int minY = Math.min(p1.getY(), p2.getY());
            int minZ = Math.min(p1.getZ(), p2.getZ());
            int maxX = Math.max(p1.getX(), p2.getX()) + 1;
            int maxY = Math.max(p1.getY(), p2.getY()) + 1;
            int maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;
            drawBox(minX, minY, minZ, maxX, maxY, maxZ, 0f, 0.79f, 0.95f, 0.9f); // #00CAF2
            drawFilled(minX, minY, minZ, maxX, maxY, maxZ, 0f, 0.79f, 0.95f, 0.15f);
        }
        if (target != null) {
            drawTargetPreview(target[0], target[1], target[2]);
        }

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();
    }

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        if (!event.buttonstate || (event.button != 0 && event.button != 1)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        EntityClientPlayerMP player = mc.thePlayer;
        ItemStack heldWand = getHeldRegionWand(player);
        if (heldWand == null) {
            return;
        }
        if (!ModConfig.ui.sceneExportEnabled) {
            return;
        }

        event.setCanceled(true);
        if (event.button == 0) {
            if (player.isSneaking()) {
                RegionWandExporter.applySelectionAction(player, SelectionAction.CLEAR, 0, 0, 0);
                return;
            }
            int[] target = resolveTarget(mc, player);
            if (target != null) {
                RegionWandExporter.applySelectionAction(player, SelectionAction.POS1, target[0], target[1], target[2]);
            }
            return;
        }

        if (player.isSneaking()) {
            if (RegionWandExporter.beginClientExportAction()) {
                RegionWandExporter.exportToClipboard(player, mc.theWorld);
            }
            return;
        }
        int[] target = resolveTarget(mc, player);
        if (target != null) {
            RegionWandExporter.applySelectionAction(player, SelectionAction.POS2, target[0], target[1], target[2]);
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (!RegionWandSelection.isBound(event.itemStack)) {
            return;
        }
        event.toolTip.add(1, GuidebookText.RegionWandTooltipBound.text());
        event.toolTip.add(GuidebookText.RegionWandTooltipSelect.text());
        event.toolTip.add(GuidebookText.RegionWandTooltipExport.text());
        event.toolTip.add(
            GuidebookText.RegionWandTooltipMode.text(
                RegionWandExporter.getExportMode()
                    .getDisplayName()));
        appendPosition(event.toolTip, 1);
        appendPosition(event.toolTip, 2);
    }

    private static ItemStack getHeldRegionWand(EntityClientPlayerMP player) {
        ItemStack held = player.getHeldItem();
        if (RegionWandSelection.isBound(held)) {
            return held;
        }
        return null;
    }

    private static void appendPosition(List<String> tooltip, int which) {
        IBlockPos position = RegionWandSelection.getPosition(which);
        if (position != null) {
            tooltip
                .add(GuidebookText.RegionWandTooltipPos.text(which, position.getX(), position.getY(), position.getZ()));
        }
    }

    private static int[] resolveTarget(Minecraft mc, EntityClientPlayerMP player) {
        return RegionWandExporter.resolveLookingAtSelection(player, mc.theWorld, true, getReachDistance(mc));
    }

    private static double getReachDistance(Minecraft mc) {
        if (mc != null && mc.playerController != null) {
            return mc.playerController.getBlockReachDistance();
        }
        return RegionWandExporter.DEFAULT_REACH_DISTANCE;
    }

    private static void drawTargetPreview(int x, int y, int z) {
        drawBox(x, y, z, x + 1, y + 1, z + 1, 1f, 0.9f, 0.1f, 0.9f);
        double cx = x + 0.5D;
        double cy = y + 0.5D;
        double cz = z + 0.5D;
        double radius = 0.72D;

        GL11.glLineWidth(3f);
        GL11.glBegin(GL11.GL_LINES);
        ColorUtils.applyGlColor(ColorUtils.REGION_X_AXIS.getColor());
        line(cx - radius, cy, cz, cx + radius, cy, cz);
        ColorUtils.applyGlColor(ColorUtils.REGION_Y_AXIS.getColor());
        line(cx, cy - radius, cz, cx, cy + radius, cz);
        ColorUtils.applyGlColor(ColorUtils.REGION_Z_AXIS.getColor());
        line(cx, cy, cz - radius, cx, cy, cz + radius);
        GL11.glEnd();
        GL11.glLineWidth(2f);
    }

    public static void drawBox(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g,
        float b, float a) {
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_LINES);
        line(x0, y0, z0, x1, y0, z0);
        line(x1, y0, z0, x1, y0, z1);
        line(x1, y0, z1, x0, y0, z1);
        line(x0, y0, z1, x0, y0, z0);
        line(x0, y1, z0, x1, y1, z0);
        line(x1, y1, z0, x1, y1, z1);
        line(x1, y1, z1, x0, y1, z1);
        line(x0, y1, z1, x0, y1, z0);
        line(x0, y0, z0, x0, y1, z0);
        line(x1, y0, z0, x1, y1, z0);
        line(x1, y0, z1, x1, y1, z1);
        line(x0, y0, z1, x0, y1, z1);
        GL11.glEnd();
    }

    public static void line(double x0, double y0, double z0, double x1, double y1, double z1) {
        GL11.glVertex3d(x0, y0, z0);
        GL11.glVertex3d(x1, y1, z1);
    }

    public static void drawFilled(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g,
        float b, float a) {
        GL11.glColor4f(r, g, b, a);
        // Disable face culling so the translucent faces are visible both from outside and from
        // inside the region (the camera can enter the box when the player stands within it).
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBegin(GL11.GL_QUADS);
        // -Y
        GL11.glVertex3d(x0, y0, z0);
        GL11.glVertex3d(x1, y0, z0);
        GL11.glVertex3d(x1, y0, z1);
        GL11.glVertex3d(x0, y0, z1);
        // +Y
        GL11.glVertex3d(x0, y1, z0);
        GL11.glVertex3d(x0, y1, z1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x1, y1, z0);
        // -Z
        GL11.glVertex3d(x0, y0, z0);
        GL11.glVertex3d(x0, y1, z0);
        GL11.glVertex3d(x1, y1, z0);
        GL11.glVertex3d(x1, y0, z0);
        // +Z
        GL11.glVertex3d(x0, y0, z1);
        GL11.glVertex3d(x1, y0, z1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x0, y1, z1);
        // -X
        GL11.glVertex3d(x0, y0, z0);
        GL11.glVertex3d(x0, y0, z1);
        GL11.glVertex3d(x0, y1, z1);
        GL11.glVertex3d(x0, y1, z0);
        // +X
        GL11.glVertex3d(x1, y0, z0);
        GL11.glVertex3d(x1, y1, z0);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x1, y0, z1);
        GL11.glEnd();
        if (cullWasEnabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }
}
