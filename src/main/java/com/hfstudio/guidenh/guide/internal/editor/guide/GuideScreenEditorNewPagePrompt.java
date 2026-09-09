package com.hfstudio.guidenh.guide.internal.editor.guide;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.internal.GuidebookText;

public class GuideScreenEditorNewPagePrompt extends GuiScreen {

    public interface Callback {

        void create(String path);

        void cancel();
    }

    private static final int PROMPT_WIDTH = 320;
    private static final int PROMPT_HEIGHT = 126;
    private static final int BUTTON_WIDTH = 82;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 10;
    private static final int FIELD_HEIGHT = 18;

    private final GuiScreen parent;
    private final String initialPath;
    private final Callback callback;
    private GuiTextField pathField;

    public GuideScreenEditorNewPagePrompt(GuiScreen parent, String initialPath, Callback callback) {
        this.parent = parent;
        this.initialPath = initialPath;
        this.callback = callback;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int baseX = (this.width - PROMPT_WIDTH) / 2;
        int baseY = (this.height - PROMPT_HEIGHT) / 2;
        int fieldX = baseX + 14;
        int fieldY = baseY + 58;
        int fieldW = PROMPT_WIDTH - 28;
        this.pathField = new GuiTextField(this.fontRendererObj, fieldX, fieldY, fieldW, FIELD_HEIGHT);
        this.pathField.setMaxStringLength(260);
        this.pathField.setText(initialPath != null ? initialPath : "NewGuide.md");
        this.pathField.setFocused(true);
        this.pathField.setCursorPositionEnd();

        int buttonY = baseY + PROMPT_HEIGHT - 28;
        int totalWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int buttonX = baseX + (PROMPT_WIDTH - totalWidth) / 2;
        this.buttonList.add(
            new GuiButton(
                0,
                buttonX,
                buttonY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                GuidebookText.GuideEditorNewPageCreate.text()));
        this.buttonList.add(
            new GuiButton(
                1,
                buttonX + BUTTON_WIDTH + BUTTON_GAP,
                buttonY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                GuidebookText.SceneEditorCancel.text()));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            callback.create(pathField.getText());
            return;
        }
        callback.cancel();
        mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            callback.cancel();
            mc.displayGuiScreen(parent);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            callback.create(pathField.getText());
            return;
        }
        pathField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        pathField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        pathField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int baseX = (this.width - PROMPT_WIDTH) / 2;
        int baseY = (this.height - PROMPT_HEIGHT) / 2;
        drawRect(baseX, baseY, baseX + PROMPT_WIDTH, baseY + PROMPT_HEIGHT, ColorUtils.DIALOG.getColor());
        drawRect(baseX, baseY, baseX + PROMPT_WIDTH, baseY + 1, ColorUtils.ARGB_FF4D5661.getColor());
        drawRect(
            baseX,
            baseY + PROMPT_HEIGHT - 1,
            baseX + PROMPT_WIDTH,
            baseY + PROMPT_HEIGHT,
            ColorUtils.ARGB_FF4D5661.getColor());
        drawCenteredString(
            fontRendererObj,
            GuidebookText.GuideEditorNewPagePromptTitle.text(),
            this.width / 2,
            baseY + 12,
            ColorUtils.TEXT.getColor());
        fontRendererObj.drawString(
            GuidebookText.GuideEditorNewPagePath.text(),
            baseX + 14,
            baseY + 42,
            ColorUtils.TEXT_MUTED.getColor());
        pathField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
