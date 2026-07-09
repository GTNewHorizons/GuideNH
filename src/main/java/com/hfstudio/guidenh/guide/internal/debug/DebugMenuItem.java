package com.hfstudio.guidenh.guide.internal.debug;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * Represents a menu item in the debug control panel.
 */
@Getter
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

    public boolean hasSubmenu() {
        return !submenuItems.isEmpty();
    }

}
