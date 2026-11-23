package com.hfstudio.guidenh.client.capture.ui;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.session.CaptureSchedule;
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
    private CaptureTextField editor;
    private String validation = "";
    private boolean open;

    public CaptureSettingsPanel(Minecraft minecraft, CaptureSession session) {
        this.minecraft = minecraft;
        this.session = session;
        CaptureSettingsModel model = session.settings();
        CaptureConfig config = model.config();
        settings.add(
            new CaptureSetting(
                "format",
                () -> config.format,
                value -> config.format = value,
                List.of("png", "gif", "sprites", "frames"),
                false));
        settings.add(
            new CaptureSetting(
                "quality",
                () -> config.qualityPolicy,
                value -> config.qualityPolicy = value,
                List.of("automatic", "strict", "compatible"),
                false));
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
        toggle("refresh_before_save", () -> config.refreshBeforeSave, value -> config.refreshBeforeSave = value, false);
        toggle("open_folder", () -> config.openFolderAfterSave, value -> config.openFolderAfterSave = value, false);
        toggle("debug", () -> config.includeDebug, value -> config.includeDebug = value, true);
        toggle("nei", () -> config.includeNei, value -> config.includeNei = value, true);
        integer("mask_opacity", () -> config.maskOpacity, value -> config.maskOpacity = value, 0, 90);
        integer("snap_distance", () -> config.snapDistance, value -> config.snapDistance = value, 1, 32);
        integer("snap_release", () -> config.snapReleaseDistance, value -> config.snapReleaseDistance = value, 1, 64);
        integer("fps", () -> config.fps, value -> config.fps = value, 1, 60);
        integer("record_seconds", () -> config.recordingSeconds, value -> config.recordingSeconds = value, 0, 3600);
        integer("frame_limit", () -> config.frameLimit, value -> config.frameLimit = value, 0, Integer.MAX_VALUE);
        integer("loop_count", () -> config.loopCount, value -> config.loopCount = value, 0, 65535);
        integer("alpha_threshold", () -> config.gifAlphaThreshold, value -> config.gifAlphaThreshold = value, 0, 255);
        toggle("gif_matte", () -> config.gifMatte, value -> config.gifMatte = value, false);
        toggle("gif_dither", () -> config.gifDither, value -> config.gifDither = value, false);
        integer("sprite_columns", () -> config.spriteColumns, value -> config.spriteColumns = value, 1, 64);
        integer("sprite_padding", () -> config.spritePadding, value -> config.spritePadding = value, 0, 128);
        integer("sprite_page", () -> config.spritePageSize, value -> config.spritePageSize = value, 256, 32768);
        integer("delay", () -> config.delaySeconds, value -> config.delaySeconds = value, 0, 86400);
        integer("interval", () -> config.intervalSeconds, value -> config.intervalSeconds = value, 1, 86400);
        integer("repeat", () -> config.repeatCount, value -> config.repeatCount = value, 1, 10000);
        settings.add(new CaptureSetting("scheduled_time", () -> config.scheduledTime, value -> {
            new CaptureSchedule(
                config.delaySeconds,
                value,
                config.intervalSeconds,
                config.repeatCount,
                System.nanoTime());
            config.scheduledTime = value;
        }, List.of(), false));
        settings.add(
            new CaptureSetting(
                "unavailable",
                () -> config.unavailablePolicy,
                value -> config.unavailablePolicy = value,
                List.of("wait", "skip", "stop"),
                false));
        integer("memory_budget", () -> config.memoryBudgetMiB, value -> config.memoryBudgetMiB = value, 32, 4096);
        integer("tile_size", () -> config.tileSize, value -> config.tileSize = value, 256, 8192);
        integer("queue_frames", () -> config.queueFrames, value -> config.queueFrames = value, 1, 16);
        integer("disk_budget", () -> config.temporaryBudgetMiB, value -> config.temporaryBudgetMiB = value, 128, 65536);
        toggle("strict_queue", () -> config.strictRecordingQueue, value -> config.strictRecordingQueue = value, false);
        toggle("batch_visible", () -> config.captureVisibleLayers, value -> config.captureVisibleLayers = value, false);
        toggle("batch_pages", () -> config.capturePages, value -> config.capturePages = value, false);
        toggle(
            "batch_reachable",
            () -> config.captureReachableGuis,
            value -> config.captureReachableGuis = value,
            false);
        toggle("batch_popups", () -> config.includePopups, value -> config.includePopups = value, false);
        toggle("batch_variants", () -> config.includeVariants, value -> config.includeVariants = value, false);
        toggle("batch_restore", () -> config.restoreOriginal, value -> config.restoreOriginal = value, false);
        integer("stable_frames", () -> config.stableFrames, value -> config.stableFrames = value, 1, 60);
        integer("timeout", () -> config.timeoutSeconds, value -> config.timeoutSeconds = value, 1, 60);
        integer("target_limit", () -> config.maxTargets, value -> config.maxTargets = value, 1, 100000);
        integer("server_radius", () -> config.serverCaptureRadius, value -> config.serverCaptureRadius = value, 1, 256);
        toggle(
            "server_travel",
            () -> config.serverTemporaryTravel,
            value -> config.serverTemporaryTravel = value,
            false);
        settings.add(
            new CaptureSetting(
                "batch_layers",
                () -> config.batchVisibleMode,
                value -> config.batchVisibleMode = value,
                List.of("composite", "windows"),
                false));
        settings.add(
            new CaptureSetting(
                "batch_providers",
                () -> config.batchProviders,
                value -> config.batchProviders = value,
                List.of(),
                false));
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

    private void integer(String key, IntSupplier value, IntConsumer update, int min, int max) {
        settings.add(new CaptureSetting(key, () -> Integer.toString(value.getAsInt()), text -> {
            int parsed = Integer.parseInt(text);
            if (parsed < min || parsed > max) throw new IllegalArgumentException(min + "–" + max);
            update.accept(parsed);
        }, List.of(), false));
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
        Gui.drawRect(x, y, x + width, y + height, CaptureUiColors.SURFACE);
        minecraft.fontRenderer.drawStringWithShadow(I18n.format("guidenh.capture.settings"), x + 8, y + 8, 0xFFFFFF);
        int visible = Math.max(1, (height - 42) / 24);
        scroll = Math.clamp(scroll, 0, Math.max(0, settings.size() - visible));
        for (int row = 0; row < visible && row + scroll < settings.size(); row++) {
            int index = row + scroll;
            CaptureSetting setting = settings.get(index);
            int rowY = y + 24 + row * 24;
            if (mouseX >= x + 4 && mouseX < x + width - 4 && mouseY >= rowY && mouseY < rowY + 23) {
                Gui.drawRect(x + 4, rowY, x + width - 4, rowY + 23, CaptureUiColors.HOVER);
            }
            String label = I18n.format("guidenh.capture.setting." + setting.key());
            minecraft.fontRenderer.drawStringWithShadow(label, x + 8, rowY + 7, 0xD3DAE2);
            if (editing == index && editor != null) {
                editor.layout(x + width / 2, rowY + 3, width / 2 - 10, 18);
                editor.drawTextBox();
                continue;
            }
            String value = setting.value()
                .get();
            if (setting.choices()
                .equals(List.of("false", "true"))) {
                Gui.drawRect(x + width - 28, rowY + 4, x + width - 8, rowY + 20, CaptureUiColors.INPUT);
                if (Boolean.parseBoolean(value)) GuideIconButton
                    .drawIcon(minecraft, CaptureIcons.CHECK, x + width - 26, rowY + 4, 16, 16, 0xFFFFFFFF);
            } else {
                String displayed = setting.choices()
                    .isEmpty() ? value : I18n.format("guidenh.capture.value." + value);
                displayed = minecraft.fontRenderer.trimStringToWidth(displayed, width / 2 - 16);
                minecraft.fontRenderer.drawStringWithShadow(
                    displayed,
                    x + width - 10 - minecraft.fontRenderer.getStringWidth(displayed),
                    rowY + 7,
                    0xFFFFFF);
            }
        }
        String footer = validation.isEmpty() ? I18n.format("guidenh.capture.settings_hint") : validation;
        minecraft.fontRenderer.drawStringWithShadow(
            minecraft.fontRenderer.trimStringToWidth(footer, width - 16),
            x + 8,
            y + height - 13,
            validation.isEmpty() ? 0x919EAC : 0xFFC5A8);
    }

    public void click(int x, int y, int button) {
        if (!contains(x, y)) return;
        if (editor != null && editor.contains(x, y)) {
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
            editor = new CaptureTextField(minecraft.fontRenderer, 0, 0, 100, 18);
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
        } catch (IllegalArgumentException | DateTimeException | ArithmeticException error) {
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
