package com.hfstudio.guidenh.guide.internal.debug;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Contains debug information about a hovered GUI element.
 */
public class HoveredElementInfo {

    private final String className;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<String> extraInfo;
    private final HoveredElementInfo parent;

    private int screenX;
    private int screenY;
    private int screenWidth;
    private int screenHeight;

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

    public String getClassName() {
        return className;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getScreenX() {
        return screenX;
    }

    public int getScreenY() {
        return screenY;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public List<String> getExtraInfo() {
        return extraInfo;
    }

    @Nullable
    public HoveredElementInfo getParent() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }
}
