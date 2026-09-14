package com.hfstudio.guidenh.integration.nei;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorMultilineTextArea;
import com.hfstudio.guidenh.integration.Mods;

public class GuideScreenNeiBridge {

    protected GuideScreenNeiBridge() {}

    public static int reservedSidePixels(EditorAccess editorAccess) {
        return Mods.NotEnoughItems.isModLoaded() ? GuideScreenNeiNativeBridge.reservedSidePixels(editorAccess) : 0;
    }

    public static int reservedBottomPixels(EditorAccess editorAccess) {
        return Mods.NotEnoughItems.isModLoaded() ? GuideScreenNeiNativeBridge.reservedBottomPixels(editorAccess) : 0;
    }

    public static int layoutStateVersion(EditorAccess editorAccess) {
        return Mods.NotEnoughItems.isModLoaded() ? GuideScreenNeiNativeBridge.layoutStateVersion(editorAccess) : 0;
    }

    public static boolean isDraggingItem() {
        return Mods.NotEnoughItems.isModLoaded() && GuideScreenNeiNativeBridge.isDraggingItem();
    }

    public static boolean mouseClicked(EditorAccess editorAccess, int mouseX, int mouseY, int button) {
        return Mods.NotEnoughItems.isModLoaded()
            && GuideScreenNeiNativeBridge.mouseClicked(editorAccess, mouseX, mouseY, button);
    }

    public static boolean mouseDragged(EditorAccess editorAccess, int mouseX, int mouseY, int button, long heldTime) {
        return Mods.NotEnoughItems.isModLoaded()
            && GuideScreenNeiNativeBridge.mouseDragged(editorAccess, mouseX, mouseY, button, heldTime);
    }

    public static boolean mouseReleased(EditorAccess editorAccess, int mouseX, int mouseY, int button) {
        return Mods.NotEnoughItems.isModLoaded()
            && GuideScreenNeiNativeBridge.mouseReleased(editorAccess, mouseX, mouseY, button);
    }

    public static boolean mouseScrolled(EditorAccess editorAccess, int mouseX, int mouseY, int wheelDelta) {
        return Mods.NotEnoughItems.isModLoaded()
            && GuideScreenNeiNativeBridge.mouseScrolled(editorAccess, mouseX, mouseY, wheelDelta);
    }

    public static boolean handleItemDrop(EditorAccess editorAccess, int mouseX, int mouseY) {
        return Mods.NotEnoughItems.isModLoaded()
            && GuideScreenNeiNativeBridge.handleItemDrop(editorAccess, mouseX, mouseY);
    }

    public static boolean keyTyped(EditorAccess editorAccess, char typedChar, int keyCode) {
        return Mods.NotEnoughItems.isModLoaded()
            && GuideScreenNeiNativeBridge.keyTyped(editorAccess, typedChar, keyCode);
    }

    public static boolean keyTypedForHoveredGuideItem(EditorAccess editorAccess, char typedChar, int keyCode) {
        return Mods.NotEnoughItems.isModLoaded()
            && GuideScreenNeiNativeBridge.keyTypedForHoveredGuideItem(editorAccess, typedChar, keyCode);
    }

    public static void tick(EditorAccess editorAccess) {
        if (Mods.NotEnoughItems.isModLoaded()) {
            GuideScreenNeiNativeBridge.tick(editorAccess);
        }
    }

    public static void drawNativeNei(EditorAccess editorAccess, int mouseX, int mouseY) {
        if (Mods.NotEnoughItems.isModLoaded()) {
            GuideScreenNeiNativeBridge.drawNativeNei(editorAccess, mouseX, mouseY);
        }
    }

    public static void drawNativeNeiTooltip(EditorAccess editorAccess, int mouseX, int mouseY) {
        if (Mods.NotEnoughItems.isModLoaded()) {
            GuideScreenNeiNativeBridge.drawNativeNeiTooltip(editorAccess, mouseX, mouseY);
        }
    }

    public static void init() {
        if (Mods.NotEnoughItems.isModLoaded()) {
            GuideScreenNeiNativeBridge.init();
        }
    }

    /** Ensures the NEI GuiContainerManager is initialized for a non-standard container screen. */
    public static void ensureManagerInitialized(GuiContainer gui) {
        if (Mods.NotEnoughItems.isModLoaded()) {
            GuideScreenNeiNativeBridge.ensureManagerInitialized(gui);
        }
    }

    public interface EditorAccess {

        boolean isEditorActive();

        boolean isFullWidth();

        GuiContainer container();

        int containerLeft();

        int containerTop();

        int neiLayoutWidth();

        int neiLayoutLeft();

        int neiLayoutVersion();

        void beginNeiLayout();

        void endNeiLayout();

        @Nullable
        SceneEditorMultilineTextArea textArea();

        boolean canDropIntoEditor(int mouseX, int mouseY);

        boolean canInsertRichTagAtMouse(int mouseX, int mouseY);

        void insertAtMouse(String text, int mouseX, int mouseY);

        void insertAtSelection(String text);

        void returnToEditorScreen();

        default void prepareForTemporaryScreenChange() {}

        default void cancelTemporaryScreenChange() {}
    }
}
