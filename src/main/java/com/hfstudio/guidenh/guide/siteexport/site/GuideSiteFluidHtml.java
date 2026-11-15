package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteItemHtml.escapeHtml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.compiler.tags.FluidImageOptions;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;

public class GuideSiteFluidHtml {

    private static final Gson GSON = new Gson();

    protected GuideSiteFluidHtml() {}

    public static Map<String, Object> exportedData(FluidStackSpec spec, GuideSiteFluidIconResolver resolver) {
        var data = new LinkedHashMap<String, Object>();
        data.put("id", spec.id());
        data.put("displayName", FluidStacks.displayName(FluidStacks.resolve(spec)));
        data.put("iconSrc", resolver.exportFluidIcon(spec));
        if (spec.amount() != null) data.put("amount", spec.amount());
        if (spec.nbt() != null) data.put("nbt", spec.nbt());
        return Map.copyOf(data);
    }

    public static String targetAttributes(List<Map<String, String>> targets) {
        return targets.isEmpty() ? "" : " data-guide-fluid-targets=\"" + escapeHtml(GSON.toJson(targets)) + "\"";
    }

    public static String pageIcon(GuidePageIcon icon, GuideSiteFluidIconResolver resolver) {
        var specs = icon.fluidSpecs();
        var frames = specs.stream()
            .map(
                spec -> Map.of(
                    "id",
                    spec.id(),
                    "src",
                    resolver.exportFluidIcon(spec),
                    "name",
                    FluidStacks.displayName(FluidStacks.resolve(spec))))
            .toList();
        String content = icon(specs.getFirst(), resolver, 1, false);
        return frames.size() == 1 ? content
            : "<span class=\"guide-fluid-cycle\" data-fluid-cycle=\"" + escapeHtml(GSON.toJson(frames))
                + "\">"
                + content
                + "</span>";
    }

    public static String tooltip(FluidStackSpec spec) {
        String name = FluidStacks.displayName(FluidStacks.resolve(spec));
        return "<div class=\"guide-fluid-tooltip\"><div>" + escapeHtml(name)
            + "</div>"
            + (spec.amount() == null ? ""
                : "<div>" + escapeHtml(NumberFormatUtil.formatNumber(spec.amount())) + " L</div>")
            + "<div class=\"guide-tooltip-muted\">"
            + escapeHtml(spec.id())
            + "</div></div>";
    }

    public static String icon(FluidStackSpec fluid, GuideSiteFluidIconResolver resolver, float scale,
        boolean showAmount) {
        var stack = FluidStacks.resolve(fluid);
        String name = FluidStacks.displayName(stack);
        String src = resolver.exportFluidIcon(fluid);
        int size = Math.max(1, Math.round(GuideSiteItemHtml.BASE_ICON_PX * scale));
        String style = scale == 1 ? "" : " style=\"width:" + size + "px;height:" + size + "px\"";
        String html = src.isEmpty()
            ? "<span class=\"fluid-icon item-icon item-icon-fallback\"" + style
                + ">"
                + escapeHtml(fluid.id())
                + "</span>"
            : "<img class=\"fluid-icon item-icon\" src=\"" + escapeHtml(src)
                + "\" alt=\""
                + escapeHtml(name)
                + "\" data-fluid-id=\""
                + escapeHtml(fluid.id())
                + "\" width=\""
                + size
                + "\" height=\""
                + size
                + "\""
                + style
                + " decoding=\"async\">";
        if (showAmount && fluid.amount() != null) {
            html = "<span class=\"fluid-icon-stack\">" + html
                + "<span class=\"fluid-amount\">"
                + escapeHtml(NumberFormatUtil.formatNumberCompact(fluid.amount()))
                + "</span></span>";
        }
        return html;
    }

    public static String image(FluidImageOptions options, GuideSiteFluidIconResolver resolver,
        @Nullable String templateId, @Nullable String href) {
        return image(options, resolver, templateId, href, List.of());
    }

    public static String image(FluidImageOptions options, GuideSiteFluidIconResolver resolver,
        @Nullable String templateId, @Nullable String href, List<Map<String, String>> targets) {
        StringBuilder html = new StringBuilder("<span class=\"guide-inline-item guide-inline-fluid");
        if (templateId != null && !templateId.isEmpty()) html.append(" guide-tooltip");
        html.append("\" data-fluid-id=\"")
            .append(
                escapeHtml(
                    options.fluid()
                        .id()))
            .append("\"");
        if (templateId != null && !templateId.isEmpty()) html.append(" data-template=\"")
            .append(escapeHtml(templateId))
            .append("\"");
        if (href != null && !href.isEmpty()) html.append(" data-guide-fluid-href=\"")
            .append(escapeHtml(href))
            .append("\"");
        html.append(targetAttributes(targets));
        if (options.yOffset() != null) html.append(" style=\"position:relative;top:")
            .append(Math.round(options.yOffset() * options.scale()))
            .append("px\"");
        html.append('>');
        if ("left".equals(options.labelPosition())) html.append(label(options));
        if (options.showIcon()) html.append(icon(options.fluid(), resolver, options.scale(), true));
        if (options.labelPosition() != null && !"left".equals(options.labelPosition())) html.append(label(options));
        return html.append("</span>")
            .toString();
    }

    private static String label(FluidImageOptions options) {
        String text = FluidStacks.displayName(FluidStacks.resolve(options.fluid()));
        String style = options.labelYOffset() == null ? ""
            : " style=\"position:relative;top:" + Math.round(options.labelYOffset() * options.scale()) + "px\"";
        return "<span class=\"guide-item-label guide-fluid-label\"" + style
            + ">"
            + GuideSiteLabelFormatter.render(text, options.format())
            + "</span>";
    }
}
