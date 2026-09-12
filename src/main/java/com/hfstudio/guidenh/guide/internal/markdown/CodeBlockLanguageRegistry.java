package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class CodeBlockLanguageRegistry {

    private static final Map<String, CodeBlockLanguage> BY_LANGUAGE_ID = buildLanguageMap();
    private static final Map<String, String> NORMALIZED_ALIASES = buildNormalizedAliases();
    private static final List<String> LANGUAGE_IDS = buildLanguageIds();
    private static final List<String> FENCE_ALIASES = buildFenceAliases();

    protected CodeBlockLanguageRegistry() {}

    public static @Nullable CodeBlockLanguage findById(@Nullable String languageId) {
        if (languageId == null || languageId.isEmpty()) {
            return null;
        }
        return BY_LANGUAGE_ID.get(languageId);
    }

    public static List<String> getLanguageIds() {
        return LANGUAGE_IDS;
    }

    /** Every accepted alias that is not itself a language id, sorted. */
    public static List<String> getFenceAliases() {
        return FENCE_ALIASES;
    }

    public static @Nullable CodeBlockLanguage findByFenceName(@Nullable String fenceName) {
        String normalized = normalizeFenceLanguage(fenceName);
        return normalized != null ? BY_LANGUAGE_ID.get(normalized) : null;
    }

    public static @Nullable String normalizeFenceLanguage(@Nullable String fenceName) {
        if (fenceName == null) {
            return null;
        }
        String normalizedFenceName = fenceName.trim();
        if (normalizedFenceName.isEmpty()) {
            return null;
        }
        return NORMALIZED_ALIASES.get(normalizedFenceName.toLowerCase(Locale.ROOT));
    }

    private static Map<String, CodeBlockLanguage> buildLanguageMap() {
        Map<String, CodeBlockLanguage> result = new HashMap<>();
        register(result, new CodeBlockLanguage("text", "Text"));
        register(result, new CodeBlockLanguage("java", "Java"));
        register(result, new CodeBlockLanguage("kotlin", "Kotlin"));
        register(result, new CodeBlockLanguage("scala", "Scala"));
        register(result, new CodeBlockLanguage("groovy", "Groovy"));
        register(result, new CodeBlockLanguage("lua", "Lua"));
        register(result, new CodeBlockLanguage("json", "JSON"));
        register(result, new CodeBlockLanguage("yaml", "YAML"));
        register(result, new CodeBlockLanguage("xml", "XML"));
        register(result, new CodeBlockLanguage("properties", "Properties"));
        register(result, new CodeBlockLanguage("bash", "Bash"));
        register(result, new CodeBlockLanguage("powershell", "PowerShell"));
        register(result, new CodeBlockLanguage("markdown", "Markdown"));
        register(result, new CodeBlockLanguage("csv", "CSV"));
        register(result, new CodeBlockLanguage("mermaid", "Mermaid"));
        register(result, new CodeBlockLanguage("javascript", "JavaScript"));
        register(result, new CodeBlockLanguage("typescript", "TypeScript"));
        return Map.copyOf(result);
    }

    private static Map<String, String> buildNormalizedAliases() {
        Map<String, String> result = new HashMap<>();
        registerAlias(result, "text", "text", "plain", "plaintext", "txt");
        registerAlias(result, "java", "java");
        registerAlias(result, "kotlin", "kt", "kotlin", "kts");
        registerAlias(result, "scala", "scala", "sc");
        registerAlias(result, "groovy", "groovy", "gradle");
        registerAlias(result, "lua", "lua");
        registerAlias(result, "json", "json");
        registerAlias(result, "yaml", "yaml", "yml");
        registerAlias(result, "xml", "xml", "html");
        registerAlias(result, "properties", "properties", "ini");
        registerAlias(result, "bash", "bash", "sh", "shell");
        registerAlias(result, "powershell", "powershell", "ps1", "pwsh");
        registerAlias(result, "markdown", "markdown", "md");
        registerAlias(result, "csv", "csv");
        registerAlias(result, "mermaid", "mermaid");
        registerAlias(result, "javascript", "javascript", "js");
        registerAlias(result, "typescript", "typescript", "ts");
        return Map.copyOf(result);
    }

    private static void register(Map<String, CodeBlockLanguage> result, CodeBlockLanguage language) {
        result.put(language.id(), language);
    }

    private static List<String> buildLanguageIds() {
        List<String> ids = new ArrayList<>(BY_LANGUAGE_ID.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }

    private static List<String> buildFenceAliases() {
        List<String> aliases = new ArrayList<>(NORMALIZED_ALIASES.size());
        for (String alias : NORMALIZED_ALIASES.keySet()) {
            if (!BY_LANGUAGE_ID.containsKey(alias) && !aliases.contains(alias)) {
                aliases.add(alias);
            }
        }
        Collections.sort(aliases);
        return Collections.unmodifiableList(aliases);
    }

    private static void registerAlias(Map<String, String> result, String languageId, String... aliases) {
        for (String alias : aliases) {
            result.put(alias.toLowerCase(Locale.ROOT), languageId);
        }
    }
}
