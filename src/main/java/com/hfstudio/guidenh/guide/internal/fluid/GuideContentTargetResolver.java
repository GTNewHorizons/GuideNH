package com.hfstudio.guidenh.guide.internal.fluid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.indices.ItemMultiIndex;
import com.hfstudio.guidenh.guide.indices.OreIndex;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

public class GuideContentTargetResolver {

    protected GuideContentTargetResolver() {}

    public static List<GuideAnchor> findItemTargets(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) return List.of();
        var guides = new ArrayList<MutableGuide>(GuideRegistry.getAll());
        guides.sort(
            Comparator.comparing(
                guide -> guide.getId()
                    .toString()));
        var targets = new LinkedHashSet<GuideAnchor>();
        for (MutableGuide guide : guides) {
            if (!guide.isAvailableToOpenHotkey()) continue;
            try {
                var anchors = orderAnchors(
                    guide,
                    guide.getIndex(ItemMultiIndex.class)
                        .findAllByStack(stack));
                for (var anchor : anchors) {
                    if (guide.pageExists(anchor.pageId())) targets.add(new GuideAnchor(guide.getId(), anchor));
                }
                if (anchors.isEmpty()) {
                    var anchor = guide.getIndex(OreIndex.class)
                        .findByStack(stack);
                    if (anchor != null && guide.pageExists(anchor.pageId()))
                        targets.add(new GuideAnchor(guide.getId(), anchor));
                }
            } catch (IllegalArgumentException exception) {
                continue;
            }
        }
        return List.copyOf(targets);
    }

    public static List<PageAnchor> orderAnchors(MutableGuide guide, List<PageAnchor> anchors) {
        if (anchors.size() < 2) return anchors;
        var navigation = guide.getNavigationTree();
        return anchors.stream()
            .sorted(
                Comparator
                    .comparing(
                        (PageAnchor anchor) -> navigation.getNodeById(anchor.pageId()),
                        Comparator.nullsLast(NavigationTree.NODE_COMPARATOR))
                    .thenComparing(PageAnchor::toString))
            .toList();
    }
}
