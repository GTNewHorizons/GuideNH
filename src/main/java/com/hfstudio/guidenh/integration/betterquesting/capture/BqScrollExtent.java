package com.hfstudio.guidenh.integration.betterquesting.capture;

import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;

public record BqScrollExtent(double x, double y, double width, double height, int minimumX, int maximumX, int minimumY,
    int maximumY) {

    public static BqScrollExtent from(CanvasScrolling canvas) {
        double viewportWidth = canvas.getTransform()
            .getWidth() / (double) canvas.getZoom();
        double viewportHeight = canvas.getTransform()
            .getHeight() / (double) canvas.getZoom();
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        boolean content = false;
        for (IGuiPanel panel : canvas.getChildren()) {
            IGuiRect rectangle = panel.getTransform();
            if (rectangle.getWidth() <= 0 && rectangle.getHeight() <= 0) continue;
            content = true;
            left = Math.min(left, rectangle.getX());
            top = Math.min(top, rectangle.getY());
            right = Math.max(right, rectangle.getX() + Math.max(0, rectangle.getWidth()));
            bottom = Math.max(bottom, rectangle.getY() + Math.max(0, rectangle.getHeight()));
        }
        if (!content) {
            left = 0;
            top = 0;
            right = 0;
            bottom = 0;
        }
        double width = Math.max(viewportWidth, (double) right - left);
        double height = Math.max(viewportHeight, (double) bottom - top);
        IGuiRect allowed = canvas.getScrollBounds();
        int minimumX = Math.max(allowed.getX(), Math.min(left, allowed.getX() + allowed.getWidth()));
        int minimumY = Math.max(allowed.getY(), Math.min(top, allowed.getY() + allowed.getHeight()));
        int maximumX = Math.max(
            minimumX,
            Math.min(allowed.getX() + allowed.getWidth(), (int) Math.ceil(left + width - viewportWidth)));
        int maximumY = Math.max(
            minimumY,
            Math.min(allowed.getY() + allowed.getHeight(), (int) Math.ceil(top + height - viewportHeight)));
        return new BqScrollExtent(left, top, width, height, minimumX, maximumX, minimumY, maximumY);
    }
}
