package com.hfstudio.guidenh.guide.syntax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

public class GuideSyntaxModel {

    private static final Map<ExtensionCollection, GuideSyntaxModel> CACHE = Collections
        .synchronizedMap(new WeakHashMap<>());

    private final int revision;
    private final Map<String, TagFact> tags;
    private final List<String> rootTags;
    private final List<MarkdownSnippet> markdownSnippets;
    private final List<String> fenceLanguages;
    private final List<String> frontmatterKeys;
    private final Map<String, ValueSlot> frontmatterValues;
    private final Map<String, List<SyntaxValueSource>> valueSources;
    private final List<SyntaxEnvironmentAware> environmentAwareSources;
    private final Map<String, InsertTemplate> insertTemplates;
    private final List<SyntaxSlot> slots;

    private GuideSyntaxModel(Builder builder, int revision, List<SyntaxSlot> slots) {
        this.revision = revision;
        this.tags = Collections.unmodifiableMap(builder.buildTagFacts());
        List<String> roots = new ArrayList<>();
        for (Map.Entry<String, TagFact> entry : tags.entrySet()) {
            if (!entry.getValue().hidden) {
                roots.add(entry.getKey());
            }
        }
        this.rootTags = Collections.unmodifiableList(roots);
        this.markdownSnippets = List.copyOf(builder.markdownSnippets);
        this.fenceLanguages = List.copyOf(builder.fenceLanguages);
        this.frontmatterKeys = List.copyOf(builder.frontmatterKeys);
        this.frontmatterValues = Collections.unmodifiableMap(builder.frontmatterValues);
        this.valueSources = Collections.unmodifiableMap(builder.valueSources);
        this.environmentAwareSources = collectEnvironmentAwareSources(builder.valueSources);
        this.insertTemplates = Collections.unmodifiableMap(builder.insertTemplates);
        this.slots = List.copyOf(slots);
    }

    public static GuideSyntaxModel empty() {
        return of(null);
    }

    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    public static GuideSyntaxModel of(@Nullable ExtensionCollection extensions) {
        ExtensionCollection key = extensions != null ? extensions : ExtensionCollection.empty();
        int revision = GuideNhIntegrationRegistry.global()
            .syntaxRevision();
        synchronized (CACHE) {
            GuideSyntaxModel cached = CACHE.get(key);
            if (cached != null && cached.revision == revision) {
                return cached;
            }
        }
        GuideSyntaxModel model = build(key, revision);
        synchronized (CACHE) {
            CACHE.put(key, model);
        }
        return model;
    }

    private static GuideSyntaxModel build(ExtensionCollection extensions, int revision) {
        Builder builder = new Builder();
        for (SyntaxContributor contributor : contributors(extensions)) {
            try {
                contributor.contribute(builder);
            } catch (RuntimeException e) {
                GuideDebugLog.error(
                    "[GuideNH] [SyntaxContributor] {} failed to contribute: {}",
                    namespaceOf(contributor),
                    e.toString());
            }
        }
        collectCompilerTagNames(builder, extensions);
        return new GuideSyntaxModel(builder, revision, slots(extensions));
    }

    private static String namespaceOf(SyntaxContributor contributor) {
        return namespaceOf(contributor, contributor::namespace);
    }

    private static String namespaceOf(SyntaxSlot slot) {
        return namespaceOf(slot, slot::namespace);
    }

    private static String namespaceOf(Object owner, Supplier<String> namespace) {
        try {
            String declared = namespace.get();
            if (declared != null && !declared.isEmpty()) {
                return declared;
            }
        } catch (RuntimeException e) {}
        return owner.getClass()
            .getSimpleName();
    }

    private static List<SyntaxSlot> slots(ExtensionCollection extensions) {
        List<SyntaxSlot> declared = extensions.get(SyntaxSlot.EXTENSION_POINT);
        List<SyntaxSlot> global = GuideNhIntegrationRegistry.global()
            .syntaxSlots();
        if (global.isEmpty()) {
            return declared;
        }
        if (declared.isEmpty()) {
            return global;
        }
        List<SyntaxSlot> all = new ArrayList<>(declared.size() + global.size());
        all.addAll(declared);
        all.addAll(global);
        return all;
    }

