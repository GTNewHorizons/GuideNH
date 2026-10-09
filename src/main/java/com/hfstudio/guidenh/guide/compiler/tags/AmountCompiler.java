package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.units.AmountFormatter;
import com.hfstudio.guidenh.guide.units.AmountUnitRegistry;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class AmountCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Amount");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields element) {
        String value = element.getAttributeString("value", null);
        if (value == null || value.trim()
            .isEmpty()) {
            parent.appendError(compiler, "Missing value attribute", element);
            return;
        }
        String unit = AmountUnitRegistry.normalize(element.getAttributeString("unit", "none"));
        String format = element.getAttributeString("format", "default");
        Integer decimalPlaces = parseDecimalPlaces(compiler, parent, element);
        try {
            parent.append(LytFlowText.of(AmountFormatter.format(value, unit, format, decimalPlaces)));
        } catch (IllegalArgumentException exception) {
            parent.appendError(compiler, exception.getMessage(), element);
        }
    }

    private Integer parseDecimalPlaces(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields element) {
        String raw = element.getAttributeString("decimals", null);
        if (raw == null || raw.trim()
            .isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            parent.appendError(compiler, "decimals must be a non-negative integer", element);
            return null;
        }
    }
}
