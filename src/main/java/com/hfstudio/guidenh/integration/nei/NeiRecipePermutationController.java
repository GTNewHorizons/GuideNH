package com.hfstudio.guidenh.integration.nei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

/** Keeps the currently rendered NEI permutation in sync with the embedded recipe view. */
public class NeiRecipePermutationController {

    private static final Object LOCK = new Object();
    private static final WeakHashMap<Object, Map<Integer, RecipeState>> STATES = new WeakHashMap<>();

    private NeiRecipePermutationController() {}

    /**
     * Advances every embedded recipe once per client tick. NEI advances its own permutation state
     * from the widget tick counter, so this method must not be called from a render pass.
     */
    public static void tick() {
        synchronized (LOCK) {
            if (GuiScreen.isShiftKeyDown()) return;
            for (Map<Integer, RecipeState> handlerStates : STATES.values()) {
                for (RecipeState state : handlerStates.values()) {
                    state.renderRevision++;
                    state.clientTicks++;
                    int cycle = state.clientTicks / 20;
                    if (cycle == state.lastCycle) continue;
                    state.lastCycle = cycle;
                    state.candidateRevision++;
                    for (SlotState slot : state.slots.values()) {
                        if (slot.candidates.size() > 1) {
                            slot.index = (slot.index + 1) % slot.candidates.size();
                        }
                    }
                }
            }
        }
    }

    public static void advance(Object handler, int recipeIndex) {
        if (handler == null) return;
        synchronized (LOCK) {
            state(handler, recipeIndex);
        }
    }

    public static void apply(Object handler, int recipeIndex, String group, int ordinal, Object positionedStack) {
        if (handler == null || positionedStack == null) return;
        synchronized (LOCK) {
            applyLocked(handler, recipeIndex, group, ordinal, positionedStack, state(handler, recipeIndex));
        }
    }

    static void applyAll(Object handler, int recipeIndex, String group, List<Object> positionedStacks) {
        if (handler == null || positionedStacks == null || positionedStacks.isEmpty()) return;
        synchronized (LOCK) {
            RecipeState state = state(handler, recipeIndex);
            for (int ordinal = 0; ordinal < positionedStacks.size(); ordinal++) {
                Object positionedStack = positionedStacks.get(ordinal);
                if (positionedStack != null) {
                    applyLocked(handler, recipeIndex, group, ordinal, positionedStack, state);
                }
            }
        }
    }

    public static boolean scroll(Object handler, int recipeIndex, int localX, int localY, int wheelDelta) {
        if (handler == null || wheelDelta == 0 || !GuiScreen.isShiftKeyDown()) return false;
        synchronized (LOCK) {
            RecipeState state = state(handler, recipeIndex);
            List<Object> ingredients = safeList(NeiDirectCalls.ingredientStacks(handler, recipeIndex));
            List<Object> catalysts = safeList(NeiDirectCalls.otherStacks(handler, recipeIndex));
            ItemStack target = findAndAdvance(
                handler,
                recipeIndex,
                "ingredient",
                ingredients,
                state,
                localX,
                localY,
                wheelDelta);
            if (target == null) {
                target = findAndAdvance(handler, recipeIndex, "other", catalysts, state, localX, localY, wheelDelta);
            }
            if (target == null) return false;
            setMatchingSlots(handler, recipeIndex, "ingredient", ingredients, state, target);
            setMatchingSlots(handler, recipeIndex, "other", catalysts, state, target);
            state.renderRevision++;
            return true;
        }
    }

    private static ItemStack findAndAdvance(Object handler, int recipeIndex, String group, List<Object> stacks,
        RecipeState state, int localX, int localY, int wheelDelta) {
        for (int i = 0; i < stacks.size(); i++) {
            Object positionedStack = stacks.get(i);
            if (!NeiDirectCalls.contains(positionedStack, localX, localY)) continue;
            applyLocked(handler, recipeIndex, group, i, positionedStack, state);
            SlotState slot = state.slots
                .get(new SlotKey(group, i, NeiDirectCalls.relX(positionedStack), NeiDirectCalls.relY(positionedStack)));
            ItemStack target = chooseAdjacent(slot, wheelDelta);
            if (target != null) return target;
        }
        return null;
    }

    public static void clear() {
        synchronized (LOCK) {
            STATES.clear();
        }
    }

    public static long revision(Object handler, int recipeIndex) {
        if (handler == null) return -1L;
        synchronized (LOCK) {
            Map<Integer, RecipeState> handlerStates = STATES.get(handler);
            if (handlerStates == null) return -1L;
            RecipeState state = handlerStates.get(recipeIndex);
            return state == null ? -1L : state.renderRevision;
        }
    }

    private static void setMatchingSlots(Object handler, int recipeIndex, String group, List<Object> stacks,
        RecipeState state, ItemStack target) {
        for (int i = 0; i < stacks.size(); i++) {
            Object positionedStack = stacks.get(i);
            applyLocked(handler, recipeIndex, group, i, positionedStack, state);
            SlotKey key = new SlotKey(
                group,
                i,
                NeiDirectCalls.relX(positionedStack),
                NeiDirectCalls.relY(positionedStack));
            SlotState slot = state.slots.get(key);
            int index = findCandidate(slot == null ? List.of() : slot.candidates, target);
            if (index >= 0) {
                slot.index = index;
                setPermutationIfChanged(positionedStack, target);
            }
        }
    }

