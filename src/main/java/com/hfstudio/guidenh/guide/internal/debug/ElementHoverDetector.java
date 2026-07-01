package com.hfstudio.guidenh.guide.internal.debug;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;

/**
 * Detects hovered elements in GuideNH screens and extracts debug information.
 * Uses interface-based system for extensible info extraction.
 *
 * Supports:
 * - Deep-first traversal for finest-grained element detection
 * - DebugComponent interface for sub-component detection (sliders, buttons, tabs)
 * - DebugFlowContainer interface for flow content detection (inline elements)
 * - Registry-based info extraction via DebugInfoExtractor
 */
public class ElementHoverDetector {

    @Nullable
    public HoveredElementInfo detectHoveredElement(LytDocument document, int mouseX, int mouseY) {
        if (!ModConfig.debug.guiDebugMode || document == null) {
            return null;
        }

        DebugInfoExtractorInit.init();

        List<HoveredCandidate> candidates = new ArrayList<>();
        collectHoveredNodes(document, mouseX, mouseY, null, candidates);

        return selectBestCandidate(candidates);
    }

    private void collectHoveredNodes(LytNode node, int mouseX, int mouseY, @Nullable HoveredElementInfo parentInfo,
        List<HoveredCandidate> candidates) {

        LytRect bounds = node.getBounds();

        if (bounds != null && bounds.contains(mouseX, mouseY)) {
            HoveredElementInfo info = createElementInfo(node, bounds, parentInfo);
            int depth = calculateDepth(node);
            candidates.add(new HoveredCandidate(info, depth, bounds.width() * bounds.height()));

            if (node instanceof DebugComponent debugComponent) {
                collectDebugComponents(debugComponent, mouseX, mouseY, info, depth, candidates);
            }

            if (node instanceof DebugFlowContainer flowContainer) {
                collectFlowContent(flowContainer, mouseX, mouseY, info, depth, candidates);
            }

            for (LytNode child : node.getChildren()) {
                collectHoveredNodes(child, mouseX, mouseY, info, candidates);
            }
        } else if (bounds == null) {
            for (LytNode child : node.getChildren()) {
                collectHoveredNodes(child, mouseX, mouseY, parentInfo, candidates);
            }
        }
    }

    private void collectDebugComponents(DebugComponent debugComponent, int mouseX, int mouseY,
        HoveredElementInfo parentInfo, int parentDepth, List<HoveredCandidate> candidates) {

        for (DebugComponent.ComponentEntry component : debugComponent.getDebugComponents()) {
            if (component.containsPoint(mouseX, mouseY)) {
                String className = parentInfo.getClassName() + "$" + component.getName();
                HoveredElementInfo info = new HoveredElementInfo(
                    className,
                    (int) component.getBounds()
                        .x(),
                    (int) component.getBounds()
                        .y(),
                    (int) component.getBounds()
                        .width(),
                    (int) component.getBounds()
                        .height(),
                    parentInfo);

                info.addExtraInfo("Component: " + component.getName());
                if (component.getExtraInfo() != null) {
                    info.addExtraInfo(component.getExtraInfo());
                }

                int effectiveDepth = parentDepth + 1 + component.getPriority();
                float area = component.getBounds()
                    .width()
                    * component.getBounds()
                        .height();
                candidates.add(new HoveredCandidate(info, effectiveDepth, area));
            }
        }
    }

    private void collectFlowContent(DebugFlowContainer flowContainer, int mouseX, int mouseY,
        HoveredElementInfo parentInfo, int parentDepth, List<HoveredCandidate> candidates) {

        DebugFlowContainer.FlowContentEntry entry = flowContainer.pickFlowContent(mouseX, mouseY);
        if (entry != null && entry.bounds()
            .contains(mouseX, mouseY)) {
            String className = entry.content()
                .getClass()
                .getSimpleName();
            HoveredElementInfo info = new HoveredElementInfo(
                className,
                (int) entry.bounds()
                    .x(),
                (int) entry.bounds()
                    .y(),
                (int) entry.bounds()
                    .width(),
                (int) entry.bounds()
                    .height(),
                parentInfo);

            FlowContentInfoExtractor.extract(entry.content(), info);

            float area = entry.bounds()
                .width()
                * entry.bounds()
                    .height();
            candidates.add(new HoveredCandidate(info, parentDepth + 1, area));
        }
    }

    @Nullable
    private HoveredElementInfo selectBestCandidate(List<HoveredCandidate> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        HoveredCandidate best = candidates.get(0);
        for (HoveredCandidate candidate : candidates) {
            if (candidate.depth > best.depth || (candidate.depth == best.depth && candidate.area < best.area)) {
                best = candidate;
            }
        }

        return best.info;
    }

    private int calculateDepth(LytNode node) {
        int depth = 0;
        LytNode current = node.getParent();
        while (current != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    private HoveredElementInfo createElementInfo(LytNode node, LytRect bounds,
        @Nullable HoveredElementInfo parentInfo) {
        String className = node.getClass()
            .getName();
        HoveredElementInfo info = new HoveredElementInfo(
            className,
            (int) bounds.x(),
            (int) bounds.y(),
            (int) bounds.width(),
            (int) bounds.height(),
            parentInfo);

        addBasicInfo(node, info);
        DebugInfoExtractorRegistry.extract(node, info);

        return info;
    }

    private void addBasicInfo(LytNode node, HoveredElementInfo info) {
        if (node.getId() != null) {
            info.addExtraInfo("ID: " + node.getId());
        }

        if (node.getNodeUid() != null) {
            info.addExtraInfo("UID: " + node.getNodeUid());
        }

        if (node.getStyleClass() != null) {
            info.addExtraInfo("Style Class: " + node.getStyleClass());
        }
    }

    private static class HoveredCandidate {

        final HoveredElementInfo info;
        final int depth;
        final float area;

        HoveredCandidate(HoveredElementInfo info, int depth, float area) {
            this.info = info;
            this.depth = depth;
            this.area = area;
        }
    }
}
