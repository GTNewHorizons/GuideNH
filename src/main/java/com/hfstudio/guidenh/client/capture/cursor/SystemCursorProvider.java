package com.hfstudio.guidenh.client.capture.cursor;

public interface SystemCursorProvider extends AutoCloseable {

    CaptureCursorImage current();

    @Override
    default void close() {}
}
