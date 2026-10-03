package com.hfstudio.guidenh.guide.compiler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytTaskListItem;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.internal.extensions.DefaultExtensions;

/**
 * Test that the full page-compilation pipeline produces LytTaskListItem
 * for markdown task-list syntax {@code - [x]} / {@code - [ ]}.
 *
 * <p>
 * Replicates the EXACT runtime path used by
 * {@link com.hfstudio.guidenh.guide.internal.headless.RenderPageService}:
 * {@code PageCompiler.parse() → PageCompiler.compile()}.
 *
 * <p>
 * Uses the first 17 items of {@code visualtest/lists/tasks.md} (excludes
 * the last {@code <ItemImage>} line which requires item-registry infrastructure).
 */
class TaskListCompilationTest {

    private static final String TASKS_MD = "## Mixed Checked and Unchecked\n" + "\n"
        + "- [x] Completed task A\n"
        + "- [ ] Pending task B\n"
        + "- [x] Completed task C\n"
        + "- [ ] Pending task D\n"
        + "- [ ] Pending task E\n"
        + "\n"
        + "## Nested Task Lists\n"
        + "\n"
        + "- [x] Parent completed\n"
        + "  - [ ] Child pending A\n"
        + "  - [x] Child completed B\n"
        + "- [ ] Parent pending\n"
        + "  - [ ] Sub-task one\n"
        + "  - [ ] Sub-task two\n"
        + "    - [x] Deep nested completed\n"
        + "    - [ ] Deep nested pending\n"
        + "\n"
        + "## Rich Text Labels\n"
        + "\n"
        + "- [x] **Bold completed** with *italic* suffix\n"
        + "- [ ] `code inline` inside task label\n"
        + "- [x] Task with [link](https://example.com) embedded\n"
        + "- [ ] ~~Strikethrough~~ ++underline++ ^^wave^^ mixed\n";

    /**
     * THE CRITICAL TEST: follows the exact runtime compilation pipeline.
     *
     * <p>
     * Runtime path (headless rendering):
     * <ol>
     * <li>{@link PageCompiler#parse(String, String, ResourceLocation, String)}</li>
     * <li>{@link PageCompiler#compile(PageCollection, ExtensionCollection, ParsedGuidePage)}</li>
     * </ol>
     */
    @Test
    void runtimePipeline_producesLytTaskListItem() {
        // 1. Parse, exactly as RenderPageService.compileMdFile does.
        ResourceLocation pageId = new ResourceLocation("guidenh", "test/tasks");
        ParsedGuidePage parsed = PageCompiler.parse("test", "en_us", pageId, TASKS_MD);

        // 2. Build the ExtensionCollection with tag compilers, as DefaultExtensions does.
        ExtensionCollection extensions = buildTagExtensions();

        // 3. Compile, as RenderPageService.compileRegisteredPage / compileMdFile does.
        // We need a PageCollection. Create a minimal one.
        PageCollection pages = createMinimalPageCollection();

        GuidePage compiledPage = PageCompiler.compile(pages, extensions, parsed);
        LytDocument document = compiledPage.document();

        // 4. Walk the document tree for LytTaskListItem instances.
        List<LytNode> taskItems = new ArrayList<>();
        List<LytNode> plainItems = new ArrayList<>();
        walkForListItems(document, taskItems, plainItems);

        // Debug output
        System.out.println(
            "TaskListCompilationTest: found " + taskItems.size()
                + " LytTaskListItem, "
                + plainItems.size()
                + " LytListItem");

        // 5. Assert the collected items.
        // All 17 items (without ItemImage line) are task items ("- [x]" or "- [ ]")
        int total = taskItems.size() + plainItems.size();
        assertTrue(total > 0, "Expected at least some list items");
        assertEquals(
            total,
            taskItems.size(),
            "All " + total
                + " items should be LytTaskListItem, got "
                + taskItems.size()
                + " task items and "
                + plainItems.size()
                + " plain items");
    }