    private static List<SyntaxContributor> contributors(ExtensionCollection extensions) {
        List<SyntaxContributor> global = GuideNhIntegrationRegistry.global()
            .syntaxContributors();
        List<SyntaxContributor> declared = extensions.get(SyntaxContributor.EXTENSION_POINT);
        if (global.isEmpty()) {
            return declared;
        }
        if (declared.isEmpty()) {
            return global;
        }
        List<SyntaxContributor> all = new ArrayList<>(global.size() + declared.size());
        all.addAll(global);
        all.addAll(declared);
        return all;
    }

    private static void collectCompilerTagNames(Builder builder, ExtensionCollection extensions) {
        for (TagCompiler compiler : extensions.get(TagCompiler.EXTENSION_POINT)) {
            declareCompilerTagNames(builder, compiler, compiler::getTagNames);
        }
        for (SceneElementTagCompiler compiler : extensions.get(SceneElementTagCompiler.EXTENSION_POINT)) {
            declareCompilerTagNames(builder, compiler, compiler::getTagNames);
        }
    }

    private static void declareCompilerTagNames(Builder builder, Object owner, Supplier<Collection<String>> tagNames) {
        Collection<String> published;
        try {
            published = tagNames.get();
        } catch (RuntimeException e) {
            GuideDebugLog.error(
                "[GuideNH] [SyntaxModel] {} failed to publish its tags: {}",
                owner.getClass()
                    .getSimpleName(),
                e.toString());
            return;
        }
        if (published == null) {
            return;
        }
        for (String tagName : published) {
            if (tagName != null && !tagName.isEmpty()) {
                builder.declareCompilerTag(tagName);
            }
        }
    }

    public List<String> tagNames(@Nullable String parentTagName, @Nullable String partial) {
        List<String> candidates = childTags(parentTagName);
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<String> results = new ArrayList<>();
        for (String tagName : candidates) {
            if (isHidden(tagName)) {
                continue;
            }
            if (lower.isEmpty() || tagName.toLowerCase(Locale.ROOT)
                .startsWith(lower)) {
                results.add(tagName);
            }
        }
        return results;
    }

    private boolean isHidden(String tagName) {
        TagFact fact = tags.get(tagName);
        return fact != null && fact.hidden;
    }

    private List<String> childTags(@Nullable String parentTagName) {
        TagFact parent = parentTagName != null ? tags.get(parentTagName) : null;
        if (parent == null || parent.children.isEmpty()) {
            return rootTags;
        }
        return parent.children;
    }

    public boolean isContainerTag(String tagName) {
        TagFact fact = tags.get(tagName);
        return fact != null && fact.container;
    }

    @Nullable
    public InsertTemplate insertTemplate(@Nullable String tagName) {
        return tagName != null ? insertTemplates.get(tagName) : null;
    }

    public Map<String, InsertTemplate> insertTemplates() {
        return insertTemplates;
    }

    public List<SyntaxSlot> slots() {
        return slots;
    }

    public record SlotMatch(SyntaxSlot slot, SyntaxSlotMatch match) {}

    @Nullable
    public SlotMatch matchSlot(String text, int cursorIndex) {
        for (SyntaxSlot slot : slots) {
            SyntaxSlotMatch match = matchSafely(slot, text, cursorIndex);
            if (match != null) {
                return new SlotMatch(slot, match);
            }
        }
        return null;
    }

    @Nullable
    public SyntaxSelection matchSlotSelection(String text, int cursorIndex) {
        for (SyntaxSlot slot : slots) {
            try {
                if (slot.match(text, cursorIndex, this) == null) {
                    continue;
                }
                SyntaxSelection selection = slot.selection(text, cursorIndex);
                if (selection != null && !selection.isEmpty()) {
                    return selection;
                }
            } catch (RuntimeException e) {
                reportSlotFailure(slot, e);
            }
        }
        return null;
    }

    @Nullable
    private SyntaxSlotMatch matchSafely(SyntaxSlot slot, String text, int cursorIndex) {
        try {
            return slot.match(text, cursorIndex, this);
        } catch (RuntimeException e) {
            reportSlotFailure(slot, e);
            return null;
        }
    }

    private static void reportSlotFailure(SyntaxSlot slot, RuntimeException failure) {
        GuideDebugLog.error("[GuideNH] [SyntaxSlot] {} failed to answer: {}", namespaceOf(slot), failure.toString());
    }

