package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AttributeNameCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AutocompleteCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.MarkdownSyntaxCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.SyntaxValueCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.TagCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.TextCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FenceLanguageContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MarkdownSyntaxContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxAttrNameContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.TagStartContext;
import com.hfstudio.guidenh.guide.syntax.AttributeSyntax;
import com.hfstudio.guidenh.guide.syntax.GuideSyntaxModel;
import com.hfstudio.guidenh.guide.syntax.MarkdownSnippet;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;

/**
 * Turns the syntax under the caret into completion candidates, using only the information a guide's
 * {@link GuideSyntaxModel} carries. Nothing here knows a specific tag: which tags exist, which
 * attributes they accept and which values those attributes take all come from the model, so a plugin
 * that registers a contributor or a tag compiler changes what the editor offers without touching this
 * class.
 */
public class GuideSyntaxCompletion {

    private static final int RELEVANCE_PREFIX = 0;
    private static final int RELEVANCE_NAMESPACED_PREFIX = 1;
    private static final int RELEVANCE_CONTAINS = 2;
    private static final int RELEVANCE_LAST = 3;

    private GuideSyntaxCompletion() {}

    public static List<AutocompleteCandidate> query(GuideSyntaxModel model, @Nullable TextSyntaxContext syntax,
        int limit) {
        if (model == null || syntax == null || !syntax.shouldAutocomplete()) {
            return List.of();
        }
        AutocompleteContext context = syntax.getAutocomplete();
        List<AutocompleteCandidate> candidates = resolve(model, context, limit);
        if (candidates.isEmpty()) {
            return candidates;
        }
        List<AutocompleteCandidate> filtered = removeRedundant(candidates, context);
        filtered.sort(relevanceOrder(context.getPartialText()));
        return filtered.size() > limit ? new ArrayList<>(filtered.subList(0, limit)) : filtered;
    }

    private static List<AutocompleteCandidate> resolve(GuideSyntaxModel model, AutocompleteContext context, int limit) {
        if (context instanceof TagStartContext tagStart) {
            return tagCandidates(model, tagStart, limit);
        }
        if (context instanceof MdxAttrNameContext attributeName) {
            return attributeCandidates(model, attributeName, limit);
        }
        if (context instanceof MarkdownSyntaxContext markdown) {
            return markdownCandidates(model, markdown, limit);
        }
        if (context instanceof FenceLanguageContext fence) {
            return textCandidates(model.fenceLanguages(fence.getPartialText()), limit);
        }
        if (context instanceof FrontmatterContext frontmatter) {
            return frontmatterCandidates(model, frontmatter, limit);
        }
        if (context instanceof MdxValueContext value) {
            return valueCandidates(model, value, limit);
        }
        return List.of();
    }

