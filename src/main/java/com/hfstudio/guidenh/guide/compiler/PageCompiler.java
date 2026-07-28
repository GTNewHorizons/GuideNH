package com.hfstudio.guidenh.guide.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.tags.CsvTableCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.DetailsContentExtractor;
import com.hfstudio.guidenh.guide.document.block.LatexRenderOptions;
import com.hfstudio.guidenh.guide.document.block.LatexVerticalAlign;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
import com.hfstudio.guidenh.guide.document.block.LytFloatAwareBlock;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytLatexBlock;
import com.hfstudio.guidenh.guide.document.block.LytLatexDisplayBlock;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.flow.LytSpoilerSpan;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.markdown.FootnotePreprocessor;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownActionLink;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownHtmlRuntimeNormalizer;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLatexShorthand;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLiteralAutolink;
import com.hfstudio.guidenh.guide.internal.markdown.MdAstToMdxConverter;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.sound.GuideSoundParsers;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.TextStyle;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.MdastOptions;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTable;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableRow;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstDefinition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstFlowContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPosition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;
import com.hfstudio.guidenh.libs.mdx.MdxCommentMasker;
import com.hfstudio.guidenh.libs.micromark.ParseException;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistPoint;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

import lombok.Getter;

public class PageCompiler {

    /**
     * Default gap between block-level elements. Set as margin.
     */
    public static final int DEFAULT_ELEMENT_SPACING = 5;
    public static final MdastOptions PARSE_OPTIONS = GuideMarkdownOptions.runtime();
    public static final int DEFAULT_MARK_BACKGROUND_COLOR = 0xFF8A6A00;
    private static final Pattern TABLE_ATTRIBUTE_LINE = Pattern.compile("^\\{:\\s*(.+?)\\s*}$");
    private static PageLinkResolver pageLinkResolver = PageCompiler::defaultPageExistsForLink;
    private static final State<List<SourceSlice>> SOURCE_SLICE_STACK = new State<>(
        "source_slice_stack",
        castClass(List.class),
        Collections.emptyList());

    private final PageCollection pages;
    @Getter
    private final ExtensionCollection extensions;
    @Getter
    private final String sourcePack;
    @Getter
    private final String language;
    /**
     * -- GETTER --
     * Get the current page id.
     */
    @Getter
    private final ResourceLocation pageId;
    private final String pageContent;
    private final Map<String, MdAstDefinition> definitions = new HashMap<>();

    private final Map<String, TagCompiler> tagCompilers = new HashMap<>();

    // Data associated with the current page being compiled, this is used by
    // compilers to communicate with each other within the current page.
    private final Map<State<?>, Object> compilerState = new IdentityHashMap<>();
    private final Map<MdxJsxElementFields, BlockTagChildrenCacheEntry> blockTagChildrenCache = new IdentityHashMap<>();
    private final Map<String, ParsedGuidePage> inlineMarkdownParseCache = new HashMap<>();

    public PageCompiler(PageCollection pages, ExtensionCollection extensions, String sourcePack,
        ResourceLocation pageId, String pageContent) {
        this(pages, extensions, sourcePack, LangUtil.ENGLISH_LANGUAGE, pageId, pageContent);
    }

    public PageCompiler(PageCollection pages, ExtensionCollection extensions, String sourcePack, String language,
        ResourceLocation pageId, String pageContent) {
        this.pages = pages;
        this.extensions = extensions;
        this.sourcePack = sourcePack;
        this.language = language;
        this.pageId = pageId;
        this.pageContent = pageContent;

        // Index available tag-compilers
        for (var tagCompiler : extensions.get(TagCompiler.EXTENSION_POINT)) {
            for (String tagName : tagCompiler.getTagNames()) {
                tagCompilers.put(tagName, tagCompiler);
            }
        }
    }

    @Deprecated
    public static ParsedGuidePage parse(String sourcePack, ResourceLocation id, InputStream in) throws IOException {
        return parse(sourcePack, "en_us", id, in);
    }

