package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.extensions.DefaultExtensions;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

/**
 * Compiles pages against a set of template pages the way the client does, so template behaviour can be
 * asserted without a running game. A template's body is read from the page collection during compilation,
 * so the collection has to hold the template pages.
 */
public class MediaWikiTemplateTestHarness {

    public static final String PACK = "guidenh";
    public static final String TEMPLATE_FOLDER = "guidenh/_en_us/templates/";

    private final Map<ResourceLocation, ParsedGuidePage> pages = new LinkedHashMap<>();

    /** Registers a template under the page id its name maps to. */
    public MediaWikiTemplateTestHarness defineTemplate(String name, String body) {
        return definePage(TEMPLATE_FOLDER + name + ".md", body);
    }

    /** Registers a page by path, used both for templates and for pages a template may test for. */
    public MediaWikiTemplateTestHarness definePage(String path, String body) {
        ResourceLocation pageId = new ResourceLocation(PACK, path);
        pages.put(pageId, PageCompiler.parse(PACK, "en_us", pageId, body));
        return this;
    }

    /** Compiles source as an ordinary page and returns the laid-out result. */
    public GuidePage compile(String source) {
        return compilePage("guidenh/probe.md", source);
    }

    public GuidePage compilePage(String path, String source) {
        publishTemplates();
        ResourceLocation pageId = new ResourceLocation(PACK, path);
        ParsedGuidePage parsed = PageCompiler.parse(PACK, "en_us", pageId, source);
        return PageCompiler.compile(new MapPageCollection(pages), extensions(), parsed);
    }

    /** The parsed page without compiling, for assertions about parse failures. */
    public ParsedGuidePage parse(String source) {
        return PageCompiler.parse(PACK, "en_us", new ResourceLocation(PACK, "guidenh/probe.md"), source);
    }

    /** Publishes the registered templates so the compiler can resolve them by name. */
    public void publishTemplates() {
        List<MediaWikiTemplateRepository.TemplatePageSource> sources = new ArrayList<>();
        for (ResourceLocation pageId : pages.keySet()) {
            if (MediaWikiTemplatePageIds.isTemplatePage(pageId)) {
                sources.add(new MediaWikiTemplateRepository.TemplatePageSource(PACK, "en_us", pageId));
            }
        }
        MediaWikiTemplateRepository.rebuild(sources);
    }

    /** The visible text of a compiled page, which is what a reader would see. */
    public static String text(GuidePage page) {
        String extracted = page.document()
            .getTextContent();
        return extracted != null ? extracted : "";
    }

    /** Compiles one page against fresh templates and returns its visible text. */
    public static String textOf(String source) {
        return text(new MediaWikiTemplateTestHarness().compile(source));
    }

    /** The visible text of a page compiled by this harness, for a one-line assertion. */
    public String textOfPage(String source) {
        return text(compile(source));
    }

    public static ResourceLocation templatePageId(String name) {
        return new ResourceLocation(PACK, TEMPLATE_FOLDER + name + ".md");
    }

    /**
     * The real tag set, because a template body may emit any tag and so may the page around a call; a
     * narrower set would report those tags as unhandled instead of testing template behaviour.
     */
    private static ExtensionCollection extensions() {
        ExtensionCollection.Builder builder = ExtensionCollection.builder();
        DefaultExtensions.addAll(builder, Set.of());
        return builder.build();
    }

    /** A read-only page collection over an explicit page map. */
    private static final class MapPageCollection implements PageCollection {

        private final Map<ResourceLocation, ParsedGuidePage> pages;

        MapPageCollection(Map<ResourceLocation, ParsedGuidePage> pages) {
            this.pages = pages;
        }

        @Override
        public <T extends PageIndex> T getIndex(Class<T> indexClass) {
            return null;
        }

        @Override
        public Collection<ParsedGuidePage> getPages() {
            return List.copyOf(pages.values());
        }

        @Override
        public ParsedGuidePage getParsedPage(ResourceLocation id) {
            return pages.get(id);
        }

        @Override
        public GuidePage getPage(ResourceLocation id) {
            return null;
        }

        @Override
        public byte[] loadAsset(ResourceLocation id) {
            return null;
        }

        @Override
        public NavigationTree getNavigationTree() {
            return new NavigationTree();
        }

        @Override
        public boolean pageExists(ResourceLocation pageId) {
            return pages.containsKey(pageId);
        }
    }
}
