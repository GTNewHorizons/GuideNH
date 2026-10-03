package com.hfstudio.guidenh.guide.internal.settings;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.GuideME;

/**
 * Routing helpers for the virtual "settings" page (Kind.SETTINGS). The page
 * document itself is built by {@link GuideSettingsDocumentBuilder}; this class
 * this class owns the page id / anchor only.
 */
public class GuideSettingsPage {

    public static final ResourceLocation PAGE_ID = GuideME.makeId("settings");

    private GuideSettingsPage() {}

    public static boolean isSettingsAnchor(@Nullable PageAnchor anchor) {
        return anchor != null && PAGE_ID.equals(anchor.pageId());
    }

    public static PageAnchor anchor() {
        return PageAnchor.page(PAGE_ID);
    }
}
