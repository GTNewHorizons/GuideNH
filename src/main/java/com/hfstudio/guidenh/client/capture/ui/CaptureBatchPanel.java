package com.hfstudio.guidenh.client.capture.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchCatalog;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchController;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOutput;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProgress;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchResult;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;

public class CaptureBatchPanel {

    private final Minecraft minecraft;
    private final CaptureBatchController controller;
    private final Set<String> selected = new HashSet<>();
    private final CaptureIconButton start;
    private final CaptureIconButton cancel;
    private final CaptureIconButton refresh;
    private final CaptureIconButton selectAll;
    private final CaptureIconButton close;
    private final CaptureIconButton pause;
    private final CaptureIconButton stop;
    private final CaptureIconButton settingsButton;
    private CaptureTextField filter;
    private CaptureRect bounds = new CaptureRect(0, 0, 0, 0);
    private boolean open;
    private boolean closeWhenReady;
    private int scroll;
    private CaptureBatchCatalog displayedCatalog;
    private CaptureBatchCatalog filteredCatalog;
    private String filteredQuery = "";
    private List<CaptureTargetDescriptor> filteredTargets = List.of();
    private boolean selectionEdited;

    public CaptureBatchPanel(Minecraft minecraft, CaptureBatchController controller, Runnable showSettings) {
        this.minecraft = minecraft;
        this.controller = controller;
        settingsButton = new CaptureIconButton(7, CaptureIcons.SETTINGS, "guidenh.capture.settings", showSettings);
        start = new CaptureIconButton(
            0,
            CaptureIcons.PLAY,
            "guidenh.capture.batch_start",
            () -> controller.start(Set.copyOf(selected)));
        cancel = new CaptureIconButton(1, CaptureIcons.CLOSE, "guidenh.capture.batch_cancel", controller::cancel);
        refresh = new CaptureIconButton(2, CaptureIcons.REFRESH, "guidenh.capture.batch_refresh", this::refresh);
        selectAll = new CaptureIconButton(3, CaptureIcons.CHECK, "guidenh.capture.batch_select_all", this::selectAll);
        close = new CaptureIconButton(4, CaptureIcons.CLOSE, "guidenh.capture.close", this::close);
        pause = new CaptureIconButton(5, CaptureIcons.PAUSE, "guidenh.capture.pause", controller::pauseCapture);
        stop = new CaptureIconButton(6, CaptureIcons.SAVE, "guidenh.capture.stop", controller::stopCapture);
    }

    public boolean isOpen() {
        return open;
    }

    public void toggle() {
        if (open) close();
        else {
            open = true;
            closeWhenReady = false;
            filter = new CaptureTextField(minecraft.fontRenderer, 0, 0, 100, 18);
            filter.setMaxStringLength(256);
            refresh();
        }
    }

    private void refresh() {
        if (controller.busy()) return;
        controller.refresh();
        selectionEdited = false;
        selected.clear();
        selectAll();
        scroll = 0;
    }

    private void selectAll() {
        if (controller.catalog() == null) return;
        for (CaptureTargetDescriptor target : filtered()) if (target.available()) selected.add(target.id());
    }

    public void close() {
        if (controller.busy()) {
            controller.cancel();
            closeWhenReady = true;
        } else {
            open = false;
            if (filter != null) filter.setFocused(false);
        }
    }

    private List<CaptureTargetDescriptor> filtered() {
        CaptureBatchCatalog catalog = controller.catalog();
        if (catalog == null) return List.of();
        String query = filter == null ? ""
            : filter.getText()
                .toLowerCase(Locale.ROOT);
        if (catalog == filteredCatalog && query.equals(filteredQuery)) return filteredTargets;
        filteredCatalog = catalog;
        filteredQuery = query;
        filteredTargets = catalog.targets()
            .stream()
            .filter(
                target -> query.isEmpty()
                    || (target.label() + " " + target.providerId() + " " + target.scope()).toLowerCase(Locale.ROOT)
                        .contains(query))
            .toList();
        return filteredTargets;
    }

