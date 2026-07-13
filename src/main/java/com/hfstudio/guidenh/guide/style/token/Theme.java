package com.hfstudio.guidenh.guide.style.token;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * One theme — a resolved name→id→value mapping.
 * Immutable after construction. Created by GuideThemeManager.reload().
 */
public final class Theme {

    private final String name;
    private final Object2IntMap<String> nameToId;
    private final ResolvedValue[] values;

    Theme(String name, Object2IntMap<String> nameToId, ResolvedValue[] values) {
        this.name = name;
        this.nameToId = nameToId;
        this.values = values;
    }

    public String name() { return name; }

    /** Fast lookup by TokenKey — preferred. */
    @SuppressWarnings("unchecked")
    public <T extends ResolvedValue> T get(TokenKey<T> key) {
        int id = key.id();
        if (id < values.length) {
            ResolvedValue v = values[id];
            if (v != null) return (T) v;
        }
        return key.defaultValue();
    }

    /** Slow lookup by name — for flexibility only. */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends ResolvedValue> T get(String name, Class<T> type) {
        int id = nameToId.getOrDefault(name, -1);
        if (id >= 0 && id < values.length) {
            ResolvedValue v = values[id];
            if (v != null && type.isInstance(v)) return (T) v;
        }
        return null;
    }




    public ColorValue color(TokenKey<ColorValue> key) { return get(key); }
    public DimensionValue dim(TokenKey<DimensionValue> key) { return get(key); }
    public FloatValue flt(TokenKey<FloatValue> key) { return get(key); }
    public IntValue int_(TokenKey<IntValue> key) { return get(key); }

    /** Builder for GuideThemeManager. */

    static Theme build(String themeName, List<TokenKey<?>> keys,
                       Map<String, String> overrides) {
        Object2IntMap<String> nameToId = new Object2IntArrayMap<>(keys.size());
        ResolvedValue[] values = new ResolvedValue[keys.size()];

        for (TokenKey<?> key : keys) {
            int id = key.id();
            nameToId.put(key.name(), id);

            String raw = overrides.get(key.name());
            if (raw != null) {
                values[id] = parseWithFallback(key, raw);
            } else {
                values[id] = key.defaultValue();
            }
        }

        return new Theme(themeName, nameToId, values);
    }

    private static ResolvedValue parseWithFallback(TokenKey<?> key, String raw) {
        try {
            return key.type().parse(raw);
        } catch (Exception e) {
            GuideDebugLog.warnAlways(
                "Theme: failed to parse '" + key.name() + "'='" + raw +
                "', falling back to default (" + key.defaultValue() + ")");
            return key.defaultValue();
        }
    }
}
