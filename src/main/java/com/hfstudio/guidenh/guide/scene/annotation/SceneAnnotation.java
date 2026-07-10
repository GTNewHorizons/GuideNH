package com.hfstudio.guidenh.guide.scene.annotation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneCondition;

import lombok.Getter;
import lombok.Setter;

public abstract class SceneAnnotation {

    @Nullable
    private GuideTooltip tooltip;
    @Nullable
    private StructureLibSceneCondition structureLibCondition;

    @Getter
    @Setter
    private boolean hovered;

    @Nullable
    public GuideTooltip getTooltip() {
        return tooltip;
    }

    public void setTooltip(@Nullable GuideTooltip tooltip) {
        this.tooltip = tooltip;
    }

    public void setTooltipText(@Nullable String text) {
        this.tooltip = (text != null && !text.isEmpty()) ? new TextTooltip(text) : null;
    }

    public boolean hasTooltip() {
        return tooltip != null;
    }

    @Nullable
    public StructureLibSceneCondition getStructureLibCondition() {
        return structureLibCondition;
    }

    public void setStructureLibCondition(@Nullable StructureLibSceneCondition structureLibCondition) {
        this.structureLibCondition = structureLibCondition;
    }

}
