package com.hfstudio.guidenh.guide.internal.fluid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.indices.FluidIndex;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.search.GuideFluidLinksPage;

public class FluidGuideTargetResolver {

    protected FluidGuideTargetResolver() {}

    public static List<GuideAnchor> findAll(String id, Iterable<MutableGuide> guides) {
        var orderedGuides = new ArrayList<MutableGuide>();
        guides.forEach(orderedGuides::add);
        orderedGuides.sort(
            Comparator.comparing(
                guide -> guide.getId()
                    .toString()));
        var targets = new LinkedHashSet<GuideAnchor>();
        for (MutableGuide guide : orderedGuides) {
            if (!guide.isAvailableToOpenHotkey()) continue;
            FluidIndex index;
            try {
                index = guide.getIndex(FluidIndex.class);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            for (var page : GuideContentTargetResolver.orderAnchors(guide, index.get(id))) {
                if (guide.pageExists(page.pageId())) targets.add(new GuideAnchor(guide.getId(), page));
            }
        }
        return List.copyOf(targets);
    }

    public static @Nullable GuideAnchor resolve(String id, Iterable<MutableGuide> guides) {
        List<GuideAnchor> targets = findAll(id, guides);
        if (targets.isEmpty()) return null;
        if (targets.size() == 1) return targets.getFirst();
        return new GuideAnchor(
            targets.getFirst()
                .guideId(),
            GuideFluidLinksPage.anchorForFluid(id));
    }
}
