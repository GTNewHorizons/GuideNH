package com.hfstudio.guidenh.client.capture.ui;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureBackendControls;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.session.CaptureSchedule;
import com.hfstudio.guidenh.client.capture.session.CaptureSession;
import com.hfstudio.guidenh.client.capture.session.CaptureSettingsModel;
import com.hfstudio.guidenh.config.CaptureConfig;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;

public class CaptureSettingsPanel {

    private final Minecraft minecraft;
    private final CaptureSession session;
    private final CaptureBackendControls backend;
    private final List<CaptureSetting> settings = new ArrayList<>();
    private CaptureRect bounds = new CaptureRect(0, 0, 0, 0);
    private float scroll;
    private final SmoothFloatState visualScroll = new SmoothFloatState();
    private final Map<String, SmoothFloatState> sliderPositions = new HashMap<>();
    private final CaptureTextField search;
    private List<Integer> filtered = List.of();
    private List<Integer> rowOffsets = List.of();
    private String filterText = "\u0000";
    private String filterLanguage = "";
    private int contentHeight;
    private CaptureRect content = new CaptureRect(0, 0, 0, 0);
    private CaptureRect clearSearch = new CaptureRect(0, 0, 0, 0);
    private int draggingSlider = -1;
    private boolean draggingScrollbar;
    private float scrollbarGrab;
    private int editing = -1;
    private CaptureTextField editor;
    private String validation = "";
    private boolean open;

    public CaptureSettingsPanel(Minecraft minecraft, CaptureSession session, CaptureBackendControls backend) {
        this.minecraft = minecraft;
        this.session = session;
        this.backend = backend;
        search = new CaptureTextField(minecraft.fontRenderer, 0, 0, 100, 18);
        search.setMaxStringLength(128);
        visualScroll.snapTo(0);
        CaptureSettingsModel model = session.settings();
        CaptureConfig config = model.config();
        settings
            .add(new CaptureSetting("source", () -> config.sourceId, backend::selectSource, backend::sourceIds, true));
        settings
            .add(new CaptureSetting("reader", () -> config.readerId, backend::selectReader, backend::readerIds, true));
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
        toggle("separate_regions", () -> config.separateRegions, value -> config.separateRegions = value, false);
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
        toggle("gui", () -> config.includeGui, value -> config.includeGui = value, true);
        toggle(
            "player_inventory",
            () -> config.includePlayerInventory,
            value -> config.includePlayerInventory = value,
            true);
        toggle(
            "invert_transparency_grid",
            () -> config.invertTransparencyGrid,
            value -> config.invertTransparencyGrid = value,
            false);
        toggle("hud", model::includeHud, model::includeHud, true);
        toggle("cursor", () -> config.includeCursor, value -> config.includeCursor = value, true);
        toggle("backdrop", () -> config.includeGuiBackdrop, value -> config.includeGuiBackdrop = value, true);
        toggle("tooltips", () -> config.includeTooltips, value -> config.includeTooltips = value, true);
        toggle("snapping", () -> config.snapping, value -> config.snapping = value, false);
        toggle("snap_slots", () -> config.snapSlots, value -> config.snapSlots = value, false);
        toggle("snap_components", () -> config.snapComponents, value -> config.snapComponents = value, false);
        toggle("snap_windows", () -> config.snapWindows, value -> config.snapWindows = value, false);
        toggle("class_name", () -> config.showClassName, value -> config.showClassName = value, false);
        toggle(
            "selection_decorations",
            () -> config.showSelectionDecorations,
            value -> config.showSelectionDecorations = value,
            false);
        toggle("capture_area", () -> config.showCaptureArea, value -> config.showCaptureArea = value, false);
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
        toggle("batch_contact_sheet", () -> config.batchContactSheet, value -> config.batchContactSheet = value, false);
        toggle("batch_scroll_images", () -> config.batchScrollImages, value -> config.batchScrollImages = value, false);
        toggle(
            "batch_group_provider",
            () -> config.batchGroupByProvider,
            value -> config.batchGroupByProvider = value,
            false);
        integer(
            "batch_sheet_columns",
            () -> config.batchSheetColumns,
            value -> config.batchSheetColumns = value,
            1,
            16);
        integer(
            "batch_thumbnail_size",
            () -> config.batchThumbnailSize,
            value -> config.batchThumbnailSize = value,
            64,
            1024);
        integer(
            "batch_sheet_page",
            () -> config.batchSheetPageSize,
            value -> config.batchSheetPageSize = value,
            512,
            32768);
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
        for (int index = 0; index < settings.size(); index++) {
            CaptureSetting setting = settings.get(index);
            CaptureSetting numeric = switch (setting.key()) {
                case "scale" -> setting.withSlider(1, 32, 0.1);
                case "width" -> setting.withSlider(
                    0,
                    Math.min(
                        Integer.MAX_VALUE,
                        session.viewport()
                            .pixelWidth() * 32L),
                    1);
                case "height" -> setting.withSlider(
                    0,
                    Math.min(
                        Integer.MAX_VALUE,
                        session.viewport()
                            .pixelHeight() * 32L),
                    1);
                case "columns" -> setting.withSlider(1, 32, 1);
                case "spacing" -> setting.withSlider(0, 128, 1);
                default -> setting;
            };
            settings.set(index, numeric);
        }
    }

