package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class GuideSiteTemplateRegistry {

    private int counter = 1;
    private final List<String> rendered = new ArrayList<>();
    private final Map<String, String> idsByHtml = new LinkedHashMap<>();
    private final Map<String, String> renderedBySemanticKey;

    public GuideSiteTemplateRegistry() {
        this(new LinkedHashMap<>());
    }

    public GuideSiteTemplateRegistry(Map<String, String> renderedBySemanticKey) {
        this.renderedBySemanticKey = renderedBySemanticKey != null ? renderedBySemanticKey : new LinkedHashMap<>();
    }

    public String getOrComputeRendered(String semanticKey, Supplier<String> renderer) {
        if (semanticKey == null || semanticKey.isEmpty() || renderer == null) {
            return renderer != null ? renderer.get() : "";
        }
        if (renderedBySemanticKey.containsKey(semanticKey)) {
            return renderedBySemanticKey.get(semanticKey);
        }
        String html = renderer.get();
        renderedBySemanticKey.put(semanticKey, html != null ? html : "");
        return html != null ? html : "";
    }

    public String create(String html) {
        String existing = idsByHtml.get(html);
        if (existing != null) {
            return existing;
        }

        String id = "tmpl-" + counter++;
        idsByHtml.put(html, id);
        rendered.add("<template id=\"" + id + "\">" + html + "</template>");
        return id;
    }

    public List<String> renderAll() {
        return rendered;
    }
}
