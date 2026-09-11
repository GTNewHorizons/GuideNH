package com.hfstudio.guidenh.guide.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

/**
 * The syntax a guide's editor can complete, assembled from two plugin sources:
 *
 * <ol>
 * <li>every registered {@link TagCompiler} and {@link SceneElementTagCompiler} publishes its tag names,
 * so a mod that adds a tag compiler gets tag completion without touching autocomplete at all;
 * <li>every registered {@link SyntaxContributor} adds the facts a compiler does not express - container
 * shape, child tags, attributes and their value kinds - plus markdown snippets, fence names,
 * frontmatter keys and {@link SyntaxValueSource value sources}.
 * </ol>
 *
 * <p>
 * Contributions come from the guide's {@link SyntaxContributor} extensions - which include the
 * library's own built-in syntax - and from {@link GuideNhIntegrationRegistry} registrations that apply
 * to every guide.
 *
 * <p>
 * Models are immutable, so one is built once per extension collection and reused until the global
 * registrations change.
 */
public class GuideSyntaxModel {

    /**
     * Models are cached per extension collection while it is still reachable, so building one costs
     * once per guide instead of once per keystroke. The cache holds its keys weakly: a guide reload
     * builds a new collection and the entry for the replaced one disappears with it.
     */
    private static final Map<ExtensionCollection, GuideSyntaxModel> CACHE = Collections
        .synchronizedMap(new WeakHashMap<>());