    private void integer(String key, IntSupplier value, IntConsumer update, int min, int max) {
        settings.add(new CaptureSetting(key, () -> Integer.toString(value.getAsInt()), text -> {
            int parsed = Integer.parseInt(text);
            if (parsed < min || parsed > max) throw new IllegalArgumentException(min + "–" + max);
            update.accept(parsed);
        }, List.of(), false).withSlider(min, max, 1));
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

    public boolean textInputFocused() {
        return open && (search.isFocused() || editor != null && editor.isFocused());
    }

    public void toggle() {
        if (open) close();
        else open = true;
    }

    public void close() {
        open = false;
        search.setFocused(false);
        draggingSlider = -1;
        draggingScrollbar = false;
        endEdit();
    }

    public boolean contains(int x, int y) {
        return open && bounds.contains(x, y);
    }

    private void filter() {
        String text = search.getText()
            .strip()
            .toLowerCase(Locale.ROOT);
        String language = minecraft.gameSettings.language;
        if (text.equals(filterText) && language.equals(filterLanguage)) return;
        filterText = text;
        filterLanguage = language;
        String[] words = text.isEmpty() ? new String[0] : text.split("\\s+");
        List<Integer> visible = new ArrayList<>();
        List<Integer> offsets = new ArrayList<>();
        int height = 0;
        for (int index = 0; index < settings.size(); index++) {
            CaptureSetting setting = settings.get(index);
            String label = (setting.key() + " " + I18n.format("guidenh.capture.setting." + setting.key()))
                .toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String word : words) matches &= label.contains(word);
            if (!matches) continue;
            visible.add(index);
            offsets.add(height);
            height += setting.slider() == null ? 24 : 32;
        }
        filtered = List.copyOf(visible);
        rowOffsets = List.copyOf(offsets);
        contentHeight = height;
        scroll = 0;
        visualScroll.snapTo(0);
    }

