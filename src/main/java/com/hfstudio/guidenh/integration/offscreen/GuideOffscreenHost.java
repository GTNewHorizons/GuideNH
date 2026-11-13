package com.hfstudio.guidenh.integration.offscreen;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.input.GuideInputContext;
import com.hfstudio.guidenh.guide.internal.input.GuidePointerInput;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.sbancuz.offscreen.api.HostUI;
import com.sbancuz.offscreen.api.UIRegistry;
import com.sbancuz.offscreen.integration.vanilla.VanillaUI;
import com.sbancuz.offscreen.scope.Scope;
import com.sbancuz.offscreen.window.Window;
import com.sbancuz.offscreen.window.input.FrameEvent;
import com.sbancuz.offscreen.window.input.KeyEvent;

public class GuideOffscreenHost implements HostUI {

    private enum Disposal {
        TRANSFER,
        CLOSE
    }

    private static final Runnable TASK_BOUNDARY = () -> {};

    private final GuideScreen guide;
    private final GuideOffscreenScope scope;
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final KeyEvent dispatchedKey = new KeyEvent();
    private final GuideOffscreenRenderState renderState = new GuideOffscreenRenderState();
    private GuiScreen activeScreen;
    private HostUI childAdapter;
    private Disposal disposal = Disposal.CLOSE;
    private boolean disposed;
    private boolean showWelcome;
    private boolean hadFocus;
    private int dispatchDepth;
    private int dragButton = -1;
    private long dragStartedAt;

    public GuideOffscreenHost(GuideScreen guide) {
        this.guide = guide;
        this.activeScreen = guide;
        this.scope = new GuideOffscreenScope(this::getGuiScreen);
        this.showWelcome = guide.width <= 0;
    }

    public void bind(Window window) {
        scope.bind(window);
    }

    public GuideScreen guide() {
        return guide;
    }

    public boolean isDispatching() {
        return dispatchDepth > 0;
    }

    public void enqueue(Runnable action) {
        tasks.add(action);
    }

    public void retainGuide() {
        disposal = Disposal.TRANSFER;
    }

    public void closeGuide() {
        disposal = Disposal.CLOSE;
    }

    public void requestClose() {
        enqueue(() -> {
            closeChildInScope();
            guide.close();
        });
    }

    @Override
    public Scope scope() {
        return scope;
    }

    @Override
    public GuiScreen getGuiScreen() {
        return activeScreen;
    }

    @Override
    public void onResize(int width, int height) {
        if (disposed) {
            scope.restoreMainScreen();
            return;
        }
        try {
            if (activeScreen != guide && (guide.width != width || guide.height != height)) {
                dispatch(guide, () -> guide.setWorldAndResolution(Minecraft.getMinecraft(), width, height));
            }
            if (childAdapter != null) {
                dispatch(activeScreen, () -> childAdapter.onResize(width, height));
            }
            if (showWelcome) {
                showWelcome = false;
                dispatch(guide, guide::showWelcomeIfNeeded);
            }
        } finally {
            scope.restoreMainScreen();
        }
    }

    @Override
    public void update() {
        if (disposed || guide.width <= 0) {
            scope.restoreMainScreen();
            return;
        }
        tasks.add(TASK_BOUNDARY);
        Runnable task;
        while ((task = tasks.poll()) != null && task != TASK_BOUNDARY) {
            try {
                dispatch(guide, task);
            } catch (RuntimeException error) {
                GuideDebugLog.error("Additional guide window task failed", error);
            }
        }
        GuiScreen current = activeScreen;
        dispatch(current, current::updateScreen);
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float partialTicks, long now) {
        if (disposed) {
            scope.restoreMainScreen();
            return;
        }
        GuiScreen current = activeScreen;
        renderState.capture();
        try {
            dispatch(current, () -> current.drawScreen(mouseX, mouseY, partialTicks));
        } finally {
            renderState.restore();
        }
    }

    @Override
    public void onInput(FrameEvent event, float partialTicks) {
        GuideInputContext input = scope.input();
        input.setPointer(event.mouseX, event.mouseY);
        if (event.pressButton >= 0) {
            input.setButton(event.pressButton, true);
        }
        if (event.releaseButton >= 0) {
            input.setButton(event.releaseButton, false);
        }
        GuiScreen current = activeScreen;
        try {
            if (!disposed) {
                dispatch(current, () -> dispatchInput(current, event));
            } else {
                scope.restoreMainScreen();
            }
        } finally {
            event.pressButton = -1;
            event.releaseButton = -1;
            event.wheelDelta = 0f;
            event.keyCount = 0;
            event.text.setLength(0);
        }
    }

