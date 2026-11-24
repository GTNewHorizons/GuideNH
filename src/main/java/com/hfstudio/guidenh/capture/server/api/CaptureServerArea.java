package com.hfstudio.guidenh.capture.server.api;

public record CaptureServerArea(int dimension, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public CaptureServerArea {
        if (minX > maxX || minY > maxY
            || minZ > maxZ
            || minY < 0
            || maxY > 255
            || minX < -30_000_000
            || maxX > 30_000_000
            || minZ < -30_000_000
            || maxZ > 30_000_000)
            throw new IllegalArgumentException("The selected capture area exceeds world coordinates");
    }

    public boolean contains(double x, double y, double z) {
        return x >= minX && x < (double) maxX + 1
            && y >= minY
            && y < (double) maxY + 1
            && z >= minZ
            && z < (double) maxZ + 1;
    }

    public static CaptureServerArea parse(String text, int dimension) {
        if (text == null || text.isBlank()) return null;
        String[] corners = text.trim()
            .split(":", -1);
        if (corners.length != 2) throw new IllegalArgumentException("Use x,y,z:x,y,z for the capture area");
        int[] first = corner(corners[0]);
        int[] second = corner(corners[1]);
        return new CaptureServerArea(
            dimension,
            Math.min(first[0], second[0]),
            Math.min(first[1], second[1]),
            Math.min(first[2], second[2]),
            Math.max(first[0], second[0]),
            Math.max(first[1], second[1]),
            Math.max(first[2], second[2]));
    }

    private static int[] corner(String text) {
        String[] parts = text.split(",", -1);
        if (parts.length != 3) throw new IllegalArgumentException("Each capture corner needs three coordinates");
        return new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
            Integer.parseInt(parts[2].trim()) };
    }
}
