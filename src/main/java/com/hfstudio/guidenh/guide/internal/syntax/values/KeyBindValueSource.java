package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/** Suggests key binding descriptions for key bind attributes. */
public class KeyBindValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.KEY_BIND);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String partial = request.partialText();
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (KeyBinding keyBinding : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if (results.size() >= limit) {
                break;
            }
            String description = keyBinding.getKeyDescription();
            if (lower.isEmpty() || description.toLowerCase(Locale.ROOT)
                .contains(lower)) {
                results.add(SyntaxSuggestion.of(description));
            }
        }
        return results;
    }
}
