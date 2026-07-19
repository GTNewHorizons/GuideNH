package com.hfstudio.guidenh.integration.ae2;

import net.minecraftforge.common.util.ForgeDirection;

import appeng.me.helpers.AENetworkProxy;

public class Ae2CableConnectionRules {

    private Ae2CableConnectionRules() {}

    public static boolean shouldConnect(AENetworkProxy source, ForgeDirection sourceDirection, AENetworkProxy target,
        ForgeDirection targetDirection) {
        return source.getConnectableSides()
            .contains(sourceDirection)
            && target.getConnectableSides()
                .contains(targetDirection)
            && source.getColor()
                .matches(target.getColor());
    }
}
