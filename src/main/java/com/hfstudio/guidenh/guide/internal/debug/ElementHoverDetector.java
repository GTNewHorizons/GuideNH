package com.hfstudio.guidenh.guide.internal.debug;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;

/**
 * Detects hovered elements in GuideNH screens and extracts debug information.
 * Uses interface-based system for extensible info extraction.
 * <p>
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

        HoveredCandidate bestCandidate = new HoveredCandidate();
        collectHoveredNodes(document, mouseX, mouseY, null, 0, bestCandidate);
        return bestCandidate.info;
    }

    private void collectHoveredNodes(LytNode node, int mouseX, int mouseY, @Nullable HoveredElementInfo parentInfo,
        int depth, HoveredCandidate bestCandidate) {

        LytRect bounds = node.getBounds();

        if (bounds != null && bounds.contains(mouseX, mouseY)) {
            HoveredElementInfo info = createElementInfo(node, bounds, parentInfo);

            // The current node inherits scroll offset from its parent, but NOT from itself.
            // Only the node's children will be affected by the node's own scroll offset.
            float cumulativeScrollX = parentInfo != null ? parentInfo.getCumulativeScrollOffsetX() : 0f;
            float cumulativeScrollY = parentInfo != null ? parentInfo.getCumulativeScrollOffsetY() : 0f;

            info.setCumulativeScrollOffset(cumulativeScrollX, cumulativeScrollY);

            bestCandidate.consider(info, depth, bounds.width() * bounds.height());

            if (node instanceof DebugComponent debugComponent) {
                collectDebugComponents(debugComponent, mouseX, mouseY, info, depth, bestCandidate);
            }

            if (node instanceof DebugFlowContainer flowContainer) {
                collectFlowContent(flowContainer, mouseX, mouseY, info, depth, bestCandidate);
            }

            // Calculate cumulative scroll offset for children: parent's cumulative + this node's offset
            float childCumulativeScrollX = cumulativeScrollX;
            float childCumulativeScrollY = cumulativeScrollY;
            int adjustedMouseX = mouseX;
            int adjustedMouseY = mouseY;

            if (node instanceof InterpolatedViewport viewport) {
                float nodeScrollX = viewport.getVisualScrollOffsetX();
                float nodeScrollY = viewport.getVisualScrollOffsetY();
                childCumulativeScrollX += nodeScrollX;
                childCumulativeScrollY += nodeScrollY;
                adjustedMouseX += Math.round(nodeScrollX);
                adjustedMouseY += Math.round(nodeScrollY);
            }

            // Pass child cumulative offset by creating a temporary info wrapper for children
            HoveredElementInfo childParentInfo = info;
            if (node instanceof InterpolatedViewport) {
                // Create a wrapper info with the child cumulative offset
                childParentInfo = new HoveredElementInfo(
                    info.getClassName(),
                    info.getX(),
                    info.getY(),
                    info.getWidth(),
                    info.getHeight(),
                    info.getParent());
                childParentInfo.setCumulativeScrollOffset(childCumulativeScrollX, childCumulativeScrollY);
            }

            for (LytNode child : node.getChildren()) {
                collectHoveredNodes(child, adjustedMouseX, adjustedMouseY, childParentInfo, depth + 1, bestCandidate);
            }
        } else if (bounds == null) {
            for (LytNode child : node.getChildren()) {
                collectHoveredNodes(child, mouseX, mouseY, parentInfo, depth + 1, bestCandidate);
            }
        }
    }

    private void collectDebugComponents(DebugComponent debugComponent, int mouseX, int mouseY,
        HoveredElementInfo parentInfo, int parentDepth, HoveredCandidate bestCandidate) {

        for (DebugComponent.ComponentEntry component : debugComponent.getDebugComponents()) {
            if (component.containsPoint(mouseX, mouseY)) {
                String className = parentInfo.getClassName() + "$" + component.getName();
                HoveredElementInfo info = new HoveredElementInfo(
                    className,
                    component.getBounds()
                        .x(),
                    component.getBounds()
                        .y(),
                    component.getBounds()
                        .width(),
                    component.getBounds()
                        .height(),
                    parentInfo);

                // Inherit cumulative scroll offset from parent (debug components are children of the parent node)
                info.setCumulativeScrollOffset(
                    parentInfo.getCumulativeScrollOffsetX(),
                    parentInfo.getCumulativeScrollOffsetY());

                info.addExtraInfo("Component: " + component.getName());
                if (component.getExtraInfo() != null) {
                    info.addExtraInfo(component.getExtraInfo());
                }

                int effectiveDepth = parentDepth + 1 + component.getPriority();
                float area = component.getBounds()
                    .width()
                    * component.getBounds()
                        .height();
                bestCandidate.consider(info, effectiveDepth, area);
            }
        }
    }

    private void collectFlowContent(DebugFlowContainer flowContainer, int mouseX, int mouseY,
        HoveredElementInfo parentInfo, int parentDepth, HoveredCandidate bestCandidate) {

        DebugFlowContainer.FlowContentEntry entry = flowContainer.pickFlowContent(mouseX, mouseY);
        if (entry != null && entry.bounds()
            .contains(mouseX, mouseY)) {
            String className = entry.content()
                .getClass()
                .getName();
            HoveredElementInfo info = new HoveredElementInfo(
                className,
                entry.bounds()
                    .x(),
                entry.bounds()
                    .y(),
                entry.bounds()
                    .width(),
                entry.bounds()
                    .height(),
                parentInfo);

            // Inherit cumulative scroll offset from parent (flow content is child of the parent node)
            info.setCumulativeScrollOffset(
                parentInfo.getCumulativeScrollOffsetX(),
                parentInfo.getCumulativeScrollOffsetY());

            FlowContentInfoExtractor.extract(entry.content(), info);

            float area = entry.bounds()
                .width()
                * entry.bounds()
                    .height();
            bestCandidate.consider(info, parentDepth + 1, area);
        }
    }

    private HoveredElementInfo createElementInfo(LytNode node, LytRect bounds,
        @Nullable HoveredElementInfo parentInfo) {
        String className = node.getClass()
            .getName();
        HoveredElementInfo info = new HoveredElementInfo(
            className,
            bounds.x(),
            bounds.y(),
            bounds.width(),
            bounds.height(),
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

        @Nullable
        private HoveredElementInfo info;
        private int depth = Integer.MIN_VALUE;
        private float area = Float.POSITIVE_INFINITY;

        private void consider(HoveredElementInfo info, int depth, float area) {
            if (depth > this.depth || depth == this.depth && area < this.area) {
                this.info = info;
                this.depth = depth;
                this.area = area;
            }
        }
    }
}
