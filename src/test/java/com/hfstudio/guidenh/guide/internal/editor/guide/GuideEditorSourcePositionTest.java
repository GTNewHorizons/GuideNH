package com.hfstudio.guidenh.guide.internal.editor.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.internal.markdown.MdAstToMdxConverter;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;

public class GuideEditorSourcePositionTest {

    @Test
    public void markdownConversionRetainsBlockPositionsAfterFrontmatter() {
        String source = "---\ntitle: Example\nbindings:\n  - {type: \"thaumcraft\", id: \"NITOR\"}\n---\n\n# Heading\n\nText **bold**.\n\n```text\nfirst\nsecond\n```\n";
        var root = MdAst.fromMarkdown(source, GuideMarkdownOptions.runtime());
        MdAstToMdxConverter.convert(root, Map.of());

        var heading = (MdAstNode) root.children()
            .get(0);
        var paragraph = (MdAstNode) root.children()
            .get(1);
        var code = (MdAstNode) root.children()
            .get(2);
        assertNotNull(heading.position());
        assertNotNull(paragraph.position());
        assertNotNull(code.position());
        assertEquals(
            source.indexOf("# Heading"),
            heading.position()
                .start()
                .offset());
        assertEquals(
            source.indexOf("Text **bold**."),
            paragraph.position()
                .start()
                .offset());
        assertEquals(
            source.indexOf("```text"),
            code.position()
                .start()
                .offset());
    }
}
