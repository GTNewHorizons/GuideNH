package com.hfstudio.guidenh.mixins.late.compat.nei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import codechicken.nei.RestartableTask;

@Mixin(value = RestartableTask.class, remap = false)
public interface AccessorCaptureNeiTask {

    @Accessor("thread")
    Thread guidenh$getCaptureThread();
}
