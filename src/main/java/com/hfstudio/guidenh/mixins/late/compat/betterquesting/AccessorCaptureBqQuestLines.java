package com.hfstudio.guidenh.mixins.late.compat.betterquesting;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import betterquesting.api.questing.IQuestLine;
import betterquesting.api2.client.gui.panels.lists.CanvasQuestLine;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.utils.Tuple2;
import betterquesting.client.gui2.GuiQuestLines;

@Mixin(value = GuiQuestLines.class, remap = false)
public interface AccessorCaptureBqQuestLines {

    @Accessor("visChapters")
    List<Tuple2<Map.Entry<UUID, IQuestLine>, Integer>> guidenh$getCaptureChapters();

    @Accessor("cvQuest")
    CanvasQuestLine guidenh$getCaptureQuestCanvas();

    @Accessor("cvLines")
    CanvasScrolling guidenh$getCaptureChapterList();

    @Accessor("cvDesc")
    CanvasScrolling guidenh$getCaptureDescription();

    @Invoker("openQuestLine")
    void guidenh$openCaptureChapter(Map.Entry<UUID, IQuestLine> chapter);

    @Accessor("selectedLineId")
    static UUID guidenh$getCaptureSelectedChapter() {
        throw new AssertionError();
    }

    @Accessor("selectedLineId")
    static void guidenh$setCaptureSelectedChapter(UUID id) {
        throw new AssertionError();
    }
}
