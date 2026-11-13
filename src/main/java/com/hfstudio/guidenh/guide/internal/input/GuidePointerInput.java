package com.hfstudio.guidenh.guide.internal.input;

public interface GuidePointerInput {

    void handlePointerScroll(int mouseX, int mouseY, int wheelDelta);

    default void handlePointerDrag(int mouseX, int mouseY, int button, long heldMillis) {}
}
