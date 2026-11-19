package com.hfstudio.guidenh.integration.betterquesting;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.guide.GuideBuilder;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
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
 * This class never references BetterQuesting types directly; all such access is funneled
 * through {@link BqHelpers}. That keeps {@code BqCompat} safe to load even when BetterQuesting
 * is missing from the classpath, so it can be statically referenced from the rest of the mod.
 */
public class BqCompat {

    /**
     * Attaches the {@link QuestIndex} to the given guide builder. Safe to call when BQ is
     * absent because {@link QuestIndex} only depends on standard library types.
     */
    public static void attachQuestIndex(GuideBuilder builder) {
        builder.index(new QuestIndex());
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