    private void dispatchInput(GuiScreen current, FrameEvent event) {
        if (!scope.isFocused()) {
            if (hadFocus && dragButton >= 0) {
                releasePointer(current, event.mouseX, event.mouseY, dragButton);
            }
            dragButton = -1;
            if (hadFocus) {
                if (current == guide) guide.cancelPointerCapture();
                hadFocus = false;
            }
            scope.input()
                .clearKeys();
            scope.input()
                .clearButtons();
            return;
        }
        hadFocus = true;
        dispatchPointerInput(current, event);
        boolean changed = screenChanged(current);
        for (int i = 0; i < event.keyCount; i++) {
            KeyEvent key = event.keys[i];
            int keyCode = key.keyCode();
            boolean repeat = GuideInputContext.isKeyDown(keyCode);
            scope.input()
                .setKey(keyCode, key.pressed());
            if (changed) continue;
            // Printable text arrives through SDL text events, with the correct layout and IME result.
            char character = key.character() >= ' ' ? '\0' : key.character();
            if (current == guide) {
                if (key.pressed()) guide.handleGuideKey(character, keyCode, repeat);
            } else {
                dispatchedKey.set(keyCode, character, key.pressed())
                    .setSdl(key.sdlKeyCode(), key.sdlScanCode(), (short) key.sdlMod());
                childAdapter.onKeyPressed(dispatchedKey);
            }
            changed = screenChanged(current);
        }
        if (changed) return;
        if (!event.text.isEmpty()) {
            GuideOffscreenTextInput.commit(event.text.toString());
            try {
                for (int i = 0; i < event.text.length(); i++) {
                    char character = event.text.charAt(i);
                    if (current == guide) {
                        guide.handleGuideKey(character, 0, false);
                    } else {
                        dispatchedKey.set(0, character, true)
                            .setSdl(0, 0, (short) 0);
                        childAdapter.onKeyPressed(dispatchedKey);
                    }
                    if (screenChanged(current)) return;
                }
            } finally {
                GuideOffscreenTextInput.clearPendingText();
            }
        }
    }

    private void dispatchPointerInput(GuiScreen current, FrameEvent event) {
        if (event.pressButton >= 0) {
            dragButton = event.pressButton;
            dragStartedAt = Minecraft.getSystemTime();
            if (current == guide) {
                guide.handleGuideMouseButton(event.mouseX, event.mouseY, event.pressButton, true);
            } else {
                childAdapter.onMouseClicked(event.mouseX, event.mouseY, event.pressButton);
            }
            if (screenChanged(current)) return;
        }
        if (event.releaseButton >= 0) {
            releasePointer(current, event.mouseX, event.mouseY, event.releaseButton);
            if (dragButton == event.releaseButton) dragButton = -1;
            if (screenChanged(current)) return;
        }
        if (dragButton >= 0 && current != guide) {
            long held = Minecraft.getSystemTime() - dragStartedAt;
            childAdapter.onMouseDrag(event.mouseX, event.mouseY, dragButton, held);
            if (current instanceof GuidePointerInput pointerInput) {
                pointerInput.handlePointerDrag(event.mouseX, event.mouseY, dragButton, held);
            }
            if (screenChanged(current)) return;
        }
        if (event.wheelDelta != 0f) {
            int wheel = event.wheelDelta > 0f ? 120 : -120;
            if (current == guide) {
                guide.handleGuideMouseWheel(event.mouseX, event.mouseY, wheel);
            } else if (current instanceof GuidePointerInput pointerInput) {
                pointerInput.handlePointerScroll(event.mouseX, event.mouseY, wheel);
            } else {
                childAdapter.onMouseScroll(event.mouseX, event.mouseY, wheel);
            }
            if (screenChanged(current)) return;
        }
    }

    private void releasePointer(GuiScreen current, int x, int y, int button) {
        if (current == guide) {
            guide.handleGuideMouseButton(x, y, button, false);
        } else {
            childAdapter.onMouseReleased(x, y, button);
        }
    }

    private static boolean screenChanged(GuiScreen expected) {
        return Minecraft.getMinecraft().currentScreen != expected;
    }

    private void dispatch(GuiScreen screen, Runnable action) {
        scope.show(screen);
        dispatchDepth++;
        if (screen == guide) guide.prepareForTemporaryScreenChange();
        try {
            action.run();
        } finally {
            try {
                GuiScreen changed = Minecraft.getMinecraft().currentScreen;
                if (changed != screen) {
                    adopt(changed);
                }
            } finally {
                if (screen == guide) guide.cancelTemporaryScreenChange();
                dispatchDepth--;
                // Leave screen transitions to this host; Offscreen must see the original main screen.
                scope.restoreMainScreen();
            }
        }
    }

    private void adopt(GuiScreen changed) {
        if (activeScreen == guide) guide.cancelPointerCapture();
        dragButton = -1;
        scope.input()
            .clearButtons();
        if (changed == null) {
            if (activeScreen == guide) {
                GuideOffscreenController.close(guide);
                return;
            }
            changed = guide;
            guide.setWorldAndResolution(Minecraft.getMinecraft(), activeScreen.width, activeScreen.height);
        }
        activeScreen = changed;
        childAdapter = changed == guide ? null : UIRegistry.resolve(changed);
        if (changed != guide && childAdapter == null) {
            childAdapter = new VanillaUI(changed);
        }
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        tasks.clear();
        try {
            if (disposal == Disposal.CLOSE) {
                releaseGuide();
            } else {
                closeChild();
            }
        } finally {
            GuideOffscreenController.onDisposed(this, disposal == Disposal.TRANSFER);
        }
    }

    private void closeChild() {
        if (activeScreen == guide) return;
        scope.enter();
        try {
            closeChildInScope();
        } finally {
            scope.restore();
        }
    }

    private void closeChildInScope() {
        if (activeScreen == guide) return;
        GuiScreen child = activeScreen;
        activeScreen = guide;
        childAdapter = null;
        scope.show(child);
        try {
            child.onGuiClosed();
        } finally {
            scope.show(guide);
        }
    }

    private void releaseGuide() {
        closeChild();
        scope.enter();
        try {
            guide.finishWindowTransfer();
            guide.cancelTemporaryScreenChange();
            guide.onGuiClosed();
        } finally {
            scope.restore();
        }
    }
}
