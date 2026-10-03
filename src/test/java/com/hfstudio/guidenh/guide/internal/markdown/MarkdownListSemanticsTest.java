package com.hfstudio.guidenh.guide.internal.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

class MarkdownListSemanticsTest {

    /**
     * Parses markdown through the full pipeline (mdast + MDX conversion) and
     * extracts the first
     * <li>element's MdxJsxFlowElement.
     */
    private MdxJsxFlowElement parseFirstLi(String markdown) {
        var root = MdAst.fromMarkdown(markdown, GuideMarkdownOptions.runtime());
        // Convert mdast → MDX (same as PageCompiler)
        MdAstToMdxConverter.convert(root, Map.of());

        // Navigate: root → <ul> or <ol> → <li>
        var rootChildren = root.children();
        assertFalse(rootChildren.isEmpty(), "root must have children");
        // Skip frontmatter nodes
        MdxJsxFlowElement listEl = null;
        for (var child : rootChildren) {
            if (child instanceof MdxJsxFlowElement flow && ("ul".equals(flow.name()) || "ol".equals(flow.name()))) {
                listEl = flow;
                break;
            }
        }
        assertNotNull(listEl, "expected a list (ul/ol) in root children: " + debugTypes(rootChildren));

        List<? extends MdAstAnyContent> liChildren = listEl.children();
        assertFalse(liChildren.isEmpty(), "list must have children");
        var li = liChildren.get(0);
        assertInstanceOf(
            MdxJsxFlowElement.class,
            li,
            "list child must be MdxJsxFlowElement, got: " + (li != null ? li.getClass() : "null"));
        return (MdxJsxFlowElement) li;
    }

