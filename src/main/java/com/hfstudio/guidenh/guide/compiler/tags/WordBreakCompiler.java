package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.layout.flow.LineBuilder;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class WordBreakCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("wbr");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        // Kept as text so every export path carries it; browsers treat U+200B as the same break opportunity.
        parent.append(LytFlowText.of(String.valueOf(LineBuilder.WORD_BREAK_OPPORTUNITY)));
    }
}
