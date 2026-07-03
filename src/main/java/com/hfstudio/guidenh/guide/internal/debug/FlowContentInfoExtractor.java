package com.hfstudio.guidenh.guide.internal.debug;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytLatexBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;

/**
 * Extracts detailed debug information from flow content (inline elements).
 */
public class FlowContentInfoExtractor {

    public static void extract(LytFlowContent content, HoveredElementInfo info) {
        if (content == null) {
            return;
        }

        try {
            if (content instanceof LytFlowInlineBlock inlineBlock) {
                extractInlineBlockInfo(inlineBlock, info);
            } else if (content instanceof LytFlowLink link) {
                extractLinkInfo(link, info);
            } else if (content instanceof LytFlowText text) {
                extractTextInfo(text, info);
            } else if (content instanceof LytFlowSpan span) {
                extractSpanInfo(span, info);
            } else {
                info.addExtraInfo(
                    "Type: " + content.getClass()
                        .getName());
            }

            if (content.getStyleClass() != null) {
                info.addExtraInfo("Style Class: " + content.getStyleClass());
            }

            if (content.getNodeUid() != null) {
                info.addExtraInfo("UID: " + content.getNodeUid());
            }
        } catch (Exception e) {
            info.addExtraInfo("Error: " + e.getMessage());
        }
    }

    private static void extractInlineBlockInfo(LytFlowInlineBlock inlineBlock, HoveredElementInfo info) {
        var block = inlineBlock.getBlock();
        if (block == null) {
            info.addExtraInfo("Type: Inline Block (Empty)");
            return;
        }

        String blockType = block.getClass()
            .getName();
        info.addExtraInfo("Type: " + blockType);

        if (block instanceof LytItemImage itemImage) {
            extractItemImageInfo(itemImage, info, inlineBlock);
        } else if (block instanceof LytLatexBlock latex) {
            extractLatexInfo(latex, info);
        }
    }

    private static void extractItemImageInfo(LytItemImage itemImage, HoveredElementInfo info,
        LytFlowInlineBlock inlineBlock) {
        try {
            var stackField = LytItemImage.class.getDeclaredField("stack");
            stackField.setAccessible(true);
            ItemStack stack = (ItemStack) stackField.get(itemImage);

            if (stack != null) {
                info.addExtraInfo("Item: " + stack.getDisplayName());

                String itemId = Item.itemRegistry.getNameForObject(stack.getItem());
                if (itemId != null) {
                    if (stack.getItemDamage() != 0) {
                        itemId += ":" + stack.getItemDamage();
                    }
                    info.addExtraInfo("ID: " + itemId);
                }

                if (stack.stackSize != 1) {
                    info.addExtraInfo("Count: " + stack.stackSize);
                }

                if (stack.hasTagCompound()) {
                    info.addExtraInfo("Has NBT: Yes");
                }
            }
        } catch (Exception e) {
            info.addExtraInfo("Item: (Cannot access)");
        }

        info.addExtraInfo("Scale: " + itemImage.getScale());
        info.addExtraInfo("Show Icon: " + itemImage.isShowingIcon());
        info.addExtraInfo(
            "Alignment: " + inlineBlock.getAlignment()
                .name());
    }

    private static void extractLatexInfo(LytLatexBlock latex, HoveredElementInfo info) {
        try {
            var sourceField = LytLatexBlock.class.getDeclaredField("sourceText");
            sourceField.setAccessible(true);
            String source = (String) sourceField.get(latex);

            if (source != null && !source.isEmpty()) {
                String preview = source.length() > 50 ? source.substring(0, 47) + "..." : source;
                info.addExtraInfo("Formula: " + preview);
                info.addExtraInfo("Length: " + source.length() + " chars");
                info.addExtraInfo("Mode: Inline");
            }
        } catch (Exception e) {
            info.addExtraInfo("Formula: (Cannot access)");
        }
    }

    private static void extractLinkInfo(LytFlowLink link, HoveredElementInfo info) {
        info.addExtraInfo("Type: Link");

        try {
            var hrefField = LytFlowLink.class.getDeclaredField("href");
            hrefField.setAccessible(true);
            String href = (String) hrefField.get(link);

            if (href != null && !href.isEmpty()) {
                String preview = href.length() > 40 ? href.substring(0, 37) + "..." : href;
                info.addExtraInfo("Href: " + preview);

                if (href.startsWith("http://") || href.startsWith("https://")) {
                    info.addExtraInfo("Link Type: External");
                } else if (href.startsWith("#")) {
                    info.addExtraInfo("Link Type: Anchor");
                } else {
                    info.addExtraInfo("Link Type: Internal");
                }
            }
        } catch (Exception e) {
            // Silently ignore
        }

        int childCount = link.getChildren()
            .size();
        if (childCount > 0) {
            info.addExtraInfo("Children: " + childCount);
        }
    }

    private static void extractTextInfo(LytFlowText text, HoveredElementInfo info) {
        info.addExtraInfo("Type: Text");

        try {
            var textField = LytFlowText.class.getDeclaredField("text");
            textField.setAccessible(true);
            String content = (String) textField.get(text);

            if (content != null && !content.isEmpty()) {
                String preview = content.length() > 50 ? content.substring(0, 47) + "..." : content;
                info.addExtraInfo("Content: \"" + preview + "\"");
                info.addExtraInfo("Length: " + content.length() + " chars");

                if (content.contains("\n")) {
                    info.addExtraInfo("Multiline: Yes");
                }
            }
        } catch (Exception e) {
            info.addExtraInfo("Content: (Cannot access)");
        }
    }

    private static void extractSpanInfo(LytFlowSpan span, HoveredElementInfo info) {
        info.addExtraInfo("Type: Span");

        int childCount = span.getChildren()
            .size();
        if (childCount > 0) {
            info.addExtraInfo("Children: " + childCount);
        }

        var style = span.getStyle();
        if (false) {
            info.addExtraInfo("Has Custom Style: Yes");
        }
    }
}