    public void draw(int x, int y, int width, int height, int mouseX, int mouseY) {
        if (!open) return;
        bounds = new CaptureRect(x, y, width, height);
        Gui.drawRect(x, y, x + width, y + height, CaptureUiColors.SURFACE);
        String title = minecraft.fontRenderer
            .trimStringToWidth(I18n.format("guidenh.capture.settings"), Math.max(1, width / 3));
        int searchX = x + 16 + minecraft.fontRenderer.getStringWidth(title);
        int searchWidth = Math.max(16, x + width - 8 - searchX);
        boolean hasSearch = !search.getText()
            .isEmpty();
        clearSearch = new CaptureRect(x + width - 28, y + 4, 20, 18);
        minecraft.fontRenderer.drawStringWithShadow(title, x + 8, y + 9, 0xFFFFFF);
        search.layout(searchX, y + 4, Math.max(16, searchWidth - (hasSearch ? 22 : 0)), 18);
        search.drawTextBox();
        if (search.getText()
            .isEmpty() && !search.isFocused())
            minecraft.fontRenderer.drawStringWithShadow(
                minecraft.fontRenderer
                    .trimStringToWidth(I18n.format("guidenh.capture.settings_search"), Math.max(1, searchWidth - 24)),
                searchX + 4,
                y + 9,
                0x919EAC);
        if (hasSearch) {
            Gui.drawRect(
                (int) clearSearch.x(),
                (int) clearSearch.y(),
                (int) clearSearch.right(),
                (int) clearSearch.bottom(),
                CaptureUiColors.INPUT);
            GuideIconButton.drawIcon(minecraft, CaptureIcons.CLOSE, x + width - 26, y + 5, 16, 16, 0xFFFFFFFF);
        }
        filter();
        content = new CaptureRect(x + 4, y + 28, Math.max(1, width - 8), Math.max(1, height - 60));
        scroll = Math.clamp(scroll, 0, Math.max(0, contentHeight - (float) content.height()));
        visualScroll.updateTowards(scroll, 20, 0.1f, 0.1f, Float.POSITIVE_INFINITY);
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        try {
            CaptureRect pixels = session.viewport()
                .pixels(content);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(
                (int) pixels.x(),
                session.viewport()
                    .pixelHeight() - (int) pixels.bottom(),
                (int) pixels.width(),
                (int) pixels.height());
            for (int row = 0; row < filtered.size(); row++) {
                int index = filtered.get(row);
                CaptureSetting setting = settings.get(index);
                int rowY = (int) content.y() + rowOffsets.get(row) - visualScroll.rounded();
                int rowHeight = setting.slider() == null ? 24 : 32;
                if (rowY + rowHeight <= content.y() || rowY >= content.bottom()) continue;
                drawRow(setting, index, x, rowY, width, rowHeight, mouseX, mouseY);
            }
            if (filtered.isEmpty()) minecraft.fontRenderer
                .drawStringWithShadow(I18n.format("guidenh.capture.settings_no_results"), x + 8, y + 33, 0x919EAC);
        } finally {
            GL11.glPopAttrib();
        }
        if (contentHeight > content.height()) {
            float thumb = scrollbarHeight();
            float top = (float) content.y()
                + visualScroll.value() / Math.max(1, contentHeight - (float) content.height())
                    * ((float) content.height() - thumb);
            Gui.drawRect(
                x + width - 6,
                (int) content.y(),
                x + width - 3,
                (int) content.bottom(),
                CaptureUiColors.INPUT);
            Gui.drawRect(
                x + width - 6,
                Math.round(top),
                x + width - 3,
                Math.round(top + thumb),
                CaptureUiColors.BORDER);
        }
        String footer = validation.isEmpty() ? I18n.format("guidenh.capture.settings_hint") : validation;
        if (validation.isEmpty() && backend.activeBackend() != null) footer = I18n.format(
            "guidenh.capture.active_backend",
            backendName(
                backend.activeBackend()
                    .sourceId()),
            backendName(
                backend.activeBackend()
                    .readerId()));
        List<String> lines = minecraft.fontRenderer.listFormattedStringToWidth(footer, Math.max(1, width - 16));
        for (int line = 0; line < Math.min(2, lines.size()); line++) minecraft.fontRenderer.drawStringWithShadow(
            lines.get(line),
            x + 8,
            y + height - 25 + line * 10,
            validation.isEmpty() ? 0x919EAC : 0xFFC5A8);
    }

