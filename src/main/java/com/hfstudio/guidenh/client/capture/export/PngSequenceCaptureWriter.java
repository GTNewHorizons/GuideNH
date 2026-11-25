package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordedFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;
import com.hfstudio.guidenh.client.capture.export.CaptureAnimationManifest.FrameEntry;

public class PngSequenceCaptureWriter implements CaptureAnimationWriter {

    private final CaptureExportRequest request;
    private final CaptureRecordingOptions options;
    private final Path target;
    private final PngCaptureExporter png = new PngCaptureExporter();
    private CaptureFrameTimeline timeline;
    private CaptureOutputSize size;
    private long bytes;
    private boolean finished;
    private boolean closed;

    public PngSequenceCaptureWriter(CaptureExportRequest request, CaptureRecordingOptions options, Path target) {
        this.request = request;
        this.options = options;
        this.target = target;
    }

    private Path framePath(int index) {
        return target.resolve(String.format("frame_%06d.png", index));
    }

    @Override
    public void append(CaptureRecordedFrame frame) throws IOException {
        checkOpen();
        checkCancellation();
        long timestamp = timeline == null || timeline.size() == 0 ? 0 : frame.elapsedNanos();
        if (timestamp < 0 || timeline != null && timeline.size() > 0 && timestamp < timeline.last())
            throw new IOException("Animation frame times must be nonnegative and ordered");
        if (timeline != null && timeline.size() == Integer.MAX_VALUE)
            throw new IOException("The animation frame count exceeds its representable range");
        long frameBudget = remainingBytes() - Long.BYTES;
        if (frameBudget < 0) throw new IOException("The animation timeline exceeds the configured disk budget");
        CaptureExportRequest current = frame.request(request);
        CaptureOptions initial = current.options();
        CaptureExportRequest remaining = new CaptureExportRequest(
            current.taskId(),
            current.sourceId(),
            current.viewport(),
            current.regions(),
            new CaptureOptions(
                initial.resolution(),
                initial.content(),
                initial.layout(),
                initial.outputDirectory(),
                frameBudget,
                initial.backend(),
                initial.format()));
        CaptureRowComposer rows = new CaptureRowComposer(remaining, frame.frame());
        if (size != null && !size.equals(rows.size()))
            throw new IOException("Animation frames must retain their initial output dimensions");
        if (timeline == null) timeline = new CaptureFrameTimeline(target);
        CaptureOutputArtifact artifact = png.exportRows(remaining, rows, framePath(timeline.size()));
        size = rows.size();
        bytes = Math.addExact(bytes, artifact.byteSize());
        timeline.append(timestamp);
        checkBudget();
    }

    private void checkBudget() throws IOException {
        if (remainingBytes() < 0) throw new IOException("The animation exceeds the configured disk budget");
    }

    private long remainingBytes() {
        return request.options()
            .temporaryBudgetBytes() - bytes
            - (timeline == null ? 0 : timeline.byteSize());
    }

    @Override
    public CaptureOutputArtifact finish(long elapsedNanos) throws IOException {
        checkOpen();
        checkCancellation();
        if (timeline == null || timeline.size() == 0) throw new IOException("No GUI frames were captured");
        int count = timeline.size();
        long duration = Math.max(elapsedNanos, Math.addExact(timeline.last(), 1_000_000_000L / options.fps()));
        finished = true;
        CaptureSpritePageLayout layout = options.format()
            .equals("sprites") ? spritePages() : null;
        Gson gson = new Gson();
        Path metadata = target.resolve("animation.json");
        try (
            Writer output = new OutputStreamWriter(
                new CaptureBudgetOutputStream(Files.newOutputStream(metadata), remainingBytes()),
                StandardCharsets.UTF_8);
            JsonWriter json = new JsonWriter(output)) {
            json.setIndent("  ");
            json.setHtmlSafe(true);
            json.setSerializeNulls(false);
            json.beginObject();
            json.name("sourceId")
                .value(request.sourceId());
            json.name("width")
                .value(size.width());
            json.name("height")
                .value(size.height());
            json.name("durationNanos")
                .value(duration);
            json.name("selections");
            gson.toJson(request.regions(), List.class, json);
            json.name("frames")
                .beginArray();
            long start = timeline.time(0);
            for (int index = 0; index < count; index++) {
                checkCancellation();
                long end = index + 1 < count ? timeline.time(index + 1) : duration;
                int offset = layout == null ? 0 : index % layout.framesPerPage();
                String file = layout == null ? framePath(index).getFileName()
                    .toString() : pageName(index / layout.framesPerPage());
                gson.toJson(
                    new FrameEntry(
                        file,
                        layout == null ? 0 : layout.x(offset),
                        layout == null ? 0 : layout.y(offset),
                        size.width(),
                        size.height(),
                        start,
                        end - start),
                    FrameEntry.class,
                    json);
                start = end;
            }
            json.endArray()
                .endObject();
        }
        bytes = Math.addExact(bytes, Files.size(metadata));
        checkBudget();
        timeline.close();
        timeline = null;
        return new CaptureOutputArtifact(target, options.format(), size.width(), size.height(), count, bytes);
    }

    private static String pageName(int page) {
        return String.format("sprites_%04d.png", page);
    }

    private CaptureSpritePageLayout spritePages() throws IOException {
        CaptureSpritePageLayout layout = CaptureSpritePageLayout.create(size, options);
        int perPage = layout.framesPerPage();
        for (int first = 0, page = 0; first < timeline.size(); page++) {
            checkCancellation();
            int count = Math.min(perPage, timeline.size() - first);
            int pageStart = first;
            Path output = target.resolve(pageName(page));
            try (CaptureSpritePageRows rows = new CaptureSpritePageRows(
                layout,
                size,
                count,
                index -> framePath(pageStart + index))) {
                CaptureOutputArtifact artifact = png.exportRows(rows, output, remainingBytes());
                bytes = Math.addExact(bytes, artifact.byteSize());
            }
            checkBudget();
            for (int offset = 0; offset < count; offset++) {
                checkCancellation();
                Path source = framePath(first + offset);
                long sourceBytes = Files.size(source);
                Files.delete(source);
                bytes -= sourceBytes;
            }
            first += count;
        }
        return layout;
    }

    private void checkOpen() {
        if (closed || finished) throw new IllegalStateException("The animation writer no longer accepts frames");
    }

    private static void checkCancellation() throws IOException {
        if (Thread.currentThread()
            .isInterrupted()) throw new IOException("Animation export cancelled");
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        if (timeline != null) timeline.close();
        timeline = null;
    }
}
