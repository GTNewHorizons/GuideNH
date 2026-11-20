package com.hfstudio.guidenh.guide.internal.binding;

import java.util.List;
import java.util.Objects;

import net.minecraft.client.gui.GuiScreen;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.search.GuideTargetSelection;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;

public class BindingHoverSession {

    @Nullable
    private GuiScreen screen;
    @Nullable
    private BindingKey key;
    private GuideTargetSelection selection = new GuideTargetSelection("", List.of(), List.of());
    private long revision = Long.MIN_VALUE;
    private int ticksHeld;
    private boolean activationBlocked;

    public GuideTargetSelection update(GuiScreen screen, @Nullable BindingKey key) {
        long nextRevision = BindingNavigationService.revision();
        if (this.screen != screen || !Objects.equals(this.key, key) || revision != nextRevision) {
            revision = nextRevision;
            this.screen = screen;
            this.key = key;
            ticksHeld = 0;
            selection = key == null ? new GuideTargetSelection("", List.of(), List.of())
                : new GuideTargetSelection(
                    BindingRuntime.title(BindingRuntime.describe(key, BindingRuntime.view(false, false))),
                    List.of(),
                    BindingNavigationService.findTargets(key));
        }
        return selection;
    }

    public boolean advance(boolean held, int threshold) {
        if (!held) {
            ticksHeld = 0;
            activationBlocked = false;
            return false;
        }
        if (key == null || selection.targets()
            .isEmpty() || activationBlocked) {
            ticksHeld = 0;
            return false;
        }
        if (++ticksHeld < threshold) return false;
        ticksHeld = threshold;
        activationBlocked = true;
        return true;
    }

    public void reset() {
        screen = null;
        key = null;
        ticksHeld = 0;
        selection = new GuideTargetSelection("", List.of(), List.of());
    }

    public int ticksHeld() {
        return ticksHeld;
    }

    public GuideTargetSelection selection() {
        return selection;
    }
}
