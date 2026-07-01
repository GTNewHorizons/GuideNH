package com.hfstudio.guidenh.guide.internal.debug;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.config.ModConfig;

/**
 * Renders debug information panel at the left-bottom corner of the screen.
 * Displays FPS, memory, mouse position, and hovered element details.
 */
public class DebugInfoPanel {

    private static final int LEFT_MARGIN = 5;
    private static final int BOTTOM_MARGIN = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int SEPARATOR_HEIGHT = 4;

    private final PerformanceMonitor performanceMonitor;
    private final DashedBorderRenderer borderRenderer;

    public DebugInfoPanel(PerformanceMonitor performanceMonitor, DashedBorderRenderer borderRenderer) {
        this.performanceMonitor = performanceMonitor;
        this.borderRenderer = borderRenderer;
    }

    public void render(int screenWidth, int screenHeight, int mouseX, int mouseY,
        @Nullable HoveredElementInfo hoveredInfo, FontRenderer fontRenderer) {
        if (!ModConfig.debug.guiDebugMode) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        List<String> lines = new ArrayList<>();
        collectBasicInfo(lines, mouseX, mouseY);

        if (hoveredInfo != null) {
            lines.add("");
            collectHoveredInfo(lines, hoveredInfo);

            if (hoveredInfo.hasParent() && ModConfig.debug.showParentInfo) {
                lines.add("");
                collectParentInfo(lines, hoveredInfo.getParent());
            }
        }

        renderLines(lines, screenHeight, fontRenderer);

        if (hoveredInfo != null && ModConfig.debug.showHoveredOutline) {
            renderHoveredOutline(hoveredInfo);
        }

        if (hoveredInfo != null && hoveredInfo.hasParent()
            && ModConfig.debug.showParentInfo
            && ModConfig.debug.showParentOutline) {
            renderParentOutline(hoveredInfo.getParent());
        }

        GL11.glPopAttrib();
    }

    private void collectBasicInfo(List<String> lines, int mouseX, int mouseY) {
        if (ModConfig.debug.showMousePosition) {
            lines.add(translate("guidenh.debug.info.mouse_pos", mouseX, mouseY));
        }

        if (ModConfig.debug.showFps) {
            int fps = performanceMonitor.getFps();
            lines.add(translate("guidenh.debug.info.fps", fps));
        }

        if (ModConfig.debug.showMemory) {
            long usedMB = performanceMonitor.getUsedMemoryMB();
            long maxMB = performanceMonitor.getMaxMemoryMB();
            int percentage = performanceMonitor.getMemoryPercentage();
            lines.add(translate("guidenh.debug.info.memory", usedMB, maxMB, percentage));
        }
    }

    private void collectHoveredInfo(List<String> lines, HoveredElementInfo info) {
        if (!ModConfig.debug.showHoveredInfo) {
            return;
        }

        lines.add(translate("guidenh.debug.info.hovered_element"));

        if (ModConfig.debug.showHoveredTheme) {
            lines.add("  " + translate("guidenh.debug.info.class_name", simplifyClassName(info.getClassName())));
        }

        if (ModConfig.debug.showHoveredSize) {
            lines.add("  " + translate("guidenh.debug.info.size", info.getWidth(), info.getHeight()));
        }

        if (ModConfig.debug.showHoveredPosition) {
            lines.add("  " + translate("guidenh.debug.info.position", info.getX(), info.getY()));
        }

        if (ModConfig.debug.showHoveredExtra && !info.getExtraInfo()
            .isEmpty()) {
            for (String extra : info.getExtraInfo()) {
                lines.add("  " + extra);
            }
        }
    }

    private void collectParentInfo(List<String> lines, HoveredElementInfo parent) {
        lines.add(translate("guidenh.debug.info.parent_element"));

        if (ModConfig.debug.showParentTheme) {
            lines.add("  " + translate("guidenh.debug.info.class_name", simplifyClassName(parent.getClassName())));
        }

        if (ModConfig.debug.showParentSize) {
            lines.add("  " + translate("guidenh.debug.info.size", parent.getWidth(), parent.getHeight()));
        }

        if (ModConfig.debug.showParentPosition) {
            lines.add("  " + translate("guidenh.debug.info.position", parent.getX(), parent.getY()));
        }
    }

    private void renderLines(List<String> lines, int screenHeight, FontRenderer fontRenderer) {
        float scale = ModConfig.debug.debugTextScale;
        int textColor = ModConfig.debug.debugTextColor;

        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);

        int scaledLeftMargin = (int) (LEFT_MARGIN / scale);
        int scaledLineHeight = (int) (LINE_HEIGHT / scale);
        int scaledBottomMargin = (int) (BOTTOM_MARGIN / scale);
        int scaledHeight = (int) (screenHeight / scale);

        int y = scaledHeight - scaledBottomMargin - (lines.size() * scaledLineHeight);

        for (String line : lines) {
            if (!line.isEmpty()) {
                fontRenderer.drawStringWithShadow(line, scaledLeftMargin, y, textColor);
            }
            y += scaledLineHeight;
        }

        GL11.glPopMatrix();
    }

    private void renderHoveredOutline(HoveredElementInfo info) {
        int color = ModConfig.debug.debugOutlineColor;
        if (color == 0) {
            color = ModConfig.debug.debugTextColor;
        }
        borderRenderer.renderDashedBorder(
            info.getScreenX(),
            info.getScreenY(),
            info.getScreenWidth(),
            info.getScreenHeight(),
            color);
        renderClassNameLabel(info.getScreenX(), info.getScreenY(), info.getClassName());
    }

    private void renderParentOutline(HoveredElementInfo parent) {
        int color = ModConfig.debug.debugOutlineColor;
        if (color == 0) {
            color = ModConfig.debug.debugTextColor;
        }
        int alphaColor = (color & 0x00FFFFFF) | 0x4D000000;
        borderRenderer.renderDashedBorder(
            parent.getScreenX(),
            parent.getScreenY(),
            parent.getScreenWidth(),
            parent.getScreenHeight(),
            alphaColor);
    }

    private void renderClassNameLabel(int x, int y, String className) {
        String simpleName = simplifyClassName(className);
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        int textWidth = fontRenderer.getStringWidth(simpleName);
        int textHeight = fontRenderer.FONT_HEIGHT;

        int labelX = x + 2;
        int labelY = y - textHeight - 2;

        if (labelY < 0) {
            labelY = y + 2;
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        drawRect(labelX - 2, labelY - 1, textWidth + 4, textHeight + 2, 0xD0000000);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        fontRenderer.drawStringWithShadow(simpleName, labelX, labelY, 0xFFFFFFFF);
    }

    private void drawRect(int x, int y, int width, int height, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y + height);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
    }

    private String simplifyClassName(String fullClassName) {
        if (fullClassName == null) {
            return "Unknown";
        }
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }

    private String translate(String key, Object... args) {
        String translated = StatCollector.translateToLocalFormatted(key, args);
        return translated.equals(key) ? formatFallback(key, args) : translated;
    }

    private String formatFallback(String key, Object... args) {
        String simplified = key.replace("guidenh.debug.info.", "")
            .replace("_", " ");
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder(simplified).append(": ");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(args[i]);
            }
            return sb.toString();
        }
        return simplified;
    }
}
