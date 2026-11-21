package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;

import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.nei.NeiLinkTarget;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.nei.GuideScreenNeiBridge;
import com.hfstudio.guidenh.integration.nei.NeiGuideNavigation;

public class NeiLinkScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "NeiRecipeLink";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext context) {
        if (event.type() == EventType.MOUNT && node instanceof LytFlowLink link
            && link.getData("neiLinkTarget") instanceof NeiLinkTarget target) {
            link.setClickCallback(screen -> open(screen, target));
            context.replace(link);
        }
    }

    public static boolean open(GuideUiHost screen, NeiLinkTarget target) {
        String failure = "guidenh.nei.link.no_recipes";
        try {
            if (!Mods.NotEnoughItems.isModLoaded()) {
                failure = "guidenh.nei.link.unavailable";
            } else if (NeiGuideNavigation
                .openLink(screen instanceof GuideScreenNeiBridge.EditorAccess access ? access : null, target)) {
                    return true;
                }
        } catch (RuntimeException | LinkageError exception) {
            GuideDebugLog.error("[GuideNH] NEI link failed for {}: {}", target, exception.toString());
        }
        GuideDebugLog.warn("[GuideNH] Unable to open NEI link: {}", target);
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentTranslation(failure));
        }
        return false;
    }
}
