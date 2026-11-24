package com.hfstudio.guidenh.client.capture.export;

import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;

public class CaptureImageReadCancellation implements IIOReadProgressListener {

    private void check(ImageReader reader) {
        if (Thread.currentThread()
            .isInterrupted()) reader.abort();
    }

    @Override
    public void sequenceStarted(ImageReader reader, int index) {
        check(reader);
    }

    @Override
    public void sequenceComplete(ImageReader reader) {}

    @Override
    public void imageStarted(ImageReader reader, int index) {
        check(reader);
    }

    @Override
    public void imageProgress(ImageReader reader, float progress) {
        check(reader);
    }

    @Override
    public void imageComplete(ImageReader reader) {}

    @Override
    public void thumbnailStarted(ImageReader reader, int imageIndex, int thumbnailIndex) {
        check(reader);
    }

    @Override
    public void thumbnailProgress(ImageReader reader, float progress) {
        check(reader);
    }

    @Override
    public void thumbnailComplete(ImageReader reader) {}

    @Override
    public void readAborted(ImageReader reader) {}
}
