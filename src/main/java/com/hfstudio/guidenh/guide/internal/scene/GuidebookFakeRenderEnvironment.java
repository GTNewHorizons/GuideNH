package com.hfstudio.guidenh.guide.internal.scene;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GuidebookFakeRenderEnvironment implements AutoCloseable {

    /** Weak cache: the player points back to its preview WorldClient and must not keep old scenes alive. */
    public static WeakReference<GuidebookPreviewPlayer> cachedPreviewPlayer = new WeakReference<>(null);
    public static NetHandlerPlayClient cachedNetHandler;
    private static final ThreadLocal<ArrayDeque<GuidebookFakeRenderEnvironment>> ENVIRONMENT_POOL = ThreadLocal
        .withInitial(ArrayDeque::new);

    private Minecraft minecraft;
    private EntityClientPlayerMP previousPlayer;
    private EntityLivingBase previousRenderViewEntity;
    private Entity previousPointedEntity;
    private WorldClient previousWorld;
    private final RenderManagerState renderManagerState = new RenderManagerState();
    private final TileEntityDispatcherState tileEntityDispatcherState = new TileEntityDispatcherState();
    private boolean active;

    private void enterScope(GuidebookLevel level, CameraSettings camera, float partialTicks) {
        this.minecraft = Minecraft.getMinecraft();
        this.previousWorld = minecraft.theWorld;
        this.previousPlayer = minecraft.thePlayer;
        this.previousRenderViewEntity = minecraft.renderViewEntity;
        this.previousPointedEntity = minecraft.pointedEntity;
        this.renderManagerState.capture(RenderManager.instance);
        this.tileEntityDispatcherState.capture(TileEntityRendererDispatcher.instance);
        this.active = true;

        WorldClient fakeWorld = getOrCreateClientWorld(level);
        GuidebookPreviewPlayer previewPlayer = getOrCreatePreviewPlayer(minecraft, fakeWorld);
        previewPlayer.syncToPreviewWorld(fakeWorld, level, camera);

        minecraft.theWorld = fakeWorld;
        minecraft.thePlayer = previewPlayer;
        minecraft.renderViewEntity = previewPlayer;
        minecraft.pointedEntity = null;

        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        if (!level.getTileEntities()
            .isEmpty()) {
            dispatcher.cacheActiveRenderInfo(
                fakeWorld,
                minecraft.getTextureManager(),
                minecraft.fontRenderer,
                previewPlayer,
                partialTicks);
        }

        RenderManager renderManager = RenderManager.instance;
        renderManager.cacheActiveRenderInfo(
            fakeWorld,
            minecraft.getTextureManager(),
            minecraft.fontRenderer,
            previewPlayer,
            null,
            minecraft.gameSettings,
            partialTicks);

        double viewerX = previewPlayer.lastTickPosX + (previewPlayer.posX - previewPlayer.lastTickPosX) * partialTicks;
        double viewerY = previewPlayer.lastTickPosY + (previewPlayer.posY - previewPlayer.lastTickPosY) * partialTicks;
        double viewerZ = previewPlayer.lastTickPosZ + (previewPlayer.posZ - previewPlayer.lastTickPosZ) * partialTicks;
        TileEntityRendererDispatcher.staticPlayerX = viewerX;
        TileEntityRendererDispatcher.staticPlayerY = viewerY;
        TileEntityRendererDispatcher.staticPlayerZ = viewerZ;
        RenderManager.renderPosX = viewerX;
        RenderManager.renderPosY = viewerY;
        RenderManager.renderPosZ = viewerZ;
    }

    public static GuidebookFakeRenderEnvironment enter(GuidebookLevel level, CameraSettings camera,
        float partialTicks) {
        GuidebookPreviewPlayerRenderer.ensureRegistered();
        ArrayDeque<GuidebookFakeRenderEnvironment> pool = ENVIRONMENT_POOL.get();
        GuidebookFakeRenderEnvironment environment = pool.pollFirst();
        if (environment == null) {
            environment = new GuidebookFakeRenderEnvironment();
        }
        environment.enterScope(level, camera, partialTicks);
        return environment;
    }

    public static WorldClient getOrCreateClientWorld(GuidebookLevel level) {
        World world = level.getOrCreateFakeWorld();
        if (!(world instanceof WorldClient clientWorld)) {
            throw new IllegalStateException("Guidebook preview world must be a client world.");
        }
        return clientWorld;
    }

    @Override
    public void close() {
        if (!active) {
            return;
        }
        minecraft.theWorld = previousWorld;
        minecraft.thePlayer = previousPlayer;
        minecraft.renderViewEntity = previousRenderViewEntity;
        minecraft.pointedEntity = previousPointedEntity;

        renderManagerState.restore(RenderManager.instance);
        tileEntityDispatcherState.restore(TileEntityRendererDispatcher.instance);
        clearCapturedState();
        ENVIRONMENT_POOL.get()
            .addFirst(this);
    }

    private void clearCapturedState() {
        previousWorld = null;
        previousPlayer = null;
        previousRenderViewEntity = null;
        previousPointedEntity = null;
        minecraft = null;
        renderManagerState.clear();
        tileEntityDispatcherState.clear();
        active = false;
    }

    public static GuidebookPreviewPlayer getOrCreatePreviewPlayer(Minecraft minecraft, WorldClient world) {
        NetHandlerPlayClient netHandler = minecraft.getNetHandler();
        if (netHandler == null) {
            throw new IllegalStateException("Guidebook preview requires an active client world");
        }
        GuidebookPreviewPlayer previewPlayer = cachedPreviewPlayer.get();
        if (previewPlayer == null || cachedNetHandler != netHandler) {
            previewPlayer = new GuidebookPreviewPlayer(minecraft, world, netHandler);
            cachedPreviewPlayer = new WeakReference<>(previewPlayer);
            cachedNetHandler = netHandler;
        }
        return previewPlayer;
    }

    public static void releaseCachedPreviewPlayer(WorldClient world) {
        GuidebookPreviewPlayer previewPlayer = cachedPreviewPlayer.get();
        if (previewPlayer == null || previewPlayer.worldObj == world) {
            cachedPreviewPlayer.clear();
            cachedNetHandler = null;
        }
    }

    /** Clears process-wide preview-player state during a client world transition. */
    public static void clearClientRuntimeCaches() {
        GuidebookPreviewPlayer previewPlayer = cachedPreviewPlayer.get();
        if (previewPlayer != null) {
            // A caller may still hold the player while the event is dispatched. Detach its
            // back-reference explicitly instead of relying solely on clearing the weak cache.
            previewPlayer.worldObj = null;
        }
        cachedPreviewPlayer.clear();
        cachedNetHandler = null;
    }

    public static class RenderManagerState {

        private World world;
        private EntityLivingBase livingPlayer;
        private Entity field147941I;
        private float playerViewY;
        private float playerViewX;
        private double viewerPosX;
        private double viewerPosY;
        private double viewerPosZ;
        private double renderPosX;
        private double renderPosY;
        private double renderPosZ;

        private void capture(RenderManager renderManager) {
            this.world = renderManager.worldObj;
            this.livingPlayer = renderManager.livingPlayer;
            this.field147941I = renderManager.field_147941_i;
            this.playerViewY = renderManager.playerViewY;
            this.playerViewX = renderManager.playerViewX;
            this.viewerPosX = renderManager.viewerPosX;
            this.viewerPosY = renderManager.viewerPosY;
            this.viewerPosZ = renderManager.viewerPosZ;
            this.renderPosX = RenderManager.renderPosX;
            this.renderPosY = RenderManager.renderPosY;
            this.renderPosZ = RenderManager.renderPosZ;
        }

        private void restore(RenderManager renderManager) {
            renderManager.set(world);
            renderManager.livingPlayer = livingPlayer;
            renderManager.field_147941_i = field147941I;
            renderManager.playerViewY = playerViewY;
            renderManager.playerViewX = playerViewX;
            renderManager.viewerPosX = viewerPosX;
            renderManager.viewerPosY = viewerPosY;
            renderManager.viewerPosZ = viewerPosZ;
            RenderManager.renderPosX = renderPosX;
            RenderManager.renderPosY = renderPosY;
            RenderManager.renderPosZ = renderPosZ;
        }

        private void clear() {
            world = null;
            livingPlayer = null;
            field147941I = null;
        }
    }

    public static class TileEntityDispatcherState {

        private World world;
        private EntityLivingBase livingPlayer;
        private float rotationYaw;
        private float rotationPitch;
        private double viewerPosX;
        private double viewerPosY;
        private double viewerPosZ;
        private double staticPlayerX;
        private double staticPlayerY;
        private double staticPlayerZ;

        private void capture(TileEntityRendererDispatcher dispatcher) {
            this.world = dispatcher.field_147550_f;
            this.livingPlayer = dispatcher.field_147551_g;
            this.rotationYaw = dispatcher.field_147562_h;
            this.rotationPitch = dispatcher.field_147563_i;
            this.viewerPosX = dispatcher.field_147560_j;
            this.viewerPosY = dispatcher.field_147561_k;
            this.viewerPosZ = dispatcher.field_147558_l;
            this.staticPlayerX = TileEntityRendererDispatcher.staticPlayerX;
            this.staticPlayerY = TileEntityRendererDispatcher.staticPlayerY;
            this.staticPlayerZ = TileEntityRendererDispatcher.staticPlayerZ;
        }

        private void restore(TileEntityRendererDispatcher dispatcher) {
            if (dispatcher.field_147550_f != world) {
                dispatcher.func_147543_a(world);
            }
            dispatcher.field_147551_g = livingPlayer;
            dispatcher.field_147562_h = rotationYaw;
            dispatcher.field_147563_i = rotationPitch;
            dispatcher.field_147560_j = viewerPosX;
            dispatcher.field_147561_k = viewerPosY;
            dispatcher.field_147558_l = viewerPosZ;
            TileEntityRendererDispatcher.staticPlayerX = staticPlayerX;
            TileEntityRendererDispatcher.staticPlayerY = staticPlayerY;
            TileEntityRendererDispatcher.staticPlayerZ = staticPlayerZ;
        }

        private void clear() {
            world = null;
            livingPlayer = null;
        }
    }
}
