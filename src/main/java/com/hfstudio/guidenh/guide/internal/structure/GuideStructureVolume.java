package com.hfstudio.guidenh.guide.internal.structure;

public class GuideStructureVolume {

    private GuideStructureVolume() {}

    public static long blockCount(int sizeX, int sizeY, int sizeZ) {
        return (long) sizeX * (long) sizeY * (long) sizeZ;
    }

    public static boolean exceedsLimit(int sizeX, int sizeY, int sizeZ, long limit) {
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0 || limit < 0) {
            return true;
        }
        long area = (long) sizeX * sizeY;
        return area > limit || sizeZ > limit / area;
    }
}
