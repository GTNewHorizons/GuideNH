package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeAttribute;
import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeHtml;

import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.tooltip.AppendedItemTooltip;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene.BlockStatsLayoutState;
import com.hfstudio.guidenh.guide.scene.SceneBlockStatsEntry;

public class GuideSiteBlockStatsRenderer {

    private GuideSiteBlockStatsRenderer() {}

    public static String renderBlockStatsHtml(LytGuidebookScene scene, GuideSiteTemplateRegistry templates,
        ResourceLocation currentPageId, GuideSiteItemIconResolver itemIconResolver) {
        if (scene == null || !scene.shouldExportBlockStats()) {
            return null;
        }
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-scene-block-stats\" data-scene-block-stats>");
        for (SceneBlockStatsEntry entry : scene.getBlockStatsEntriesForExport()) {
            ItemStack displayStack = entry != null ? entry.copyDisplayStack() : null;
            if (displayStack == null) {
                continue;
            }
            GuideSiteExportedItem item = GuideSiteItemSupport.export(displayStack, itemIconResolver);
            String tooltipHtml = GuideSiteSceneAnnotationSerializer.renderTooltipHtml(
                new AppendedItemTooltip(
                    displayStack,
                    List.of(GuidebookText.SceneBlockStatsTooltipCount.text(Integer.toString(entry.getCount())))),
                currentPageId,
                null,
                itemIconResolver,
                templates);
            String templateId = tooltipHtml != null && !tooltipHtml.isBlank() ? templates.create(tooltipHtml) : null;
            String itemHref = templates.resolveItemHref(displayStack);
            html.append("<div class=\"guide-scene-block-stat")
                .append(templateId != null ? " guide-tooltip" : "")
                .append("\" data-block-stat-key=\"")
                .append(escapeAttribute(entry.getKey()))
                .append("\"");
            if (templateId != null) {
                html.append(" data-template=\"")
                    .append(escapeAttribute(templateId))
                    .append("\" tabindex=\"0\"");
            }
            if (!itemHref.isEmpty()) {
                html.append(" data-guide-item-href=\"")
                    .append(escapeAttribute(itemHref))
                    .append("\"");
            }
            html.append(">");
            GuideSiteItemHtml.appendIcon(html, item, "guide-scene-block-stat-icon", 1.0f, false);
            html.append("<span class=\"guide-scene-block-stat-label\">")
                .append(escapeHtml(entry.getLabel()))
                .append("</span><span class=\"guide-scene-block-stat-count\">x")
                .append(LytGuidebookScene.formatBlockStatsDisplayCount(entry.getCount()))
                .append("</span></div>");
        }
        html.append("</div>");
        return html.toString();
    }

    public static String blockStatsLayoutClass(LytGuidebookScene scene) {
        if (scene == null || !scene.shouldExportBlockStats()) {
            return null;
        }
        BlockStatsLayoutState state = scene.getBlockStatsLayoutStateForExport();
        StringBuilder cssClass = new StringBuilder("guide-scene-export-frame--block-stats");
        cssClass.append(" guide-scene-export-frame--block-stats-")
            .append(
                state.dock()
                    .name()
                    .toLowerCase(Locale.ROOT));
        cssClass.append(" guide-scene-export-frame--block-stats-corner-")
            .append(
                state.corner()
                    .name()
                    .toLowerCase(Locale.ROOT)
                    .replace('_', '-'));
        cssClass.append(" guide-scene-export-frame--block-stats-mode-")
            .append(
                state.mode()
                    .name()
                    .toLowerCase(Locale.ROOT));
        if (!state.visible()) {
            cssClass.append(" guide-scene-export-frame--block-stats-hidden");
        }
        cssClass.append(
            state.showNames() ? " guide-scene-export-frame--block-stats-names"
                : " guide-scene-export-frame--block-stats-icons");
        return cssClass.toString();
    }

    public static String blockStatsLayoutStyle(LytGuidebookScene scene) {
        if (scene == null || !scene.shouldExportBlockStats()) {
            return null;
        }
        BlockStatsLayoutState state = scene.getBlockStatsLayoutStateForExport();
        StringBuilder style = new StringBuilder();
        appendCssPx(style, "--guide-scene-block-stats-max-width", state.maxWidth());
        appendCssPx(style, "--guide-scene-block-stats-max-height", state.maxHeight());
        appendCssPx(style, "--guide-scene-logical-width", state.sceneWidth());
        appendCssPx(style, "--guide-scene-logical-height", state.sceneHeight());
        appendCssPx(style, "--guide-scene-button-reserve", state.buttonColumnReserve());
        return style.toString();
    }

    private static void appendCssPx(StringBuilder style, String property, int value) {
        if (!style.isEmpty()) {
            style.append(';');
        }
        style.append(property)
            .append(':')
            .append(Math.max(0, value))
            .append("px");
    }

    public static boolean blockStatsButtonEnabled(LytGuidebookScene scene) {
        if (scene == null || !scene.shouldExportBlockStats()) {
            return false;
        }
        return scene.getSceneButtonColumnReserveForExport() > 0;
    }
}