    private void drawRow(CaptureSetting setting, int index, int x, int y, int width, int height, int mouseX,
        int mouseY) {
        if (content.contains(mouseX, mouseY) && mouseY >= y && mouseY < y + height)
            Gui.drawRect(x + 4, y, x + width - 8, y + height - 1, CaptureUiColors.HOVER);
        boolean toggle = setting.choices()
            .equals(List.of("false", "true"));
        int labelWidth = toggle ? width - 46 : width / 2 - 16;
        minecraft.fontRenderer.drawStringWithShadow(
            minecraft.fontRenderer
                .trimStringToWidth(I18n.format("guidenh.capture.setting." + setting.key()), Math.max(1, labelWidth)),
            x + 8,
            y + 6,
            0xD3DAE2);
        if (editing == index && editor != null) {
            editor.layout(x + width / 2, y + 2, Math.max(20, width / 2 - 12), 17);
            editor.drawTextBox();
        } else if (toggle) {
            Gui.drawRect(x + width - 30, y + 3, x + width - 10, y + 20, CaptureUiColors.INPUT);
            if (Boolean.parseBoolean(
                setting.value()
                    .get()))
                GuideIconButton.drawIcon(minecraft, CaptureIcons.CHECK, x + width - 28, y + 4, 16, 16, 0xFFFFFFFF);
        } else {
            String displayed = setting.choices()
                .isEmpty()
                    ? setting.value()
                        .get()
                    : backendName(
                        setting.value()
                            .get());
            displayed = minecraft.fontRenderer.trimStringToWidth(displayed, Math.max(1, width / 2 - 18));
            minecraft.fontRenderer.drawStringWithShadow(
                displayed,
                x + width - 12 - minecraft.fontRenderer.getStringWidth(displayed),
                y + 6,
                0xFFFFFF);
        }
        if (setting.slider() != null) {
            SmoothFloatState value = sliderPositions.get(setting.key());
            float target = setting.slider()
                .position(
                    setting.value()
                        .get());
            if (value == null) {
                value = new SmoothFloatState();
                value.snapTo(target);
                sliderPositions.put(setting.key(), value);
            }
            value.updateTowards(target, 26, 0.1f, 0.0001f, Float.POSITIVE_INFINITY);
            int left = x + 10;
            int right = x + width - 14;
            int thumb = Math.round(left + (right - left) * value.value());
            Gui.drawRect(left, y + 24, right, y + 26, CaptureUiColors.INPUT);
            Gui.drawRect(left, y + 24, thumb, y + 26, CaptureUiColors.BORDER);
            Gui.drawRect(thumb - 2, y + 20, thumb + 3, y + 30, CaptureUiColors.ACTIVE);
        }
    }

    private static String backendName(String id) {
        String key = "guidenh.capture.value." + id;
        String translated = I18n.format(key);
        return translated.equals(key) ? id : translated;
    }

    private int rowAt(int x, int y) {
        if (!content.contains(x, y)) return -1;
        float point = y - (float) content.y() + visualScroll.value();
        for (int row = 0; row < filtered.size(); row++) {
            int index = filtered.get(row);
            int height = settings.get(index)
                .slider() == null ? 24 : 32;
            if (point >= rowOffsets.get(row) && point < rowOffsets.get(row) + height) return index;
        }
        return -1;
    }

