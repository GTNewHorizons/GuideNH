package com.hfstudio.guidenh.integration.ae2;

import org.jetbrains.annotations.Nullable;

import appeng.api.util.AEColor;

public class Ae2CableConnectionRules {

    private Ae2CableConnectionRules() {}

    public static boolean shouldConnect(boolean sourceHasSidePart, boolean sourceBlocked, boolean sourceCanConnect,
        boolean neighborCanConnect, boolean neighborFaceBlockedByPart, boolean neighborBlocked,
        boolean neighborAcceptsSide, @Nullable AEColor sourceCableColor, @Nullable AEColor neighborCableColor) {
        return !sourceHasSidePart && !sourceBlocked
            && sourceCanConnect
            && neighborCanConnect
            && !neighborFaceBlockedByPart
            && !neighborBlocked
            && neighborAcceptsSide
            && areCableColorsCompatible(sourceCableColor, neighborCableColor);
    }

    public static boolean facePartBlocksAdjacentCable(boolean hasFacePart, boolean facePartCanConnect) {
        return hasFacePart && !facePartCanConnect;
    }

    public static boolean areCableColorsCompatible(@Nullable AEColor color1, @Nullable AEColor color2) {
        if (color1 == null || color2 == null) {
            return true;
        }
        return color1 == AEColor.Transparent || color2 == AEColor.Transparent || color1 == color2;
    }
}
