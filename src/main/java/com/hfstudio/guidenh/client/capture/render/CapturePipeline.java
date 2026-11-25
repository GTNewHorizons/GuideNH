package com.hfstudio.guidenh.client.capture.render;

import java.util.Objects;

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.CaptureCleanup;
import com.hfstudio.guidenh.client.capture.CaptureRegistry;
import com.hfstudio.guidenh.client.capture.api.CaptureBackendSelection;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureLayerSource;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryAccount;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReaderProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderPhase;
import com.hfstudio.guidenh.client.capture.api.CaptureSource;
import com.hfstudio.guidenh.client.capture.api.CaptureSourceProvider;

public class CapturePipeline implements AutoCloseable {

    private final CaptureRegistry registry;
    private final CaptureMemoryAccount memory;
    private CaptureSourceProvider sourceProvider;
    private CapturePixelReaderProvider readerProvider;
    private CaptureSource source;
    private CapturePixelReader reader;
    private Object sourceKey;
    private Object readerKey;
    private CaptureBackendSelection requested;
    private long revision = Long.MIN_VALUE;
    private boolean guiOpen;

    public CapturePipeline(CaptureRegistry registry) {
        this(registry, null);
    }

    public CapturePipeline(CaptureRegistry registry, CaptureMemoryAccount memory) {
        this.registry = registry;
        this.memory = memory;
    }

    public CaptureBackendSelection selection() {
        return source == null || reader == null ? null
            : new CaptureBackendSelection(sourceProvider.id(), readerProvider.id());
    }

    public boolean select(CaptureContext context, CaptureContentOptions content, CaptureBackendSelection selection,
        boolean locked) {
        if (guiOpen) throw new IllegalStateException("The capture pipeline cannot change while the GUI is drawing");
        boolean current = source != null && reader != null;
        if (current && selection.equals(requested)
            && (!selection.sourceId()
                .equals("automatic") || sourceProvider.automaticSelection(context, content))
            && revision == registry.backendRevision()
            && Objects.equals(sourceKey, sourceProvider.contextKey(context))
            && Objects.equals(readerKey, readerProvider.contextKey(context))
            && sourceProvider.availability(context, content)
                .available()
            && readerProvider.availability(context)
                .available())
            return false;
        CaptureSourceProvider nextSource = registry.sourceProvider(context, content, selection.sourceId());
        CapturePixelReaderProvider nextReader = registry.readerProvider(context, selection.readerId());
        Object nextSourceKey = nextSource.contextKey(context);
        Object nextReaderKey = nextReader.contextKey(context);
        if (current && locked
            && (nextSource != sourceProvider || nextReader != readerProvider
                || !Objects.equals(readerKey, nextReaderKey)))
            throw new IllegalStateException("The rendering backend changed during a capture task");
        boolean replaceSource = source == null || nextSource != sourceProvider
            || !Objects.equals(sourceKey, nextSourceKey);
        boolean replaceReader = reader == null || nextReader != readerProvider
            || !Objects.equals(readerKey, nextReaderKey);
        CaptureSource openedSource = null;
        CapturePixelReader openedReader = null;
        try {
            if (replaceSource) openedSource = Objects.requireNonNull(nextSource.open(context));
            if (replaceReader) openedReader = Objects.requireNonNull(nextReader.open(context));
            if (memory != null) {
                if (openedSource != null) openedSource.memoryAccount(memory);
                if (openedReader != null) openedReader = new CaptureAccountedPixelReader(openedReader, memory);
            }
        } catch (RuntimeException | Error error) {
            release(openedSource, openedReader, error);
            throw error;
        }
        CaptureSource previousSource = replaceSource ? source : null;
        CapturePixelReader previousReader = replaceReader ? reader : null;
        source = replaceSource ? openedSource : source;
        reader = replaceReader ? openedReader : reader;
        sourceProvider = nextSource;
        readerProvider = nextReader;
        sourceKey = nextSourceKey;
        readerKey = nextReaderKey;
        requested = selection;
        revision = registry.backendRevision();
        release(previousSource, previousReader, null);
        return replaceSource || replaceReader;
    }

    private static void release(CaptureSource source, CapturePixelReader reader, Throwable failure) {
        CaptureCleanup cleanup = new CaptureCleanup(failure);
        cleanup.close(source);
        cleanup.close(reader);
        if (failure == null) cleanup.rethrow();
    }

