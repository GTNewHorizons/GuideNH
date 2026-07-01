package com.hfstudio.guidenh.guide.internal.debug;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.config.ModConfig;

/**
 * Debug control panel displayed at center-bottom of the screen.
 * Provides buttons and dropdown menus for debug options.
 */
public class DebugControlPanel {

    private static final int PANEL_WIDTH = 180;
    private static final int PANEL_HEIGHT = 16;
    private static final int BUTTON_PADDING = 2;
    private static final int DROPDOWN_ITEM_HEIGHT = 14;

    private int panelX;
    private int panelY;
    private boolean expanded = false;
    private DebugMenuItem hoveredItem = null;
    private DebugMenuItem expandedMenu = null;

    private final List<DebugMenuItem> menuItems = new ArrayList<>();
    private GuideDebugOverlay debugOverlay;

    public DebugControlPanel() {
        initializeMenu();
    }

    private void initializeMenu() {
        menuItems.clear();

        DebugMenuItem printTrees = new DebugMenuItem("guidenh.debug.menu.print_trees", DebugMenuAction.PRINT_TREES);
        menuItems.add(printTrees);

        DebugMenuItem hoveredInfoMenu = new DebugMenuItem(
            "guidenh.debug.menu.hovered_info",
            DebugMenuAction.TOGGLE_HOVERED_INFO);
        hoveredInfoMenu.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.hovered_info.show_any", DebugMenuAction.TOGGLE_HOVERED_INFO));
        hoveredInfoMenu.addSubmenuItem(
            new DebugMenuItem(
                "guidenh.debug.menu.hovered_info.show_position",
                DebugMenuAction.TOGGLE_HOVERED_POSITION));
        hoveredInfoMenu.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.hovered_info.show_size", DebugMenuAction.TOGGLE_HOVERED_SIZE));
        hoveredInfoMenu.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.hovered_info.show_theme", DebugMenuAction.TOGGLE_HOVERED_THEME));
        hoveredInfoMenu.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.hovered_info.show_extra", DebugMenuAction.TOGGLE_HOVERED_EXTRA));
        hoveredInfoMenu.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.hovered_info.show_outline", DebugMenuAction.TOGGLE_HOVERED_OUTLINE));
        menuItems.add(hoveredInfoMenu);

        DebugMenuItem parentInfoMenu = new DebugMenuItem(
            "guidenh.debug.menu.parent_info",
            DebugMenuAction.TOGGLE_PARENT_INFO);
        parentInfoMenu.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.parent_info.show_any", DebugMenuAction.TOGGLE_PARENT_INFO));
        parentInfoMenu.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.parent_info.show_position", DebugMenuAction.TOGGLE_PARENT_POSITION));
        parentInfoMenu.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.parent_info.show_size", DebugMenuAction.TOGGLE_PARENT_SIZE));
        parentInfoMenu.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.parent_info.show_theme", DebugMenuAction.TOGGLE_PARENT_THEME));
        parentInfoMenu.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.parent_info.show_outline", DebugMenuAction.TOGGLE_PARENT_OUTLINE));
        menuItems.add(parentInfoMenu);

        DebugMenuItem displayOptions = new DebugMenuItem("guidenh.debug.menu.display_options", DebugMenuAction.NONE);
        displayOptions.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.display_options.show_fps", DebugMenuAction.TOGGLE_FPS));
        displayOptions.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.display_options.show_memory", DebugMenuAction.TOGGLE_MEMORY));
        displayOptions.addSubmenuItem(
            new DebugMenuItem("guidenh.debug.menu.display_options.show_mouse", DebugMenuAction.TOGGLE_MOUSE_POSITION));
        menuItems.add(displayOptions);

        DebugMenuItem recompile = new DebugMenuItem(
            "guidenh.debug.menu.recompile_page",
            DebugMenuAction.RECOMPILE_PAGE);
        menuItems.add(recompile);

        DebugMenuItem exportDebug = new DebugMenuItem(
            "guidenh.debug.menu.export_debug_data",
            DebugMenuAction.EXPORT_DEBUG_DATA);
        menuItems.add(exportDebug);
    }

    public void setDebugOverlay(GuideDebugOverlay debugOverlay) {
        this.debugOverlay = debugOverlay;
    }

    public void updatePosition(int screenWidth, int screenHeight) {
        panelX = (screenWidth - PANEL_WIDTH) / 2;
        panelY = screenHeight - PANEL_HEIGHT - 2;
    }

    public void render(int mouseX, int mouseY, FontRenderer fontRenderer) {
        if (!ModConfig.debug.guiDebugMode) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        drawRoundedRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0x80000000, 0xFFAAAAAA);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        String label = Minecraft.getMinecraft().fontRenderer.trimStringToWidth("Debug Options", PANEL_WIDTH - 4);
        fontRenderer.drawStringWithShadow(
            label,
            panelX + (PANEL_WIDTH - fontRenderer.getStringWidth(label)) / 2,
            panelY + (PANEL_HEIGHT - 8) / 2,
            0xFFFFFF);

