package com.hfstudio.guidenh.client.capture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.client.event.GuiOpenEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchHost;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchManifest;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOutput;
import com.hfstudio.guidenh.client.capture.api.CaptureClientGuiFactory;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGuiRenderer;
import com.hfstudio.guidenh.client.capture.api.CaptureInputEvent;
import com.hfstudio.guidenh.client.capture.api.CaptureInputOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;
import com.hfstudio.guidenh.client.capture.api.CaptureSessionState;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.batch.CaptureBatchEngine;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CaptureExportWorker;
import com.hfstudio.guidenh.client.capture.export.CaptureRowComposer;
import com.hfstudio.guidenh.client.capture.geometry.CaptureCoordinateMapper;
import com.hfstudio.guidenh.client.capture.recording.CaptureRecording;
import com.hfstudio.guidenh.client.capture.render.CaptureHighResolutionJob;
import com.hfstudio.guidenh.client.capture.render.CaptureRenderScope;
import com.hfstudio.guidenh.client.capture.render.GlCapturePixelReader;
import com.hfstudio.guidenh.client.capture.render.GuiLayerCaptureSource;
import com.hfstudio.guidenh.client.capture.render.GuiTileCaptureSource;
import com.hfstudio.guidenh.client.capture.server.CaptureClientConnection;
import com.hfstudio.guidenh.client.capture.session.CaptureSchedule;
import com.hfstudio.guidenh.client.capture.session.CaptureSelectionSnapshot;
import com.hfstudio.guidenh.client.capture.session.CaptureSession;
import com.hfstudio.guidenh.client.capture.ui.CaptureNavigationScreen;
import com.hfstudio.guidenh.client.capture.ui.CaptureOverlay;
import com.hfstudio.guidenh.client.capture.ui.CaptureOverlayActions;
import com.hfstudio.guidenh.client.hotkey.OpenGuiCaptureHotkey;
import com.hfstudio.guidenh.integration.capture.ClientGuiBatchProvider;
import com.hfstudio.guidenh.integration.capture.VanillaCaptureGeometryProvider;
import com.hfstudio.guidenh.integration.capture.VanillaCaptureGuiRenderer;
import com.hfstudio.guidenh.integration.capture.VanillaClientGuiFactories;
import com.hfstudio.guidenh.integration.capture.VisibleGuiBatchProvider;

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
    private CaptureSchedule schedule;
    private volatile boolean sourceVisible;
    private CaptureOutputArtifact openedArtifact;
    private CaptureBatchEngine batch;
    private final RuntimeBatchHost batchHost = new RuntimeBatchHost();
    private final ClientGuiBatchProvider clientGuis = new ClientGuiBatchProvider();
    private CaptureRecording batchRecording;
    private CaptureGeometry geometry;
    private CaptureSelectionSnapshot batchSelections;
    private CompletableFuture<CaptureOutputArtifact> highResolutionCompletion;
    private boolean closeAfterBatch;

    public static CaptureRuntime instance() {
        return INSTANCE;
    }

    public CaptureRegistry registry() {
        return registry;
    }

    public void initialize() {
        registry.registerGeometry(new VanillaCaptureGeometryProvider());
        registry.registerRenderer(new VanillaCaptureGuiRenderer());
        registry.registerBatch(new VisibleGuiBatchProvider());
        registry.registerBatch(clientGuis);
        VanillaClientGuiFactories.register(clientGuis);
        guiAvailable = Minecraft.getMinecraft().currentScreen != null;
    }

    public void useNativeKeyboard() {
        nativeKeyboard = true;
    }

    public boolean guiAvailable() {
        return guiAvailable;
    }

    public void registerClientGui(CaptureClientGuiFactory factory) {
        clientGuis.register(factory);
    }

    public void refreshBatchCatalog() {
        if (batch != null && !batch.busy()) batch.refresh();
    }

    public boolean inputCaptured() {
        return session != null && sourceVisible || activationRequested;
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
        if (session != null && minecraft.currentScreen != session.sourceScreen()) close();
        if (session != null || minecraft.currentScreen == null) return;
        previousKeyRepeat = Keyboard.areRepeatEventsEnabled();
        Keyboard.enableRepeatEvents(true);
        session = new CaptureSession(minecraft.currentScreen, CaptureCoordinateMapper.current(minecraft));
        sourceVisible = true;
        batch = new CaptureBatchEngine(registry, batchHost);
        overlay = new CaptureOverlay(
            minecraft,
            session,
            new CaptureInputOptions(
                OpenGuiCaptureHotkey.getHotkey()
                    .getKeyCode(),
                nativeKeyboard),
            new CaptureOverlayActions(
                this::save,
                this::startRecording,
                session::pauseRecording,
                session::stopRecording,
                this::toggleSchedule,
                this::close,
                () -> schedule != null),
            batch);
        frameRequested = true;
        registry.opened(new CaptureContext(session.sourceScreen(), session.viewport(), frameId, partialTicks));
    }

    public void routeInput(GuiScreen screen) {
        guiAvailable = true;
        activateRequested();
        OpenGuiCaptureHotkey.poll();
        if (session != null && batch.busy() && screen != session.sourceScreen()) batchHost.adoptScreen();
        if (session == null || screen != session.sourceScreen()) screen.handleInput();
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
            unavailableSource();
            return;
        }
        frameId++;
        partialTicks = partial;
        CaptureViewport viewport = CaptureCoordinateMapper.current(minecraft);
        session.updateViewport(viewport);
        if (batch.needsFrame()) session.requestRefresh();
        long sampleTime = System.nanoTime();
        if (batchRecording != null) {
            batchRecording.tick(sampleTime);
            if (!batchRecording.finished() && !batchRecording.paused()
                && !viewport.equals(
                    batchRecording.request()
                        .viewport()))
                batchRecording.togglePause(sampleTime);
        }
        frameRequested = session.wantsFrame(sampleTime)
            || batchRecording != null && batchRecording.needsFrame(sampleTime);
        if (frameRequested && session.previewRequested()) {
            sourceMouseX = Mouse.getX() * viewport.logicalWidth() / viewport.pixelWidth();
            sourceMouseY = viewport.logicalHeight() - Mouse.getY() * viewport.logicalHeight() / viewport.pixelHeight()
                - 1;
        }
        guiLayer.begin(
            viewport,
            content().includeDebug(),
            session.previewRequested() || session.selections()
                .following());
    }

    public void finishGui() {
        if (session != null) guiLayer.finish(Minecraft.getMinecraft());
    }

    public boolean hideBackdrop() {
        if (drawingTile) return !highResolution.content()
            .includeGuiBackdrop();
        return session != null && (guiLayer.drawing() || drawingTile) && !content().includeGuiBackdrop();
    }

    public boolean hideTooltips() {
        if (drawingTile) return !highResolution.content()
            .includeTooltips();
        return session != null && (guiLayer.drawing() || drawingTile) && !content().includeTooltips();
    }

    public boolean hideDebug() {
        if (drawingTile) return !highResolution.content()
            .includeDebug();
        return session != null && (guiLayer.drawing() || drawingTile) && !content().includeDebug();
    }

    public boolean bindMainFramebuffer(Framebuffer target, boolean viewport) {
        if (session == null || target != Minecraft.getMinecraft()
            .getFramebuffer()) return false;
        if (drawingTile) {
            tileSource.bindCurrent(viewport);
            return true;
        }
        if (guiLayer.drawing() && guiLayer.available()) {
            guiLayer.bindCurrent(viewport);
            return true;
        }
        return false;
    }

    public boolean hideNei() {
        return session != null && guiLayer.drawing() && !content().includeNei();
    }

    public void frameReady() {
        if (session == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != session.sourceScreen()) {
            unavailableSource();
            return;
        }
        try (CaptureRenderScope drawing = new CaptureRenderScope()) {
            long now = System.nanoTime();
            if (frameRequested && (session.previewRequested() || session.selections()
                .following())) {
                geometry = registry
                    .collect(new CaptureContext(session.sourceScreen(), session.viewport(), frameId, partialTicks));
                overlay.updateGeometry(geometry);
            }
            if (frameRequested) {
                CaptureViewport viewport = session.viewport();
                CaptureFrame frame;
                if (content().includeWorld() && minecraft.theWorld != null) {
                    frame = pixels.readOpaque(0, 0, viewport.pixelWidth(), viewport.pixelHeight(), now);
                } else if (guiLayer.available()) {
                    frame = guiLayer.read(pixels, now);
                } else {
                    frame = null;
                    session.notify(I18n.format("guidenh.capture.requires_framebuffer"));
                }
                if (frame != null) {
                    if (batchRecording != null) batchRecording.offer(frame, now);
                    session.acceptFrame(frame);
                }
            }
            frameRequested = false;
            if (batch.busy()) batch.frameReady(batchHost.context());
            if (pendingSave) {
                pendingSave = false;
                saveNow();
            }
            if (highResolution != null) {
                try {
                    drawingTile = true;
                    if (highResolution.step(tileSource, pixels)) {
                        CaptureRows rows = highResolution.takeRows();
                        CompletableFuture<CaptureOutputArtifact> completion = highResolutionCompletion;
                        CompletableFuture<CaptureOutputArtifact> writing = exporter
                            .submitRows(highResolution.request(), rows, highResolution.timestampNanos());
                        completion
                            .whenComplete((artifact, error) -> { if (completion.isCancelled()) writing.cancel(true); });
                        writing.whenComplete((artifact, error) -> {
                            if (error == null) completion.complete(artifact);
                            else completion.completeExceptionally(error);
                        });
                        highResolutionCompletion = null;
                        highResolution.close();
                        highResolution = null;
                        tileSource.close();
                    } else session.notify(
                        I18n.format(
                            "guidenh.capture.rendering_tiles",
                            highResolution.completedTiles(),
                            highResolution.tileCount()));
                } catch (IOException | RuntimeException error) {
                    if (highResolutionCompletion != null) highResolutionCompletion.completeExceptionally(error);
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
        sourceVisible = Minecraft.getMinecraft().currentScreen == session.sourceScreen();
        if (batchRecording != null) {
            batchRecording.tick(System.nanoTime());
            if (batchRecording.completion()
                .isDone()) batchRecording = null;
        }
        batch.tick(System.nanoTime());
        if (closeAfterBatch && !batch.busy()) {
            closeAfterBatch = false;
            close();
            return;
        }
        session.pollExport();
        if (session.lastArtifact() != null && session.lastArtifact() != openedArtifact) {
            openedArtifact = session.lastArtifact();
            if (session.settings()
                .config().openFolderAfterSave) overlay.openOutputFolder();
        }
        if (session.state()
            .equals(CaptureSessionState.CLOSED)) {
            close();
            return;
        }
        updateSchedule(System.nanoTime());
        if (!sourceVisible) unavailableSource();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui == null && session != null
            && batch != null
            && batch.busy()
            && Minecraft.getMinecraft().theWorld != null) event.gui = new CaptureNavigationScreen();
        guiAvailable = event.gui != null;
        if (event.gui != null && !(event.gui instanceof CaptureNavigationScreen)) clientGuis.observe(event.gui);
        sourceVisible = session != null && event.gui == session.sourceScreen();
    }

    public void close() {
        if (session == null) return;
        if (batch != null && batch.busy()) {
            batch.cancel();
            closeAfterBatch = true;
            return;
        }
        if (batch != null) batch.close();
        batch = null;
        releaseHighResolution();
        pendingSave = false;
        schedule = null;
        overlay.close();
        registry.closed(new CaptureContext(session.sourceScreen(), session.viewport(), frameId, partialTicks));
        guiLayer.close();
        pixels.release();
        session.close();
        Keyboard.enableRepeatEvents(previousKeyRepeat);
        overlay = null;
        session = null;
        openedArtifact = null;
        sourceVisible = false;
        activationRequested = false;
        inputEvents.clear();
        CaptureClientConnection.instance()
            .closeCapture();
        if (Minecraft.getMinecraft().currentScreen instanceof CaptureNavigationScreen) Minecraft.getMinecraft()
            .displayGuiScreen(null);
    }

    public void save() {
        if (!session.canSave()) return;
        if (!session.settings()
            .config().format.equals("png")) {
            startRecording();
            return;
        }
        if (session.settings()
            .config().refreshBeforeSave) {
            pendingSave = true;
            session.requestRefresh();
        } else saveNow();
    }

    private CaptureContentOptions content() {
        if (batch != null && batch.busy()) return batch.catalog()
            .options()
            .output()
            .content();
        CaptureRecording recording = session.recording();
        if (recording != null && !recording.finished()) return recording.request()
            .options()
            .content();
        return session.settings()
            .content();
    }

    public void startRecording() {
        if (!session.canSave()) return;
        try {
            CaptureExportRequest request = session.request(Minecraft.getMinecraft().mcDataDir.toPath());
            CaptureComposition composition = CaptureRowComposer.compose(request);
            CaptureOutputSize output = request.options()
                .resolution()
                .resolve(
                    composition.size()
                        .width(),
                    composition.size()
                        .height());
            if (request.options()
                .resolution()
                .qualityPolicy()
                .equals("strict")
                && (output.width() > composition.size()
                    .width() || output.height()
                        > composition.size()
                            .height())) {
                throw new IllegalArgumentException(I18n.format("guidenh.capture.no_high_resolution"));
            }
            session.startRecording(exporter, Minecraft.getMinecraft().mcDataDir.toPath());
        } catch (RuntimeException error) {
            session.outputFailed(error);
        }
    }

    public void toggleSchedule() {
        if (schedule != null) {
            schedule.cancel();
            schedule = null;
            session.keepOpenAfterSave(false);
            session.notify(I18n.format("guidenh.capture.schedule_cancelled"));
            return;
        }
        if (!session.canSave()) return;
        try {
            var config = session.settings()
                .config();
            schedule = new CaptureSchedule(
                config.delaySeconds,
                config.scheduledTime,
                config.intervalSeconds,
                config.repeatCount,
                System.nanoTime());
            session.keepOpenAfterSave(true);
        } catch (RuntimeException error) {
            session.outputFailed(error);
        }
    }

    private void updateSchedule(long now) {
        if (schedule == null) return;
        if (schedule.complete()) {
            schedule = null;
            session.keepOpenAfterSave(false);
            return;
        }
        if (!schedule.due(now)) {
            if (!session.busy()) session
                .notify(I18n.format("guidenh.capture.countdown", schedule.secondsUntil(now), schedule.remaining()));
            return;
        }
        boolean available = sourceVisible && Display.isActive() && Display.isVisible();
        if (!available) {
            String policy = session.settings()
                .config().unavailablePolicy;
            if (policy.equals("skip")) schedule.consumed(now);
            else if (policy.equals("stop")) schedule.cancel();
            else session.notify(I18n.format("guidenh.capture.schedule_waiting"));
            return;
        }
        if (!session.canSave()) return;
        schedule.consumed(now);
        if (session.settings()
            .config().format.equals("png")) {
            pendingSave = true;
            session.requestRefresh();
        } else startRecording();
    }

    private void unavailableSource() {
        sourceVisible = false;
        if (batch != null && batch.busy()) {
            if (batchRecording != null && !batchRecording.finished() && !batchRecording.paused())
                batchRecording.togglePause(System.nanoTime());
            return;
        }
        CaptureRecording recording = session.recording();
        if (recording != null && !recording.finished()) {
            if (!recording.paused()) session.pauseRecording();
            session.notify(I18n.format("guidenh.capture.target_changed"));
        } else if (schedule == null) close();
    }

    private void saveNow() {
        if (!session.canSave()) return;
        try {
            CaptureExportRequest request = session.request(Minecraft.getMinecraft().mcDataDir.toPath());
            session.beginExport(
                captureFrame(
                    request,
                    session.preview(),
                    session.settings()
                        .config().tileSize,
                    (long) session.settings()
                        .config().memoryBudgetMiB << 20));
        } catch (IOException | RuntimeException error) {
            session.outputFailed(error);
        }
    }

    private CompletableFuture<CaptureOutputArtifact> captureFrame(CaptureExportRequest request, CaptureFrame frame,
        int tileSize, long memoryBudget) throws IOException {
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
                && guiLayer.available()
                && request.viewport()
                    .equals(session.viewport())) {
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
                        tileSource.tileLimit(tileSize, memoryBudget),
                        frame.timestampNanos());
                } catch (IOException | RuntimeException error) {
                    snapshot.close();
                    throw error;
                }
                highResolutionCompletion = new CompletableFuture<>();
                session.outputQuality("redrawn");
                return highResolutionCompletion;
            }
            if (quality.equals("strict"))
                throw new IllegalArgumentException(I18n.format("guidenh.capture.no_high_resolution"));
        }
        session.outputQuality(enlarged ? "resampled" : "native");
        return exporter.submit(request, frame, "png");
    }

    public class RuntimeBatchHost implements CaptureBatchHost {

        @Override
        public CaptureBatchContext context() {
            Minecraft minecraft = Minecraft.getMinecraft();
            CaptureViewport viewport = CaptureCoordinateMapper.current(minecraft);
            CaptureGeometry current = geometry == null || !geometry.viewport()
                .equals(viewport) ? new CaptureGeometry(0, viewport, List.of()) : geometry;
            return new CaptureBatchContext(
                new CaptureContext(minecraft.currentScreen, viewport, frameId, partialTicks),
                current);
        }

        @Override
        public CaptureBatchOptions options() {
            return session.settings()
                .batch(Minecraft.getMinecraft().mcDataDir.toPath());
        }

        @Override
        public CaptureFrame frame() {
            return session.preview();
        }

        @Override
        public boolean readyForBatch() {
            return !session.busy() && !pendingSave && highResolution == null;
        }

        @Override
        public void beginBatch() {
            batchSelections = session.selections()
                .snapshot();
            schedule = null;
            session.keepOpenAfterSave(true);
        }

        @Override
        public void endBatch(boolean restoreSelections) {
            if (restoreSelections) session.selections()
                .restore(batchSelections);
            session.keepOpenAfterSave(false);
            batchSelections = null;
            session.requestRefresh();
        }

        @Override
        public void adoptScreen() {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.currentScreen == null || minecraft.currentScreen == session.sourceScreen()) return;
            registry.closed(new CaptureContext(session.sourceScreen(), session.viewport(), frameId, partialTicks));
            session.adoptScreen(minecraft.currentScreen, CaptureCoordinateMapper.current(minecraft));
            geometry = null;
            sourceVisible = true;
            inputEvents.clear();
            registry.opened(new CaptureContext(session.sourceScreen(), session.viewport(), frameId, partialTicks));
        }

        @Override
        public CompletableFuture<CaptureOutputArtifact> capture(CaptureExportRequest request,
            CaptureBatchOptions options, CaptureFrame frame) {
            try {
                if (options.recording() != null) {
                    CaptureComposition composition = CaptureRowComposer.compose(request);
                    CaptureOutputSize size = request.options()
                        .resolution()
                        .resolve(
                            composition.size()
                                .width(),
                            composition.size()
                                .height());
                    if (request.options()
                        .resolution()
                        .qualityPolicy()
                        .equals("strict")
                        && (size.width() > composition.size()
                            .width() || size.height()
                                > composition.size()
                                    .height())) {
                        throw new IllegalArgumentException(I18n.format("guidenh.capture.no_high_resolution"));
                    }
                    CaptureRecording created = new CaptureRecording(request, options.recording(), System.nanoTime());
                    try {
                        exporter.startRecording(created);
                    } catch (RuntimeException error) {
                        created.close();
                        throw error;
                    }
                    batchRecording = created;
                    return batchRecording.completion();
                }
                return captureFrame(request, frame, options.tileSize(), options.memoryBudgetBytes());
            } catch (IOException | RuntimeException error) {
                return CompletableFuture.failedFuture(error);
            }
        }

        @Override
        public boolean recordingActive() {
            return batchRecording != null && !batchRecording.finished();
        }

        @Override
        public boolean recordingPaused() {
            return recordingActive() && batchRecording.paused();
        }

        @Override
        public void pauseCapture() {
            if (recordingActive()) batchRecording.togglePause(System.nanoTime());
        }

        @Override
        public void stopCapture() {
            if (recordingActive()) batchRecording.stop(System.nanoTime());
        }

        @Override
        public void cancelCapture() {
            if (batchRecording != null) batchRecording.close();
            releaseHighResolution();
        }

        @Override
        public CompletableFuture<List<CaptureBatchOutput>> compose(CaptureBatchManifest manifest,
            CaptureBatchOptions options, Path directory, long remainingBytes) {
            return exporter.compose(manifest, options, directory, remainingBytes);
        }

        @Override
        public CompletableFuture<Path> writeManifest(CaptureBatchManifest manifest, Path directory,
            long remainingBytes) {
            return exporter.writeManifest(manifest, directory, remainingBytes);
        }
    }

    private void releaseHighResolution() {
        if (highResolutionCompletion != null)
            highResolutionCompletion.completeExceptionally(new CancellationException("Capture cancelled"));
        highResolutionCompletion = null;
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
        if (batch != null) batch.close();
        batch = null;
        if (batchRecording != null) batchRecording.close();
        close();
        exporter.close();
    }
}