    public void draw(CaptureViewport viewport, int mouseX, int mouseY) {
        if (!open) return;
        if (displayedCatalog != controller.catalog()) {
            displayedCatalog = controller.catalog();
            if (!selectionEdited) {
                selected.clear();
                selectAll();
            }
        }
        if (closeWhenReady && !controller.busy()) {
            close();
            return;
        }
        int width = Math.min(440, viewport.logicalWidth() - 16);
        int height = Math.min(360, viewport.logicalHeight() - 24);
        int x = (viewport.logicalWidth() - width) / 2;
        int y = (viewport.logicalHeight() - height) / 2;
        bounds = new CaptureRect(x, y, width, height);
        Gui.drawRect(0, 0, viewport.logicalWidth(), viewport.logicalHeight(), CaptureUiColors.DIM);
        Gui.drawRect(x, y, x + width, y + height, CaptureUiColors.SURFACE);
        minecraft.fontRenderer
            .drawStringWithShadow(I18n.format("guidenh.capture.batch_title"), x + 8, y + 10, 0xFFFFFF);
        close.xPosition = x + width - 28;
        close.yPosition = y + 3;
        refresh.xPosition = x + width - 54;
        refresh.yPosition = y + 3;
        selectAll.xPosition = x + width - 80;
        selectAll.yPosition = y + 3;
        filter.layout(x + 8, y + 29, width - 16, 18);
        settingsButton.xPosition = x + width - 106;
        settingsButton.yPosition = y + 3;
        settingsButton.enabled = !controller.busy();
        filter.drawTextBox();
        List<CaptureTargetDescriptor> targets = filtered();
        int visible = Math.max(1, (height - 107) / 22);
        scroll = Math.clamp(scroll, 0, Math.max(0, targets.size() - visible));
        Map<String, CaptureBatchResult> results = new HashMap<>();
        for (CaptureBatchResult result : controller.results()) results.put(result.targetId(), result);
        for (int row = 0; row < visible && row + scroll < targets.size(); row++) {
            CaptureTargetDescriptor target = targets.get(row + scroll);
            int rowY = y + 54 + row * 22;
            boolean hovered = mouseX >= x + 4 && mouseX < x + width - 4 && mouseY >= rowY && mouseY < rowY + 21;
            Gui.drawRect(x + 4, rowY, x + width - 4, rowY + 21, hovered ? CaptureUiColors.HOVER : CaptureUiColors.ROW);
            if (selected.contains(target.id()))
                GuideIconButton.drawIcon(minecraft, CaptureIcons.CHECK, x + 7, rowY + 2, 16, 16, 0xFFFFFFFF);
            String name = minecraft.fontRenderer.trimStringToWidth(target.label(), width - 142);
            minecraft.fontRenderer
                .drawStringWithShadow(name, x + 28, rowY + 6, target.available() ? 0xE0E7EE : 0x929DA8);
            CaptureBatchResult result = results.get(target.id());
            String status = result == null ? target.availability() : result.status();
            String label = I18n.format("guidenh.capture.batch_status." + status);
            minecraft.fontRenderer.drawStringWithShadow(
                minecraft.fontRenderer.trimStringToWidth(label, 105),
                x + width - 112,
                rowY + 6,
                status.equals("success") ? 0xA5DCA2 : 0xA8B5C2);
            if (hovered && result != null
                && !result.reason()
                    .isEmpty()) {
                minecraft.fontRenderer.drawStringWithShadow(
                    minecraft.fontRenderer.trimStringToWidth(result.reason(), width - 16),
                    x + 8,
                    y + height - 39,
                    0xFFC5A8);
            }
        }
        CaptureBatchProgress progress = controller.progress();
        String summary = controller.busy()
            ? I18n.format(
                "guidenh.capture.batch_progress",
                progress.completed(),
                progress.total(),
                I18n.format("guidenh.capture.batch_stage." + progress.stage()))
            : I18n.format(
                "guidenh.capture.batch_selection",
                selected.size(),
                controller.catalog() == null ? 0
                    : controller.catalog()
                        .options()
                        .maxTargets());
        if (!progress.reason()
            .isEmpty() && !controller.busy()) summary = progress.reason();
        List<CaptureBatchOutput> outputs = controller.outputs();
        if (!controller.busy() && !outputs.isEmpty()) {
            long ready = outputs.stream()
                .filter(
                    output -> output.status()
                        .equals("success"))
                .count();
            summary = I18n.format("guidenh.capture.batch_outputs", ready, outputs.size() - ready, summary);
        }
        minecraft.fontRenderer.drawStringWithShadow(
            minecraft.fontRenderer.trimStringToWidth(summary, width - 128),
            x + 8,
            y + height - 16,
            0xD4DEE7);
        start.xPosition = x + width - 56;
        start.yPosition = y + height - 30;
        cancel.xPosition = x + width - 30;
        cancel.yPosition = y + height - 30;
        pause.xPosition = x + width - 108;
        pause.yPosition = y + height - 30;
        stop.xPosition = x + width - 82;
        stop.yPosition = y + height - 30;
        pause.enabled = stop.enabled = controller.recordingActive();
        pause.setIcon(controller.recordingPaused() ? CaptureIcons.PLAY : CaptureIcons.PAUSE);
        start.enabled = !controller.busy() && !selected.isEmpty();
        cancel.enabled = controller.busy();
        refresh.enabled = selectAll.enabled = !controller.busy();
        for (CaptureIconButton button : List
            .of(start, cancel, refresh, selectAll, close, pause, stop, settingsButton)) {
            button.drawButton(minecraft, mouseX, mouseY);
            if (button.hovered()) {
                minecraft.fontRenderer.drawStringWithShadow(
                    minecraft.fontRenderer.trimStringToWidth(button.tooltip(), width - 16),
                    x + 8,
                    y + height - 39,
                    0xFFFFFF);
            }
        }
    }

    public void click(int x, int y, int button) {
        if (!open || button != 0) return;
        for (CaptureIconButton control : List
            .of(start, cancel, refresh, selectAll, close, pause, stop, settingsButton)) {
            if (control.mousePressed(minecraft, x, y)) {
                control.activate();
                return;
            }
        }
        filter.mouseClicked(x, y, button);
        if (controller.busy() || x < bounds.x() + 4
            || x >= bounds.right() - 4
            || y < bounds.y() + 54
            || y >= bounds.bottom() - 53) return;
        int index = (y - (int) bounds.y() - 54) / 22 + scroll;
        List<CaptureTargetDescriptor> targets = filtered();
        if (index >= targets.size()) return;
        String id = targets.get(index)
            .id();
        selectionEdited = true;
        if (!selected.remove(id)) selected.add(id);
    }

    public void scroll(int wheel) {
        if (open) scroll = Math.max(0, scroll - Integer.signum(wheel));
    }

    public boolean key(char character, int key) {
        if (!open) return false;
        if (key == Keyboard.KEY_ESCAPE) close();
        else if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) && start.enabled) start.activate();
        else filter.textboxKeyTyped(character, key);
        return true;
    }

    public void text(String text) {
        if (open && filter.isFocused()) filter.writeText(text);
    }
}
