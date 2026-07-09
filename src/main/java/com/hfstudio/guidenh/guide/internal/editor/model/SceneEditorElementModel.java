package com.hfstudio.guidenh.guide.internal.editor.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import lombok.Getter;
import lombok.Setter;

public class SceneEditorElementModel {

    @Getter
    private final UUID id;
    @Getter
    private final SceneEditorElementType type;
    @Getter
    @Setter
    private float primaryX;
    @Getter
    @Setter
    private float primaryY;
    @Getter
    @Setter
    private float primaryZ;
    @Getter
    @Setter
    private float secondaryX;
    @Getter
    @Setter
    private float secondaryY;
    @Getter
    @Setter
    private float secondaryZ;
    @Getter
    @Setter
    private String colorLiteral;
    @Getter
    @Setter
    private float thickness;
    @Getter
    @Setter
    private boolean visible;
    @Getter
    @Setter
    private boolean alwaysOnTop;
    @Getter
    @Setter
    private String tooltipMarkdown;
    @Getter
    private String textKey;
    @Getter
    @Setter
    private String textMarkdown;
    @Getter
    private String showWhenStructure;
    @Getter
    private String showWhenTier;
    @Getter
    private String showWhenChannels;
    @Getter
    @Setter
    private int maxWidth;
    @Getter
    private int backgroundAlpha;
    private final List<Vector3f> linePoints;
    private final Map<String, String> extraAttributes;

    public SceneEditorElementModel(SceneEditorElementType type) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.primaryX = 0f;
        this.primaryY = 0f;
        this.primaryZ = 0f;
        this.secondaryX = 0f;
        this.secondaryY = 0f;
        this.secondaryZ = 0f;
        this.colorLiteral = type.getDefaultColorLiteral();
        this.thickness = type.getDefaultThickness();
        this.visible = true;
        this.alwaysOnTop = false;
        this.tooltipMarkdown = "";
        this.textKey = "";
        this.textMarkdown = type.getDefaultText();
        this.showWhenStructure = "";
        this.showWhenTier = "";
        this.showWhenChannels = "";
        this.maxWidth = type.getDefaultMaxWidth();
        this.backgroundAlpha = type.getDefaultBackgroundAlpha();
        this.linePoints = new ArrayList<>();
        this.extraAttributes = new LinkedHashMap<>();
    }

    public List<Vector3f> getLinePoints() {
        return List.copyOf(linePoints);
    }

    public void setLinePoints(List<Vector3f> points) {
        linePoints.clear();
        if (points == null) {
            return;
        }
        for (Vector3f point : points) {
            if (point != null) {
                linePoints.add(new Vector3f(point));
            }
        }
    }

    public void setTextKey(String textKey) {
        this.textKey = textKey != null ? textKey : "";
    }

    public void setShowWhenStructure(String showWhenStructure) {
        this.showWhenStructure = showWhenStructure != null ? showWhenStructure : "";
    }

    public void setShowWhenTier(String showWhenTier) {
        this.showWhenTier = showWhenTier != null ? showWhenTier : "";
    }

    public void setShowWhenChannels(String showWhenChannels) {
        this.showWhenChannels = showWhenChannels != null ? showWhenChannels : "";
    }

    public void setBackgroundAlpha(int backgroundAlpha) {
        this.backgroundAlpha = Math.clamp(backgroundAlpha, 0, 255);
    }

    public Map<String, String> getExtraAttributes() {
        return Map.copyOf(extraAttributes);
    }

    public String getExtraAttribute(String name) {
        return extraAttributes.get(name);
    }

    public void setExtraAttribute(String name, String value) {
        if (name == null || name.trim()
            .isEmpty()) {
            return;
        }
        if (value == null || value.trim()
            .isEmpty()) {
            extraAttributes.remove(name);
            return;
        }
        extraAttributes.put(name, value);
    }

    public void setExtraAttributes(Map<String, String> attributes) {
        extraAttributes.clear();
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            setExtraAttribute(entry.getKey(), entry.getValue());
        }
    }

    public SceneEditorElementModel duplicate() {
        SceneEditorElementModel duplicate = new SceneEditorElementModel(this.type);
        duplicate.setPrimaryX(this.primaryX);
        duplicate.setPrimaryY(this.primaryY);
        duplicate.setPrimaryZ(this.primaryZ);
        duplicate.setSecondaryX(this.secondaryX);
        duplicate.setSecondaryY(this.secondaryY);
        duplicate.setSecondaryZ(this.secondaryZ);
        duplicate.setColorLiteral(this.colorLiteral);
        duplicate.setThickness(this.thickness);
        duplicate.setVisible(this.visible);
        duplicate.setAlwaysOnTop(this.alwaysOnTop);
        duplicate.setTooltipMarkdown(this.tooltipMarkdown);
        duplicate.setTextKey(this.textKey);
        duplicate.setTextMarkdown(this.textMarkdown);
        duplicate.setShowWhenStructure(this.showWhenStructure);
        duplicate.setShowWhenTier(this.showWhenTier);
        duplicate.setShowWhenChannels(this.showWhenChannels);
        duplicate.setMaxWidth(this.maxWidth);
        duplicate.setBackgroundAlpha(this.backgroundAlpha);
        duplicate.setLinePoints(this.linePoints);
        duplicate.setExtraAttributes(this.extraAttributes);
        return duplicate;
    }
}
