package com.hfstudio.guidenh.guide.internal.binding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.indices.GuideBindingIndex;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.fluid.GuideContentTargetResolver;
import com.hfstudio.guidenh.guide.internal.search.GuideTargetSelection;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.api.binding.BindingOpenResult;

public class BindingNavigationService {

    private static final int MAX_CACHED_TARGETS = 256;
    private static final LinkedHashMap<BindingKey, List<GuideAnchor>> cachedTargets = new LinkedHashMap<>(
        16,
        0.75f,
        true);
    private static long cachedRevision = Long.MIN_VALUE;

    protected BindingNavigationService() {}

    public static long revision() {
        long revision = 1;
        for (var guide : GuideRegistry.getAll()) {
            if (!guide.isAvailableToOpenHotkey()) continue;
            try {
                revision = 31 * revision + guide.getId()
                    .hashCode();
                var index = guide.getIndex(GuideBindingIndex.class);
                revision = 31 * revision + System.identityHashCode(index);
                revision = 31 * revision + index.getRevision();
            } catch (IllegalArgumentException ignored) {}
        }
        return revision;
    }

    public static synchronized List<GuideAnchor> findTargets(BindingKey key) {
        long revision = revision();
        if (cachedRevision != revision) {
            cachedTargets.clear();
            cachedRevision = revision;
        }
        var cached = cachedTargets.get(key);
        if (cached != null) return cached;
        var guides = new ArrayList<MutableGuide>(GuideRegistry.getAll());
        guides.sort(
            Comparator.comparing(
                guide -> guide.getId()
                    .toString()));
        var targets = new LinkedHashSet<GuideAnchor>();
        for (var guide : guides) {
            if (!guide.isAvailableToOpenHotkey()) continue;
            try {
                for (var anchor : GuideContentTargetResolver.orderAnchors(
                    guide,
                    guide.getIndex(GuideBindingIndex.class)
                        .findAll(key))) {
                    if (guide.pageExists(anchor.pageId())) targets.add(new GuideAnchor(guide.getId(), anchor));
                }
            } catch (IllegalArgumentException exception) {
                continue;
            }
        }
        var result = List.copyOf(targets);
        cachedTargets.put(key, result);
        if (cachedTargets.size() > MAX_CACHED_TARGETS) cachedTargets.pollFirstEntry();
        return result;
    }

    public static boolean openGuides(GuideUiHost host, BindingKey key, String title) {
        var targets = findTargets(key);
        if (targets.isEmpty()) return false;
        if (host instanceof GuideScreen screen) {
            screen.openResourceTargets(new GuideTargetSelection(title, List.of(), targets));
        } else if (targets.size() == 1) {
            var target = targets.getFirst();
            host.navigateTo(target.guideId(), target.page());
        } else {
            GuideScreen.openFromGuideHotkey(new GuideTargetSelection(title, List.of(), targets));
        }
        return true;
    }

    public static BindingOpenResult openNative(GuideUiHost host, BindingKey key) {
        var provider = GuideNhIntegrationRegistry.global()
            .bindingProvider(key.type());
        BindingOpenResult result = BindingOpenResult.failure("guidenh.binding.unavailable");
        GuideScreen guideScreen = host instanceof GuideScreen screen ? screen : null;
        if (guideScreen != null) guideScreen.prepareForTemporaryScreenChange();
        if (provider != null) {
            try {
                result = provider.open(key, host);
            } catch (RuntimeException exception) {
                GuideDebugLog.warn("[GuideNH] Binding provider {} could not open {}", key.type(), key.id(), exception);
            }
        }
        if (!result.opened() && guideScreen != null) guideScreen.cancelTemporaryScreenChange();
        if (!result.opened() && result.messageKey() != null && Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer
                .addChatMessage(new ChatComponentTranslation(result.messageKey(), key.id()));
        }
        return result;
    }

    public static void openGuidesOnly(GuideUiHost host, BindingKey key, String title) {
        if (!openGuides(host, key, title) && Minecraft.getMinecraft().thePlayer != null)
            Minecraft.getMinecraft().thePlayer
                .addChatMessage(new ChatComponentTranslation("guidenh.binding.no_guide", key.id()));
    }

    public static void openGuidesOrNative(GuideUiHost host, BindingKey key, String title) {
        if (!openGuides(host, key, title)) openNative(host, key);
    }
}