    public static ParsedGuidePage parse(String sourcePack, String language, ResourceLocation id, InputStream in)
        throws IOException {
        StringBuilder buffer = new StringBuilder();
        char[] chunk = new char[4096];
        try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            int n;
            while ((n = reader.read(chunk)) != -1) {
                buffer.append(chunk, 0, n);
            }
        }
        return parse(sourcePack, language, id, buffer.toString());
    }

    @Deprecated
    public static ParsedGuidePage parse(String sourcePack, ResourceLocation id, String pageContent) {
        return parse(sourcePack, "en_us", id, pageContent);
    }

    public static ParsedGuidePage parse(String sourcePack, String language, ResourceLocation id, String pageContent) {
        pageContent = pageContent != null ? pageContent : "";
        pageContent = normalizeLineEndings(pageContent);
        pageContent = FootnotePreprocessor.preprocess(pageContent);
        var sourceFrontmatter = parseFrontmatterFromSource(id, pageContent);
        MarkdownLatexShorthand.MaskResult latexMask = MarkdownLatexShorthand.mask(pageContent);
        String parseContent = MdxCommentMasker.mask(latexMask.source());

        MdAstRoot astRoot;
        String parseFailureMessage = null;
        UnistPoint parseFailureFrom = null;
        UnistPoint parseFailureTo = null;
        Frontmatter frontmatter;
        try {
            astRoot = MdAst.fromMarkdown(parseContent, PARSE_OPTIONS);
            MarkdownLatexShorthand.restore(astRoot, latexMask);
            MarkdownHtmlRuntimeNormalizer.normalize(astRoot);

            Map<String, MdAstDefinition> definitions = GuideMarkdownDefinitions.collect(astRoot);
            frontmatter = parseFrontmatter(id, astRoot);
            MdAstToMdxConverter.convert(astRoot, definitions);
        } catch (RuntimeException t) {
            if (t instanceof ParseException e) {
                parseFailureFrom = e.getFrom();
                parseFailureTo = e.getTo();
            }
            String errorMessage = formatParseFailureMessage(id, language, sourcePack, parseFailureFrom);
            GuideDebugLog.error("[GuideNH] [PageCompiler] {}", errorMessage, t);
            parseFailureMessage = errorMessage + ": \n" + t;
            astRoot = buildErrorPage(parseFailureMessage);
            frontmatter = new Frontmatter(null, Collections.emptyMap());
        }

        if (parseFailureMessage != null && sourceFrontmatter.navigationEntry() != null) {
            frontmatter = sourceFrontmatter;
        }

        return new ParsedGuidePage(
            sourcePack,
            id,
            pageContent,
            astRoot,
            frontmatter,
            language,
            parseFailureMessage,
            parseFailureFrom,
            parseFailureTo);
    }

    /**
     * Lightweight parse that extracts only YAML frontmatter from the raw source,
     * deferring the full Micromark → mdast pipeline to first call of
     * {@link ParsedGuidePage#getAstRoot()}.
     *
     * <p>
     * F3+T reload uses this path so that index/navigation rebuilds —
     * which only need frontmatter — complete without paying Micromark cost.
     * </p>
     */
    public static ParsedGuidePage parseFrontmatterOnly(String sourcePack, String language, ResourceLocation id,
        String pageContent) {
        pageContent = pageContent != null ? pageContent : "";
        pageContent = normalizeLineEndings(pageContent);
        var sourceFrontmatter = parseFrontmatterFromSource(id, pageContent);

        return new ParsedGuidePage(
            sourcePack,
            id,
            pageContent,
            null, // astRoot — triggers lazy parse on first getAstRoot()
            sourceFrontmatter,
            language,
            null,
            null,
            null);
    }

    public static String normalizeLineEndings(String pageContent) {
        return GuideStringLines.normalizeLineEndings(pageContent);
    }

    private static String formatParseFailureMessage(ResourceLocation id, String language, String sourcePack,
        @Nullable UnistPoint position) {
        String positionText = "";
        if (position != null) {
            positionText = " at line " + position.line() + " column " + position.column();
        }
        return String.format(
            Locale.ROOT,
            "Failed to parse GuideME page %s (lang: %s)%s from resource pack %s",
            id,
            language,
            positionText,
            sourcePack);
    }

    public static MdAstRoot buildErrorPage(String errorText) {
        return buildErrorPage("PARSING ERROR", errorText);
    }

    public static MdAstRoot buildErrorPage(String headingText, String errorText) {
        var root = new MdAstRoot();

        var heading = new MdxJsxFlowElement();
        heading.setName("h1");
        heading.addAttribute("depth", 1);
        root.addChild(heading);
        var headingTextNode = new MdAstText();
        headingTextNode.setValue(headingText);
        safeAddChild(heading, headingTextNode);

        var errorParagraph = new MdxJsxFlowElement();
        errorParagraph.setName("p");
        root.addChild(errorParagraph);
        var errorTextNode = new MdAstText();
        errorTextNode.setValue(errorText);
        safeAddChild(errorParagraph, errorTextNode);

        return root;
    }

    /**
     * Adds a child node to an {@link MdxJsxFlowElement} with type validation.
     * If the node is a valid {@link MdAstFlowContent} (the expected child type),
     * it is added via the normal {@code addChild} path. Otherwise, raw-type
     * access is used as a safe fallback to bypass the type constraint — this
     * prevents the error page builder itself from crashing when attempting to
     * add phrasing content (e.g. {@link MdAstText}) that is semantically valid
     * inside flow elements like {@code <h1>} or {@code <p>}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void safeAddChild(MdxJsxFlowElement element, MdAstNode node) {
        if (node instanceof MdAstFlowContent) {
            element.addChild(node);
        } else {
            ((List) element.children()).add(node);
        }
    }

    public static GuidePage buildErrorGuidePage(PageCollection pages, ExtensionCollection extensions, String sourcePack,
        ResourceLocation id, String pageContent, String headingText, String errorText) {
        var errorRoot = buildErrorPage(headingText, errorText);
        var document = new PageCompiler(pages, extensions, sourcePack, id, pageContent).compile(errorRoot);
        var titleHeading = extractPageTitleHeading(document);
        return new GuidePage(sourcePack, id, document, titleHeading);
    }

    public static GuidePage compile(PageCollection pages, ExtensionCollection extensions, ParsedGuidePage parsedPage) {
        // Translate page tree over to layout pages
        var document = new PageCompiler(
            pages,
            extensions,
            parsedPage.getSourcePack(),
            parsedPage.getLanguage(),
            parsedPage.getId(),
            parsedPage.getSource()).compile(parsedPage.getAstRoot());
        var titleHeading = extractPageTitleHeading(document);
        FrontmatterPageMeta pageMeta = parsedPage.getFrontmatter() != null ? parsedPage.getFrontmatter()
            .parseMeta() : null;
        if (pageMeta != null && pageMeta.isEmpty()) pageMeta = null;
        return new GuidePage(parsedPage.getSourcePack(), parsedPage.getId(), document, titleHeading, pageMeta);
    }

    /**
     * Finds the first H1 {@link LytHeading} in the compiled document, removes it from the
     * document (so it is not rendered twice inside the content area when displayed in a
     * toolbar), and returns it. Non-heading blocks are skipped during the search. Returns
     * {@code null} when no H1 is present or when the first heading is not H1.
     */
    @Nullable
    private static LytHeading extractPageTitleHeading(LytDocument document) {
        for (var block : new ArrayList<>(document.getBlocks())) {
            if (block instanceof LytHeading heading) {
                if (heading.getDepth() == 1) {
                    document.removeChild(heading);
                    return heading;
                } else {
                    break;
                }
            }
        }
        return null;
    }

    public <T extends Extension> List<T> getExtensions(ExtensionPoint<T> extensionPoint) {
        return extensions.get(extensionPoint);
    }

    private LytDocument compile(MdAstRoot root) {
        definitions.clear();
        definitions.putAll(GuideMarkdownDefinitions.collect(root));
        var document = new LytDocument();
        document.setSourceNode(root);
        compileBlockContext(root, document);
        return document;
    }

    public static Frontmatter parseFrontmatter(ResourceLocation pageId, MdAstRoot root) {
        Frontmatter result = null;

        for (var child : root.children()) {
            if (child instanceof MdAstYamlFrontmatter frontmatter) {
                if (result != null) {
                    GuideDebugLog.error("[GuideNH] [PageCompiler] Found more than one frontmatter!");
                    continue;
                }
                try {
                    result = Frontmatter.parse(pageId, frontmatter.value);
                } catch (Exception e) {
                    GuideDebugLog.error("[GuideNH] [PageCompiler] Failed to parse frontmatter for page {}", pageId, e);
                    break;
                }
            }
        }

        return result != null ? result : new Frontmatter(null, Collections.emptyMap());
    }

    public static Frontmatter parseFrontmatterFromSource(ResourceLocation pageId, String pageContent) {
        // Strip UTF-8 BOM if present (resource pack files may include it)
        if (pageContent.startsWith("﻿")) {
            pageContent = pageContent.substring(1);
        }
        var yamlText = extractFrontmatterText(pageContent);
        if (yamlText == null) {
            return new Frontmatter(null, Collections.emptyMap());
        }

        try {
            return Frontmatter.parse(pageId, yamlText);
        } catch (Exception e) {
            GuideDebugLog.error("[GuideNH] [PageCompiler] Failed to parse frontmatter for page {}", pageId, e);
            return new Frontmatter(null, Collections.emptyMap());
        }
    }

    public static @Nullable String extractFrontmatterText(String pageContent) {
        if (!pageContent.startsWith("---\n")) {
            return null;
        }

        int bodyStart = 4;
        int closingMarker = pageContent.indexOf("\n---\n", bodyStart);
        if (closingMarker >= 0) {
            return pageContent.substring(bodyStart, closingMarker);
        }

        if (pageContent.endsWith("\n---")) {
            return pageContent.substring(bodyStart, pageContent.length() - 4);
        }

        return null;
    }

    public void compileBlockContext(MdAstParent<?> markdownParent, LytBlockContainer layoutParent) {
        compileBlockContext(markdownParent.children(), layoutParent);
    }

    public void compileBlockTagChildren(MdxJsxElementFields element, LytBlockContainer layoutParent) {
        BlockTagChildrenCacheEntry cachedChildren = getBlockTagChildrenCacheEntry(element);
        if (cachedChildren.source() == null || cachedChildren.parsedPage() == null) {
            compileBlockContextInSourceContext(element.children(), layoutParent);
            return;
        }

        Map<String, MdAstDefinition> previousDefinitions = new HashMap<>(definitions);
        definitions.putAll(
            GuideMarkdownDefinitions.collect(
                cachedChildren.parsedPage()
                    .getAstRoot()));
        try {
            withSourceSlice(
                cachedChildren.source(),
                () -> compileBlockContext(
                    cachedChildren.parsedPage()
                        .getAstRoot(),
                    layoutParent));
        } finally {
            definitions.clear();
            definitions.putAll(previousDefinitions);
        }
    }

    public List<? extends MdAstAnyContent> reparseBlockTagChildren(MdxJsxElementFields element) {
        BlockTagChildrenCacheEntry cachedChildren = getBlockTagChildrenCacheEntry(element);
        if (cachedChildren.parsedPage() == null) {
            return element.children();
        }
        return cachedChildren.parsedPage()
            .getAstRoot()
            .children();
    }

    /**
     * Returns the verbatim, dedented source text between the opening and closing tag of a block
     * level MDX element, or {@code null} when the element has no source position information.
     * Useful for tag compilers whose body is parsed by a non-Markdown grammar (file trees, etc.).
     */
    public @Nullable String getBlockTagChildrenSource(MdxJsxElementFields element) {
        BlockTagChildrenCacheEntry cachedChildren = getBlockTagChildrenCacheEntry(element);
        if (cachedChildren.source() != null) {
            return cachedChildren.source();
        }
        return sourceForChildren(element.children());
    }

    /**
     * Parses {@code source} as a standalone markdown fragment and appends the resulting inline
     * (phrasing-level) content of its first paragraph into {@code layoutParent}. Block-level nodes
     * other than the leading paragraph are flattened to their inline content. Used by tag
     * compilers that need to render free-form rich-text fragments.
     */
    public void compileInlineMarkdown(String source, LytFlowParent layoutParent) {
        if (source == null || source.isEmpty()) {
            return;
        }
        ParsedGuidePage parsed = inlineMarkdownParseCache.get(source);
        if (parsed == null) {
            parsed = parse(sourcePack, "en_us", pageId, source);
            inlineMarkdownParseCache.put(source, parsed);
        }
        compileInlineFragment(
            parsed.getAstRoot()
                .children(),
            layoutParent);
    }

    public void compileBlockMarkdown(String source, LytBlockContainer layoutParent) {
        if (source == null || source.isEmpty()) {
            return;
        }
        ParsedGuidePage parsed = parse(sourcePack, language, pageId, source);
        Map<String, MdAstDefinition> previousDefinitions = new HashMap<>(definitions);
        definitions.putAll(GuideMarkdownDefinitions.collect(parsed.getAstRoot()));
        try {
            withSourceSlice(source, () -> compileBlockContext(parsed.getAstRoot(), layoutParent));
        } finally {
            definitions.clear();
            definitions.putAll(previousDefinitions);
        }
    }

    public void compileInlineFragment(Collection<? extends MdAstAnyContent> children, LytFlowParent layoutParent) {
        for (MdAstAnyContent child : children) {
            if (child instanceof MdxJsxFlowElement el && "p".equals(el.name())) {
                compileFlowContext(el, layoutParent);
            } else if (child instanceof MdxJsxFlowElement el) {
                for (var nestedChild : el.children()) {
                    compileFlowContent(layoutParent, nestedChild);
                }
            } else if (child instanceof MdxJsxTextElement el) {
                compileFlowContent(layoutParent, el);
            } else if (child instanceof MdAstParent<?>nestedParent) {
                for (var nestedChild : nestedParent.children()) {
                    compileFlowContent(layoutParent, nestedChild);
                }
            } else {
                compileFlowContent(layoutParent, child);
            }
        }
    }

    public void compileTableCellContent(MdAstParent<?> markdownParent, LytBlockContainer layoutParent) {
        compileTableCellContent(markdownParent.children(), layoutParent);
    }

    public void compileTableCellContent(List<? extends MdAstAnyContent> children, LytBlockContainer layoutParent) {
        var paragraph = new LytParagraph();
        paragraph.setMarginTop(0);
        paragraph.setMarginBottom(0);
        withChildrenSourceContext(children, () -> compileInlineFragment(children, paragraph));
        if (paragraph.isEmpty()) {
            return;
        }
        layoutParent.append(paragraph);
    }

    public void compileBlockContextInSourceContext(List<? extends MdAstAnyContent> children,
        LytBlockContainer layoutParent) {
        withChildrenSourceContext(children, () -> compileBlockContext(children, layoutParent));
    }

    public void withBlockTagChildrenSourceContext(MdxJsxElementFields element, Runnable action) {
        BlockTagChildrenCacheEntry cachedChildren = getBlockTagChildrenCacheEntry(element);
        if (cachedChildren.source() != null) {
            withSourceSlice(cachedChildren.source(), action);
            return;
        }
        withChildrenSourceContext(element.children(), action);
    }

    public void withSourceContext(String sourceText, Runnable action) {
        if (sourceText == null) {
            action.run();
            return;
        }
        withSourceSlice(sourceText, action);
    }

    public void compileBlockContext(List<? extends MdAstAnyContent> children, LytBlockContainer layoutParent) {
        LytBlock previousLayoutChild = null;
        for (MdAstAnyContent child : children) {
            LytBlock layoutChild = null;

            if (child instanceof MdxJsxFlowElement el) {
                // Definition elements are metadata, not rendered
                if ("definition".equals(el.name())) {
                    layoutChild = null;
                } else {
                    var compiler = tagCompilers.get(el.name());
                    if (compiler == null) {
                        layoutChild = createErrorBlock("Unhandled MDX element in block context: " + el.name(), child);
                    } else {
                        layoutChild = null;
                        compiler.compileBlockContext(this, layoutParent, el);
                    }
                }
            } else if (child instanceof MdxJsxTextElement el) {
                // Inline element at block level — merge into previous paragraph when possible
                if (previousLayoutChild instanceof LytParagraph paragraph) {
                    var flowCompiler = tagCompilers.get(el.name());
                    if (flowCompiler != null) {
                        flowCompiler.compileFlowContext(this, paragraph, el);
                    }
                    continue;
                }
                var paragraph = new LytParagraph();
                var flowCompiler = tagCompilers.get(el.name());
                if (flowCompiler != null) {
                    flowCompiler.compileFlowContext(this, paragraph, el);
                }
                layoutChild = paragraph;
            } else if (child instanceof MdAstText text) {
                // Orphan text — merge into previous paragraph when possible
                if (previousLayoutChild instanceof LytParagraph paragraph) {
                    var flowText = new LytFlowText();
                    flowText.setText(text.value);
                    paragraph.append(flowText);
                    continue;
                }
                var paragraph = new LytParagraph();
                var flowText = new LytFlowText();
                flowText.setText(text.value);
                paragraph.append(flowText);
                layoutChild = paragraph;
            } else if (child instanceof MdAstDefinition) {
                layoutChild = null; // handled via <definition> element
            } else {
                layoutChild = createErrorBlock(
                    "Unhandled node in block context: " + child.getClass()
                        .getSimpleName(),
                    child);
            }

            if (layoutChild != null) {
                layoutChild = wrapFloatAwareIfNeeded(layoutChild);
                if (child instanceof MdAstNode astNode) {
                    layoutChild.setSourceNode(astNode);
                }
                layoutParent.append(layoutChild);
            }
            previousLayoutChild = layoutChild;
        }
    }

    private void compileParagraphBlock(MdAstParagraph astParagraph, LytBlockContainer parent) {
        var children = astParagraph.children();
        if (children.size() == 1 && children.getFirst() instanceof MdAstText soleText) {
            String formula = MarkdownLatexShorthand.extractSoleDisplayFormula(soleText.value);
            if (formula != null) {
                var displayBlock = new LytLatexDisplayBlock(
                    formula,
                    LatexRenderOptions.builder()
                        .build());
                displayBlock.setMarginTop(DEFAULT_ELEMENT_SPACING);
                displayBlock.setMarginBottom(DEFAULT_ELEMENT_SPACING);
                parent.append(wrapFloatAwareIfNeeded(displayBlock));
                return;
            }
        }
        var paragraph = new LytParagraph();
        compileFlowContext(astParagraph, paragraph);
        paragraph.setMarginTop(DEFAULT_ELEMENT_SPACING);
        paragraph.setMarginBottom(DEFAULT_ELEMENT_SPACING);
        if (astParagraph.children()
            .isEmpty() && paragraph.isEmpty()) {
            return;
        }
        parent.append(wrapFloatAwareIfNeeded(paragraph));
    }

    public static LytBlock wrapFloatAwareIfNeeded(LytBlock block) {
        if (block instanceof LytParagraph || block instanceof LytDocumentFloat
            || block instanceof LytFloatAwareBlock
            || block instanceof LytListItem) {
            return block;
        }
        return new LytFloatAwareBlock(block);
    }

    public void compileFlowContext(MdAstParent<?> markdownParent, LytFlowParent layoutParent) {
        compileFlowContext(markdownParent.children(), layoutParent);
    }

    public void compileFlowContext(Collection<? extends MdAstAnyContent> children, LytFlowParent layoutParent) {
        for (var child : children) {
            compileFlowContent(layoutParent, child);
        }
    }

    private void compileFlowContent(LytFlowParent layoutParent, MdAstAnyContent content) {
        LytFlowContent layoutChild = null;

        if (content instanceof MdAstText astText) {
            if (compileActionLinks(layoutParent, astText.value)) {
                layoutChild = null;
            } else if (compileLiteralAutolinks(layoutParent, astText.value)) {
                layoutChild = null;
            } else if (compileInlineDollarLatex(layoutParent, astText.value)) {
                layoutChild = null;
            } else {
                String value = astText.value;
                if (value.indexOf('§') >= 0) {
                    List<LytFlowContent> fragments = parseSectionFormatting(value);
                    for (var fragment : fragments) {
                        layoutParent.append(fragment);
                    }
                    layoutChild = null;
                } else {
                    var text = new LytFlowText();
                    text.setText(value);
                    layoutChild = text;
                }
            }
        } else if (content instanceof MdxJsxTextElement el) {
            if ("Spoiler".equals(el.name())) {
                var span = new LytSpoilerSpan();
                span.modifyStyle(style -> style.backgroundColor(new ConstantColor(0xFF000000)));
                compileFlowContext(el, span);
                layoutChild = span;
            } else if ("span".equals(el.name())) {
                // Residual inline HTML span wrapper; preserve its children.
                compileFlowContext(el, layoutParent);
                layoutChild = null;
            } else {
                var compiler = tagCompilers.get(el.name());
                if (compiler == null) {
                    layoutChild = createErrorFlowContent(
                        "Unhandled MDX element in flow context: " + el.name(),
                        content);
                } else {
                    layoutChild = null;
                    compiler.compileFlowContext(this, layoutParent, el);
                }
            }
        } else {
            layoutChild = createErrorFlowContent(
                "Unhandled node in flow context: " + content.getClass()
                    .getSimpleName(),
                content);
        }

        if (layoutChild != null) {
            layoutParent.append(layoutChild);
        }
    }

    private boolean compileActionLinks(LytFlowParent layoutParent, String text) {
        if (!MarkdownActionLink.mayContain(text)) {
            return false;
        }

        List<MarkdownActionLink.Segment> segments = MarkdownActionLink.split(text);
        ArrayList<LytFlowContent> rendered = new ArrayList<>(segments.size());
        boolean foundSoundLink = false;
        for (var segment : segments) {
            if (!segment.isLink()) {
                if (!segment.text()
                    .isEmpty()) {
                    rendered.add(LytFlowText.of(segment.text()));
                }
                continue;
            }

            var sound = GuideSoundParsers.parseActionUri(this, segment.href());
            if (sound == null) {
                rendered.add(LytFlowText.of("&[" + segment.text() + "](" + segment.href() + ")"));
                continue;
            }

            var link = new LytFlowLink();
            link.setClickSoundSpec(sound);
            link.setClickCallback(uiHost -> {});
            link.appendText(segment.text());
            rendered.add(link);
            foundSoundLink = true;
        }
        if (!foundSoundLink) {
            return false;
        }

        for (var content : rendered) {
            layoutParent.append(content);
        }
        return true;
    }

    private boolean compileLiteralAutolinks(LytFlowParent layoutParent, String text) {
        if (!MarkdownLiteralAutolink.mayContainLiteralAutolink(text)) {
            return false;
        }

        List<MarkdownLiteralAutolink.Segment> segments = MarkdownLiteralAutolink.split(text);
        boolean foundLink = false;
        for (var segment : segments) {
            if (segment.isLink()) {
                foundLink = true;
                LytFlowLink link = new LytFlowLink();
                link.appendText(segment.text());
                link.setExternalUrl(MarkdownLiteralAutolink.toUri(segment.href()));
                layoutParent.append(link);
            } else if (!segment.text()
                .isEmpty()) {
                    layoutParent.appendText(segment.text());
                }
        }
        return foundLink;
    }

    private boolean compileInlineDollarLatex(LytFlowParent layoutParent, String text) {
        if (!MarkdownLatexShorthand.mayContain(text)) {
            return false;
        }
        List<MarkdownLatexShorthand.Segment> segments = MarkdownLatexShorthand.split(text);
        boolean foundFormula = false;
        for (var segment : segments) {
            if (segment.isFormula()) {
                foundFormula = true;
                var block = new LytLatexBlock(
                    segment.getValue(),
                    LatexRenderOptions.builder()
                        .valign(LatexVerticalAlign.BASELINE)
                        .build());
                layoutParent.append(LytFlowInlineBlock.of(block));
            } else if (!segment.getValue()
                .isEmpty()) {
                    layoutParent.appendText(segment.getValue());
                }
        }
        return foundFormula;
    }

    private List<Integer> parseWidthHintsFromMetaExpression(String metaExpression) {
        for (String token : splitMetaTokens(metaExpression)) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex == token.length() - 1) {
                continue;
            }

            String key = token.substring(0, equalsIndex);
            String value = stripOptionalQuotes(token.substring(equalsIndex + 1));
            if ("widths".equals(key)) {
                return CsvTableCompiler.parseWidthHints(value);
            }
        }
        return Collections.emptyList();
    }

    private List<String> splitMetaTokens(String meta) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quote = 0;
        for (int i = 0; i < meta.length(); i++) {
            char ch = meta.charAt(i);
            if ((ch == '"' || ch == '\'') && (!inQuotes || ch == quote)) {
                if (inQuotes && ch == quote) {
                    inQuotes = false;
                    quote = 0;
                } else if (!inQuotes) {
                    inQuotes = true;
                    quote = ch;
                }
                current.append(ch);
                continue;
            }
            if (Character.isWhitespace(ch) && !inQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private String stripOptionalQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private @Nullable BlockTagChildSource extractBlockTagChildrenSource(MdxJsxElementFields element) {
        String sourceText = getCurrentSourceText();
        String body = MdxBlockTagSourceExtractor.extractRawBody(element, sourceText);
        if (body == null && !Objects.equals(sourceText, pageContent)) {
            body = MdxBlockTagSourceExtractor.extractRawBody(element, pageContent);
        }
        if (body == null) {
            return null;
        }

        return new BlockTagChildSource(DetailsContentExtractor.dedent(body));
    }

    private BlockTagChildrenCacheEntry getBlockTagChildrenCacheEntry(MdxJsxElementFields element) {
        BlockTagChildrenCacheEntry cachedEntry = blockTagChildrenCache.get(element);
        if (cachedEntry != null) {
            return cachedEntry;
        }

        BlockTagChildSource extractedSource = extractBlockTagChildrenSource(element);
        if (extractedSource == null) {
            cachedEntry = new BlockTagChildrenCacheEntry(null, null);
        } else {
            cachedEntry = new BlockTagChildrenCacheEntry(
                extractedSource.source(),
                parse(sourcePack, "en_us", pageId, extractedSource.source()));
        }
        blockTagChildrenCache.put(element, cachedEntry);
        return cachedEntry;
    }

    public LytBlock createErrorBlock(String text, UnistNode child) {
        var paragraph = new LytParagraph();
        paragraph.append(createErrorFlowContent(text, child));
        return paragraph;
    }

    public LytFlowContent createErrorFlowContent(String text, UnistNode child) {
        LytFlowSpan span = new LytFlowSpan();
        span.modifyStyle(
            style -> style.color(SymbolicColor.ERROR_TEXT)
                .whiteSpace(WhiteSpaceMode.PRE));

        // Find the position in the source
        var position = child.position();
        if (position != null) {
            var pos = position.start();
            String sourceText = getCurrentSourceText();
            var startOfLine = sourceText.lastIndexOf('\n', pos.offset()) + 1;
            var endOfLine = sourceText.indexOf('\n', pos.offset() + 1);
            if (endOfLine == -1) {
                endOfLine = sourceText.length();
            }
            var line = sourceText.substring(startOfLine, endOfLine);

            text += " " + child.type() + " (" + MdAstPosition.stringify(pos) + ")";

            span.appendText(text);
            span.appendBreak();

            span.appendText(line);
            span.appendBreak();

            String tildes = new String(new char[pos.column() - 1]).replace('\0', '~');
            span.appendText(tildes + "^");
            span.appendBreak();

            GuideDebugLog.warnAlways("[GuideNH] [PageCompiler] {}\n{}\n{}\n", text, line, tildes + "^");
        } else {
            GuideDebugLog.warnAlways("[GuideNH] [PageCompiler] {}\n", text);
        }

        return span;
    }

    public ResourceLocation resolveId(String idText) {
        return IdUtils.resolveId(idText, pageId.getResourceDomain());
    }

    public ResourceLocation getGuideId() {
        return pages.getId();
    }

    public PageCollection getPageCollection() {
        return pages;
    }

    public boolean pageExistsForLink(ResourceLocation guideId, ResourceLocation pageId) {
        return pageLinkResolver.pageExists(this, guideId, pageId);
    }

    public static void setPageLinkResolver(PageLinkResolver resolver) {
        pageLinkResolver = Objects.requireNonNull(resolver, "resolver");
    }

    public static void resetPageLinkResolver() {
        pageLinkResolver = PageCompiler::defaultPageExistsForLink;
    }

    private static boolean defaultPageExistsForLink(PageCompiler compiler, ResourceLocation guideId,
        ResourceLocation pageId) {
        PageCollection pages = compiler.getPageCollection();
        if (guideId.equals(pages.getId())) {
            return pages.pageExists(pageId);
        }
        var guide = GuideRegistry.getById(guideId);
        return guide != null && guide.pageExists(pageId);
    }

    public interface PageLinkResolver {

        boolean pageExists(PageCompiler compiler, ResourceLocation guideId, ResourceLocation pageId);
    }

    public byte @Nullable [] loadAsset(ResourceLocation imageId) {
        return pages.loadAsset(imageId);
    }

    public <T extends PageIndex> T getIndex(Class<T> clazz) {
        return pages.getIndex(clazz);
    }

    public <T> T getCompilerState(State<T> state) {
        var current = compilerState.getOrDefault(state, state.defaultValue);
        return state.dataClass.cast(current);
    }

    public <T> void setCompilerState(State<T> state, T value) {
        compilerState.put(state, value);
    }

    public <T> void clearCompilerState(State<T> state) {
        compilerState.remove(state);
    }

    public String getCurrentSourceText() {
        List<SourceSlice> sourceSlices = getCompilerState(SOURCE_SLICE_STACK);
        if (!sourceSlices.isEmpty()) {
            return sourceSlices.getLast()
                .source();
        }
        return pageContent;
    }

    public void withChildrenSourceContext(List<? extends MdAstAnyContent> children, Runnable action) {
        String sourceText = sourceForChildren(children);
        if (sourceText == null) {
            action.run();
            return;
        }
        withSourceSlice(sourceText, action);
    }

    private @Nullable String sourceForChildren(List<? extends MdAstAnyContent> children) {
        if (children == null || children.isEmpty()) {
            return null;
        }

        UnistPosition firstPosition = null;
        UnistPosition lastPosition = null;
        for (MdAstAnyContent child : children) {
            UnistPosition position = child.position();
            if (position == null || position.start() == null || position.end() == null) {
                return null;
            }
            if (firstPosition == null) {
                firstPosition = position;
            }
            lastPosition = position;
        }

        if (firstPosition == null || lastPosition == null) {
            return null;
        }

        String sourceText = getCurrentSourceText();
        int sourceStart = firstPosition.start()
            .offset();
        int sourceEnd = lastPosition.end()
            .offset();
        if (sourceStart < 0 || sourceEnd <= sourceStart || sourceEnd > sourceText.length()) {
            return null;
        }
        return sourceText.substring(sourceStart, sourceEnd);
    }

    private void withSourceSlice(String sourceText, Runnable action) {
        List<SourceSlice> currentSlices = getCompilerState(SOURCE_SLICE_STACK);
        List<SourceSlice> nextSlices = new ArrayList<>(currentSlices.size() + 1);
        nextSlices.addAll(currentSlices);
        nextSlices.add(new SourceSlice(sourceText));
        setCompilerState(SOURCE_SLICE_STACK, nextSlices);
        try {
            action.run();
        } finally {
            if (currentSlices.isEmpty()) {
                clearCompilerState(SOURCE_SLICE_STACK);
            } else {
                setCompilerState(SOURCE_SLICE_STACK, currentSlices);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> castClass(Class<?> rawClass) {
        return (Class<T>) rawClass;
    }

    @Desugar
    private record BlockTagChildSource(String source) {}

    @Desugar
    private record BlockTagChildrenCacheEntry(@Nullable String source, @Nullable ParsedGuidePage parsedPage) {}

    @Desugar
    private record SourceSlice(String source) {}

    @Desugar
    public record State<T> (String name, Class<T> dataClass, T defaultValue) {}

    // ---- § color/format code parsing ----

    /**
     * Parses Minecraft § color/format codes in {@code text} and returns a list of
     * styled flow content fragments (plain {@link LytFlowText} or {@link LytFlowSpan}
     * wrapping a text node).
     */
    private static List<LytFlowContent> parseSectionFormatting(String text) {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        List<LytFlowContent> result = new ArrayList<>();
        StringBuilder segment = new StringBuilder();

        // Current style state.  Boolean null = inherit/not set.
        ConstantColor color = null;
        Boolean bold = null;
        Boolean italic = null;
        Boolean underlined = null;
        Boolean strikethrough = null;
        Boolean obfuscated = null;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '§' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                int mappedColor = mapSectionColor(code);
                if (mappedColor != -1 || isSectionFormatCode(code)) {
                    // Valid § code – flush current segment and apply
                    flushSectionSegment(result, segment, color, bold, italic, underlined, strikethrough, obfuscated);

                    if (mappedColor != -1) {
                        // §0-§f color: reset all formatting and set colour
                        color = new ConstantColor(mappedColor);
                        bold = false;
                        italic = false;
                        underlined = false;
                        strikethrough = false;
                        obfuscated = false;
                    } else {
                        // §k-§o, §r: format code
                        switch (Character.toLowerCase(code)) {
                            case 'l' -> bold = true;
                            case 'o' -> italic = true;
                            case 'm' -> strikethrough = true;
                            case 'n' -> underlined = true;
                            case 'k' -> obfuscated = true;
                            case 'r' -> {
                                color = null;
                                bold = null;
                                italic = null;
                                underlined = null;
                                strikethrough = null;
                                obfuscated = null;
                            }
                            default -> { /* unreachable – isSectionFormatCode already validated */ }
                        }
                    }
                    i++; // skip the format-code character
                    continue;
                }
            }
            segment.append(ch);
        }

        flushSectionSegment(result, segment, color, bold, italic, underlined, strikethrough, obfuscated);
        return result;
    }

    /** Appends the accumulated {@code segment} text as either plain or styled flow content. */
    private static void flushSectionSegment(List<LytFlowContent> result, StringBuilder segment,
            ConstantColor color, Boolean bold, Boolean italic, Boolean underlined,
            Boolean strikethrough, Boolean obfuscated) {
        if (segment.isEmpty()) {
            return;
        }
        String text = segment.toString();
        segment.setLength(0);

        if (color == null && bold == null && italic == null && underlined == null
            && strikethrough == null && obfuscated == null) {
            result.add(LytFlowText.of(text));
            return;
        }

        var span = new LytFlowSpan();
        var builder = TextStyle.builder();
        if (color != null) {
            builder = builder.color(color);
        }
        if (bold != null) {
            builder = builder.bold(bold);
        }
        if (italic != null) {
            builder = builder.italic(italic);
        }
        if (underlined != null) {
            builder = builder.underlined(underlined);
        }
        if (strikethrough != null) {
            builder = builder.strikethrough(strikethrough);
        }
        if (obfuscated != null) {
            builder = builder.obfuscated(obfuscated);
        }
        span.setStyle(builder.build());
        span.appendText(text);
        result.add(span);
    }

    /** Returns ARGB color int for §0-§f, or -1 if {@code code} is not a colour code. */
    private static int mapSectionColor(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> 0xFF000000; // Black
            case '1' -> 0xFF0000AA; // Dark Blue
            case '2' -> 0xFF00AA00; // Dark Green
            case '3' -> 0xFF00AAAA; // Dark Aqua
            case '4' -> 0xFFAA0000; // Dark Red
            case '5' -> 0xFFAA00AA; // Dark Purple
            case '6' -> 0xFFFFAA00; // Gold
            case '7' -> 0xFFAAAAAA; // Gray
            case '8' -> 0xFF555555; // Dark Gray
            case '9' -> 0xFF5555FF; // Blue
            case 'a' -> 0xFF55FF55; // Green
            case 'b' -> 0xFF55FFFF; // Aqua
            case 'c' -> 0xFFFF5555; // Red
            case 'd' -> 0xFFFF55FF; // Light Purple
            case 'e' -> 0xFFFFFF55; // Yellow
            case 'f' -> 0xFFFFFFFF; // White
            default -> -1;
        };
    }

    /** Returns true for §k/l/m/n/o/r (format codes, not colour codes). */
    private static boolean isSectionFormatCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case 'k', 'l', 'm', 'n', 'o', 'r' -> true;
            default -> false;
        };
    }
}
