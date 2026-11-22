package com.hfstudio.guidenh.client.capture;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiOpenEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureGuiRenderer;
import com.hfstudio.guidenh.client.capture.api.CaptureInputEvent;
import com.hfstudio.guidenh.client.capture.api.CaptureInputOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureSessionState;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CaptureExportWorker;
import com.hfstudio.guidenh.client.capture.export.CaptureRowComposer;
import com.hfstudio.guidenh.client.capture.geometry.CaptureCoordinateMapper;
import com.hfstudio.guidenh.client.capture.render.CaptureHighResolutionJob;
import com.hfstudio.guidenh.client.capture.render.CaptureRenderScope;
import com.hfstudio.guidenh.client.capture.render.GlCapturePixelReader;
import com.hfstudio.guidenh.client.capture.render.GuiLayerCaptureSource;
import com.hfstudio.guidenh.client.capture.render.GuiTileCaptureSource;
import com.hfstudio.guidenh.client.capture.session.CaptureSession;
import com.hfstudio.guidenh.client.capture.ui.CaptureOverlay;
import com.hfstudio.guidenh.client.hotkey.OpenGuiCaptureHotkey;
import com.hfstudio.guidenh.integration.capture.VanillaCaptureGeometryProvider;
import com.hfstudio.guidenh.integration.capture.VanillaCaptureGuiRenderer;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class CaptureRuntime {

    private static final CaptureRuntime INSTANCE = new CaptureRuntime();
    private final CaptureRegistry registry = new CaptureRegistry();
    private final GuiLayerCaptureSource guiLayer = new GuiLayerCaptureSource();
    private final GlCapturePixelReader pixels = new GlCapturePixelReader();
    private final CaptureExportWorker exporter = new CaptureExportWorker();
    private volatile CaptureSession session;
    private CaptureOverlay overlay;
    private long frameId;
    private float partialTicks;
    private boolean frameRequested;
    private boolean previousKeyRepeat;
    private final Queue<CaptureInputEvent> inputEvents = new ConcurrentLinkedQueue<>();
    private volatile boolean activationRequested;
    private volatile boolean guiAvailable;
    private boolean nativeKeyboard;
    private final GuiTileCaptureSource tileSource = new GuiTileCaptureSource();
    private CaptureHighResolutionJob highResolution;
    private boolean pendingSave;
    private boolean drawingTile;
    private int sourceMouseX;
    private int sourceMouseY;

    public static CaptureRuntime instance() {
        return INSTANCE;
    }

    public CaptureRegistry registry() {
        return registry;
    }

    public void initialize() {
        registry.registerGeometry(new VanillaCaptureGeometryProvider());
        registry.registerRenderer(new VanillaCaptureGuiRenderer());
        guiAvailable = Minecraft.getMinecraft().currentScreen != null;
    }

    public void useNativeKeyboard() {
        nativeKeyboard = true;
    }

    public boolean guiAvailable() {
        return guiAvailable;
    }

    public boolean inputCaptured() {
        return session != null || activationRequested;
    }

    public void requestOpen() {
        activationRequested = true;
    }

    public void queueInput(CaptureInputEvent event) {
        inputEvents.add(event);
    }

    public boolean active() {
        return session != null;
    }

    public void open() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (session != null || minecraft.currentScreen == null) return;
        previousKeyRepeat = Keyboard.areRepeatEventsEnabled();
        Keyboard.enableRepeatEvents(true);
        session = new CaptureSession(minecraft.currentScreen, CaptureCoordinateMapper.current(minecraft));
        overlay = new CaptureOverlay(
            minecraft,
            session,
            new CaptureInputOptions(
                OpenGuiCaptureHotkey.getHotkey()
                    .getKeyCode(),
                nativeKeyboard),
            this::save,
            this::close);
        frameRequested = true;
        registry.opened(new CaptureContext(session.sourceScreen(), session.viewport(), frameId, partialTicks));
    }

    public void routeInput(GuiScreen screen) {
        guiAvailable = true;
        activateRequested();
        OpenGuiCaptureHotkey.poll();
        if (session == null) screen.handleInput();
        else {
            overlay.handleInput();
            processInputEvents();
        }
    }

    private void activateRequested() {
        if (!activationRequested) return;
        activationRequested = false;
        open();
        if (session == null) inputEvents.clear();
    }

    private void processInputEvents() {
        CaptureInputEvent event;
        while ((event = inputEvents.poll()) != null) {
            if (session == null) continue;
            if (event.text() != null) overlay.text(event.text());
            else overlay.nativeKey(event.key(), event.pressed(), event.control(), event.shift());
        }
    }

    public void beginGui(float partial) {
        if (session == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != session.sourceScreen()) {
            close();
            return;
        }
        frameId++;
        partialTicks = partial;
        CaptureViewport viewport = CaptureCoordinateMapper.current(minecraft);
        session.updateViewport(viewport);
        frameRequested = session.wantsFrame(System.nanoTime());
        if (frameRequested) {
            sourceMouseX = Mouse.getX() * viewport.logicalWidth() / viewport.pixelWidth();
            sourceMouseY = viewport.logicalHeight() - Mouse.getY() * viewport.logicalHeight() / viewport.pixelHeight()
                - 1;
        }
        guiLayer.begin(
            viewport,
            session.settings()
                .config().includeDebug,
            frameRequested);
    }

    public void finishGui() {
        if (session != null) guiLayer.finish(Minecraft.getMinecraft());
    }

    public boolean hideBackdrop() {
        if (drawingTile) return !highResolution.content()
            .includeGuiBackdrop();
        return session != null && (guiLayer.drawing() || drawingTile)
            && !session.settings()
                .config().includeGuiBackdrop;
    }

    public boolean hideTooltips() {
        if (drawingTile) return !highResolution.content()
            .includeTooltips();
        return session != null && (guiLayer.drawing() || drawingTile)
            && !session.settings()
                .config().includeTooltips;
    }

    public boolean hideDebug() {
        if (drawingTile) return !highResolution.content()
            .includeDebug();
        return session != null && (guiLayer.drawing() || drawingTile)
            && !session.settings()
                .config().includeDebug;
    }

    public void frameReady() {
        if (session == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != session.sourceScreen()) {
            close();
            return;
        }
        try (CaptureRenderScope drawing = new CaptureRenderScope()) {
            long now = System.nanoTime();
            if (frameRequested) {
                CaptureViewport viewport = session.viewport();
                CaptureFrame frame;
                if (session.settings()
                    .config().includeWorld && minecraft.theWorld != null) {
                    frame = pixels.readOpaque(0, 0, viewport.pixelWidth(), viewport.pixelHeight(), now);
                } else if (guiLayer.available()) {
                    frame = guiLayer.read(pixels, now);
                } else {
                    frame = null;
                    session.notify(I18n.format("guidenh.capture.requires_framebuffer"));
                }
                if (frame != null) session.acceptFrame(frame);
            }
            if (frameRequested) {
                overlay.updateGeometry(
                    registry.collect(
                        new CaptureContext(session.sourceScreen(), session.viewport(), frameId, partialTicks)));
                frameRequested = false;
            }
            if (pendingSave) {
                pendingSave = false;
                saveNow();
            }
            if (highResolution != null) {
                try {
                    drawingTile = true;
                    if (highResolution.step(tileSource, pixels, session, exporter)) {
                        highResolution.close();
                        highResolution = null;
                        tileSource.close();
                    } else session.notify(
                        I18n.format(
                            "guidenh.capture.rendering_tiles",
                            highResolution.completedTiles(),
                            highResolution.tileCount()));
                } catch (IOException | RuntimeException error) {
                    session.outputFailed(error);
                    releaseHighResolution();
                } finally {
                    drawingTile = false;
                }
            }
            minecraft.entityRenderer.setupOverlayRendering();
            overlay.draw();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        guiAvailable = Minecraft.getMinecraft().currentScreen != null;
        activateRequested();
        OpenGuiCaptureHotkey.poll();
        if (session == null) return;
        session.pollExport();
        if (Minecraft.getMinecraft().currentScreen != session.sourceScreen() || session.state()
            .equals(CaptureSessionState.CLOSED)) close();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiOpen(GuiOpenEvent event) {
        guiAvailable = event.gui != null;
    }

    public void close() {
        if (session == null) return;
        releaseHighResolution();
        pendingSave = false;
        overlay.close();
        registry.closed(new CaptureContext(session.sourceScreen(), session.viewport(), frameId, partialTicks));
        guiLayer.close();
        pixels.release();
        session.close();
        Keyboard.enableRepeatEvents(previousKeyRepeat);
        overlay = null;
        session = null;
        activationRequested = false;
        inputEvents.clear();
    }

    public void save() {
        if (!session.canSave()) return;
        if (session.settings()
            .config().refreshBeforeSave) {
            pendingSave = true;
            session.requestRefresh();
        } else saveNow();
    }

    private void saveNow() {
        if (!session.canSave()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        try {
            CaptureExportRequest request = session.request(minecraft.mcDataDir.toPath());
            CaptureComposition composition = CaptureRowComposer.compose(request);
            CaptureOutputSize output = request.options()
                .resolution()
                .resolve(
                    composition.size()
                        .width(),
                    composition.size()
                        .height());
            boolean enlarged = output.width() > composition.size()
                .width() || output.height()
                    > composition.size()
                        .height();
            String quality = request.options()
                .resolution()
                .qualityPolicy();
            if (enlarged && !quality.equals("compatible")) {
                CaptureContext context = new CaptureContext(
                    session.sourceScreen(),
                    session.viewport(),
                    frameId,
                    partialTicks);
                CaptureGuiRenderer renderer = registry.renderer(context);
                if (renderer != null && !request.options()
                    .content()
                    .includeWorld()
                    && session.settings()
                        .config().format.equals("png")
                    && guiLayer.available()) {
                    int tileSize = tileSource.tileLimit(
                        session.settings()
                            .config().tileSize,
                        (long) session.settings()
                            .config().memoryBudgetMiB << 20);
                    CaptureRenderSnapshot snapshot = renderer.snapshot(
                        context,
                        sourceMouseX,
                        sourceMouseY,
                        request.options()
                            .content()
                            .includeGuiBackdrop());
                    try {
                        highResolution = new CaptureHighResolutionJob(
                            request,
                            snapshot,
                            tileSize,
                            session.preview()
                                .timestampNanos());
                    } catch (IOException | RuntimeException error) {
                        snapshot.close();
                        throw error;
                    }
                    session.preparingOutput();
                    session.outputQuality("redrawn");
                    return;
                }
                if (quality.equals("strict"))
                    throw new IllegalArgumentException(I18n.format("guidenh.capture.no_high_resolution"));
            }
            session.save(exporter, minecraft.mcDataDir.toPath());
            session.outputQuality(enlarged ? "resampled" : "native");
            if (enlarged) session.notify(I18n.format("guidenh.capture.compatible_scaling"));
        } catch (IOException | RuntimeException error) {
            session.outputFailed(error);
        }
    }

    private void releaseHighResolution() {
        try {
            if (highResolution != null) highResolution.close();
        } catch (IOException error) {
            session.outputFailed(error);
        } finally {
            highResolution = null;
            tileSource.close();
        }
    }

    public boolean capturingGuiLayer() {
        return session != null && (guiLayer.available() && guiLayer.drawing() || drawingTile);
    }

    public void shutdown() {
        close();
        exporter.close();
    }
}
