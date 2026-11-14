package com.hfstudio.guidenh.guide.internal.markdown;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytFileTree;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeEntry;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeIcon;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeIconKind;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeModel;
import com.hfstudio.guidenh.guide.layout.flow.LineBuilder;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Turns a textual file tree string into a {@link LytFileTree} block. Each entry payload is
 * re-parsed as inline markdown so that authors can use the full set of GuideNH inline tags such
 * as {@code <ItemImage/>}, {@code <a/>} or {@code <Color>} on individual rows. Optional per-row
 * icon directives are resolved here as well.
 */
public class FileTreeCompiler {

    private FileTreeCompiler() {}

    public static LytFileTree compile(PageCompiler compiler, String source) {
        FileTreeModel model = FileTreeParser.parse(source);
        LytFileTree tree = new LytFileTree();
        tree.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        tree.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        for (FileTreeEntry entry : model.entries()) {
            LytParagraph payload = new LytParagraph();
            payload.setMarginTop(0);
            payload.setMarginBottom(0);
            String payloadSource = entry.payloadSource();
            if (payloadSource != null && !payloadSource.isEmpty()) {
                compiler.compileInlineMarkdown(payloadSource, payload);
                addPathBreaks(payload.getContent());
            }

            LytBlock iconBlock = entry.icon() != null ? buildIconBlock(compiler, entry.icon()) : null;
            tree.appendRow(entry.slots(), iconBlock, payload);
        }
        return tree;
    }

    private static void addPathBreaks(Iterable<LytFlowContent> content) {
        for (var node : content) {
            if (node instanceof LytFlowSpan span) {
                addPathBreaks(span.getChildren());
            } else if (node instanceof LytFlowText text) {
                String source = text.getText();
                StringBuilder wrapped = new StringBuilder();
                for (int i = 0; i < source.length(); i++) {
                    char character = source.charAt(i);
                    wrapped.append(character);
                    if ("/\\_-".indexOf(character) >= 0 && i + 1 < source.length()
                        && source.charAt(i + 1) != LineBuilder.WORD_BREAK_OPPORTUNITY) {
                        wrapped.append(LineBuilder.WORD_BREAK_OPPORTUNITY);
                    }
                }
                text.setText(wrapped.toString());
            }
        }
    }

    private static LytBlock buildIconBlock(PageCompiler compiler, FileTreeIcon icon) {
        FileTreeIconKind kind = icon.kind();
        String value = icon.value();
        switch (kind) {
            case TEXT -> {
                LytParagraph paragraph = new LytParagraph();
                paragraph.setMarginTop(0);
                paragraph.setMarginBottom(0);
                paragraph.appendText(value);
                return paragraph;
            }
            case PNG -> {
                LytImage image = new LytImage();
                try {
                    var imageId = IdUtils.resolveLink(value, compiler.getPageId());
                    var imageContent = compiler.loadAsset(imageId);
                    if (imageContent == null) {
                        GuideDebugLog.warn("[GuideNH] [FileTreeCompiler] File tree iconPng not found: {}", value);
                        image.setTitle("Missing image: " + value);
                    }
                    image.setImage(imageId, imageContent);
                } catch (IllegalArgumentException e) {
                    GuideDebugLog.warn(
                        "[GuideNH] [FileTreeCompiler] File tree iconPng has invalid id '{}': {}",
                        value,
                        e.getMessage());
                    image.setTitle("Invalid image: " + value);
                }
                return image;
            }
            case ITEM -> {
                var stack = GuideDisplayItemStacks.resolveItemStack(
                    value,
                    compiler.getPageId()
                        .getResourceDomain());
                if (stack == null) {
                    GuideDebugLog
                        .warn("[GuideNH] [FileTreeCompiler] File tree iconItem could not be resolved: {}", value);
                    LytParagraph fallback = new LytParagraph();
                    fallback.setMarginTop(0);
                    fallback.setMarginBottom(0);
                    fallback.appendText("?");
                    return fallback;
                }
                LytItemImage image = new LytItemImage(stack);
                image.setInline(false);
                return image;
            }
            default -> {
                return null;
            }
        }
    }
}