    public List<AttributeSyntax> attributes(@Nullable String tagName, @Nullable String partial) {
        TagFact fact = tagName != null ? tags.get(tagName) : null;
        if (fact == null || fact.attributes.isEmpty()) {
            return List.of();
        }
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<AttributeSyntax> results = new ArrayList<>();
        for (AttributeSyntax attribute : fact.attributes) {
            if (lower.isEmpty() || attribute.name()
                .toLowerCase(Locale.ROOT)
                .startsWith(lower)) {
                results.add(attribute);
            }
        }
        return results;
    }

    @Nullable
    public AttributeSyntax attribute(@Nullable String tagName, @Nullable String attributeName) {
        TagFact fact = tagName != null ? tags.get(tagName) : null;
        if (fact == null || attributeName == null) {
            return null;
        }
        for (AttributeSyntax attribute : fact.attributes) {
            if (attribute.name()
                .equals(attributeName)) {
                return attribute;
            }
        }
        return null;
    }

    public List<MarkdownSnippet> markdownSnippets() {
        return markdownSnippets;
    }

    public List<String> fenceLanguages(@Nullable String partial) {
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<String> results = new ArrayList<>();
        for (String language : fenceLanguages) {
            if (lower.isEmpty() || language.toLowerCase(Locale.ROOT)
                .startsWith(lower)) {
                results.add(language);
            }
        }
        return results;
    }

    public List<String> frontmatterKeys(@Nullable String partial) {
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<String> results = new ArrayList<>();
        for (String key : frontmatterKeys) {
            if (lower.isEmpty() || key.toLowerCase(Locale.ROOT)
                .contains(lower)) {
                results.add(key);
            }
        }
        return results;
    }

    @Nullable
    public SyntaxValueKind frontmatterKind(@Nullable String key) {
        ValueSlot slot = key != null ? frontmatterValues.get(key) : null;
        return slot != null ? slot.kind : null;
    }

    public List<SyntaxSuggestion> values(SyntaxValueRequest request, int limit) {
        int safeLimit = Math.max(0, limit);
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (SyntaxSuggestion suggestion : declaredValues(request)) {
            if (results.size() >= safeLimit) {
                return results;
            }
            if (matches(suggestion, request.partialText())) {
                results.add(suggestion);
            }
        }
        for (SyntaxSuggestion suggestion : enumValues(request)) {
            if (results.size() >= safeLimit) {
                return results;
            }
            results.add(suggestion);
        }
        for (SyntaxValueSource source : valueSources.getOrDefault(
            request.kind()
                .id(),
            List.of())) {
            if (results.size() >= safeLimit) {
                break;
            }
            results.addAll(suggestSafely(source, request, safeLimit - results.size()));
        }
        return results;
    }

    private static List<SyntaxSuggestion> suggestSafely(SyntaxValueSource source, SyntaxValueRequest request,
        int limit) {
        List<SyntaxSuggestion> suggested;
        try {
            suggested = source.suggest(request, limit);
        } catch (RuntimeException e) {
            GuideDebugLog.error(
                "[GuideNH] [SyntaxValueSource] {} failed to suggest values: {}",
                source.getClass()
                    .getSimpleName(),
                e.toString());
            return List.of();
        }
        if (suggested == null) {
            return List.of();
        }
        List<SyntaxSuggestion> values = new ArrayList<>(suggested.size());
        for (SyntaxSuggestion suggestion : suggested) {
            if (suggestion != null) {
                values.add(suggestion);
            }
        }
        return values;
    }

    private List<SyntaxSuggestion> enumValues(SyntaxValueRequest request) {
        if (request.kind() != SyntaxValueKind.ENUM) {
            return List.of();
        }
        AttributeSyntax attribute = attribute(request.tagName(), request.attributeName());
        Class<? extends Enum<?>> enumType = attribute != null ? attribute.enumType() : null;
        if (enumType == null) {
            return List.of();
        }
        Enum<?>[] constants = enumType.getEnumConstants();
        if (constants == null) {
            return List.of();
        }
        List<SyntaxSuggestion> results = new ArrayList<>(constants.length);
        for (Enum<?> constant : constants) {
            String name = constant.name();
            if (request.partialText()
                .isEmpty()
                || name.toLowerCase(Locale.ROOT)
                    .startsWith(
                        request.partialText()
                            .toLowerCase(Locale.ROOT))) {
                results.add(SyntaxSuggestion.of(name));
            }
        }
        return results;
    }