    public void beginGui(CaptureContext context, CaptureContentOptions content, boolean collectGeometry) {
        if (guiOpen) throw new IllegalStateException("The previous GUI capture has not finished");
        source.beginGui(context, content, collectGeometry);
        guiOpen = true;
    }

    public void finishGui(CaptureContext context) {
        if (!guiOpen) return;
        guiOpen = false;
        source.finishGui(context);
    }

    public boolean drawing() {
        return guiOpen && source != null && source.drawing();
    }

    public boolean drawingOnCurrentFramebuffer() {
        return drawing()
            && source.drawingOnFramebuffer(GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT));
    }

    public boolean guiOpen() {
        return guiOpen;
    }

    public boolean bindMainFramebuffer(boolean viewport) {
        return guiOpen && source != null && source.bindMainFramebuffer(viewport);
    }

    public boolean supportsRedraw() {
        return source != null && source.supportsRedraw();
    }

    public CapturePixelReader reader() {
        return reader;
    }

    public boolean readerContextMatches(CaptureContext context) {
        return readerProvider != null && Objects.equals(readerKey, readerProvider.contextKey(context));
    }

    public boolean supportsLayerCapture() {
        return source instanceof CaptureLayerSource;
    }

    public CaptureFrame captureGuiLayer(CaptureContext context, CapturePixelReader pixelReader, long timestamp) {
        if (!(source instanceof CaptureLayerSource layered))
            throw new IllegalStateException("The capture source does not expose an isolated GUI layer");
        if (context.phase() != source.capturePhase())
            throw new IllegalStateException("The capture source phase is not ready");
        CaptureProviderAvailability readiness = source.readiness(context);
        if (!readiness.available()) throw new IllegalStateException(readiness.reason());
        return validateFrame(context, layered.captureGuiLayer(context, pixelReader, timestamp));
    }

    public CaptureRenderPhase capturePhase() {
        return source == null ? null : source.capturePhase();
    }

    public CaptureReadback enqueueGuiLayer(CaptureContext context, CapturePixelReader pixelReader, long timestamp,
        long memoryBudget) {
        if (!(source instanceof CaptureLayerSource layered))
            throw new IllegalStateException("The capture source does not expose an isolated GUI layer");
        if (context.phase() != source.capturePhase())
            throw new IllegalStateException("The capture source phase is not ready");
        CaptureProviderAvailability readiness = source.readiness(context);
        if (!readiness.available()) throw new IllegalStateException(readiness.reason());
        return new CaptureAccountedReadback(
            new CaptureSizedReadback(
                Objects.requireNonNull(layered.enqueueGuiLayer(context, pixelReader, timestamp, memoryBudget)),
                context.viewport()),
            memory);
    }

    public CaptureFrame capture(CaptureContext context, CapturePixelReader pixelReader, long timestamp) {
        if (source == null || reader == null) throw new IllegalStateException("The capture pipeline is not available");
        if (context.phase() != source.capturePhase())
            throw new IllegalStateException("The capture source phase is not ready");
        CaptureProviderAvailability readiness = source.readiness(context);
        if (!readiness.available()) throw new IllegalStateException(readiness.reason());
        return validateFrame(context, source.capture(context, pixelReader, timestamp));
    }

    private CaptureFrame validateFrame(CaptureContext context, CaptureFrame result) {
        CaptureFrame frame = Objects.requireNonNull(result);
        if (frame.width() != context.viewport()
            .pixelWidth() || frame.height()
                != context.viewport()
                    .pixelHeight()) {
            frame.close();
            throw new IllegalStateException("The capture source returned dimensions that differ from the viewport");
        }
        return CaptureAccountedReadback.account(frame, memory);
    }

    public CaptureFrame captureCompletedFrame(CaptureContext context, long timestamp) {
        if (guiOpen) throw new IllegalStateException("The GUI capture has not finished");
        return capture(context, reader, timestamp);
    }

    @Override
    public void close() {
        CaptureSource previousSource = source;
        CapturePixelReader previousReader = reader;
        source = null;
        reader = null;
        sourceProvider = null;
        readerProvider = null;
        requested = null;
        sourceKey = readerKey = null;
        revision = Long.MIN_VALUE;
        guiOpen = false;
        release(previousSource, previousReader, null);
    }
}
