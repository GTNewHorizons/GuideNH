package com.hfstudio.guidenh.guide.internal.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.editor.SceneEditorActionContext;
import com.hfstudio.guidenh.guide.editor.SceneEditorMenuItem;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;

/** Owns the built-in Scene Editor toolbar and dropdown registration definitions. */
public class SceneEditorDefaultControlRegistry {

    public static List<GuideIconButton> createToolbarButtons(int x, int y) {
        List<GuideIconButton> buttons = new ArrayList<>();
        buttons
            .add(new GuideIconButton(SceneEditorScreen.CLOSE_BUTTON_ID, x, y, GuideIconButton.Role.SCENE_EDITOR_CLOSE));
        buttons.add(
            new GuideIconButton(
                SceneEditorScreen.RESET_PREVIEW_BUTTON_ID,
                x + 20,
                y,
                GuideIconButton.Role.SCENE_EDITOR_RESET_PREVIEW));
        buttons.add(
            new GuideIconButton(SceneEditorScreen.SNAP_BUTTON_ID, x + 40, y, GuideIconButton.Role.SCENE_EDITOR_SNAP));
        buttons.add(
            new GuideIconButton(
                SceneEditorScreen.AUTO_PICK_BUTTON_ID,
                x + 60,
                y,
                GuideIconButton.Role.SCENE_EDITOR_AUTO_PICK));
        buttons.add(
            new GuideIconButton(
                SceneEditorScreen.IMPORT_STRUCTURE_BUTTON_ID,
                x + 80,
                y,
                GuideIconButton.Role.SCENE_EDITOR_IMPORT_STRUCTURE));
        buttons.add(
            new GuideIconButton(
                SceneEditorScreen.EXPORT_BUTTON_ID,
                x + 100,
                y,
                GuideIconButton.Role.SCENE_EDITOR_EXPORT));
        buttons.add(
            new GuideIconButton(
                SceneEditorScreen.SCREENSHOT_BUTTON_ID,
                x + 120,
                y,
                GuideIconButton.Role.SCENE_EDITOR_SCREENSHOT));
        return buttons;
    }

    public List<SceneEditorMenuItem> createExportItems(SceneEditorActionContext context,
        BooleanSupplier blockImageAvailable) {
        List<SceneEditorMenuItem> items = new ArrayList<>();
        items.add(
            new SceneEditorMenuItem(
                "snbt",
                GuidebookText.SceneEditorExportSnbt::text,
                0,
                () -> true,
                () -> true,
                null,
                ignored -> context.exportSnbt()));
        items.add(
            new SceneEditorMenuItem(
                "snbt-open-folder-after-export",
                GuidebookText.SceneEditorExportSnbtOpenFolder::text,
                10,
                () -> true,
                () -> true,
                () -> ModConfig.ui.sceneEditorExportOpenFolderAfterExport,
                ignored -> {
                    ModConfig.ui.sceneEditorExportOpenFolderAfterExport = !ModConfig.ui.sceneEditorExportOpenFolderAfterExport;
                    ModConfig.save();
                }));
        items.add(
            new SceneEditorMenuItem(
                "game-scene",
                GuidebookText.SceneEditorCopyGameScene::text,
                20,
                blockImageAvailable,
                () -> true,
                null,
                ignored -> context.copyGameScene()));
        items.add(
            new SceneEditorMenuItem(
                "block-image",
                GuidebookText.SceneEditorCopyBlockImage::text,
                30,
                blockImageAvailable,
                () -> true,
                null,
                ignored -> context.copyBlockImage()));
        return items;
    }

    public List<SceneEditorMenuItem> createSnapItems(SceneEditorActionContext context) {
        List<SceneEditorMenuItem> items = new ArrayList<>();
        items.add(
            new SceneEditorMenuItem(
                "line",
                GuidebookText.SceneEditorSnapLine::text,
                0,
                () -> true,
                () -> true,
                () -> ModConfig.ui.sceneEditorSnapLineEnabled,
                ignored -> {
                    ModConfig.ui.sceneEditorSnapLineEnabled = !ModConfig.ui.sceneEditorSnapLineEnabled;
                    ModConfig.save();
                }));
        items.add(
            new SceneEditorMenuItem(
                "point",
                GuidebookText.SceneEditorSnapPoint::text,
                10,
                () -> true,
                () -> true,
                () -> ModConfig.ui.sceneEditorSnapPointEnabled,
                ignored -> {
                    ModConfig.ui.sceneEditorSnapPointEnabled = !ModConfig.ui.sceneEditorSnapPointEnabled;
                    ModConfig.save();
                }));
        items.add(
            new SceneEditorMenuItem(
                "face",
                GuidebookText.SceneEditorSnapFace::text,
                20,
                () -> true,
                () -> true,
                () -> ModConfig.ui.sceneEditorSnapFaceEnabled,
                ignored -> {
                    ModConfig.ui.sceneEditorSnapFaceEnabled = !ModConfig.ui.sceneEditorSnapFaceEnabled;
                    ModConfig.save();
                }));
        items.add(
            new SceneEditorMenuItem(
                "center",
                GuidebookText.SceneEditorSnapCenter::text,
                30,
                () -> true,
                () -> true,
                () -> ModConfig.ui.sceneEditorSnapCenterEnabled,
                ignored -> {
                    ModConfig.ui.sceneEditorSnapCenterEnabled = !ModConfig.ui.sceneEditorSnapCenterEnabled;
                    ModConfig.save();
                }));
        return items;
    }
}
