package com.hfstudio.guidenh.guide.mediawiki.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * The copy is what lets every call site substitute its own arguments without disturbing the template page.
 * A copy that quietly returned the original would let the first call rewrite the template for all later
 * ones, which is a failure that only shows up when a page calls the same template twice.
 */
class MediaWikiTemplateAstCopyTest {

    @Test
    void copyIsADistinctObjectForEveryNodeType() {
        String source = "# H\n\nText **bold** [link](Guide.md) `code` ![img](a.png)\n\n"
            + "> quote\n\n- a\n- b\n\n1. one\n\n"
            + "| a | b |\n| --- | --- |\n| 1 | 2 |\n\n"
            + "```java\nint x = 1;\n```\n\n---\n\n"
            + "<ItemImage id=\"minecraft:stone\" />\n\n<Color color=\"#ff0000\">red</Color>\n\n"
            + "<Row gap=\"4\">\n<ItemImage id=\"minecraft:iron_ingot\" /> text\n</Row>\n\n"
            + "<details open>\n<summary>S</summary>\n\nbody\n\n</details>\n\n"
            + "<FileTree>\nsrc\n  A.java\n</FileTree>\n\n"
            + "term\n: definition\n\n[ref]: Guide.md\n";

        List<String> shared = new java.util.ArrayList<>();
        for (MdAstAnyContent child : parse(source)) {
            assertDistinct(child, shared);
        }
        assertTrue(shared.isEmpty(), () -> "these nodes were returned instead of copied: " + shared);
    }

    @Test
    void copyOfATextNodeKeepsItsText() {
        MdAstAnyContent paragraph = parse("hello world\n").get(0);
        MdAstAnyContent copy = MediaWikiTemplateAst.copy(paragraph);
        assertEquals(MediaWikiTemplateAst.flatten(paragraph), MediaWikiTemplateAst.flatten(copy));
    }

    @Test
    void copyCarriesAttributesAndNames() {
        MdAstAnyContent source = parse("<Param name=\"note\" default=\"none\" />\n").get(0);
        MdAstAnyContent copy = MediaWikiTemplateAst.copy(source);
        assertNotSame(source, copy);

        MdxJsxElementFields element = findElement(copy);
        assertTrue(element != null, "expected a JSX element to survive the copy");
        assertEquals("Param", element.name());
        assertEquals("note", element.getAttributeString("name", null));
        assertEquals("none", element.getAttributeString("default", null));
    }

    @Test
    void copyDropsPositionsSoSourceBodiesAreNotReused() {
        MdAstAnyContent source = parse("<details open>\n<summary>S</summary>\n\nbody\n\n</details>\n").get(0);
        MdAstAnyContent copy = MediaWikiTemplateAst.copy(source);
        // A copied node must not claim the template page's offsets: a tag reading its body from source would
        // then slice unsubstituted text and lose the argument substitution.
        assertTrue(copy.position() == null, () -> "copied node kept a position: " + copy.position());
    }

    private static void assertDistinct(MdAstAnyContent node, List<String> shared) {
        MdAstAnyContent copy = MediaWikiTemplateAst.copy(node);
        if (copy == node) {
            shared.add(
                node.getClass()
                    .getSimpleName());
        }
        for (MdAstAnyContent child : MediaWikiTemplateAst.childrenOf(node)) {
            assertDistinct(child, shared);
        }
    }

    private static MdxJsxElementFields findElement(MdAstAnyContent node) {
        if (node instanceof MdxJsxElementFields element && "Param".equals(element.name())) {
            return element;
        }
        for (MdAstAnyContent child : MediaWikiTemplateAst.childrenOf(node)) {
            MdxJsxElementFields found = findElement(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static List<? extends MdAstAnyContent> parse(String source) {
        return PageCompiler.parse("guidenh", "en_us", new ResourceLocation("guidenh", "guidenh/probe.md"), source)
            .getAstRoot()
            .children();
    }
}
