package com.hfstudio.guidenh.client.capture.cursor;

import java.util.BitSet;

import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.render.CapturePixelConverter;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

public class X11SystemCursorProvider implements SystemCursorProvider {

    public interface X11 extends Library {

        Pointer XOpenDisplay(String name);

        int XCloseDisplay(Pointer display);

        int XFree(Pointer pointer);
    }

    public interface XFixes extends Library {

        Pointer XFixesGetCursorImage(Pointer display);
    }

    @FieldOrder({ "x", "y", "width", "height", "hotspotX", "hotspotY", "serial", "pixels" })
    public static class CursorImage extends Structure {

        public short x;
        public short y;
        public short width;
        public short height;
        public short hotspotX;
        public short hotspotY;
        public NativeLong serial;
        public Pointer pixels;

        public CursorImage(Pointer pointer) {
            super(pointer);
            read();
        }
    }

    private final X11 x11 = Native.load("X11", X11.class);
    private final XFixes fixes = Native.load("Xfixes", XFixes.class);
    private Pointer display = x11.XOpenDisplay(null);
    private long serial;
    private CaptureCursorImage image;

    @Override
    public CaptureCursorImage current() {
        if (display == null) throw new IllegalStateException("The display server does not expose system cursor pixels");
        Pointer pointer = fixes.XFixesGetCursorImage(display);
        if (pointer == null) throw new IllegalStateException("XFixes could not read the system cursor");
        try {
            CursorImage cursor = new CursorImage(pointer);
            if (image != null && serial == cursor.serial.longValue()) return image;
            int width = Short.toUnsignedInt(cursor.width);
            int height = Short.toUnsignedInt(cursor.height);
            if (width == 0 || height == 0) return null;
            int[] pixels = new int[Math.multiplyExact(width, height)];
            for (int index = 0; index < pixels.length; index++)
                pixels[index] = Native.LONG_SIZE == 8 ? (int) cursor.pixels.getLong(index * 8L)
                    : cursor.pixels.getInt(index * 4L);
            CapturePixelConverter.normalize(pixels, CaptureAlphaMode.PREMULTIPLIED);
            image = new CaptureCursorImage(
                width,
                height,
                Short.toUnsignedInt(cursor.hotspotX),
                Short.toUnsignedInt(cursor.hotspotY),
                pixels,
                new BitSet());
            serial = cursor.serial.longValue();
            return image;
        } finally {
            x11.XFree(pointer);
        }
    }

    @Override
    public void close() {
        if (display != null) x11.XCloseDisplay(display);
        display = null;
        image = null;
    }
}
