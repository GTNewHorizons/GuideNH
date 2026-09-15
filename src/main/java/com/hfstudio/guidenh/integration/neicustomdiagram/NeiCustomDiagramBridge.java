package com.hfstudio.guidenh.integration.neicustomdiagram;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.github.dcysteine.neicustomdiagram.api.diagram.Diagram;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramState;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.Component;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.CustomInteractable;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.InteractiveComponentGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.ComponentLabel;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Slot;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.TooltipElement;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.TooltipLine;
import com.github.dcysteine.neicustomdiagram.api.draw.Draw;
import com.github.dcysteine.neicustomdiagram.api.draw.Point;
import com.github.dcysteine.neicustomdiagram.main.config.ConfigOptions;
import com.google.common.collect.ImmutableList;
import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.recipe.NeiHandlerRenderer;
import com.hfstudio.guidenh.guide.internal.tooltip.AppendedItemTooltip;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.mixins.late.compat.neicustomdiagram.AccessorCustomInteractable;

import cpw.mods.fml.common.Optional;

public class NeiCustomDiagramBridge {

    private NeiCustomDiagramBridge() {}

    public static boolean isDiagramGroupHandler(Object handler) {
        return Mods.NeiCustomDiagram.isModLoaded() && isDiagramGroupHandlerImpl(handler);
    }

    @Optional.Method(modid = "neicustomdiagram")
    private static boolean isDiagramGroupHandlerImpl(Object handler) {
        return handler instanceof DiagramGroup;
    }

    /**
     * Renders nei-custom-diagram {@code DiagramGroup} content in the Guidebook.
     * <p>
     * {@code renderX}/{@code renderY} remain parent-local GUI coordinates (matching {@code Gui});
     * {@code guiScissorAbs*} are absolute GUI coords (viewport space), same convention as {@code VanillaRenderContext}
     * scissor 闁?required because {@code GL_SCISSOR} ignores {@code GL_MODELVIEW}.
     *
     * <p>
     * For wide diagrams, {@code guiScissorAbsW} may be smaller than intrinsic layout (NEI {@code HandlerInfo}
     * width defaults); clip width is inflated up to the scaled GUI bounds.
     */
    public static void renderEmbedded(Object handler, int recipeIndex, int renderX, int renderY, int guiScissorAbsX,
        int guiScissorAbsY, int guiScissorAbsW, int guiScissorAbsH) {
        if (Mods.NeiCustomDiagram.isModLoaded()) {
            renderEmbeddedImpl(
                handler,
                recipeIndex,
                renderX,
                renderY,
                guiScissorAbsX,
                guiScissorAbsY,
                guiScissorAbsW,
                guiScissorAbsH);
        }
    }

