package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeAttribute;
import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeHtml;
import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.toCssColor;
import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteLegacyTextRenderer.renderLegacyFormattedText;
import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteLegacyTextRenderer.renderPlainTextTooltip;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.GuideItemReferenceResolver;
import com.hfstudio.guidenh.guide.compiler.tags.BlockImageCompiler.BlockImagePlaceholder;
import com.hfstudio.guidenh.guide.compiler.tags.ItemImageCompiler.ItemImagePlaceholder;
import com.hfstudio.guidenh.guide.compiler.tags.MermaidCompiler.MermaidPlaceholder;
import com.hfstudio.guidenh.guide.document.block.ImageRegionAnnotation;
import com.hfstudio.guidenh.guide.document.block.LytAlertBox;
import com.hfstudio.guidenh.guide.document.block.LytAlignedBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytCodeBlock;
import com.hfstudio.guidenh.guide.document.block.LytContentTabsBlock;
import com.hfstudio.guidenh.guide.document.block.LytDetailsBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
import com.hfstudio.guidenh.guide.document.block.LytFileTree;
import com.hfstudio.guidenh.guide.document.block.LytGuiSprite;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytImageBlock;
import com.hfstudio.guidenh.guide.document.block.LytItemGrid;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytLatexBlock;
import com.hfstudio.guidenh.guide.document.block.LytLatexDisplayBlock;
import com.hfstudio.guidenh.guide.document.block.LytList;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytMermaidFlowchart;
import com.hfstudio.guidenh.guide.document.block.LytMermaidMindmap;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytQuoteBox;
import com.hfstudio.guidenh.guide.document.block.LytSizeBox;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.document.block.LytSlotGrid;
import com.hfstudio.guidenh.guide.document.block.LytStructureView;
import com.hfstudio.guidenh.guide.document.block.LytThematicBreak;
import com.hfstudio.guidenh.guide.document.block.LytWidthBox;
import com.hfstudio.guidenh.guide.document.block.chart.ChartAxisOptions;
import com.hfstudio.guidenh.guide.document.block.chart.ChartSeries;
import com.hfstudio.guidenh.guide.document.block.chart.LytBarChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytChartBase;
import com.hfstudio.guidenh.guide.document.block.chart.LytColumnChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytLineChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytPieChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytScatterChart;
import com.hfstudio.guidenh.guide.document.block.chart.PieInsetSpec;
import com.hfstudio.guidenh.guide.document.block.chart.PieSlice;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.guide.document.block.recipes.LytGenericRecipeBox;
import com.hfstudio.guidenh.guide.document.block.recipes.LytStandardRecipeBox;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.block.table.LytTableCell;
import com.hfstudio.guidenh.guide.document.block.table.LytTableRow;
import com.hfstudio.guidenh.guide.document.flow.LytFlowAnchor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowBreak;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.flow.LytSpoilerSpan;
import com.hfstudio.guidenh.guide.document.flow.LytTooltipSpan;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltipAppender;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidDiagramType;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartParser;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapParser;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipLines;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;

/**
 * Scene lookups remain lazy so nested tooltips use the active export scope.
 */
public class GuideSiteTooltipRenderer {

    private final Function<LytGuidebookScene, GuideSiteExportedScene> exportedSceneLookup;

    public GuideSiteTooltipRenderer(Function<LytGuidebookScene, GuideSiteExportedScene> exportedSceneLookup) {
        this.exportedSceneLookup = Objects.requireNonNull(exportedSceneLookup);
    }

    @Nullable
    public String createTemplateId(@Nullable GuideTooltip tooltip, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        String html = render(tooltip, currentPageId, assetExporter, itemIconResolver, templates, true);
        if (html == null || html.trim()
            .isEmpty()) {
            return null;
        }
        return templates.create(html);
    }

    public String render(@Nullable GuideTooltip tooltip, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        return switch (tooltip) {
            case TextTooltip textTooltip -> renderPlainTextTooltip(textTooltip.getText());
            case ItemTooltip itemTooltip -> renderItemTooltip(
                itemTooltip,
                itemIconResolver,
                templates);
            case ContentTooltip contentTooltip -> renderBlock(
                    contentTooltip.getContent(),
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            case null, default -> "";
        };
    }

    private String renderItemTooltip(ItemTooltip tooltip, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates) {
        String semanticKey = semanticCacheKey(tooltip);
        String html;
        if (templates != null && semanticKey != null) {
            html = templates
                .getOrComputeRendered(semanticKey, () -> renderItemTooltipUncached(tooltip, itemIconResolver));
        } else {
            html = renderItemTooltipUncached(tooltip, itemIconResolver);
        }
        String href = templates != null ? templates.resolveItemHref(tooltip.getStack()) : "";
        return href.isEmpty() ? html : "<div data-guide-item-href=\"" + escapeAttribute(href) + "\">" + html + "</div>";
    }

    private String renderItemTooltipUncached(ItemTooltip tooltip, GuideSiteItemIconResolver itemIconResolver) {
        ItemStack stack = tooltip.getStack();
        GuideSiteExportedItem item = GuideSiteItemSupport.export(stack, itemIconResolver);

        List<String> lines = new ArrayList<>();
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null) {
                lines
                    .addAll(OpenGuideHotkey.withoutTooltipHints(() -> GuideItemTooltipLines.build(tooltip, minecraft)));
            }
        } catch (Throwable ignored) {}

        if (lines.isEmpty() && stack != null) {
            try {
                lines.add(stack.getDisplayName());
            } catch (Throwable ignored) {}
        }

