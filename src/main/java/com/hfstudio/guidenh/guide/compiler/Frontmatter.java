package com.hfstudio.guidenh.guide.compiler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Frontmatter {

    @Nullable
    private final FrontmatterNavigation navigationEntry;
    private final Map<String, Object> additionalProperties;

    public Frontmatter(@Nullable FrontmatterNavigation navigationEntry, Map<String, Object> additionalProperties) {
        this.navigationEntry = navigationEntry;
        this.additionalProperties = additionalProperties;
    }

    public static final int NAVIGATION_RECOMMEND_ABSENT = Integer.MIN_VALUE;

    // SnakeYAML's Yaml is not thread-safe; use a per-thread cached instance to avoid
    // re-allocating LoaderOptions/SafeConstructor for every page parsed.
    private static final ThreadLocal<Yaml> YAML = ThreadLocal
        .withInitial(() -> new Yaml(new SafeConstructor(new LoaderOptions())));

    public static Frontmatter parse(ResourceLocation pageId, String yamlText) {
        var yaml = YAML.get();

        FrontmatterNavigation navigation = null;
        Object loaded = yaml.load(yamlText);
        if (loaded == null) {
            return new Frontmatter(null, Collections.emptyMap());
        }
        if (!(loaded instanceof Map<?, ?>loadedMap)) {
            throw new IllegalArgumentException("Frontmatter root has to be a map");
        }

        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<?, ?> entry : loadedMap.entrySet()) {
            Object key = entry.getKey();
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Frontmatter keys have to be strings");
            }
            data.put((String) key, entry.getValue());
        }

        var navigationObj = data.remove("navigation");
        if (navigationObj != null) {
            if (!(navigationObj instanceof Map<?, ?>navigationMap)) {
                throw new IllegalArgumentException("The navigation key in the frontmatter has to be a map");
            }

            var title = getString(navigationMap, "title");
            if (title == null) {
                throw new IllegalArgumentException("title is missing in navigation frontmatter");
            }
            var parentIdStr = getString(navigationMap, "parent");
            var position = 0;
            if (navigationMap.containsKey("position")) {
                position = getInt(navigationMap, "position");
            }
            int recommend = NAVIGATION_RECOMMEND_ABSENT;
            if (navigationMap.containsKey("recommend")) {
                recommend = getInt(navigationMap, "recommend");
            }
            int loadPriority = 0;
            if (navigationMap.containsKey("priority")) {
                loadPriority = getInt(navigationMap, "priority");
            }
            var iconIdStr = getString(navigationMap, "icon");
            var iconTextureStr = getString(navigationMap, "icon_texture");

            ResourceLocation parentId = null;
            if (parentIdStr != null) {
                parentId = IdUtils.resolveLink(parentIdStr, pageId);
            }

            int iconMeta = 0;
            String iconId = null;
            NBTTagCompound iconNbt = null;
            if (iconIdStr != null) {
                var parsedIcon = parseIconEntryString(iconIdStr, pageId);
                if (parsedIcon != null) {
                    iconId = parsedIcon.itemId();
                    iconMeta = parsedIcon.meta();
                    iconNbt = parsedIcon.nbt();
                }
            }
            ResourceLocation iconTextureId = null;
            if (iconTextureStr != null) {
                iconTextureId = IdUtils.resolveLink(iconTextureStr, pageId);
            }

            // Parse icons: list (cycling item icons, each entry uses same syntax as icon:)
            List<NavigationIconEntry> iconEntries = null;
            Object iconsObj = navigationMap.get("icons");
            if (iconsObj instanceof List<?>iconsList) {
                iconEntries = new ArrayList<>(iconsList.size());
                for (Object entry : iconsList) {
                    NavigationIconEntry parsed = parseIconEntry(entry, pageId);
                    if (parsed != null) {
                        iconEntries.add(parsed);
                    }
                }
                if (iconEntries.isEmpty()) iconEntries = null;
            }

            // Parse icon_textures: list (cycling texture icons)
            List<ResourceLocation> iconTextureEntries = null;
            Object iconTexturesObj = navigationMap.get("icon_textures");
            if (iconTexturesObj instanceof List<?>iconTexturesList) {
                iconTextureEntries = new ArrayList<>(iconTexturesList.size());
                for (Object entry : iconTexturesList) {
                    if (entry instanceof String) {
                        String texStr = ((String) entry).trim();
                        if (!texStr.isEmpty()) {
                            iconTextureEntries.add(IdUtils.resolveLink(texStr, pageId));
                        }
                    }
                }
                if (iconTextureEntries.isEmpty()) iconTextureEntries = null;
            }

            // Parse required_mod (single mod id) and required_mods (list of mod ids).
            // Either or both may be specified; duplicates are preserved as-is.
            List<String> requiredMods = null;
            String requiredModSingle = getString(navigationMap, "required_mod");
            Object requiredModsObj = navigationMap.get("required_mods");
            if (requiredModSingle != null || requiredModsObj != null) {
                int expectedSize = requiredModSingle == null ? 0 : 1;
                if (requiredModsObj instanceof List<?>requiredModsList) {
                    expectedSize += requiredModsList.size();
                } else if (requiredModsObj instanceof String) {
                    expectedSize++;
                }
                requiredMods = new ArrayList<>(expectedSize);
                if (requiredModSingle != null && !requiredModSingle.trim()
                    .isEmpty()) {
                    requiredMods.add(requiredModSingle.trim());
                }
                if (requiredModsObj instanceof List<?>requiredModsList) {
                    for (Object entry : requiredModsList) {
                        if (entry instanceof String) {
                            String s = ((String) entry).trim();
                            if (!s.isEmpty()) {
                                requiredMods.add(s);
                            }
                        }
                    }
                } else if (requiredModsObj instanceof String) {
                    String s = ((String) requiredModsObj).trim();
                    if (!s.isEmpty()) {
                        requiredMods.add(s);
                    }
                }
                if (requiredMods.isEmpty()) requiredMods = null;
            }

            navigation = new FrontmatterNavigation(
                title,
                parentId,
                position,
                recommend,
                iconId,
                iconMeta,
                iconNbt,
                iconTextureId,
                iconEntries,
                iconTextureEntries,
                requiredMods,
                loadPriority);
        }

        return new Frontmatter(navigation, Map.copyOf(new HashMap<>(data)));
    }

    @Nullable
    public FrontmatterNavigation navigationEntry() {
        return navigationEntry;
    }

    public Map<String, Object> additionalProperties() {
        return additionalProperties;
    }

    @Nullable
    public static String getString(Map<?, ?> map, String key) {
        var value = map.get(key);
        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException("Key " + key + " has to be a String!");
        }
        return (String) value;
    }

    public static int getInt(Map<?, ?> map, String key) {
        var value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Key " + key + " is missing in navigation frontmatter");
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Key " + key + " has to be a number!");
        }
        return number.intValue();
    }

    @Nullable
    public static Map<?, ?> getCompound(Map<?, ?> map, String key) {
        var value = map.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?>mapValue)) {
            throw new IllegalArgumentException("Key " + key + " has to be a map!");
        }
        return mapValue;
    }

    @Nullable
    public static NBTTagCompound parseNavigationNbt(@Nullable Object value, String key) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?>mapValue) {
            return YamlNbtConverter.toNbt(mapValue);
        }
        if (value instanceof String stringValue) {
            String text = stringValue.trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                NBTBase parsed = JsonToNBT.func_150315_a(text);
                if (parsed instanceof NBTTagCompound compound) {
                    return compound;
                }
                throw new IllegalArgumentException("Key " + key + " string value has to decode to a compound tag!");
            } catch (Exception exception) {
                throw new IllegalArgumentException("Key " + key + " has invalid SNBT!", exception);
            }
        }
        throw new IllegalArgumentException("Key " + key + " has to be a map or SNBT string!");
    }

    /**
     * Parses {@code author}/{@code authors}, {@code date}, and {@code updated} from
     * {@link #additionalProperties()} into a {@link FrontmatterPageMeta} value object.
     */
    public FrontmatterPageMeta parseMeta() {
        List<String> authors = new ArrayList<>();

        // Single-author shorthand: author: "Name"
        Object authorObj = additionalProperties.get("author");
        if (authorObj instanceof String s) {
            if (!s.trim()
                .isEmpty()) authors.add(s.trim());
        }

        // Multi-author list: authors: ["Name"] or authors: [{name: "Name"}]
        Object authorsObj = additionalProperties.get("authors");
        if (authorsObj instanceof List<?>) {
            for (Object item : (List<?>) authorsObj) {
                if (item instanceof String) {
                    String s = ((String) item).trim();
                    if (!s.isEmpty()) authors.add(s);
                } else if (item instanceof Map<?, ?>) {
                    Object name = ((Map<?, ?>) item).get("name");
                    if (name instanceof String) {
                        String s = ((String) name).trim();
                        if (!s.isEmpty()) authors.add(s);
                    }
                }
            }
        }

        String date = toDateString(additionalProperties.get("date"));
        String updated = toDateString(additionalProperties.get("updated"));

        float zoom = 1.0f;
        Object zoomObj = additionalProperties.get("zoom");
        if (zoomObj instanceof Number) {
            float z = ((Number) zoomObj).floatValue();
            if (z > 0f) zoom = z;
        }

        return new FrontmatterPageMeta(List.copyOf(authors), date, updated, zoom);
    }

    @Nullable
    private static NavigationIconEntry parseIconEntry(Object entry, ResourceLocation pageId) {
        if (entry instanceof String) {
            return parseIconEntryString((String) entry, pageId);
        }
        if (entry instanceof Map<?, ?>entryMap) {
            var idStr = getString(entryMap, "id");
            if (idStr == null) return null;
            var parsed = parseIconEntryString(idStr, pageId);
            if (parsed == null) return null;
            // Allow explicit meta override in the map form
            int meta = parsed.meta();
            if (entryMap.containsKey("meta")) {
                meta = getInt(entryMap, "meta");
            }
            NBTTagCompound nbt = parsed.nbt();
            if (entryMap.containsKey("nbt")) {
                nbt = parseNavigationNbt(entryMap.get("nbt"), "nbt");
            }
            return new NavigationIconEntry(parsed.itemId(), meta, nbt);
        }
        return null;
    }

    @Nullable
    private static NavigationIconEntry parseIconEntryString(String raw, ResourceLocation pageId) {
        String s = raw.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("<") && s.endsWith(">")) {
            s = s.substring(1, s.length() - 1)
                .trim();
        }
        var parsed = IdUtils.parseItemRef(s, pageId.getResourceDomain());
        if (parsed == null) {
            return null;
        }
        return new NavigationIconEntry(parsed.rawKey(), parsed.concreteMeta(), copyNbt(parsed.nbt()));
    }

    @Nullable
    private static String toDateString(@Nullable Object value) {
        switch (value) {
            case null -> {
                return null;
            }
            case String string -> {
                String s = string.trim();
                return s.isEmpty() ? null : s;
            }
            case Date date -> {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(date);
            }
            default -> {
            }
        }
        return value.toString();
    }

    @Nullable
    private static NBTTagCompound copyNbt(@Nullable NBTTagCompound nbt) {
        return nbt == null ? null : (NBTTagCompound) nbt.copy();
    }
}
