package com.hfstudio.guidenh.guide.internal.editor.gui;

import lombok.Getter;

public class SceneEditorHoverMenuState {

    @Getter
    private boolean open;
    private boolean stickyUntilPointerLeaves;

    public void update(boolean hoveringButton, boolean hoveringMenu) {
        if (open && stickyUntilPointerLeaves) {
            open = hoveringButton || hoveringMenu;
            if (!open) {
                stickyUntilPointerLeaves = false;
            }
            return;
        }
        if (open) {
            open = hoveringButton || hoveringMenu;
            return;
        }
        open = hoveringButton;
    }

    public void keepOpenAfterAction() {
        open = true;
        stickyUntilPointerLeaves = true;
    }

    public void close() {
        open = false;
        stickyUntilPointerLeaves = false;
    }
}
