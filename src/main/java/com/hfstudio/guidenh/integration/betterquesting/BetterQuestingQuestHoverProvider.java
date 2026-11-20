package com.hfstudio.guidenh.integration.betterquesting;

import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.integration.api.client.QuestHoverProvider;

import betterquesting.api2.client.gui.context.IQuestHoverListener;

public class BetterQuestingQuestHoverProvider implements QuestHoverProvider, IQuestHoverListener {

    @Nullable
    private Object currentTarget;
    @Nullable
    private GuiScreen sourceScreen;

    @Override
    public @Nullable UUID currentHoveredQuestId() {
        if (Minecraft.getMinecraft().currentScreen != sourceScreen || !(currentTarget instanceof Entry<?, ?>)) {
            return null;
        }

        Object key = ((Entry<?, ?>) currentTarget).getKey();
        return key instanceof UUID ? (UUID) key : null;
    }

    @Override
    public void onQuestHoverChanged(@Nullable Object target) {
        currentTarget = target;
        sourceScreen = target == null ? null : Minecraft.getMinecraft().currentScreen;
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
