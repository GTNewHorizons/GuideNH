package com.hfstudio.guidenh.guide.internal.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a menu item in the debug control panel.
 */
public class DebugMenuItem {

    private final String translationKey;
    private final DebugMenuAction action;
    private final List<DebugMenuItem> submenuItems;

    public DebugMenuItem(String translationKey, DebugMenuAction action) {
        this.translationKey = translationKey;
        this.action = action;
        this.submenuItems = new ArrayList<>();
    }

    public void addSubmenuItem(DebugMenuItem item) {
        submenuItems.add(item);
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public DebugMenuAction getAction() {
        return action;
    }

    public boolean hasSubmenu() {
        return !submenuItems.isEmpty();
    }

    public List<DebugMenuItem> getSubmenuItems() {
        return submenuItems;
    }
}
