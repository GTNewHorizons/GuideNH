package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

public class GuideSiteAnimatedPng {

    private GuideSiteAnimatedPng() {}

    public record Frame(BufferedImage image, int ticks) {

        public Frame {
            if (image == null || ticks <= 0) {
                throw new IllegalArgumentException("Animated PNG frames require an image and positive duration");
            }
        }
    }

    public static byte[] encode(List<Frame> frames) throws IOException {
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("Animated PNG requires at least one frame");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeLong(0x89504e470d0a1a0aL);
        int width = frames.getFirst()
            .image()
            .getWidth();
        int height = frames.getFirst()
            .image()
            .getHeight();
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        DataOutputStream header = new DataOutputStream(headerBytes);
        header.writeInt(width);
        header.writeInt(height);
        header.write(new byte[] { 8, 6, 0, 0, 0 });
        chunk(out, "IHDR", headerBytes.toByteArray());
        ByteArrayOutputStream controlBytes = new ByteArrayOutputStream();
        DataOutputStream control = new DataOutputStream(controlBytes);
        control.writeInt(frames.size());
        control.writeInt(0);
        chunk(out, "acTL", controlBytes.toByteArray());
        int sequence = 0;
        BufferedImage previous = null;
        for (Frame frame : frames) {
            BufferedImage image = frame.image();
            if (image.getWidth() != width || image.getHeight() != height || frame.ticks() > 65535) {
                throw new IllegalArgumentException("Animated PNG frame dimensions or duration are invalid");
            }
            int[] rect = changedRectangle(previous, image);
            ByteArrayOutputStream frameBytes = new ByteArrayOutputStream();
            DataOutputStream frameControl = new DataOutputStream(frameBytes);
            frameControl.writeInt(sequence++);
            frameControl.writeInt(rect[2]);
            frameControl.writeInt(rect[3]);
            frameControl.writeInt(rect[0]);
            frameControl.writeInt(rect[1]);
            frameControl.writeShort(frame.ticks());
            frameControl.writeShort(20);
            frameControl.writeByte(0);
            frameControl.writeByte(0);
            chunk(out, "fcTL", frameBytes.toByteArray());

            BufferedImage crop = new BufferedImage(rect[2], rect[3], BufferedImage.TYPE_INT_ARGB);
            crop.setRGB(
                0,
                0,
                rect[2],
                rect[3],
                image.getRGB(rect[0], rect[1], rect[2], rect[3], null, 0, rect[2]),
                0,
                rect[2]);
            ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
            if (!ImageIO.write(crop, "png", pngBytes)) {
                throw new IOException("No PNG writer is available");
            }
            // ImageIO owns PNG compression; APNG adds frame control and sequence numbers.
            DataInputStream png = new DataInputStream(new ByteArrayInputStream(pngBytes.toByteArray()));
            png.readLong();
            while (png.available() > 0) {
                int length = png.readInt();
                String type = new String(png.readNBytes(4), StandardCharsets.US_ASCII);
                byte[] data = png.readNBytes(length);
                png.readInt();
                if (!"IDAT".equals(type)) continue;
                if (previous == null) {
                    chunk(out, "IDAT", data);
                } else {
                    ByteArrayOutputStream partBytes = new ByteArrayOutputStream();
                    DataOutputStream part = new DataOutputStream(partBytes);
                    part.writeInt(sequence++);
                    part.write(data);
                    chunk(out, "fdAT", partBytes.toByteArray());
                }
            }
            previous = image;
        }
        chunk(out, "IEND", new byte[0]);
        return bytes.toByteArray();
    }

    private static int[] changedRectangle(BufferedImage previous, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (previous == null) return new int[] { 0, 0, width, height };
        int left = width, right = -1, top = height, bottom = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (previous.getRGB(x, y) != image.getRGB(x, y)) {
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        return right < 0 ? new int[] { 0, 0, 1, 1 } : new int[] { left, top, right - left + 1, bottom - top + 1 };
    }

    private static void chunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        out.writeInt(data.length);
        out.write(name);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }
}
