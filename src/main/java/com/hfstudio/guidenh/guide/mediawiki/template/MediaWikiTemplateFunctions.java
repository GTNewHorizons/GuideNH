package com.hfstudio.guidenh.guide.mediawiki.template;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionExpr;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionExprParser;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * The tags that produce a value rather than markup: arithmetic and the string helpers. Each takes its
 * operands from attributes, nested {@code <Param>} tags, or its own body, and returns text nodes for the
 * caller to splice in.
 */
public class MediaWikiTemplateFunctions {

    private static final String START_ATTRIBUTE = "start";
    private static final String LENGTH_ATTRIBUTE = "length";
    private static final String FROM_ATTRIBUTE = "from";
    private static final String TO_ATTRIBUTE = "to";
    private static final String INDEX_ATTRIBUTE = "index";
    private static final String WIDTH_ATTRIBUTE = "width";
    private static final String PAD_ATTRIBUTE = "pad";
    private static final String NEEDLE_ATTRIBUTE = "needle";

    private MediaWikiTemplateFunctions() {}

    public static @Nullable List<MdAstAnyContent> evaluate(MdxJsxElementFields element,
        MediaWikiTemplateArguments arguments, MediaWikiTemplateContext context) {
        String tag = TemplateTags.canonical(element.name());
        String value = TemplateValues.read(element, TemplateTags.VALUE_ATTRIBUTE, arguments, context);
        String result;
        switch (tag) {
            case "lower" -> result = nullToEmpty(value).toLowerCase(Locale.ROOT);
            case "upper" -> result = nullToEmpty(value).toUpperCase(Locale.ROOT);
            case "trim" -> result = nullToEmpty(value).trim();
            case "len" -> result = String.valueOf(nullToEmpty(value).length());
            case "uriencode", "urlencode" -> result = URLEncoder.encode(nullToEmpty(value), StandardCharsets.UTF_8)
                .replace("+", "%20");
            case "pos" -> result = String.valueOf(
                nullToEmpty(value)
                    .indexOf(nullToEmpty(TemplateValues.read(element, NEEDLE_ATTRIBUTE, arguments, context))));
            case "sub" -> result = substring(element, value, arguments, context);
            case "replace" -> result = nullToEmpty(value).replace(
                nullToEmpty(TemplateValues.read(element, FROM_ATTRIBUTE, arguments, context)),
                nullToEmpty(TemplateValues.read(element, TO_ATTRIBUTE, arguments, context)));
            case "explode" -> result = explode(element, value, arguments, context);
            case "padleft" -> result = pad(element, value, arguments, context, true);
            case "padright" -> result = pad(element, value, arguments, context, false);
            case "expr" -> result = expression(value, context);
            default -> {
                return null;
            }
        }
        if (result == null) {
            return List.of();
        }
        return List.of(MediaWikiTemplateAst.text(result));
    }

    /**
     * Evaluates an expression through the function-graph parser, which already covers precedence, powers,
     * and the usual math functions. A single comparison is handled here because that grammar has none.
     */
    private static @Nullable String expression(@Nullable String source, MediaWikiTemplateContext context) {
        if (source == null || source.trim()
            .isEmpty()) {
            context.addIssue(MediaWikiTemplateIssueKind.INVALID_FUNCTION_ARGS, "Expr needs an expression");
            return "";
        }
        Double value = MediaWikiExpression.evaluate(source);
        if (value == null) {
            context.addIssue(MediaWikiTemplateIssueKind.INVALID_FUNCTION_ARGS, "Bad expression: " + source);
            return "";
        }
        return MediaWikiExpression.format(value);
    }

    private static String substring(MdxJsxElementFields element, @Nullable String value,
        MediaWikiTemplateArguments arguments, MediaWikiTemplateContext context) {
        String text = nullToEmpty(value);
        int start = TemplateValues.toInt(TemplateValues.read(element, START_ATTRIBUTE, arguments, context), 0);
        String rawLength = TemplateValues.read(element, LENGTH_ATTRIBUTE, arguments, context);
        int length = rawLength == null ? text.length() : TemplateValues.toInt(rawLength, text.length());
        int from = Math.clamp(start < 0 ? text.length() + start : start, 0, text.length());
        int to = Math.clamp(from + Math.max(length, 0), from, text.length());
        return text.substring(from, to);
    }