        if (expanded || expandedMenu != null) {
            renderExpandedMenu(mouseX, mouseY, fontRenderer);
        }

        GL11.glPopAttrib();
    }

    private void renderExpandedMenu(int mouseX, int mouseY, FontRenderer fontRenderer) {
        int menuY = panelY - (menuItems.size() * DROPDOWN_ITEM_HEIGHT) - 2;
        int menuHeight = menuItems.size() * DROPDOWN_ITEM_HEIGHT + 4;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        drawRoundedRect(panelX, menuY, PANEL_WIDTH, menuHeight, 0xD0000000, 0xFFCCCCCC);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        hoveredItem = null;

        for (int i = 0; i < menuItems.size(); i++) {
            DebugMenuItem item = menuItems.get(i);
            int itemY = menuY + 2 + (i * DROPDOWN_ITEM_HEIGHT);
            boolean isHovered = mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH
                && mouseY >= itemY
                && mouseY < itemY + DROPDOWN_ITEM_HEIGHT;

            if (isHovered) {
                hoveredItem = item;
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                drawRect(panelX + 2, itemY, PANEL_WIDTH - 4, DROPDOWN_ITEM_HEIGHT, 0x80FFFFFF);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            String displayText = translateKey(item.getTranslationKey());
            if (item.hasSubmenu()) {
                displayText += " >";
            }

            boolean checkState = getCheckState(item.getAction());
            if (checkState && !item.hasSubmenu()) {
                displayText = "✓ " + displayText;
            }

            fontRenderer.drawStringWithShadow(displayText, panelX + 6, itemY + 3, 0xFFFFFF);
        }

        if (expandedMenu != null && expandedMenu.hasSubmenu()) {
            renderSubmenu(expandedMenu, mouseX, mouseY, fontRenderer);
        }
    }

    private void renderSubmenu(DebugMenuItem parentItem, int mouseX, int mouseY, FontRenderer fontRenderer) {
        List<DebugMenuItem> submenuItems = parentItem.getSubmenuItems();
        int submenuX = panelX + PANEL_WIDTH + 2;
        int parentIndex = menuItems.indexOf(parentItem);
        int submenuY = panelY - (menuItems.size() * DROPDOWN_ITEM_HEIGHT)
            - 2
            + 2
            + (parentIndex * DROPDOWN_ITEM_HEIGHT);
        int submenuHeight = submenuItems.size() * DROPDOWN_ITEM_HEIGHT + 4;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        drawRoundedRect(submenuX, submenuY, PANEL_WIDTH, submenuHeight, 0xD0000000, 0xFFCCCCCC);

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        for (int i = 0; i < submenuItems.size(); i++) {
            DebugMenuItem item = submenuItems.get(i);
            int itemY = submenuY + 2 + (i * DROPDOWN_ITEM_HEIGHT);
            boolean isHovered = mouseX >= submenuX && mouseX <= submenuX + PANEL_WIDTH
                && mouseY >= itemY
                && mouseY < itemY + DROPDOWN_ITEM_HEIGHT;

            if (isHovered) {
                hoveredItem = item;
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                drawRect(submenuX + 2, itemY, PANEL_WIDTH - 4, DROPDOWN_ITEM_HEIGHT, 0x80FFFFFF);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            boolean checkState = getCheckState(item.getAction());
            String displayText = (checkState ? "✓ " : "  ") + translateKey(item.getTranslationKey());
            fontRenderer.drawStringWithShadow(displayText, submenuX + 6, itemY + 3, 0xFFFFFF);
        }
    }

    public boolean handleMouseClick(int mouseX, int mouseY, int button) {
        if (!ModConfig.debug.guiDebugMode || button != 0) {
            return false;
        }

        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH && mouseY >= panelY && mouseY < panelY + PANEL_HEIGHT) {
            expanded = !expanded;
            if (!expanded) {
                expandedMenu = null;
            }
            return true;
        }

        if (expanded || expandedMenu != null) {
            if (hoveredItem != null) {
                if (hoveredItem.hasSubmenu()) {
                    expandedMenu = hoveredItem;
                } else {
                    executeAction(hoveredItem.getAction());
                    expanded = false;
                    expandedMenu = null;
                }
                return true;
            }
        }

        if (expanded || expandedMenu != null) {
            expanded = false;
            expandedMenu = null;
            return true;
        }

        return false;
    }

    private void executeAction(DebugMenuAction action) {
        switch (action) {
            case PRINT_TREES -> printDocumentTree();
            case TOGGLE_HOVERED_INFO -> ModConfig.debug.showHoveredInfo = !ModConfig.debug.showHoveredInfo;
            case TOGGLE_HOVERED_POSITION -> ModConfig.debug.showHoveredPosition = !ModConfig.debug.showHoveredPosition;
            case TOGGLE_HOVERED_SIZE -> ModConfig.debug.showHoveredSize = !ModConfig.debug.showHoveredSize;
            case TOGGLE_HOVERED_THEME -> ModConfig.debug.showHoveredTheme = !ModConfig.debug.showHoveredTheme;
            case TOGGLE_HOVERED_EXTRA -> ModConfig.debug.showHoveredExtra = !ModConfig.debug.showHoveredExtra;
            case TOGGLE_HOVERED_OUTLINE -> ModConfig.debug.showHoveredOutline = !ModConfig.debug.showHoveredOutline;
            case TOGGLE_PARENT_INFO -> ModConfig.debug.showParentInfo = !ModConfig.debug.showParentInfo;
            case TOGGLE_PARENT_POSITION -> ModConfig.debug.showParentPosition = !ModConfig.debug.showParentPosition;
            case TOGGLE_PARENT_SIZE -> ModConfig.debug.showParentSize = !ModConfig.debug.showParentSize;
            case TOGGLE_PARENT_THEME -> ModConfig.debug.showParentTheme = !ModConfig.debug.showParentTheme;
            case TOGGLE_PARENT_OUTLINE -> ModConfig.debug.showParentOutline = !ModConfig.debug.showParentOutline;
            case TOGGLE_FPS -> ModConfig.debug.showFps = !ModConfig.debug.showFps;
            case TOGGLE_MEMORY -> ModConfig.debug.showMemory = !ModConfig.debug.showMemory;
            case TOGGLE_MOUSE_POSITION -> ModConfig.debug.showMousePosition = !ModConfig.debug.showMousePosition;
            case RECOMPILE_PAGE -> recompilePage();
            case EXPORT_DEBUG_DATA -> exportDebugData();
        }
        ModConfig.save();
    }

    private boolean getCheckState(DebugMenuAction action) {
        return switch (action) {
            case TOGGLE_HOVERED_INFO -> ModConfig.debug.showHoveredInfo;
            case TOGGLE_HOVERED_POSITION -> ModConfig.debug.showHoveredPosition;
            case TOGGLE_HOVERED_SIZE -> ModConfig.debug.showHoveredSize;
            case TOGGLE_HOVERED_THEME -> ModConfig.debug.showHoveredTheme;
            case TOGGLE_HOVERED_EXTRA -> ModConfig.debug.showHoveredExtra;
            case TOGGLE_HOVERED_OUTLINE -> ModConfig.debug.showHoveredOutline;
            case TOGGLE_PARENT_INFO -> ModConfig.debug.showParentInfo;
            case TOGGLE_PARENT_POSITION -> ModConfig.debug.showParentPosition;
            case TOGGLE_PARENT_SIZE -> ModConfig.debug.showParentSize;
            case TOGGLE_PARENT_THEME -> ModConfig.debug.showParentTheme;
            case TOGGLE_PARENT_OUTLINE -> ModConfig.debug.showParentOutline;
            case TOGGLE_FPS -> ModConfig.debug.showFps;
            case TOGGLE_MEMORY -> ModConfig.debug.showMemory;
            case TOGGLE_MOUSE_POSITION -> ModConfig.debug.showMousePosition;
            default -> false;
        };
    }

    private void printDocumentTree() {
        if (debugOverlay != null) {
            debugOverlay.printDocumentTree();
        }
    }

    private void recompilePage() {
        System.out.println("=== Recompile Current Page ===");
        // TODO: Implement page recompilation
    }

    private void exportDebugData() {
        System.out.println("=== Export Debug Data ===");
        // TODO: Implement debug data export
    }

    private String translateKey(String key) {
        String translated = StatCollector.translateToLocal(key);
        return translated.equals(key) ? key.replace("guidenh.debug.menu.", "")
            .replace("_", " ") : translated;
    }

    private void drawRoundedRect(int x, int y, int width, int height, int bgColor, int borderColor) {
        drawRect(x, y, width, height, bgColor);
        drawBorder(x, y, width, height, borderColor);
    }

    private void drawRect(int x, int y, int width, int height, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(red, green, blue, alpha);
        tessellator.addVertex(x, y + height, 0.0);
        tessellator.addVertex(x + width, y + height, 0.0);
        tessellator.addVertex(x + width, y, 0.0);
        tessellator.addVertex(x, y, 0.0);
        tessellator.draw();
    }

    private void drawBorder(int x, int y, int width, int height, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        GL11.glLineWidth(1.0f);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINE_LOOP);
        tessellator.setColorRGBA_F(red, green, blue, alpha);
        tessellator.addVertex(x, y, 0.0);
        tessellator.addVertex(x + width, y, 0.0);
        tessellator.addVertex(x + width, y + height, 0.0);
        tessellator.addVertex(x, y + height, 0.0);
        tessellator.draw();
    }

    public boolean isExpanded() {
        return expanded || expandedMenu != null;
    }
}
