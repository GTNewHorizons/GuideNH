package com.hfstudio.guidenh.integration.betterquesting;

import com.hfstudio.guidenh.integration.api.binding.GuideBindingType;

public class BetterQuestingBindingType implements GuideBindingType {

    @Override
    public String id() {
        return "betterquesting";
    }

    @Override
    public String normalizeId(String rawId) {
        var uuid = QuestIdParser.parse(rawId);
        if (uuid == null) throw new IllegalArgumentException("Invalid BetterQuesting quest id: " + rawId);
        return uuid.toString();
    }
}