    private static String explode(MdxJsxElementFields element, @Nullable String value,
        MediaWikiTemplateArguments arguments, MediaWikiTemplateContext context) {
        String text = nullToEmpty(value);
        String delimiter = nullToEmpty(TemplateValues.read(element, "delimiter", arguments, context));
        int index = TemplateValues.toInt(TemplateValues.read(element, INDEX_ATTRIBUTE, arguments, context), 0);
        if (delimiter.isEmpty()) {
            return index == 0 ? text : "";
        }
        String[] parts = text.split(Pattern.quote(delimiter), -1);
        if (index < 0) {
            index = parts.length + index;
        }
        return index >= 0 && index < parts.length ? parts[index] : "";
    }

    private static String pad(MdxJsxElementFields element, @Nullable String value, MediaWikiTemplateArguments arguments,
        MediaWikiTemplateContext context, boolean left) {
        String text = nullToEmpty(value);
        int width = TemplateValues
            .toInt(TemplateValues.read(element, WIDTH_ATTRIBUTE, arguments, context), text.length());
        String padding = nullToEmpty(TemplateValues.read(element, PAD_ATTRIBUTE, arguments, context));
        if (padding.isEmpty()) {
            padding = "0";
        }
        if (width <= text.length()) {
            return text;
        }
        StringBuilder filler = new StringBuilder();
        while (filler.length() < width - text.length()) {
            filler.append(padding);
        }
        filler.setLength(width - text.length());
        return left ? filler + text : text + filler;
    }

    private static String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    static class MediaWikiExpression {

        private static final String[] COMPARISONS = { ">=", "<=", "<>", "!=", "==", ">", "<", "=" };

        static @Nullable Double evaluate(String expression) {
            String text = expression.trim();
            for (String operator : COMPARISONS) {
                int at = findTopLevel(text, operator);
                if (at < 0) {
                    continue;
                }
                Double left = arithmetic(text.substring(0, at));
                Double right = arithmetic(text.substring(at + operator.length()));
                if (left == null || right == null) {
                    return null;
                }
                return compare(left, right, operator) ? 1.0 : 0.0;
            }
            return arithmetic(text);
        }

        private static boolean compare(double left, double right, String operator) {
            return switch (operator) {
                case ">=" -> left >= right;
                case "<=" -> left <= right;
                case "<>", "!=" -> left != right;
                case ">" -> left > right;
                case "<" -> left < right;
                default -> left == right;
            };
        }

        private static int findTopLevel(String text, String operator) {
            int depth = 0;
            for (int index = 0; index + operator.length() <= text.length(); index++) {
                char ch = text.charAt(index);
                if (ch == '(') {
                    depth++;
                    continue;
                }
                if (ch == ')') {
                    depth--;
                    continue;
                }
                if (depth != 0 || !text.startsWith(operator, index)) {
                    continue;
                }
                if (operator.equals("=") && index > 0 && isComparisonChar(text.charAt(index - 1))) {
                    continue;
                }
                return index;
            }
            return -1;
        }

        private static boolean isComparisonChar(char ch) {
            return ch == '<' || ch == '>' || ch == '=' || ch == '!';
        }

        private static @Nullable Double arithmetic(String expression) {
            if (expression == null || expression.trim()
                .isEmpty()) {
                return null;
            }
            FunctionExpr parsed = FunctionExprParser.parse(expression.trim(), 0);
            double value = parsed.evaluate(0, 0);
            return Double.isNaN(value) || Double.isInfinite(value) ? null : value;
        }

        static String format(double value) {
            if (value == Math.rint(value) && Math.abs(value) < 1.0e15) {
                return Long.toString((long) value);
            }
            String text = Double.toString(value);
            return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
        }
    }
}
