package com.hfstudio.guidenh.guide.internal.debug;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ChatComponentText;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.block.LytDocument;

import lombok.Getter;

/**
 * Main debug overlay system for GuideNH.
 * Manages all debug UI components: info panel, control panel, and hover detection.
 * Optimized for zero overhead when disabled.
 */
public class GuideDebugOverlay {

    public static final float OVERLAY_Z = 500.0F;
    public static final float ATTACHED_LABEL_Z = 10.0F;
    public static final float INFO_PANEL_Z = 20.0F;

    /**
     * -- GETTER --
     * Get the performance monitor for external use.
     */
    @Getter
    private final PerformanceMonitor performanceMonitor;
    private final DashedBorderRenderer borderRenderer;
    private final DebugInfoPanel infoPanel;
    private final DebugControlPanel controlPanel;
    /**
     * -- GETTER --
     * Get the hover detector for external use (e.g., custom element detection).
     */
    @Getter
    private final ElementHoverDetector hoverDetector;
    private LytDocument currentDocument;

    private boolean initialized = false;

    public GuideDebugOverlay() {
        this.performanceMonitor = new PerformanceMonitor();
        this.borderRenderer = new DashedBorderRenderer();
        this.infoPanel = new DebugInfoPanel(performanceMonitor, borderRenderer);
        this.controlPanel = new DebugControlPanel();
        this.hoverDetector = new ElementHoverDetector();
        this.controlPanel.setDebugOverlay(this);
    }

    /**
     * Call this every frame from the GUI screen.
     */
    public void onFrameStart() {
        if (!ModConfig.debug.guiDebugMode) {
            return;
        }
        performanceMonitor.onFrameStart();
    }

    /**
     * Render the debug overlay with scroll and coordinate transformation support.
     * Should be called after all normal GUI rendering is complete.
     *
     * @param contentX Document viewport X position
     * @param contentY Document viewport Y position
     * @param contentW Document viewport width
     * @param contentH Document viewport height
     * @param scrollY  Current scroll position
     * @param zoom     Current zoom level
     */
    public void render(int screenWidth, int screenHeight, int mouseX, int mouseY, int contentX, int contentY,
        int contentW, int contentH, int scrollY, float zoom, @Nullable LytDocument document,
        FontRenderer fontRenderer) {
        render(
            screenWidth,
            screenHeight,
            mouseX,
            mouseY,
            contentX,
            contentY,
            contentW,
            contentH,
            scrollY,
            zoom,
            document,
            List.of(),
            fontRenderer);
    }

