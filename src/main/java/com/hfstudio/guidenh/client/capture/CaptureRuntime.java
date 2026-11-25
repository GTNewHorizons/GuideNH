package com.hfstudio.guidenh.client.capture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.hfstudio.guidenh.client.capture.api.CaptureBackendControls;
import com.hfstudio.guidenh.client.capture.api.CaptureBackendSelection;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchHost;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchManifest;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOutput;
import com.hfstudio.guidenh.client.capture.api.CaptureClientGuiFactory;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureFrameIdentity;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGuiRenderer;
import com.hfstudio.guidenh.client.capture.api.CaptureInputEvent;
import com.hfstudio.guidenh.client.capture.api.CaptureInputOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureItemGuiFactory;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CapturePhaseSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;
import com.hfstudio.guidenh.client.capture.api.CaptureReadbackMetadata;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderPhase;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderProbe;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderState;
import com.hfstudio.guidenh.client.capture.api.CaptureRowExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;
import com.hfstudio.guidenh.client.capture.api.CaptureScheduleCondition;
import com.hfstudio.guidenh.client.capture.api.CaptureSessionState;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetChangePolicy;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.batch.CaptureBatchEngine;
import com.hfstudio.guidenh.client.capture.cursor.CaptureSystemCursor;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CaptureExportWorker;
import com.hfstudio.guidenh.client.capture.export.CaptureRowComposer;
import com.hfstudio.guidenh.client.capture.geometry.CaptureAlphaGeometry;
import com.hfstudio.guidenh.client.capture.geometry.CaptureButtonCollector;
import com.hfstudio.guidenh.client.capture.geometry.CaptureContentGeometry;
import com.hfstudio.guidenh.client.capture.geometry.CaptureCoordinateMapper;
import com.hfstudio.guidenh.client.capture.geometry.CaptureDrawCollector;
import com.hfstudio.guidenh.client.capture.geometry.CaptureHudGeometry;
import com.hfstudio.guidenh.client.capture.geometry.CaptureTooltipCollector;
import com.hfstudio.guidenh.client.capture.recording.CaptureRecording;
import com.hfstudio.guidenh.client.capture.render.CaptureContentSnapshot;
import com.hfstudio.guidenh.client.capture.render.CaptureDiscardOverlay;
import com.hfstudio.guidenh.client.capture.render.CaptureDrawRecorder;
import com.hfstudio.guidenh.client.capture.render.CaptureFramePhases;
import com.hfstudio.guidenh.client.capture.render.CaptureFrozenLayers;
import com.hfstudio.guidenh.client.capture.render.CaptureHighResolutionJob;
import com.hfstudio.guidenh.client.capture.render.CaptureLayerMask;
import com.hfstudio.guidenh.client.capture.render.CapturePipeline;
import com.hfstudio.guidenh.client.capture.render.CaptureQueuedFrame;
import com.hfstudio.guidenh.client.capture.render.CaptureRecordedGuiRenderer;
import com.hfstudio.guidenh.client.capture.render.CaptureRecordedSnapshot;
import com.hfstudio.guidenh.client.capture.render.CaptureRedrawMismatchException;
import com.hfstudio.guidenh.client.capture.render.CaptureRenderScope;
import com.hfstudio.guidenh.client.capture.render.CaptureTracingPixelReader;
import com.hfstudio.guidenh.client.capture.render.CaptureWorldReader;
import com.hfstudio.guidenh.client.capture.render.CaptureWorldSnapshot;
import com.hfstudio.guidenh.client.capture.render.GlCaptureRenderProbe;
import com.hfstudio.guidenh.client.capture.render.GuiTileCaptureSource;
import com.hfstudio.guidenh.client.capture.server.CaptureClientConnection;
import com.hfstudio.guidenh.client.capture.session.CaptureSchedule;
import com.hfstudio.guidenh.client.capture.session.CaptureSelectionSnapshot;
import com.hfstudio.guidenh.client.capture.session.CaptureSession;
import com.hfstudio.guidenh.client.capture.ui.CaptureGameplayScreen;
import com.hfstudio.guidenh.client.capture.ui.CaptureNavigationScreen;
import com.hfstudio.guidenh.client.capture.ui.CaptureOverlay;
import com.hfstudio.guidenh.client.capture.ui.CaptureOverlayActions;
import com.hfstudio.guidenh.client.hotkey.OpenGuiCaptureHotkey;
import com.hfstudio.guidenh.config.CaptureConfig;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.integration.capture.ClientGuiBatchProvider;
import com.hfstudio.guidenh.integration.capture.ItemGuiBatchProvider;
import com.hfstudio.guidenh.integration.capture.VanillaCaptureGeometryProvider;
import com.hfstudio.guidenh.integration.capture.VanillaCaptureGuiRenderer;
import com.hfstudio.guidenh.integration.capture.VanillaClientGuiFactories;
import com.hfstudio.guidenh.integration.capture.VanillaPreviewGuiFactories;
import com.hfstudio.guidenh.integration.capture.VisibleGuiBatchProvider;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorGuiCaptureScreen;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class CaptureRuntime implements CaptureBackendControls, IResourceManagerReloadListener {

    private static final CaptureRuntime INSTANCE = new CaptureRuntime();
    private final CaptureRegistry registry = new CaptureRegistry();
    private final CapturePipeline pipeline = new CapturePipeline(registry);
    private CaptureBackendSelection exportBackend;
    private CaptureBackendSelection exportPreference;
    private CaptureBackendSelection recordingPreference;
    private CaptureContentOptions exportContent;
    private CaptureRenderProbe renderProbe = new GlCaptureRenderProbe();
    private final CaptureFramePhases framePhases = new CaptureFramePhases();
    private CaptureFrame pendingFrame;
    private CaptureQueuedFrame pendingReadback;
    private CaptureWorldSnapshot pendingWorld;
    private final CaptureFrozenLayers frozenLayers = new CaptureFrozenLayers();
    private final CaptureDrawRecorder drawRecorder = new CaptureDrawRecorder();
    private boolean reusingLayers;
    private CaptureFrameIdentity pendingIdentity;
    private CaptureRenderState pendingTarget;
    private CaptureRenderPhase pendingPhase;
    private boolean backendPresentationHook;
    private final CaptureExportWorker exporter = new CaptureExportWorker(registry);
    private volatile CaptureSession session;
    private Thread captureThread;
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
    private CaptureRowExporter highResolutionEncoder;
    private boolean pendingSave;
    private boolean drawingTile;
    private int sourceMouseX;
    private int sourceMouseY;
    private double sourceCursorX;
    private double sourceCursorY;
    private final CaptureSystemCursor systemCursor = new CaptureSystemCursor();
    private CaptureSchedule schedule;
    private volatile boolean sourceVisible;
    private CaptureOutputArtifact openedArtifact;
    private CaptureBatchEngine batch;
    private final RuntimeBatchHost batchHost = new RuntimeBatchHost();
    private final ClientGuiBatchProvider clientGuis = new ClientGuiBatchProvider();
    private final ItemGuiBatchProvider itemGuis = new ItemGuiBatchProvider();
    private CaptureRecording batchRecording;
    private CaptureGeometry geometry;
    private CaptureGeometry frameGeometry;
    private final CaptureTooltipCollector tooltips = new CaptureTooltipCollector();
    private final CaptureButtonCollector drawnButtons = new CaptureButtonCollector();
    private final CaptureDrawCollector overlayDraws = new CaptureDrawCollector();
    private final CaptureDiscardOverlay discardedHud = new CaptureDiscardOverlay();
    private CaptureGeometry renderedGeometry;
    private String drawingLayer = CaptureTarget.GUI_LAYER;
    private boolean drawingCursor;
    private CaptureRect cursorBounds;
    private boolean restoreGameplayFocus;
    private boolean hotbarDrawn;
    private CaptureSelectionSnapshot batchSelections;
    private CompletableFuture<CaptureOutputArtifact> highResolutionCompletion;
    private boolean closeAfterBatch;
    private volatile boolean resourcesInvalidated;

    public static CaptureRuntime instance() {
        return INSTANCE;
    }

    public CaptureRegistry registry() {
        return registry;
    }

    public void renderProbe(CaptureRenderProbe probe, boolean presentationHook) {
        renderProbe.reset();
        renderProbe = probe;
        backendPresentationHook = presentationHook;
    }

    public List<CapturePhaseSnapshot> phaseSnapshots() {
        return framePhases.snapshots();
    }

    public boolean backendPresentationHook() {
        return backendPresentationHook;
    }

    private long memoryBudget() {
        if (session != null && session.recording() != null) return session.recording()
            .options()
            .memoryBudgetBytes();
        return batch != null && batch.busy() ? batch.catalog()
            .options()
            .memoryBudgetBytes()
            : (long) session.settings()
                .config().memoryBudgetMiB << 20;
    }

    private void discardPendingFrame() {
        if (batch != null) {
            if (pendingReadback != null) batch.discardFrame(
                pendingReadback.context()
                    .frameId());
            else if (pendingFrame != null) batch.discardFrame(frameId);
        }
        if (pendingReadback != null) pendingReadback.close();
        pendingReadback = null;
        if (pendingFrame != null) pendingFrame.close();
        if (pendingWorld != null) pendingWorld.close();
        pendingFrame = null;
        pendingWorld = null;
        pendingIdentity = null;
        pendingTarget = null;
        pendingPhase = null;
    }

    private void reachPhase(CaptureRenderPhase phase) {
        if (session == null || !framePhases.active()) return;
        try {
            CaptureRenderState state = renderProbe.capture(captureContext(), memoryBudget());
            if (state.shadowPass()) return;
            CaptureContext context = framePhases.reach(phase, state, System.nanoTime());
            if (context == null) return;
            registry.phaseReached(context);
            if (phase == CaptureRenderPhase.FRAME_READY && pendingReadback != null
                && pendingReadback.context()
                    .frameId() == frameId)
                pendingReadback = pendingReadback.withPhases(framePhases.snapshots());
            if (!frameRequested || pendingFrame != null || pendingReadback != null || pipeline.capturePhase() != phase)
                return;
            if (phase == CaptureRenderPhase.GUI_END || phase == CaptureRenderPhase.FRAME_READY)
                pipeline.finishGui(context);
            CaptureTracingPixelReader reader = new CaptureTracingPixelReader(
                pipeline.reader(),
                renderProbe,
                context,
                memoryBudget());
            if (reusingLayers) {
                if (pendingWorld != null) {
                    frozenLayers.includeWorld(pendingWorld);
                    pendingWorld = null;
                }
                pendingFrame = frozenLayers.compose(content().includeWorld());
            } else if (pipeline.supportsLayerCapture()) {
                long timestamp = System.nanoTime();
                CaptureReadback readback = pipeline.enqueueGuiLayer(context, reader, timestamp, memoryBudget());
                try {
                    collectRenderedGeometry(null);
                    prepareBatchFrame(context);
                    CaptureFrameIdentity identity = new CaptureFrameIdentity(
                        pipeline.selection(),
                        previewSelection(),
                        content(),
                        new CaptureReadbackMetadata(frameId, phase, reader.target(), framePhases.snapshots()));
                    pendingReadback = new CaptureQueuedFrame(
                        readback,
                        context,
                        identity,
                        renderedGeometry,
                        session.refreshRevision(),
                        pendingWorld,
                        cursorBounds,
                        session.previewRequested(),
                        timestamp,
                        drawRecorder.take());
                    pendingWorld = null;
                } catch (RuntimeException error) {
                    if (batch != null) batch.discardFrame(context.frameId());
                    readback.close();
                    throw error;
                }
                return;
            } else {
                frozenLayers.close();
                pendingFrame = pipeline.capture(context, reader, System.nanoTime());
                collectRenderedGeometry(null);
            }
            prepareBatchFrame(context);
            pendingIdentity = new CaptureFrameIdentity(pipeline.selection(), previewSelection(), content());
            pendingTarget = reusingLayers ? frozenLayers.target() : reader.target();
            pendingPhase = phase;
        } catch (RuntimeException error) {
            captureFailed(error);
        }
    }

    private void completePendingReadback() {
        CaptureQueuedFrame queued = pendingReadback;
        if (queued == null) return;
        try {
            if (!queued.readback()
                .ready()) {
                if (Display.isActive() && System.nanoTime() - queued.submittedAt() > 10_000_000_000L)
                    throw new IllegalStateException("The GPU pixel readback did not finish within ten seconds");
                return;
            }
            pendingReadback = null;
            try (queued) {
                CaptureFrame gui = queued.readback()
                    .take();
                CaptureFrame world = null;
                CaptureRecordedSnapshot recorded = queued.snapshot() == null ? null
                    : queued.snapshot()
                        .retain();
                boolean transferred = false;
                try {
                    CaptureGeometry captured = completeFrameGeometry(queued, gui);
                    world = queued.world() == null ? null
                        : queued.world()
                            .compose(gui);
                    frozenLayers.replace(
                        gui,
                        world,
                        queued.context(),
                        queued.identity()
                            .content(),
                        queued.revision(),
                        queued.identity()
                            .readback()
                            .target(),
                        recorded);
                    transferred = true;
                    acceptCapturedFrame(
                        frozenLayers.compose(
                            queued.identity()
                                .content()
                                .includeWorld()),
                        queued.identity(),
                        captured,
                        queued.previewRequested(),
                        queued.context());
                } finally {
                    if (!transferred) {
                        gui.close();
                        if (world != null) world.close();
                        if (recorded != null) recorded.close();
                    }
                }
            }
        } catch (RuntimeException error) {
            if (batch != null) batch.discardFrame(
                queued.context()
                    .frameId());
            captureFailed(error);
        }
    }

    private CaptureGeometry completeFrameGeometry(CaptureQueuedFrame queued, CaptureFrame isolated) {
        if (queued.geometry() == null) return null;
        CaptureViewport viewport = queued.context()
            .viewport();
        List<CaptureTarget> targets = new ArrayList<>(
            queued.geometry()
                .targets()
                .stream()
                .filter(
                    target -> !target.className()
                        .equals(CaptureDrawCollector.class.getName())
                        || CaptureDrawCollector.covered(target.visibleBounds(), isolated, viewport))
                .toList());
        String layer = queued.context()
            .screen() instanceof CaptureGameplayScreen
            || !queued.identity()
                .content()
                .includeGui() ? CaptureTarget.HUD_LAYER : CaptureTarget.GUI_LAYER;
        targets.addAll(CaptureAlphaGeometry.collect(isolated, viewport, layer, queued.cursorBounds()));
        return CaptureContentGeometry.visible(
            new CaptureGeometry(targets.hashCode(), viewport, targets),
            queued.identity()
                .content());
    }

    private void acceptCapturedFrame(CaptureFrame frame, CaptureFrameIdentity identity, CaptureGeometry captured,
        boolean updatePreview, CaptureContext capturedContext) {
        try {
            if (captured != null) {
                geometry = captured;
                overlay.updateGeometry(captured);
            }
            if (batchRecording != null) batchRecording.offer(frame, frame.timestampNanos());
            session.acceptFrame(frame, identity, updatePreview);
        } catch (RuntimeException error) {
            frame.close();
            throw error;
        }
        if (batch != null && batch.busy()) batch.frameReady(
            new CaptureBatchContext(
                capturedContext,
                captured == null ? batchHost.context()
                    .geometry() : captured));
    }

    private void prepareBatchFrame(CaptureContext context) {
        if (batch == null || !batch.busy()) return;
        batch.framePrepared(
            new CaptureBatchContext(context, renderedGeometry == null ? registry.collect(context) : renderedGeometry));
    }

    public String readerId() {
        CaptureBackendSelection selection = pipeline.selection();
        return selection == null ? null : selection.readerId();
    }

    public String sourceId() {
        CaptureBackendSelection selection = pipeline.selection();
        return selection == null ? null : selection.sourceId();
    }

    @Override
    public CaptureBackendSelection activeBackend() {
        return pipeline.selection();
    }

    @Override
    public List<String> sourceIds() {
        return backendChoices(registry.sourceIds());
    }

    @Override
    public List<String> readerIds() {
        return backendChoices(registry.readerIds());
    }

    private static List<String> backendChoices(List<String> ids) {
        List<String> choices = new ArrayList<>(ids.size() + 1);
        choices.add("automatic");
        choices.addAll(ids);
        return List.copyOf(choices);
    }

    @Override
    public void selectSource(String id) {
        CaptureConfig config = session == null ? ModConfig.capture
            : session.settings()
                .config();
        selectBackend(new CaptureBackendSelection(id, config.readerId));
    }

    @Override
    public void selectReader(String id) {
        CaptureConfig config = session == null ? ModConfig.capture
            : session.settings()
                .config();
        selectBackend(new CaptureBackendSelection(config.sourceId, id));
    }

    private void selectBackend(CaptureBackendSelection selection) {
        if (session != null && (session.busy() || batch != null && batch.busy()))
            throw new IllegalStateException("The capture source and pixel reader cannot change during a capture task");
        CaptureConfig config = session == null ? ModConfig.capture
            : session.settings()
                .config();
        if (session != null) {
            CaptureContext context = captureContext();
            pipeline.select(context, content(), selection, false);
            session.invalidatePreview();
        }
        config.sourceId = selection.sourceId();
        config.readerId = selection.readerId();
        refreshBatchCatalog();
    }

    private CaptureContext captureContext() {
        return new CaptureContext(
            session.sourceScreen(),
            session.viewport(),
            frameId,
            partialTicks,
            framePhases.phase(),
            framePhases.renderState());
    }

    private boolean taskOwnsPipeline() {
        return session.busy() || highResolution != null
            || batchRecording != null && !batchRecording.finished()
            || batch != null && batch.busy()
                && batch.progress()
                    .stage()
                    .equals("saving");
    }

    private CaptureBackendSelection backendSelection() {
        CaptureRecording recording = session.recording();
        if (recording != null) return recording.request()
            .options()
            .backend();
        if (batchRecording != null) return batchRecording.request()
            .options()
            .backend();
        if (highResolution != null) return highResolution.request()
            .options()
            .backend();
        if (exportBackend != null && taskOwnsPipeline()) return exportBackend;
        if (batch != null && batch.busy()) return batch.catalog()
            .options()
            .output()
            .backend();
        CaptureConfig config = session.settings()
            .config();
        return new CaptureBackendSelection(config.sourceId, config.readerId);
    }

    private CaptureBackendSelection previewSelection() {
        if (batch != null && batch.busy()) return batch.catalog()
            .options()
            .output()
            .backend();
        if (session.recording() != null && recordingPreference != null) return recordingPreference;
        if (session.busy() && exportPreference != null) return exportPreference;
        return session.settings()
            .backend();
    }

    private void captureFailed(RuntimeException error) {
        drawRecorder.close();
        pendingSave = false;
        discardedHud.end();
        discardPendingFrame();
        frozenLayers.close();
        reusingLayers = false;
        try {
            pipeline.close();
        } catch (RuntimeException closing) {
            error.addSuppressed(closing);
        }
        session.invalidatePreview();
        if (session.recording() != null && !session.recording()
            .finished()
            && !session.recording()
                .paused())
            session.pauseRecording();
        if (highResolutionCompletion != null) highResolutionCompletion.completeExceptionally(error);
        releaseHighResolution();
        if (batch != null && batch.busy()) batch.captureUnavailable(error.getMessage());
        session.notify(I18n.format("guidenh.capture.failed", error.getMessage()));
        frameRequested = false;
    }

    public void initialize() {
        registry.registerGeometry(new VanillaCaptureGeometryProvider());
        registry.registerRenderer(new VanillaCaptureGuiRenderer());
        registry.registerRenderer(new CaptureRecordedGuiRenderer(frozenLayers));
        registry.registerBatch(new VisibleGuiBatchProvider());
        registry.registerBatch(clientGuis);
        registry.registerBatch(itemGuis);
        VanillaClientGuiFactories.register(clientGuis);
        VanillaPreviewGuiFactories.register(clientGuis);
        if (Minecraft.getMinecraft()
            .getResourceManager() instanceof IReloadableResourceManager manager) manager.registerReloadListener(this);
        guiAvailable = captureAvailable();
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        resourcesInvalidated = true;
    }

    private void refreshResources() {
        if (!resourcesInvalidated) return;
        resourcesInvalidated = false;
        discardPendingFrame();
        drawRecorder.close();
        releaseHighResolution();
        discardedHud.end();
        frozenLayers.close();
        pipeline.close();
        reusingLayers = false;
        if (session.recording() != null && !session.recording()
            .finished()
            && !session.recording()
                .paused())
            session.pauseRecording();
        if (batchRecording != null && !batchRecording.finished() && !batchRecording.paused())
            batchRecording.togglePause(System.nanoTime());
        session.invalidatePreview();
        refreshBatchCatalog();
    }

    public void useNativeKeyboard() {
        nativeKeyboard = true;
        OpenGuiCaptureHotkey.useNativeKeyboard();
    }

    public boolean guiAvailable() {
        return guiAvailable;
    }

    public void registerClientGui(CaptureClientGuiFactory factory) {
        clientGuis.register(factory);
    }

    public void registerItemGui(CaptureItemGuiFactory factory) {
        itemGuis.register(factory);
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

    private boolean captureAvailable() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.currentScreen != null || minecraft.theWorld != null && minecraft.thePlayer != null;
    }

    public boolean sourceMatches(GuiScreen screen) {
        if (session == null) return false;
        return session.sourceScreen() instanceof CaptureGameplayScreen
            ? screen == null && Minecraft.getMinecraft().theWorld != null
            : screen == session.sourceScreen();
    }

    public boolean gameplayInputCaptured() {
        return session != null && session.settings()
            .gameplay() && sourceMatches(Minecraft.getMinecraft().currentScreen);
    }

    public void toggleCaptureInterface() {
        if (overlay != null) overlay.toggleInterface();
    }

    public boolean captureTextInputFocused() {
        return overlay != null && overlay.textInputFocused();
    }

    public void open() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (session != null && !sourceMatches(minecraft.currentScreen)) close();
        if (session != null || !captureAvailable()) return;
        CaptureViewport viewport = CaptureCoordinateMapper.current(minecraft);
        GuiScreen source = minecraft.currentScreen;
        boolean gameplay = source == null;
        restoreGameplayFocus = gameplay && minecraft.inGameHasFocus;
        if (gameplay) {
            source = new CaptureGameplayScreen();
            source.setWorldAndResolution(minecraft, viewport.logicalWidth(), viewport.logicalHeight());
            minecraft.setIngameNotInFocus();
        }
        previousKeyRepeat = Keyboard.areRepeatEventsEnabled();
        captureThread = Thread.currentThread();
        Keyboard.enableRepeatEvents(true);
        session = new CaptureSession(source, viewport, gameplay, registry);
        OpenGuiCaptureHotkey.captureOpened();
        updateSourcePointer(session.viewport(), true);
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
            batch,
            this,
            registry);
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
            processInputEvents();
            if (session != null && screen == session.sourceScreen()) overlay.handleInput();
        }
    }

    public void routeGameplayInput() {
        Minecraft minecraft = Minecraft.getMinecraft();
        guiAvailable = captureAvailable();
        if (minecraft.currentScreen != null) return;
        activateRequested();
        OpenGuiCaptureHotkey.poll();
        if (!gameplayInputCaptured()) return;
        processInputEvents();
        if (gameplayInputCaptured()) overlay.handleInput();
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

    public void beginFrame(float partial) {
        if (session == null) return;
        refreshResources();
        drawRecorder.close();
        discardedHud.end();
        Minecraft minecraft = Minecraft.getMinecraft();
        CaptureViewport viewport = CaptureCoordinateMapper.current(minecraft);
        if (pendingReadback != null) {
            CaptureQueuedFrame queued = pendingReadback;
            if (!sourceMatches(minecraft.currentScreen) || !viewport.equals(
                queued.context()
                    .viewport())
                || !pipeline.readerContextMatches(queued.context())
                || !content().equals(
                    queued.identity()
                        .content())
                || !previewSelection().equals(
                    queued.identity()
                        .requestedBackend())
                || session.refreshRevision() != queued.revision()) discardPendingFrame();
            else completePendingReadback();
        }
        if (pendingReadback == null) discardPendingFrame();
        framePhases.close();
        if (renderProbe.shadowPass()) return;
        if (!sourceMatches(minecraft.currentScreen)) {
            unavailableSource();
            return;
        }
        frameId++;
        frameGeometry = null;
        renderedGeometry = null;
        cursorBounds = null;
        overlayDraws.clear();
        hotbarDrawn = false;
        tooltips.clear();
        drawnButtons.clear();
        partialTicks = partial;
        session.updateViewport(viewport);
        framePhases.begin(captureContext());
        try {
            boolean changed = pipeline.select(captureContext(), content(), backendSelection(), taskOwnsPipeline());
            if (changed || session.preview() != null && !session.previewMatches(content(), previewSelection()))
                session.invalidatePreview();
        } catch (RuntimeException error) {
            captureFailed(error);
            return;
        }
        if (batch.needsFrame() && pendingReadback == null) session.requestRefresh();
        long sampleTime = System.nanoTime();
        if (batchRecording != null) {
            batchRecording.tick(sampleTime);
            if (!batchRecording.finished() && !viewport.equals(
                batchRecording.request()
                    .viewport()))
                batchRecordingTargetChanged(sampleTime);
        }
        boolean batchFrame = batchRecording != null && batchRecording.needsFrame(sampleTime);
        boolean wanted = session.wantsFrame(sampleTime, batchRecording != null && batchRecording.sampling())
            || batchFrame;
        frameRequested = wanted && pendingReadback == null && highResolution == null;
        reusingLayers = session.settings()
            .config().freezePreview
            && !session.selections()
                .following()
            && !taskOwnsPipeline()
            && !batch.needsFrame()
            && pipeline.supportsLayerCapture()
            && frozenLayers.matches(captureContext(), content(), session.refreshRevision());
        if (frameRequested && !reusingLayers
            && pipeline.supportsLayerCapture()
            && session.recording() == null
            && batchRecording == null
            && (batch == null || !batch.busy()
                || batch.catalog()
                    .options()
                    .recording() == null)
            && !session.selections()
                .following()
            && !content().includeWorld()
            && renderProbe.capture(captureContext(), memoryBudget())
                .openGl())
            drawRecorder.begin(captureContext(), content(), memoryBudget());
        updateSourcePointer(viewport, livePreview());
    }

    private boolean livePreview() {
        return session != null && (session.livePreview() || batchRecording != null && batchRecording.sampling());
    }

    private void updateSourcePointer(CaptureViewport viewport, boolean sample) {
        if (sample) {
            sourceCursorX = Mouse.getX() / viewport.scaleX();
            sourceCursorY = (viewport.pixelHeight() - Mouse.getY() - 1) / viewport.scaleY();
            sourceMouseX = Mouse.getX() * viewport.logicalWidth() / viewport.pixelWidth();
            sourceMouseY = viewport.logicalHeight() - Mouse.getY() * viewport.logicalHeight() / viewport.pixelHeight()
                - 1;
        }
        sourceMouseX = Math.min(Math.max(0, sourceMouseX), viewport.logicalWidth() - 1);
        sourceMouseY = Math.min(Math.max(0, sourceMouseY), viewport.logicalHeight() - 1);
        sourceCursorX = Math.clamp(sourceCursorX, 0, viewport.logicalWidth() - 1 / viewport.scaleX());
        sourceCursorY = Math.clamp(sourceCursorY, 0, viewport.logicalHeight() - 1 / viewport.scaleY());
    }

    public int captureMouseX(int original) {
        return session != null && !livePreview() ? sourceMouseX : original;
    }

    public int captureMouseY(int original) {
        return session != null && !livePreview() ? sourceMouseY : original;
    }

    public void worldReady() {
        reachPhase(CaptureRenderPhase.WORLD_READY);
        if (session == null || !framePhases.active()
            || framePhases.phase() != CaptureRenderPhase.WORLD_READY
            || pipeline.guiOpen()
            || pendingReadback != null
            || !frameRequested
            || !content().includeWorld()
            || reusingLayers && frozenLayers.hasWorld()
            || !pipeline.supportsLayerCapture()
            || pendingWorld != null
            || Minecraft.getMinecraft().theWorld == null) return;
        try {
            // Read only a requested world layer; the owned buffer becomes the composed preview.
            pendingWorld = CaptureWorldReader.snapshot(captureContext(), pipeline.reader(), System.nanoTime());
        } catch (RuntimeException error) {
            captureFailed(error);
        }
    }

    public void beginGui(float partial) {
        if (session == null || !framePhases.active()) return;
        drawingLayer = CaptureTarget.GUI_LAYER;
        if (framePhases.phase() == null) worldReady();
        try {
            Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
            CaptureContext context = captureContext()
                .atPhase(CaptureRenderPhase.GUI_BEGIN, renderProbe.capture(captureContext(), memoryBudget()));
            if (pipeline.selection() != null && !pipeline.guiOpen()) pipeline.beginGui(
                context,
                content(),
                session.previewRequested() || session.selections()
                    .following());
            reachPhase(CaptureRenderPhase.GUI_BEGIN);
        } catch (RuntimeException error) {
            captureFailed(error);
        }
    }

    public void finishGui() {
        if (session == null || !framePhases.active()) return;
        if (framePhases.phase() == null || framePhases.phase() == CaptureRenderPhase.WORLD_READY)
            beginGui(partialTicks);
        if (!framePhases.active()) return;
        try {
            if (pipeline.drawing() && !reusingLayers) {
                if (frameRequested || batch.needsFrame()
                    || session.selections()
                        .following())
                    frameGeometry = registry.collect(captureContext());
                if (frameGeometry != null) {
                    Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
                    List<CaptureRect> masks = CaptureContentGeometry
                        .inventoryMask(tooltips.merge(frameGeometry), content());
                    drawRecorder.mask(masks, session.viewport());
                    CaptureLayerMask.clear(masks);
                }
            }
        } catch (RuntimeException error) {
            captureFailed(error);
        }
    }

    private boolean collectingOverlay() {
        if (Thread.currentThread() != captureThread || session == null
            || !frameRequested
            || reusingLayers
            || drawingTile
            || drawingCursor
            || discardedHud.active()
            || !pipeline.drawingOnCurrentFramebuffer()) return false;
        return CaptureTarget.HUD_LAYER.equals(drawingLayer) ? content().includeHud() : content().includeGui();
    }

    public void recordOverlayVertices(int[] vertices, int count, int used) {
        if (collectingOverlay()) overlayDraws.recordVertices(vertices, count, used, session.viewport(), drawingLayer);
    }

    public void recordOverlayDraw(int[] vertices, int count, int used, int mode, boolean texture, boolean color,
        boolean brightness, boolean normals) {
        if (!collectingOverlay()) return;
        overlayDraws.recordVertices(vertices, count, used, session.viewport(), drawingLayer);
        drawRecorder.mesh(vertices, count, used, mode, texture, color, brightness, normals);
    }

    public void beginOverlayText(FontRenderer font, String text, int x, int y, int color, boolean shadow) {
        if (Thread.currentThread() == captureThread && session != null && !drawingTile)
            drawRecorder.beginText(font, text, x, y, color, shadow, collectingOverlay());
    }

    public void endOverlayText() {
        if (Thread.currentThread() == captureThread && session != null && !drawingTile) drawRecorder.endText();
    }

    public void recordOverlayText(String text, int x, int y, boolean shadow, FontRenderer font) {
        if (collectingOverlay() && font != null && text != null && !text.isEmpty()) overlayDraws.recordText(
            text,
            x,
            y,
            font.getStringWidth(text),
            font.FONT_HEIGHT,
            shadow,
            session.viewport(),
            drawingLayer);
    }

    private void collectRenderedGeometry(CaptureFrame isolated) {
        if (!session.previewRequested() && !batch.needsFrame()
            && !session.selections()
                .following())
            return;
        CaptureGeometry current = frameGeometry == null ? registry.collect(captureContext()) : frameGeometry;
        if (hotbarDrawn && Minecraft.getMinecraft().thePlayer != null) {
            List<CaptureTarget> targets = new ArrayList<>(current.targets());
            long owner = Integer.toUnsignedLong(System.identityHashCode(Minecraft.getMinecraft().thePlayer.inventory));
            targets.addAll(CaptureHudGeometry.hotbar(session.viewport(), owner));
            current = new CaptureGeometry(targets.hashCode(), session.viewport(), targets);
        }
        current = tooltips.merge(drawnButtons.merge(current));
        current = overlayDraws.merge(current, isolated);
        if (isolated != null) {
            List<CaptureTarget> targets = new ArrayList<>(current.targets());
            String layer = session.settings()
                .gameplay() || !content().includeGui() ? CaptureTarget.HUD_LAYER : CaptureTarget.GUI_LAYER;
            targets.addAll(CaptureAlphaGeometry.collect(isolated, session.viewport(), layer, cursorBounds));
            current = new CaptureGeometry(targets.hashCode(), session.viewport(), targets);
        }
        renderedGeometry = CaptureContentGeometry.visible(current, content());
    }

    public void recordTooltip(CaptureRect bounds) {
        if (session != null && pipeline.drawing()
            && !discardedHud.active()
            && frameRequested
            && content().includeGui()
            && content().includeTooltips()) tooltips.record(bounds, session.viewport());
    }

    public void recordDrawnButton(GuiButton button) {
        if (Thread.currentThread() != captureThread || session == null
            || !frameRequested
            || reusingLayers
            || discardedHud.active()
            || !pipeline.drawing()
            || !content().includeGui()
            || !button.visible
            || button.width <= 0
            || button.height <= 0) return;
        if (((AccessorGuiCaptureScreen) session.sourceScreen()).guidenh$getCaptureButtons()
            .contains(button)) return;
        // Only manually drawn controls need projection; registered buttons already have screen coordinates.
        drawnButtons.record(
            button,
            session.viewport(),
            session.sourceScreen() instanceof GuiContainer ? "minecraft:window" : "minecraft:screen");
    }

    public void recordTooltip(List<String> lines, int x, int y, FontRenderer font) {
        if (session == null || lines.isEmpty() || font == null) return;
        recordTooltip(CaptureTooltipCollector.vanillaBounds(lines, x, y, font, session.viewport()));
    }

    public boolean prepareHud() {
        if (drawingTile) return highResolution != null && highResolution.content()
            .includeHud();
        if (session == null || !framePhases.active()) return true;
        drawingLayer = CaptureTarget.HUD_LAYER;
        if (reusingLayers) return false;
        if (!content().includeHud()) return false;
        if (pipeline.selection() == null || pipeline.guiOpen()) return true;
        try {
            pipeline.beginGui(
                captureContext()
                    .atPhase(CaptureRenderPhase.GUI_BEGIN, renderProbe.capture(captureContext(), memoryBudget())),
                content(),
                session.previewRequested() || session.selections()
                    .following());
        } catch (RuntimeException error) {
            captureFailed(error);
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHudElementPre(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR && hidePlayerInventoryHud())
            event.setCanceled(true);
    }

    @SubscribeEvent
    public void onHudElementPost(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR && collectingOverlay()) hotbarDrawn = true;
    }

    public boolean hidePlayerInventoryHud() {
        return Thread.currentThread() == captureThread && session != null
            && !discardedHud.active()
            && pipeline.drawing()
            && !content().includePlayerInventory();
    }

    public void finishFrameLayers() {
        if (session == null || !framePhases.active()) return;
        try {
            discardedHud.end();
            if (framePhases.phase() == null || framePhases.phase() == CaptureRenderPhase.WORLD_READY) finishGui();
            if (frameRequested && !reusingLayers && content().includeCursor() && pipeline.drawing()) {
                drawingCursor = true;
                try (CaptureRenderScope scope = new CaptureRenderScope()) {
                    Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
                    systemCursor.draw(session.viewport(), sourceCursorX, sourceCursorY);
                    cursorBounds = systemCursor.bounds(session.viewport(), sourceCursorX, sourceCursorY);
                } finally {
                    drawingCursor = false;
                }
            }
            reachPhase(CaptureRenderPhase.GUI_END);
            pipeline.finishGui(captureContext().atPhase(CaptureRenderPhase.GUI_END, null));
        } catch (RuntimeException error) {
            captureFailed(error);
        }
    }

    public void beginLateHud() {
        if (session == null || !framePhases.active() || !pipeline.guiOpen()) return;
        drawingLayer = CaptureTarget.HUD_LAYER;
        if (reusingLayers || !content().includeHud()) {
            try {
                discardedHud.begin(session.viewport());
            } catch (RuntimeException error) {
                captureFailed(error);
            }
        }
    }

    public void beforePresentation() {
        if (session != null) framePhases.preparePresentation();
    }

    public boolean hideBackdrop() {
        if (drawingTile) return !highResolution.content()
            .includeGuiBackdrop();
        return session != null && (pipeline.drawing() || drawingTile) && !content().includeGuiBackdrop();
    }

    public boolean hideTooltips() {
        if (drawingTile) return !highResolution.content()
            .includeTooltips();
        return session != null && pipeline.drawing() && (!content().includeGui() || !content().includeTooltips());
    }

    public boolean hideDebug() {
        if (drawingTile) return !highResolution.content()
            .includeDebug();
        return session != null && (pipeline.drawing() || drawingTile) && !content().includeDebug();
    }

    public boolean bindMainFramebuffer(Framebuffer target, boolean viewport) {
        if (session == null || target != Minecraft.getMinecraft()
            .getFramebuffer()) return false;
        if (drawingTile) {
            tileSource.bindCurrent(viewport);
            return true;
        }
        if (discardedHud.active()) {
            discardedHud.bind(viewport);
            return true;
        }
        return pipeline.bindMainFramebuffer(viewport);
    }

    public boolean hideNei() {
        return session != null && pipeline.drawing() && (!content().includeGui() || !content().includeNei());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSourceGuiDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (session != null && event.gui == session.sourceScreen() && (reusingLayers || !content().includeGui()))
            event.setCanceled(true);
    }

    public void frameReady() {
        if (session == null || !framePhases.readyToPresent()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!sourceMatches(minecraft.currentScreen)) {
            unavailableSource();
            return;
        }
        try (CaptureRenderScope drawing = new CaptureRenderScope()) {
            long now = System.nanoTime();
            reachPhase(CaptureRenderPhase.FRAME_READY);
            framePhases.presented();
            completePendingReadback();
            if (pendingFrame != null) {
                CaptureFrame frame = pendingFrame;
                pendingFrame = null;
                CaptureFrameIdentity identity = new CaptureFrameIdentity(
                    pendingIdentity.backend(),
                    pendingIdentity.requestedBackend(),
                    pendingIdentity.content(),
                    new CaptureReadbackMetadata(frameId, pendingPhase, pendingTarget, framePhases.snapshots()));
                try {
                    acceptCapturedFrame(
                        frame,
                        identity,
                        reusingLayers ? null : renderedGeometry,
                        session.previewRequested(),
                        captureContext());
                } catch (RuntimeException error) {
                    captureFailed(error);
                }
                pendingIdentity = null;
                pendingTarget = null;
                pendingPhase = null;
            }
            frameRequested = false;
            if (pendingSave && pendingReadback == null && session.canSave()) {
                pendingSave = false;
                saveNow();
            }
            if (highResolution != null && pendingReadback == null) {
                try {
                    drawingTile = true;
                    if (highResolution.step(tileSource, pipeline.reader())) {
                        CaptureRows rows = highResolution.takeRows();
                        CompletableFuture<CaptureOutputArtifact> completion = highResolutionCompletion;
                        CompletableFuture<CaptureOutputArtifact> writing = exporter.submitRows(
                            highResolution.request(),
                            rows,
                            highResolution.timestampNanos(),
                            highResolutionEncoder);
                        completion
                            .whenComplete((artifact, error) -> { if (completion.isCancelled()) writing.cancel(true); });
                        writing.whenComplete((artifact, error) -> {
                            if (error == null) completion.complete(artifact);
                            else completion.completeExceptionally(error);
                        });
                        highResolutionCompletion = null;
                        highResolution.close();
                        highResolution = null;
                        highResolutionEncoder = null;
                        tileSource.close();
                    } else session.notify(
                        I18n.format(
                            "guidenh.capture.rendering_tiles",
                            highResolution.completedTiles(),
                            highResolution.tileCount()));
                } catch (CaptureRedrawMismatchException mismatch) {
                    fallbackHighResolution(mismatch);
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
        guiAvailable = captureAvailable();
        activateRequested();
        OpenGuiCaptureHotkey.poll();
        if (session == null) return;
        sourceVisible = sourceMatches(Minecraft.getMinecraft().currentScreen);
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
        if (session.nextRecordingRequested() && session.recording() == null && captureAvailable()) {
            if (!sourceMatches(Minecraft.getMinecraft().currentScreen)) {
                adoptCurrentSource();
                session.selections()
                    .clearTracking();
            }
            if (session.canSave()) startRecording();
        }
        updateSchedule(System.nanoTime());
        if (!sourceVisible) unavailableSource();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiOpen(GuiOpenEvent event) {
        boolean restoringGameplay = event.gui instanceof CaptureGameplayScreen;
        if (restoringGameplay) {
            if (session != null) adoptSourceScreen(event.gui, true);
            event.gui = null;
        }
        if (event.gui == null && session != null
            && batch != null
            && batch.busy()
            && !session.settings()
                .gameplay()
            && !restoringGameplay
            && Minecraft.getMinecraft().theWorld != null) event.gui = new CaptureNavigationScreen();
        guiAvailable = event.gui != null || Minecraft.getMinecraft().theWorld != null;
        if (event.gui != null && !(event.gui instanceof CaptureNavigationScreen)) clientGuis.observe(event.gui);
        sourceVisible = sourceMatches(event.gui);
    }

    private void adoptSourceScreen(GuiScreen source, boolean gameplay) {
        if (session.sourceScreen() == source) return;
        CaptureViewport viewport = CaptureCoordinateMapper.current(Minecraft.getMinecraft());
        registry.closed(new CaptureContext(session.sourceScreen(), session.viewport(), frameId, partialTicks));
        session.adoptScreen(source, viewport, gameplay);
        geometry = null;
        sourceVisible = true;
        inputEvents.clear();
        registry.opened(new CaptureContext(source, viewport, frameId, partialTicks));
        Keyboard.enableRepeatEvents(true);
    }

    private void adoptCurrentSource() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (sourceMatches(minecraft.currentScreen)) return;
        boolean gameplay = minecraft.currentScreen == null;
        if (gameplay && minecraft.theWorld == null) return;
        GuiScreen source = gameplay ? new CaptureGameplayScreen() : minecraft.currentScreen;
        CaptureViewport viewport = CaptureCoordinateMapper.current(minecraft);
        if (gameplay) {
            source.setWorldAndResolution(minecraft, viewport.logicalWidth(), viewport.logicalHeight());
            minecraft.setIngameNotInFocus();
        }
        adoptSourceScreen(source, gameplay);
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
        try {
            discardedHud.close();
            pipeline.close();
        } catch (RuntimeException error) {
            session.notify(I18n.format("guidenh.capture.failed", error.getMessage()));
        }
        discardPendingFrame();
        frozenLayers.close();
        drawRecorder.close();
        reusingLayers = false;
        framePhases.close();
        renderProbe.reset();
        systemCursor.close();
        tooltips.clear();
        drawnButtons.clear();
        overlayDraws.clear();
        renderedGeometry = null;
        cursorBounds = null;
        hotbarDrawn = false;
        frameGeometry = null;
        geometry = null;
        exportBackend = null;
        exportContent = null;
        exportPreference = null;
        recordingPreference = null;
        session.close();
        Keyboard.enableRepeatEvents(previousKeyRepeat);
        overlay = null;
        session = null;
        captureThread = null;
        openedArtifact = null;
        sourceVisible = false;
        activationRequested = false;
        inputEvents.clear();
        CaptureClientConnection.instance()
            .closeCapture();
        if (Minecraft.getMinecraft().currentScreen instanceof CaptureNavigationScreen) Minecraft.getMinecraft()
            .displayGuiScreen(null);
        if (restoreGameplayFocus && Minecraft.getMinecraft().currentScreen == null
            && Minecraft.getMinecraft().theWorld != null)
            Minecraft.getMinecraft()
                .setIngameFocus();
        restoreGameplayFocus = false;
    }

    public void save() {
        if (!session.canSave()) return;
        if (session.settings()
            .animationFormat()) {
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
        CaptureRecording recording = session.recording();
        if (recording != null) return recording.request()
            .options()
            .content();
        if (highResolution != null) return highResolution.content();
        if (batchRecording != null) return batchRecording.request()
            .options()
            .content();
        if (exportContent != null && taskOwnsPipeline()) return exportContent;
        if (batch != null && batch.busy()) return batch.catalog()
            .options()
            .output()
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
            recordingPreference = session.previewIdentity()
                .requestedBackend();
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
        String policy = scheduleUnavailablePolicy();
        if (policy != null) {
            if (policy.equals("skip")) schedule.consumed(now);
            else if (policy.equals("stop")) schedule.cancel();
            else session.notify(I18n.format("guidenh.capture.schedule_waiting"));
            return;
        }
        if (!session.canSave()) return;
        schedule.consumed(now);
        if (!session.settings()
            .animationFormat()) {
            pendingSave = true;
            session.requestRefresh();
        } else startRecording();
    }

    private String scheduleUnavailablePolicy() {
        Minecraft minecraft = Minecraft.getMinecraft();
        String policy = null;
        if (!Display.isActive()) policy = mergeSchedulePolicy(policy, CaptureScheduleCondition.FOCUS);
        if (!Display.isVisible()) policy = mergeSchedulePolicy(policy, CaptureScheduleCondition.MINIMIZED);
        boolean noGui = minecraft.currentScreen == null && (!session.settings()
            .gameplay() || minecraft.theWorld == null);
        if (noGui) policy = mergeSchedulePolicy(policy, CaptureScheduleCondition.NO_GUI);
        if (minecraft.isGamePaused()) policy = mergeSchedulePolicy(policy, CaptureScheduleCondition.PAUSED);
        if (!sourceVisible && !noGui || session.targetMissing())
            policy = mergeSchedulePolicy(policy, CaptureScheduleCondition.TARGET);
        return policy;
    }

    private String mergeSchedulePolicy(String existing, CaptureScheduleCondition condition) {
        String policy = session.settings()
            .schedulePolicy(condition);
        if ("stop".equals(existing) || "stop".equals(policy)) return "stop";
        if ("skip".equals(existing) || "skip".equals(policy)) return "skip";
        return "wait";
    }

    private void unavailableSource() {
        sourceVisible = false;
        if (batch != null && batch.busy()) {
            batchRecordingTargetChanged(System.nanoTime());
            return;
        }
        CaptureRecording recording = session.recording();
        if (recording != null) {
            if (!recording.finished()) {
                session.targetChanged();
                if (recording.options()
                    .targetChangePolicy() == CaptureTargetChangePolicy.PAUSE)
                    session.notify(I18n.format("guidenh.capture.target_changed"));
            }
        } else if (schedule == null && !session.nextRecordingRequested()) close();
    }

    private void batchRecordingTargetChanged(long now) {
        if (batchRecording == null || batchRecording.finished()) return;
        if (batchRecording.options()
            .targetChangePolicy() == CaptureTargetChangePolicy.PAUSE) {
            if (!batchRecording.paused()) batchRecording.togglePause(now);
        } else batchRecording.stop(now);
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
        exportPreference = batch != null && batch.busy() ? batch.catalog()
            .options()
            .output()
            .backend()
            : session.previewIdentity()
                .requestedBackend();
        exportBackend = request.options()
            .backend();
        exportContent = request.options()
            .content();
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
        CaptureExporter encoder = registry.exporter(
            request.options()
                .format());
        CaptureRowExporter rowEncoder = encoder instanceof CaptureRowExporter supported ? supported : null;
        if (enlarged && quality.equals("strict") && rowEncoder == null)
            throw new IllegalArgumentException(I18n.format("guidenh.capture.no_row_exporter", encoder.format()));
        if (enlarged && !quality.equals("compatible") && rowEncoder != null) {
            CaptureContext context = new CaptureContext(
                session.sourceScreen(),
                session.viewport(),
                frameId,
                partialTicks);
            CaptureGuiRenderer renderer = registry.renderer(
                context,
                request.options()
                    .content());
            if (renderer != null && !request.options()
                .content()
                .includeWorld()
                && pipeline.supportsRedraw()
                && request.options()
                    .backend()
                    .equals(pipeline.selection())
                && request.viewport()
                    .equals(session.viewport())) {
                CaptureRenderSnapshot snapshot = renderer.snapshot(
                    context,
                    request.options()
                        .content(),
                    sourceMouseX,
                    sourceMouseY);
                try {
                    snapshot = new CaptureContentSnapshot(
                        snapshot,
                        registry.collect(context),
                        request.options()
                            .content());
                    if (request.options()
                        .content()
                        .includeCursor())
                        snapshot = systemCursor.snapshot(snapshot, context.viewport(), sourceCursorX, sourceCursorY);
                    highResolution = new CaptureHighResolutionJob(
                        request,
                        snapshot,
                        tileSource.tileLimit(tileSize, memoryBudget),
                        frame.timestampNanos(),
                        memoryBudget,
                        frame);
                } catch (IOException | RuntimeException error) {
                    snapshot.close();
                    throw error;
                }
                highResolutionEncoder = rowEncoder;
                highResolutionCompletion = new CompletableFuture<>();
                session.outputQuality("redrawn");
                return highResolutionCompletion;
            }
            if (quality.equals("strict"))
                throw new IllegalArgumentException(I18n.format("guidenh.capture.no_high_resolution"));
        }
        session.outputQuality(enlarged ? "resampled" : "native");
        return exporter.submit(request, frame, encoder);
    }

    public class RuntimeBatchHost implements CaptureBatchHost {

        @Override
        public CaptureBatchContext context() {
            Minecraft minecraft = Minecraft.getMinecraft();
            CaptureViewport viewport = CaptureCoordinateMapper.current(minecraft);
            CaptureGeometry current = geometry == null || !geometry.viewport()
                .equals(viewport) ? new CaptureGeometry(0, viewport, List.of()) : geometry;
            return new CaptureBatchContext(
                new CaptureContext(
                    session.sourceScreen(),
                    viewport,
                    frameId,
                    partialTicks,
                    framePhases.phase(),
                    framePhases.renderState()),
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
        public CaptureFrameIdentity frameIdentity() {
            return session.previewIdentity();
        }

        @Override
        public boolean readyForBatch() {
            return session.previewMatches(
                session.settings()
                    .content(),
                session.settings()
                    .backend())
                && !session.busy()
                && !pendingSave
                && highResolution == null;
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
            adoptCurrentSource();
        }

        @Override
        public void requestFrame() {
            session.requestRefresh();
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
        highResolutionEncoder = null;
        try {
            if (highResolution != null) highResolution.close();
        } catch (IOException error) {
            session.outputFailed(error);
        } finally {
            highResolution = null;
            tileSource.close();
        }
    }

    private void fallbackHighResolution(CaptureRedrawMismatchException mismatch) {
        CaptureHighResolutionJob failed = highResolution;
        CompletableFuture<CaptureOutputArtifact> completion = highResolutionCompletion;
        CaptureRowExporter encoder = highResolutionEncoder;
        highResolutionEncoder = null;
        highResolution = null;
        highResolutionCompletion = null;
        if (failed.originalRequest()
            .options()
            .resolution()
            .qualityPolicy()
            .equals("strict")) {
            try {
                failed.close();
            } catch (IOException cleanup) {
                mismatch.addSuppressed(cleanup);
            }
            completion.completeExceptionally(mismatch);
            tileSource.close();
            return;
        }
        try (CaptureFrame frame = failed.reference()) {
            CaptureExportRequest request = failed.originalRequest();
            failed.close();
            session.outputQuality("resampled");
            CompletableFuture<CaptureOutputArtifact> writing = exporter.submit(request, frame, encoder);
            completion.whenComplete((artifact, error) -> { if (completion.isCancelled()) writing.cancel(true); });
            writing.whenComplete((artifact, error) -> {
                if (error == null) completion.complete(artifact);
                else completion.completeExceptionally(error);
            });
        } catch (IOException | RuntimeException error) {
            completion.completeExceptionally(error);
        } finally {
            tileSource.close();
        }
    }

    public boolean capturingGuiLayer() {
        return Thread.currentThread() == captureThread && session != null
            && !discardedHud.active()
            && (pipeline.supportsRedraw() && pipeline.drawing() || drawingTile && tileSource.drawing());
    }

    public void shutdown() {
        if (batch != null) batch.close();
        batch = null;
        if (batchRecording != null) batchRecording.close();
        close();
        exporter.close();
    }
}
