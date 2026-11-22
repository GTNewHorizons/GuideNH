package com.hfstudio.guidenh.client.capture;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiOpenEvent;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureInputEvent;
import com.hfstudio.guidenh.client.capture.api.CaptureInputOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureSessionState;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.export.CaptureExportWorker;
import com.hfstudio.guidenh.client.capture.geometry.CaptureCoordinateMapper;
import com.hfstudio.guidenh.client.capture.render.CaptureRenderScope;
import com.hfstudio.guidenh.client.capture.render.GlCapturePixelReader;
import com.hfstudio.guidenh.client.capture.render.GuiLayerCaptureSource;
import com.hfstudio.guidenh.client.capture.session.CaptureSession;
import com.hfstudio.guidenh.client.capture.ui.CaptureOverlay;
import com.hfstudio.guidenh.client.hotkey.OpenGuiCaptureHotkey;
import com.hfstudio.guidenh.integration.capture.VanillaCaptureGeometryProvider;

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
    private long geometryTime;
    private boolean previousKeyRepeat;
    private final Queue<CaptureInputEvent> inputEvents = new ConcurrentLinkedQueue<>();
    private volatile boolean activationRequested;
    private volatile boolean guiAvailable;
    private boolean nativeKeyboard;

    public static CaptureRuntime instance() {
        return INSTANCE;
    }

    public CaptureRegistry registry() {
        return registry;
    }

    public void initialize() {
        registry.registerGeometry(new VanillaCaptureGeometryProvider());
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
            exporter,
            new CaptureInputOptions(
                OpenGuiCaptureHotkey.getHotkey()
                    .getKeyCode(),
                nativeKeyboard),
            this::close);
        geometryTime = 0;
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
        guiLayer.begin(
            viewport,
            session.settings()
                .config().includeDebug);
    }

    public void finishGui() {
        if (session != null) guiLayer.finish(Minecraft.getMinecraft());
    }

    public boolean hideBackdrop() {
        return session != null && guiLayer.drawing()
            && !session.settings()
                .config().includeGuiBackdrop;
    }

    public boolean hideTooltips() {
        return session != null && guiLayer.drawing()
            && !session.settings()
                .config().includeTooltips;
    }

    public boolean hideDebug() {
        return session != null && guiLayer.drawing()
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
            if (session.wantsFrame(now)) {
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
            if (now - geometryTime >= 50_000_000L) {
                overlay.updateGeometry(
                    registry.collect(
                        new CaptureContext(session.sourceScreen(), session.viewport(), frameId, partialTicks)));
                geometryTime = now;
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

    public void shutdown() {
        close();
        exporter.close();
    }
}
