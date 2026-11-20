package com.hfstudio.guidenh.integration.thaumcraft;

import java.util.HashSet;
import java.util.Set;

import com.hfstudio.guidenh.integration.api.binding.BindingState;
import com.hfstudio.guidenh.integration.api.binding.BindingViewContext;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchBrowser;

public class ThaumcraftResearchVisibility {

    private final ThaumcraftResearchCatalog catalog;

    public ThaumcraftResearchVisibility(ThaumcraftResearchCatalog catalog) {
        this.catalog = catalog;
    }

    public Set<String> progress(BindingViewContext context) {
        if (context.staticView() || context.playerName() == null) return Set.of();
        var values = GuiResearchBrowser.completedResearch.get(context.playerName());
        return values == null ? Set.of() : new HashSet<>(values);
    }

    public BindingState state(ResearchItem item, BindingViewContext context, Set<String> progress) {
        if (context.authoring()) return BindingState.UNKNOWN;
        if (context.staticView()) {
            return item.isVirtual() || item.isHidden() || item.isLost() || item.isConcealed() ? BindingState.HIDDEN
                : BindingState.UNKNOWN;
        }
        if (item.isVirtual() || "ELDRITCH".equals(item.category) && !progress.contains("ELDRITCHMINOR"))
            return BindingState.HIDDEN;
        if (progress.contains(item.key)) return BindingState.COMPLETED;
        boolean ready = prerequisitesComplete(item, progress);
        boolean discovered = progress.contains("@" + item.key);
        if (!discovered && (item.isLost() || item.isHidden() || item.isConcealed() && !ready))
            return BindingState.HIDDEN;
        return ready ? BindingState.VISIBLE : BindingState.LOCKED;
    }

    public boolean prerequisitesComplete(ResearchItem item, Set<String> progress) {
        return complete(item.parents, progress) && complete(item.parentsHidden, progress);
    }

    private boolean complete(String[] keys, Set<String> progress) {
        if (keys == null) return true;
        for (String key : keys) if (catalog.get(key) != null && !progress.contains(key)) return false;
        return true;
    }
}
