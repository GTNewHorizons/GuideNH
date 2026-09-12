package com.hfstudio.guidenh.guide.scene.annotation;

import java.util.Map;

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

    /**
     * How this annotation appears in an exported scene, or null when the exporter does not know it.
     *
     * <p>
     * The exported site draws the annotations this mod ships with. An annotation of your own renders in the
     * book but would be dropped from the site unless it describes itself here, so override this and return a
     * payload the site runtime understands.
     *
     * <p>
     * The returned map is serialized as JSON and read by the scene viewer, which expects a {@code "type"}
     * field it knows. Return null to leave the annotation out of the export.
     *
     * @return the payload for the site viewer, or null to omit the annotation
     */
    @Nullable
    public Map<String, Object> toSitePayload() {
        return null;
    }
}