    private static String debugTypes(List<?> children) {
        var sb = new StringBuilder("[");
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) sb.append(", ");
            Object c = children.get(i);
            if (c == null) {
                sb.append("null");
            } else {
                sb.append(
                    c.getClass()
                        .getSimpleName());
                if (c instanceof MdxJsxFlowElement f) {
                    sb.append("(")
                        .append(f.name())
                        .append(")");
                } else if (c instanceof MdAstNode n) {
                    sb.append("(")
                        .append(n.type())
                        .append(")");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Dump the
     * <li>children tree safely (using raw Object iteration to avoid
     * ClassCastException from raw-typed phrasing content inside flow elements).
     */
    private static String dumpTreeSafely(MdxJsxFlowElement container) {
        var sb = new StringBuilder();
        dumpObject(sb, container, 0);
        return sb.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void dumpObject(StringBuilder sb, Object obj, int depth) {
        if (obj == null) {
            sb.append("  ".repeat(depth))
                .append("null\n");
            return;
        }
        String className = obj.getClass()
            .getSimpleName();
        sb.append("  ".repeat(depth))
            .append(className);
        if (obj instanceof MdAstText text) {
            sb.append(" value='")
                .append(text.value)
                .append("'");
        } else if (obj instanceof MdxJsxFlowElement flow) {
            sb.append(" name=")
                .append(flow.name());
        }
        sb.append("\n");
        // Recurse into children if this is a parent node, using raw iteration
        if (obj instanceof MdxJsxFlowElement flow) {
            for (Object child : (List) flow.children()) {
                dumpObject(sb, child, depth + 1);
            }
        }
    }

    // Simple task items.

    @Test
    void testSimpleChecked() {
        String md = "- [x] Completed task A\n";
        var li = parseFirstLi(md);
        System.out.println("=== AST dump for '" + md.trim() + "' ===");
        System.out.println(dumpTreeSafely(li));
        System.out.println("li children types: " + debugTypes(li.children()));
        System.out.println(
            "li children size: " + li.children()
                .size());

        // Manually dump the first child's type
        var firstP = li.children()
            .get(0);
        System.out.println(
            "first child type: " + firstP.getClass()
                .getName());

        var marker = MarkdownListSemantics.extractTaskMarker(li.children());
        assertNotNull(marker, "extractTaskMarker must detect checked task");
        assertTrue(marker.checked(), "must be checked");
        assertEquals("Completed task A", marker.remainingText(), "remaining text must match");
    }

    @Test
    void testSimpleUnchecked() {
        String md = "- [ ] Incomplete task\n";
        var li = parseFirstLi(md);
        System.out.println("=== AST dump for '" + md.trim() + "' ===");
        System.out.println(dumpTreeSafely(li));

        var marker = MarkdownListSemantics.extractTaskMarker(li.children());
        assertNotNull(marker, "extractTaskMarker must detect unchecked task");
        assertFalse(marker.checked(), "must be unchecked");
        assertEquals("Incomplete task", marker.remainingText(), "remaining text must match");
    }

    // Nested task items.

    @Test
    void testNestedChecked() {
        String md = "- [x] Outer task\n  - [ ] Inner task\n";
        var root = MdAst.fromMarkdown(md, GuideMarkdownOptions.runtime());
        MdAstToMdxConverter.convert(root, Map.of());

        // Find the outer <li> (first child of the <ul>)
        MdxJsxFlowElement outerLi = null;
        for (var child : root.children()) {
            if (child instanceof MdxJsxFlowElement flow && "ul".equals(flow.name())) {
                var listChildren = flow.children();
                if (!listChildren.isEmpty() && listChildren.get(0) instanceof MdxJsxFlowElement li) {
                    outerLi = li;
                }
                break;
            }
        }
        assertNotNull(outerLi, "outer <li> must be found");
        System.out.println("=== AST dump for nested li ===");
        System.out.println(dumpTreeSafely(outerLi));
        System.out.println("outer li children types: " + debugTypes(outerLi.children()));
        System.out.println(
            "outer li children size: " + outerLi.children()
                .size());

        var marker = MarkdownListSemantics.extractTaskMarker(outerLi.children());
        assertNotNull(marker, "extractTaskMarker must detect nested checked task");
        assertTrue(marker.checked(), "outer task must be checked");
        assertEquals("Outer task", marker.remainingText(), "remaining text must match");
    }

    // Rich text task items.

    @Test
    void testRichTextChecked() {
        String md = "- [x] **Bold** task\n";
        var li = parseFirstLi(md);
        System.out.println("=== AST dump for '" + md.trim() + "' ===");
        System.out.println(dumpTreeSafely(li));

        var marker = MarkdownListSemantics.extractTaskMarker(li.children());
        assertNotNull(marker, "extractTaskMarker must detect rich-text checked task");
        assertTrue(marker.checked(), "must be checked");
        // extractTaskMarker is now NON-mutating: remainingText is the regex-computed
        // full remaining text (from toText, which concatenates leaf values, so
        // the strong markers around "Bold" are dropped), NOT the first text
        // node's stripped value.
        // The prefix-stripping for display is done in ListItemCompiler with save/restore.
        assertEquals("Bold task", marker.remainingText(), "remaining text must be full text after prefix");
    }

    @Test
    void testRichTextUnchecked() {
        String md = "- [ ] *italic* task\n";
        var li = parseFirstLi(md);
        System.out.println("=== AST dump for '" + md.trim() + "' ===");
        System.out.println(dumpTreeSafely(li));

        var marker = MarkdownListSemantics.extractTaskMarker(li.children());
        assertNotNull(marker, "extractTaskMarker must detect rich-text unchecked task");
        assertFalse(marker.checked(), "must be unchecked");
        // Same as checked: remainingText is the regex-computed full remaining text.
        assertEquals("italic task", marker.remainingText(), "remaining text must be full text after prefix");
    }

    // Negative case: a regular list item is not a task.

    @Test
    void testRegularListItem_notATask() {
        var li = parseFirstLi("- Regular item\n");
        var marker = MarkdownListSemantics.extractTaskMarker(li.children());
        assertNull(marker, "regular list item must not be detected as task");
    }
}
