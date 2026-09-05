package com.hfstudio.guidenh.guide.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry for contributions to the Scene Editor's named dropdown menus.
 *
 * <p>
 * Menu ids identify existing editor menus; registering an item does not create a new visual menu.
 * An item id must be unique within its menu. {@link #snapshot(String)} returns a new immutable,
 * order-sorted list containing only items visible at the time of the call, so callers may safely
 * render it without holding the registry lock.
 * </p>
 */
public class SceneEditorMenuRegistry {

    public static final String MENU_EXPORT = "export";
    public static final String MENU_SNAP = "snap";

    private static final Object LOCK = new Object();
    private static final Map<String, SceneEditorMenuItem> ITEMS = new ConcurrentHashMap<>();

    /** Registers an item and returns a handle that removes that exact contribution when closed. */
    public static SceneEditorRegistration register(String menuId, SceneEditorMenuItem item) {
        if (menuId == null || menuId.trim()
            .isEmpty()) throw new IllegalArgumentException("menuId must not be blank");
        if (item == null) throw new NullPointerException("item");
        String key = menuId + "\n" + SceneEditorToolbarButton.requireId(item.id());
        synchronized (LOCK) {
            if (ITEMS.putIfAbsent(key, item) != null) {
                throw new IllegalArgumentException("Scene Editor menu id already registered: " + key);
            }
        }
        return () -> ITEMS.remove(key, item);
    }

    /** Returns the currently visible items for a menu, sorted by order and then id. */
    public static List<SceneEditorMenuItem> snapshot(String menuId) {
        synchronized (LOCK) {
            List<SceneEditorMenuItem> result = new ArrayList<>();
            String prefix = menuId + "\n";
            for (Map.Entry<String, SceneEditorMenuItem> entry : ITEMS.entrySet()) {
                if (entry.getKey()
                    .startsWith(prefix)
                    && entry.getValue()
                        .visible())
                    result.add(entry.getValue());
            }
            result.sort(
                Comparator.comparingInt(SceneEditorMenuItem::order)
                    .thenComparing(SceneEditorMenuItem::id));
            return List.copyOf(result);
        }
    }
}
