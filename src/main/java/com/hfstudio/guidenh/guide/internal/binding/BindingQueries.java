package com.hfstudio.guidenh.guide.internal.binding;

import java.util.List;

import com.hfstudio.guidenh.client.GuideClientTasks;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.api.binding.BindingSearchResult;
import com.hfstudio.guidenh.integration.api.binding.BindingViewContext;

public class BindingQueries {

    protected BindingQueries() {}

    public static BindingSearchResult search(String type, String text, int offset, int limit) {
        if (type == null || type.isBlank()) return new BindingSearchResult(List.of(), 0, 0);
        return GuideClientTasks.call(() -> {
            var provider = GuideNhIntegrationRegistry.global()
                .bindingProvider(BindingKey.normalizeType(type));
            if (provider == null) return new BindingSearchResult(List.of(), 0, 0);
            return provider.search(
                text,
                offset,
                limit,
                new BindingViewContext(null, null, LangUtil.getCurrentLanguage(), false, true, true));
        });
    }
}
