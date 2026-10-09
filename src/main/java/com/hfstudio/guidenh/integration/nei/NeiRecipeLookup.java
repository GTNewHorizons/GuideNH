package com.hfstudio.guidenh.integration.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;

public class NeiRecipeLookup {

    private static final Map<Object, PermutationCache> PERMUTATION_CACHE = Collections
        .synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, String> HANDLER_FINGERPRINT_CACHE = Collections
        .synchronizedMap(new WeakHashMap<>());

    public static class Slot {

        public final int relx;
        public final int rely;
        public final List<ItemStack> stacks;
        public final @Nullable ItemStack current;

        public Slot(int relx, int rely, List<ItemStack> stacks) {
            this(relx, rely, stacks, null);
        }

        public Slot(int relx, int rely, List<ItemStack> stacks, @Nullable ItemStack current) {
            this.relx = relx;
            this.rely = rely;
            this.stacks = stacks;
            this.current = current;
        }
    }

    public static class Entry {

        public final String handlerName;
        public final String recipeName;
        public final List<Slot> ingredients;
        public final List<Slot> others;
        public final @Nullable Slot result;

        public Entry(String handlerName, String recipeName, List<Slot> ingredients, List<Slot> others,
            @Nullable Slot result) {
            this.handlerName = handlerName;
            this.recipeName = recipeName;
            this.ingredients = ingredients;
            this.others = others;
            this.result = result;
        }
    }

    /** NEI crafting recipe tied to {@code handler} index for Phase1/OpenGL snapshots; mirrors {@link #Entry}. */
    public static class CraftingRecipeRef {

        public final Object handler;
        public final int recipeIndex;
        public final Entry entry;

        CraftingRecipeRef(Object handler, int recipeIndex, Entry entry) {
            this.handler = handler;
            this.recipeIndex = recipeIndex;
            this.entry = entry;
        }
    }

    public static List<CraftingRecipeRef> findCraftingRecipeRefs(ItemStack target) {
        if (!Mods.NotEnoughItems.isModLoaded() || target == null) return List.of();
        try {
            List<Object> handlers = NeiDirectCalls.getCraftingHandlers(target);
            List<CraftingRecipeRef> out = new ArrayList<>();
            Set<String> fingerprints = new LinkedHashSet<>();
            for (Object handler : handlers) {
                if (handler == null) continue;
                CraftingRecipeRef[] refs = readHandlerCraftingRecipeRefs(handler);
                if (refs != null && refs.length > 0) {
                    for (CraftingRecipeRef ref : refs) {
                        if (ref != null && fingerprints.add(recipeFingerprint(ref.handler, ref.entry))) {
                            out.add(ref);
                        }
                    }
                }
            }
            return out;
        } catch (Throwable t) {
            GuideDebugLog.warn("[GuideNH] [NeiRecipeLookup] NEI crafting refs query failed", t);
            return List.of();
        }
    }

    public static List<Entry> findCraftingRecipes(ItemStack target) {
        List<CraftingRecipeRef> refs = findCraftingRecipeRefs(target);
        List<Entry> entries = new ArrayList<>(refs.size());
        for (CraftingRecipeRef r : refs) {
            entries.add(r.entry);
        }
        return entries;
    }

    public static List<Entry> findUsages(ItemStack target) {
        if (!Mods.NotEnoughItems.isModLoaded() || target == null) return List.of();
        try {
            return processHandlers(NeiDirectCalls.getUsageHandlers(target));
        } catch (Throwable t) {
            GuideDebugLog.warn("[GuideNH] [NeiRecipeLookup] NEI usage query failed", t);
            return List.of();
        }
    }

    /**
     * Returns the raw {@code IRecipeHandler} instances matching {@code target}. Caller must check
     * {@link #lookupNumRecipes(Object)} before iterating recipe indices.
     */
    public static List<Object> queryRawCraftingHandlers(ItemStack target) {
        if (!Mods.NotEnoughItems.isModLoaded() || target == null) return List.of();
        try {
            return NeiDirectCalls.getCraftingHandlers(target);
        } catch (Throwable t) {
            GuideDebugLog.warn("[GuideNH] [NeiRecipeLookup] queryRawCraftingHandlers failed", t);
            return List.of();
        }
    }

    /**
     * Returns the raw {@code IUsageHandler} instances matching {@code target}. These cover handlers
     * that consume {@code target} as an input (anvil / fuel / brewing ingredient).
     */
    public static List<Object> queryRawUsageHandlers(ItemStack target) {
        if (!Mods.NotEnoughItems.isModLoaded() || target == null) return List.of();
        try {
            return NeiDirectCalls.getUsageHandlers(target);
        } catch (Throwable t) {
            GuideDebugLog.warn("[GuideNH] [NeiRecipeLookup] queryRawUsageHandlers failed", t);
            return List.of();
        }
    }

