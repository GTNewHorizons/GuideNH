package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneConditionParser;
import com.hfstudio.guidenh.guide.scene.annotation.PonderInputAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Compiles {@code <InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />} into the mouse-input icon a
 * scene shows to hint that the player should click somewhere.
 *
 * <p>
 * The icon is anchored at a world position, optionally prefixed with a modifier key and followed by an
 * item icon, so a scene can say "sneak and right-click with this item". The same tag is understood by
 * the site exporter, which renders the equivalent HTML annotation.
 */
public class InputAnnotationElementCompiler implements SceneElementTagCompiler {

    private static final int POSITION_PARTS = 3;

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("InputAnnotation");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        LytGuidebookScene scene = AnnotationTagCompiler.CURRENT_SCENE.get();
        if (scene == null) {
            errorSink.appendError(compiler, "InputAnnotation used outside <GameScene>", el);
            return;
        }

        PonderInputAnnotation annotation = new PonderInputAnnotation(
            readPosition(compiler, errorSink, el),
            resolveInputType(el.getAttributeString("inputType", null)));
        String modifier = trimToNull(el.getAttributeString("modifier", null));
        if (modifier != null) {
            annotation.setModifier(modifier);
        }
        annotation.setItemStack(resolveItem(compiler, errorSink, el));
        annotation.setStructureLibCondition(StructureLibSceneConditionParser.parse(compiler, errorSink, el));
        scene.addAnnotation(annotation);
    }

    /** Reads {@code pos="x y z"}, falling back to separate {@code x}, {@code y} and {@code z} attributes. */
    private static Vector3f readPosition(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        String pos = trimToNull(el.getAttributeString("pos", null));
        if (pos != null) {
            String[] parts = pos.split("[\\s,]+");
            if (parts.length >= POSITION_PARTS) {
                try {
                    return new Vector3f(
                        Float.parseFloat(parts[0]),
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2]));
                } catch (NumberFormatException e) {
                    errorSink.appendError(compiler, "pos must be three numbers, for example pos=\"0.5 1.5 0.5\"", el);
                }
            } else {
                errorSink.appendError(compiler, "pos must contain three numbers", el);
            }
        }
        return new Vector3f(
            MdxAttrs.getFloat(compiler, errorSink, el, "x", 0f),
            MdxAttrs.getFloat(compiler, errorSink, el, "y", 0f),
            MdxAttrs.getFloat(compiler, errorSink, el, "z", 0f));
    }

    /** Accepts the short names plus the spellings the site exporter accepts. */
    private static PonderInputAnnotation.InputType resolveInputType(@Nullable String raw) {
        if (raw == null) {
            return PonderInputAnnotation.InputType.LMB;
        }
        return switch (raw.trim()
            .toLowerCase(Locale.ROOT)) {
            case "rmb", "rightclick", "right_click", "right-click" -> PonderInputAnnotation.InputType.RMB;
            case "scroll", "scrollwheel", "scroll_wheel", "scroll-wheel" -> PonderInputAnnotation.InputType.SCROLL;
            default -> PonderInputAnnotation.InputType.LMB;
        };
    }

    /**
     * Resolves the optional item icon. A malformed id is an authoring mistake and is reported as an
     * error, while a well formed id that no mod registers is only warned about: the annotation is still
     * useful without its item icon.
     */
    @Nullable
    private static ItemStack resolveItem(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        String itemId = trimToNull(el.getAttributeString("item", null));
        if (itemId == null) {
            return null;
        }
        ItemStack stack;
        try {
            stack = GuideDisplayItemStacks.resolveItemStack(itemId, "minecraft");
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, "item is not a valid item id: " + itemId, el);
            return null;
        }
        if (stack == null || stack.getItem() == null) {
            GuideDebugLog.warnAlways("[GuideNH] [InputAnnotation] No item is registered as '{}'", itemId);
            return null;
        }
        return stack;
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
