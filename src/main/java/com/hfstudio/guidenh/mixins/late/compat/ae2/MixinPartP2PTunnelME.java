package com.hfstudio.guidenh.mixins.late.compat.ae2;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.hfstudio.guidenh.integration.ae2.Ae2ExternalGridPart;

import appeng.me.helpers.AENetworkProxy;
import appeng.parts.p2p.PartP2PTunnelME;

@Mixin(value = PartP2PTunnelME.class, remap = false)
public abstract class MixinPartP2PTunnelME implements Ae2ExternalGridPart {

    @Shadow
    @Final
    private AENetworkProxy outerProxy;

    @Override
    public AENetworkProxy guideNh$getExternalConnectionProxy() {
        return outerProxy;
    }
}
