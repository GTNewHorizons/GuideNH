package com.hfstudio.guidenh.guide.internal.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.internal.GuideME;

public record GuideTargetSelection(String title, List<GuideAnchor> fluidTargets, List<GuideAnchor> itemTargets) {

    public static final ResourceLocation PAGE_ID = GuideME.makeId("resource_links");

    public GuideTargetSelection {
        fluidTargets = List.copyOf(new LinkedHashSet<>(fluidTargets));
        var items = new LinkedHashSet<>(itemTargets);
        items.removeAll(fluidTargets);
        itemTargets = List.copyOf(items);
    }

    public int size() {
        return fluidTargets.size() + itemTargets.size();
    }

    public List<GuideAnchor> targets() {
        var targets = new ArrayList<GuideAnchor>(size());
        targets.addAll(fluidTargets);
        targets.addAll(itemTargets);
        return List.copyOf(targets);
    }
}
