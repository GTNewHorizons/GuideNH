package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytSize;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.render.GuiSprite;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytButton extends LytGuiSprite {

    @Nullable
    private Consumer<GuideUiHost> onClick;
    @Nullable
    private Supplier<String> tooltipSupplier;

    public LytButton(GuiSprite sprite, LytSize size) {
        super(sprite, size);
    }

    public void setOnClick(@Nullable Consumer<GuideUiHost> onClick) {
        this.onClick = onClick;
    }

    public void setTooltipSupplier(@Nullable Supplier<String> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
    }

    public void setTooltipText(@Nullable String tooltipText) {
        this.tooltipSupplier = tooltipText != null ? () -> tooltipText : null;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button != 0) return false;
        var bounds = getBounds();
        if (bounds == null || !bounds.contains(x, y) || onClick == null) return false;
        onClick.accept(screen);
        return true;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (tooltipSupplier == null) return Optional.empty();
        var bounds = getBounds();
        if (bounds != null && bounds.contains((int) x, (int) y)) {
            return Optional.of(new TextTooltip(tooltipSupplier.get()));
        }
        return Optional.empty();
    }
}
