package com.hfstudio.guidenh.integration.betterquesting;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.guide.GuideBuilder;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.indices.GuideBindingIndex;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.client.GuideNhClientIntegrationRegistry;
import com.hfstudio.guidenh.integration.betterquesting.compiler.QuestCardCompiler;
import com.hfstudio.guidenh.integration.betterquesting.compiler.QuestLinkCompiler;

import betterquesting.api2.client.gui.context.QuestHoverRegistry;
import betterquesting.api2.client.gui.context.QuestTooltipRegistry;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import cpw.mods.fml.common.Optional;

/**
 * BetterQuesting integration entry point.
 * <p/>
 * Optional callbacks that use BetterQuesting types are stripped when the mod is absent.
 * Index registration and compiler setup remain independent of BetterQuesting classes.
 */
public class BqCompat {

    /**
     * Attaches the {@link QuestIndex} to the given guide builder. Safe to call when BQ is
     * absent because {@link QuestIndex} only depends on standard library types.
     */
    public static void attachQuestIndex(GuideBuilder builder) {
        builder.index(new QuestIndex(builder.getIndex(GuideBindingIndex.class)));
    }

    /**
     * Appends BetterQuesting-aware tag compilers to the given mutable list of tag compilers.
     * The compilers themselves do not reference BQ types; they delegate runtime work to
     * {@link BqHelpers}.
     */
    public static void appendCompilers(List<TagCompiler> compilers) {
        compilers.add(new QuestLinkCompiler());
        compilers.add(new QuestCardCompiler());
    }

    @Optional.Method(modid = "betterquesting")
    public static void registerBetterQuestCompact() {
        GuideNhIntegrationRegistry.global()
            .registerBindingProvider(new BetterQuestingBindingProvider());
        BqGuidePageUriHandler.register();
        PanelTextBox.registerTextProcessor(
            new ResourceLocation("guidenh", "guide_page_links"),
            BqGuidePageLinks::replaceGuideTags);
        QuestTooltipRegistry.register((target, tooltip) -> {
            if (target instanceof UUID uuid) {
                OpenGuideHotkey.appendQuestTooltip(uuid, tooltip);
            } else if (target instanceof Map.Entry<?, ?>entry) {
                Object key = entry.getKey();
                if (key instanceof UUID uuid) {
                    OpenGuideHotkey.appendQuestTooltip(uuid, tooltip);
                }
            } else if (target instanceof String s) {
                List<String> guideTooltip = BqGuidePageLinks.getTooltip(s);
                if (guideTooltip != null) {
                    tooltip.addAll(guideTooltip);
                }
            }
        });
        BetterQuestingQuestHoverProvider hoverProvider = new BetterQuestingQuestHoverProvider();
        QuestHoverRegistry.register(hoverProvider);
        GuideNhClientIntegrationRegistry.global()
            .registerQuestHoverProvider(hoverProvider);
    }
}
