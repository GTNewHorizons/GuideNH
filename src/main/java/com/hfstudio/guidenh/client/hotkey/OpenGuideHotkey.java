package com.hfstudio.guidenh.client.hotkey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.binding.BindingHoverSession;
import com.hfstudio.guidenh.guide.internal.binding.BindingNavigationService;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackResolver;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.GuideHoverSession;
import com.hfstudio.guidenh.guide.internal.input.GuideKeyBindingSupport;
import com.hfstudio.guidenh.guide.internal.search.GuideTargetSelection;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.api.client.GuideNhClientIntegrationRegistry;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class OpenGuideHotkey {

    private static final ThreadLocal<Boolean> TOOLTIP_HINTS_SUPPRESSED = ThreadLocal.withInitial(() -> false);

    public static final int TICKS_TO_OPEN = 10;

    public static final KeyBinding OPEN_GUIDE_KEY = new KeyBinding(
        "key.guidenh.open_guide",
        Keyboard.KEY_G,
        "key.categories.guidenh");

    public static String previousItemId;
    public static final List<FoundPage> guidebookPages = new ArrayList<>();
    public static int ticksKeyHeld;
    public static boolean holding;
    public static boolean newTick = true;

    private static final BindingHoverSession BINDING_SESSION = new BindingHoverSession();
    private static BindingKey guideHoveredBinding;
    private static final GuideHoverSession HOVER_SESSION = new GuideHoverSession();
    private static GuideTargetSelection resourceSelection = new GuideTargetSelection("", List.of(), List.of());
    private static boolean hoverObserved;
    private static boolean activationBlocked;

    // Parallel state for the BetterQuesting quest-hover hotkey path. Kept independent of the
    // item-tooltip flow so that hovering a BQ quest in the BQ GUI does not interfere with item
    // hover detection in the inventory and vice versa.
    public static UUID previousQuestId;
    public static final List<FoundPage> questGuidebookPages = new ArrayList<>();
    public static int questTicksKeyHeld;

    private OpenGuideHotkey() {}

    public static class FoundPage {

        public final MutableGuide guide;
        public final PageAnchor page;

        public FoundPage(MutableGuide guide, PageAnchor page) {
            this.guide = guide;
            this.page = page;
        }
    }

    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN_GUIDE_KEY);
        OpenGuideHotkey instance = new OpenGuideHotkey();
        FMLCommonHandler.instance()
            .bus()
            .register(instance);
        MinecraftForge.EVENT_BUS.register(instance);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickResourceHover();
            newTick = true;
            tickQuestHover();
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        var mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || event.entityPlayer != mc.thePlayer) {
            return;
        }
        handleTooltip(event.itemStack, event.toolTip);
    }

    public static void handleTooltip(ItemStack itemStack, List<String> lines) {
        if (TOOLTIP_HINTS_SUPPRESSED.get()) {
            return;
        }
        if (GuideScreen.current() == null) {
            hoverObserved = true;
            if (newTick) {
                newTick = false;
                update(itemStack);
            }
        }

        if (guidebookPages.isEmpty()) {
            return;
        }

        var found = guidebookPages.getFirst();

        var current = GuideScreen.current();
        if (current != null && found.page.pageId()
            .equals(current.getCurrentPageId())) {
            return;
        }

        float progress = ticksKeyHeld / (float) TICKS_TO_OPEN;
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;

        String hint = renderHint(progress);
        if (lines.isEmpty()) {
            lines.add(hint);
        } else {
            lines.add(1, hint);
        }
    }

    public static boolean appendQuestTooltip(UUID questId, List<String> lines) {
        if (TOOLTIP_HINTS_SUPPRESSED.get() || questId == null || lines == null) {
            return false;
        }
        List<FoundPage> pages = findQuestPages(questId);
        if (pages.isEmpty()) {
            return false;
        }

        var found = pages.getFirst();
        var current = GuideScreen.current();
        if (current != null && found.page.pageId()
            .equals(current.getCurrentPageId())) {
            return false;
        }

        float progress = questTicksKeyHeld / (float) TICKS_TO_OPEN;
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;

        String hint = renderHint(progress);
        if (lines.isEmpty()) {
            lines.add(hint);
        } else if (!lines.contains(hint)) {
            lines.add(1, hint);
        }
        return true;
    }

    /**
     * Builds tooltip content without live hotkey hints or changes to the hovered-item state.
     */
    public static <T> T withoutTooltipHints(Supplier<T> tooltipBuilder) {
        boolean previous = TOOLTIP_HINTS_SUPPRESSED.get();
        TOOLTIP_HINTS_SUPPRESSED.set(true);
        try {
            return tooltipBuilder.get();
        } finally {
            if (previous) {
                TOOLTIP_HINTS_SUPPRESSED.set(true);
            } else {
                TOOLTIP_HINTS_SUPPRESSED.remove();
            }
        }
    }

    public static String renderHint(float progress) {
        var fr = Minecraft.getMinecraft().fontRenderer;
        String keyName = GuideKeyBindingSupport.describe(OPEN_GUIDE_KEY);
        String holdLabel = GuidebookText.HoldToShow
            .text(EnumChatFormatting.GRAY + keyName + EnumChatFormatting.DARK_GRAY);

        if (progress <= 0f) {
            return EnumChatFormatting.DARK_GRAY + holdLabel;
        }

        int barChar = fr.getCharWidth('|');
        if (barChar <= 0) {
            barChar = 2;
        }
        int totalWidth = fr.getStringWidth(holdLabel);
        int totalChars = Math.max(1, totalWidth / barChar);
        int filled = (int) (progress * totalChars);
        if (filled < 0) filled = 0;
        if (filled > totalChars) filled = totalChars;

        var sb = new StringBuilder();
        sb.append(EnumChatFormatting.GRAY);
        sb.repeat("|", filled);
        if (filled < totalChars) {
            sb.append(EnumChatFormatting.DARK_GRAY);
            sb.repeat("|", totalChars - filled);
        }
        return sb.toString();
    }

    public static void update(ItemStack stack) {
        updateResource(stack, null, null);
    }

    public static void updateResource(@Nullable ItemStack stack, @Nullable FluidStackSpec fluid,
        @Nullable Object source) {
        hoverObserved = stack != null || fluid != null;
        previousItemId = resolveItemId(stack);
        GuideTargetSelection next = HOVER_SESSION.update(stack, fluid, source);
        if (!next.equals(resourceSelection)) {
            resourceSelection = next;
            guidebookPages.clear();
            for (var target : next.targets()) {
                MutableGuide guide = GuideRegistry.getById(target.guideId());
                if (guide != null) guidebookPages.add(new FoundPage(guide, target.page()));
            }
        }
        ticksKeyHeld = HOVER_SESSION.ticksHeld();
    }

    public static void resetResourceHover() {
        HOVER_SESSION.reset();
        resourceSelection = new GuideTargetSelection("", List.of(), List.of());
        guidebookPages.clear();
        previousItemId = null;
        ticksKeyHeld = 0;
        holding = false;
        hoverObserved = false;
    }

    private static void tickResourceHover() {
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean held = isKeyHeld();
        if (!held) activationBlocked = false;
        if (!Display.isActive() || minecraft.currentScreen == null || minecraft.thePlayer == null) {
            resetResourceHover();
            return;
        }
        if (minecraft.currentScreen instanceof GuideScreen screen) {
            screen.updateGuideHotkeyHover();
        } else {
            int x = Mouse.getX() * minecraft.currentScreen.width / Math.max(1, minecraft.displayWidth);
            int y = minecraft.currentScreen.height
                - Mouse.getY() * minecraft.currentScreen.height / Math.max(1, minecraft.displayHeight)
                - 1;
            var fluid = FluidStackResolver.getHoveredFluid(minecraft.currentScreen, x, y);
            if (fluid != null) updateResource(null, FluidStackSpec.fromStack(fluid, false), null);
        }
        if (!hoverObserved) resetResourceHover();
        holding = held && !activationBlocked;
        if (HOVER_SESSION.advance(holding, TICKS_TO_OPEN)) {
            activationBlocked = true;
            GuideScreen screen = GuideScreen.current();
            if (screen != null) screen.openResourceTargets(resourceSelection);
            else GuideScreen.openFromGuideHotkey(resourceSelection);
        }
        ticksKeyHeld = HOVER_SESSION.ticksHeld();
        hoverObserved = false;
    }

    public static boolean isKeyHeld() {
        return GuideKeyBindingSupport.isDown(OPEN_GUIDE_KEY);
    }

    public static String resolveItemId(ItemStack stack) {
        if (stack == null) return null;
        Item item = stack.getItem();
        if (item == null) return null;
        Object name = Item.itemRegistry.getNameForObject(item);
        if (name == null) return null;
        int meta = stack.getItemDamage();
        return meta == 0 ? name.toString() : name + ":" + meta;
    }

    private static List<FoundPage> findQuestPages(UUID questId) {
        if (questId == null) return List.of();
        return BindingNavigationService.findTargets(new BindingKey("betterquesting", questId.toString()))
            .stream()
            .map(target -> new FoundPage(GuideRegistry.getById(target.guideId()), target.page()))
            .toList();
    }

    public static void updateBindingHover(BindingKey key) {
        guideHoveredBinding = key;
    }

    public static boolean appendBindingTooltip(BindingKey key, List<String> lines) {
        String hint = bindingHint(key);
        if (hint == null) return false;
        lines.add(hint);
        return true;
    }

    @Nullable
    public static String bindingHint(BindingKey key) {
        if (TOOLTIP_HINTS_SUPPRESSED.get()) return null;
        var selection = BINDING_SESSION.update(Minecraft.getMinecraft().currentScreen, key);
        if (selection.targets()
            .isEmpty()) return null;
        return renderHint(BINDING_SESSION.ticksHeld() / (float) TICKS_TO_OPEN);
    }

    public static void tickQuestHover() {
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen screen = minecraft.currentScreen;
        boolean held = isKeyHeld();
        if (!Display.isActive() || screen == null || minecraft.thePlayer == null) {
            BINDING_SESSION.reset();
            guideHoveredBinding = null;
            return;
        }
        int x = Mouse.getX() * screen.width / Math.max(1, minecraft.displayWidth);
        int y = screen.height - Mouse.getY() * screen.height / Math.max(1, minecraft.displayHeight) - 1;
        BindingKey key = guideHoveredBinding;
        guideHoveredBinding = null;
        var registry = GuideNhClientIntegrationRegistry.global();
        if (!(screen instanceof GuideScreen)) {
            var target = registry.findHoveredBinding(screen, x, y);
            if (target != null) key = target.key();
            else {
                UUID quest = registry.currentHoveredQuestId();
                if (quest != null) key = new BindingKey("betterquesting", quest.toString());
            }
        }
        var selection = BINDING_SESSION.update(screen, key);
        previousQuestId = key != null && "betterquesting".equals(key.type()) ? UUID.fromString(key.id()) : null;
        questGuidebookPages.clear();
        if (previousQuestId != null) {
            for (var target : selection.targets())
                questGuidebookPages.add(new FoundPage(GuideRegistry.getById(target.guideId()), target.page()));
        }
        if (BINDING_SESSION.advance(held, TICKS_TO_OPEN)) {
            if (screen instanceof GuideScreen guide) guide.openResourceTargets(selection);
            else GuideScreen.openFromGuideHotkey(selection);
        }
        questTicksKeyHeld = BINDING_SESSION.ticksHeld();
    }

    public static KeyBinding getHotkey() {
        return OPEN_GUIDE_KEY;
    }
}