    private static List<AutocompleteCandidate> tagCandidates(GuideSyntaxModel model, TagStartContext context,
        int limit) {
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String tagName : model.tagNames(context.getParentTagName(), context.getPartialText())) {
            if (results.size() >= limit) {
                break;
            }
            results.add(new TagCandidate(tagName, model.isContainerTag(tagName)));
        }
        return results;
    }

    private static List<AutocompleteCandidate> attributeCandidates(GuideSyntaxModel model, MdxAttrNameContext context,
        int limit) {
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (AttributeSyntax attribute : model.attributes(context.getTagName(), context.getPartialText())) {
            if (results.size() >= limit) {
                break;
            }
            results.add(new AttributeNameCandidate(attribute));
        }
        return results;
    }

    private static List<AutocompleteCandidate> markdownCandidates(GuideSyntaxModel model, MarkdownSyntaxContext context,
        int limit) {
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (MarkdownSnippet snippet : MarkdownSnippet.matching(model.markdownSnippets(), context.getPartialText())) {
            if (results.size() >= limit) {
                break;
            }
            if (snippet.kind()
                .equals(context.getKind())) {
                results.add(new MarkdownSyntaxCandidate(snippet));
            }
        }
        return results;
    }

    private static List<AutocompleteCandidate> frontmatterCandidates(GuideSyntaxModel model, FrontmatterContext context,
        int limit) {
        if (!context.isValue()) {
            return textCandidates(model.frontmatterKeys(context.getPartialText()), limit);
        }
        SyntaxValueKind kind = model.frontmatterKind(context.getKey());
        if (kind == null) {
            return List.of();
        }
        return suggestionCandidates(
            model.values(SyntaxValueRequest.frontmatter(kind, context.getPartialText(), context.getKey()), limit),
            kind,
            limit);
    }

    private static List<AutocompleteCandidate> valueCandidates(GuideSyntaxModel model, MdxValueContext context,
        int limit) {
        AttributeSyntax attribute = model.attribute(context.getTagName(), context.getAttrName());
        if (attribute == null) {
            return List.of();
        }
        SyntaxValueKind kind = attribute.kind();
        return suggestionCandidates(
            model.values(
                SyntaxValueRequest.of(kind, context.getPartialText(), context.getTagName(), context.getAttrName()),
                limit),
            kind,
            limit);
    }

    private static List<AutocompleteCandidate> suggestionCandidates(List<SyntaxSuggestion> suggestions,
        SyntaxValueKind kind, int limit) {
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (SyntaxSuggestion suggestion : suggestions) {
            if (results.size() >= limit) {
                break;
            }
            results.add(new SyntaxValueCandidate(suggestion, kind));
        }
        return results;
    }

    private static List<AutocompleteCandidate> textCandidates(List<String> values, int limit) {
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String value : values) {
            if (results.size() >= limit) {
                break;
            }
            results.add(new TextCandidate(value));
        }
        return results;
    }

    /**
     * Drops candidates that would insert exactly what the user already typed. Contexts that wrap the
     * typed text in markup keep them, because accepting still adds something. Duplicate values are
     * removed too, since a fixed value list and a value source may overlap.
     */
    private static List<AutocompleteCandidate> removeRedundant(List<AutocompleteCandidate> candidates,
        AutocompleteContext context) {
        Set<String> seen = new HashSet<>();
        List<AutocompleteCandidate> results = new ArrayList<>(candidates.size());
        boolean expands = context.expandsTypedText();
        String typed = context.getPartialText() != null ? context.getPartialText()
            .toLowerCase(Locale.ROOT) : "";
        for (AutocompleteCandidate candidate : candidates) {
            String replacement = candidate.replacementText();
            if (replacement == null) {
                continue;
            }
            if (!seen.add(replacement)) {
                continue;
            }
            if (!expands && !typed.isEmpty()
                && replacement.toLowerCase(Locale.ROOT)
                    .equals(typed)) {
                continue;
            }
            results.add(candidate);
        }
        return results;
    }

    /**
     * Ranks the closest matches first, since the popup pre-selects the head of the list: an exact
     * prefix beats a match on the identifier path, which beats a match anywhere in the name. The sort
     * is stable, so sources keep their own ordering inside a rank.
     */
    private static Comparator<AutocompleteCandidate> relevanceOrder(@Nullable String partialText) {
        if (partialText == null || partialText.isEmpty()) {
            return (left, right) -> 0;
        }
        String partial = partialText.toLowerCase(Locale.ROOT);
        return Comparator.comparingInt(candidate -> relevanceRank(candidate.replacementText(), partial));
    }

    private static int relevanceRank(@Nullable String replacement, String partial) {
        if (replacement == null) {
            return RELEVANCE_LAST;
        }
        String lower = replacement.toLowerCase(Locale.ROOT);
        if (lower.startsWith(partial)) {
            return RELEVANCE_PREFIX;
        }
        int separator = lower.lastIndexOf(':');
        if (separator >= 0 && lower.startsWith(partial, separator + 1)) {
            return RELEVANCE_NAMESPACED_PREFIX;
        }
        return lower.contains(partial) ? RELEVANCE_CONTAINS : RELEVANCE_LAST;
    }
}
