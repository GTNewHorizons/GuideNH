package com.hfstudio.guidenh.guide.syntax;

import java.util.List;

public record SyntaxValueKind(String id, boolean quoted) {

    public static final SyntaxValueKind STRING = new SyntaxValueKind("STRING", true);
    public static final SyntaxValueKind INT = new SyntaxValueKind("INT", false);
    public static final SyntaxValueKind FLOAT = new SyntaxValueKind("FLOAT", false);
    public static final SyntaxValueKind BOOLEAN = new SyntaxValueKind("BOOLEAN", false);
    public static final SyntaxValueKind COLOR = new SyntaxValueKind("COLOR", true);
    public static final SyntaxValueKind ENUM = new SyntaxValueKind("ENUM", true);
    public static final SyntaxValueKind ITEM_ID = new SyntaxValueKind("ITEM_ID", true);
    public static final SyntaxValueKind BLOCK_ID = new SyntaxValueKind("BLOCK_ID", true);
    public static final SyntaxValueKind ORE_DICT = new SyntaxValueKind("ORE_DICT", true);
    public static final SyntaxValueKind MOD_ID = new SyntaxValueKind("MOD_ID", true);
    public static final SyntaxValueKind ENTITY_ID = new SyntaxValueKind("ENTITY_ID", true);
    public static final SyntaxValueKind KEY_BIND = new SyntaxValueKind("KEY_BIND", true);
    public static final SyntaxValueKind PAGE_PATH = new SyntaxValueKind("PAGE_PATH", true);
    public static final SyntaxValueKind FILE_PATH = new SyntaxValueKind("FILE_PATH", true);
    public static final SyntaxValueKind QUEST_UUID = new SyntaxValueKind("QUEST_UUID", true);
    public static final SyntaxValueKind COMMAND = new SyntaxValueKind("COMMAND", true);
    public static final SyntaxValueKind EXPRESSION = new SyntaxValueKind("EXPRESSION", true);
    public static final SyntaxValueKind VECTOR3 = new SyntaxValueKind("VECTOR3", false);
    public static final SyntaxValueKind SNBT = new SyntaxValueKind("SNBT", false);
    public static final SyntaxValueKind DOMAIN = new SyntaxValueKind("DOMAIN", true);
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

    public static List<SyntaxValueKind> builtIn() {
        return BUILT_IN;
    }

    public static SyntaxValueKind of(String id) {
        return new SyntaxValueKind(id, true);
    }

    public static SyntaxValueKind of(String id, boolean quoted) {
        return new SyntaxValueKind(id, quoted);
    }

    public boolean isQuoted() {
        return quoted;
    }

    @Override
    public String toString() {
        return id;
    }
}
