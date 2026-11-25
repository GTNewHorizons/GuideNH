package com.hfstudio.guidenh.client.capture.geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;

public class CaptureTargetOcclusion {

    private static final Comparator<CaptureRect> FRAGMENT_ORDER = Comparator
        .comparingDouble((CaptureRect bounds) -> bounds.width() * bounds.height())
        .reversed()
        .thenComparingDouble(CaptureRect::y)
        .thenComparingDouble(CaptureRect::x);

    private CaptureTargetOcclusion() {}

    public static List<CaptureTarget> clipComponents(List<CaptureTarget> targets, List<CaptureTarget> windows) {
        if (windows.isEmpty()) return List.copyOf(targets);
        List<CaptureTarget> visible = new ArrayList<>(targets.size());
        for (CaptureTarget target : targets) {
            if (target.kind()
                .equals(CaptureTarget.WINDOW)
                || target.kind()
                    .equals(CaptureTarget.SCREEN)) {
                visible.add(target);
                continue;
            }
            List<CaptureRect> fragments = List.of(target.visibleBounds());
            for (CaptureTarget window : windows) {
                if (window.zOrder() <= target.zOrder()) continue;
                List<CaptureRect> remaining = new ArrayList<>(fragments.size());
                for (CaptureRect fragment : fragments) remaining.addAll(fragment.subtract(window.visibleBounds()));
                fragments = remaining;
                if (fragments.isEmpty()) break;
            }
            if (fragments.size() == 1 && fragments.getFirst()
                .equals(target.visibleBounds())) {
                visible.add(target);
                continue;
            }
            fragments.sort(FRAGMENT_ORDER);
            for (int index = 0; index < fragments.size(); index++) {
                visible.add(
                    new CaptureTarget(
                        index == 0 ? target.id() : target.id() + "/visible:" + index,
                        target.parentId(),
                        target.kind(),
                        target.className(),
                        target.label(),
                        target.bounds(),
                        fragments.get(index),
                        target.zOrder(),
                        target.priority(),
                        target.ownerIdentity(),
                        target.contentLayer(),
                        target.playerInventory(),
                        target.providerId()));
            }
        }
        return List.copyOf(visible);
    }
}