        StringBuilder html = new StringBuilder();
        if (!item.isEmpty()) {
            html.append("<div class=\"guide-tooltip-item-row\">");
            GuideSiteItemHtml.appendIcon(html, item, null);
            html.append("</div>");
        }
        for (String line : lines) {
            html.append("<p>")
                .append(renderLegacyFormattedText(line))
                .append("</p>");
        }
        return html.toString();
    }

    @Nullable
    private String semanticCacheKey(@Nullable GuideTooltip tooltip) {
        if (!(tooltip instanceof ItemTooltip itemTooltip) || tooltip instanceof ItemTooltipAppender) {
            return null;
        }
        return GuideSiteItemSupport.tooltipCacheKey(itemTooltip.getStack());
    }

    public String renderBlock(@Nullable LytBlock block, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        if (block == null) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        appendBlock(html, block, currentPageId, assetExporter, itemIconResolver, templates, allowNestedItemTooltips);
        return html.toString();
    }

    private void appendBlock(StringBuilder html, @Nullable LytNode node,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        switch (node) {
            case null -> {
                return;
            }
            case LytDocument document -> {
                for (LytBlock child : document.getBlocks()) {
                    appendBlock(
                            html,
                            child,
                            currentPageId,
                            assetExporter,
                            itemIconResolver,
                            templates,
                            allowNestedItemTooltips);
                }
                return;
            }
            case LytGuidebookScene scene -> {
                // Use a static preview in tooltips. Live nested scenes would exhaust the browser's shared WebGL context
                // limit.
                GuideSiteExportedScene exportedScene = exportedSceneLookup.apply(scene);
                int width = Math.max(16, scene.getSceneWidth());
                int height = Math.max(16, scene.getSceneHeight());
                html.append(GuideSiteSceneTagRenderer.renderStaticScenePlaceholder(width, height, exportedScene));
                return;
            }
            case LytHeading heading -> {
                appendParagraph(
                        html,
                        heading,
                        "h" + clampHeadingDepth(heading.getDepth()),
                        currentPageId,
                        assetExporter,
                        itemIconResolver,
                        templates,
                        allowNestedItemTooltips);
                return;
            }
            case LytImageBlock imageBlock -> {
                appendImageBlock(html, imageBlock, currentPageId, assetExporter, itemIconResolver, templates);
                return;
            }
            case BlockImagePlaceholder blockImage -> {
                appendBlockImage(html, blockImage, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                return;
            }
            case LytMermaidMindmap mindmap -> {
                appendMermaidMindmap(html, mindmap, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                return;
            }
            case LytMermaidFlowchart flowchart -> {
                appendMermaidFlowchart(html, flowchart, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                return;
            }
            case MermaidPlaceholder mermaid -> {
                appendMermaidPlaceholder(html, mermaid, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                return;
            }
            case LytCodeBlock codeBlock -> {
                appendCodeBlock(html, codeBlock);
                return;
            }
            case LytLatexDisplayBlock latex -> {
                appendLatex(html, latex.getFormula(), latex.getFillColorArgb(), latex.getSourceScale(),
                    latex.getUserScale(), latex.getLatexTooltip(), true, latex.getOffsetX(), latex.getOffsetY(),
                    currentPageId, assetExporter, itemIconResolver, templates);
                return;
            }
            case LytLatexBlock latex -> {
                appendLatex(html, latex.getFormula(), latex.getFillColorArgb(), latex.getSourceScale(),
                    latex.getUserScale(), latex.getLatexTooltip(), false, latex.getOffsetX(), latex.getOffsetY(),
                    currentPageId, assetExporter, itemIconResolver, templates);
                return;
            }
            case LytFileTree fileTree -> {
                appendFileTree(html, fileTree, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                return;
            }
            case LytStructureView structure -> {
                appendStructureView(html, structure, itemIconResolver, currentPageId, assetExporter, templates,
                    allowNestedItemTooltips);
                return;
            }
            case LytGuiSprite sprite -> {
                appendGuiSprite(html, sprite, assetExporter);
                return;
            }
            case LytStandardRecipeBox recipe -> {
                html.append("<div class=\"guide-recipe-box guide-recipe-box-standard\">");
                appendBlockChildren(html, recipe, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                html.append("</div>");
                return;
            }
            case LytGenericRecipeBox recipe -> {
                html.append("<div class=\"guide-recipe-box guide-recipe-box-generic\">");
                appendBlockChildren(html, recipe, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                html.append("</div>");
                return;
            }
            case LytColumnChart column -> {
                html.append(renderColumnChart(column));
                return;
            }
            case LytBarChart bar -> {
                html.append(renderBarChart(bar));
                return;
            }
            case LytLineChart line -> {
                html.append(renderLineChart(line));
                return;
            }
            case LytPieChart pie -> {
                html.append(renderPieChart(pie));
                return;
            }
            case LytScatterChart scatter -> {
                html.append(renderScatterChart(scatter));
                return;
            }
            case LytFunctionGraph functionGraph -> {
                html.append(GuideSiteGraphRenderer.renderFunctionGraph(
                    functionGraph,
                    assetExporter != null ? assetExporter.latexExporter() : null));
                return;
            }
            case LytDetailsBlock details -> {
                html.append("<details class=\"guide-details\"")
                    .append(details.isOpen() ? " open" : "")
                    .append("><summary>");
                appendBlockChildren(html, details.getSummaryBox(), currentPageId, assetExporter, itemIconResolver,
                    templates, allowNestedItemTooltips);
                html.append("</summary><div class=\"guide-details-body\">");
                if (details.isOpen()) {
                    appendBlockChildren(html, details.getContentBox(), currentPageId, assetExporter,
                        itemIconResolver, templates, allowNestedItemTooltips);
                }
                html.append("</div></details>");
                return;
            }
            case LytContentTabsBlock tabs -> {
                appendContentTabs(html, tabs, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                return;
            }
            case LytQuoteBox quote -> {
                html.append("<blockquote class=\"guide-quote-box\">");
                appendBlockChildren(html, quote, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                html.append("</blockquote>");
                return;
            }
            case LytAlertBox alert -> {
                html.append("<div class=\"guide-alert-box\">");
                appendBlockChildren(html, alert, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                html.append("</div>");
                return;
            }
            case LytAlignedBlock aligned -> {
                html.append("<div class=\"guide-aligned-block\">");
                appendBlockChildren(html, aligned, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                html.append("</div>");
                return;
            }
            case LytDocumentFloat documentFloat -> {
                appendBlockChildren(html, node, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                return;
            }
            case LytSizeBox sizeBox -> {
                appendBlockChildren(html, node, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                return;
            }
            case ItemImagePlaceholder itemImage -> {
                appendItemImage(html, itemImage, currentPageId, assetExporter, itemIconResolver, templates,
                    allowNestedItemTooltips);
                return;
            }
            case LytParagraph paragraph -> {
                appendParagraph(
                        html,
                        paragraph,
                        "p",
                        currentPageId,
                        assetExporter,
                        itemIconResolver,
                        templates,
                        allowNestedItemTooltips);
                return;
            }
            case LytList list -> {
                appendList(
                        html,
                        list,
                        currentPageId,
                        assetExporter,
                        itemIconResolver,
                        templates,
                        allowNestedItemTooltips);
                return;
            }
            case LytListItem listItem -> {
                appendListItem(
                        html,
                        listItem,
                        currentPageId,
                        assetExporter,
                        itemIconResolver,
                        templates,
                        allowNestedItemTooltips);
                return;
            }
            case LytHBox row -> {
                html.append("<div class=\"guide-layout guide-layout-row guide-tooltip-layout\" style=\"--guide-layout-gap:")
                    .append(Math.max(0, row.getGap()))
                    .append("px;\">");
                for (LytNode child : row.getChildren()) {
                    appendBlock(html, child, currentPageId, assetExporter, itemIconResolver, templates,
                        allowNestedItemTooltips);
                }
                html.append("</div>");
                return;
            }
            case LytThematicBreak lytThematicBreak -> {
                html.append("<hr>");
                return;
            }
            case LytTable table -> {
                appendTable(
                        html,
                        table,
                        currentPageId,
                        assetExporter,
                        itemIconResolver,
                        templates,
                        allowNestedItemTooltips);
                return;
            }
            case LytSlotGrid slotGrid -> {
                appendSlotGrid(
                        html,
                        slotGrid,
                        itemIconResolver,
                        currentPageId,
                        assetExporter,
                        templates,
                        allowNestedItemTooltips);
                return;
            }
            case LytItemGrid itemGrid -> {
                appendItemGrid(
                        html,
                        itemGrid,
                        itemIconResolver,
                        currentPageId,
                        assetExporter,
                        templates,
                        allowNestedItemTooltips);
                return;
            }
            case LytItemImage itemImage -> {
                appendItemStacks(
                        html,
                        itemImage.getStacks(),
                        itemIconResolver,
                        currentPageId,
                        assetExporter,
                        templates,
                        allowNestedItemTooltips);
                return;
            }
            case LytImage image -> {
                appendImage(html, image, assetExporter);
                return;
            }
            case LytSlot slot -> {
                appendSlot(
                        html,
                        slot,
                        itemIconResolver,
                        currentPageId,
                        assetExporter,
                        templates,
                        allowNestedItemTooltips);
                return;
            }
            default -> {
            }
        }

        List<? extends LytNode> children = node.getChildren();
        if (children != null) {
            for (LytNode child : children) {
                appendBlock(
                    html,
                    child,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            }
        }
        if ((children == null || children.isEmpty()) && node.getTextContent().isEmpty()) {
            html.append("<span class=\"guide-export-placeholder\">")
                .append(escapeHtml(node.getClass().getSimpleName()))
                .append("</span>");
        }
    }

    private void appendParagraph(StringBuilder html, LytParagraph paragraph, String tagName,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        boolean open = false;
        boolean emitted = false;
        for (LytFlowContent content : paragraph.getContent()) {
            boolean blockLayout = "p".equals(tagName) && content instanceof LytFlowInlineBlock inline
                && (inline.getBlock() instanceof LytHBox || inline.getBlock() instanceof LytWidthBox);
            if (blockLayout) {
                if (open) {
                    html.append("</p>");
                    open = false;
                }
            } else if (!open) {
                if (emitted && content instanceof LytFlowText text
                    && text.getText()
                        .isBlank()) {
                    continue;
                }
                html.append("<")
                    .append(tagName);
                appendParagraphStyleAttribute(html, paragraph.resolveStyle());
                if (paragraph instanceof LytHeading heading) {
                    String anchor = GuideSiteHrefResolver.headingAnchor(heading.getTextContent());
                    if (!anchor.isEmpty()) {
                        html.append(" id=\"")
                            .append(escapeAttribute(anchor))
                            .append("\"");
                    }
                }
                html.append(">");
                open = true;
            }
            appendFlowContent(
                html,
                content,
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
            emitted = true;
        }
        if (open) {
            html.append("</")
                .append(tagName)
                .append(">");
        } else if (!emitted) {
            html.append("<")
                .append(tagName)
                .append("></")
                .append(tagName)
                .append(">");
        }
    }

    private void appendList(StringBuilder html, LytList list, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        String tagName = list.isOrdered() ? "ol" : "ul";
        html.append("<")
            .append(tagName);
        if (list.isOrdered() && list.getStart() > 1) {
            html.append(" start=\"")
                .append(list.getStart())
                .append("\"");
        }
        html.append(">");
        for (LytNode child : list.getChildren()) {
            appendBlock(
                html,
                child,
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
        }
        html.append("</")
            .append(tagName)
            .append(">");
    }

    private void appendListItem(StringBuilder html, LytListItem listItem, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        html.append("<li>");
        for (LytNode child : listItem.getChildren()) {
            appendBlock(
                html,
                child,
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
        }
        html.append("</li>");
    }

    private void appendTable(StringBuilder html, LytTable table, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        html.append("<div class=\"guide-table-wrap\"><table class=\"guide-table\">");
        List<LytTableRow> rows = table.getChildren();
        if (!rows.isEmpty()) {
            html.append("<thead>");
            appendTableRow(
                html,
                rows.getFirst(),
                "th",
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
            html.append("</thead>");
        }
        if (rows.size() > 1) {
            html.append("<tbody>");
            for (int i = 1; i < rows.size(); i++) {
                appendTableRow(
                    html,
                    rows.get(i),
                    "td",
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            }
            html.append("</tbody>");
        }
        html.append("</table></div>");
    }

    private void appendTableRow(StringBuilder html, LytTableRow row, String cellTagName,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        html.append("<tr>");
        for (LytTableCell cell : row.getChildren()) {
            html.append("<")
                .append(cellTagName)
                .append(">");
            for (LytNode child : cell.getChildren()) {
                appendBlock(
                    html,
                    child,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            }
            html.append("</")
                .append(cellTagName)
                .append(">");
        }
        html.append("</tr>");
    }

    private void appendParagraphStyleAttribute(StringBuilder html, ResolvedTextStyle style) {
        StringBuilder css = new StringBuilder();
        if (style.alignment() == TextAlignment.CENTER) {
            css.append("text-align:center;");
        } else if (style.alignment() == TextAlignment.RIGHT) {
            css.append("text-align:right;");
        }
        if (!css.isEmpty()) {
            html.append(" style=\"")
                .append(escapeAttribute(css.toString()))
                .append("\"");
        }
    }

    private void appendFlowContent(StringBuilder html, @Nullable LytFlowContent content,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        switch (content) {
            case LytFlowText text -> html.append(escapeHtml(text.getText()));
            case LytFlowBreak lytFlowBreak -> html.append("<br>");
            case LytFlowAnchor flowAnchor -> html.append("<span id=\"")
                    .append(escapeAttribute(flowAnchor.getName()))
                    .append("\"></span>");
            case LytFlowInlineBlock inlineBlock -> appendInlineBlock(
                    html,
                    inlineBlock,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            case LytFlowLink link -> appendLink(html, link, currentPageId, assetExporter, itemIconResolver,
                templates, allowNestedItemTooltips);
            case LytFlowSpan span -> appendStyledFlowContainer(
                    html,
                    span,
                    "span",
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips,
                    null);
            case null, default -> {
            }
        }
    }

    private void appendInlineBlock(StringBuilder html, LytFlowInlineBlock inlineBlock,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        LytBlock block = inlineBlock.getBlock();
        if (block instanceof LytItemImage itemImage) {
            appendItemStacks(
                html,
                itemImage.getStacks(),
                itemIconResolver,
                currentPageId,
                assetExporter,
                templates,
                allowNestedItemTooltips);
            return;
        }
        if (block instanceof LytImage image) {
            appendImage(html, image, assetExporter);
            return;
        }
        appendBlock(html, block, currentPageId, assetExporter, itemIconResolver, templates, allowNestedItemTooltips);
    }

    private void appendLink(StringBuilder html, LytFlowLink link, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        String href = null;
        PageAnchor pageAnchor = link.getPageAnchor();
        if (pageAnchor != null) {
            GuideAnchor guideAnchor = link.getGuideAnchor();
            ResourceLocation targetGuideId = guideAnchor != null ? guideAnchor.guideId() : null;
            href = GuideSiteHrefResolver.resolvePageAnchor(currentPageId, targetGuideId, pageAnchor);
        } else if (link.getExternalUrl() != null) {
            href = link.getExternalUrl()
                .toString();
        }

        appendStyledFlowContainer(
            html,
            link,
            href != null && !href.isEmpty() ? "a" : "span",
            currentPageId,
            assetExporter,
            itemIconResolver,
            templates,
            allowNestedItemTooltips,
            href);
    }

    private void appendStyledFlowContainer(StringBuilder html, LytFlowSpan span, String tagName,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips, @Nullable String href) {
        html.append("<")
            .append(tagName);
        String templateId = null;
        if (templates != null && span instanceof LytTooltipSpan tooltipSpan) {
            templateId = createTemplateId(
                tooltipSpan.getTooltip(0, 0)
                    .orElse(null),
                templates,
                currentPageId,
                assetExporter,
                itemIconResolver);
        }
        if ("a".equals(tagName) && href != null && !href.isEmpty()) {
            html.append(" href=\"")
                .append(escapeAttribute(href))
                .append("\"");
        }
        boolean insideSpoiler = isInsideSpoiler(span);
        boolean spoiler = span instanceof LytSpoilerSpan;
        if (templateId != null || spoiler) {
            html.append(" class=\"")
                .append(templateId != null ? "guide-tooltip" : "")
                .append(templateId != null && spoiler ? " " : "")
                .append(spoiler ? "guide-spoiler" : "")
                .append("\"");
        }
        if (templateId != null) {
            html.append(" data-template=\"")
                .append(escapeAttribute(templateId))
                .append("\"");
        }
        if (spoiler) {
            html.append(" tabindex=\"0\"");
        }
        appendInlineStyleAttribute(html, span.resolveStyle(), insideSpoiler);
        html.append(">");
        for (LytFlowContent child : span.getChildren()) {
            appendFlowContent(
                html,
                child,
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
        }
        html.append("</")
            .append(tagName)
            .append(">");
    }

    private boolean isInsideSpoiler(LytFlowContent content) {
        return content.findAncestor(LytSpoilerSpan.class) != null;
    }

    private void appendBlockChildren(StringBuilder html, LytNode node, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        for (LytNode child : node.getChildren()) {
            if (child instanceof LytBlock childBlock) {
                appendBlock(
                    html,
                    childBlock,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            }
        }
    }

    private void appendCodeBlock(StringBuilder html, LytCodeBlock codeBlock) {
        String language = codeBlock.getDetectedLanguageId();
        if ((language == null || language.isBlank() || "text".equalsIgnoreCase(language))
            && codeBlock.getLanguageFenceName() != null
            && !codeBlock.getLanguageFenceName()
                .isBlank()) {
            language = codeBlock.getLanguageFenceName();
        }
        String source = codeBlock.getCodeText() != null ? codeBlock.getCodeText() : "";
        if ("csv".equalsIgnoreCase(language)) {
            html.append(GuideSiteGraphRenderer.renderCsvTable(source, true));
            return;
        }
        if ("tree".equalsIgnoreCase(language) || "filetree".equalsIgnoreCase(language)) {
            html.append(GuideSiteGraphRenderer.renderFileTree(source));
            return;
        }
        if ("mermaid".equalsIgnoreCase(language)) {
            appendMermaidSource(html, source);
            return;
        }
        html.append("<pre class=\"guide-code-block\"");
        if (codeBlock.getForcedBodyHeight() > 0) {
            html.append(" style=\"max-height:")
                .append(codeBlock.getForcedBodyHeight())
                .append("px;overflow:auto;\"");
        }
        html.append("><code");
        if (codeBlock.getLanguageDisplayName() != null && !codeBlock.getLanguageDisplayName()
            .isEmpty()) {
            html.append(" data-language=\"")
                .append(escapeAttribute(codeBlock.getLanguageDisplayName()))
                .append("\"");
        }
        html.append(">")
            .append(escapeHtml(source))
            .append("</code></pre>");
    }

    private void appendMermaidSource(StringBuilder html, String source) {
        try {
            MermaidDiagramType type = MermaidDiagramType.detect(source);
            if (type == MermaidDiagramType.MINDMAP) {
                html.append(GuideSiteGraphRenderer.renderMermaidTree(MindmapParser.parse(source)));
            } else if (type == MermaidDiagramType.FLOWCHART) {
                html.append(GuideSiteGraphRenderer.renderFlowchart(FlowchartParser.parse(source)));
            } else {
                html.append("<pre class=\"guide-code-block\"><code>")
                    .append(escapeHtml(source))
                    .append("</code></pre>");
            }
        } catch (RuntimeException exception) {
            html.append("<pre class=\"guide-code-block\"><code>")
                .append(escapeHtml(source))
                .append("</code></pre>");
        }
    }

    private void appendFileTree(StringBuilder html, LytFileTree fileTree, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        html.append("<div class=\"guide-file-tree\">");
        for (LytFileTree.Row row : fileTree.getRows()) {
            html.append("<div class=\"guide-file-tree-row\">");
            html.append("<span class=\"guide-file-tree-prefix\">")
                .append(escapeHtml(fileTreePrefix(row.getSlots())))
                .append("</span>");
            if (row.getIconBlock() != null) {
                html.append("<span class=\"guide-file-tree-icon\">");
                appendBlock(
                    html,
                    row.getIconBlock(),
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
                html.append("</span>");
            }
            html.append("<span class=\"guide-file-tree-name\">");
            appendBlock(
                html,
                row.getPayload(),
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
            html.append("</span>");
            html.append("</div>");
        }
        html.append("</div>");
    }

    private String fileTreePrefix(List<SlotKind> slots) {
        StringBuilder prefix = new StringBuilder();
        for (SlotKind slot : slots) {
            switch (slot) {
                case VERTICAL -> prefix.append("|   ");
                case BRANCH -> prefix.append("|-- ");
                case LAST_BRANCH -> prefix.append("`-- ");
                case EMPTY -> prefix.append("    ");
            }
        }
        return prefix.toString();
    }

    private void appendGuiSprite(StringBuilder html, LytGuiSprite sprite,
        @Nullable GuideSitePageAssetExporter assetExporter) {
        int width = Math.max(
            1,
            sprite.getSize()
                .width());
        int height = Math.max(
            1,
            sprite.getSize()
                .height());
        if (sprite.getSprite() == null || assetExporter == null) {
            html.append("<span class=\"guide-gui-sprite-placeholder\" style=\"width:")
                .append(width)
                .append("px;height:")
                .append(height)
                .append("px\" aria-label=\"GUI sprite\"></span>");
            return;
        }
        String src = assetExporter.exportResource(
            sprite.getSprite()
                .getTexture());
        if (src.isEmpty()) {
            html.append("<span class=\"guide-gui-sprite-placeholder\" style=\"width:")
                .append(width)
                .append("px;height:")
                .append(height)
                .append("px\" aria-label=\"GUI sprite\"></span>");
            return;
        }
        html.append("<span class=\"guide-gui-sprite\" style=\"display:inline-block;width:")
            .append(width)
            .append("px;height:")
            .append(height)
            .append("px;background-image:url('")
            .append(escapeAttribute(src))
            .append("');background-size:")
            .append(
                sprite.getSprite()
                    .getTexWidth())
            .append("px ")
            .append(
                sprite.getSprite()
                    .getTexHeight())
            .append("px;background-position:-")
            .append(
                sprite.getSprite()
                    .getU())
            .append("px -")
            .append(
                sprite.getSprite()
                    .getV())
            .append("px;background-repeat:no-repeat;\"></span>");
    }

    private void appendStructureView(StringBuilder html, LytStructureView structure,
        GuideSiteItemIconResolver itemIconResolver, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        html.append("<div class=\"guide-structure-view guide-tooltip-structure\">");
        for (LytStructureView.BlockEntry entry : structure.getBlocks()) {
            GuideSiteExportedItem item = GuideSiteItemSupport.export(entry.stack, itemIconResolver);
            String templateId = allowNestedItemTooltips && templates != null
                ? createNestedItemTemplateId(entry.stack, currentPageId, assetExporter, itemIconResolver, templates)
                : null;
            html.append("<span class=\"guide-structure-block");
            if (templateId != null) html.append(" guide-tooltip");
            html.append("\" title=\"")
                .append(escapeAttribute("(" + entry.x + ", " + entry.y + ", " + entry.z + ")"));
            if (templateId != null) {
                html.append("\" data-template=\"")
                    .append(escapeAttribute(templateId));
            }
            html.append("\">");
            GuideSiteItemHtml.appendIcon(html, item, "guide-structure-block-icon");
            html.append("</span>");
        }
        html.append("</div>");
    }

    private void appendLatex(StringBuilder html, String formula, int fillColorArgb, float sourceScale, float userScale,
        @Nullable GuideTooltip tooltip, boolean display, int offsetX, int offsetY,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates) {
        GuideSiteLatexExporter.ExportedLatex exported = assetExporter != null ? assetExporter.latexExporter()
            .export(formula, fillColorArgb, sourceScale) : null;
        String templateId = templates != null
            ? createTemplateId(
                tooltip != null ? tooltip : new TextTooltip(formula),
                templates,
                currentPageId,
                assetExporter,
                itemIconResolver)
            : null;
        String tag = display ? "div" : "span";
        html.append("<")
            .append(tag)
            .append(" class=\"guide-latex ")
            .append(display ? "guide-latex-display" : "guide-latex-inline");
        if (templateId != null) {
            html.append(" guide-tooltip\" data-template=\"")
                .append(escapeAttribute(templateId));
        } else {
            html.append("\"");
        }
        if (offsetX != 0 || offsetY != 0) {
            html.append(" style=\"transform:translate(")
                .append(offsetX)
                .append("px,")
                .append(offsetY)
                .append("px)\"");
        }
        html.append(">");
        if (exported != null) {
            int width = Math.max(1, Math.round(exported.widthPx() * userScale));
            int height = Math.max(1, Math.round(exported.heightPx() * userScale));
            html.append("<img class=\"guide-latex-image\" src=\"")
                .append(escapeAttribute(exported.src()))
                .append("\" width=\"")
                .append(width)
                .append("\" height=\"")
                .append(height)
                .append("\" alt=\"")
                .append(escapeAttribute(formula))
                .append("\">");
        } else {
            html.append(escapeHtml(formula));
        }
        html.append("</")
            .append(tag)
            .append(">");
    }

    private void appendContentTabs(StringBuilder html, LytContentTabsBlock tabs,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        html.append("<div class=\"guide-content-tabs\"");
        if (tabs.getBackgroundColor() != null) {
            html.append(" style=\"--guide-content-tabs-background:")
                .append(escapeAttribute(toCssColor(tabs.getBackgroundColor())))
                .append(";\"");
        }
        html.append('>');
        List<? extends LytNode> children = tabs.getChildren();
        if (!children.isEmpty() && children.getFirst() instanceof LytParagraph title) {
            appendBlock(
                html,
                title,
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
        }
        html.append("<div class=\"guide-content-tabs-body\">");
        for (int i = 1; i < children.size(); i++) {
            appendBlock(
                html,
                children.get(i),
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
        }
        html.append("</div></div>");
    }

    private GuideSiteGraphRenderer.ChartStyle chartStyle(LytChartBase chart, @Nullable ChartAxisOptions xAxis,
        @Nullable ChartAxisOptions yAxis, float barWidthRatio, float pieStartAngle, boolean pieClockwise) {
        return new GuideSiteGraphRenderer.ChartStyle(
            chart.getExplicitWidth(),
            chart.getExplicitHeight(),
            chart.getBackgroundColor(),
            chart.getBorderColor(),
            chart.getTitle(),
            chart.getLegendPosition(),
            chart.getLabelPosition(),
            chart.getLabelColor(),
            SiteChartAxis.from(xAxis, GuideSiteGraphRenderer.DEFAULT_GRID_COLOR),
            SiteChartAxis.from(yAxis, GuideSiteGraphRenderer.DEFAULT_GRID_COLOR),
            chart.getTitleColor(),
            barWidthRatio,
            pieStartAngle,
            pieClockwise);
    }

    private List<GuideSiteGraphRenderer.SeriesData> seriesData(List<ChartSeries> series) {
        List<GuideSiteGraphRenderer.SeriesData> result = new ArrayList<>();
        for (ChartSeries item : series) {
            result.add(
                new GuideSiteGraphRenderer.SeriesData(item.getName(), item.getColor(), item.getXs(), item.getYs()));
        }
        return result;
    }

    private String[] categories(String[] values) {
        return values != null ? values : new String[0];
    }

    private String renderColumnChart(LytColumnChart chart) {
        GuideSiteGraphRenderer.ChartStyle style = chartStyle(
            chart,
            chart.getXAxis(),
            chart.getYAxis(),
            chart.getBarWidthRatio(),
            -90f,
            true);
        GuideSiteGraphRenderer.PieInsetData inset = chart.getPieInset() != null ? pieInsetData(chart.getPieInset())
            : null;
        List<GuideSiteGraphRenderer.SeriesData> series = seriesData(chart.getSeries());
        for (ChartSeries overlay : chart.getLineOverlays()) {
            series.add(
                new GuideSiteGraphRenderer.SeriesData(
                    overlay.getName(),
                    overlay.getColor(),
                    overlay.getXs(),
                    overlay.getYs(),
                    GuideSiteGraphRenderer.SeriesData.TYPE_LINE));
        }
        return GuideSiteGraphRenderer.renderColumnChart(style, categories(chart.getCategories()), series, inset, null);
    }

    private String renderBarChart(LytBarChart chart) {
        return GuideSiteGraphRenderer.renderBarChart(
            chartStyle(chart, chart.getXAxis(), chart.getYAxis(), chart.getBarWidthRatio(), -90f, true),
            categories(chart.getCategories()),
            seriesData(chart.getSeries()));
    }

    private String renderLineChart(LytLineChart chart) {
        return GuideSiteGraphRenderer.renderLineChart(
            chartStyle(chart, chart.getXAxis(), chart.getYAxis(), 0.7f, -90f, true),
            categories(chart.getCategories()),
            seriesData(chart.getSeries()),
            chart.isNumericX(),
            chart.isShowPoints(),
            chart.getCornerLegendPosition(),
            chart.getCornerLegendWidth(),
            chart.getCornerLegendHeight(),
            chart.getCornerLegendBackgroundColor());
    }

    private String renderPieChart(LytPieChart chart) {
        List<GuideSiteGraphRenderer.SliceData> slices = new ArrayList<>();
        for (PieSlice slice : chart.getSlices()) {
            slices.add(new GuideSiteGraphRenderer.SliceData(slice.getLabel(), slice.getValue(), slice.getColor()));
        }
        return GuideSiteGraphRenderer
            .renderPieChart(chartStyle(chart, null, null, 0.7f, chart.getStartAngleDeg(), chart.isClockwise()), slices);
    }

    private String renderScatterChart(LytScatterChart chart) {
        return GuideSiteGraphRenderer.renderScatterChart(
            chartStyle(chart, chart.getXAxis(), chart.getYAxis(), 0.7f, -90f, true),
            seriesData(chart.getSeries()),
            chart.getCornerLegendPosition(),
            chart.getCornerLegendWidth(),
            chart.getCornerLegendHeight(),
            chart.getCornerLegendBackgroundColor());
    }

    private GuideSiteGraphRenderer.PieInsetData pieInsetData(PieInsetSpec inset) {
        List<GuideSiteGraphRenderer.SliceData> slices = new ArrayList<>();
        for (PieSlice slice : inset.getSlices()) {
            slices.add(new GuideSiteGraphRenderer.SliceData(slice.getLabel(), slice.getValue(), slice.getColor()));
        }
        return new GuideSiteGraphRenderer.PieInsetData(
            slices,
            inset.getSize(),
            inset.getPosition()
                .name()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-'),
            inset.getTitle());
    }

    private void appendImageBlock(StringBuilder html, LytImageBlock image, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates) {
        String source = image.getSrc();
        if (source == null || source.isBlank()) {
            return;
        }
        String src = source;
        if (assetExporter != null) {
            try {
                src = assetExporter.exportResource(new ResourceLocation(source));
            } catch (IllegalArgumentException e) {
                GuideDebugLog.warnAlways("[GuideNH] [SceneExport] Invalid tooltip image source: {}", source);
                return;
            }
        }
        if (src.isEmpty()) {
            GuideDebugLog.warnAlways("[GuideNH] [SceneExport] Missing tooltip image: {}", source);
            return;
        }

        boolean cropped = image.getCropWidth() > 0 && image.getCropHeight() > 0;
        int displayWidth = image.getDisplayWidth();
        int displayHeight = image.getDisplayHeight();
        if (cropped) {
            if (displayWidth > 0 || displayHeight > 0) {
                if (displayWidth <= 0) {
                    displayWidth = Math.max(
                        1,
                        (int) Math.round(displayHeight * image.getCropWidth() / (double) image.getCropHeight()));
                }
                if (displayHeight <= 0) {
                    displayHeight = Math
                        .max(1, (int) Math.round(displayWidth * image.getCropHeight() / (double) image.getCropWidth()));
                }
            } else {
                displayWidth = Math.max(1, (int) Math.round(image.getCropWidth() * image.getScaleX()));
                displayHeight = Math.max(1, (int) Math.round(image.getCropHeight() * image.getScaleY()));
            }
        }

        html.append("<span class=\"guide-floating-image-wrap guide-floating-image-inline\"");
        if (displayWidth > 0) {
            html.append(" style=\"width:")
                .append(displayWidth)
                .append("px;\"");
        }
        html.append("><span class=\"guide-floating-image-stage");
        if (cropped) {
            html.append(" guide-floating-image-crop");
        }
        html.append("\"");
        if (cropped) {
            html.append(" style=\"aspect-ratio:")
                .append(displayWidth)
                .append(" / ")
                .append(displayHeight)
                .append(";\"");
        }
        html.append("><img class=\"guide-image guide-floating-image\" src=\"")
            .append(escapeAttribute(src))
            .append("\" alt=\"")
            .append(escapeAttribute(image.getAlt() != null ? image.getAlt() : ""))
            .append("\"");
        if (cropped) {
            html.append(" data-crop-x=\"")
                .append(image.getCropX())
                .append("\" data-crop-y=\"")
                .append(image.getCropY())
                .append("\" data-crop-width=\"")
                .append(image.getCropWidth())
                .append("\" data-crop-height=\"")
                .append(image.getCropHeight())
                .append("\"");
        } else if (displayHeight > 0 && displayWidth <= 0) {
            html.append(" data-display-height=\"")
                .append(displayHeight)
                .append("\"");
        }
        html.append(" loading=\"lazy\" decoding=\"async\">");
        for (ImageRegionAnnotation annotation : image.getAnnotations()) {
            String templateId = templates != null
                ? createTemplateId(annotation.getTooltip(), templates, currentPageId, assetExporter, itemIconResolver)
                : null;
            html.append("<span class=\"guide-image-annotation");
            if (templateId != null) {
                html.append(" guide-tooltip");
            }
            html.append("\"");
            if (templateId != null) {
                html.append(" data-template=\"")
                    .append(escapeAttribute(templateId))
                    .append("\"");
            }
            if (!annotation.isWholeImage()) {
                html.append(" data-source-x=\"")
                    .append(annotation.getImgX())
                    .append("\" data-source-y=\"")
                    .append(annotation.getImgY())
                    .append("\" data-source-width=\"")
                    .append(annotation.getImgW())
                    .append("\" data-source-height=\"")
                    .append(annotation.getImgH())
                    .append("\"");
            }
            html.append(" style=\"")
                .append(
                    annotation.isWholeImage() ? "left:0;top:0;width:100%;height:100%;"
                        : "left:0;top:0;width:1px;height:1px;");
            if (annotation.isShowBorder()) {
                html.append("border:")
                    .append(annotation.getBorderThickness())
                    .append("px solid ")
                    .append(toCssColor(annotation.getBorderColor()))
                    .append(";");
            }
            html.append("\"></span>");
        }
        html.append("</span>");
        if (image.getTitle() != null && !image.getTitle()
            .isEmpty()) {
            html.append("<span class=\"guide-floating-image-title\">")
                .append(escapeHtml(image.getTitle()))
                .append("</span>");
        }
        html.append("</span>");
    }

    private void appendBlockImage(StringBuilder html, BlockImagePlaceholder blockImage,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        GuideItemReferenceResolver.ResolvedBlockReference block = GuideItemReferenceResolver
            .resolveBlockReference("minecraft", blockImage.id, blockImage.ore);
        if (block == null || block.stack() == null) {
            html.append("<span class=\"guide-export-error\">")
                .append(
                    escapeHtml(
                        blockImage.id != null ? blockImage.id : blockImage.ore != null ? blockImage.ore : "BlockImage"))
                .append("</span>");
            return;
        }
        ItemStack stack = block.stack()
            .copy();
        if (blockImage.meta != Integer.MIN_VALUE && stack.getItem() != null) {
            stack.setItemDamage(blockImage.meta);
        }
        if (blockImage.nbt != null && !blockImage.nbt.isBlank()) {
            try {
                NBTTagCompound explicitTag = GuideTextNbtCodec.readTextSafeCompound(blockImage.nbt.trim());
                if (stack.stackTagCompound == null) {
                    stack.stackTagCompound = explicitTag;
                } else {
                    for (String key : explicitTag.func_150296_c()) {
                        stack.stackTagCompound.setTag(key, explicitTag.getTag(key));
                    }
                }
            } catch (Exception exception) {
                GuideDebugLog.warnAlways(
                    "[GuideNH] [SceneExport] Invalid tooltip BlockImage NBT: {}",
                    blockImage.id != null ? blockImage.id : blockImage.ore);
            }
        }
        String templateId = allowNestedItemTooltips && templates != null
            ? createNestedItemTemplateId(stack, currentPageId, assetExporter, itemIconResolver, templates)
            : null;
        GuideSiteExportedItem item = GuideSiteItemSupport.export(stack, itemIconResolver);
        html.append("<span class=\"guide-block-image guide-inline-item");
        if (templateId != null && !templateId.isEmpty()) {
            html.append(" guide-tooltip");
        }
        html.append("\" data-item-id=\"")
            .append(escapeAttribute(item.itemId()))
            .append("\" data-guide-item-id=\"")
            .append(escapeAttribute(item.itemId()))
            .append("\"");
        if (templateId != null && !templateId.isEmpty()) {
            html.append(" data-template=\"")
                .append(escapeAttribute(templateId))
                .append("\"");
        }
        html.append(">");
        GuideSiteItemHtml.appendIcon(html, item, "guide-inline-item-icon", blockImage.scale);
        html.append("</span>");
    }

    private void appendMermaidMindmap(StringBuilder html, LytMermaidMindmap mindmap,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        Map<String, String> nodeHtml = renderMermaidNodeContent(
            mindmap.getNodeContent(),
            currentPageId,
            assetExporter,
            itemIconResolver,
            templates,
            allowNestedItemTooltips);
        html.append(GuideSiteGraphRenderer.renderMermaidTree(mindmap.getMindmap(), nodeHtml));
    }

    private void appendMermaidFlowchart(StringBuilder html, LytMermaidFlowchart flowchart,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        Map<String, String> nodeHtml = renderMermaidNodeContent(
            flowchart.getNodeContent(),
            currentPageId,
            assetExporter,
            itemIconResolver,
            templates,
            allowNestedItemTooltips);
        html.append(GuideSiteGraphRenderer.renderFlowchart(flowchart.getFlowchart(), nodeHtml));
    }

    private void appendMermaidPlaceholder(StringBuilder html, MermaidPlaceholder mermaid,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        String source = mermaid.sourceText != null ? mermaid.sourceText.trim() : "";
        if (source.isEmpty()) {
            html.append("<div class=\"guide-export-error\">Mermaid source is unavailable for this tooltip.</div>");
            return;
        }
        try {
            Map<String, String> nodeHtml = renderMermaidNodeContent(
                mermaid.nodeContentBlocks,
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
            if (MermaidDiagramType.MINDMAP == mermaid.diagramType) {
                html.append(GuideSiteGraphRenderer.renderMermaidTree(MindmapParser.parse(source), nodeHtml));
            } else if (MermaidDiagramType.FLOWCHART == mermaid.diagramType) {
                html.append(GuideSiteGraphRenderer.renderFlowchart(FlowchartParser.parse(source), nodeHtml));
            } else {
                html.append("<pre class=\"guide-code-block\"><code>")
                    .append(escapeHtml(source))
                    .append("</code></pre>");
            }
        } catch (RuntimeException exception) {
            html.append("<pre class=\"guide-code-block\"><code>")
                .append(escapeHtml(source))
                .append("</code></pre>");
        }
    }

    private Map<String, String> renderMermaidNodeContent(Map<String, LytBlock> nodeContent,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, LytBlock> entry : nodeContent.entrySet()) {
            StringBuilder body = new StringBuilder();
            appendBlock(
                body,
                entry.getValue(),
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
            result.put(entry.getKey(), body.toString());
        }
        return result;
    }

    private void appendItemImage(StringBuilder html, ItemImagePlaceholder image,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        ItemStack stack = GuideDisplayItemStacks.resolveItemStack(image.itemId, "minecraft");
        if (stack == null && image.ore != null) {
            stack = GuideDisplayItemStacks.resolveOreStack(image.ore);
        }
        if (stack == null) {
            html.append("<span class=\"guide-export-error\">")
                .append(escapeHtml(image.itemId))
                .append("</span>");
            return;
        }
        if (image.nbt != null && !image.nbt.isBlank()) {
            try {
                NBTTagCompound explicitTag = GuideTextNbtCodec.readTextSafeCompound(image.nbt.trim());
                if (stack.stackTagCompound == null) {
                    stack.stackTagCompound = explicitTag;
                } else {
                    for (String key : explicitTag.func_150296_c()) {
                        stack.stackTagCompound.setTag(key, explicitTag.getTag(key));
                    }
                }
            } catch (Exception e) {
                GuideDebugLog.warnAlways("[GuideNH] [SceneExport] Invalid tooltip ItemImage NBT: {}", image.itemId);
            }
        }

        GuideSiteExportedItem item = GuideSiteItemSupport.export(stack, itemIconResolver);
        String templateId = image.showTooltip && allowNestedItemTooltips && templates != null
            ? createNestedItemTemplateId(stack, currentPageId, assetExporter, itemIconResolver, templates)
            : null;
        html.append("<span class=\"guide-inline-item");
        if (templateId != null && !templateId.isEmpty()) {
            html.append(" guide-tooltip");
        }
        html.append("\" data-item-id=\"")
            .append(escapeAttribute(item.itemId()))
            .append("\" data-guide-item-id=\"")
            .append(escapeAttribute(item.itemId()))
            .append("\"");
        String href = templates != null ? templates.resolveItemHref(stack) : "";
        if (!href.isEmpty()) {
            html.append(" data-guide-item-href=\"")
                .append(escapeAttribute(href))
                .append("\"");
        }
        if (templateId != null && !templateId.isEmpty()) {
            html.append(" data-template=\"")
                .append(escapeAttribute(templateId))
                .append("\"");
        }
        if (image.yOffset != null) {
            html.append(" style=\"position:relative;top:")
                .append(Math.round(image.yOffset * Math.max(0.125f, image.scale)))
                .append("px;\"");
        }
        html.append(">");
        String label = image.labelPosition != null ? stack.getDisplayName() : null;
        if (label != null && "left".equals(image.labelPosition)) {
            appendItemImageLabel(html, label, image);
        }
        if (!Boolean.FALSE.equals(image.showIcon)) {
            GuideSiteItemHtml.appendIcon(html, item, "guide-inline-item-icon", image.scale);
        }
        if (label != null && !"left".equals(image.labelPosition)) {
            appendItemImageLabel(html, label, image);
        }
        html.append("</span>");
    }

    private void appendItemImageLabel(StringBuilder html, String label, ItemImagePlaceholder image) {
        String formatted = image.labelFormat != null && image.labelFormat.contains("%s")
            ? image.labelFormat.replace("%s", label)
            : image.labelFormat != null ? image.labelFormat : label;
        html.append("<em class=\"guide-item-label\"");
        if (image.labelYOffset != null) {
            html.append(" style=\"position:relative;top:")
                .append(image.labelYOffset)
                .append("px;\"");
        }
        html.append(">")
            .append(escapeHtml(formatted))
            .append("</em>");
    }

    private void appendImage(StringBuilder html, LytImage image, @Nullable GuideSitePageAssetExporter assetExporter) {
        String src = "";
        if (image.getImageId() != null && assetExporter != null) {
            src = assetExporter.exportResource(image.getImageId());
        }
        if (src.isEmpty() && image.getImageId() != null) {
            src = image.getImageId()
                .toString();
        }
        if (src.isEmpty()) {
            return;
        }

        html.append("<img class=\"guide-image\" src=\"")
            .append(escapeAttribute(src))
            .append("\" alt=\"")
            .append(escapeAttribute(image.getAlt() != null ? image.getAlt() : ""))
            .append("\"");
        if (image.getTitle() != null && !image.getTitle()
            .isEmpty()) {
            html.append(" title=\"")
                .append(escapeAttribute(image.getTitle()))
                .append("\"");
        }
        html.append(" loading=\"lazy\" decoding=\"async\">");
    }

    private void appendInlineStyleAttribute(StringBuilder html, ResolvedTextStyle style, boolean insideSpoiler) {
        StringBuilder css = new StringBuilder();
        if (style.bold()) {
            css.append("font-weight:700;");
        }
        if (style.italic()) {
            css.append("font-style:italic;");
        }
        if (!insideSpoiler && (style.underlined() || style.strikethrough())) {
            css.append("text-decoration:");
            if (style.underlined()) {
                css.append(" underline");
            }
            if (style.strikethrough()) {
                css.append(" line-through");
            }
            css.append(";");
        }
        if (!insideSpoiler && style.color() != null) {
            css.append("color:")
                .append(toCssColor(style.color()))
                .append(";");
        }
        if (style.fontScale() != 1.0f) {
            css.append("font-size:")
                .append(style.fontScale())
                .append("em;");
        }
        if (!css.isEmpty()) {
            html.append(" style=\"")
                .append(escapeAttribute(css.toString()))
                .append("\"");
        }
    }

    private void appendSlotGrid(StringBuilder html, LytSlotGrid grid, GuideSiteItemIconResolver itemIconResolver,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        html.append("<div class=\"guide-tooltip-item-grid\" style=\"--guide-tooltip-columns:")
            .append(Math.max(1, grid.getWidth()))
            .append("\">");
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                LytSlot slot = grid.getSlot(col, row);
                if (slot == null && !grid.isRenderEmptySlots()) {
                    continue;
                }
                html.append(
                    grid.isRenderSlotBackground() ? "<div class=\"ingredient-box\">"
                        : "<div class=\"ingredient-box ingredient-box--plain\">");
                if (slot != null) appendSlot(
                    html,
                    slot,
                    itemIconResolver,
                    currentPageId,
                    assetExporter,
                    templates,
                    allowNestedItemTooltips);
                html.append("</div>");
            }
        }
        html.append("</div>");
    }

    private void appendItemGrid(StringBuilder html, LytItemGrid grid, GuideSiteItemIconResolver itemIconResolver,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        html.append("<div class=\"guide-tooltip-item-grid\">");
        for (LytNode child : grid.getChildren()) {
            if (!(child instanceof LytSlot slot)) {
                continue;
            }
            html.append("<div class=\"ingredient-box\">");
            appendSlot(html, slot, itemIconResolver, currentPageId, assetExporter, templates, allowNestedItemTooltips);
            html.append("</div>");
        }
        html.append("</div>");
    }

    private void appendSlot(StringBuilder html, LytSlot slot, GuideSiteItemIconResolver itemIconResolver,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        Optional<GuideTooltip> tooltip = slot.getTooltip(0, 0);
        if (tooltip.isPresent() && tooltip.get() instanceof ItemTooltip itemTooltip) {
            ItemStack stack = itemTooltip.getStack();
            if (stack != null) {
                appendItemStacks(
                    html,
                    List.of(stack),
                    itemIconResolver,
                    currentPageId,
                    assetExporter,
                    templates,
                    allowNestedItemTooltips);
            }
        }
    }

    private void appendItemStacks(StringBuilder html, List<ItemStack> stacks,
        GuideSiteItemIconResolver itemIconResolver, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        if (stacks == null || stacks.isEmpty()) {
            return;
        }
        for (ItemStack stack : stacks) {
            String itemKey = templates != null ? GuideSiteItemSupport.tooltipCacheKey(stack) : null;
            if (itemKey != null) {
                String fragmentKey = (allowNestedItemTooltips ? "nested-icon:" : "plain-icon:") + itemKey;
                html.append(templates.getOrComputeLocalRendered(fragmentKey, () -> {
                    StringBuilder fragment = new StringBuilder();
                    appendExportedItemIcon(
                        fragment,
                        stack,
                        itemIconResolver,
                        currentPageId,
                        assetExporter,
                        templates,
                        allowNestedItemTooltips);
                    return fragment.toString();
                }));
                continue;
            }
            appendExportedItemIcon(
                html,
                stack,
                itemIconResolver,
                currentPageId,
                assetExporter,
                templates,
                allowNestedItemTooltips);
        }
    }

    private void appendExportedItemIcon(StringBuilder html, ItemStack stack, GuideSiteItemIconResolver itemIconResolver,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
        GuideSiteExportedItem item = GuideSiteItemSupport.export(stack, itemIconResolver);
        if (item.isEmpty()) {
            return;
        }
        String href = templates != null ? templates.resolveItemHref(stack) : "";
        if (!href.isEmpty()) {
            html.append("<span data-guide-item-href=\"")
                .append(escapeAttribute(href))
                .append("\">");
        }
        appendTooltipCapableItemIcon(
            html,
            stack,
            item,
            currentPageId,
            assetExporter,
            itemIconResolver,
            templates,
            allowNestedItemTooltips);
        if (!href.isEmpty()) {
            html.append("</span>");
        }
    }

    private void appendTooltipCapableItemIcon(StringBuilder html, ItemStack stack, GuideSiteExportedItem item,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
        boolean allowNestedItemTooltips) {
        if (!allowNestedItemTooltips || templates == null || stack == null || stack.stackSize <= 0) {
            GuideSiteItemHtml.appendIcon(html, item, null);
            return;
        }

        String templateId = createNestedItemTemplateId(
            stack,
            currentPageId,
            assetExporter,
            itemIconResolver,
            templates);
        if (templateId == null || templateId.isEmpty()) {
            GuideSiteItemHtml.appendIcon(html, item, null);
            return;
        }

        html.append("<span class=\"guide-tooltip\" data-template=\"")
            .append(escapeAttribute(templateId))
            .append("\">");
        GuideSiteItemHtml.appendIcon(html, item, null);
        html.append("</span>");
    }

    @Nullable
    private String createNestedItemTemplateId(ItemStack stack, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        GuideSiteTemplateRegistry templates) {
        String semanticKey = GuideSiteItemSupport.tooltipCacheKey(stack);
        return templates.getOrCreate(
            semanticKey,
            () -> render(
                new ItemTooltip(stack.copy()),
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                false));
    }

    private int clampHeadingDepth(int depth) {
        if (depth < 1) {
            return 1;
        }
        return Math.min(depth, 6);
    }
}
