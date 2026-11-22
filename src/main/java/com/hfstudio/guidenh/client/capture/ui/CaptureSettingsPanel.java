package com.hfstudio.guidenh.client.capture.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.session.CaptureSession;
import com.hfstudio.guidenh.client.capture.session.CaptureSettingsModel;
import com.hfstudio.guidenh.config.CaptureConfig;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;

public class CaptureSettingsPanel {

    private final Minecraft minecraft;
    private final CaptureSession session;
    private final List<CaptureSetting> settings = new ArrayList<>();
    private CaptureRect bounds = new CaptureRect(0, 0, 0, 0);
    private int scroll;
    private int editing = -1;
    private GuiTextField editor;
    private String validation = "";
    private boolean open;

    public CaptureSettingsPanel(Minecraft minecraft, CaptureSession session) {
        this.minecraft = minecraft;
        this.session = session;
        CaptureSettingsModel model = session.settings();
        CaptureConfig config = model.config();
        settings.add(new CaptureSetting("scale", () -> Float.toString(config.scale), value -> {
            float scale = Float.parseFloat(value);
            if (!Float.isFinite(scale) || scale < 1 || scale > 32) throw new IllegalArgumentException("1–32");
            config.scale = scale;
        }, List.of(), false));
        settings.add(
            new CaptureSetting(
                "width",
                () -> Integer.toString(model.requestedWidth()),
                value -> model.setRequestedWidth(Integer.parseInt(value)),
                List.of(),
                false));
        settings.add(
            new CaptureSetting(
                "height",
                () -> Integer.toString(model.requestedHeight()),
                value -> model.setRequestedHeight(Integer.parseInt(value)),
                List.of(),
                false));
        toggle("keep_ratio", model::keepAspectRatio, model::setKeepAspectRatio, false);
        settings.add(
            new CaptureSetting(
                "arrangement",
                () -> config.arrangement,
                value -> config.arrangement = value,
                List.of("original", "horizontal", "vertical", "grid"),
                false));
        settings.add(new CaptureSetting("columns", () -> Integer.toString(config.columns), value -> {
            int columns = Integer.parseInt(value);
            if (columns < 1 || columns > 32) throw new IllegalArgumentException("1–32");
            config.columns = columns;
        }, List.of(), false));
        settings.add(new CaptureSetting("spacing", () -> Integer.toString(config.spacing), value -> {
            int spacing = Integer.parseInt(value);
            if (spacing < 0 || spacing > 128) throw new IllegalArgumentException("0–128");
            config.spacing = spacing;
        }, List.of(), false));
        toggle("world", () -> config.includeWorld, value -> config.includeWorld = value, true);
        toggle("backdrop", () -> config.includeGuiBackdrop, value -> config.includeGuiBackdrop = value, true);
        toggle("tooltips", () -> config.includeTooltips, value -> config.includeTooltips = value, true);
        toggle("snapping", () -> config.snapping, value -> config.snapping = value, false);
        toggle("snap_slots", () -> config.snapSlots, value -> config.snapSlots = value, false);
        toggle("snap_components", () -> config.snapComponents, value -> config.snapComponents = value, false);
        toggle("snap_windows", () -> config.snapWindows, value -> config.snapWindows = value, false);
        toggle("class_name", () -> config.showClassName, value -> config.showClassName = value, false);
        toggle("freeze", () -> config.freezePreview, value -> config.freezePreview = value, true);
        toggle("close_after_save", () -> config.closeAfterSave, value -> config.closeAfterSave = value, false);
        settings.add(
            new CaptureSetting(
                "background",
                () -> Integer.toUnsignedString(config.backgroundArgb, 16),
                value -> config.backgroundArgb = (int) Long.parseUnsignedLong(value.replace("#", ""), 16),
                List.of(),
                false));
        settings.add(
            new CaptureSetting(
                "directory",
                () -> config.outputDirectory,
                value -> config.outputDirectory = value,
                List.of(),
                false));
    }

    private void toggle(String key, BooleanSupplier value, Consumer<Boolean> update, boolean recapture) {
        settings.add(
            new CaptureSetting(
                key,
                () -> Boolean.toString(value.getAsBoolean()),
                text -> update.accept(Boolean.parseBoolean(text)),
                List.of("false", "true"),
                recapture));
    }

    public boolean isOpen() {
        return open;
    }

    public void toggle() {
        if (open) close();
        else open = true;
    }

    public void close() {
        open = false;
        endEdit();
    }

    public boolean contains(int x, int y) {
        return open && bounds.contains(x, y);
    }

