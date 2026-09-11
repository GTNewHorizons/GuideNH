package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.Locale;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.syntax.AttributeSyntax;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;

/**
 * An attribute name suggestion. Accepting it also writes the value placeholder that matches the
 * attribute's {@link SyntaxValueKind}, so the caret lands ready to type or accept the value.
 */
public class AttributeNameCandidate implements AutocompleteCandidate {

    private static final int LABEL_COLOR = ColorUtils.TEXT.getColor();
    private static final int TYPE_COLOR = ColorUtils.TEXT_DISABLED.getColor();
    private static final String LABEL_GAP = "  ";

    private final AttributeSyntax attribute;
    private final String template;
    private final int caretOffset;
    private final int selectionEnd;

    public AttributeNameCandidate(AttributeSyntax attribute) {
        this.attribute = attribute;
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
        // A kind that is written without quotes keeps an empty bare value, so a value typed or accepted
        // afterwards lands where the page expects it.
        return kind.isQuoted() ? name + "=\"\"" : name + "=";
    }

    @Override
    public String displayText() {
        return attribute.name();
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
        return fontRenderer.getStringWidth(attribute.name()) + fontRenderer.getStringWidth(LABEL_GAP)
            + fontRenderer.getStringWidth(typeLabel());
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        int labelWidth = fontRenderer.drawString(attribute.name(), x, y + 2, LABEL_COLOR);
        fontRenderer
            .drawString(typeLabel(), x + labelWidth + fontRenderer.getStringWidth(LABEL_GAP), y + 2, TYPE_COLOR);
    }

    private String typeLabel() {
        return attribute.kind()
            .id()
            .toLowerCase(Locale.ROOT);
    }
}
