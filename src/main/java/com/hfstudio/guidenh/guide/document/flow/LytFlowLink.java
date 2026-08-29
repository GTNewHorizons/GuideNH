package com.hfstudio.guidenh.guide.document.flow;

import java.net.URI;
import java.util.function.Consumer;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.sound.GuideSoundPlayback;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytFlowLink extends LytTooltipSpan {

    @Nullable
    private Consumer<GuideUiHost> clickCallback;
    @Nullable
    private URI externalUrl;
    @Nullable
    private GuideAnchor guideAnchor;
    @Nullable
    private PageAnchor pageAnchor;

    @Nullable
    private GuideSoundSpec clickSoundSpec;
    private boolean playedCustomClickSound;

    public LytFlowLink() {
        modifyStyle(
            style -> style.color(SymbolicColor.LINK)
                .underlined(true));
        modifyHoverStyle(style -> style.underlined(true));
    }

    public void setClickCallback(@Nullable Consumer<GuideUiHost> clickCallback) {
        this.clickCallback = clickCallback;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button == 0 && clickCallback != null) {
            playedCustomClickSound = GuideSoundPlayback.play(clickSoundSpec);
            clickCallback.accept(screen);
            return true;
        }
        return false;
    }

    public void setClickSoundSpec(@Nullable GuideSoundSpec clickSoundSpec) {
        this.clickSoundSpec = clickSoundSpec;
    }

    public boolean consumePlayedCustomClickSound() {
        boolean played = playedCustomClickSound;
        playedCustomClickSound = false;
        return played;
    }

    @Nullable
    public GuideSoundSpec getClickSoundSpec() {
        return clickSoundSpec;
    }

    public void setExternalUrl(URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("External URLs must be absolute: " + uri);
        }
        this.externalUrl = uri;
        this.guideAnchor = null;
        this.pageAnchor = null;
        setClickCallback(screen -> screen.openExternalUrl(uri));
    }

    public void setPageLink(PageAnchor anchor) {
        setGuideLink(null, anchor);
    }

    public void setGuideLink(@Nullable ResourceLocation guideId, PageAnchor anchor) {
        this.guideAnchor = guideId == null ? null : new GuideAnchor(guideId, anchor);
        this.pageAnchor = anchor;
        this.externalUrl = null;
        setClickCallback(screen -> {
            if (guideId == null) {
                screen.navigateTo(anchor);
            } else {
                screen.navigateTo(guideId, anchor);
            }
        });
    }

    @Nullable
    public URI getExternalUrl() {
        return externalUrl;
    }

    @Nullable
    public PageAnchor getPageAnchor() {
        return pageAnchor;
    }

    @Nullable
    public GuideAnchor getGuideAnchor() {
        return guideAnchor;
    }
}
