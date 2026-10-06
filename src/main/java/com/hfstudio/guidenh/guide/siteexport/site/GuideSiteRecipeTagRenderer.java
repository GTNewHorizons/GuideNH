package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.tags.RecipeCompiler;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeCache;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeLookup;
import com.hfstudio.guidenh.guide.siteexport.site.layout.SiteRecipeLayoutContext;
import com.hfstudio.guidenh.guide.siteexport.site.layout.SiteRecipeLayoutStrategyRegistry;
import com.hfstudio.guidenh.guide.siteexport.site.layout.SiteRecipeRawHandlerAccess;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.RecipeSlot;
import com.hfstudio.guidenh.integration.nei.NeiRecipeLookup;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class GuideSiteRecipeTagRenderer implements GuideSiteHtmlCompiler.RecipeTagRenderer {

    /**
     * Reads the ingredient or other slots of a recipe the way the in-game renderer does: through the registered slot.
     */
    private static List<NeiRecipeLookup.Slot> siteRecipeSlots(Object handler, int recipeIndex, boolean ingredients) {
        List<RecipeSlot> slots = ingredients ? GuideNhIntegrationRegistry.global()
            .readRecipeIngredientSlots(handler, recipeIndex)
            : GuideNhIntegrationRegistry.global()
                .readRecipeOtherSlots(handler, recipeIndex);
        return toNeiSlots(slots);
    }

    private static NeiRecipeLookup.Slot siteRecipeResultSlot(Object handler, int recipeIndex) {
        RecipeSlot slot = GuideNhIntegrationRegistry.global()
            .readRecipeResultSlot(handler, recipeIndex);
        return slot != null ? new NeiRecipeLookup.Slot(slot.x(), slot.y(), slot.stacks(), slot.currentStack()) : null;
    }

    private static List<NeiRecipeLookup.Slot> toNeiSlots(List<RecipeSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        List<NeiRecipeLookup.Slot> converted = new ArrayList<>(slots.size());
        for (RecipeSlot slot : slots) {
            converted.add(new NeiRecipeLookup.Slot(slot.x(), slot.y(), slot.stacks(), slot.currentStack()));
        }
        return converted;
    }

    public interface TargetStackResolver {

        @Nullable
        ItemStack resolve(String recipeId, String defaultNamespace);
    }

    public interface VanillaRecipeFinder {

        List<RecipeLookup.Entry> findByOutput(ItemStack targetStack);
    }

    public interface NeiRecipeFinder {

        List<NeiRecipeLookup.Entry> findCraftingRecipes(ItemStack targetStack);

        List<NeiRecipeLookup.CraftingRecipeRef> findCraftingRecipeRefs(ItemStack targetStack);

        default List<NeiRecipeLookup.Entry> findUsages(ItemStack targetStack) {
            return List.of();
        }
    }

    public interface RawHandlerFinder {

        List<Object> findCraftingHandlers(ItemStack targetStack);

        List<Object> findUsageHandlers(ItemStack targetStack);
    }

    public interface HandlerRuntime extends RecipeCompiler.HandlerMetadataReader, RecipeCompiler.HandlerRecipeAccess {

        int recipeCount(Object handler);

        List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex);
    }

    private final GuideSiteRecipeExporter exporter;
    private final GuideSiteItemIconResolver itemIconResolver;
    private final TargetStackResolver targetStackResolver;
    private final VanillaRecipeFinder vanillaRecipeFinder;
    private final NeiRecipeFinder neiRecipeFinder;
    private final RawHandlerFinder rawHandlerFinder;
    private final HandlerRuntime handlerRuntime;
    private final SiteRecipeLayoutStrategyRegistry layoutRegistry;
    private final SiteRecipeRawHandlerAccess rawHandlerSlotAccess;
    private final @Nullable GuideSiteNeiPhase1BackgroundExporter neiPhase1BackgroundExporter;

    public GuideSiteRecipeTagRenderer() {
        this(GuideSiteItemIconResolver.NONE, null);
    }

    public GuideSiteRecipeTagRenderer(GuideSiteItemIconResolver itemIconResolver) {
        this(itemIconResolver, null);
    }

    public GuideSiteRecipeTagRenderer(GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteNeiPhase1BackgroundExporter neiPhase1BackgroundExporter) {
        this(new GuideSiteRecipeExporter(), itemIconResolver, IdUtils::resolveItemStack, targetStack -> {
            if (targetStack == null || targetStack.getItem() == null) {
                return List.of();
            }
            return RecipeLookup.findByOutput(targetStack.getItem());
        }, new NeiRecipeFinder() {

            @Override
            public List<NeiRecipeLookup.Entry> findCraftingRecipes(ItemStack targetStack) {
                if (targetStack == null) {
                    return List.of();
                }
                return NeiRecipeLookup.findCraftingRecipes(targetStack);
            }

            @Override
            public List<NeiRecipeLookup.CraftingRecipeRef> findCraftingRecipeRefs(ItemStack targetStack) {
                if (targetStack == null) {
                    return List.of();
                }
                return NeiRecipeLookup.findCraftingRecipeRefs(targetStack);
            }

            @Override
            public List<NeiRecipeLookup.Entry> findUsages(ItemStack targetStack) {
                if (targetStack == null) {
                    return List.of();
                }
                return NeiRecipeLookup.findUsages(targetStack);
            }
        }, new RawHandlerFinder() {

            @Override
            public List<Object> findCraftingHandlers(ItemStack targetStack) {
                if (targetStack == null) {
                    return List.of();
                }
                return RecipeCache.getCraftingHandlers(targetStack);
            }

            @Override
            public List<Object> findUsageHandlers(ItemStack targetStack) {
                if (targetStack == null) {
                    return List.of();
                }
                return RecipeCache.getUsageHandlers(targetStack);
            }
        }, new HandlerRuntime() {

            @Override
            public @Nullable String handlerName(Object handler) {
                return NeiRecipeLookup.lookupHandlerName(handler);
            }

            @Override
            public @Nullable String handlerId(Object handler) {
                return NeiRecipeLookup.lookupHandlerId(handler);
            }

            @Override
            public @Nullable String overlayIdentifier(Object handler) {
                return NeiRecipeLookup.lookupOverlayIdentifier(handler);
            }

            @Override
            public int recipeCount(Object handler) {
                return NeiRecipeLookup.lookupNumRecipes(handler);
            }

            @Override
            public List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex) {
                return siteRecipeSlots(handler, recipeIndex, true);
            }

            @Override
            public @Nullable NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex) {
                return siteRecipeResultSlot(handler, recipeIndex);
            }

            @Override
            public List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex) {
                return siteRecipeSlots(handler, recipeIndex, false);
            }
        }, neiPhase1BackgroundExporter);
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, GuideSiteItemIconResolver itemIconResolver,
        TargetStackResolver targetStackResolver, VanillaRecipeFinder vanillaRecipeFinder,
        NeiRecipeFinder neiRecipeFinder) {
        this(
            exporter,
            itemIconResolver,
            targetStackResolver,
            vanillaRecipeFinder,
            neiRecipeFinder,
            new RawHandlerFinder() {

                @Override
                public List<Object> findCraftingHandlers(ItemStack targetStack) {
                    return List.of();
                }

                @Override
                public List<Object> findUsageHandlers(ItemStack targetStack) {
                    return List.of();
                }
            },
            new HandlerRuntime() {

                @Override
                public @Nullable String handlerName(Object handler) {
                    return null;
                }

                @Override
                public @Nullable String handlerId(Object handler) {
                    return null;
                }

                @Override
                public @Nullable String overlayIdentifier(Object handler) {
                    return null;
                }

                @Override
                public int recipeCount(Object handler) {
                    return 0;
                }

                @Override
                public List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex) {
                    return List.of();
                }

                @Override
                public @Nullable NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex) {
                    return null;
                }

                @Override
                public List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex) {
                    return List.of();
                }
            },
            null);
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, TargetStackResolver targetStackResolver,
        VanillaRecipeFinder vanillaRecipeFinder, NeiRecipeFinder neiRecipeFinder) {
        this(exporter, GuideSiteItemIconResolver.NONE, targetStackResolver, vanillaRecipeFinder, neiRecipeFinder);
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, TargetStackResolver targetStackResolver,
        VanillaRecipeFinder vanillaRecipeFinder, NeiRecipeFinder neiRecipeFinder, RawHandlerFinder rawHandlerFinder,
        HandlerRuntime handlerRuntime) {
        this(
            exporter,
            GuideSiteItemIconResolver.NONE,
            targetStackResolver,
            vanillaRecipeFinder,
            neiRecipeFinder,
            rawHandlerFinder,
            handlerRuntime,
            null);
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, GuideSiteItemIconResolver itemIconResolver,
        TargetStackResolver targetStackResolver, VanillaRecipeFinder vanillaRecipeFinder,
        NeiRecipeFinder neiRecipeFinder, RawHandlerFinder rawHandlerFinder, HandlerRuntime handlerRuntime) {
        this(
            exporter,
            itemIconResolver,
            targetStackResolver,
            vanillaRecipeFinder,
            neiRecipeFinder,
            rawHandlerFinder,
            handlerRuntime,
            null);
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, GuideSiteItemIconResolver itemIconResolver,
        TargetStackResolver targetStackResolver, VanillaRecipeFinder vanillaRecipeFinder,
        NeiRecipeFinder neiRecipeFinder, RawHandlerFinder rawHandlerFinder, HandlerRuntime handlerRuntime,
        @Nullable GuideSiteNeiPhase1BackgroundExporter neiPhase1BackgroundExporter) {
        this.exporter = exporter;
        this.itemIconResolver = itemIconResolver != null ? itemIconResolver : GuideSiteItemIconResolver.NONE;
        this.targetStackResolver = targetStackResolver;
        this.vanillaRecipeFinder = vanillaRecipeFinder;
        this.neiRecipeFinder = neiRecipeFinder;
        this.rawHandlerFinder = rawHandlerFinder;
        this.handlerRuntime = handlerRuntime;
        this.layoutRegistry = SiteRecipeLayoutStrategyRegistry.createDefault();
        this.rawHandlerSlotAccess = rawHandlerSlots(handlerRuntime);
        this.neiPhase1BackgroundExporter = neiPhase1BackgroundExporter;
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, GuideSiteItemIconResolver itemIconResolver,
        TargetStackResolver targetStackResolver, VanillaRecipeFinder vanillaRecipeFinder,
        NeiRecipeFinder neiRecipeFinder, RawHandlerFinder rawHandlerFinder) {
        this(
            exporter,
            itemIconResolver,
            targetStackResolver,
            vanillaRecipeFinder,
            neiRecipeFinder,
            rawHandlerFinder,
            new HandlerRuntime() {

                @Override
                public @Nullable String handlerName(Object handler) {
                    return null;
                }

                @Override
                public @Nullable String handlerId(Object handler) {
                    return null;
                }

                @Override
                public @Nullable String overlayIdentifier(Object handler) {
                    return null;
                }

                @Override
                public int recipeCount(Object handler) {
                    return 0;
                }

                @Override
                public List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex) {
                    return List.of();
                }

                @Override
                public @Nullable NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex) {
                    return null;
                }

                @Override
                public List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex) {
                    return List.of();
                }
            },
            null);
    }

    @Override
    public String render(MdxJsxElementFields element, String defaultNamespace) {
        String fallbackText = element.getAttributeString("fallbackText", "");
        String handlerOrderRaw = RecipeCompiler.trimToNull(element.getAttributeString("handlerOrder", null));
        Integer parsedHandlerOrder = parseInteger(handlerOrderRaw);
        if (handlerOrderRaw != null && parsedHandlerOrder == null) {
            return fallbackParagraph(fallbackText);
        }
        String recipeIndexRaw = RecipeCompiler.trimToNull(element.getAttributeString("recipeIndex", null));
        Integer parsedRecipeIndex = parseInteger(recipeIndexRaw);
        if (recipeIndexRaw != null && (parsedRecipeIndex == null || parsedRecipeIndex < 0)) {
            return fallbackParagraph(fallbackText);
        }

        String limitRaw = RecipeCompiler.trimToNull(element.getAttributeString("limit", null));
        Integer parsedLimit = parseInteger(limitRaw);
        if (limitRaw != null && parsedLimit == null) {
            return fallbackParagraph(fallbackText);
        }

        String tagName = element.name();
        boolean multi = tagName != null && tagName.startsWith("Recipes");
        boolean usageQuery = tagName != null && tagName.endsWith("Usage");
        int limit = multi ? Integer.MAX_VALUE : 1;
        if (parsedLimit != null && parsedLimit > 0) {
            limit = parsedLimit;
        }

        return renderInternal(
            new RenderRequest(
                element.name() != null ? element.name() : "Recipe",
                element.getAttributeString("id", ""),
                fallbackText,
                defaultNamespace,
                RecipeCompiler.trimToNull(element.getAttributeString("handlerName", null)),
                RecipeCompiler.trimToNull(element.getAttributeString("handlerId", null)),
                parsedHandlerOrder != null ? parsedHandlerOrder : -1,
                RecipeCompiler.parseNameList(element.getAttributeString("handlerWhitelist", null)),
                RecipeCompiler.parseNameList(element.getAttributeString("handlerBlacklist", null)),
                RecipeCompiler.parseFilterExpr(
                    RecipeCompiler.trimToNull(element.getAttributeString("input", null)),
                    defaultNamespace),
                RecipeCompiler.parseFilterExpr(
                    RecipeCompiler.trimToNull(element.getAttributeString("output", null)),
                    defaultNamespace),
                parsedRecipeIndex != null ? parsedRecipeIndex : -1,
                limit,
                multi,
                usageQuery));
    }

    @Override
    public String render(MdxJsxElementFields element, String defaultNamespace, GuideSiteTemplateRegistry templates) {
        exporter.setTooltipTemplates(templates);
        try {
            return render(element, defaultNamespace);
        } finally {
            exporter.setTooltipTemplates(null);
        }
    }

    @Override
    public String render(String recipeId, String fallbackText, String defaultNamespace) {
        return renderInternal(
            new RenderRequest(
                "Recipe",
                recipeId,
                fallbackText,
                defaultNamespace,
                null,
                null,
                -1,
                List.of(),
                List.of(),
                RecipeCompiler.parseFilterExpr(null, defaultNamespace),
                RecipeCompiler.parseFilterExpr(null, defaultNamespace),
                -1,
                1,
                false,
                false));
    }

    private String renderInternal(RenderRequest request) {
        String recipeId = RecipeCompiler.trimToNull(request.recipeId);
        if (recipeId == null) {
            return fallbackParagraph(request.fallbackText);
        }

        ItemStack targetStack;
        try {
            targetStack = targetStackResolver.resolve(recipeId, request.defaultNamespace);
        } catch (IllegalArgumentException e) {
            return fallbackParagraph(request.fallbackText);
        }
        if (targetStack == null || targetStack.getItem() == null) {
            return fallbackParagraph(request.fallbackText);
        }

        boolean hasRecipeFilter = !request.inputExpr.isEmpty() || !request.outputExpr.isEmpty();
        boolean hasHandlerFilter = request.handlerNameFilter != null || request.handlerIdFilter != null
            || request.handlerOrder >= 0
            || !request.handlerWhitelist.isEmpty();

        RawHandlerRenderResult rawHandlerResult = renderFromRawHandlers(request, targetStack, hasRecipeFilter);
        if (!rawHandlerResult.renderedRecipes.isEmpty()) {
            return exporter.renderRecipeCollection(rawHandlerResult.renderedRecipes, request.multi);
        }
        if (hasHandlerFilter) {
            return fallbackParagraph(request.fallbackText);
        }

        List<String> renderedRecipes = renderFromNeiEntries(request, targetStack, hasRecipeFilter);
        if (!renderedRecipes.isEmpty()) {
            return exporter.renderRecipeCollection(renderedRecipes, request.multi);
        }

        renderedRecipes = request.usageQuery ? List.of()
            : renderFromVanillaEntries(request, targetStack, hasRecipeFilter);
        if (!renderedRecipes.isEmpty()) {
            return exporter.renderRecipeCollection(renderedRecipes, request.multi);
        }

        return fallbackParagraph(request.fallbackText);
    }

    private RawHandlerRenderResult renderFromRawHandlers(RenderRequest request, ItemStack targetStack,
        boolean hasRecipeFilter) {
        List<Object> rawHandlers = request.usageQuery ? safeHandlers(rawHandlerFinder.findUsageHandlers(targetStack))
            : safeHandlers(rawHandlerFinder.findCraftingHandlers(targetStack));
        if (!request.usageQuery && (request.handlerIdFilter != null || request.handlerNameFilter != null
            || !request.handlerWhitelist.isEmpty())) {
            rawHandlers = mergeHandlers(rawHandlers, safeHandlers(rawHandlerFinder.findUsageHandlers(targetStack)));
        }

        List<Object> handlers = RecipeCompiler.filterHandlers(
            rawHandlers,
            request.handlerNameFilter,
            request.handlerIdFilter,
            request.handlerOrder,
            handlerRuntime,
            request.handlerWhitelist,
            RecipeCompiler.effectiveHandlerBlacklist(request.handlerBlacklist));
        if (handlers.isEmpty()) {
            return new RawHandlerRenderResult(List.of());
        }

        IdUtils.ParsedItemRef targetRef = parseTargetRef(request.recipeId, request.defaultNamespace);
        List<String> renderedRecipes = new ArrayList<>();
        Set<String> recipeFingerprints = new LinkedHashSet<>();
        for (int hi = 0; hi < handlers.size() && renderedRecipes.size() < request.limit; hi++) {
            Object handler = handlers.get(hi);
            int recipeCount = handlerRuntime.recipeCount(handler);
            int recipeStart = Math.max(request.recipeIndex, 0);
            int recipeEnd = request.recipeIndex >= 0 ? Math.min(recipeCount, request.recipeIndex + 1) : recipeCount;
            for (int recipeIndex = recipeStart; recipeIndex < recipeEnd
                && renderedRecipes.size() < request.limit; recipeIndex++) {
                NeiRecipeLookup.Entry entry = readRawHandlerEntry(handler, recipeIndex);
                if (!request.usageQuery && (targetRef == null || !RecipeCompiler.handlerTargetMatches(
                    entry,
                    targetRef,
                    request.handlerIdFilter != null || request.handlerNameFilter != null
                        || !request.handlerWhitelist.isEmpty()))) {
                    continue;
                }
                if (hasRecipeFilter && !RecipeCompiler.recipeMatches(entry, request.inputExpr, request.outputExpr)) {
                    continue;
                }
                if (!recipeFingerprints.add(NeiRecipeLookup.recipeFingerprint(handler, entry))) continue;

                String rendered = renderHandlerRecipe(handler, recipeIndex, targetStack, entry);
                if (!rendered.isEmpty()) {
                    renderedRecipes.add(rendered);
                }
            }
        }
        return new RawHandlerRenderResult(renderedRecipes);
    }

    private List<String> renderFromNeiEntries(RenderRequest request, ItemStack targetStack, boolean hasRecipeFilter) {
        if (request.usageQuery) {
            return renderFromUsageEntries(request, targetStack, hasRecipeFilter);
        }
        List<NeiRecipeLookup.CraftingRecipeRef> refs = neiRecipeFinder.findCraftingRecipeRefs(targetStack);
        if (refs.isEmpty()) {
            return List.of();
        }

        List<String> renderedRecipes = new ArrayList<>();
        Set<String> recipeFingerprints = new LinkedHashSet<>();
        for (int i = 0; i < refs.size() && renderedRecipes.size() < request.limit; i++) {
            NeiRecipeLookup.CraftingRecipeRef ref = refs.get(i);
            if (request.recipeIndex >= 0 && (ref == null || ref.recipeIndex != request.recipeIndex)) {
                continue;
            }
            NeiRecipeLookup.Entry entry = ref != null ? ref.entry : null;
            if (entry == null || !neiEntryHasAnySlots(entry)) {
                continue;
            }
            if (!matchesRequestedTarget(request, ref, entry)) {
                continue;
            }
            if (hasRecipeFilter && !RecipeCompiler.entryMatches(entry, request.inputExpr, request.outputExpr)) {
                continue;
            }
            if (!recipeFingerprints.add(NeiRecipeLookup.recipeFingerprint(ref.handler, entry))) continue;

            @Nullable
            GuideSiteNeiPhase1BackgroundExporter.Result phase1 = neiPhase1Capture(ref.handler, ref.recipeIndex);

            String rendered = layoutRegistry.render(
                SiteRecipeLayoutContext.neiEntry(
                    entry,
                    targetStack,
                    exporter,
                    itemIconResolver,
                    phase1 != null ? phase1.relativeUrl : null,
                    phase1 != null ? phase1.pixelWidth : null,
                    phase1 != null ? phase1.pixelHeight : null,
                    phase1 != null ? phase1.bodyYShiftPx : null));
            if (!rendered.isEmpty()) {
                renderedRecipes.add(rendered);
            }
        }
        return renderedRecipes;
    }

    private List<String> renderFromUsageEntries(RenderRequest request, ItemStack targetStack, boolean hasRecipeFilter) {
        List<NeiRecipeLookup.Entry> entries = neiRecipeFinder.findUsages(targetStack);
        if (entries.isEmpty()) {
            return List.of();
        }

        List<String> renderedRecipes = new ArrayList<>();
        Set<String> recipeFingerprints = new LinkedHashSet<>();
        int entryStart = Math.max(request.recipeIndex, 0);
        int entryEnd = request.recipeIndex >= 0 ? Math.min(entries.size(), request.recipeIndex + 1) : entries.size();
        for (int i = entryStart; i < entryEnd && renderedRecipes.size() < request.limit; i++) {
            NeiRecipeLookup.Entry entry = entries.get(i);
            if (entry == null || !neiEntryHasAnySlots(entry)) {
                continue;
            }
            if (hasRecipeFilter && !RecipeCompiler.entryMatches(entry, request.inputExpr, request.outputExpr)) {
                continue;
            }
            if (!recipeFingerprints.add(NeiRecipeLookup.recipeFingerprint(null, entry))) continue;

            String rendered = layoutRegistry.render(
                SiteRecipeLayoutContext
                    .neiEntry(entry, targetStack, exporter, itemIconResolver, null, null, null, null));
            if (!rendered.isEmpty()) {
                renderedRecipes.add(rendered);
            }
        }
        return renderedRecipes;
    }

    private List<String> renderFromVanillaEntries(RenderRequest request, ItemStack targetStack,
        boolean hasRecipeFilter) {
        List<RecipeLookup.Entry> vanillaEntries = vanillaRecipeFinder.findByOutput(targetStack);
        if (vanillaEntries.isEmpty()) {
            return List.of();
        }

        List<String> renderedRecipes = new ArrayList<>();
        Set<String> recipeFingerprints = new LinkedHashSet<>();
        int entryStart = Math.max(request.recipeIndex, 0);
        int entryEnd = request.recipeIndex >= 0 ? Math.min(vanillaEntries.size(), request.recipeIndex + 1)
            : vanillaEntries.size();
        for (int i = entryStart; i < entryEnd && renderedRecipes.size() < request.limit; i++) {
            RecipeLookup.Entry entry = vanillaEntries.get(i);
            if (entry == null || entry.result == null) {
                continue;
            }
            if (hasRecipeFilter && !RecipeCompiler.vanillaEntryMatches(entry, request.inputExpr, request.outputExpr)) {
                continue;
            }
            if (!recipeFingerprints.add(vanillaFingerprint(entry))) continue;

            String rendered = layoutRegistry
                .render(SiteRecipeLayoutContext.vanilla(entry, targetStack, exporter, itemIconResolver));
            if (!rendered.isEmpty()) {
                renderedRecipes.add(rendered);
            }
        }
        return renderedRecipes;
    }

    private static String vanillaFingerprint(RecipeLookup.Entry entry) {
        StringBuilder value = new StringBuilder();
        if (entry == null) return value.toString();
        if (entry.input3x3 != null) {
            for (ItemStack stack : entry.input3x3) appendStackFingerprint(value, stack);
        }
        appendStackFingerprint(value, entry.result);
        value.append(entry.shapeless);
        return value.toString();
    }

    private static void appendStackFingerprint(StringBuilder value, @Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            value.append("null;");
            return;
        }
        Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        value.append(
            registryName != null ? registryName
                : stack.getItem()
                    .getClass()
                    .getName())
            .append('#')
            .append(stack.getItemDamage())
            .append('x')
            .append(stack.stackSize);
        if (stack.stackTagCompound != null) value.append('@')
            .append(stack.stackTagCompound);
        value.append(';');
    }

    private NeiRecipeLookup.Entry readRawHandlerEntry(Object handler, int recipeIndex) {
        List<NeiRecipeLookup.Slot> ingredients = handlerRuntime.readIngredientSlots(handler, recipeIndex);
        List<NeiRecipeLookup.Slot> supporting = handlerRuntime.readOtherSlots(handler, recipeIndex);
        NeiRecipeLookup.Slot result = handlerRuntime.readResultSlot(handler, recipeIndex);
        String name = handlerRuntime.handlerName(handler);
        return new NeiRecipeLookup.Entry(
            name == null ? "" : name,
            name == null ? "" : name,
            ingredients,
            supporting,
            result);
    }

    private String renderHandlerRecipe(Object handler, int recipeIndex, ItemStack targetStack,
        NeiRecipeLookup.Entry entry) {
        List<NeiRecipeLookup.Slot> ingredientSlots = entry.ingredients;
        List<NeiRecipeLookup.Slot> supportingSlotData = entry.others;
        NeiRecipeLookup.Slot resultSlot = entry.result;
        List<List<GuideSiteExportedItem>> ingredients = exporter
            .ingredientItemsFromNeiSlots(ingredientSlots, itemIconResolver);
        List<List<GuideSiteExportedItem>> supportingSlots = exporter
            .supportingSlotItemsFromNeiSlots(supportingSlotData, itemIconResolver);
        GuideSiteExportedItem resultItem = exporter.resultItem(resultSlot, targetStack, itemIconResolver);
        if (ingredients.isEmpty() && supportingSlots.isEmpty() && resultItem.isEmpty()) {
            return "";
        }
        @Nullable
        GuideSiteNeiPhase1BackgroundExporter.Result phase1 = neiPhase1Capture(handler, recipeIndex);
        return layoutRegistry.render(
            SiteRecipeLayoutContext.rawHandlerWithEntry(
                handler,
                recipeIndex,
                targetStack,
                exporter,
                itemIconResolver,
                rawHandlerSlotAccess,
                entry,
                phase1 != null ? phase1.relativeUrl : null,
                phase1 != null ? phase1.pixelWidth : null,
                phase1 != null ? phase1.pixelHeight : null,
                phase1 != null ? phase1.bodyYShiftPx : null));
    }

    private @Nullable GuideSiteNeiPhase1BackgroundExporter.Result neiPhase1Capture(@Nullable Object handler,
        int recipeIndex) {
        if (neiPhase1BackgroundExporter == null || handler == null) {
            return null;
        }
        return neiPhase1BackgroundExporter.capture(handler, recipeIndex);
    }

    private static boolean neiEntryHasAnySlots(NeiRecipeLookup.Entry entry) {
        if (entry.ingredients != null && !entry.ingredients.isEmpty()) {
            return true;
        }
        if (entry.others != null && !entry.others.isEmpty()) {
            return true;
        }
        return entry.result != null;
    }

    private boolean matchesRequestedTarget(RenderRequest request, @Nullable NeiRecipeLookup.CraftingRecipeRef ref,
        NeiRecipeLookup.Entry entry) {
        if (ref == null || entry == null) {
            return false;
        }
        IdUtils.ParsedItemRef target = parseTargetRef(request.recipeId, request.defaultNamespace);
        if (target == null) {
            return false;
        }
        if (request.handlerIdFilter != null || request.handlerNameFilter != null
            || !request.handlerWhitelist.isEmpty()) {
            return RecipeCompiler.handlerTargetMatches(ref.handler, ref.recipeIndex, target, handlerRuntime, true);
        }
        return RecipeCompiler.resultSlotContains(entry.result, target);
    }

    private static @Nullable IdUtils.ParsedItemRef parseTargetRef(String recipeId, String defaultNamespace) {
        try {
            return IdUtils.parseItemRef(recipeId, defaultNamespace);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static SiteRecipeRawHandlerAccess rawHandlerSlots(final HandlerRuntime hr) {
        return new SiteRecipeRawHandlerAccess() {

            @Override
            public List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex) {
                return hr.readIngredientSlots(handler, recipeIndex);
            }

            @Override
            public List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex) {
                return hr.readOtherSlots(handler, recipeIndex);
            }

            @Override
            public @Nullable NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex) {
                return hr.readResultSlot(handler, recipeIndex);
            }
        };
    }

    private List<Object> mergeHandlers(List<Object> craftingHandlers, List<Object> usageHandlers) {
        if (craftingHandlers == null || craftingHandlers.isEmpty()) {
            return usageHandlers != null ? usageHandlers : List.of();
        }
        if (usageHandlers == null || usageHandlers.isEmpty()) {
            return craftingHandlers;
        }

        List<Object> merged = new ArrayList<>(craftingHandlers.size() + usageHandlers.size());
        merged.addAll(craftingHandlers);
        Set<String> seen = new LinkedHashSet<>();
        for (Object handler : craftingHandlers) {
            seen.add(NeiRecipeLookup.handlerFingerprint(handler));
        }
        for (Object handler : usageHandlers) {
            if (seen.add(NeiRecipeLookup.handlerFingerprint(handler))) {
                merged.add(handler);
            }
        }
        return merged;
    }

    private List<Object> safeHandlers(List<Object> handlers) {
        return handlers != null ? handlers : List.of();
    }

    @Nullable
    private Integer parseInteger(@Nullable String raw) {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String fallbackParagraph(String fallbackText) {
        if (fallbackText == null || fallbackText.isEmpty()) {
            return "";
        }
        return "<p>" + escapeHtml(fallbackText) + "</p>";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static class RenderRequest {

        private final String tagName;
        private final String recipeId;
        private final String fallbackText;
        private final String defaultNamespace;
        @Nullable
        private final String handlerNameFilter;
        @Nullable
        private final String handlerIdFilter;
        private final int handlerOrder;
        private final List<String> handlerWhitelist;
        private final List<String> handlerBlacklist;
        private final RecipeCompiler.FilterExpr inputExpr;
        private final RecipeCompiler.FilterExpr outputExpr;
        private final int recipeIndex;
        private final int limit;
        private final boolean multi;
        private final boolean usageQuery;

        private RenderRequest(String tagName, String recipeId, String fallbackText, String defaultNamespace,
            @Nullable String handlerNameFilter, @Nullable String handlerIdFilter, int handlerOrder,
            List<String> handlerWhitelist, List<String> handlerBlacklist, RecipeCompiler.FilterExpr inputExpr,
            RecipeCompiler.FilterExpr outputExpr, int recipeIndex, int limit, boolean multi, boolean usageQuery) {
            this.tagName = tagName;
            this.recipeId = recipeId;
            this.fallbackText = fallbackText;
            this.defaultNamespace = defaultNamespace;
            this.handlerNameFilter = handlerNameFilter;
            this.handlerIdFilter = handlerIdFilter;
            this.handlerOrder = handlerOrder;
            this.handlerWhitelist = handlerWhitelist;
            this.handlerBlacklist = handlerBlacklist;
            this.inputExpr = inputExpr;
            this.outputExpr = outputExpr;
            this.recipeIndex = recipeIndex;
            this.limit = Math.max(1, limit);
            this.multi = multi;
            this.usageQuery = usageQuery;
        }
    }

    private static class RawHandlerRenderResult {

        private final List<String> renderedRecipes;

        private RawHandlerRenderResult(List<String> renderedRecipes) {
            this.renderedRecipes = renderedRecipes;
        }
    }
}
