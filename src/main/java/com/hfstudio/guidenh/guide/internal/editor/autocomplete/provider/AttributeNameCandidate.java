package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.Locale;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.syntax.AttributeSyntax;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;

public class AttributeNameCandidate implements AutocompleteCandidate {

    private static final int LABEL_COLOR = ColorUtils.TEXT.getColor();
    private static final int TYPE_COLOR = ColorUtils.TEXT_DISABLED.getColor();
    private static final String LABEL_GAP = "  ";

    private final String label;
    private final String type;
    private final String template;
    private final int caretOffset;
    private final int selectionEnd;

    public AttributeNameCandidate(AttributeSyntax attribute) {
        this.label = CompletionPreview.summarize(attribute.name());
        this.type = CompletionPreview.summarize(
            attribute.kind()
                .id()
                .toLowerCase(Locale.ROOT));
        this.template = buildTemplate(attribute.name(), attribute.kind());
        this.caretOffset = template.indexOf('"') >= 0 ? template.length() - 1
            : template.indexOf('}') >= 0 ? template.length() - 1 : template.length();
        this.selectionEnd = SyntaxValueKind.BOOLEAN.equals(attribute.kind()) && template.endsWith("}")
            ? template.length() - 1
            : caretOffset;
    }

    private static String buildTemplate(String name, SyntaxValueKind kind) {
        if (SyntaxValueKind.BOOLEAN.equals(kind)) {
            return name + "={true}";
        }
        if (SyntaxValueKind.SNBT.equals(kind) || SyntaxValueKind.VECTOR3.equals(kind)
            || SyntaxValueKind.EXPRESSION.equals(kind)) {
            return name + "={}";
        }
        return kind.isQuoted() ? name + "=\"\"" : name + "=";
    }

    @Override
    public String displayText() {
        return label;
    }

    @Override
    public String replacementText() {
        return template;
    }

    @Override
    public int caretOffsetInReplacement() {
        return caretOffset;
    }

    @Override
    public int selectionEndInReplacement() {
        return selectionEnd;
    }

    @Override
    public int renderWidth(FontRenderer fontRenderer) {
        return fontRenderer.getStringWidth(label) + fontRenderer.getStringWidth(LABEL_GAP)
            + fontRenderer.getStringWidth(type);
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        int labelWidth = fontRenderer.drawString(label, x, y + 2, LABEL_COLOR);
        fontRenderer.drawString(type, x + labelWidth + fontRenderer.getStringWidth(LABEL_GAP), y + 2, TYPE_COLOR);
    }
}
