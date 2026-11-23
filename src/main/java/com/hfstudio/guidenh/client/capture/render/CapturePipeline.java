package com.hfstudio.guidenh.client.capture.render;

import java.util.Objects;

import com.hfstudio.guidenh.client.capture.CaptureRegistry;
import com.hfstudio.guidenh.client.capture.api.CaptureBackendSelection;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReaderProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;
import com.hfstudio.guidenh.client.capture.api.CaptureSource;
import com.hfstudio.guidenh.client.capture.api.CaptureSourceProvider;

public class CapturePipeline implements AutoCloseable {

    private final CaptureRegistry registry;
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
        this.registry = registry;
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
        } catch (RuntimeException error) {
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

    private static void release(CaptureSource source, CapturePixelReader reader, RuntimeException failure) {
        RuntimeException error = failure;
        try {
            if (source != null) source.close();
        } catch (RuntimeException closing) {
            if (error == null) error = closing;
            else error.addSuppressed(closing);
        }
        try {
            if (reader != null) reader.close();
        } catch (RuntimeException closing) {
            if (error == null) error = closing;
            else error.addSuppressed(closing);
        }
        if (failure == null && error != null) throw error;
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

    public boolean bindMainFramebuffer(boolean viewport) {
        return guiOpen && source != null && source.bindMainFramebuffer(viewport);
    }

    public boolean supportsRedraw() {
        return source != null && source.supportsRedraw();
    }

    public CapturePixelReader reader() {
        return reader;
    }

    public CaptureFrame captureCompletedFrame(CaptureContext context, long timestamp) {
        if (guiOpen) throw new IllegalStateException("The GUI capture has not finished");
        if (source == null || reader == null) throw new IllegalStateException("The capture pipeline is not available");
        CaptureProviderAvailability readiness = source.readiness(context);
        if (!readiness.available()) throw new IllegalStateException(readiness.reason());
        CaptureFrame frame = Objects.requireNonNull(source.captureCompletedFrame(context, reader, timestamp));
        if (frame.width() != context.viewport()
            .pixelWidth() || frame.height()
                != context.viewport()
                    .pixelHeight()) {
            frame.close();
            throw new IllegalStateException("The capture source returned dimensions that differ from the viewport");
        }
        return frame;
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
