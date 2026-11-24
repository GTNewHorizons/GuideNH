package com.hfstudio.guidenh.client.capture.cursor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.render.CaptureCursorRenderer;
import com.hfstudio.guidenh.client.capture.render.CaptureCursorSnapshot;
import com.sun.jna.Platform;

public class CaptureSystemCursor implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger("GuideNH system cursor");

    private SystemCursorProvider provider;
    private CaptureCursorImage image;
    private CaptureCursorRenderer renderer;
    private IllegalStateException unavailable;
    private boolean sampled;

    private CaptureCursorImage current() {
        if (unavailable != null) throw unavailable;
        try {
            if (provider == null) {
                if (Platform.isWindows()) provider = new WindowsSystemCursorProvider();
                else if (Platform.isLinux() || Platform.isFreeBSD()) provider = new X11SystemCursorProvider();
                else throw new IllegalStateException("The platform does not expose a system cursor capture adapter");
            }
            return provider.current();
        } catch (LinkageError error) {
            LOGGER.error("Could not initialize the native system cursor API", error);
            unavailable = new IllegalStateException("The native system cursor API is unavailable", error);
            throw unavailable;
        }
    }

    public void draw(CaptureViewport viewport, double x, double y) {
        CaptureCursorImage current = current();
        sampled = true;
        if (current == null) {
            image = null;
            return;
        }
        if (image != current) {
            if (renderer != null) renderer.close();
            image = current;
            renderer = new CaptureCursorRenderer(current);
        }
        renderer.draw(viewport, x, y);
    }

    public CaptureRenderSnapshot snapshot(CaptureRenderSnapshot delegate, CaptureViewport viewport, double x,
        double y) {
        CaptureCursorImage current = sampled ? image : current();
        return current == null ? delegate : new CaptureCursorSnapshot(delegate, current, viewport, x, y);
    }

    public CaptureRect bounds(CaptureViewport viewport, double x, double y) {
        return image == null ? null
            : new CaptureRect(
                x - image.hotspotX() / viewport.scaleX(),
                y - image.hotspotY() / viewport.scaleY(),
                image.width() / viewport.scaleX(),
                image.height() / viewport.scaleY()).intersect(viewport.bounds());
    }

    @Override
    public void close() {
        try {
            if (renderer != null) renderer.close();
        } finally {
            renderer = null;
            image = null;
            unavailable = null;
            sampled = false;
            try {
                if (provider != null) provider.close();
            } finally {
                provider = null;
            }
        }
    }
}