    public static int lookupNumRecipes(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return 0;
        try {
            return NeiDirectCalls.numRecipes(handler);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String lookupHandlerName(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return "";
        try {
            return NeiDirectCalls.recipeName(handler);
        } catch (Throwable t) {
            return "";
        }
    }

    public static @Nullable String lookupOverlayIdentifier(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return null;
        try {
            return NeiDirectCalls.overlayId(handler);
        } catch (Throwable t) {
            return null;
        }
    }

    public static @Nullable String lookupHandlerId(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return null;
        try {
            return NeiDirectCalls.handlerId(handler);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void callOnUpdate(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return;
        try {
            NeiDirectCalls.onUpdate(handler);
        } catch (Throwable ignored) {}
    }

    public static void callDrawBackground(Object handler, int recipeIndex) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return;
        try {
            NeiDirectCalls.drawBackground(handler, recipeIndex);
        } catch (Throwable ignored) {}
    }

    public static void callDrawForeground(Object handler, int recipeIndex) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return;
        try {
            NeiDirectCalls.drawForeground(handler, recipeIndex);
        } catch (Throwable ignored) {}
    }

    public static void callDrawExtras(Object handler, int recipeIndex) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return;
        try {
            NeiDirectCalls.drawExtras(handler, recipeIndex);
        } catch (Throwable ignored) {}
    }

    /**
     * Append handler-specific tooltip lines for a hovered stack.
     */
    public static void appendItemTooltip(Object handler, ItemStack stack, List<String> tooltip, int recipeIndex) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null || stack == null || tooltip == null) return;
        try {
            NeiDirectCalls.handleItemTooltip(handler, stack, tooltip, recipeIndex);
        } catch (Throwable ignored) {}
    }

    /**
     * Returns a stable identity for a handler across NEI's crafting and usage query instances.
     * NEI can create separate handler objects for the same recipe pool, so object identity is not
     * sufficient when a recipe tag combines both query directions.
     */
    public static String handlerFingerprint(Object handler) {
        if (handler == null) return "";
        String cached = HANDLER_FINGERPRINT_CACHE.get(handler);
        if (cached != null) return cached;
        String className = handler.getClass()
            .getName();
        String overlayId = lookupOverlayIdentifier(handler);
        String handlerId = lookupHandlerId(handler);
        String handlerName = lookupHandlerName(handler);
        String fingerprint = className + '|'
            + nullToEmpty(overlayId)
            + '|'
            + nullToEmpty(handlerId)
            + '|'
            + nullToEmpty(handlerName);
        HANDLER_FINGERPRINT_CACHE.put(handler, fingerprint);
        return fingerprint;
    }

    /**
     * Returns a structural recipe identity based on the handler metadata, slot coordinates, and
     * every candidate stack. The recipe index is deliberately excluded because separate NEI
     * handler instances can expose the same entry at different indices.
     */
    public static String recipeFingerprint(Object handler, int recipeIndex) {
        return recipeFingerprint(
            handler,
            new Entry(
                "",
                "",
                readIngredientSlots(handler, recipeIndex),
                readOtherSlots(handler, recipeIndex),
                readResultSlot(handler, recipeIndex)));
    }

    public static String recipeFingerprint(Object handler, @Nullable Entry entry) {
        StringBuilder out = new StringBuilder(handlerFingerprint(handler));
        if (entry == null) return out.toString();
        appendSlots(out, entry.ingredients);
        appendSlots(out, entry.others);
        appendSlot(out, entry.result);
        return out.toString();
    }

    private static void appendSlots(StringBuilder out, List<Slot> slots) {
        out.append("[");
        if (slots != null) {
            for (Slot slot : slots) appendSlot(out, slot);
        }
        out.append("]");
    }

    private static void appendSlot(StringBuilder out, @Nullable Slot slot) {
        if (slot == null) {
            out.append("null;");
            return;
        }
        out.append(slot.relx)
            .append(',')
            .append(slot.rely)
            .append('{');
        if (slot.stacks != null) {
            for (ItemStack stack : slot.stacks) appendStack(out, stack);
        }
        out.append("};");
    }

    private static void appendStack(StringBuilder out, @Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            out.append("null,");
            return;
        }
        Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        out.append(
            registryName != null ? registryName
                : stack.getItem()
                    .getClass()
                    .getName())
            .append('#')
            .append(stack.getItemDamage())
            .append('x')
            .append(stack.stackSize);
        if (stack.stackTagCompound != null) out.append('@')
            .append(stack.stackTagCompound);
        out.append(',');
    }

    private static String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    public static class PositionedStackHit {

        public final Object positionedStack;
        public final boolean ingredient;

        public PositionedStackHit(Object positionedStack, boolean ingredient) {
            this.positionedStack = positionedStack;
            this.ingredient = ingredient;
        }
    }

    @Nullable
    public static List<String> positionedTooltip(@Nullable Object positionedStack) {
        if (positionedStack == null) return null;
        try {
            return NeiDirectCalls.positionedTooltip(positionedStack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static List<String> positionedTooltip(Object handler, Object positionedStack, boolean input) {
        if (handler == null || positionedStack == null) return null;
        try {
            return NeiDirectCalls.positionedTooltip(handler, positionedStack, input);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static String acceptsLabel(@Nullable Object positionedStack) {
        if (positionedStack == null) return null;
        try {
            return NeiDirectCalls.acceptsLabel(positionedStack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean showCycledIngredientsTooltip() {
        try {
            return NeiDirectCalls.showCycledIngredientsTooltip();
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static List<ItemStack> filteredPermutations(@Nullable Object positionedStack) {
        if (positionedStack == null) return List.of();
        ItemStack[] sourceItems;
        try {
            sourceItems = NeiDirectCalls.items(positionedStack);
        } catch (Throwable ignored) {
            sourceItems = null;
        }
        try {
            List<ItemStack> filtered = NeiDirectCalls.filteredPermutations(positionedStack);
            PermutationCache cached = PERMUTATION_CACHE.get(positionedStack);
            if (cached != null && cached.matches(sourceItems, filtered)) return cached.values;
            List<ItemStack> result = new ArrayList<>();
            addUniquePermutationCopies(result, filtered);
            if (sourceItems != null) {
                for (ItemStack source : sourceItems) {
                    addUniquePermutationCopy(result, source);
                }
            }
            result = result.isEmpty() ? List.of() : List.copyOf(result);
            PERMUTATION_CACHE.put(positionedStack, new PermutationCache(sourceItems, result, filtered));
            return result;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static void addUniquePermutationCopies(List<ItemStack> target, List<ItemStack> values) {
        if (values == null) return;
        for (ItemStack value : values) addUniquePermutationCopy(target, value);
    }

    private static void addUniquePermutationCopy(List<ItemStack> target, @Nullable ItemStack value) {
        if (value == null) return;
        for (ItemStack existing : target) {
            if (NeiDirectCalls.sameTypeWithNbt(existing, value)) return;
        }
        target.add(value.copy());
    }

    private static class PermutationCache {

        private final ItemStack[] sourceItems;
        private final List<ItemStack> values;
        private final List<ItemStack> filtered;

        private PermutationCache(ItemStack[] sourceItems, List<ItemStack> values, List<ItemStack> filtered) {
            this.sourceItems = sourceItems;
            this.values = values;
            this.filtered = filtered;
        }

        private boolean matches(ItemStack[] currentItems, List<ItemStack> currentFiltered) {
            if (sourceItems != currentItems && !sameItemArray(sourceItems, currentItems)) return false;
            return sameItemList(filtered, currentFiltered);
        }

        private static boolean sameItemArray(ItemStack[] first, ItemStack[] second) {
            if (first == second) return true;
            if (first == null || second == null || first.length != second.length) return false;
            for (int index = 0; index < first.length; index++) {
                if (first[index] != second[index]) return false;
            }
            return true;
        }

        private static boolean sameItemList(List<ItemStack> first, List<ItemStack> second) {
            if (first == second) return true;
            if (first == null || second == null || first.size() != second.size()) return false;
            for (int index = 0; index < first.size(); index++) {
                ItemStack left = first.get(index);
                ItemStack right = second.get(index);
                if (left == right) continue;
                if (left == null || right == null || !NeiDirectCalls.sameTypeWithNbt(left, right)) return false;
            }
            return true;
        }
    }

    @Nullable
    public static PositionedStackHit findPositionedStackHit(Object handler, int recipeIndex, int localX, int localY) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return null;
        try {
            Object positionedStack = findPositionedStack(
                safeList(NeiDirectCalls.ingredientStacks(handler, recipeIndex)),
                localX,
                localY);
            if (positionedStack != null) return new PositionedStackHit(positionedStack, true);
            positionedStack = findPositionedStack(
                safeList(NeiDirectCalls.otherStacks(handler, recipeIndex)),
                localX,
                localY);
            if (positionedStack != null) return new PositionedStackHit(positionedStack, false);
            Object result = NeiDirectCalls.resultStack(handler, recipeIndex);
            return result != null && NeiDirectCalls.contains(result, localX, localY)
                ? new PositionedStackHit(result, false)
                : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Object findPositionedStack(Object handler, int recipeIndex, int localX, int localY) {
        PositionedStackHit hit = findPositionedStackHit(handler, recipeIndex, localX, localY);
        return hit == null ? null : hit.positionedStack;
    }

    @Nullable
    private static Object findPositionedStack(List<Object> stacks, int localX, int localY) {
        for (Object positionedStack : stacks) {
            if (positionedStack != null && NeiDirectCalls.contains(positionedStack, localX, localY)) {
                return positionedStack;
            }
        }
        return null;
    }

    private static List<Object> safeList(List<Object> values) {
        return values == null ? List.of() : values;
    }

    public static int lookupRecipeHeight(Object handler, int recipeIndex) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return 0;
        try {
            return NeiDirectCalls.recipeHeight(handler, recipeIndex);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static List<Slot> readIngredientSlots(Object handler, int recipeIndex) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return List.of();
        try {
            return readSlotList(
                handler,
                recipeIndex,
                "ingredient",
                NeiDirectCalls.ingredientStacks(handler, recipeIndex));
        } catch (Throwable t) {
            return List.of();
        }
    }

    public static List<Slot> readOtherSlots(Object handler, int recipeIndex) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return List.of();
        try {
            return readSlotList(handler, recipeIndex, "other", NeiDirectCalls.otherStacks(handler, recipeIndex));
        } catch (Throwable t) {
            return List.of();
        }
    }

    /**
     * Returns {@code true} if invoking {@code getOtherStacks} on this handler throws an exception.
     * Used to skip {@code drawForeground}/{@code drawExtras} for broken handlers, keeping the log
     * clean.
     */
    public static boolean otherStacksThrows(Object handler, int recipeIndex) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return false;
        try {
            return NeiDirectCalls.otherStacksThrows(handler, recipeIndex);
        } catch (Throwable t) {
            return false;
        }
    }

    public static @Nullable Slot readResultSlot(Object handler, int recipeIndex) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return null;
        try {
            return readSlot(handler, recipeIndex, "result", 0, NeiDirectCalls.resultStack(handler, recipeIndex));
        } catch (Throwable t) {
            return null;
        }
    }

    /** Returns the {@code HandlerInfo} display stack for a handler's recipe tab icon. */
    public static @Nullable ItemStack lookupHandlerIcon(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return null;
        try {
            return NeiDirectCalls.handlerIconStack(handler);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Returns the raw {@code DrawableResource} for this handler's tab image as an opaque
     * {@code Object}, or {@code null} when absent. Pass the result to {@link #drawableWidth},
     * {@link #drawableHeight}, and {@link #drawHandlerImage}.
     */
    public static @Nullable Object lookupHandlerImage(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return null;
        try {
            return NeiDirectCalls.handlerImage(handler);
        } catch (Throwable t) {
            return null;
        }
    }

    public static int lookupHandlerWidth(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return 166;
        try {
            return NeiDirectCalls.handlerWidth(handler);
        } catch (Throwable t) {
            return 166;
        }
    }

    public static int lookupHandlerHeight(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return 65;
        try {
            return NeiDirectCalls.handlerHeight(handler);
        } catch (Throwable t) {
            return 65;
        }
    }

    public static int lookupHandlerYShift(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return 0;
        try {
            return NeiDirectCalls.handlerYShift(handler);
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Native pixel width of a {@code DrawableResource} (includes padding). */
    public static int drawableWidth(Object drawable) {
        if (drawable == null || !Mods.NotEnoughItems.isModLoaded()) return 0;
        try {
            return NeiDirectCalls.drawableWidth(drawable);
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Native pixel height of a {@code DrawableResource} (includes padding). */
    public static int drawableHeight(Object drawable) {
        if (drawable == null || !Mods.NotEnoughItems.isModLoaded()) return 0;
        try {
            return NeiDirectCalls.drawableHeight(drawable);
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * Invoke {@code DrawableResource.draw(x, y)} at native pixel size. Callers that need scaling
     * should wrap the call in {@code glPushMatrix / glScalef / glPopMatrix}.
     */
    public static void drawHandlerImage(Object drawable, int x, int y) {
        if (drawable == null || !Mods.NotEnoughItems.isModLoaded()) return;
        try {
            NeiDirectCalls.drawDrawable(drawable, x, y);
        } catch (Throwable ignored) {}
    }

    public static @Nullable CraftingRecipeRef[] readHandlerCraftingRecipeRefs(Object handler) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return null;
        try {
            int n = NeiDirectCalls.numRecipes(handler);
            if (n <= 0) return new CraftingRecipeRef[0];
            String recipeName = NeiDirectCalls.recipeName(handler);
            String handlerName = handler.getClass()
                .getSimpleName();
            CraftingRecipeRef[] out = new CraftingRecipeRef[n];
            for (int i = 0; i < n; i++) {
                List<Slot> ing = readSlotList(handler, i, "ingredient", NeiDirectCalls.ingredientStacks(handler, i));
                List<Slot> oth = readSlotList(handler, i, "other", NeiDirectCalls.otherStacks(handler, i));
                Slot res = readSlot(handler, i, "result", 0, NeiDirectCalls.resultStack(handler, i));
                out[i] = new CraftingRecipeRef(handler, i, new Entry(handlerName, recipeName, ing, oth, res));
            }
            return out;
        } catch (Throwable t) {
            GuideDebugLog.warn("[GuideNH] [NeiRecipeLookup] NEI handler {} read failed", handler.getClass(), t);
            return null;
        }
    }

    public static @Nullable Entry[] readHandler(Object handler) {
        CraftingRecipeRef[] refs = readHandlerCraftingRecipeRefs(handler);
        if (refs == null) return null;
        if (refs.length == 0) return new Entry[0];
        Entry[] out = new Entry[refs.length];
        for (int i = 0; i < refs.length; i++) {
            out[i] = refs[i].entry;
        }
        return out;
    }

    public static List<Slot> readSlotList(Object obj) {
        if (!(obj instanceof List)) return List.of();
        List<?> values = (List<?>) obj;
        List<Slot> out = new ArrayList<>(values.size());
        for (Object ps : values) {
            Slot s = readSlot(ps);
            if (s != null) out.add(s);
        }
        return out;
    }

    public static @Nullable Slot readSlot(@Nullable Object ps) {
        return readSlot(null, 0, "", 0, ps);
    }

    public static void advancePermutations(Object handler, int recipeIndex) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return;
        try {
            NeiRecipePermutationController.advance(handler, recipeIndex);
        } catch (Throwable ignored) {}
    }

    public static boolean scrollPermutation(Object handler, int recipeIndex, int localX, int localY, int wheelDelta) {
        if (!Mods.NotEnoughItems.isModLoaded() || handler == null) return false;
        try {
            return NeiRecipePermutationController.scroll(handler, recipeIndex, localX, localY, wheelDelta);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<Slot> readSlotList(Object handler, int recipeIndex, String group, List<Object> values) {
        if (values == null || values.isEmpty()) return List.of();
        if (handler != null) {
            NeiRecipePermutationController.applyAll(handler, recipeIndex, group, values);
        }
        List<Slot> out = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            Slot slot = readSlot(null, recipeIndex, group, index, values.get(index));
            if (slot != null) out.add(slot);
        }
        return out;
    }

    private static @Nullable Slot readSlot(@Nullable Object handler, int recipeIndex, String group, int ordinal,
        @Nullable Object ps) {
        if (ps == null) return null;
        try {
            if (handler != null) {
                NeiRecipePermutationController.apply(handler, recipeIndex, group, ordinal, ps);
            }
            int relx = NeiDirectCalls.relX(ps);
            int rely = NeiDirectCalls.relY(ps);
            ItemStack[] itemsArr = NeiDirectCalls.items(ps);
            List<ItemStack> stacks = itemsArr == null ? new ArrayList<>(1) : new ArrayList<>(itemsArr.length);
            if (itemsArr != null) {
                for (ItemStack s : itemsArr) {
                    if (s != null) stacks.add(s);
                }
            }
            if (stacks.isEmpty()) {
                ItemStack single = NeiDirectCalls.item(ps);
                if (single != null) stacks.add(single);
            }
            if (stacks.isEmpty()) return null;
            return new Slot(relx, rely, stacks, NeiDirectCalls.item(ps));
        } catch (Throwable t) {
            return null;
        }
    }

    private static List<Entry> processHandlers(List<Object> handlers) {
        List<Entry> out = new ArrayList<>();
        for (Object handler : handlers) {
            if (handler == null) continue;
            Entry[] entries = readHandler(handler);
            if (entries != null) out.addAll(List.of(entries));
        }
        return out;
    }
}
