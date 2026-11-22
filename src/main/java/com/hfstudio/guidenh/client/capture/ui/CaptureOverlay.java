package com.hfstudio.guidenh.client.capture.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.export.CaptureExportWorker;
import com.hfstudio.guidenh.client.capture.geometry.CaptureGeometryIndex;
import com.hfstudio.guidenh.client.capture.session.CaptureSession;
import com.hfstudio.guidenh.guide.editor.SceneEditorIcon;

public class CaptureOverlay implements AutoCloseable {

    private final Minecraft minecraft;
    private final CaptureSession session;
    private final CaptureExportWorker worker;
    private final Runnable closeAction;
    private final CapturePreviewRenderer preview;
    private final CaptureSelectionRenderer selections = new CaptureSelectionRenderer();
    private final CaptureSettingsPanel settings;
    private final List<CaptureIconButton> buttons = new ArrayList<>();
    private CaptureGeometryIndex geometry;
    private CaptureTarget hovered;
    private CaptureRect toolbar = new CaptureRect(0, 0, 0, 0);
    private int toolbarX = -1;
    private int toolbarY = -1;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean draggingToolbar;
    private boolean additive;
    private int candidateOffset;
    private final int activationKey;
    private boolean activationHeld = true;

    public CaptureOverlay(Minecraft minecraft, CaptureSession session, CaptureExportWorker worker, int activationKey,
        Runnable closeAction) {
        this.minecraft = minecraft;
        this.session = session;
        this.worker = worker;
        this.closeAction = closeAction;
        this.activationKey = activationKey;
        preview = new CapturePreviewRenderer(minecraft);
        settings = new CaptureSettingsPanel(minecraft, session);
        add(CaptureIcons.SAVE, "save", this::save);
        add(
            CaptureIcons.CAPTURE_ALL,
            "all_visible",
            () -> session.selections()
                .select(
                    session.viewport()
                        .bounds(),
                    false));
        add(CaptureIcons.ADD, "add", () -> additive = !additive);
        add(
            CaptureIcons.SNAP,
            "snapping",
            () -> session.settings()
                .config().snapping = !session.settings()
                    .config().snapping);
        add(CaptureIcons.WORLD, "world", () -> {
            session.settings()
                .config().includeWorld = !session.settings()
                    .config().includeWorld;
            session.requestRefresh();
        });
        add(CaptureIcons.REFRESH, "refresh", session::requestRefresh);
        add(
            CaptureIcons.UNDO,
            "undo",
            () -> session.selections()
                .undo());
        add(
            CaptureIcons.REDO,
            "redo",
            () -> session.selections()
                .redo());
        add(CaptureIcons.SETTINGS, "settings", settings::toggle);
        add(CaptureIcons.CLOSE, "close", closeAction);
    }

    private void add(SceneEditorIcon icon, String tooltip, Runnable action) {
        buttons.add(new CaptureIconButton(buttons.size(), icon, "guidenh.capture." + tooltip, action));
    }

    public void updateGeometry(CaptureGeometry current) {
        if (geometry == null || !geometry.geometry()
            .equals(current)) {
            geometry = new CaptureGeometryIndex(current);
            candidateOffset = 0;
        }
    }

