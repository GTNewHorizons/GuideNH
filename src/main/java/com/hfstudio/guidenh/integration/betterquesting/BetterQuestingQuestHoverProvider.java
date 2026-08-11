package com.hfstudio.guidenh.integration.betterquesting;

import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.api.client.QuestHoverProvider;

import betterquesting.api2.client.gui.context.IQuestHoverListener;

public class BetterQuestingQuestHoverProvider implements QuestHoverProvider, IQuestHoverListener {

    @Nullable
    private Object currentTarget;

    @Override
    public boolean isQuestHoverAvailable() {
        return Mods.BetterQuesting.isModLoaded();
    }

    @Override
    public @Nullable UUID currentHoveredQuestId() {
        if (!(currentTarget instanceof Map.Entry<?, ?>)) return null;

        Object key = ((Map.Entry<?, ?>) currentTarget).getKey();
        return key instanceof UUID ? (UUID) key : null;
    }

    @Override
    public void onQuestHoverChanged(@Nullable Object target) {
        currentTarget = target;
    }

    @Override
    public @Nullable PageAnchor findQuestHoverPage(MutableGuide guide, UUID questId) {
        try {
            return guide.getIndex(QuestIndex.class)
                .findByUuid(questId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
