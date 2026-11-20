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

## Runtime action resolver registration

Custom `&[label](scheme:payload)` actions use `ActionLinkResolver.EXTENSION_POINT` in the existing per-guide `ExtensionCollection`. `DefaultExtensions` registers `sound`, `sound-src`, and `nei`. The compiler indexes schemes once, case-insensitively, rejects duplicate registrations and expands an action into an AST tag. Unknown schemes remain literal source; invalid recognized actions report an authoring error. The direct site exporter receives the owning guide extensions and shares its snapshot with tooltip compilers.

```java
import java.util.ArrayList;
import java.util.Set;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.ActionLinkResolver;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownActionLink;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

public class MyActionResolver implements ActionLinkResolver {
    @Override
    public Set<String> getSchemes() {
        return Set.of("inspect");
    }

    @Override
    public MdxJsxTextElement resolve(String label, String destination, String defaultNamespace) {
        MdxJsxTextElement element = new MdxJsxTextElement("MyActionLink", new ArrayList<>());
        element.addAttribute("target", destination.substring(destination.indexOf(':') + 1));
        element.addChild(MarkdownActionLink.plainLabel(label));
        return element;
    }
}
```

Register the resolver on the guide builder alongside the `MyActionLink` tag compiler:

```java
Guide.builder(id).extension(ActionLinkResolver.EXTENSION_POINT, new MyActionResolver()).build();
```

The resolver supplies syntax expansion; its tag compiler supplies layout and deferred behavior. Keep native game lookups out of the resolver. `SyntaxContributor` supplies attributes and insertion snippets. `SyntaxSlot` supplies editor completion, replacement and selection for custom syntax; registering a slot does not register runtime action behavior. These are separate extension points in the existing infrastructure, not independent global registries. Register addon site tag renderers when exported presentation is required. The Web Editor has a small scheme-to-tag resolver map for its supported actions and does not execute Java extensions.

## Content binding providers

Quests, research and future custom content share these interfaces. Once a provider is registered, authors can use `BindingLink`, `BindingCard`, `BindingIcon` and `bindings` without another tag family.

### Registration and responsibilities

| Interface | Registration | Responsibility |
| --- | --- | --- |
| `GuideBindingType` | `GuideNhIntegrationRegistry.global().registerBindingType(type)` | Declare the type with `id()` and normalize content IDs with `normalizeId(String)`, without game-registry access |
| `GuideBindingProvider` | `GuideNhIntegrationRegistry.global().registerBindingProvider(provider)` | Search the content catalogue, produce display snapshots and open native screens |
| `GuideBindingHoverProvider` | `GuideNhClientIntegrationRegistry.global().registerBindingHoverProvider(provider)` | Return `BindingHoverTarget(key, bounds)` for the supported screen and actual hit area |

The provider methods have separate responsibilities:

| Method | Purpose |
| --- | --- |
| `type()` | Identify its registered content type |
| `describe(BindingKey, BindingViewContext)` | Return the content's display snapshot |
| `search(String, int, int, BindingViewContext)` | Search by text, offset and limit; a zero limit returns an empty page |
| `open(BindingKey, GuideUiHost)` | Open the native screen with the originating guide context |
| `revision()` | Report the catalogue version for cache invalidation |

### Identity, snapshots and threading

`BindingKey(type, id)` is the complete identity. Type names are trimmed and lowercased; each type normalizes its IDs, and research IDs preserve case. `BindingDisplay` carries state, title, description, an optional item/texture icon, tooltip, prerequisite `BindingKey` entries and native-open availability. State IDs are extensible.

Return immutable snapshots and omit hidden details. Honor `BindingViewContext.staticView()` without reading the exporting player's progress. Game-registry queries and native UI operations run on the client thread; pure type normalization and page indexing do not access research registries.

`GuideBindingIndex` retains every page and anchor. Core navigation owns destination selection and key-hold state; hover providers only report the current hit target. The public `QuestIndex` API remains a facade over this shared index, while quest YAML uses `bindings`.

### Export and authoring

Both site export paths use static snapshots. The runtime bridge advertises `bindings`; semantic and preview queries use `filters.type` to select the content system and preserve original ID case. Preview payloads carry `type`, `state` and prerequisite `BindingKey` entries alongside the standard fields.

See [External content bindings](Guide-Page-Format#external-content-bindings) for single-line YAML and escaping, and [Research and custom content](Tags-Reference#research-and-custom-content) for tag behavior.

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

## Render embedded recipes

Implement `RecipeHandlerRenderProvider` and register it during client initialization with `GuideNhIntegrationRegistry.global().registerRecipeHandlerRenderProvider(provider)`. `canRenderRecipeHandler(Object)` identifies the handlers your provider supports.

The guide calls `renderRecipeHandlerBackground(Object, int, boolean)`, draws the recipe slots, then calls `renderRecipeHandlerForeground(Object, int, boolean)`. Draw backgrounds in the first stage and slot badges or other overlays in the second. The coordinates are local to the recipe body. Honor `skipForeground` when the handler's foreground depends on unavailable extra slots.

Existing providers implementing only `renderRecipeHandler(Object, int, boolean)` run once in the background stage. Override both stage methods to place overlays above the slots. The NEI provider follows NEI's native draw order and lets `drawForeground` invoke `drawExtras`; do not invoke extras a second time.

## Extend fluid tooltips

Implement `FluidTooltipProvider.appendTooltip(FluidStack, List<String>)` and register it during client initialization with `GuideNhClientIntegrationRegistry.global().registerFluidTooltipProvider(provider)`. Providers append material information after the fluid name; GuideNH adds quantity, temperature, state and optional advanced registry information. Each provider receives an independent stack and an empty list. A failing provider contributes no partial lines and does not prevent the remaining providers or default details from running. Keep lookups inexpensive and avoid storing mutable stacks. GregTech material tooltips use this API without requiring NEI.

## Extend debug information

Implement `DebugInfoExtractor` with `canHandle(LytNode)`, `extract(LytNode, HoveredElementInfo)` and an optional `getPriority()`. The first matching extractor handles a node; higher priorities run first.

```java
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractorRegistry;

DebugInfoExtractorRegistry.register(extractor);
```

Register your `extractor` during mod initialization. Keep extraction read-only and inexpensive because it runs while inspecting the page. See [Debug Mode](Debug-Mode) for the author-facing controls.