    public void draw() {
        CaptureViewport viewport = session.viewport();
        int mouseX = Mouse.getX() * viewport.logicalWidth() / viewport.pixelWidth();
        int mouseY = viewport.logicalHeight() - Mouse.getY() * viewport.logicalHeight() / viewport.pixelHeight() - 1;
        updateLayout(viewport);
        hovered = null;
        if (!toolbar.contains(mouseX, mouseY) && !settings.contains(mouseX, mouseY)) hovered = targetAt(mouseX, mouseY);
        preview.draw(
            session.preview(),
            session.previewVersion(),
            viewport,
            session.settings()
                .config().backgroundArgb);
        selections.draw(
            session.selections()
                .regions(),
            session.selections()
                .active(),
            hovered,
            viewport,
            session.settings()
                .config().maskOpacity,
            minecraft.fontRenderer);
        Gui.drawRect((int) toolbar.x(), (int) toolbar.y(), (int) toolbar.right(), (int) toolbar.bottom(), 0xF51B222A);
        Gui.drawRect(
            (int) toolbar.x() + 6,
            (int) toolbar.y() + 4,
            (int) toolbar.right() - 6,
            (int) toolbar.y() + 6,
            0xFF687581);
        buttons.get(0).enabled = session.canSave();
        buttons.get(2)
            .setActive(additive);
        buttons.get(3)
            .setActive(
                session.settings()
                    .config().snapping);
        buttons.get(4)
            .setActive(
                session.settings()
                    .config().includeWorld);
        buttons.get(6).enabled = session.selections()
            .history()
            .canUndo();
        buttons.get(7).enabled = session.selections()
            .history()
            .canRedo();
        buttons.get(8)
            .setActive(settings.isOpen());
        for (CaptureIconButton button : buttons) button.drawButton(minecraft, mouseX, mouseY);
        int panelWidth = Math.min(280, viewport.logicalWidth() - 12);
        int above = (int) toolbar.y() - 12;
        int below = viewport.logicalHeight() - (int) toolbar.bottom() - 24;
        boolean aboveToolbar = above >= below;
        int panelHeight = Math.min(320, Math.max(48, aboveToolbar ? above : below));
        int panelX = Math.clamp((int) toolbar.right() - panelWidth, 6, viewport.logicalWidth() - panelWidth - 6);
        int panelY = aboveToolbar ? (int) toolbar.y() - panelHeight - 4 : (int) toolbar.bottom() + 4;
        settings.draw(panelX, panelY, panelWidth, panelHeight, mouseX, mouseY);
        for (CaptureIconButton button : buttons) {
            if (button.hovered() && !settings.contains(mouseX, mouseY))
                tooltip(button.tooltip(), mouseX, mouseY, viewport);
        }
        if (hovered != null && session.settings()
            .config().showClassName) {
            String name = hovered.className();
            name = name.substring(name.lastIndexOf('.') + 1);
            tooltip(
                name + "  "
                    + (int) hovered.visibleBounds()
                        .width()
                    + " × "
                    + (int) hovered.visibleBounds()
                        .height(),
                mouseX,
                mouseY,
                viewport);
        }
        String status = session.message()
            .isEmpty() ? I18n.format("guidenh.capture.hint") : session.message();
        minecraft.fontRenderer.drawStringWithShadow(
            minecraft.fontRenderer.trimStringToWidth(status, viewport.logicalWidth() - 16),
            8,
            viewport.logicalHeight() - 12,
            0xE0E7EE);
    }

    private void updateLayout(CaptureViewport viewport) {
        int columns = Math.max(1, Math.min(buttons.size(), (viewport.logicalWidth() - 16) / 24));
        int rows = (buttons.size() + columns - 1) / columns;
        int width = columns * 24 + 8;
        int height = rows * 24 + 12;
        if (toolbarX < 0) toolbarX = (viewport.logicalWidth() - width) / 2;
        if (toolbarY < 0) toolbarY = viewport.logicalHeight() - height - 24;
        toolbarX = Math.clamp(toolbarX, 4, Math.max(4, viewport.logicalWidth() - width - 4));
        toolbarY = Math.clamp(toolbarY, 4, Math.max(4, viewport.logicalHeight() - height - 20));
        toolbar = new CaptureRect(toolbarX, toolbarY, width, height);
        for (int index = 0; index < buttons.size(); index++) {
            CaptureIconButton button = buttons.get(index);
            button.xPosition = toolbarX + 4 + index % columns * 24;
            button.yPosition = toolbarY + 9 + index / columns * 24;
        }
    }

    private void tooltip(String text, int mouseX, int mouseY, CaptureViewport viewport) {
        String displayed = minecraft.fontRenderer.trimStringToWidth(text, viewport.logicalWidth() - 16);
        int width = minecraft.fontRenderer.getStringWidth(displayed);
        int x = Math.clamp(mouseX + 12, 4, viewport.logicalWidth() - width - 8);
        int y = Math.clamp(mouseY - 19, 4, viewport.logicalHeight() - 18);
        Gui.drawRect(x - 4, y - 3, x + width + 4, y + 12, 0xF5141A22);
        minecraft.fontRenderer.drawStringWithShadow(displayed, x, y, 0xFFFFFF);
    }

    public void handleInput() {
        if (Mouse.isCreated()) while (Mouse.next()) mouse();
        if (Keyboard.isCreated()) while (Keyboard.next()) {
            int code = Keyboard.getEventKey();
            if (activationHeld && code == activationKey) {
                if (!Keyboard.getEventKeyState()) activationHeld = false;
                continue;
            }
            if (Keyboard.getEventKeyState()) key(Keyboard.getEventCharacter(), code);
        }
    }

