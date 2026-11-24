package com.hfstudio.guidenh.client.capture.cursor;

import java.util.BitSet;

import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.render.CapturePixelConverter;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.win32.StdCallLibrary;

public class WindowsSystemCursorProvider implements SystemCursorProvider {

    public interface User32 extends StdCallLibrary {

        boolean GetCursorInfo(CursorInfo info);

        boolean GetIconInfo(Pointer cursor, IconInfo info);

        Pointer GetDC(Pointer window);

        int ReleaseDC(Pointer window, Pointer dc);
    }

    public interface Gdi32 extends StdCallLibrary {

        int GetObjectW(Pointer object, int bytes, Bitmap bitmap);

        int GetDIBits(Pointer dc, Pointer bitmap, int start, int lines, Pointer pixels, BitmapInfo info, int usage);

        boolean DeleteObject(Pointer object);
    }

    @FieldOrder({ "x", "y" })
    public static class Point extends Structure {

        public int x;
        public int y;
    }

    @FieldOrder({ "size", "flags", "cursor", "position" })
    public static class CursorInfo extends Structure {

        public int size;
        public int flags;
        public Pointer cursor;
        public Point position = new Point();
    }

    @FieldOrder({ "icon", "hotspotX", "hotspotY", "mask", "color" })
    public static class IconInfo extends Structure {

        public int icon;
        public int hotspotX;
        public int hotspotY;
        public Pointer mask;
        public Pointer color;
    }

    @FieldOrder({ "type", "width", "height", "widthBytes", "planes", "bitsPixel", "bits" })
    public static class Bitmap extends Structure {

        public int type;
        public int width;
        public int height;
        public int widthBytes;
        public short planes;
        public short bitsPixel;
        public Pointer bits;
    }

    @FieldOrder({ "size", "width", "height", "planes", "bitCount", "compression", "sizeImage", "xPixelsPerMeter",
        "yPixelsPerMeter", "colorsUsed", "colorsImportant", "color" })
    public static class BitmapInfo extends Structure {

        public int size = 40;
        public int width;
        public int height;
        public short planes = 1;
        public short bitCount = 32;
        public int compression;
        public int sizeImage;
        public int xPixelsPerMeter;
        public int yPixelsPerMeter;
        public int colorsUsed;
        public int colorsImportant;
        public int color;
    }

    private final User32 user = Native.load("user32", User32.class);
    private final Gdi32 gdi = Native.load("gdi32", Gdi32.class);
    private long cursorHandle;
    private CaptureCursorImage image;

    @Override
    public CaptureCursorImage current() {
        CursorInfo cursor = new CursorInfo();
        cursor.size = cursor.size();
        if (!user.GetCursorInfo(cursor)) throw failure("GetCursorInfo");
        if (cursor.cursor == null) return null;
        long handle = Pointer.nativeValue(cursor.cursor);
        if (image != null && cursorHandle == handle) return image;
        IconInfo icon = new IconInfo();
        if (!user.GetIconInfo(cursor.cursor, icon)) throw failure("GetIconInfo");
        Pointer dc = user.GetDC(null);
        try {
            Bitmap bitmap = describe(icon.color != null ? icon.color : icon.mask);
            int width = bitmap.width;
            int height = icon.color != null ? bitmap.height : bitmap.height / 2;
            int count = Math.multiplyExact(width, height);
            int[] pixels = icon.color == null ? new int[count] : read(dc, icon.color, width, height);
            boolean alpha = false;
            for (int pixel : pixels) alpha |= pixel >>> 24 != 0;
            BitSet inverted = new BitSet(count);
            if (alpha) CapturePixelConverter.normalize(pixels, CaptureAlphaMode.PREMULTIPLIED);
            else {
                int[] mask = read(dc, icon.mask, width, icon.color == null ? height * 2 : height);
                for (int index = 0; index < count; index++) {
                    boolean and = (mask[index] & 0xFFFFFF) != 0;
                    if (icon.color != null) pixels[index] = and ? 0 : pixels[index] | 0xFF000000;
                    else {
                        boolean xor = (mask[count + index] & 0xFFFFFF) != 0;
                        if (and && xor) inverted.set(index);
                        pixels[index] = and ? 0 : xor ? 0xFFFFFFFF : 0xFF000000;
                    }
                }
            }
            image = new CaptureCursorImage(width, height, icon.hotspotX, icon.hotspotY, pixels, inverted);
            cursorHandle = handle;
            return image;
        } finally {
            if (dc != null) user.ReleaseDC(null, dc);
            if (icon.mask != null) gdi.DeleteObject(icon.mask);
            if (icon.color != null) gdi.DeleteObject(icon.color);
        }
    }

    private Bitmap describe(Pointer handle) {
        Bitmap bitmap = new Bitmap();
        if (handle == null || gdi.GetObjectW(handle, bitmap.size(), bitmap) == 0) throw failure("GetObject");
        return bitmap;
    }

    private int[] read(Pointer dc, Pointer bitmap, int width, int height) {
        BitmapInfo info = new BitmapInfo();
        info.width = width;
        info.height = -height;
        info.sizeImage = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        try (Memory data = new Memory(info.sizeImage)) {
            if (gdi.GetDIBits(dc, bitmap, 0, height, data, info, 0) != height) throw failure("GetDIBits");
            return data.getIntArray(0, width * height);
        }
    }

    private static IllegalStateException failure(String operation) {
        return new IllegalStateException("System cursor " + operation + " failed: " + Native.getLastError());
    }
}
