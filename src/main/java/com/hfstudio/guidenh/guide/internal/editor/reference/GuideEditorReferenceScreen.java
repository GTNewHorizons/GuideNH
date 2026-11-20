package com.hfstudio.guidenh.guide.internal.editor.reference;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideEditorReferenceScreen extends GuideScreen {

    private final GuiScreen sourceScreen;
    private final GuideEditorReferencePages pages;
    private final String language;
    private GuideIconButton referenceButton;
    private GuideIconButton copyButton;

    public GuideEditorReferenceScreen(GuiScreen parent, Guide guide, String language) {
        this(parent, createPages(guide, language), language);
    }

    private GuideEditorReferenceScreen(GuiScreen parent, GuideEditorReferencePages pages, String language) {
        super(pages, pages.initialAnchor(), parent);
        this.sourceScreen = parent;
        this.pages = pages;
        this.language = language;
        getNavBar().setSearchPresentation(
            GuidebookText.GuideEditorReference.text(),
            GuidebookText.GuideEditorReferenceSearch.text(),
            pages::searchText);
    }

    private static GuideEditorReferencePages createPages(Guide guide, String language) {
        GuideEditorReferenceCatalog catalog;
        try {
            catalog = GuideEditorReferenceCatalog.load(language);
        } catch (IOException e) {
            GuideDebugLog.error("[GuideNH] Failed to load the editor reference", e);
            catalog = new GuideEditorReferenceCatalog(List.of());
        }
        return new GuideEditorReferencePages(guide, catalog, language);
    }

    @Override
    protected int getModeToolbarWidth() {
        return GuideIconButton.WIDTH * 2 + TOOLBAR_GAP;
    }

    @Override
    protected void rebuildModeToolbarButtons(int rightX, int buttonY) {
        referenceButton = new GuideIconButton(
            108,
            rightX - GuideIconButton.WIDTH,
            buttonY,
            GuideIconButton.Role.GUIDE_EDITOR_REFERENCE);
        referenceButton.setActive(true);
        buttonList.add(referenceButton);
        copyButton = new GuideIconButton(
            109,
            rightX - GuideIconButton.WIDTH * 2 - TOOLBAR_GAP,
            buttonY,
            GuideIconButton.Role.GUIDE_EDITOR_REFERENCE_COPY);
        copyButton.enabled = pages.section(getCurrentPageId()) != null;
        buttonList.add(copyButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == referenceButton) {
            mc.displayGuiScreen(sourceScreen);
        } else if (button == copyButton) {
            var section = pages.section(getCurrentPageId());
            if (section != null) {
                copyCodeBlock(section.markdown());
            }
        } else if (button.id == 4) {
            getNavBar().focusSearch(mc, false);
        } else if (button.id == 5) {
            navigateTo(pages.initialAnchor());
        } else {
            super.actionPerformed(button);
        }
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_F && isCtrlKeyDown()) {
            getNavBar().focusSearch(mc, false);
            return;
        }
        super.keyTyped(character, key);
    }

    @Override
    public void navigateTo(PageAnchor anchor) {
        String fragment = anchor.anchor() == null ? null
            : URLDecoder.decode(
                anchor.anchor()
                    .replace("+", "%2B"),
                StandardCharsets.UTF_8);
        PageAnchor target = pages.resolveAnchor(new PageAnchor(anchor.pageId(), fragment));
        if (target != null) {
            super.navigateTo(target);
        } else {
            String suffix = language.toLowerCase(Locale.ROOT)
                .startsWith("zh") ? "-zh-CN" : "";
            openExternalUrl(
                URI.create(
                    "https://github.com/GTNewHorizons/GuideNH/wiki/" + GuideEditorReferenceCatalog.normalizeDocumentId(
                        anchor.pageId()
                            .getResourcePath())
                        + suffix));
        }
    }

    @Override
    public void navigateTo(ResourceLocation guideId, PageAnchor anchor) {
        navigateTo(anchor);
    }

    @Override
    public boolean copyCodeBlock(String text) {
        setClipboardString(text);
        return true;
    }

    @Override
    public void close() {
        if (sourceScreen instanceof GuideScreen sourceGuide) {
            mc.displayGuiScreen(sourceGuide);
            sourceGuide.close();
        } else {
            super.close();
        }
    }
}