    public void click(int x, int y, int button) {
        if (!contains(x, y)) return;
        if (button == 0 && clearSearch.contains(x, y)
            && !search.getText()
                .isEmpty()) {
            if (editing >= 0 && !applyEdit()) return;
            search.setText("");
            search.setFocused(true);
            return;
        }
        if (search.contains(x, y)) {
            if (editing >= 0 && !applyEdit()) return;
            search.mouseClicked(x, y, button);
            search.setFocused(true);
            return;
        }
        search.setFocused(false);
        if (editor != null && editor.contains(x, y)) {
            editor.mouseClicked(x, y, button);
            return;
        }
        if (editing >= 0 && !applyEdit()) return;
        if (button == 0 && x >= bounds.right() - 9 && content.contains(x, y) && contentHeight > content.height()) {
            draggingScrollbar = true;
            float top = (float) content.y()
                + visualScroll.value() / Math.max(1, contentHeight - (float) content.height())
                    * ((float) content.height() - scrollbarHeight());
            scrollbarGrab = y >= top && y <= top + scrollbarHeight() ? y - top : scrollbarHeight() / 2;
            move(x, y);
            return;
        }
        int index = rowAt(x, y);
        if (index < 0) return;
        CaptureSetting setting = settings.get(index);
        int row = filtered.indexOf(index);
        float rowY = (float) content.y() + rowOffsets.get(row) - visualScroll.value();
        if (button == 0 && setting.slider() != null && y >= rowY + 17) {
            draggingSlider = index;
            move(x, y);
        } else if (!setting.choices()
            .isEmpty()) {
                try {
                    setting.cycle(button == 1 ? -1 : 1);
                    if (setting.recapture()) session.invalidatePreview();
                    validation = "";
                } catch (IllegalArgumentException | IllegalStateException error) {
                    validation = I18n.format("guidenh.capture.invalid_value", error.getMessage());
                }
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

    private float scrollbarHeight() {
        return Math.max(18, (float) (content.height() * content.height() / contentHeight));
    }

    public boolean dragging() {
        return draggingSlider >= 0 || draggingScrollbar;
    }

    public void move(int x, int y) {
        if (draggingScrollbar) {
            scroll = Math.clamp(
                (y - (float) content.y() - scrollbarGrab) / Math.max(1, (float) content.height() - scrollbarHeight())
                    * (contentHeight - (float) content.height()),
                0,
                Math.max(0, contentHeight - (float) content.height()));
        } else if (draggingSlider >= 0) {
            CaptureSetting setting = settings.get(draggingSlider);
            float position = (float) ((x - bounds.x() - 10) / Math.max(1, bounds.width() - 24));
            try {
                String value = setting.slider()
                    .value(position);
                if (!value.equals(
                    setting.value()
                        .get())) {
                    setting.update()
                        .accept(value);
                    if (setting.recapture()) session.invalidatePreview();
                }
                validation = "";
            } catch (IllegalArgumentException | IllegalStateException error) {
                validation = I18n.format("guidenh.capture.invalid_value", error.getMessage());
            }
        }
    }

    public void release(int x, int y) {
        if (dragging()) move(x, y);
        draggingSlider = -1;
        draggingScrollbar = false;
    }

    public void scroll(int amount) {
        if (editing >= 0 && !applyEdit()) return;
        float notches = Math.abs((long) amount) >= 120 ? amount / 120f : amount;
        float movement = notches * 72;
        scroll = Math.clamp(scroll - movement, 0, Math.max(0, contentHeight - (float) content.height()));
    }

    public boolean key(char character, int key) {
        return key(character, key, GuiScreen.isCtrlKeyDown());
    }

    public boolean key(char character, int key, boolean control) {
        if (!open) return false;
        if (control && key == Keyboard.KEY_F) {
            if (editing >= 0 && !applyEdit()) return true;
            search.setFocused(true);
            search.setCursorPositionEnd();
            search.setSelectionPos(0);
            return true;
        }
        if (search.isFocused()) {
            if (key == Keyboard.KEY_ESCAPE) {
                if (!search.getText()
                    .isEmpty()) search.setText("");
                else search.setFocused(false);
            } else search.textboxKeyTyped(character, key);
            return true;
        }
        if (editor == null) return false;
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) applyEdit();
        else if (key == Keyboard.KEY_ESCAPE) endEdit();
        else editor.textboxKeyTyped(character, key);
        return true;
    }

    private boolean applyEdit() {
        CaptureSetting setting = settings.get(editing);
        try {
            setting.update()
                .accept(editor.getText());
            if (setting.recapture()) session.invalidatePreview();
            endEdit();
            return true;
        } catch (IllegalArgumentException | IllegalStateException | DateTimeException | ArithmeticException error) {
            validation = I18n.format("guidenh.capture.invalid_value", error.getMessage());
            return false;
        }
    }

    public void text(String text) {
        if (search.isFocused()) search.writeText(text);
        else if (editor != null) editor.writeText(text);
    }

    private void endEdit() {
        if (editor != null) editor.setFocused(false);
        editing = -1;
        editor = null;
        validation = "";
    }
}
