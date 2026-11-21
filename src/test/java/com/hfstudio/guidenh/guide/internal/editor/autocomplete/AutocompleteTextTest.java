package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.MarkdownSyntaxCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.SyntaxValueCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.TagCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MarkdownSyntaxContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.TagStartContext;
import com.hfstudio.guidenh.guide.internal.syntax.BuiltinSyntaxContributor;
import com.hfstudio.guidenh.guide.syntax.GuideSyntaxModel;
import com.hfstudio.guidenh.guide.syntax.InsertTemplate;
import com.hfstudio.guidenh.guide.syntax.MarkdownSnippet;
import com.hfstudio.guidenh.guide.syntax.MarkdownSnippetKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxContributor;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;

public class AutocompleteTextTest {

    @Test
    public void allBuiltinTagTemplatesRemainExplicitInsteadOfExpandingOrdinaryCompletion() {
        var model = builtinModel();
        assertFalse(
            model.insertTemplates()
                .isEmpty());
        for (String tag : model.insertTemplates()
            .keySet()) {
            String parent = tag.equals("InputAnnotation") || tag.equals("ImportStructureLib") ? "GameScene"
                : tag.equals("BlockStat") ? "BlockStats" : null;
            var context = new TagStartContext(1, tag.length() + 1, tag, parent);
            var candidates = GuideSyntaxCompletion
                .query(model, new TextSyntaxContext(SyntaxElementType.TAG_START, 0, tag.length() + 1, context), 20);
            var candidate = candidates.stream()
                .filter(
                    value -> value.rankingText()
                        .equals(tag))
                .findFirst()
                .orElseThrow();
            assertFalse(
                candidate.displayText()
                    .contains("="),
                tag);
            assertFalse(
                candidate.replacementText()
                    .contains("="),
                tag);
            var commit = AutocompleteCommitService.commit("<" + tag, context, candidate);
            assertNotNull(commit);
            assertEquals(
                model.isContainerTag(tag) ? "<" + tag + "></" + tag + ">" : "<" + tag + " />",
                commit.getText());
            assertNotNull(model.insertTemplate(tag));
        }
    }

    @Test
    public void explicitMultilineTagNormalizesNewlinesAndMapsCaretThroughIndentation() {
        String source = "<Row>\n    <Graph/>\n</Row>";
        String text = "<Graph>\r\n  <Plot expr=\"\" />\r\n</Graph>";
        int caret = text.indexOf("expr=\"") + "expr=\"".length();
        var candidate = new TagCandidate("Graph", true, new InsertTemplate("Graph", text, caret));
        int start = source.indexOf("Graph");
        var commit = AutocompleteCommitService
            .commit(source, new TagStartContext(start, start + 5, "Graph"), candidate);
        String expected = "<Row>\n    <Graph>\n      <Plot expr=\"\" />\n    </Graph>\n</Row>";
        assertEquals(expected, commit.getText());
        assertEquals(expected.indexOf("expr=\"") + "expr=\"".length(), commit.getSelectionStart());
        assertEquals(commit.getSelectionStart(), commit.getSelectionEnd());
        assertEquals("Graph  </Graph>", candidate.displayText());
    }

    @Test
    public void markdownCompletionKeepsIndentationWithoutDuplicatingTrailingNewline() {
        String source = "\t```\nnext";
        var snippet = MarkdownSnippet.block("```", "Code block", "```\n\n```\n", 4);
        var context = new MarkdownSyntaxContext(MarkdownSnippetKind.BLOCK, 1, 4, "```");
        var commit = AutocompleteCommitService.commit(source, context, new MarkdownSyntaxCandidate(snippet));
        assertEquals("\t```\n\t\n\t```\nnext", commit.getText());
        assertEquals(6, commit.getSelectionStart());
    }

    @Test
    public void inheritedYamlValueDoesNotAddAnEmptyLineOrDoubleItsIndentation() {
        String value = "\n  icons:\n    - ";
        for (String source : List.of("---\nnavigation:\n  \n---", "---\nnavigation: \n---")) {
            int start = source.indexOf("\n---");
            var context = new FrontmatterContext("navigation", true, start, start, "");
            var commit = AutocompleteCommitService
                .commit(source, context, new SyntaxValueCandidate(SyntaxSuggestion.of(value), SyntaxValueKind.STRING));
            String expected = source.contains("navigation: ") ? "---\nnavigation: \n  icons:\n    - \n---"
                : "---\nnavigation:\n  icons:\n    - \n---";
            assertEquals(expected, commit.getText());
            assertEquals(expected.indexOf("\n---"), commit.getSelectionStart());
        }
    }

    @Test
    public void multilineValueLabelsAreCompactWithoutChangingInsertion() {
        String value = "\n  icons:\n    - " + "x".repeat(250);
        var candidate = new SyntaxValueCandidate(SyntaxSuggestion.of(value), SyntaxValueKind.STRING);
        assertFalse(
            candidate.displayText()
                .contains("\n"));
        assertFalse(
            candidate.displayText()
                .contains("\r"));
        assertTrue(
            candidate.displayText()
                .length() <= 83);
        assertTrue(
            candidate.displayText()
                .contains("\\n"));
        assertEquals(value, candidate.replacementText());
    }

    private static GuideSyntaxModel builtinModel() {
        return GuideSyntaxModel.of(
            ExtensionCollection.builder()
                .add(SyntaxContributor.EXTENSION_POINT, new BuiltinSyntaxContributor())
                .build());
    }
}
