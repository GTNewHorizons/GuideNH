package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteItemHtml.escapeHtml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.tags.FluidImageOptions;
import com.hfstudio.guidenh.guide.compiler.tags.FluidTagAttributes;
import com.hfstudio.guidenh.guide.compiler.tags.ItemImageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.indices.FluidIndex;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class GuideSiteFluidTagRenderer implements GuideSiteTagRenderer {

    private static final Set<String> TAG_NAMES = Set.of("FluidImage", "FluidLink");
    private final GuideSiteFluidIconResolver icons;
    private final Map<ResourceLocation, ParsedGuidePage> pages;
    private FluidIndex index;

    public GuideSiteFluidTagRenderer(GuideSiteFluidIconResolver icons, Map<ResourceLocation, ParsedGuidePage> pages) {
        this.icons = icons;
        this.pages = pages;
    }

    @Override
    public Set<String> getTagNames() {
        return TAG_NAMES;
    }

    @Override
    public @Nullable String render(GuideSiteTagRenderContext context, MdxJsxElementFields element) {
        if (!getTagNames().contains(element.name())) return null;
        try {
            if ("FluidImage".equals(element.name()))
                return renderImage(element, context.templates(), context.currentPageId());
            FluidStackSpec spec = FluidTagAttributes.readFluid(element);
            float scale = FluidTagAttributes.scale(element);
            String label = context.compiler()
                .compileInlineFragment(
                    element.children(),
                    context.templates(),
                    context.defaultNamespace(),
                    context.sceneResolver(),
                    context.currentPageId());
            if (label.isEmpty() && MdxAttrs.getBoolean(element, "showText", true)) {
                label = escapeHtml(FluidStacks.displayName(FluidStacks.resolve(spec)));
            }
            String iconPosition = ItemImageCompiler.resolveLabelPosition(element.getAttributeString("showIcon", null));
            if (iconPosition != null) {
                String icon = GuideSiteFluidHtml.icon(spec, icons, scale, true);
                label = "left".equals(iconPosition) ? icon + label : label + icon;
            }
            String override = element.getAttributeString("linksTo", null);
            List<PageAnchor> targets = override == null || override.isBlank() ? targets(spec.id())
                : explicitTarget(override, context.currentPageId());
            String template = FluidTagAttributes.showTooltip(element) ? context.templates()
                .create(GuideSiteFluidHtml.tooltip(spec)) : null;
            String attrs = template == null ? "" : " data-template=\"" + escapeHtml(template) + "\"";
            if (targets.size() > 1) {
                attrs += GuideSiteFluidHtml.targetAttributes(choices(targets, context.currentPageId()));
            }
            String destination = targets.isEmpty() ? "" : href(targets.getFirst(), context.currentPageId());
            String classes = "guide-item-link guide-fluid-link" + (template == null ? "" : " guide-tooltip");
            if (targets.size() > 1)
                return "<button type=\"button\" class=\"" + classes + "\"" + attrs + ">" + label + "</button>";
            if (destination.isEmpty())
                return "<span class=\"" + classes + " guide-item-link-muted\"" + attrs + ">" + label + "</span>";
            return "<a class=\"" + classes
                + "\" href=\""
                + escapeHtml(destination)
                + "\" data-guide-fluid-href=\""
                + escapeHtml(destination)
                + "\""
                + attrs
                + ">"
                + label
                + "</a>";
        } catch (IllegalArgumentException exception) {
            return "<span class=\"guide-export-error\">" + escapeHtml(exception.getMessage()) + "</span>";
        }
    }

    public String renderImage(MdxJsxElementFields element, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation page) {
        FluidImageOptions options = FluidImageOptions.read(element);
        String template = options.showTooltip() ? templates.create(GuideSiteFluidHtml.tooltip(options.fluid())) : null;
        return GuideSiteFluidHtml.image(
            options,
            icons,
            template,
            resolveHref(
                options.fluid()
                    .id(),
                page),
            resolveTargets(
                options.fluid()
                    .id(),
                page));
    }

    public String renderIcon(MdxJsxElementFields element, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation page) {
        FluidStackSpec spec = FluidTagAttributes.readFluid(element);
        FluidImageOptions options = new FluidImageOptions(
            spec,
            1,
            FluidTagAttributes.showTooltip(element),
            true,
            null,
            null,
            null,
            null);
        String template = options.showTooltip() ? templates.create(GuideSiteFluidHtml.tooltip(spec)) : null;
        return GuideSiteFluidHtml
            .image(options, icons, template, resolveHref(spec.id(), page), resolveTargets(spec.id(), page));
    }

    public String resolveHref(String fluid, @Nullable ResourceLocation currentPage) {
        var targets = targets(fluid);
        return targets.size() != 1 ? "" : href(targets.getFirst(), currentPage);
    }

    public List<Map<String, String>> resolveTargets(String fluid, @Nullable ResourceLocation currentPage) {
        return choices(targets(fluid), currentPage);
    }

    private List<Map<String, String>> choices(List<PageAnchor> targets, @Nullable ResourceLocation page) {
        return targets.stream()
            .filter(target -> !href(target, page).isEmpty())
            .map(target -> {
                var parsed = pages.get(target.pageId());
                var navigation = parsed.getFrontmatter()
                    .navigationEntry();
                String title = navigation == null ? null : navigation.title();
                if (title == null || title.isBlank()) title = target.pageId()
                    .toString();
                if (target.anchor() != null && !target.anchor()
                    .isEmpty()) title += " #" + target.anchor();
                return Map.of("href", href(target, page), "title", title);
            })
            .toList();
    }

    private List<PageAnchor> targets(String fluid) {
        if (index == null) {
            var ordered = new ArrayList<>(pages.values());
            ordered.sort(
                Comparator.comparing(
                    page -> page.getId()
                        .toString()));
            index = new FluidIndex();
            index.rebuild(ordered);
        }
        return index.get(fluid)
            .stream()
            .filter(target -> pages.containsKey(target.pageId()))
            .distinct()
            .toList();
    }

    private String href(PageAnchor target, @Nullable ResourceLocation page) {
        return target.anchor() == null && target.pageId()
            .equals(page) ? "" : GuideSiteHrefResolver.resolvePageAnchor(page, target);
    }

    private List<PageAnchor> explicitTarget(String value, @Nullable ResourceLocation page) {
        if (page == null) return List.of();
        int hash = value.indexOf('#');
        String path = hash < 0 ? value : value.substring(0, hash);
        String anchor = hash < 0 ? null : value.substring(hash + 1);
        ResourceLocation target = path.isEmpty() ? page : IdUtils.resolveLink(path, page);
        return pages.containsKey(target) ? List.of(new PageAnchor(target, anchor)) : List.of();
    }
}
