package com.hfstudio.guidenh.client.capture.export;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;

public class PngCaptureExporter implements CaptureExporter {

    @Override
    public String format() {
        return "png";
    }

    @Override
    public CaptureOutputArtifact export(CaptureExportRequest request, CaptureFrame frame, Path target)
        throws IOException {
        return exportRows(request, new CaptureRowComposer(request, frame), target);
    }

    public CaptureOutputArtifact exportRows(CaptureExportRequest request, CaptureRows rows, Path target)
        throws IOException {
        CaptureOutputSize size = rows.size();
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(target))) {
            output.writeLong(0x89504E470D0A1A0AL);
            ByteArrayOutputStream headerBytes = new ByteArrayOutputStream(13);
            try (DataOutputStream header = new DataOutputStream(headerBytes)) {
                header.writeInt(size.width());
                header.writeInt(size.height());
                header.writeByte(8);
                header.writeByte(6);
                header.writeByte(0);
                header.writeByte(0);
                header.writeByte(0);
            }
            chunk(output, "IHDR", headerBytes.toByteArray(), 13);
            IdatOutputStream idat = new IdatOutputStream(
                output,
                request.options()
                    .temporaryBudgetBytes());
            Deflater deflater = new Deflater();
            try (DeflaterOutputStream compressed = new DeflaterOutputStream(idat, deflater, 65536)) {
                int[] row = new int[size.width()];
                byte[] filtered = new byte[Math.addExact(Math.multiplyExact(size.width(), 4), 1)];
                filtered[0] = 1;
                for (int y = 0; y < size.height(); y++) {
                    if (Thread.currentThread()
                        .isInterrupted()) throw new IOException("Capture export cancelled");
                    rows.row(y, row);
                    int previous = 0;
                    for (int x = 0; x < row.length; x++) {
                        int pixel = row[x];
                        int offset = x * 4 + 1;
                        filtered[offset] = (byte) (((pixel >>> 16) & 255) - ((previous >>> 16) & 255));
                        filtered[offset + 1] = (byte) (((pixel >>> 8) & 255) - ((previous >>> 8) & 255));
                        filtered[offset + 2] = (byte) ((pixel & 255) - (previous & 255));
                        filtered[offset + 3] = (byte) ((pixel >>> 24) - (previous >>> 24));
                        previous = pixel;
                    }
                    compressed.write(filtered);
                }
                compressed.finish();
                idat.flush();
            } finally {
                deflater.end();
            }
            chunk(output, "IEND", new byte[0], 0);
        }
        return new CaptureOutputArtifact(target, format(), size.width(), size.height(), 1, Files.size(target));
    }

    public static void chunk(DataOutputStream output, String type, byte[] bytes, int count) throws IOException {
        byte[] tag = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(tag);
        crc.update(bytes, 0, count);
        output.writeInt(count);
        output.write(tag);
        output.write(bytes, 0, count);
        output.writeInt((int) crc.getValue());
    }

    public static class IdatOutputStream extends OutputStream {

        private final DataOutputStream output;
        private final long budget;
        private final byte[] buffer = new byte[65536];
        private int count;
        private long written;

        public IdatOutputStream(DataOutputStream output, long budget) {
            this.output = output;
            this.budget = budget;
        }

        @Override
        public void write(int value) throws IOException {
            if (count == buffer.length) flush();
            buffer[count++] = (byte) value;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            while (length > 0) {
                int copied = Math.min(length, buffer.length - count);
                System.arraycopy(bytes, offset, buffer, count, copied);
                offset += copied;
                length -= copied;
                count += copied;
                if (count == buffer.length) flush();
            }
        }

        @Override
        public void flush() throws IOException {
            if (count == 0) return;
            if (written + count + 12 > budget)
                throw new IOException("Capture output exceeds the configured disk budget");
            chunk(output, "IDAT", buffer, count);
            written += count + 12;
            count = 0;
        }
    }
}