    @Optional.Method(modid = "neicustomdiagram")
    private static void renderEmbeddedImpl(Object handler, int recipeIndex, int renderX, int renderY,
        int guiScissorAbsX, int guiScissorAbsY, int guiScissorAbsW, int guiScissorAbsH) {
        if (!(handler instanceof DiagramGroup diagramGroup) || guiScissorAbsW <= 0 || guiScissorAbsH <= 0) {
            return;
        }
        Diagram diagram = diagramAt(diagramGroup, recipeIndex);
        if (diagram == null) {
            return;
        }
        DiagramState diagramState = diagramGroup.diagramState;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scaledW = sr.getScaledWidth();
        int scaledH = sr.getScaledHeight();
        int gw = guiScissorAbsW;
        int gh = guiScissorAbsH;
        int maxGw = Math.max(1, scaledW - guiScissorAbsX);
        gw = Math.clamp(gw, gw + 400, maxGw);
        int maxGh = Math.max(1, scaledH - guiScissorAbsY);
        gh = Math.min(maxGh, gh + 32);

        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_SCISSOR_BIT
                | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            applyAbsoluteGuiScissor(guiScissorAbsX, guiScissorAbsY, gw, gh);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glTranslatef(renderX, renderY, 0f);
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());
            diagram.drawBackground(diagramState);
            renderForeground(diagram, diagramState, guiScissorAbsX, guiScissorAbsY, gw, gh);
        } catch (Throwable t) {
            GuideDebugLog.warnAlways("[GuideNH] [NeiCustomDiagramBridge] Embedded nei-custom-diagram render failed", t);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());
        }
    }

    public static GuideTooltip getEmbeddedTooltip(Object handler, int recipeIndex, int localMouseX, int localMouseY) {
        return Mods.NeiCustomDiagram.isModLoaded()
            ? getEmbeddedTooltipImpl(handler, recipeIndex, localMouseX, localMouseY)
            : null;
    }

    @Optional.Method(modid = "neicustomdiagram")
    private static GuideTooltip getEmbeddedTooltipImpl(Object handler, int recipeIndex, int localMouseX,
        int localMouseY) {
        if (!(handler instanceof DiagramGroup diagramGroup)) {
            return null;
        }
        Diagram diagram = diagramAt(diagramGroup, recipeIndex);
        if (diagram == null) {
            return null;
        }
        DiagramState diagramState = diagramGroup.diagramState;

        try {
            Interactable hovered = findHoveredInteractable(diagram, diagramState, localMouseX, localMouseY);
            if (hovered instanceof InteractiveComponentGroup interactiveGroup) {
                return tooltipForInteractiveComponentGroup(interactiveGroup, diagramState);
            }
            if (hovered instanceof CustomInteractable customInteractable) {
                List<String> lines = flattenTooltip(customInteractable.tooltip());
                return lines.isEmpty() ? null : new TextTooltip(String.join("\n", lines));
            }
        } catch (Throwable t) {
            GuideDebugLog
                .warnAlways("[GuideNH] [NeiCustomDiagramBridge] Embedded nei-custom-diagram tooltip lookup failed", t);
        }
        return null;
    }

    private static void renderForeground(Diagram diagram, DiagramState diagramState, int clipX, int clipY,
        int clipWidth, int clipHeight) {
        for (Interactable interactable : diagram.interactables(diagramState)) {
            if (interactable instanceof Slot) {
                continue;
            }

            reapplyClipState(clipX, clipY, clipWidth, clipHeight);
            if (interactable instanceof InteractiveComponentGroup interactiveGroup) {
                renderInteractiveComponentGroup(interactiveGroup, diagramState, clipX, clipY, clipWidth, clipHeight);
            } else if (interactable instanceof CustomInteractable customInteractable) {
                renderCustomInteractable(customInteractable, diagramState, clipX, clipY, clipWidth, clipHeight);
            } else {
                interactable.draw(diagramState);
            }
        }
    }

    private static void renderInteractiveComponentGroup(InteractiveComponentGroup interactable,
        DiagramState diagramState, int clipX, int clipY, int clipWidth, int clipHeight) {
        renderDisplayComponent(
            interactable.currentComponent(diagramState),
            interactable.position(),
            clipX,
            clipY,
            clipWidth,
            clipHeight);
    }

    private static void renderCustomInteractable(CustomInteractable interactable, DiagramState diagramState, int clipX,
        int clipY, int clipWidth, int clipHeight) {
        if (interactable.drawable() instanceof ComponentLabel componentLabel) {
            Point position = interactable.position();
            ((AccessorCustomInteractable) interactable).getDrawBackground()
                .accept(position);
            renderComponent(componentLabel.component(), position, clipX, clipY, clipWidth, clipHeight);
            ((AccessorCustomInteractable) interactable).getDrawForeground()
                .accept(position);
            return;
        }

        interactable.draw(diagramState);
    }

    private static void renderDisplayComponent(DisplayComponent displayComponent, Point position, int clipX, int clipY,
        int clipWidth, int clipHeight) {
        if (displayComponent.stack() instanceof ItemStack stack) {
            reapplyClipState(clipX, clipY, clipWidth, clipHeight);
            NeiHandlerRenderer.drawItemIcon(stack, position.x() - 8, position.y() - 8);
            renderDisplayComponentDecorations(displayComponent, position);
            return;
        }

        displayComponent.draw(position);
    }

    private static void renderComponent(Component component, Point position, int clipX, int clipY, int clipWidth,
        int clipHeight) {
        if (component.stack() instanceof ItemStack stack) {
            reapplyClipState(clipX, clipY, clipWidth, clipHeight);
            NeiHandlerRenderer.drawItemIcon(stack, position.x() - 8, position.y() - 8);
            return;
        }

        component.draw(position);
    }

    private static void renderDisplayComponentDecorations(DisplayComponent displayComponent, Point position) {
        var stackSize = displayComponent.stackSize();
        if (stackSize.isPresent() && shouldDrawStackSize(stackSize.get())) {
            Draw.drawStackSize(stackSize.get(), position);
        }

        var additionalInfo = displayComponent.additionalInfo();
        if (additionalInfo.isPresent() && !additionalInfo.get()
            .isEmpty()) {
            Draw.drawAdditionalInfo(additionalInfo.get(), position, true);
        }
    }

    private static boolean shouldDrawStackSize(int stackSize) {
        return stackSize != 1 || Boolean.TRUE.equals(ConfigOptions.SHOW_STACK_SIZE_ONE.get());
    }

    private static void reapplyClipState(int absGuiX, int absGuiY, int absGuiW, int absGuiH) {
        applyAbsoluteGuiScissor(absGuiX, absGuiY, absGuiW, absGuiH);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());
    }

    private static GuideTooltip tooltipForInteractiveComponentGroup(InteractiveComponentGroup hovered,
        DiagramState diagramState) {
        DisplayComponent displayComponent = hovered.currentComponent(diagramState);
        ItemStack stack = displayComponent.stack() instanceof ItemStack itemStack && itemStack.stackSize > 0 ? itemStack
            : null;
        List<String> extraLines = new ArrayList<>();
        appendTooltipLines(
            extraLines,
            displayComponent.descriptionTooltip(),
            stack == null ? null : stack.getDisplayName());
        appendTooltipLines(extraLines, hovered.slotTooltip, null);
        appendTooltipLines(extraLines, displayComponent.additionalTooltip(), null);
        appendTooltipLines(extraLines, hovered.cycleTooltip(diagramState), null);

        if (stack == null) {
            return extraLines.isEmpty() ? null : new TextTooltip(String.join("\n", extraLines));
        }
        return extraLines.isEmpty() ? new ItemTooltip(stack) : new AppendedItemTooltip(stack, extraLines);
    }

    private static Interactable findHoveredInteractable(Diagram diagram, DiagramState diagramState, int localMouseX,
        int localMouseY) {
        Point point = Point.create(localMouseX, localMouseY);
        for (Interactable interactable : diagram.interactables(diagramState)) {
            if (interactable.checkBoundingBox(point)) {
                return interactable;
            }
        }
        return null;
    }

    private static void appendTooltipLines(List<String> output, Tooltip tooltip, String firstLineToSkip) {
        for (String line : flattenTooltip(tooltip)) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            if (firstLineToSkip != null && firstLineToSkip.equals(line)) {
                firstLineToSkip = null;
                continue;
            }
            firstLineToSkip = null;
            if (!output.contains(line)) {
                output.add(line);
            }
        }
    }

    private static List<String> flattenTooltip(Tooltip tooltip) {
        try {
            List<String> flattened = new ArrayList<>();
            for (TooltipLine line : tooltip.lines()) {
                String text = flattenTooltipLine(line);
                if (!text.isEmpty()) {
                    flattened.add(text);
                }
            }
            return flattened;
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static String flattenTooltipLine(TooltipLine line) {
        StringBuilder builder = new StringBuilder();
        for (TooltipElement element : line.elements()) {
            TooltipElement.ElementType type = element.type();
            if (type == TooltipElement.ElementType.TEXT) {
                appendToken(builder, element.text());
            } else if (type == TooltipElement.ElementType.COMPONENT_DESCRIPTION) {
                appendToken(
                    builder,
                    element.componentDescription()
                        .description());
            }
            if (type == TooltipElement.ElementType.SPACING && !builder.isEmpty()
                && builder.charAt(builder.length() - 1) != ' ') {
                builder.append(' ');
            }
        }
        return builder.toString()
            .trim();
    }

    private static void appendToken(StringBuilder builder, String token) {
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != ' ') {
            builder.append(' ');
        }
        builder.append(trimmed);
    }

    @Nullable
    private static Diagram diagramAt(DiagramGroup diagramGroup, int recipeIndex) {
        ImmutableList<Diagram> diagrams = diagramGroup.diagrams;
        if (recipeIndex < 0 || recipeIndex >= diagrams.size()) {
            return null;
        }
        return diagrams.get(recipeIndex);
    }

    private static void applyAbsoluteGuiScissor(int guiX, int guiY, int guiW, int guiH) {
        Minecraft mc = Minecraft.getMinecraft();
        int scale = DisplayScale.scaleFactor();
        int sx = guiX * scale;
        int sy = mc.displayHeight - (guiY + guiH) * scale;
        int sw = guiW * scale;
        int sh = guiH * scale;
        GL11.glScissor(sx, Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
    }
}
