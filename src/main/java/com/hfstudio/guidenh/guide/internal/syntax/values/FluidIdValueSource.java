package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.List;
import java.util.Set;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

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
            .map(FluidIdValueSource::createSuggestion)
            .toList();
    }

    private static SyntaxSuggestion createSuggestion(String id) {
        if (id.endsWith(":")) return SyntaxSuggestion.of(id);
        Fluid fluid = FluidRegistry.getFluid(id);
        return fluid != null
            ? SyntaxSuggestion.withFluidIcon(id, new FluidStack(fluid, FluidContainerRegistry.BUCKET_VOLUME))
            : SyntaxSuggestion.of(id);
    }
}
