package com.hfstudio.guidenh.integration.api.binding;

import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public interface GuideBindingProvider {

    String type();

    BindingDisplay describe(BindingKey key, BindingViewContext context);

    BindingSearchResult search(String query, int offset, int limit, BindingViewContext context);

    BindingOpenResult open(BindingKey key, GuideUiHost origin);

    long revision();
}
