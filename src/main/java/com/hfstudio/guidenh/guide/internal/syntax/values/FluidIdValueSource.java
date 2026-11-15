package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.RegistryIdIndex;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

public class FluidIdValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.FLUID_ID);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        return RegistryIdIndex.fluids()
            .match(request.partialText(), limit)
            .stream()
            .map(SyntaxSuggestion::of)
            .toList();
    }
}