    /**
     * Render the debug overlay with document and screen-space component picking.
     *
     * @param screenComponents UI components whose bounds are already in screen coordinates
     */
    public void render(int screenWidth, int screenHeight, int mouseX, int mouseY, int contentX, int contentY,
        int contentW, int contentH, int scrollY, float zoom, @Nullable LytDocument document,
        List<DebugComponent.ComponentEntry> screenComponents, FontRenderer fontRenderer) {
        if (!ModConfig.debug.guiDebugMode) {
            return;
        }

        this.currentDocument = document;

        if (!initialized) {
            initialized = true;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());
            GL11.glTranslatef(0.0F, 0.0F, OVERLAY_Z);

            HoveredElementInfo documentHoveredInfo = null;
            if (document != null) {
                int docX = Math.round((mouseX - contentX) / zoom);
                int docY = Math.round((mouseY - contentY) / zoom) + scrollY;
                documentHoveredInfo = hoverDetector.detectHoveredElement(document, docX, docY);

                if (documentHoveredInfo != null) {
                    adjustCoordinatesForRendering(documentHoveredInfo, contentX, contentY, scrollY, zoom);
                }
            }
            ScreenComponentHit screenComponentHit = detectScreenComponent(screenComponents, mouseX, mouseY);
            HoveredElementInfo hoveredInfo = screenComponentHit != null
                && (documentHoveredInfo == null || screenComponentHit.priority() > 0) ? screenComponentHit.info()
                    : documentHoveredInfo;

            controlPanel.updatePosition(screenWidth, screenHeight);
            controlPanel.render(mouseX, mouseY, fontRenderer);

            infoPanel.render(screenWidth, screenHeight, mouseX, mouseY, hoveredInfo, fontRenderer);

            renderCursorDot(mouseX, mouseY);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    @Nullable
    private ScreenComponentHit detectScreenComponent(List<DebugComponent.ComponentEntry> components, int mouseX,
        int mouseY) {
        DebugComponent.ComponentEntry bestComponent = null;
        int bestPriority = Integer.MIN_VALUE;
        long bestArea = Long.MAX_VALUE;
        for (DebugComponent.ComponentEntry component : components) {
            if (!component.containsPoint(mouseX, mouseY)) {
                continue;
            }
            var bounds = component.getBounds();
            long area = (long) bounds.width() * bounds.height();
            if (component.getPriority() > bestPriority || component.getPriority() == bestPriority && area < bestArea) {
                bestComponent = component;
                bestPriority = component.getPriority();
                bestArea = area;
            }
        }
        if (bestComponent == null) {
            return null;
        }
        var bounds = bestComponent.getBounds();
        HoveredElementInfo info = new HoveredElementInfo(
            "ScreenComponent$" + bestComponent.getName(),
            bounds.x(),
            bounds.y(),
            bounds.width(),
            bounds.height(),
            null);
        info.setScreenCoordinates(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        info.addExtraInfo("Component: " + bestComponent.getName());
        if (bestComponent.getExtraInfo() != null) {
            info.addExtraInfo(bestComponent.getExtraInfo());
        }
        return new ScreenComponentHit(info, bestPriority);
    }

    private record ScreenComponentHit(HoveredElementInfo info, int priority) {}

    /**
     * Adjust element coordinates from document space to screen space.
     * Applies cumulative scroll offsets from all ancestor scrollable containers.
     */
    private void adjustCoordinatesForRendering(HoveredElementInfo info, int contentX, int contentY, int scrollY,
        float zoom) {
        // Apply global scroll and zoom
        int screenX = contentX + Math.round(info.getX() * zoom);
        int screenY = contentY + Math.round((info.getY() - scrollY) * zoom);

        // Apply cumulative container scroll offsets (interpolated for smooth animation)
        screenX -= Math.round(info.getCumulativeScrollOffsetX() * zoom);
        screenY -= Math.round(info.getCumulativeScrollOffsetY() * zoom);

        int screenW = Math.round(info.getWidth() * zoom);
        int screenH = Math.round(info.getHeight() * zoom);

        info.setScreenCoordinates(screenX, screenY, screenW, screenH);

        if (info.hasParent()) {
            adjustCoordinatesForRendering(info.getParent(), contentX, contentY, scrollY, zoom);
        }
    }

    /**
     * Handle mouse click events.
     * Returns true if the click was consumed by the debug overlay.
     */
    public boolean handleMouseClick(int mouseX, int mouseY, int button) {
        if (!ModConfig.debug.guiDebugMode) {
            return false;
        }
        return controlPanel.handleMouseClick(mouseX, mouseY, button);
    }

    /**
     * Handle keyboard input.
     * Returns true if the key was consumed by the debug overlay.
     */
    public boolean handleKeyPress(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_C && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
            && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
            && Keyboard.isKeyDown(Keyboard.KEY_LMENU)) {
            toggleDebugMode();
            return true;
        }
        return false;
    }

    /**
     * Check if debug overlay is consuming mouse events (e.g., when menu is open).
     */
    public boolean isConsumingInput() {
        return ModConfig.debug.guiDebugMode && controlPanel.isExpanded();
    }

    private void toggleDebugMode() {
        ModConfig.debug.guiDebugMode = !ModConfig.debug.guiDebugMode;
        ModConfig.save();
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            new ChatComponentText("Debug Mode: " + (ModConfig.debug.guiDebugMode ? "§aEnabled" : "§cDisabled")));
    }

    private void renderCursorDot(int mouseX, int mouseY) {
        int color = ColorUtils.DEBUG_CURSOR.getColor();
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        ColorUtils.applyGlColor(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(mouseX, mouseY);
        GL11.glVertex2f(mouseX + 1, mouseY);
        GL11.glVertex2f(mouseX + 1, mouseY + 1);
        GL11.glVertex2f(mouseX, mouseY + 1);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /**
     * Print the current document tree to console.
     */
    public void printDocumentTree() {
        TreePrinter.printDocumentTree(currentDocument);
    }
}
