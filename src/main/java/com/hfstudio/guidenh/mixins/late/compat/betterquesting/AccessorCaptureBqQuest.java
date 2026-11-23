package com.hfstudio.guidenh.mixins.late.compat.betterquesting;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.client.gui2.GuiQuest.ScrollPosition;

@Mixin(value = GuiQuest.class, remap = false)
public interface AccessorCaptureBqQuest {

    @Accessor("questID")
    UUID guidenh$getCaptureQuestId();

    @Accessor("csTask")
    CanvasScrolling guidenh$getCaptureTasks();

    @Accessor("csReward")
    CanvasScrolling guidenh$getCaptureRewards();

    @Accessor("csDesc")
    CanvasScrolling guidenh$getCaptureDescription();

    @Accessor("scrollsPositions")
    static Map<UUID, ScrollPosition> guidenh$getCaptureScrollPositions() {
        throw new AssertionError();
    }
}
