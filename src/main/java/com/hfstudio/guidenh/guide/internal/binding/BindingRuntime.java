package com.hfstudio.guidenh.guide.internal.binding;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.binding.BindingDisplay;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.api.binding.BindingState;
import com.hfstudio.guidenh.integration.api.binding.BindingViewContext;

public class BindingRuntime {

    protected BindingRuntime() {}

    public static BindingViewContext view(boolean description, boolean staticView) {
        var player = staticView ? null : Minecraft.getMinecraft().thePlayer;
        return new BindingViewContext(
            player == null ? null : player.getUniqueID(),
            player == null ? null : player.getCommandSenderName(),
            LangUtil.getCurrentLanguage(),
            description,
            false,
            staticView);
    }

    public static BindingDisplay describe(BindingKey key, BindingViewContext context) {
        var provider = GuideNhIntegrationRegistry.global()
            .bindingProvider(key.type());
        if (provider == null) return placeholder(key, BindingState.UNAVAILABLE);
        try {
            return provider.describe(key, context);
        } catch (RuntimeException exception) {
            GuideDebugLog.warn("[GuideNH] Binding provider {} could not describe {}", key.type(), key.id(), exception);
            return placeholder(key, BindingState.UNAVAILABLE);
        }
    }

    public static BindingDisplay placeholder(BindingKey key, BindingState state) {
        return new BindingDisplay(key, state, null, null, null, List.of(), List.of(), false);
    }

    public static String title(BindingDisplay display) {
        if (display.state()
            .equals(BindingState.HIDDEN)
            || display.state()
                .equals(BindingState.MISSING)
            || display.state()
                .equals(BindingState.UNAVAILABLE)) {
            return "[" + netTitle(display.state()) + "]";
        }
        return display.title() == null || display.title()
            .isEmpty() ? display.key()
                .id() : display.title();
    }

    private static String netTitle(BindingState state) {
        return StatCollector.translateToLocal("guidenh.binding.state." + state.id());
    }
}
