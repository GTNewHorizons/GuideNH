package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.indices.GuideBindingIndex;
import com.hfstudio.guidenh.guide.internal.binding.BindingNavigationService;
import com.hfstudio.guidenh.guide.internal.binding.BindingPresentation;
import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;
import com.hfstudio.guidenh.guide.internal.binding.BindingSpec;
import com.hfstudio.guidenh.guide.internal.binding.BindingTagParser;
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;
import com.hfstudio.guidenh.integration.api.binding.BindingDisplay;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class GuideSiteBindingTagRenderer implements GuideSiteTagRenderer {

    @Nullable
    private final Guide guide;
    private final GuideBindingIndex index = new GuideBindingIndex();
    private final GuideSiteItemIconResolver icons;
    @Nullable
    private final GuideSitePageAssetExporter assets;

    public GuideSiteBindingTagRenderer(@Nullable Guide guide, Map<ResourceLocation, ParsedGuidePage> pages,
        GuideSiteItemIconResolver icons, @Nullable GuideSitePageAssetExporter assets) {
        this.guide = guide;
        this.icons = icons;
        this.assets = assets;
        index.rebuild(new ArrayList<>(pages.values()));
    }

    @Override
    public Set<String> getTagNames() {
        return Set.of(
            "BindingLink",
            "BindingCard",
            "BindingIcon",
            "ResearchLink",
            "ResearchCard",
            "ResearchIcon",
            "QuestLink",
            "QuestCard");
    }

    @Override
    public String render(GuideSiteTagRenderContext context, MdxJsxElementFields element) {
        try {
            return renderSpec(BindingTagParser.parse(element), element.name(), context.currentPageId());
        } catch (RuntimeException exception) {
            return "<span class=\"guide-error\">" + GuideSiteHtml.escapeHtml(exception.getMessage()) + "</span>";
        }
    }

    public String renderSpec(BindingSpec spec, String name, @Nullable ResourceLocation currentPageId) {
        try {
            var display = BindingRuntime
                .describe(spec.key(), BindingRuntime.view(spec.showDescription() || spec.showTooltip(), true));
            var targets = new LinkedHashSet<GuideAnchor>();
            if (guide != null)
                for (var anchor : index.findAll(spec.key())) targets.add(new GuideAnchor(guide.getId(), anchor));
            targets.addAll(BindingNavigationService.findTargets(spec.key()));
            String link = GuideSiteBindingHtml.link(spec, display, List.copyOf(targets), currentPageId);
            if (name.endsWith("Link")) return link;
            String image = icon(spec, display);
            if (name.endsWith("Icon")) return image.isEmpty() ? link : image;
            var html = new StringBuilder("<blockquote class=\"guide-binding-card\"><p>");
            if (spec.showIcon()) html.append(image)
                .append(" ");
            html.append(link)
                .append("</p>");
            if (spec.showDescription() && BindingPresentation.showDescription(display) && display.description() != null)
                html.append("<p>")
                    .append(GuideSiteHtml.escapeHtml(display.description()))
                    .append("</p>");
            if (spec.showParents() && !BindingPresentation.restricted(display)) {
                for (var parent : display.prerequisites()) {
                    var parentSpec = new BindingSpec(
                        parent,
                        null,
                        null,
                        "auto",
                        spec.showTooltip(),
                        false,
                        false,
                        false,
                        1);
                    var parentDisplay = BindingRuntime.describe(parent, BindingRuntime.view(false, true));
                    if (!BindingPresentation.restricted(parentDisplay)) html.append("<p>")
                        .append(
                            GuideSiteBindingHtml.link(
                                parentSpec,
                                parentDisplay,
                                BindingNavigationService.findTargets(parent),
                                currentPageId))
                        .append("</p>");
                }
            }
            return html.append("</blockquote>")
                .toString();
        } catch (RuntimeException exception) {
            return "<span class=\"guide-error\">" + GuideSiteHtml.escapeHtml(exception.getMessage()) + "</span>";
        }
    }

    private String icon(BindingSpec spec, BindingDisplay display) {
        var icon = display.icon();
        if (icon == null || BindingPresentation.restricted(display)) return "";
        String source = "";
        if (icon.texture() != null && assets != null) source = assets.exportResource(icon.texture());
        else if (icon.itemId() != null) source = icons.exportIcon(
            GuideDisplayItemStacks.resolveItemStack(
                icon.itemId() + ":" + icon.meta() + (icon.nbt() == null ? "" : icon.nbt()),
                "minecraft"));
        if (source == null || source.isEmpty()) return "";
        return "<img class=\"guide-binding-icon\" alt=\"" + GuideSiteHtml.escapeAttribute(BindingRuntime.title(display))
            + "\" src=\""
            + GuideSiteHtml.escapeAttribute(source)
            + "\" width=\""
            + Math.max(1, Math.round(16 * spec.scale()))
            + "\">";
    }
}
