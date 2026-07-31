package com.hfstudio.guidenh.guide.internal.debug;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

/**
 * Contains debug information about a hovered GUI element.
 */
public class HoveredElementInfo {

    @Getter
    private final String className;
    @Getter
    private final int x;
    @Getter
    private final int y;
    @Getter
    private final int width;
    @Getter
    private final int height;
    @Getter
    private final List<String> extraInfo;
    private final HoveredElementInfo parent;

    @Getter
    private int screenX;
    @Getter
    private int screenY;
    @Getter
    private int screenWidth;
    @Getter
    private int screenHeight;
    @Getter
    private float cumulativeScrollOffsetX;
    @Getter
    private float cumulativeScrollOffsetY;

    public HoveredElementInfo(String className, int x, int y, int width, int height,
        @Nullable HoveredElementInfo parent) {
        this.className = className;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.extraInfo = new ArrayList<>();
        this.parent = parent;
        this.screenX = x;
        this.screenY = y;
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void addExtraInfo(String info) {
        extraInfo.add(info);
    }

    public void setScreenCoordinates(int screenX, int screenY, int screenWidth, int screenHeight) {
        this.screenX = screenX;
        this.screenY = screenY;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void setCumulativeScrollOffset(float offsetX, float offsetY) {
        this.cumulativeScrollOffsetX = offsetX;
        this.cumulativeScrollOffsetY = offsetY;
    }

    @Nullable
    public HoveredElementInfo getParent() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }
}
