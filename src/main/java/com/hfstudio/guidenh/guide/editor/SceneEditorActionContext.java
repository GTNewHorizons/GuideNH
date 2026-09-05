package com.hfstudio.guidenh.guide.editor;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;

/**
 * The client-side operations and state that GuideNH exposes to a registered Scene Editor action.
 *
 * <p>The context is valid only while the action is being invoked. Actions should use the supplied
 * session and operations instead of retaining the context or reaching into the editor screen. Calls
 * are expected to run on the Minecraft client thread, which is also the thread that owns the editor
 * UI and preview world.</p>
 */
public interface SceneEditorActionContext {

    /** Returns the current editor session, including the document and scene selection state. */
    SceneEditorSession session();

    /** Returns the editor viewport width in screen pixels. */
    int width();

    /** Returns the editor viewport height in screen pixels. */
    int height();

    /** Rebuilds the scene preview from the current editor document. */
    void rebuildPreview();

    /** Saves the current editor document through the editor's normal save path. */
    void save();

    /** Exports the current scene as SNBT and applies the configured post-export folder behavior. */
    default void exportSnbt() {
        save();
    }

    /** Copies the current scene as a {@code GameScene} document fragment. */
    void copyGameScene();

    /** Copies the currently selected block as a {@code BlockImage} document fragment. */
    void copyBlockImage();

    /** Opens the folder containing the current scene export destination. */
    void openExportFolder();

    /** Closes the active editor dropdowns and transient menus. */
    void closeMenus();
}
