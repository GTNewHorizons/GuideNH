package com.hfstudio.guidenh.guide.syntax;

import java.util.List;

/**
 * The kind of value an attribute (or frontmatter key) expects.
 *
 * <p>
 * The kind decides which {@link SyntaxValueSource} answers the completion request and how the value is
 * written into the page. The constants below cover the registries this mod knows about; a mod that owns
 * another registry declares its own kind with {@link #of(String)} and registers a
 * {@link SyntaxValueSource} for it. Kinds compare by value, so a kind declared in two places still
 * routes to the same sources.
 *
 * @param id     stable identifier of the kind
 * @param quoted true when a suggested value is written inside quotes unless the page already quotes it
 */
public record SyntaxValueKind(String id, boolean quoted) {

    /** Free text, written inside quotes. */
    public static final SyntaxValueKind STRING = new SyntaxValueKind("STRING", true);
    /** Whole number, written as a bare attribute value. */
    public static final SyntaxValueKind INT = new SyntaxValueKind("INT", false);
    /** Decimal number, written as a bare attribute value. */
    public static final SyntaxValueKind FLOAT = new SyntaxValueKind("FLOAT", false);
    /** {@code true} or {@code false}, written as a bare attribute value. */
    public static final SyntaxValueKind BOOLEAN = new SyntaxValueKind("BOOLEAN", false);
    /** A colour, either {@code #rrggbb} or a symbolic colour name. */
    public static final SyntaxValueKind COLOR = new SyntaxValueKind("COLOR", true);
    /** One value out of a fixed set declared next to the attribute, or an enum's constants. */
    public static final SyntaxValueKind ENUM = new SyntaxValueKind("ENUM", true);
    /** An item registry name, for example {@code minecraft:stone}. */
    public static final SyntaxValueKind ITEM_ID = new SyntaxValueKind("ITEM_ID", true);
    /** A block registry name. */
    public static final SyntaxValueKind BLOCK_ID = new SyntaxValueKind("BLOCK_ID", true);
    /** An ore dictionary name, for example {@code ingotIron}. */
    public static final SyntaxValueKind ORE_DICT = new SyntaxValueKind("ORE_DICT", true);
    /** A mod id, for example in the frontmatter keys that gate a page on installed mods. */
    public static final SyntaxValueKind MOD_ID = new SyntaxValueKind("MOD_ID", true);
    /** An entity registry name. */
    public static final SyntaxValueKind ENTITY_ID = new SyntaxValueKind("ENTITY_ID", true);
    /** A key binding name, for example {@code key.jump}. */
    public static final SyntaxValueKind KEY_BIND = new SyntaxValueKind("KEY_BIND", true);
    /** A guide page id, for example {@code guidenh:guide/example.md}. */
    public static final SyntaxValueKind PAGE_PATH = new SyntaxValueKind("PAGE_PATH", true);
    /** A path to a file inside the guide assets. */
    public static final SyntaxValueKind FILE_PATH = new SyntaxValueKind("FILE_PATH", true);
    /** A BetterQuesting quest id. */
    public static final SyntaxValueKind QUEST_UUID = new SyntaxValueKind("QUEST_UUID", true);
    /** A Minecraft command, written with its leading slash, for example {@code /tp @s 0 90 0}. */
    public static final SyntaxValueKind COMMAND = new SyntaxValueKind("COMMAND", true);
    /** A math expression. */
    public static final SyntaxValueKind EXPRESSION = new SyntaxValueKind("EXPRESSION", true);
    /** A coordinate, written as a bare attribute value. */
    public static final SyntaxValueKind VECTOR3 = new SyntaxValueKind("VECTOR3", false);
    /** An NBT compound, written inside braces. */
    public static final SyntaxValueKind SNBT = new SyntaxValueKind("SNBT", false);
    /** A namespace, for example a mod id or a function graph domain. */
    public static final SyntaxValueKind DOMAIN = new SyntaxValueKind("DOMAIN", true);
    /** A format pattern such as {@code %s}. */
    public static final SyntaxValueKind FORMAT_PATTERN = new SyntaxValueKind("FORMAT_PATTERN", true);

    private static final List<SyntaxValueKind> BUILT_IN = List.of(
        STRING,
        INT,
        FLOAT,
        BOOLEAN,
        COLOR,
        ENUM,
        ITEM_ID,
        BLOCK_ID,
        ORE_DICT,
        MOD_ID,
        ENTITY_ID,
        KEY_BIND,
        PAGE_PATH,
        FILE_PATH,
        QUEST_UUID,
        COMMAND,
        EXPRESSION,
        VECTOR3,
        SNBT,
        DOMAIN,
        FORMAT_PATTERN);

    /** The kinds this mod ships, for tooling and diagnostics. */
    public static List<SyntaxValueKind> builtIn() {
        return BUILT_IN;
    }

    /** A quoted kind owned by another mod, for example a machine or fluid registry. */
    public static SyntaxValueKind of(String id) {
        return new SyntaxValueKind(id, true);
    }

    /** A kind owned by another mod, with an explicit quoting rule. */
    public static SyntaxValueKind of(String id, boolean quoted) {
        return new SyntaxValueKind(id, quoted);
    }

    /** True when a suggested value is written inside quotes unless the page already quotes it. */
    public boolean isQuoted() {
        return quoted;
    }

    @Override
    public String toString() {
        return id;
    }
}
