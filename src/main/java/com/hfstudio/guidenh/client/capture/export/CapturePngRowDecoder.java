package com.hfstudio.guidenh.client.capture.export;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.InflaterInputStream;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.render.CaptureTileStore;

public class CapturePngRowDecoder {

    private static final long SIGNATURE = 0x89504E470D0A1A0AL;
    private static final int IHDR = 0x49484452;
    private static final int IDAT = 0x49444154;
    private static final int IEND = 0x49454E44;

    private CapturePngRowDecoder() {}

    public static boolean streamingFormat(Path source) throws IOException {
        try (DataInputStream input = new DataInputStream(Files.newInputStream(source))) {
            if (input.readLong() != SIGNATURE || input.readInt() != 13 || input.readInt() != IHDR) return false;
            byte[] header = new byte[13];
            input.readFully(header);
            return header[8] == 8 && (header[9] == 6 || header[9] == 2) && header[12] == 0;
        }
    }

    public static void decode(Path source, CaptureRect crop, CaptureTileStore target, long memoryBudget)
        throws IOException {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(source)))) {
            if (input.readLong() != SIGNATURE || input.readInt() != 13 || input.readInt() != IHDR)
                throw new IOException("The scroll source is not a supported PNG");
            byte[] header = new byte[13];
            input.readFully(header);
            CRC32 headerCrc = new CRC32();
            tag(headerCrc, IHDR);
            headerCrc.update(header);
            if ((int) headerCrc.getValue() != input.readInt()) throw new IOException("PNG header checksum mismatch");
            ByteBuffer dimensions = ByteBuffer.wrap(header);
            int width = dimensions.getInt();
            int height = dimensions.getInt();
            int channels = header[9] == 6 ? 4 : 3;
            if (width <= 0 || height <= 0
                || header[8] != 8
                || (header[9] != 6 && header[9] != 2)
                || header[10] != 0
                || header[11] != 0
                || header[12] != 0) throw new IOException("Unsupported PNG pixel format");
            int left = (int) crop.x();
            int top = (int) crop.y();
            int count = (int) crop.width();
            int rows = (int) crop.height();
            if (left < 0 || top < 0 || crop.right() > width || crop.bottom() > height)
                throw new IOException("The scroll viewport no longer matches its saved PNG");
            long buffers = (long) width * channels * 2 + count * 8L;
            if (buffers > memoryBudget)
                throw new IOException("PNG scanlines exceed the scroll composition memory budget");
            byte[] current = new byte[Math.multiplyExact(width, channels)];
            byte[] previous = new byte[current.length];
            int[] pixels = new int[count];
            IdatInputStream chunks = new IdatInputStream(input);
            try (DataInputStream decoded = new DataInputStream(new InflaterInputStream(chunks))) {
                for (int y = 0; y < top + rows; y++) {
                    if (Thread.currentThread()
                        .isInterrupted()) throw new IOException("Scroll decoding cancelled");
                    int filter = decoded.readUnsignedByte();
                    decoded.readFully(current);
                    unfilter(current, previous, channels, filter, (left + count) * channels);
                    if (y >= top) {
                        for (int x = 0; x < count; x++) {
                            int index = (left + x) * channels;
                            int alpha = channels == 4 ? current[index + 3] & 255 : 255;
                            pixels[x] = alpha << 24 | (current[index] & 255) << 16
                                | (current[index + 1] & 255) << 8
                                | current[index + 2] & 255;
                        }
                        target.putRow(y - top, pixels);
                    }
                    byte[] swap = previous;
                    previous = current;
                    current = swap;
                }
                chunks.finish();
            }
        } finally {
            target.releaseBuffer();
        }
    }

    private static void unfilter(byte[] current, byte[] previous, int channels, int filter, int count)
        throws IOException {
        if (filter < 0 || filter > 4) throw new IOException("Unsupported PNG scanline filter: " + filter);
        if (filter == 0) return;
        for (int index = 0; index < count; index++) {
            int left = index >= channels ? current[index - channels] & 255 : 0;
            int up = previous[index] & 255;
            int corner = index >= channels ? previous[index - channels] & 255 : 0;
            int predictor = switch (filter) {
                case 0 -> 0;
                case 1 -> left;
                case 2 -> up;
                case 3 -> (left + up) / 2;
                case 4 -> paeth(left, up, corner);
                default -> throw new IOException("Unsupported PNG scanline filter");
            };
            current[index] = (byte) ((current[index] & 255) + predictor);
        }
    }

    private static int paeth(int left, int up, int corner) {
        int predictor = left + up - corner;
        int fromLeft = Math.abs(predictor - left);
        int fromUp = Math.abs(predictor - up);
        int fromCorner = Math.abs(predictor - corner);
        return fromLeft <= fromUp && fromLeft <= fromCorner ? left : fromUp <= fromCorner ? up : corner;
    }

    private static void tag(CRC32 crc, int tag) {
        crc.update(tag >>> 24);
        crc.update(tag >>> 16);
        crc.update(tag >>> 8);
        crc.update(tag);
    }

    public static class IdatInputStream extends InputStream {

        private final DataInputStream input;
        private final CRC32 crc = new CRC32();
        private final byte[] scratch = new byte[8192];
        private int remaining;
        private boolean pendingCrc;
        private boolean ended;

        public IdatInputStream(DataInputStream input) {
            this.input = input;
        }

        private void checksum() throws IOException {
            if (pendingCrc && (int) crc.getValue() != input.readInt())
                throw new IOException("PNG data checksum mismatch");
            pendingCrc = false;
        }

        private boolean next() throws IOException {
            checksum();
            while (!ended) {
                int length = input.readInt();
                int type = input.readInt();
                if (length < 0) throw new IOException("Invalid PNG chunk length");
                crc.reset();
                tag(crc, type);
                remaining = length;
                pendingCrc = true;
                if (type == IDAT && length > 0) return true;
                while (remaining > 0) {
                    int count = Math.min(remaining, scratch.length);
                    input.readFully(scratch, 0, count);
                    crc.update(scratch, 0, count);
                    remaining -= count;
                }
                checksum();
                if (type == IEND) ended = true;
            }
            return false;
        }

        @Override
        public int read() throws IOException {
            if (remaining == 0 && !next()) return -1;
            int value = input.read();
            if (value < 0) throw new EOFException("Truncated PNG data");
            remaining--;
            crc.update(value);
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (length == 0) return 0;
            if (remaining == 0 && !next()) return -1;
            int count = Math.min(length, remaining);
            input.readFully(bytes, offset, count);
            crc.update(bytes, offset, count);
            remaining -= count;
            return count;
        }

        public void finish() throws IOException {
            while (read(scratch, 0, scratch.length) >= 0) {
                if (Thread.currentThread()
                    .isInterrupted()) throw new IOException("PNG decoding cancelled");
            }
        }
    }
}
