package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTable;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableCell;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableRow;
import com.hfstudio.guidenh.libs.mdast.gfmstrikethrough.MdAstDelete;
import com.hfstudio.guidenh.libs.mdast.guidemark.MdAstMark;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstDottedUnderline;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstUnderline;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstWavyUnderline;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBlockquote;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBreak;
import com.hfstudio.guidenh.libs.mdast.model.MdAstCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstDefinition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstEmphasis;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHTML;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHeading;
import com.hfstudio.guidenh.libs.mdast.model.MdAstImage;
import com.hfstudio.guidenh.libs.mdast.model.MdAstImageReference;
import com.hfstudio.guidenh.libs.mdast.model.MdAstInlineCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLink;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLinkReference;
import com.hfstudio.guidenh.libs.mdast.model.MdAstList;
import com.hfstudio.guidenh.libs.mdast.model.MdAstListItem;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStrong;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;
import com.hfstudio.guidenh.libs.mdast.model.MdAstThematicBreak;
import com.hfstudio.guidenh.libs.micromark.extensions.gfm.Align;

public final class MdAstToMdxConverter {

    /**
     * Matches a kramdown-style attribute line ({@code {: ... }}) that the GFM
     * table parser may swallow as an extra table row.
     */
    private static final Pattern TABLE_ATTRIBUTE_LINE = Pattern.compile("^\\{:\\s*(.+?)\\s*}$");

    /**
     * Matches an inline attribute block {@code {key=value key="value" key='value'}}
     * attached to a Markdown image or link, e.g. {@code {align=center}}.
     */
    private static final Pattern INLINE_ATTR_BLOCK = Pattern.compile("^\\{(.+)\\}$");

    /**
     * Matches a single attribute key=value pair inside {@code {...}}.
     * Supports unquoted values, double-quoted, and single-quoted values.
     */
    private static final Pattern INLINE_ATTR_PAIR = Pattern.compile(
        "(\\w+)=\"([^\"]*)\"|(\\w+)='([^']*)'|(\\w+)=(\\w+)");

    private MdAstToMdxConverter() {}

    /**
     * @param definitions pre-collected link/image reference definitions (from GuideMarkdownDefinitions.collect())
     */
    public static void convert(MdAstRoot root, Map<String, MdAstDefinition> definitions) {
        convertParent(root, definitions);
    }

    private static void convertParent(MdAstParent<?> parent, Map<String, MdAstDefinition> definitions) {
        // First: depth-first recursion into all MdAstParent children, using a snapshot
        // to safely handle concurrent modification.
        List<?> children = parent.children();
        for (Object child : new ArrayList<>(children)) {
            if (child instanceof MdAstParent<?>childParent) {
                convertParent(childParent, definitions);
            }
        }

        // Then convert the current level's children in-place.
        if (isPhrasingParent(parent)) {
            convertPhrasingChildren(parent.children(), definitions);
        } else {
            convertFlowChildren(castAnyChildren(parent.children()), definitions);
        }
    }

    private static boolean isPhrasingParent(MdAstParent<?> parent) {
        if (parent instanceof MdAstParagraph || parent instanceof MdxJsxTextElement || parent instanceof GfmTableCell) {
            return true;
        }
        // New MdxJsxFlowElement containers that hold phrasing/inline children
        if (parent instanceof MdxJsxFlowElement el) {
            String name = el.name();
            return name != null && PHRASING_CONTAINER_NAMES.contains(name);
        }
        String type = parent.type();
        return "link".equals(type) || "strong".equals(type)
            || "emphasis".equals(type)
            || "delete".equals(type)
            || "heading".equals(type);
    }

    // Containers whose children are inline/phrasing content only
    private static final Set<String> PHRASING_CONTAINER_NAMES = new HashSet<>(
        Arrays.asList(
            "p",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "td",
            "th",
            "summary",
            "a",
            "strong",
            "em",
            "del",
            "u",
            "wavy",
            "dotted",
            "mark",
            "code",
            "span"));

    @SuppressWarnings("unchecked")
    private static List<MdAstAnyContent> castAnyChildren(List<?> children) {
        return (List<MdAstAnyContent>) children;
    }