    private List<SyntaxSuggestion> declaredValues(SyntaxValueRequest request) {
        if (request.frontmatterKey() != null) {
            ValueSlot slot = frontmatterValues.get(request.frontmatterKey());
            return slot != null ? slot.values : List.of();
        }
        AttributeSyntax attribute = attribute(request.tagName(), request.attributeName());
        if (attribute == null || attribute.suggestions()
            .isEmpty()) {
            return List.of();
        }
        List<SyntaxSuggestion> results = new ArrayList<>(
            attribute.suggestions()
                .size());
        for (String value : attribute.suggestions()) {
            results.add(SyntaxSuggestion.of(value));
        }
        return results;
    }

    private static boolean matches(SyntaxSuggestion suggestion, String partial) {
        if (partial == null || partial.isEmpty()) {
            return true;
        }
        return suggestion.value()
            .toLowerCase(Locale.ROOT)
            .contains(partial.toLowerCase(Locale.ROOT));
    }

    public void prepare(SyntaxEnvironment environment) {
        for (SyntaxEnvironmentAware aware : environmentAwareSources) {
            prepareSafely(aware, environment);
        }
    }

    /**
     * The sources that refresh themselves from the environment, collected once so a query does not rebuild
     * the list. A source registered under more than one kind appears once, which is why they are de-duplicated
     * by identity.
     */
    private static List<SyntaxEnvironmentAware> collectEnvironmentAwareSources(
        Map<String, List<SyntaxValueSource>> sources) {
        Set<SyntaxValueSource> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<SyntaxEnvironmentAware> aware = new ArrayList<>();
        for (List<SyntaxValueSource> perKind : sources.values()) {
            for (SyntaxValueSource source : perKind) {
                if (source instanceof SyntaxEnvironmentAware environmentAware && seen.add(source)) {
                    aware.add(environmentAware);
                }
            }
        }
        return List.copyOf(aware);
    }

    private static void prepareSafely(SyntaxEnvironmentAware source, SyntaxEnvironment environment) {
        try {
            source.prepare(environment);
        } catch (RuntimeException e) {
            GuideDebugLog.error(
                "[GuideNH] [SyntaxValueSource] {} failed to refresh: {}",
                source.getClass()
                    .getSimpleName(),
                e.toString());
        }
    }

    private static final class TagFact {

        private final boolean container;
        private final List<String> children;
        private final List<AttributeSyntax> attributes;
        private final boolean hidden;

        private TagFact(boolean container, List<String> children, List<AttributeSyntax> attributes, boolean hidden) {
            this.container = container;
            this.children = children;
            this.attributes = attributes;
            this.hidden = hidden;
        }
    }

    private static final class ValueSlot {

        private final SyntaxValueKind kind;
        private final List<SyntaxSuggestion> values;

        private ValueSlot(SyntaxValueKind kind, List<SyntaxSuggestion> values) {
            this.kind = kind;
            this.values = values;
        }
    }

    private static final class Builder implements SyntaxSink {

        private final Map<String, Boolean> containers = new LinkedHashMap<>();
        private final Map<String, Set<String>> children = new LinkedHashMap<>();
        private final Map<String, Map<String, AttributeSyntax>> attributes = new LinkedHashMap<>();
        private final Set<String> hidden = new LinkedHashSet<>();
        private final List<MarkdownSnippet> markdownSnippets = new ArrayList<>();
        private final List<String> fenceLanguages = new ArrayList<>();
        private final List<String> frontmatterKeys = new ArrayList<>();
        private final Map<String, ValueSlot> frontmatterValues = new LinkedHashMap<>();
        private final Map<String, List<SyntaxValueSource>> valueSources = new LinkedHashMap<>();
        private final Map<String, InsertTemplate> insertTemplates = new LinkedHashMap<>();

        @Override
        public SyntaxSink tags(String... names) {
            for (String name : names) {
                containers.putIfAbsent(name, Boolean.FALSE);
            }
            return this;
        }

