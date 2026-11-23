package com.hfstudio.guidenh.integration.betterquesting.capture;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.gui.GuiScreen;

import com.hfstudio.guidenh.mixins.late.compat.betterquesting.AccessorCaptureBqQuest;
import com.hfstudio.guidenh.mixins.late.compat.betterquesting.AccessorCaptureBqQuestLines;

import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.client.gui2.GuiHome;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.client.gui2.GuiQuest.ScrollPosition;

public class BqCaptureState {

    private final UUID chapter;
    private final GuiScreen bookmark;
    private final BqCanvasState canvas;
    private final Map<UUID, ScrollPosition> positions = new LinkedHashMap<>();
    private final Map<UUID, QuestScroll> scrolls = new LinkedHashMap<>();
    private final Set<UUID> touched = new HashSet<>();

    public record QuestScroll(int task, int reward, int description) {}

    public BqCaptureState(GuiScreenCanvas screen) {
        chapter = AccessorCaptureBqQuestLines.guidenh$getCaptureSelectedChapter();
        bookmark = GuiHome.bookmark;
        canvas = new BqCanvasState(screen);
        if (screen instanceof GuiQuest) touchQuest(((AccessorCaptureBqQuest) screen).guidenh$getCaptureQuestId());
    }

    public void touchQuest(UUID id) {
        if (!touched.add(id)) return;
        ScrollPosition position = AccessorCaptureBqQuest.guidenh$getCaptureScrollPositions()
            .get(id);
        if (position != null) {
            positions.put(id, position);
            scrolls.put(
                id,
                new QuestScroll(position.getTaskScrollY(), position.getRewardScrollY(), position.getDescScrollY()));
        }
    }

    public void restoreGlobals() {
        AccessorCaptureBqQuestLines.guidenh$setCaptureSelectedChapter(chapter);
        GuiHome.bookmark = bookmark;
        Map<UUID, ScrollPosition> live = AccessorCaptureBqQuest.guidenh$getCaptureScrollPositions();
        for (UUID id : touched) {
            ScrollPosition position = positions.get(id);
            if (position == null) live.remove(id);
            else {
                QuestScroll scroll = scrolls.get(id);
                position.setTaskScrollY(scroll.task());
                position.setRewardScrollY(scroll.reward());
                position.setDescScrollY(scroll.description());
                live.put(id, position);
            }
        }
    }

    public void restoreCanvas(GuiScreenCanvas screen) {
        canvas.restore(screen);
    }

    public boolean restored(GuiScreenCanvas screen) {
        return canvas.restored(screen);
    }
}
