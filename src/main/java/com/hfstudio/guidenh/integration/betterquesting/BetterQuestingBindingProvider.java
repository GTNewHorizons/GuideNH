package com.hfstudio.guidenh.integration.betterquesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;
import com.hfstudio.guidenh.integration.api.binding.BindingDisplay;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.api.binding.BindingOpenResult;
import com.hfstudio.guidenh.integration.api.binding.BindingSearchResult;
import com.hfstudio.guidenh.integration.api.binding.BindingState;
import com.hfstudio.guidenh.integration.api.binding.BindingViewContext;
import com.hfstudio.guidenh.integration.api.binding.GuideBindingProvider;

public class BetterQuestingBindingProvider implements GuideBindingProvider {

    @Override
    public String type() {
        return "betterquesting";
    }

    @Override
    public BindingDisplay describe(BindingKey key, BindingViewContext context) {
        var uuid = UUID.fromString(key.id());
        var player = context.staticView() ? null : Minecraft.getMinecraft().thePlayer;
        QuestDisplay display = BqHelpers.resolveDisplay(uuid, player, context.includeDescription());
        BindingState state = display.getState() == QuestState.HIDDEN ? BindingState.HIDDEN
            : display.getState() == QuestState.MISSING ? BindingState.MISSING
                : context.staticView() ? BindingState.UNKNOWN
                    : display.getState() == QuestState.COMPLETED ? BindingState.COMPLETED
                        : display.getState() == QuestState.LOCKED ? BindingState.LOCKED : BindingState.VISIBLE;
        String description = display.getDescription();
        String title = display.getName();
        if (state.equals(BindingState.COMPLETED)) title += " ✓";
        return new BindingDisplay(
            key,
            state,
            title,
            state.equals(BindingState.LOCKED) ? null : description,
            null,
            description == null || description.isEmpty() ? List.of() : List.of(description),
            List.of(),
            state != BindingState.HIDDEN && state != BindingState.MISSING);
    }

    @Override
    public BindingSearchResult search(String query, int offset, int limit, BindingViewContext context) {
        String filter = query == null ? "" : query.toLowerCase(Locale.ROOT);
        var entries = new ArrayList<BindingDisplay>();
        for (var entry : BqHelpers.allQuests()) {
            BindingDisplay display = describe(
                new BindingKey(
                    type(),
                    entry.getKey()
                        .toString()),
                context);
            if (display.state()
                .equals(BindingState.HIDDEN)) continue;
            if (filter.isEmpty() || display.key()
                .id()
                .toLowerCase(Locale.ROOT)
                .contains(filter)
                || BindingRuntime.title(display)
                    .toLowerCase(Locale.ROOT)
                    .contains(filter))
                entries.add(display);
        }
        entries.sort(
            (a, b) -> a.key()
                .id()
                .compareTo(
                    b.key()
                        .id()));
        int start = Math.clamp(offset, 0, entries.size());
        int end = (int) Math.min(entries.size(), (long) start + Math.max(0, limit));
        return new BindingSearchResult(entries.subList(start, end), entries.size(), entries.hashCode());
    }

    @Override
    public BindingOpenResult open(BindingKey key, GuideUiHost origin) {
        var id = UUID.fromString(key.id());
        var display = describe(key, BindingRuntime.view(false, false));
        if (!display.nativeOpenable()) return BindingOpenResult.failure("guidenh.binding.missing");
        BqHelpers.openQuestGui(id, origin instanceof GuiScreen gui ? gui : Minecraft.getMinecraft().currentScreen);
        return BindingOpenResult.success();
    }

    @Override
    public long revision() {
        return 0;
    }
}
