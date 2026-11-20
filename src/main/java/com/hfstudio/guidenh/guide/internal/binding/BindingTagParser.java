package com.hfstudio.guidenh.guide.internal.binding;

import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BindingTagParser {

    protected BindingTagParser() {}

    public static BindingSpec parse(MdxJsxElementFields element) {
        String name = element.name();
        boolean quest = name.startsWith("Quest");
        String type = quest ? "betterquesting"
            : name.startsWith("Research") ? "thaumcraft" : element.getAttributeString("type", "");
        BindingKey key = BindingKey.parse(type, element.getAttributeString("id", ""));
        return new BindingSpec(
            key,
            element.getAttributeString("text", null),
            element.getAttributeString("title", null),
            quest ? "native" : element.getAttributeString("target", name.endsWith("Card") ? "native" : "auto"),
            bool(element, "showTooltip", true),
            bool(element, "showDesc", true),
            bool(element, "showIcon", true),
            bool(element, "showParents", false),
            Float.parseFloat(element.getAttributeString("scale", "1")));
    }

    private static boolean bool(MdxJsxElementFields element, String name, boolean fallback) {
        Boolean value = MdxAttrs.parseOptionalBoolean(element.getAttribute(name), name);
        return value == null ? fallback : value;
    }

}
