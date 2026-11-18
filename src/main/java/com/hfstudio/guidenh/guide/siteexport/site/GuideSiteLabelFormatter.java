package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public class GuideSiteLabelFormatter {

    private static final List<Marker> MARKERS = List.of(
        new Marker("~~", "s"),
        new Marker("**", "strong"),
        new Marker("__", "u"),
        new Marker("++", "u"),
        new Marker("^^", "span style=\"text-decoration:underline wavy\""),
        new Marker("::", "span style=\"text-decoration:underline dotted\""),
        new Marker("*", "em"),
        new Marker("_", "em"));

    private record Marker(String token, String tag) {}

    protected GuideSiteLabelFormatter() {}

    public static String render(String name, @Nullable String format) {
        if (format == null) return "<em>" + GuideSiteItemHtml.escapeHtml(name) + "</em>";
        var open = new StringBuilder();
        var close = new StringBuilder();
        String template = format;
        boolean changed;
        do {
            changed = false;
            for (var marker : MARKERS) {
                int length = marker.token()
                    .length();
                if (template.length() <= length * 2 || !template.startsWith(marker.token())
                    || !template.endsWith(marker.token())) continue;
                open.append('<')
                    .append(marker.tag())
                    .append('>');
                close.insert(
                    0,
                    "</" + marker.tag()
                        .split(" ", 2)[0] + ">");
                template = template.substring(length, template.length() - length);
                changed = true;
                break;
            }
        } while (changed);
        String label = template.replace("%s", name);
        return open + GuideSiteItemHtml.escapeHtml(label) + close;
    }
}
