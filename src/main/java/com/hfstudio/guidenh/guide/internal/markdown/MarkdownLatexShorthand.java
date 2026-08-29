package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLiteral;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;

import lombok.Getter;

/**
 * Utility for detecting and splitting {@code $$formula$$} / {@code $formula$} shorthand LaTeX
 * expressions inside Markdown text nodes.
 *
 * <p>
 * A {@code $$formula$$} shorthand always uses default rendering parameters (white colour,
 * scale 1.0, no tooltip). For full control over appearance use the {@code <Latex>} tag instead.
 *
 * <p>
 * Display-mode detection: if a paragraph's only text content is exactly {@code $$formula$$}
 * (after trimming whitespace), the formula is rendered as a centred display block. Otherwise
 * each {@code $$formula$$} or {@code $formula$} fragment is rendered as an inline block inside
 * the surrounding text.
 *
 * <p>
 * Single-dollar inline formulas follow Pandoc-style rules:
 * the opening {@code $} must be followed by a non-whitespace character,
 * the closing {@code $} must be preceded by a non-whitespace character,
 * and the closing {@code $} must not be followed by a digit (to avoid currency false positives).
 * Use {@code \$} to escape a dollar sign that should be treated as literal text.
 */
public class MarkdownLatexShorthand {

    private static final String PLACEHOLDER_PREFIX = "\uE000GUIDENH_LATEX_";
    private static final String PLACEHOLDER_SUFFIX = "_\uE001";
    private static final String ESCAPE_PLACEHOLDER_PREFIX = "\uE000GUIDENH_LATEXESC_";

    /**
     * Sentinel used to temporarily replace {@code $$} after double-dollar masking,
     * preventing the single-dollar pattern from matching placeholder wraps.
     */
    private static final String DOLLAR_SENTINEL = "\uE004\uE005";

    /**
     * Matches {@code $$...$$} where the content contains no literal {@code $} characters.
     * DOTALL allows newlines inside the formula.
     */
    private static final Pattern DOLLAR_PATTERN = Pattern.compile("\\$\\$([^$]+?)\\$\\$", Pattern.DOTALL);

    /**
     * Matches {@code \$} escape sequences — a backslash followed by dollar.
     */
    private static final Pattern ESCAPED_DOLLAR_PATTERN = Pattern.compile("\\\\[$]");

    /**
     * Single-dollar inline formula pattern (Pandoc rules):
     * <ul>
     * <li>Opening {@code $} must be followed by a non-whitespace, non-{@code $} character.
     * <li>Closing {@code $} must be preceded by a non-whitespace, non-{@code $} character.
     * <li>Closing {@code $} must not be followed by a digit (avoid currency false positives).
     * <li>Content must not contain {@code $} or newlines.
     * </ul>
     */
    private static final String SINGLE_DOLLAR_REGEX = "\\$([^\\s$](?:[^$\\n]*[^\\s$])?)\\$(?!\\d)";
    private static final Pattern SINGLE_DOLLAR_PATTERN = Pattern.compile(SINGLE_DOLLAR_REGEX);

    /**
     * Combined pattern for {@link #split}: {@code $$...$$} branch (priority), then single {@code $...$} branch.
     * <ul>
     * <li>Match present → use {@link #formulaFromMatch(Matcher)} to extract the formula content</li>
     * </ul>
     */
    private static final Pattern COMBINED_PATTERN = Pattern
        .compile("(\\$\\$([^$]+?)\\$\\$)|(\\$([^\\s$](?:[^$\\n]*[^\\s$])?)\\$(?!\\d))", Pattern.DOTALL);

    private MarkdownLatexShorthand() {}

    public static MaskResult mask(String source) {
        if (source == null) {
            return new MaskResult("", Map.of());
        }
        if (!mayContain(source)) {
            return new MaskResult(source, Map.of());
        }
        Map<String, String> formulas = new HashMap<>();
        int index = 0;

        // Step (a): mask $$...$$ — unchanged
        {
            Matcher matcher = DOLLAR_PATTERN.matcher(source);
            StringBuilder sb = new StringBuilder(source.length());
            while (matcher.find()) {
                String placeholder = PLACEHOLDER_PREFIX + index + PLACEHOLDER_SUFFIX;
                formulas.put(placeholder, matcher.group(1));
                matcher.appendReplacement(sb, Matcher.quoteReplacement("$$" + placeholder + "$$"));
                index++;
            }
            matcher.appendTail(sb);
            source = sb.toString();
        }

        // Temporarily protect remaining $$ (placeholder wraps and unmatched pairs)
        // so the single-$ pattern in step (c) does not match them.
        boolean hasProtected = source.contains("$$");
        if (hasProtected) {
            source = source.replace("$$", DOLLAR_SENTINEL);
        }

        // Step (b): mask \$ escapes → bare placeholder, restore yields literal $
        {
            Matcher matcher = ESCAPED_DOLLAR_PATTERN.matcher(source);
            StringBuilder sb = new StringBuilder(source.length());
            while (matcher.find()) {
                String placeholder = ESCAPE_PLACEHOLDER_PREFIX + index + PLACEHOLDER_SUFFIX;
                formulas.put(placeholder, "$");
                matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
                index++;
            }
            matcher.appendTail(sb);
            source = sb.toString();
        }

        // Step (c): mask single $...$ ($$ are protected so they won't match)
        {
            Matcher matcher = SINGLE_DOLLAR_PATTERN.matcher(source);
            StringBuilder sb = new StringBuilder(source.length());
            while (matcher.find()) {
                String placeholder = PLACEHOLDER_PREFIX + index + PLACEHOLDER_SUFFIX;
                formulas.put(placeholder, matcher.group(1));
                matcher.appendReplacement(sb, Matcher.quoteReplacement("$" + placeholder + "$"));
                index++;
            }
            matcher.appendTail(sb);
            source = sb.toString();
        }

        // Restore protected $$
        if (hasProtected) {
            source = source.replace(DOLLAR_SENTINEL, "$$");
        }

        return new MaskResult(source, formulas);
    }

