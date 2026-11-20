package com.hfstudio.guidenh.integration.thaumcraft;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;
import com.hfstudio.guidenh.integration.api.binding.BindingDisplay;
import com.hfstudio.guidenh.integration.api.binding.BindingIcon;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.api.binding.BindingOpenResult;
import com.hfstudio.guidenh.integration.api.binding.BindingSearchResult;
import com.hfstudio.guidenh.integration.api.binding.BindingState;
import com.hfstudio.guidenh.integration.api.binding.BindingViewContext;
import com.hfstudio.guidenh.integration.api.binding.GuideBindingProvider;

import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;

public class ThaumcraftBindingProvider implements GuideBindingProvider, IResourceManagerReloadListener {

    private final ThaumcraftResearchCatalog catalog = new ThaumcraftResearchCatalog();
    private final ThaumcraftResearchVisibility visibility = new ThaumcraftResearchVisibility(catalog);

    @Override
    public String type() {
        return "thaumcraft";
    }

    @Override
    public BindingDisplay describe(BindingKey key, BindingViewContext context) {
        catalog.entries();
        return describe(key, context, visibility.progress(context));
    }

    private BindingDisplay describe(BindingKey key, BindingViewContext context, Set<String> progress) {
        ResearchItem item = catalog.get(key.id());
        if (item == null) return BindingRuntime.placeholder(key, BindingState.MISSING);
        BindingState state = visibility.state(item, context, progress);
        if (state.equals(BindingState.HIDDEN)) return BindingRuntime.placeholder(key, state);
        String description = context.includeDescription() ? item.getText() : null;
        var tooltip = new ArrayList<String>();
        tooltip.add(ResearchCategories.getCategoryName(item.category));
        if (description != null && !description.isEmpty()) tooltip.add(description);
        var parents = new LinkedHashSet<BindingKey>();
        appendParents(parents, item.parents, context, progress);
        appendParents(parents, item.parentsHidden, context, progress);
        return new BindingDisplay(
            key,
            state,
            item.getName(),
            description,
            icon(item),
            tooltip,
            List.copyOf(parents),
            !item.isVirtual());
    }

    private void appendParents(Set<BindingKey> result, String[] keys, BindingViewContext context,
        Set<String> progress) {
        if (keys == null) return;
        for (String key : keys) {
            ResearchItem parent = catalog.get(key);
            if (parent != null && !visibility.state(parent, context, progress)
                .equals(BindingState.HIDDEN)) result.add(new BindingKey(type(), key));
        }
    }

    private BindingIcon icon(ResearchItem item) {
        ItemStack stack = item.icon_item;
        if (stack != null && stack.getItem() != null) {
            Object id = Item.itemRegistry.getNameForObject(stack.getItem());
            if (id != null) return new BindingIcon(
                id.toString(),
                stack.getItemDamage(),
                stack.hasTagCompound() ? stack.getTagCompound()
                    .toString() : null,
                null);
        }
        return item.icon_resource == null ? null : new BindingIcon(null, 0, null, item.icon_resource);
    }

    @Override
    public BindingSearchResult search(String query, int offset, int limit, BindingViewContext context) {
        String filter = query == null ? "" : query.toLowerCase(Locale.ROOT);
        var progress = visibility.progress(context);
        var matches = new ArrayList<ResearchItem>();
        for (var item : catalog.entries()
            .values()) {
            if (visibility.state(item, context, progress)
                .equals(BindingState.HIDDEN)) continue;
            if (filter.isEmpty() || item.key.toLowerCase(Locale.ROOT)
                .contains(filter)
                || item.getName()
                    .toLowerCase(Locale.ROOT)
                    .contains(filter)
                || item.category.toLowerCase(Locale.ROOT)
                    .contains(filter)
                || ResearchCategories.getCategoryName(item.category)
                    .toLowerCase(Locale.ROOT)
                    .contains(filter))
                matches.add(item);
        }
        int start = Math.clamp(offset, 0, matches.size());
        int end = (int) Math.min(matches.size(), (long) start + Math.max(0, limit));
        var entries = new ArrayList<BindingDisplay>(end - start);
        for (int index = start; index < end; index++) {
            var item = matches.get(index);
            entries.add(describe(new BindingKey(type(), item.key), context, progress));
        }
        return new BindingSearchResult(entries, matches.size(), catalog.revision());
    }

    @Override
    public BindingOpenResult open(BindingKey key, GuideUiHost origin) {
        catalog.entries();
        ResearchItem item = catalog.get(key.id());
        var context = BindingRuntime.view(false, false);
        var progress = visibility.progress(context);
        if (item == null || visibility.state(item, context, progress)
            .equals(BindingState.HIDDEN)) return BindingOpenResult.failure("guidenh.binding.missing");
        return ThaumcraftResearchNavigation.open(item, origin);
    }

    @Override
    public long revision() {
        return catalog.revision();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        invalidate();
    }

    public void invalidate() {
        catalog.invalidate();
    }
}