    private void mouse() {
        CaptureViewport viewport = session.viewport();
        int x = Mouse.getEventX() * viewport.logicalWidth() / viewport.pixelWidth();
        int y = viewport.logicalHeight() - Mouse.getEventY() * viewport.logicalHeight() / viewport.pixelHeight() - 1;
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            if (settings.contains(x, y)) settings.scroll(wheel);
            else candidateOffset += Integer.signum(wheel);
        }
        int button = Mouse.getEventButton();
        if (activationHeld && button >= 0 && button - 100 == activationKey) {
            if (!Mouse.getEventButtonState()) activationHeld = false;
            return;
        }
        if (button >= 0 && Mouse.getEventButtonState()) {
            if (settings.contains(x, y)) {
                settings.click(x, y, button);
                return;
            }
            if (toolbar.contains(x, y)) {
                if (button == 0 && y < toolbar.y() + 9) {
                    draggingToolbar = true;
                    dragOffsetX = x - toolbarX;
                    dragOffsetY = y - toolbarY;
                } else if (button == 0) {
                    for (CaptureIconButton control : buttons) {
                        if (control.mousePressed(minecraft, x, y)) {
                            try {
                                control.activate();
                            } catch (IllegalArgumentException error) {
                                session.notify(I18n.format("guidenh.capture.failed", error.getMessage()));
                            }
                            break;
                        }
                    }
                }
                return;
            }
            if (button == 1) session.selections()
                .redraw();
            else if (button == 0) session.selections()
                .begin(x, y, additive || GuiScreen.isCtrlKeyDown(), targetAt(x, y));
        } else if (button == 0) {
            draggingToolbar = false;
            session.selections()
                .finish(x, y);
            additive = false;
        } else if (button == -1) {
            if (draggingToolbar) {
                toolbarX = x - dragOffsetX;
                toolbarY = y - dragOffsetY;
            } else {
                List<CaptureTarget> targets = geometry == null ? List.of()
                    : geometry.geometry()
                        .targets();
                session.selections()
                    .move(
                        x,
                        y,
                        GuiScreen.isShiftKeyDown(),
                        targets,
                        session.settings()
                            .snapping(alt()));
            }
        }
    }

    public void key(char character, int key) {
        if (settings.key(character, key)) return;
        boolean control = GuiScreen.isCtrlKeyDown();
        if (key == Keyboard.KEY_ESCAPE) {
            if (settings.isOpen()) settings.close();
            else if (session.selections()
                .dragging())
                session.selections()
                    .cancelDrag();
            else closeAction.run();
        } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) save();
        else if (key == Keyboard.KEY_DELETE) session.selections()
            .deleteActive();
        else if (control && key == Keyboard.KEY_Z) session.selections()
            .undo();
        else if (control && key == Keyboard.KEY_Y) session.selections()
            .redo();
        else if (control && key == Keyboard.KEY_A) session.selections()
            .select(
                session.viewport()
                    .bounds(),
                false);
        else if (control && key == Keyboard.KEY_D) session.selections()
            .duplicateActive();
        else {
            int step = GuiScreen.isShiftKeyDown() ? 10 : 1;
            int dx = key == Keyboard.KEY_LEFT ? -step : key == Keyboard.KEY_RIGHT ? step : 0;
            int dy = key == Keyboard.KEY_UP ? -step : key == Keyboard.KEY_DOWN ? step : 0;
            if (dx != 0 || dy != 0) session.selections()
                .nudge(dx, dy, control);
        }
    }

    private void save() {
        try {
            session.save(worker, minecraft.mcDataDir.toPath());
        } catch (IllegalArgumentException error) {
            session.notify(I18n.format("guidenh.capture.failed", error.getMessage()));
        }
    }

    public void openOutputFolder() {
        Path directory = session.settings()
            .snapshot(minecraft.mcDataDir.toPath())
            .outputDirectory();
        try {
            Desktop.getDesktop()
                .open(directory.toFile());
        } catch (IOException error) {
            session.notify(I18n.format("guidenh.capture.failed", error.getMessage()));
        }
    }

    private static boolean alt() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    private CaptureTarget targetAt(int x, int y) {
        if (geometry == null) return null;
        List<CaptureTarget> candidates = geometry.candidates(
            x,
            y,
            session.settings()
                .snapping(alt()));
        return candidates.isEmpty() ? null : candidates.get(Math.floorMod(candidateOffset, candidates.size()));
    }

    @Override
    public void close() {
        preview.close();
    }
}