    // Phrasing (inline) children conversion — also handles block nodes that
    // may appear inside phrasing containers (e.g. MdAstParagraph inside <td>).

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void convertPhrasingChildren(List<?> children, Map<String, MdAstDefinition> definitions) {
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            Object replacement = null;

            // Inline phrasing types → MdxJsxTextElement
            if (child instanceof MdAstStrong) {
                replacement = createText("strong", ((MdAstStrong) child).children());
            } else if (child instanceof MdAstEmphasis) {
                replacement = createText("em", ((MdAstEmphasis) child).children());
            } else if (child instanceof MdAstDelete) {
                replacement = createText("del", ((MdAstDelete) child).children());
            } else if (child instanceof MdAstUnderline) {
                replacement = createText("u", ((MdAstUnderline) child).children());
            } else if (child instanceof MdAstWavyUnderline) {
                replacement = createText("wavy", ((MdAstWavyUnderline) child).children());
            } else if (child instanceof MdAstDottedUnderline) {
                replacement = createText("dotted", ((MdAstDottedUnderline) child).children());
            } else if (child instanceof MdAstMark) {
                replacement = createText("mark", ((MdAstMark) child).children());
            } else if (child instanceof MdAstLink link) {
                MdxJsxTextElement el = createText("a", link.children());
                el.addAttribute("href", link.url());
                replacement = el;
            } else if (child instanceof MdAstLinkReference ref) {
                MdxJsxTextElement el = createText("a", ref.children());
                MdAstDefinition def = definitions.get(ref.identifier());
                el.addAttribute("href", def != null ? def.url() : "");
                replacement = el;
            } else if (child instanceof MdAstImage image) {
                MdxJsxTextElement el = createText("img", new ArrayList<>());
                el.addAttribute("src", image.url());
                if (image.alt != null) {
                    el.addAttribute("alt", image.alt);
                }
                if (image.title != null) {
                    el.addAttribute("title", image.title);
                }
                consumeTrailingAttributes(children, i, el);
                replacement = el;
            } else if (child instanceof MdAstImageReference ref) {
                MdxJsxTextElement el = createText("img", new ArrayList<>());
                MdAstDefinition def = definitions.get(ref.identifier());
                if (def != null) {
                    el.addAttribute("src", def.url());
                    if (def.title != null) {
                        el.addAttribute("title", def.title);
                    }
                } else {
                    el.addAttribute("src", "");
                }
                if (ref.alt != null) {
                    el.addAttribute("alt", ref.alt);
                }
                consumeTrailingAttributes(children, i, el);
                replacement = el;
            } else if (child instanceof MdAstInlineCode code) {
                MdxJsxTextElement el = createText("code", new ArrayList<>());
                MdAstText text = new MdAstText();
                text.setValue(code.value);
                addChildRaw(el, text);
                replacement = el;
            } else if (child instanceof MdAstHTML html) {
                MdxJsxTextElement el = createText("span", new ArrayList<>());
                MdAstText text = new MdAstText();
                text.setValue(html.value);
                addChildRaw(el, text);
                replacement = el;
            } else if (child instanceof MdAstBreak) {
                replacement = createText("br", new ArrayList<>());
            }
            // Flow block types that can appear inside phrasing containers
            else if (child instanceof MdAstParagraph p) {
                replacement = createFlow("p", p.children());
            }

            if (replacement != null) {
                ((List) children).set(i, replacement);
                if (replacement instanceof MdxJsxTextElement) {
                    convertParent((MdxJsxTextElement) replacement, definitions);
                } else if (replacement instanceof MdxJsxFlowElement) {
                    convertParent((MdxJsxFlowElement) replacement, definitions);
                }
            }
            // MdAstText, MdxJsxTextElement, MdxJsxFlowElement: silently pass through
        }
    }

    // Flow (block) children conversion

    private static void convertFlowChildren(List<MdAstAnyContent> children, Map<String, MdAstDefinition> definitions) {
        for (int i = 0; i < children.size(); i++) {
            MdAstAnyContent child = children.get(i);
            MdxJsxFlowElement replacement = null;

            if (child instanceof MdAstParagraph p) {
                String kramdownMeta = extractKramdownMeta(p);
                if (kramdownMeta != null) {
                    replacement = new MdxJsxFlowElement();
                    replacement.setName("table-meta");
                    replacement.addAttribute("content", kramdownMeta);
                } else if (isSoloAlignedImageParagraph(p)) {
                    // R4-31: standalone paragraph with only {align=...} image →
                    // promote to block-level <img> so it goes through
                    // ImageCompiler.compileBlockContext and produces a proper
                    // LytAlignedBlock (instead of inline LytFlowInlineBlock).
                    MdxJsxTextElement imgEl = (MdxJsxTextElement) p.children().get(0);
                    MdxJsxFlowElement flowImg = new MdxJsxFlowElement();
                    flowImg.setName("img");
                    flowImg.attributes().addAll(imgEl.attributes());
                    replacement = flowImg;
                } else {
                    replacement = createFlow("p", p.children());
                }
            } else if (child instanceof MdAstHeading h) {
                MdxJsxFlowElement el = createFlow("h" + h.depth, h.children());
                el.addAttribute("depth", h.depth);
                replacement = el;
            } else if (child instanceof MdAstList list) {
                String name = list.ordered ? "ol" : "ul";
                MdxJsxFlowElement el = createFlow(name, list.children());
                if (list.ordered && list.start != 1) {
                    el.addAttribute("start", list.start);
                }
                replacement = el;
            } else if (child instanceof MdAstListItem item) {
                replacement = createFlow("li", item.children());
            } else if (child instanceof MdAstCode code) {
                MdxJsxFlowElement el = createFlow("pre", new ArrayList<>());
                if (code.lang != null) {
                    el.addAttribute("lang", code.lang);
                }
                if (code.meta != null) {
                    el.addAttribute("meta", code.meta);
                }
                MdAstText text = new MdAstText();
                text.setValue(code.value);
                addChildRaw(el, text);
                replacement = el;
            } else if (child instanceof MdAstBlockquote bq) {
                replacement = createFlow("blockquote", bq.children());
            } else if (child instanceof GfmTable table) {
                MdxJsxFlowElement el = createFlow("table", table.children());
                String alignStr = serializeAlign(table.align);
                if (alignStr != null) {
                    el.addAttribute("align", alignStr);
                }
                replacement = el;
            } else if (child instanceof GfmTableRow row) {
                // Detect kramdown attribute line ({: ...}) swallowed by GFM table parser
                String rowText = getRowText(row);
                if (rowText != null && TABLE_ATTRIBUTE_LINE.matcher(rowText).matches()) {
                    replacement = new MdxJsxFlowElement();
                    replacement.setName("table-meta");
                    replacement.addAttribute("content", rowText);
                } else {
                    replacement = createFlow("tr", row.children());
                }
            } else if (child instanceof GfmTableCell cell) {
                replacement = createFlow("td", cell.children());
            } else if (child instanceof MdAstThematicBreak) {
                replacement = createFlow("hr", new ArrayList<>());
            } else if (child instanceof MdAstDefinition def) {
                MdxJsxFlowElement el = createFlow("definition", new ArrayList<>());
                if (def.identifier != null) {
                    el.addAttribute("identifier", def.identifier);
                }
                if (def.url != null) {
                    el.addAttribute("url", def.url);
                }
                if (def.title != null) {
                    el.addAttribute("title", def.title);
                }
                replacement = el;
            } else if (child instanceof MdAstYamlFrontmatter) {
                // Remove from children
                children.remove(i);
                i--;
                continue;
            } else if (child instanceof MdAstHTML html) {
                MdxJsxFlowElement el = createFlow("div", new ArrayList<>());
                MdAstText text = new MdAstText();
                text.setValue(html.value);
                addChildRaw(el, text);
                replacement = el;
            }

            if (replacement != null) {
                children.set(i, replacement);
                convertParent(replacement, definitions);
            }
            // Already-converted types (MdxJsxFlowElement, MdxJsxTextElement) and
            // leaf nodes (MdAstText) are silently passed through.
        }
    }

    // Factory helpers

    /**
     * Creates a flow element with the given tag name and children.
     * <p>
     * Uses raw-type list access to bypass the generic type check so that phrasing
     * content (e.g. {@link MdxJsxTextElement}, {@link MdAstText}) can be placed
     * inside flow elements where they are semantically valid (e.g. text inside
     * {@code 
     * 
    <p>
     * }).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static MdxJsxFlowElement createFlow(String name, List<? extends MdAstAnyContent> children) {
        MdxJsxFlowElement element = new MdxJsxFlowElement();
        element.setName(name);
        List rawChildren = element.children();
        rawChildren.addAll(children);
        return element;
    }

    /**
     * Creates a text (inline) element with the given tag name and children.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static MdxJsxTextElement createText(String name, List<? extends MdAstPhrasingContent> children) {
        MdxJsxTextElement element = new MdxJsxTextElement();
        element.setName(name);
        List rawChildren = element.children();
        rawChildren.addAll(children);
        return element;
    }

    /**
     * Adds an {@link MdAstNode} to a flow element's children list via raw-type
     * access, bypassing the generic type check. This is needed when the child
     * is phrasing content (text, inline elements) that are semantically valid
     * inside the element (e.g. a {@link MdAstText} inside a {@code 
     * 
     * 
    
    <pre>
     * } tag).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addChildRaw(MdxJsxFlowElement element, MdAstNode node) {
        ((List) element.children()).add(node);
    }

    /**
     * Adds an {@link MdAstNode} to a text element's children list via raw-type
     * access, bypassing the generic type check. This is needed when the child
     * is a non-phrasing node that is semantically valid inline (e.g. a
     * {@link MdAstText} inside {@code <code>}).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addChildRaw(MdxJsxTextElement element, MdAstNode node) {
        ((List) element.children()).add(node);
    }

    /**
     * Serializes the GfmTable align list to a comma-separated lowercase string,
     * e.g. {@code "left,center,right"}. Returns {@code null} when the list is
     * null or empty.
     */
    @Nullable
    private static String serializeAlign(@Nullable List<Align> aligns) {
        if (aligns == null || aligns.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < aligns.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            switch (aligns.get(i)) {
                case LEFT -> sb.append("left");
                case CENTER -> sb.append("center");
                case RIGHT -> sb.append("right");
                case NONE -> sb.append("none");
            }
        }
        return sb.toString();
    }

    /**
     * Checks if the child at {@code index + 1} is an {@link MdAstText} containing
     * an inline attribute block ({@code {key=value ...}}). If so, parses the
     * attributes and adds them to the given {@link MdxJsxTextElement}, then
     * removes the consumed text node from the children list.
     * <p>
     * This bridges the gap between Markdown image syntax {@code ![alt](src){align=center}}
     * and the JSX attribute representation expected by tag compilers (R4-31).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void consumeTrailingAttributes(List<?> children, int index, MdxJsxTextElement el) {
        int nextIdx = index + 1;
        if (nextIdx >= children.size()) return;
        Object next = children.get(nextIdx);
        if (!(next instanceof MdAstText text)) return;
        String trimmed = text.value.trim();
        java.util.regex.Matcher blockMatcher = INLINE_ATTR_BLOCK.matcher(trimmed);
        if (!blockMatcher.matches()) return;

        String inner = blockMatcher.group(1).trim();
        java.util.regex.Matcher pairMatcher = INLINE_ATTR_PAIR.matcher(inner);
        boolean found = false;
        while (pairMatcher.find()) {
            found = true;
            // Group 1/2: double-quoted value; 3/4: single-quoted; 5/6: unquoted
            String name = pairMatcher.group(1) != null ? pairMatcher.group(1)
                : pairMatcher.group(3) != null ? pairMatcher.group(3)
                : pairMatcher.group(5);
            String value = pairMatcher.group(2) != null ? pairMatcher.group(2)
                : pairMatcher.group(4) != null ? pairMatcher.group(4)
                : pairMatcher.group(6);
            if (name != null && value != null) {
                el.addAttribute(name, value);
            }
        }
        if (found) {
            children.remove(nextIdx);
        }
    }

    /**
     * Checks if a paragraph has been converted to contain only a single
     * {@code <img>} element with an {@code align} attribute. Such paragraphs
     * come from standalone Markdown image syntax with kramdown-style alignment,
     * e.g. {@code ![alt](src){align=center}} on its own line.
     * <p>
     * These should be promoted to block-level {@code <img>} so that
     * {@link com.hfstudio.guidenh.guide.compiler.tags.ImageCompiler} can
     * produce a block-level {@code LytAlignedBlock} at compile time, avoiding
     * the inline {@code LytFlowInlineBlock} wrapper that cannot position
     * block-level alignment nodes (R4-31).
     */
    private static boolean isSoloAlignedImageParagraph(MdAstParagraph p) {
        if (p.children().size() != 1) return false;
        Object sole = p.children().get(0);
        if (!(sole instanceof MdxJsxTextElement imgEl)) return false;
        if (!"img".equals(imgEl.name())) return false;
        return imgEl.getAttribute("align") != null;
    }

    /**
     * Extracts the expression from a kramdown-style attribute paragraph
     * ({@code {: ...}}), or returns null if not a kramdown meta line.
     */
    @Nullable
    private static String extractKramdownMeta(MdAstParagraph p) {
        if (p.children()
            .size() != 1) return null;
        if (!(p.children()
            .getFirst() instanceof MdAstText t)) return null;
        String v = t.value.trim();
        if (v.startsWith("{:") && v.endsWith("}")) return v;
        return null;
    }

    /**
     * Concatenates the text content of all cells in a GfmTableRow, separated by
     * spaces. Returns null if the resulting text is empty (after trim).
     * <p>
     * Iterates as {@link Object} and uses instanceof to handle both
     * {@link GfmTableCell} (original) and {@link MdxJsxFlowElement} (post-conversion
     * {@code <td>}) child types, avoiding ClassCastException when the row's
     * children list contains mixed types.
     */
    @Nullable
    private static String getRowText(GfmTableRow row) {
        StringBuilder sb = new StringBuilder();
        for (Object cell : row.children()) {
            String cellText = null;
            if (cell instanceof GfmTableCell tableCell) {
                cellText = tableCell.toText();
            } else if (cell instanceof MdxJsxFlowElement flowElem && "td".equals(flowElem.name())) {
                cellText = flowElem.toText();
            } else if (cell instanceof MdAstNode node) {
                cellText = node.toText();
            }
            if (cellText != null && !cellText.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(cellText);
            }
        }
        String text = sb.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