    public static void restore(MdAstNode root, MaskResult maskResult) {
        if (maskResult == null || maskResult.isEmpty() || root == null) {
            return;
        }
        restoreNode(root, maskResult);
    }

    /**
     * Quick pre-check: returns {@code false} if {@code text} cannot contain any {@code $} pattern.
     */
    public static boolean mayContain(String text) {
        return text != null && text.contains("$");
    }

    /**
     * If {@code text}, when trimmed, is exactly one {@code $$formula$$} expression,
     * returns the formula content; otherwise returns {@code null}.
     *
     * @param text the raw text value of an AST text node
     * @return the formula string, or {@code null} if the text is not a sole display formula
     */
    @Nullable
    public static String extractSoleDisplayFormula(String text) {
        if (!mayContain(text)) {
            return null;
        }
        String trimmed = text.trim();
        Matcher m = DOLLAR_PATTERN.matcher(trimmed);
        if (!m.matches()) {
            return null;
        }
        String formula = m.group(1)
            .trim();
        return formula.isEmpty() ? null : formula;
    }

    /**
     * Extracts the formula content from a {@link #COMBINED_PATTERN} match.
     * <ul>
     * <li>Group 2: formula from {@code $$...$$} branch</li>
     * <li>Group 4: formula from single {@code $...$} branch</li>
     * </ul>
     */
    private static String formulaFromMatch(Matcher m) {
        String d = m.group(2);
        if (d != null) return d;
        return m.group(4);
    }

    /**
     * Splits {@code text} into alternating plain-text and LaTeX-formula {@link Segment}s.
     * Plain-text segments may be empty strings only when the text starts or ends with a formula.
     *
     * @param text the raw text to split
     * @return ordered list of segments; never {@code null}
     */
    public static List<Segment> split(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<Segment> result = new ArrayList<>();
        Matcher m = COMBINED_PATTERN.matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                result.add(Segment.text(text.substring(last, m.start())));
            }
            result.add(Segment.formula(formulaFromMatch(m)));
            last = m.end();
        }
        if (last < text.length()) {
            result.add(Segment.text(text.substring(last)));
        }
        return result;
    }

    private static void restoreNode(MdAstNode node, MaskResult maskResult) {
        if (node instanceof MdAstLiteral literal) {
            literal.value = restoreText(literal.value, maskResult);
        }
        if (node instanceof MdxJsxAttribute attribute) {
            restoreAttribute(attribute, maskResult);
        }
        if (node instanceof MdxJsxElementFields element) {
            for (Object attribute : element.attributes()) {
                if (attribute instanceof MdAstNode attributeNode) {
                    restoreNode(attributeNode, maskResult);
                }
            }
        }
        if (node instanceof MdAstParent<?>parent) {
            for (Object child : parent.children()) {
                if (child instanceof MdAstNode childNode) {
                    restoreNode(childNode, maskResult);
                }
            }
        } else if (node instanceof MdxJsxElementFields element) {
            for (Object child : element.children()) {
                if (child instanceof MdAstNode childNode) {
                    restoreNode(childNode, maskResult);
                }
            }
        }
    }

    private static String restoreText(String text, MaskResult maskResult) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String restored = text;
        for (Map.Entry<String, String> entry : maskResult.formulas()
            .entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        return restored;
    }

    private static void restoreAttribute(MdxJsxAttribute attribute, MaskResult maskResult) {
        if (attribute.hasStringValue()) {
            attribute.setValue(restoreText(attribute.getStringValue(), maskResult));
        } else if (attribute.hasExpressionValue()) {
            attribute.setExpression(restoreText(attribute.getExpressionValue(), maskResult));
        }
    }

    @Desugar
    public record MaskResult(String source, Map<String, String> formulas) {

        public MaskResult {
            formulas = formulas == null ? Map.of() : Map.copyOf(new HashMap<>(formulas));
        }

        public boolean isEmpty() {
            return formulas.isEmpty();
        }
    }

    /** A text-or-formula segment produced by {@link #split}. */
    @Getter
    public static final class Segment {

        /**
         * -- GETTER --
         * Returns the raw text or formula string.
         */
        private final String value;
        /**
         * -- GETTER --
         * Returns
         * if this segment holds a LaTeX formula.
         */
        private final boolean formula;

        private Segment(String value, boolean formula) {
            this.value = value;
            this.formula = formula;
        }

        /** Creates a plain-text segment. */
        public static Segment text(String value) {
            return new Segment(value, false);
        }

        /** Creates a LaTeX formula segment. */
        public static Segment formula(String value) {
            return new Segment(value, true);
        }

    }
}
