package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.Locale;
import java.util.Set;

/**
 * The tag names the template layer understands. Kept in one place so the compiler, the expander, and the
 * editor syntax model agree on the vocabulary.
 */
public class TemplateTags {

    public static final String TEMPLATE = "Template";
    public static final String ARGUMENT = "Arg";
    public static final String PARAMETER = "Param";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String VALUE_ATTRIBUTE = "value";
    public static final String DEFAULT_ATTRIBUTE = "default";

    public static final String IF = "If";
    public static final String IF_EQ = "IfEq";
    public static final String IF_EXIST = "IfExist";
    public static final String ELSE = "Else";
    public static final String SWITCH = "Switch";
    public static final String CASE = "Case";
    public static final String DEFAULT = "Default";
    public static final String EXPR = "Expr";

    public static final String TEST_ATTRIBUTE = "test";
    public static final String A_ATTRIBUTE = "a";
    public static final String B_ATTRIBUTE = "b";
    public static final String PAGE_ATTRIBUTE = "page";

    public static final String LOWER = "Lower";
    public static final String UPPER = "Upper";
    public static final String TRIM = "Trim";
    public static final String LEN = "Len";
    public static final String SUB = "Sub";
    public static final String REPLACE = "Replace";
    public static final String EXPLODE = "Explode";
    public static final String PAD_LEFT = "PadLeft";
    public static final String PAD_RIGHT = "PadRight";
    public static final String URL_ENCODE = "UrlEncode";
    public static final String POS = "Pos";

    private static final Set<String> ARGUMENT_TAGS = Set.of(ARGUMENT, PARAMETER);

    // Lower-case, because lookup folds the tag name: an author may write a function in any casing, so every
    // entry has to match the folded form.
    private static final Set<String> STRING_FUNCTIONS = Set.of(
        "lower",
        "upper",
        "trim",
        "len",
        "sub",
        "replace",
        "explode",
        "padleft",
        "padright",
        "urlencode",
        "pos",
        "expr");

    private static final Set<String> CONDITIONAL_TAGS = Set.of(IF, IF_EQ, IF_EXIST, SWITCH);

    private TemplateTags() {}

    public static boolean isArgument(String name) {
        return name != null && ARGUMENT_TAGS.contains(name);
    }

    /**
     * True for the node the parser wraps a multi-line call body in. {@code <Arg>} tags written on their own
     * lines end up inside a paragraph rather than directly under {@code <Template>}, so reading a call's
     * arguments has to look through these wrappers.
     */
    public static boolean isArgumentWrapper(String name) {
        return "p".equals(name) || "div".equals(name);
    }

    public static boolean isParameter(String name) {
        return PARAMETER.equals(name);
    }

    public static boolean isStringFunction(String name) {
        return name != null && STRING_FUNCTIONS.contains(name.toLowerCase(Locale.ROOT));
    }

    public static boolean isConditional(String name) {
        return name != null && CONDITIONAL_TAGS.contains(name);
    }

    public static String canonical(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    /**
     * True when a call-site attribute is a shorthand argument. {@code name} selects the template and is
     * therefore excluded.
     */
    public static boolean isArgumentAttribute(String attributeName) {
        return attributeName != null && !attributeName.isEmpty() && !NAME_ATTRIBUTE.equals(attributeName);
    }
}