        @Override
        public SyntaxSink containerTags(String... names) {
            for (String name : names) {
                containers.put(name, Boolean.TRUE);
            }
            return this;
        }

        @Override
        public SyntaxSink children(String containerTag, String... childTags) {
            children.computeIfAbsent(containerTag, ignored -> new LinkedHashSet<>())
                .addAll(List.of(childTags));
            return this;
        }

        @Override
        public SyntaxSink hiddenTags(String... names) {
            hidden.addAll(List.of(names));
            return this;
        }

        @Override
        public SyntaxSink attributes(String tagName, AttributeSyntax... declared) {
            Map<String, AttributeSyntax> target = attributes.computeIfAbsent(tagName, ignored -> new LinkedHashMap<>());
            for (AttributeSyntax attribute : declared) {
                target.put(attribute.name(), attribute);
            }
            return this;
        }

        @Override
        public SyntaxSink markdown(MarkdownSnippet... snippets) {
            markdownSnippets.addAll(List.of(snippets));
            return this;
        }

        @Override
        public SyntaxSink insertTemplates(InsertTemplate... templates) {
            for (InsertTemplate template : templates) {
                if (!template.tagName()
                    .isEmpty()) {
                    insertTemplates.put(template.tagName(), template);
                }
            }
            return this;
        }

        @Override
        public SyntaxSink fenceLanguages(String... names) {
            for (String name : names) {
                if (!fenceLanguages.contains(name)) {
                    fenceLanguages.add(name);
                }
            }
            return this;
        }

        @Override
        public SyntaxSink frontmatterKeys(String... keys) {
            for (String key : keys) {
                if (!frontmatterKeys.contains(key)) {
                    frontmatterKeys.add(key);
                }
            }
            return this;
        }

        @Override
        public SyntaxSink frontmatterValues(String key, String... values) {
            List<SyntaxSuggestion> suggestions = new ArrayList<>(values.length);
            for (String value : values) {
                suggestions.add(SyntaxSuggestion.of(value));
            }
            ValueSlot existing = frontmatterValues.get(key);
            SyntaxValueKind kind = existing != null ? existing.kind : SyntaxValueKind.STRING;
            frontmatterValues.put(key, new ValueSlot(kind, List.copyOf(suggestions)));
            return this;
        }

        @Override
        public SyntaxSink frontmatterKind(String key, SyntaxValueKind kind) {
            ValueSlot existing = frontmatterValues.get(key);
            frontmatterValues.put(key, new ValueSlot(kind, existing != null ? existing.values : List.of()));
            return this;
        }

        @Override
        public SyntaxSink valueSource(SyntaxValueSource source) {
            Set<SyntaxValueKind> kinds;
            try {
                kinds = source.kinds();
            } catch (RuntimeException e) {
                GuideDebugLog.error(
                    "[GuideNH] [SyntaxValueSource] {} failed to declare its kinds: {}",
                    source.getClass()
                        .getSimpleName(),
                    e.toString());
                return this;
            }
            if (kinds == null) {
                return this;
            }
            for (SyntaxValueKind kind : kinds) {
                if (kind != null) {
                    valueSources.computeIfAbsent(kind.id(), ignored -> new ArrayList<>())
                        .add(source);
                }
            }
            return this;
        }

        private void declareCompilerTag(String tagName) {
            if (tagName != null && !tagName.isEmpty()) {
                containers.putIfAbsent(tagName, Boolean.FALSE);
            }
        }

        private Map<String, TagFact> buildTagFacts() {
            Set<String> names = new LinkedHashSet<>(containers.keySet());
            names.addAll(attributes.keySet());
            names.addAll(children.keySet());
            Map<String, TagFact> result = new LinkedHashMap<>();
            for (String name : names) {
                Boolean isContainer = containers.get(name);
                Set<String> declaredChildren = children.get(name);
                Map<String, AttributeSyntax> declaredAttributes = attributes.get(name);
                result.put(
                    name,
                    new TagFact(
                        isContainer != null && isContainer,
                        declaredChildren != null ? List.copyOf(declaredChildren) : List.of(),
                        declaredAttributes != null ? List.copyOf(declaredAttributes.values()) : List.of(),
                        hidden.contains(name)));
            }
            return result;
        }
    }
}
