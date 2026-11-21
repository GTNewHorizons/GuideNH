package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.List;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.internal.binding.BindingPresentation;
import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;
import com.hfstudio.guidenh.guide.internal.binding.BindingSpec;
import com.hfstudio.guidenh.integration.api.binding.BindingDisplay;

public class GuideSiteBindingHtml {

    protected GuideSiteBindingHtml() {}

    public static String link(BindingSpec spec, BindingDisplay display, List<GuideAnchor> targets,
        @Nullable ResourceLocation currentPageId) {
        String label = BindingPresentation.restricted(display) ? BindingRuntime.title(display)
            : spec.text() != null ? spec.text() : spec.title() != null ? spec.title() : BindingRuntime.title(display);
        String text = GuideSiteHtml.escapeHtml(label);
        if (BindingPresentation.restricted(display)) return "<span class=\"guide-binding-link\">" + text + "</span>";
        String tooltip = spec.showTooltip()
            ? " title=\"" + GuideSiteHtml.escapeAttribute(String.join("\n", display.tooltip())) + "\""
            : "";
        if (targets.isEmpty()) return "<span class=\"guide-binding-link\"" + tooltip
            + ">"
            + text
            + " <small>"
            + GuideSiteHtml.escapeHtml(StatCollector.translateToLocal("guidenh.binding.native_site"))
            + "</small></span>";
        if (targets.size() == 1) return anchor(targets.getFirst(), currentPageId, text, tooltip);
        var html = new StringBuilder(
            "<span class=\"guide-binding-targets\" tabindex=\"0\"><span" + tooltip
                + ">"
                + text
                + "</span><span class=\"guide-binding-target-list\">");
        for (var target : targets) html.append(
            anchor(
                target,
                currentPageId,
                GuideSiteHtml.escapeHtml(
                    target.page()
                        .toString()),
                ""));
        return html.append("</span></span>")
            .toString();
    }

    private static String anchor(GuideAnchor target, ResourceLocation currentPageId, String text, String tooltip) {
        String href = GuideSiteHrefResolver.resolvePageAnchor(currentPageId, target.guideId(), target.page());
        return "<a class=\"guide-binding-link\" href=\"" + GuideSiteHtml
            .escapeAttribute(href) + "\"" + tooltip + ">" + text + "</a>";
    }
}
