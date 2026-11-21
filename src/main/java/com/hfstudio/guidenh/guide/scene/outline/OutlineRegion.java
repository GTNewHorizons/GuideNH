package com.hfstudio.guidenh.guide.scene.outline;

import net.minecraft.util.AxisAlignedBB;

public record OutlineRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public OutlineRegion {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("OutlineRegion min must not exceed max");
        }
    }

    public static OutlineRegion of(int x0, int y0, int z0, int x1, int y1, int z1) {
        return new OutlineRegion(
            Math.min(x0, x1),
            Math.min(y0, y1),
            Math.min(z0, z1),
            Math.max(x0, x1),
            Math.max(y0, y1),
            Math.max(z0, z1));
    }

    public static OutlineRegion single(int x, int y, int z) {
        return new OutlineRegion(x, y, z, x, y, z);
    }

    public OutlineRegion translate(int dx, int dy, int dz) {
        return new OutlineRegion(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean intersects(AxisAlignedBB box) {
        return box.maxX > minX && box.minX < maxX + 1
            && box.maxY > minY
            && box.minY < maxY + 1
            && box.maxZ > minZ
            && box.minZ < maxZ + 1;
    }
}
