package com.hfstudio.guidenh.client.capture.export;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import com.hfstudio.guidenh.client.capture.api.CaptureAnimationWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordedFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;

public class GifCaptureWriter implements CaptureAnimationWriter {

    private final CaptureExportRequest request;
    private final CaptureRecordingOptions options;
    private final Path target;
    private final ImageWriter writer;
    private final ImageOutputStream output;
    private final CaptureGifPalette palette = new CaptureGifPalette();
    private BufferedImage pending;
    private long pendingTime;
    private long writtenCentiseconds;
    private int frames;
    private int width;
    private int height;

    public GifCaptureWriter(CaptureExportRequest request, CaptureRecordingOptions options, Path target)
        throws IOException {
        this.request = request;
        this.options = options;
        this.target = target;
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        if (!writers.hasNext()) throw new IOException("No GIF encoder is available");
        writer = writers.next();
        ImageOutputStream opened = null;
        try {
            opened = new CaptureBudgetImageOutputStream(target, request.options().temporaryBudgetBytes());
            writer.setOutput(opened);
            writer.prepareWriteSequence(null);
            output = opened;
        } catch (IOException | RuntimeException | Error failure) {
            if (opened != null) {
                try {
                    opened.close();
                } catch (IOException | RuntimeException cleanup) {
                    failure.addSuppressed(cleanup);
                }
            }
            try {
                writer.dispose();
            } catch (RuntimeException cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    @Override
    public void append(CaptureRecordedFrame frame) throws IOException {
        CaptureRowComposer rows = new CaptureRowComposer(frame.request(request), frame.frame());
        if (rows.size()
            .width() > 65535
            || rows.size()
                .height() > 65535)
            throw new IOException("GIF dimensions exceed 65535");
        if (pending != null) writePending(frame.elapsedNanos());
        pending = palette.convert(
            rows,
            options,
            request.options()
                .content()
                .backgroundArgb());
        pendingTime = frames == 0 ? 0 : frame.elapsedNanos();
        width = rows.size()
            .width();
        height = rows.size()
            .height();
    }

    private void writePending(long endTime) throws IOException {
        long targetCentiseconds = Math.round(endTime / 10_000_000d);
        long delay = Math.max(1, targetCentiseconds - writtenCentiseconds);
        while (delay > 0) {
            int part = (int) Math.min(65535, delay);
            IIOMetadata metadata = writer.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromRenderedImage(pending),
                writer.getDefaultWriteParam());
            String format = metadata.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            IIOMetadataNode control = child(root, "GraphicControlExtension");
            control.setAttribute("disposalMethod", "restoreToBackgroundColor");
            control.setAttribute("userInputFlag", "FALSE");
            control.setAttribute("transparentColorFlag", options.matteTransparency() ? "FALSE" : "TRUE");
            control.setAttribute("delayTime", Integer.toString(part));
            control.setAttribute("transparentColorIndex", "0");
            if (frames == 0) {
                IIOMetadataNode application = new IIOMetadataNode("ApplicationExtension");
                application.setAttribute("applicationID", "NETSCAPE");
                application.setAttribute("authenticationCode", "2.0");
                application
                    .setUserObject(new byte[] { 1, (byte) options.loopCount(), (byte) (options.loopCount() >>> 8) });
                child(root, "ApplicationExtensions").appendChild(application);
            }
            metadata.setFromTree(format, root);
            writer.writeToSequence(new IIOImage(pending, null, metadata), writer.getDefaultWriteParam());
            writtenCentiseconds += part;
            delay -= part;
            frames++;
            if (output.length() > request.options()
                .temporaryBudgetBytes()) throw new IOException("GIF output exceeds the configured disk budget");
        }
    }

    private static IIOMetadataNode child(IIOMetadataNode parent, String name) {
        for (int index = 0; index < parent.getLength(); index++) {
            if (parent.item(index)
                .getNodeName()
                .equals(name)) return (IIOMetadataNode) parent.item(index);
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        parent.appendChild(node);
        return node;
    }

    @Override
    public CaptureOutputArtifact finish(long elapsedNanos) throws IOException {
        if (pending == null) throw new IOException("No GUI frames were captured");
        writePending(Math.max(elapsedNanos, pendingTime + 1_000_000_000L / options.fps()));
        writer.endWriteSequence();
        output.flush();
        return new CaptureOutputArtifact(target, "gif", width, height, frames, output.length());
    }

    @Override
    public void close() throws IOException {
        pending = null;
        try {
            output.close();
        } finally {
            writer.dispose();
        }
    }
}
