package com.hfstudio.guidenh.guide.color;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

/**
 * This extension point can be used to register custom symbolic colors in your guide.
 */
public interface SymbolicColorResolver extends Extension {

    ExtensionPoint<SymbolicColorResolver> EXTENSION_POINT = new ExtensionPoint<>(SymbolicColorResolver.class);

    /**
     * Attempt to resolve a custom symbolic color value.
     *
     * @param id The id of the color.
     * @return Null if the color is unknown to this resolver.
     */
    @Nullable
    ColorValue resolve(ResourceLocation id);

    /**
     * Helper to resolve a symbolic color from the pre-defined colors in {@link ColorUtils}, as well as
     * user-supplied symbolic color resolvers.
     *
     * @return null when the color cannot be resolved.
     */
    @Nullable
    static ColorValue resolve(PageCompiler compiler, String id) {
        ColorValue builtIn = ColorUtils.symbolic(id);
        if (builtIn != null) {
            return builtIn;
        }

        // See if it's an identifier
        ResourceLocation identifier;
        try {
            identifier = compiler.resolveId(id);
        } catch (Exception e) {
            return null; // Invalid identifier
        }
        if (identifier == null) {
            return null;
        }

        for (var resolver : compiler.getExtensions(EXTENSION_POINT)) {
            var color = resolveSafely(resolver, identifier);
            if (color != null) {
                return color;
            }
        }
        for (SymbolicColorResolver resolver : GuideNhIntegrationRegistry.global()
            .symbolicColorResolvers()) {
            var color = resolveSafely(resolver, identifier);
            if (color != null) {
                return color;
            }
        }

        return null;
    }

    @Nullable
    private static ColorValue resolveSafely(SymbolicColorResolver resolver, ResourceLocation id) {
        try {
            return resolver.resolve(id);
        } catch (RuntimeException e) {
            GuideDebugLog.error(
                "[GuideNH] [SymbolicColorResolver] {} failed to resolve {}: {}",
                resolver.getClass()
                    .getSimpleName(),
                id,
                e.toString());
            return null;
        }
    }
}