    private final int revision;
    private final Map<String, TagFact> tags;
    private final List<String> rootTags;
    private final List<MarkdownSnippet> markdownSnippets;
    private final List<String> fenceLanguages;
    private final List<String> frontmatterKeys;
    private final Map<String, ValueSlot> frontmatterValues;
    private final Map<SyntaxValueKind, List<SyntaxValueSource>> valueSources;
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
        this.insertTemplates = Collections.unmodifiableMap(builder.insertTemplates);
        this.slots = List.copyOf(slots);
    }

    /**
     * The model used when no guide is open: only what is registered globally, with no guide-declared
     * syntax. It observes later global registrations like any other model.
     */
    public static GuideSyntaxModel empty() {
        return of(null);
    }

    /** The model for a guide, built once and reused until the global registrations change. */
    public static GuideSyntaxModel of(@Nullable ExtensionCollection extensions) {
        ExtensionCollection key = extensions != null ? extensions : ExtensionCollection.empty();
        int revision = GuideNhIntegrationRegistry.global()
            .syntaxRevision();
        synchronized (CACHE) {
            GuideSyntaxModel cached = CACHE.get(key);
            if (cached != null && cached.revision == revision) {
                return cached;
            }
            GuideSyntaxModel model = build(key);
            CACHE.put(key, model);
            return model;
        }
    }

    private static GuideSyntaxModel build(ExtensionCollection extensions) {
        int revision = GuideNhIntegrationRegistry.global()
            .syntaxRevision();
        Builder builder = new Builder();
        for (SyntaxContributor contributor : contributors(extensions)) {
            contributor.contribute(builder);
        }
        collectCompilerTagNames(builder, extensions);
        return new GuideSyntaxModel(builder, revision, slots(extensions));
    }

    /**
     * The slots a contributor owns, guide-declared first: matching stops at the first slot that claims
     * the caret, so the guide's own syntax answers before a global one.
     */
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
        // Globally registered contributors come first, so a guide's own contributors override theirs.
        List<SyntaxContributor> all = new ArrayList<>(global.size() + declared.size());
        all.addAll(global);
        all.addAll(declared);
        return all;
    }

    /**
     * Tags declared by the compilers themselves. This is what makes a third-party tag compiler appear in
     * completion without knowing anything about the editor.
     */
    private static void collectCompilerTagNames(Builder builder, ExtensionCollection extensions) {
        for (TagCompiler compiler : extensions.get(TagCompiler.EXTENSION_POINT)) {
            for (String tagName : compiler.getTagNames()) {
                builder.declareCompilerTag(tagName);
            }
        }
        for (SceneElementTagCompiler compiler : extensions.get(SceneElementTagCompiler.EXTENSION_POINT)) {
            for (String tagName : compiler.getTagNames()) {
                builder.declareCompilerTag(tagName);
            }
        }
    }

    /** Tag names offered inside {@code parentTagName}, filtered by {@code partial}. */
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

    /** True when the tag wraps content and is completed as {@code <Name></Name>}. */
    public boolean isContainerTag(String tagName) {
        TagFact fact = tags.get(tagName);
        return fact != null && fact.container;
    }

    /** The text a tag completes as, or null when completing its name is enough. */
    @Nullable
    public InsertTemplate insertTemplate(@Nullable String tagName) {
        return tagName != null ? insertTemplates.get(tagName) : null;
    }

    /** Every declared insert template, keyed by tag name. */
    public Map<String, InsertTemplate> insertTemplates() {
        return insertTemplates;
    }

    /** The slots that may own a caret, in the order they are asked. */
    public List<SyntaxSlot> slots() {
        return slots;
    }

    /**
     * Asks every slot which one owns the caret. The first match wins, so a contributor's syntax answers
     * before the editor's own resolvers, and a slot that claims text keeps it even when it has no values.
     *
     * @return the slot under the caret, or null when no slot owns it
     */
    @Nullable
    public SyntaxSlotMatch matchSlot(String text, int cursorIndex) {
        for (SyntaxSlot slot : slots) {
            SyntaxSlotMatch match = matchSafely(slot, text, cursorIndex);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /**
     * Asks every matching slot which range a double click inside it selects.
     *
     * @return the range to select, or null when no slot owns the caret
     */
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

    /** A slot of another mod must never break the editor, so a failure only skips that slot. */
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
        GuideDebugLog.error("[GuideNH] [SyntaxSlot] {} failed to answer: {}", slot.namespace(), failure.toString());
    }

    /** Attributes of a tag whose names start with {@code partial}. */
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

    /** Fence names starting with {@code partial}. */
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

    /** Frontmatter keys whose name contains {@code partial}. */
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

    /** The value kind declared for a frontmatter key, or null when it has none. */
    @Nullable
    public SyntaxValueKind frontmatterKind(@Nullable String key) {
        ValueSlot slot = key != null ? frontmatterValues.get(key) : null;
        return slot != null ? slot.kind : null;
    }

    /**
     * Values for a request: the fixed values declared next to the attribute or frontmatter key first,
     * then everything the registered sources for the request's kind produce.
     */
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
        for (SyntaxValueSource source : valueSources.getOrDefault(request.kind(), List.of())) {
            if (results.size() >= safeLimit) {
                break;
            }
            List<SyntaxSuggestion> suggested = source.suggest(request, safeLimit - results.size());
            if (suggested != null) {
                results.addAll(suggested);
            }
        }
        return results;
    }

    /**
     * Constants of an enum declared next to the attribute. An {@code ENUM} attribute does not need a
     * value source: the compiler-facing enum is the single source of truth for the allowed values.
     */
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

    /** Lets sources that need live data refresh it before a query. */
    public void prepare(SyntaxEnvironment environment) {
        Set<SyntaxValueSource> prepared = Collections.newSetFromMap(new IdentityHashMap<>());
        for (List<SyntaxValueSource> sources : valueSources.values()) {
            for (SyntaxValueSource source : sources) {
                if (source instanceof SyntaxEnvironmentAware aware && prepared.add(source)) {
                    aware.prepare(environment);
                }
            }
        }
    }

    /** Internal tag facts. Tag names come from compiler and contributor registration. */
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

    /** Collects contributions. Contributors only ever see the {@link SyntaxSink} surface. */
    private static final class Builder implements SyntaxSink {

        private final Map<String, Boolean> containers = new LinkedHashMap<>();
        private final Map<String, Set<String>> children = new LinkedHashMap<>();
        private final Map<String, Map<String, AttributeSyntax>> attributes = new LinkedHashMap<>();
        private final Set<String> hidden = new LinkedHashSet<>();
        private final List<MarkdownSnippet> markdownSnippets = new ArrayList<>();
        private final List<String> fenceLanguages = new ArrayList<>();
        private final List<String> frontmatterKeys = new ArrayList<>();
        private final Map<String, ValueSlot> frontmatterValues = new LinkedHashMap<>();
        private final Map<SyntaxValueKind, List<SyntaxValueSource>> valueSources = new LinkedHashMap<>();
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
            for (SyntaxValueKind kind : source.kinds()) {
                valueSources.computeIfAbsent(kind, ignored -> new ArrayList<>())
                    .add(source);
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
            // A contributor may declare attributes for a tag it never lists, so include those too.
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
