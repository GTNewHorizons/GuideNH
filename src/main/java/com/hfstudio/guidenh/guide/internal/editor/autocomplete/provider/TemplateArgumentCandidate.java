package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateDefinition;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateRepository;
import com.hfstudio.guidenh.guide.syntax.AttributeSyntax;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;

/**
 * A candidate for an argument a template declares, offered inside a {@code <Template>} call whose target is
 * already named. The label names the template rather than a value type, because the set of valid arguments
 * comes from that template's own body rather than from the syntax registry.
 */
public class TemplateArgumentCandidate implements AutocompleteCandidate {

    private static final int LABEL_COLOR = ColorUtils.TEXT.getColor();
    private static final int TYPE_COLOR = ColorUtils.TEXT_DISABLED.getColor();
    private static final String LABEL_GAP = "  ";

    private final AttributeSyntax attribute;
    private final String templateName;
    private final String template;
    private final int caretOffset;

    public TemplateArgumentCandidate(AttributeSyntax attribute, String templateName) {
        this.attribute = attribute;
        this.templateName = templateName;
        this.template = attribute.name() + "=\"\"";
        this.caretOffset = this.template.length() - 1;
    }

    /**
     * The named arguments a template declares, read from the parameters the repository indexed. Positional
     * slots are not named, so they contribute nothing a name completion could offer.
     */
    public static List<AttributeSyntax> argumentsOf(String templateName) {
        MediaWikiTemplateDefinition definition = MediaWikiTemplateRepository.findByName(templateName);
        if (definition == null) {
            return List.of();
        }
        List<AttributeSyntax> arguments = new ArrayList<>();
        for (String named : definition.parameters()
            .named()) {
            arguments.add(AttributeSyntax.of(named, SyntaxValueKind.STRING));
        }
        return arguments;
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
        return caretOffset;
    }

    @Override
    public int renderWidth(FontRenderer fontRenderer) {
        return fontRenderer.getStringWidth(attribute.name()) + fontRenderer.getStringWidth(LABEL_GAP)
            + fontRenderer.getStringWidth(label());
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        int labelWidth = fontRenderer.drawString(attribute.name(), x, y + 2, LABEL_COLOR);
        fontRenderer.drawString(label(), x + labelWidth + fontRenderer.getStringWidth(LABEL_GAP), y + 2, TYPE_COLOR);
    }

    private String label() {
        return "arg \u00b7 " + templateName.toLowerCase(Locale.ROOT);
    }
}
