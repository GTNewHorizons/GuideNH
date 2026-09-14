package com.hfstudio.guidenh.integration.simpleskinbackport;

import net.minecraft.client.entity.AbstractClientPlayer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.Mods;

import cpw.mods.fml.common.Optional;
import roadhog360.simpleskinbackport.ducks.IArmsState;
import roadhog360.simpleskinbackport.ducks.INewBipedModel;

public class SimpleSkinBackportHelpers {

    private SimpleSkinBackportHelpers() {}

    @Nullable
    public static Boolean resolveSlim(AbstractClientPlayer player) {
        return Mods.SimpleSkinBackport.isModLoaded() ? resolveSlimImpl(player) : null;
    }

    public static boolean tryInitialize64xModel(Object model) {
        return model != null && Mods.SimpleSkinBackport.isModLoaded() && tryInitialize64xModelImpl(model);
    }

    @Nullable
    @Optional.Method(modid = "simpleskinbackport")
    private static Boolean resolveSlimImpl(AbstractClientPlayer player) {
        return player instanceof IArmsState arms ? arms.ssb$isSlim() : null;
    }

    @Optional.Method(modid = "simpleskinbackport")
    private static boolean tryInitialize64xModelImpl(Object model) {
        if (!(model instanceof INewBipedModel biped)) {
            return false;
        }
        biped.ssb$set64x();
        return true;
    }
}
