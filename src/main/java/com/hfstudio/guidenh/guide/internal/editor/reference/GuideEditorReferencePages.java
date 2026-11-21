package com.hfstudio.guidenh.guide.internal.editor.reference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowAnchor;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

public class GuideEditorReferencePages implements Guide {

    private static final ResourceLocation ID = new ResourceLocation("guidenh", "editor-reference");
    private final Guide guide;
    private final GuideEditorReferenceCatalog catalog;
    private final Map<ResourceLocation, GuideEditorReferenceSection> sections = new LinkedHashMap<>();
    private final Map<String, ParsedGuidePage> documents = new LinkedHashMap<>();
    private final Map<String, GuidePage> compiledDocuments = new LinkedHashMap<>();
    private final Map<GuideEditorReferenceSection, ResourceLocation> sectionIds = new LinkedHashMap<>();
    private final NavigationTree navigation;

    public GuideEditorReferencePages(Guide guide, GuideEditorReferenceCatalog catalog, String language) {
        this.guide = guide;
        this.catalog = catalog;
        Map<String, List<GuideEditorReferenceSection>> grouped = new LinkedHashMap<>();
        for (var section : catalog.sections()) {
            grouped.computeIfAbsent(section.documentId(), key -> new ArrayList<>())
                .add(section);
        }
        Map<ResourceLocation, NavigationNode> nodes = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> parents = new LinkedHashMap<>();
        List<NavigationNode> roots = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            String documentId = entry.getKey();
            var topics = entry.getValue();
            ResourceLocation rootId = pageId(documentId, 0);
            StringBuilder source = new StringBuilder();
            List<NavigationNode> children = new ArrayList<>();
            for (int i = 0; i < topics.size(); i++) {
                var topic = topics.get(i);
                ResourceLocation id = pageId(documentId, i);
                sections.put(id, topic);
                sectionIds.put(topic, id);
                source.append(topic.markdown());
                if (i > 0) {
                    var node = new NavigationNode(ID, id, topic.title(), null, List.of(), i, true);
                    children.add(node);
                    nodes.put(id, node);
                    parents.put(id, rootId);
                }
            }
            documents.put(
                documentId,
                PageCompiler.parse("GuideNH authoring reference", language, rootId, source.toString()));
            var root = new NavigationNode(
                ID,
                rootId,
                topics.getFirst()
                    .documentTitle(),
                null,
                children,
                roots.size(),
                true);
            roots.add(root);
            nodes.put(rootId, root);
        }
        navigation = new NavigationTree(nodes, parents, roots);
    }

    private static ResourceLocation pageId(String documentId, int index) {
        return new ResourceLocation("guidenh", "editor-reference/" + documentId + "/" + index);
    }

    public PageAnchor initialAnchor() {
        return sections.isEmpty() ? PageAnchor.page(new ResourceLocation("guidenh", "editor-reference/empty"))
            : resolveAnchor(
                PageAnchor.page(
                    sections.keySet()
                        .iterator()
                        .next()));
    }

    public @Nullable GuideEditorReferenceSection section(ResourceLocation id) {
        return sections.get(id);
    }

    public @Nullable PageAnchor resolveAnchor(PageAnchor anchor) {
        var section = sections.get(anchor.pageId());
        if (section == null) {
            section = catalog.find(
                anchor.pageId()
                    .getResourcePath(),
                anchor.anchor());
        }
        if (section == null) {
            return null;
        }
        String fragment = anchor.anchor();
        if (fragment == null && !section.anchors()
            .isEmpty()) {
            fragment = section.anchors()
                .getFirst();
        }
        return new PageAnchor(sectionIds.get(section), fragment);
    }

    public String searchText(NavigationNode node) {
        var section = sections.get(node.pageId());
        return section == null ? node.title() : section.searchText();
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public String getDefaultNamespace() {
        return guide.getDefaultNamespace();
    }

    @Override
    public String getContentRootFolder() {
        return guide.getContentRootFolder();
    }

    @Override
    public ExtensionCollection getExtensions() {
        return guide.getExtensions();
    }

    @Override
    public <T extends PageIndex> T getIndex(Class<T> indexClass) {
        return guide.getIndex(indexClass);
    }

    @Override
    public Collection<ParsedGuidePage> getPages() {
        return documents.values();
    }

    @Override
    public @Nullable ParsedGuidePage getParsedPage(ResourceLocation id) {
        var section = sections.get(id);
        return section == null ? null : documents.get(section.documentId());
    }

    @Override
    public @Nullable GuidePage getPage(ResourceLocation id) {
        var parsed = getParsedPage(id);
        if (parsed == null) return null;
        String documentId = sections.get(id)
            .documentId();
        return compiledDocuments.computeIfAbsent(documentId, key -> compileDocument(parsed));
    }

    private GuidePage compileDocument(ParsedGuidePage parsed) {
        var page = PageCompiler.compile(this, getExtensions(), parsed);
        page.document()
            .visit(new LytVisitor() {

                @Override
                public Result beforeNode(LytNode node) {
                    if (node instanceof LytHeading heading) {
                        heading.append(
                            new LytFlowAnchor(GuideEditorReferenceCatalog.headingAnchor(heading.getTextContent())));
                    }
                    return Result.CONTINUE;
                }
            });
        return page;
    }

    @Override
    public byte[] loadAsset(ResourceLocation id) {
        return guide.loadAsset(id);
    }

    @Override
    public NavigationTree getNavigationTree() {
        return navigation;
    }

    @Override
    public boolean pageExists(ResourceLocation pageId) {
        return sections.containsKey(pageId) || catalog.find(pageId.getResourcePath(), null) != null;
    }
}