    private static ItemStack chooseAdjacent(SlotState slot, int wheelDelta) {
        if (slot == null || slot.candidates.size() <= 1) return null;
        slot.index = Math.floorMod(slot.index - Integer.signum(wheelDelta), slot.candidates.size());
        return slot.candidates.get(slot.index);
    }

    private static void applyLocked(Object handler, int recipeIndex, String group, int ordinal, Object positionedStack,
        RecipeState state) {
        SlotKey key = new SlotKey(
            group,
            ordinal,
            NeiDirectCalls.relX(positionedStack),
            NeiDirectCalls.relY(positionedStack));
        SlotState slot = state.slots.computeIfAbsent(key, ignored -> new SlotState());
        applySlot(state, slot, positionedStack);
    }

    private static void applySlot(RecipeState state, SlotState slot, Object positionedStack) {
        ItemStack[] sourceItems = safeItems(positionedStack);
        if (slot.candidateRevision != state.candidateRevision || !slot.matchesSource(positionedStack, sourceItems)) {
            List<ItemStack> candidates = resolveCandidates(positionedStack, sourceItems);
            if (candidates.isEmpty()) {
                ItemStack current = NeiDirectCalls.item(positionedStack);
                if (current != null) candidates = List.of(current.copy());
            }
            if (!sameCandidates(slot.candidates, candidates)) {
                ItemStack current = NeiDirectCalls.item(positionedStack);
                slot.candidates = candidates;
                slot.index = findCandidate(candidates, current);
            }
            slot.sourcePositionedStack = positionedStack;
            slot.sourceItems = sourceItems;
            slot.candidateRevision = state.candidateRevision;
        }
        if (!slot.candidates.isEmpty()) {
            slot.index = Math.floorMod(slot.index, slot.candidates.size());
            setPermutationIfChanged(positionedStack, slot.candidates.get(slot.index));
        }
    }

    private static void setPermutationIfChanged(Object positionedStack, ItemStack target) {
        if (NeiDirectCalls.item(positionedStack) != target) {
            NeiDirectCalls.setPermutationToRender(positionedStack, target);
        }
    }

    private static RecipeState state(Object handler, int recipeIndex) {
        return STATES.computeIfAbsent(handler, ignored -> new HashMap<>())
            .computeIfAbsent(recipeIndex, ignored -> new RecipeState());
    }

    private static List<Object> safeList(List<Object> values) {
        return values == null ? List.of() : values;
    }

    private static List<ItemStack> resolveCandidates(Object positionedStack, ItemStack[] rawItems) {
        List<ItemStack> candidates;
        try {
            candidates = NeiRecipeLookup.filteredPermutations(positionedStack);
        } catch (Throwable ignored) {
            candidates = List.of();
        }
        if (!candidates.isEmpty()) return candidates;
        try {
            if (rawItems == null || rawItems.length == 0) return List.of();
            List<ItemStack> rawCandidates = new ArrayList<>(rawItems.length);
            for (ItemStack rawItem : rawItems) {
                if (rawItem != null) rawCandidates.add(rawItem.copy());
            }
            return rawCandidates.isEmpty() ? List.of() : List.copyOf(rawCandidates);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static ItemStack[] safeItems(Object positionedStack) {
        try {
            return NeiDirectCalls.items(positionedStack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean sameCandidates(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            if (!NeiDirectCalls.sameTypeWithNbt(first.get(i), second.get(i))) return false;
        }
        return true;
    }

    private static int findCandidate(List<ItemStack> candidates, ItemStack current) {
        if (current == null) return 0;
        for (int i = 0; i < candidates.size(); i++) {
            if (NeiDirectCalls.sameTypeWithNbt(candidates.get(i), current)) return i;
        }
        return 0;
    }

    private static class RecipeState {

        private int clientTicks;
        private int lastCycle;
        private long candidateRevision;
        private long renderRevision;
        private final Map<SlotKey, SlotState> slots = new HashMap<>();
    }

    private static class SlotState {

        private List<ItemStack> candidates = List.of();
        private int index;
        private long candidateRevision = -1;
        private Object sourcePositionedStack;
        private ItemStack[] sourceItems;

        private boolean matchesSource(Object positionedStack, ItemStack[] currentItems) {
            if (currentItems == sourceItems && currentItems != null) return true;
            if (currentItems != null && sourceItems != null && currentItems.length == sourceItems.length) {
                for (int i = 0; i < currentItems.length; i++) {
                    if (currentItems[i] != sourceItems[i]) return false;
                }
                return true;
            }
            return currentItems == null && sourceItems == null && sourcePositionedStack == positionedStack;
        }
    }

    private record SlotKey(String group, int ordinal, int x, int y) {}
}
