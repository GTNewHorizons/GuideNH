package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

/**
 * A placeholder block for images that will be materialized by a LytScript.
 * The styleClass (e.g., "Img" or "FloatingImage") tells LytHost which script handles it.
 */
public class LytImageBlock extends LytParagraph {

    @Nullable
    private String src;
    @Nullable
    private String alt;
    @Nullable
    private String title;
    @Getter
    private int explicitWidth = -1;
    @Getter
    private int explicitHeight = -1;
    @Getter
    private int cropX;
    @Getter
    private int cropY;
    @Getter
    private int cropWidth = -1;
    @Getter
    private int cropHeight = -1;
    @Getter
    private double scaleX = 1.0d;
    @Getter
    private double scaleY = 1.0d;
    @Getter
    private int displayWidth = -1;
    @Getter
    private int displayHeight = -1;
    @Nullable
    private String align;
    @Getter
    private final List<ImageRegionAnnotation> annotations = new ArrayList<>();

    @Nullable
    public String getSrc() {
        return src;
    }

    public void setSrc(@Nullable String src) {
        this.src = src;
    }

    @Nullable
    public String getAlt() {
        return alt;
    }

    public void setAlt(@Nullable String alt) {
        this.alt = alt;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public void setExplicitWidth(int explicitWidth) {
        this.explicitWidth = explicitWidth > 0 ? explicitWidth : -1;
    }

    public void setExplicitHeight(int explicitHeight) {
        this.explicitHeight = explicitHeight > 0 ? explicitHeight : -1;
    }

    public void setCropX(int cropX) {
        this.cropX = Math.max(0, cropX);
    }

    public void setCropY(int cropY) {
        this.cropY = Math.max(0, cropY);
    }

    public void setCropWidth(int cropWidth) {
        this.cropWidth = cropWidth > 0 ? cropWidth : -1;
    }

    public void setCropHeight(int cropHeight) {
        this.cropHeight = cropHeight > 0 ? cropHeight : -1;
    }

    public void setScaleX(double scaleX) {
        this.scaleX = scaleX > 0.0d ? scaleX : 1.0d;
    }

    public void setScaleY(double scaleY) {
        this.scaleY = scaleY > 0.0d ? scaleY : 1.0d;
    }

    public void setDisplayWidth(int displayWidth) {
        this.displayWidth = displayWidth > 0 ? displayWidth : -1;
    }

    public void setDisplayHeight(int displayHeight) {
        this.displayHeight = displayHeight > 0 ? displayHeight : -1;
    }

    @Nullable
    public String getAlign() {
        return align;
    }

    public void setAlign(@Nullable String align) {
        this.align = align;
    }

    public void addAnnotation(ImageRegionAnnotation annotation) {
        if (annotation != null) {
            annotations.add(annotation);
        }
    }

}
