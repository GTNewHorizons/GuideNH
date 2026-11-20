package com.hfstudio.guidenh.guide.internal.host.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowBreak;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.internal.extensions.DefaultExtensions;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidParser;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartParser;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapParser;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtmlCompiler;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteMdxTagRenderer;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteSceneAnnotationSerializer;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteTemplateRegistry;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

class MermaidRichLabelTest {

    private static final ResourceLocation PAGE_ID = new ResourceLocation("guidenh:mermaid-test.md");

    @Test
    void gameCompilationKeepsLiteralQuotesAroundBoldText() throws Exception {
        assertEquals("'quoted'", gameFlowchart("A[\"`'**quoted**'`\"]").getTextContent());
    }

    @Test
    void iconLabelsKeepLiteralQuotesAroundRichText() throws Exception {
        assertEquals("'quoted'", gameFlowchart("A[\"fa:fa-house '**quoted**'\"]").getTextContent());
    }

    @Test
    void directExportKeepsEscapedTagsLiteralForBothNodeSyntaxes() {
        for (String node : new String[] { "A[\"&lt;strong&gt;text&lt;/strong&gt;\"]",
            "A@{ label: \"&lt;strong&gt;text&lt;/strong&gt;\" }" }) {
            String html = site("flowchart TD\n" + node);
            assertTrue(
                html.replaceAll("<br\\s*/?>", "")
                    .contains("&lt;strong&gt;text&lt;/strong&gt;"),
                html);
            assertFalse(html.contains("<strong>text</strong>"), html);
        }
    }

    @Test
    void escapedTagsRemainLiteralBesideRichText() throws Exception {
        String node = "A[\"**bold** &lt;strong&gt;text&lt;/strong&gt;\"]";
        assertEquals("bold <strong>text</strong>", gameFlowchart(node).getTextContent());
        String html = site("flowchart TD\n" + node);
        assertTrue(html.contains("<strong>bold</strong>"), html);
        assertTrue(html.contains("&lt;strong&gt;text&lt;/strong&gt;"), html);
    }

    @Test
    void consecutiveBreaksSurviveGameDirectExportAndCompiledExport() throws Exception {
        String node = "A[\"**first<br/><br/>second**\"]";
        LytParagraph paragraph = gameFlowchart(node);
        int[] breaks = { 0 };
        paragraph.visit(new LytVisitor() {

            @Override
            public Result beforeFlowContent(LytFlowContent content) {
                if (content instanceof LytFlowBreak) breaks[0]++;
                return Result.CONTINUE;
            }
        });
        assertEquals(2, breaks[0]);
        assertEquals(2, breakCount(site("flowchart TD\n" + node)));
        assertEquals(
            2,
            breakCount(GuideSiteSceneAnnotationSerializer.renderTooltipHtml(new ContentTooltip(paragraph), PAGE_ID)));
    }

    @Test
    void escapedBreakMarkupDoesNotBecomeABreak() throws Exception {
        String node = "A[\"**first**&lt;br/&gt;second\"]";
        assertEquals("first<br/>second", gameFlowchart(node).getTextContent());
        String html = site("flowchart TD\n" + node);
        assertTrue(html.contains("&lt;br/&gt;"), html);
        assertEquals(0, breakCount(html));
    }

    @Test
    void mindmapEntitiesStayLiteralInGameAndSite() throws Exception {
        String source = "mindmap\nroot[\"&lt;strong&gt;text&lt;/strong&gt;\"]";
        var node = MindmapParser.parse(source)
            .getRoot();
        LytParagraph compiled = compile(node.getLabelSource());
        assertEquals("<strong>text</strong>", compiled != null ? compiled.getTextContent() : node.getText());
        String html = site(source);
        assertTrue(html.contains("&lt;strong&gt;text&lt;/strong&gt;"), html);
        assertFalse(html.contains("<strong>text</strong>"), html);
    }

    private static int breakCount(String html) {
        return (int) java.util.regex.Pattern.compile("<br\\s*/?>")
            .matcher(html)
            .results()
            .count();
    }

    private static LytParagraph gameFlowchart(String source) throws Exception {
        var node = FlowchartParser.parse("flowchart TD\n" + source)
            .getNodes()
            .get("A");
        LytParagraph paragraph = compile(node.getLabelSource());
        return paragraph != null ? paragraph : LytParagraph.of(node.getLabel());
    }

    private static LytParagraph compile(String source) throws Exception {
        var extensions = ExtensionCollection.builder();
        DefaultExtensions.addAll(extensions, Set.of());
        var compiler = new PageCompiler(null, extensions.build(), "guidenh", "en_us", PAGE_ID, "");
        Method method = MermaidScript.class.getDeclaredMethod("compileRichLabel", PageCompiler.class, String.class);
        method.setAccessible(true);
        return (LytParagraph) method
            .invoke(new MermaidScript(), compiler, MermaidParser.normalizeMarkdownSource(source));
    }

    private static String site(String source) {
        var element = new MdxJsxFlowElement();
        element.setName("Mermaid");
        var text = new MdAstText();
        text.setValue(source);
        var paragraph = new MdAstParagraph();
        paragraph.children()
            .add(text);
        element.children()
            .add(paragraph);
        return new GuideSiteMdxTagRenderer(null, Map.of(), new NavigationTree())
            .render(element, "guidenh", PAGE_ID, new GuideSiteTemplateRegistry(), null, new GuideSiteHtmlCompiler());
    }
}