    public void draw(int x, int y, int width, int height, int mouseX, int mouseY) {
        if (!open) return;
        bounds = new CaptureRect(x, y, width, height);
        Gui.drawRect(x, y, x + width, y + height, 0xF51B222A);
        minecraft.fontRenderer.drawStringWithShadow(I18n.format("guidenh.capture.settings"), x + 8, y + 8, 0xFFFFFF);
        int visible = Math.max(1, (height - 42) / 24);
        scroll = Math.clamp(scroll, 0, Math.max(0, settings.size() - visible));
        for (int row = 0; row < visible && row + scroll < settings.size(); row++) {
            int index = row + scroll;
            CaptureSetting setting = settings.get(index);
            int rowY = y + 24 + row * 24;
            if (mouseX >= x + 4 && mouseX < x + width - 4 && mouseY >= rowY && mouseY < rowY + 23) {
                Gui.drawRect(x + 4, rowY, x + width - 4, rowY + 23, 0xFF303A45);
            }
            String label = I18n.format("guidenh.capture.setting." + setting.key());
            minecraft.fontRenderer.drawString(label, x + 8, rowY + 7, 0xD3DAE2);
            if (editing == index && editor != null) {
                editor.xPosition = x + width / 2;
                editor.yPosition = rowY + 3;
                editor.width = width / 2 - 10;
                editor.drawTextBox();
                continue;
            }
            String value = setting.value()
                .get();
            if (setting.choices()
                .equals(List.of("false", "true"))) {
                Gui.drawRect(x + width - 28, rowY + 4, x + width - 8, rowY + 20, 0xFF10161D);
                if (Boolean.parseBoolean(value)) GuideIconButton
                    .drawIcon(minecraft, CaptureIcons.CHECK, x + width - 26, rowY + 4, 16, 16, 0xFFFFFFFF);
            } else {
                String displayed = setting.choices()
                    .isEmpty() ? value : I18n.format("guidenh.capture.value." + value);
                displayed = minecraft.fontRenderer.trimStringToWidth(displayed, width / 2 - 16);
                minecraft.fontRenderer.drawString(
                    displayed,
                    x + width - 10 - minecraft.fontRenderer.getStringWidth(displayed),
                    rowY + 7,
                    0xFFFFFF);
            }
        }
        String footer = validation.isEmpty() ? I18n.format("guidenh.capture.settings_hint") : validation;
        minecraft.fontRenderer.drawString(
            minecraft.fontRenderer.trimStringToWidth(footer, width - 16),
            x + 8,
            y + height - 13,
            validation.isEmpty() ? 0x919EAC : 0xFFC5A8);
    }

    public void click(int x, int y, int button) {
        if (!contains(x, y)) return;
        if (editor != null && y >= editor.yPosition
            && y < editor.yPosition + editor.height
            && x >= editor.xPosition
            && x < editor.xPosition + editor.width) {
            editor.mouseClicked(x, y, button);
            return;
        }
        if (editing >= 0 && !applyEdit()) return;
        int index = (y - (int) bounds.y() - 24) / 24 + scroll;
        if (y < bounds.y() + 24 || index < 0 || index >= settings.size() || y >= bounds.bottom() - 18) return;
        CaptureSetting setting = settings.get(index);
        if (!setting.choices()
            .isEmpty()) {
            setting.cycle(button == 1 ? -1 : 1);
            if (setting.recapture()) session.requestRefresh();
        } else if (button == 0) {
            editing = index;
            editor = new GuiTextField(minecraft.fontRenderer, 0, 0, 100, 18);
            editor.setMaxStringLength(512);
            editor.setText(
                setting.value()
                    .get());
            editor.setFocused(true);
            editor.setCursorPositionEnd();
            editor.setSelectionPos(0);
        }
    }

    public void scroll(int amount) {
        if (editing >= 0 && !applyEdit()) return;
        scroll = Math.max(0, scroll - Integer.signum(amount));
    }

    public boolean key(char character, int key) {
        if (editor == null) return false;
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            applyEdit();
        } else if (key == Keyboard.KEY_ESCAPE) {
            endEdit();
        } else {
            editor.textboxKeyTyped(character, key);
        }
        return true;
    }

    private boolean applyEdit() {
        CaptureSetting setting = settings.get(editing);
        try {
            setting.update()
                .accept(editor.getText());
            if (setting.recapture()) session.requestRefresh();
            endEdit();
            return true;
        } catch (IllegalArgumentException error) {
            validation = I18n.format("guidenh.capture.invalid_value", error.getMessage());
            return false;
        }
    }

    public void text(String text) {
        if (editor != null) editor.writeText(text);
    }

    private void endEdit() {
        if (editor != null) editor.setFocused(false);
        editing = -1;
        editor = null;
        validation = "";
    }
}
