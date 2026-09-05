package com.hfstudio.guidenh.guide.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global registry for toolbar buttons contributed to the Scene Editor.
 *
 * <p>Contributions are keyed by button id and are independent of the lifetime of an individual
 * editor screen. The screen obtains a fresh immutable snapshot for each layout pass; registrations
 * can therefore be added or removed while no screen is open without retaining screen instances.</p>
 */
public class SceneEditorToolbarRegistry {

    private static final Object LOCK = new Object();
    private static final Map<String, SceneEditorToolbarButton> BUTTONS = new ConcurrentHashMap<>();
    private static final AtomicInteger BUTTON_IDS = new AtomicInteger(-1000);

    /** Registers a button and returns a handle that removes that exact contribution when closed. */
    public static SceneEditorRegistration register(SceneEditorToolbarButton button) {
        if (button == null) throw new NullPointerException("button");
        String id = SceneEditorToolbarButton.requireId(button.id());
        synchronized (LOCK) {
            if (BUTTONS.putIfAbsent(id, button) != null) {
                throw new IllegalArgumentException("Scene Editor toolbar id already registered: " + id);
            }
        }
        return () -> BUTTONS.remove(id, button);
    }

    /** Returns visible buttons sorted by order and then id. */
    public static List<SceneEditorToolbarButton> snapshot() {
        synchronized (LOCK) {
            List<SceneEditorToolbarButton> result = new ArrayList<>(BUTTONS.values());
            result.removeIf(button -> !button.visible());
            result.sort(
                Comparator.comparingInt(SceneEditorToolbarButton::order)
                    .thenComparing(SceneEditorToolbarButton::id));
            return List.copyOf(result);
        }
    }

    /** Returns a negative id suitable for editor-owned transient button widgets. */
    public static int nextButtonId() {
        return BUTTON_IDS.getAndDecrement();
    }
}
