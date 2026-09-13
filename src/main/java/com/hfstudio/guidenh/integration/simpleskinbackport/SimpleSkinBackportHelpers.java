package com.hfstudio.guidenh.integration.simpleskinbackport;

import net.minecraft.client.entity.AbstractClientPlayer;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.common.Optional;
import roadhog360.simpleskinbackport.ducks.IArmsState;
import roadhog360.simpleskinbackport.ducks.INewBipedModel;

public class SimpleSkinBackportHelpers {

    private SimpleSkinBackportHelpers() {}

    @Nullable
    @Optional.Method(modid = "simpleskinbackport")
    public static Boolean resolveSlim(AbstractClientPlayer player) {
        return player instanceof IArmsState arms ? arms.ssb$isSlim() : null;
    }

    @Optional.Method(modid = "simpleskinbackport")
    public static boolean tryInitialize64xModel(Object model) {
        if (!(model instanceof INewBipedModel biped)) {
            return false;
        }
        biped.ssb$set64x();
        return true;
    }
}
