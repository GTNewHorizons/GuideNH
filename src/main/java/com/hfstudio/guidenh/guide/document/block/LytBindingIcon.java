package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.binding.BindingSpec;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.integration.api.binding.BindingDisplay;

public class LytBindingIcon extends LytBlock implements InteractiveElement {

    private final BindingSpec spec;
    private final BindingDisplay display;
    private final LytBlock image;

    public LytBindingIcon(BindingSpec spec, BindingDisplay display, LytBlock image) {
        this.spec = spec;
        this.display = display;
        this.image = image;
        image.parent = this;
    }

    public BindingSpec getBindingSpec() {
        return spec;
    }

    public LytBlock getImage() {
        return image;
    }

    @Override
    public List<LytBlock> getChildren() {
        return List.of(image);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        return image.layout(context, x, y, availableWidth);
    }

    @Override
    protected void onLayoutMoved(int x, int y) {
        image.moveLayoutPos(x, y);
    }

    @Override
    public void render(RenderContext context) {
        image.render(context);
    }

    @Override
    protected LytVisitor.Result visitChildren(LytVisitor visitor, boolean includeOutOfTreeContent) {
        return image.visit(visitor, includeOutOfTreeContent);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (!spec.showTooltip()) return Optional.empty();
        var lines = new ArrayList<>(display.tooltip());
        OpenGuideHotkey.appendBindingTooltip(spec.key(), lines);
        return lines.isEmpty() ? Optional.empty() : Optional.of(new TextTooltip(String.join("\n", lines)));
    }
}
