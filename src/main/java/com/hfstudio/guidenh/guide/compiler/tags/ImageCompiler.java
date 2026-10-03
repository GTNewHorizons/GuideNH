package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.document.block.ContentAlign;
import com.hfstudio.guidenh.guide.document.block.LytAlignedBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytImageBlock;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

public class ImageCompiler implements TagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("img");
    }

    @Override
    public void compileFlowContext(PageCompiler compiler, LytFlowParent parent, MdxJsxTextElement el) {
        LytImageBlock block = buildBlock(compiler, el);

        var inlineBlock = new LytFlowInlineBlock();
        inlineBlock.setBlock(block);
        parent.append(inlineBlock);
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        LytImageBlock block = buildBlock(compiler, el);

        String alignAttr = el.getAttributeString("align", null);
        LytBlock result = block;
        if (alignAttr != null) {
            ContentAlign align = ContentAlign.fromString(alignAttr);
            if (align != ContentAlign.LEFT) {
                result = new LytAlignedBlock(result, align);
            }
        }

        parent.append(PageCompiler.wrapFloatAwareIfNeeded(result));
    }

    private static LytImageBlock buildBlock(PageCompiler compiler, MdxJsxElementFields el) {
        LytImageBlock block = new LytImageBlock();
        block.setStyleClass("Img");

        String src = el.getAttributeString("src", "");
        if (!src.isEmpty()) {
            try {
                var imageId = IdUtils.resolveLink(src, compiler.getPageId());
                block.setSrc(imageId.toString());
            } catch (IllegalArgumentException e) {
                GuideDebugLog.error("[GuideNH] [ImageCompiler] Invalid image id: {}", src);
                block.setTitle("Invalid image URL: " + src);
            }
        }

        String alt = el.getAttributeString("alt", "");
        String title = el.getAttributeString("title", "");
        if (!alt.isEmpty()) block.setAlt(alt);
        if (!title.isEmpty()) block.setTitle(title);

        String alignAttr = el.getAttributeString("align", null);
        if (alignAttr != null) {
            block.setAlign(alignAttr);
        }

        block.setStyle(LytParagraph.PLACEHOLDER_STYLE);
        block.appendText("[Image]");
        return block;
    }
}
