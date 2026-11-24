package com.hfstudio.guidenh.capture.server.api;

public record CaptureServerQuery(int radius, int maxTargets, boolean temporaryTravel, CaptureServerArea area) {

    public CaptureServerQuery(int radius, int maxTargets, boolean temporaryTravel) {
        this(radius, maxTargets, temporaryTravel, null);
    }

    public boolean includes(double x, double y, double z, double originX, double originY, double originZ) {
        return area == null
            ? Math.abs(x - originX) <= radius && Math.abs(y - originY) <= radius && Math.abs(z - originZ) <= radius
            : area.contains(x, y, z);
    }
}
