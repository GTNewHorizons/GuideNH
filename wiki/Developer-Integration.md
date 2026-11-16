[中文](Developer-Integration-zh-CN)

# Developer Integration

This page is for mod developers extending GuideNH. Guide authors can use the built-in syntax without Java registration; see [Tags Reference](Tags-Reference).

## Register a guide

```java
import net.minecraft.util.ResourceLocation;
import com.hfstudio.guidenh.guide.Guide;

Guide.builder(new ResourceLocation("yourmod", "guidenh")).build();
```

Place pages in `assets/yourmod/guidenh/` with underscored language folders. See [Localization](Localization) for fallback rules.

## Export custom tags

Implement `GuideSiteTagRenderer` for tags supplied by your mod's compiler. Declare the supported names with `getTagNames()` and return HTML from `render(context, element)`; return `null` to leave a tag to another renderer.

Register globally or on a single guide:

```java
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteTagRenderer;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

GuideNhIntegrationRegistry.global().registerSiteTagRenderer(renderer);
Guide.builder(id).extension(GuideSiteTagRenderer.EXTENSION_POINT, renderer).build();
```

Here `renderer` is your implementation and `id` is the guide ID. Custom renderers run before built-ins; the first non-null result wins. Escape user-controlled text before returning HTML, for example with `GuideSiteGraphRenderer.esc(text)`. A failing renderer is logged and skipped.

The render context provides `defaultNamespace`, `currentPageId`, `templates`, `sceneResolver` and `compiler` for page-relative resources and shared export services.

## Extend fluid tooltips

Implement `FluidTooltipProvider.appendTooltip(FluidStack, List<String>)` and register it during client initialization with `GuideNhClientIntegrationRegistry.global().registerFluidTooltipProvider(provider)`. Providers append material information after the fluid name; GuideNH adds quantity, temperature, state and optional advanced registry information. Each provider receives an independent stack and an empty list. A failing provider contributes no partial lines and does not prevent the remaining providers or default details from running. Keep lookups inexpensive and avoid storing mutable stacks. GregTech material tooltips use this API without requiring NEI.

## Extend debug information

Implement `DebugInfoExtractor` with `canHandle(LytNode)`, `extract(LytNode, HoveredElementInfo)` and an optional `getPriority()`. The first matching extractor handles a node; higher priorities run first.

```java
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractorRegistry;

DebugInfoExtractorRegistry.register(extractor);
```

Register your `extractor` during mod initialization. Keep extraction read-only and inexpensive because it runs while inspecting the page. See [Debug Mode](Debug-Mode) for the author-facing controls.