    /**
     * Tests that COMPILATION IS IDEMPOTENT: calling {@code PageCompiler.compile()}
     * twice on the same {@code ParsedGuidePage} must produce the same result.
     *
     * <p>
     * This guards against the real runtime bug: the CompileWorker pre-compiles
     * all pages eagerly on the {@code guidenh-compile} thread. When
     * {@code RenderPageService} compiles the same page again, the AST text nodes
     * must not have been permanently mutated by the first compile (prefix stripped
     * by {@code MarkdownListSemantics.extractTaskMarker}), otherwise the second
     * compile would fail to detect task markers.
     */
    @Test
    void doubleCompile_stillProducesLytTaskListItem() {
        ResourceLocation pageId = new ResourceLocation("guidenh", "test/tasks");
        ParsedGuidePage parsed = PageCompiler.parse("test", "en_us", pageId, TASKS_MD);
        ExtensionCollection extensions = buildTagExtensions();
        PageCollection pages = createMinimalPageCollection();

        // First compile (as CompileWorker would do)
        GuidePage compiled1 = PageCompiler.compile(pages, extensions, parsed);
        List<LytNode> taskItems1 = new ArrayList<>();
        List<LytNode> plainItems1 = new ArrayList<>();
        walkForListItems(compiled1.document(), taskItems1, plainItems1);
        System.out.println(
            "First compile: found " + taskItems1.size() + " LytTaskListItem, " + plainItems1.size() + " LytListItem");

        // Second compile (as RenderPageService would do)
        GuidePage compiled2 = PageCompiler.compile(pages, extensions, parsed);
        List<LytNode> taskItems2 = new ArrayList<>();
        List<LytNode> plainItems2 = new ArrayList<>();
        walkForListItems(compiled2.document(), taskItems2, plainItems2);
        System.out.println(
            "Second compile: found " + taskItems2.size() + " LytTaskListItem, " + plainItems2.size() + " LytListItem");

        // Both compiles must detect task markers
        int total1 = taskItems1.size() + plainItems1.size();
        int total2 = taskItems2.size() + plainItems2.size();
        assertTrue(total1 > 0, "First compile must produce list items");
        assertEquals(total1, taskItems1.size(), "First compile: all " + total1 + " items must be LytTaskListItem");
        assertTrue(total2 > 0, "Second compile must produce list items");
        assertEquals(
            total2,
            taskItems2.size(),
            "Second compile: all " + total2 + " items must be LytTaskListItem (idempotent)");
    }

    /**
     * Tests the lazy-parse path used for registered pages at runtime:
     * {@code PageCompiler.parseFrontmatterOnly()} → then
     * {@code ParsedGuidePage.getAstRoot()} (lazy) → {@code PageCompiler.compile()}.
     *
     * This is the EXACT path used by
     * {@link com.hfstudio.guidenh.guide.internal.headless.RenderPageService#compileRegisteredPage}.
     */
    @Test
    void runtimeLazyParsePath_producesLytTaskListItem() {
        ResourceLocation pageId = new ResourceLocation("guidenh", "test/tasks");
        // Simulate parseFrontmatterOnly like GuideLightweightReloadService does
        ParsedGuidePage parsed = PageCompiler.parseFrontmatterOnly("test", "en_us", pageId, TASKS_MD);
        // At this point, astRoot is null

        ExtensionCollection extensions = buildTagExtensions();
        PageCollection pages = createMinimalPageCollection();

        // This triggers getAstRoot() → lazy parse → PageCompiler.parse()
        GuidePage compiledPage = PageCompiler.compile(pages, extensions, parsed);
        LytDocument document = compiledPage.document();

        // Walk and inspect
        List<LytNode> taskItems = new ArrayList<>();
        List<LytNode> plainItems = new ArrayList<>();
        walkForListItems(document, taskItems, plainItems);

        System.out.println(
            "TaskListCompilationTest (lazy-parse): found " + taskItems.size()
                + " LytTaskListItem, "
                + plainItems.size()
                + " LytListItem");

        int total2 = taskItems.size() + plainItems.size();
        assertTrue(total2 > 0, "Expected at least some list items");
        assertEquals(
            total2,
            taskItems.size(),
            "All " + total2
                + " items should be LytTaskListItem (lazy-parse path), got "
                + taskItems.size()
                + " task items and "
                + plainItems.size()
                + " plain items");
    }

    /**
     * Walk the document tree and collect LytTaskListItem vs LytListItem instances.
     */
    private static void walkForListItems(LytNode node, List<LytNode> taskItems, List<LytNode> plainItems) {
        if (node instanceof LytTaskListItem) {
            taskItems.add(node);
        } else if (node instanceof LytListItem) {
            plainItems.add(node);
        }
        for (LytNode child : node.getChildren()) {
            walkForListItems(child, taskItems, plainItems);
        }
    }

    /**
     * Create a minimal PageCollection stub for testing.
     */
    private static PageCollection createMinimalPageCollection() {
        return new PageCollection() {

            @Override
            public <T extends com.hfstudio.guidenh.guide.indices.PageIndex> T getIndex(Class<T> indexClass) {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.util.Collection<ParsedGuidePage> getPages() {
                return List.of();
            }

            @Override
            public ParsedGuidePage getParsedPage(ResourceLocation id) {
                return null;
            }

            @Override
            public GuidePage getPage(ResourceLocation id) {
                return null;
            }

            @Override
            public byte[] loadAsset(ResourceLocation id) {
                return new byte[0];
            }

            @Override
            public com.hfstudio.guidenh.guide.navigation.NavigationTree getNavigationTree() {
                return null;
            }

            @Override
            public boolean pageExists(ResourceLocation pageId2) {
                return false;
            }
        };
    }

    /**
     * Build an ExtensionCollection with tag compilers matching {@code DefaultExtensions.tagCompilers()}.
     */
    static ExtensionCollection buildTagExtensions() {
        var builder = ExtensionCollection.builder();
        for (var compiler : DefaultExtensions.tagCompilers()) {
            builder.add(com.hfstudio.guidenh.guide.compiler.TagCompiler.EXTENSION_POINT, compiler);
        }
        return builder.build();
    }
}
