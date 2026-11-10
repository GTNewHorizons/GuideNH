package com.hfstudio.guidenh.guide.internal.mermaid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartParser;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapParser;

class MermaidLabelTest {

    @Test
    void markdownBodyKeepsItsLiteralQuotes() {
        assertEquals("'**quoted**'", MermaidParser.normalizeMarkdownSource("\"`'**quoted**'`\""));
        assertEquals("\"**quoted**\"", MermaidParser.normalizeMarkdownSource("\"`\"**quoted**\"`\""));
    }

    @Test
    void flowchartPlainFallbackKeepsDoubleQuotesInsideMarkdownDelimiters() {
        var node = FlowchartParser.parse("flowchart TD\nA[\"`\"quoted\"`\"]")
            .getNodes()
            .get("A");
        assertEquals("\"quoted\"", node.getLabel());
    }

    @Test
    void extendedNodesKeepSourceEntitiesAndDecodeTheirPlainFallbackOnce() {
        var node = FlowchartParser.parse("flowchart TD\nA@{ label: \"&lt;strong&gt;text&lt;/strong&gt; &amp;lt;\" }")
            .getNodes()
            .get("A");
        assertEquals("<strong>text</strong> &lt;", node.getLabel());
        assertTrue(
            node.getLabelSource()
                .contains("&lt;strong&gt;"));
    }

    @Test
    void extendedNodeBodyKeepsQuotedTextAndNormalizesBreaks() {
        var node = FlowchartParser.parse("flowchart TD\nA@{ label: \"'first'<br/>second\" }")
            .getNodes()
            .get("A");
        assertEquals("'first'\nsecond", node.getLabel());
    }

    @Test
    void extendedNodePlainFallbackRemovesOnlyTheMarkdownDelimiters() {
        var node = FlowchartParser.parse("flowchart TD\nA@{ label: \"`'quoted'`\" }")
            .getNodes()
            .get("A");
        assertEquals("'quoted'", node.getLabel());
    }

    @Test
    void mindmapsKeepEscapedTagsInSourceAndLiteralTagsInFallback() {
        var node = MindmapParser.parse("mindmap\nroot[\"&lt;strong&gt;text&lt;/strong&gt; &amp;lt;\"]")
            .getRoot();
        assertTrue(
            node.getLabelSource()
                .contains("&lt;strong&gt;"));
        assertEquals("<strong>text</strong> &lt;", node.getText());
    }

    @Test
    void mindmapPlainFallbackKeepsQuotesInsideMarkdownDelimiters() {
        var node = MindmapParser.parse("mindmap\nroot[\"`'**quoted**'`\"]")
            .getRoot();
        assertEquals("'quoted'", node.getText());
    }
}
