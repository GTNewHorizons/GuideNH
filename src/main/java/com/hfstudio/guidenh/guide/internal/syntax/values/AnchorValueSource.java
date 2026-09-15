package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironment;
import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironmentAware;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

public class AnchorValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    public record Heading(String anchor, String title) {}

    private static final String ANCHOR_TAG = "a";
    private static final String ANCHOR_PREFIX = "#";

    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+(.+)$", Pattern.MULTILINE);
    private static final List<Heading> NO_HEADINGS = List.of();

    /** The document the cached headings came from, held weakly: it is only a key for "did the text change". */
    @Nullable
    private WeakReference<String> cachedSource;
    private int cachedSourceLength = -1;
    @Nullable
    private List<Heading> cachedHeadings = NO_HEADINGS;

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.PAGE_PATH);
    }

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // The document is only remembered here. Parsing its headings is deferred to the query that needs
        // them: prepare runs for every edit, and almost none of them are an anchor, so parsing here walked
        // the whole page for nothing. Measured at about a millisecond for a long page, per query.
        String text = environment.documentText();
        if (text == null) {
            cachedSource = null;
            cachedSourceLength = -1;
            cachedHeadings = NO_HEADINGS;
            return;
        }
        String cached = cachedSource != null ? cachedSource.get() : null;
        if (cached != null && cached == text && cachedSourceLength == text.length()) {
            return;
        }
        cachedSource = new WeakReference<>(text);
        cachedSourceLength = text.length();
        cachedHeadings = null;
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        if (!ANCHOR_TAG.equals(request.tagName())) {
            return List.of();
        }
        String partial = request.partialText();
        if (partial == null || !partial.startsWith(ANCHOR_PREFIX)) {
            return List.of();
        }
        List<Heading> headings = headings();
        String query = partial.substring(1)
            .toLowerCase(Locale.ROOT);
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (Heading heading : headings) {
            if (results.size() >= limit) {
                break;
            }
            if (query.isEmpty() || heading.anchor()
                .contains(query)) {
                results.add(SyntaxSuggestion.of(ANCHOR_PREFIX + heading.anchor(), heading.title()));
            }
        }
        return results;
    }

    /** The headings of the current document, parsed on the first query that needs them. */
    private List<Heading> headings() {
        List<Heading> parsed = cachedHeadings;
        if (parsed != null) {
            return parsed;
        }
        String text = cachedSource != null ? cachedSource.get() : null;
        if (text == null) {
            return NO_HEADINGS;
        }
        parsed = parseHeadings(text);
        cachedHeadings = parsed;
        return parsed;
    }

    private static List<Heading> parseHeadings(String text) {
        List<Heading> headings = new ArrayList<>();
        Matcher matcher = HEADING.matcher(text);
        while (matcher.find()) {
            String title = matcher.group(1)
                .trim();
            if (title.isEmpty()) {
                continue;
            }
            String anchor = title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
            if (!anchor.isEmpty()) {
                headings.add(new Heading(anchor, title));
            }
        }
        return List.copyOf(headings);
    }
}
