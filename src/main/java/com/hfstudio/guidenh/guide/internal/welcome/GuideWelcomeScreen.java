package com.hfstudio.guidenh.guide.internal.welcome;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.hfstudio.guidenh.ClientProxy;
import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.DocumentInteractionSnapshot;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.host.LytHost;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.MinecraftFontMetrics;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class GuideWelcomeScreen extends GuiScreen implements GuideUiHost, GuiYesNoCallback {

    private static final ResourceLocation PAGE_ID = new ResourceLocation(GuideNH.MODID, "welcome_popup");
    private static final int PANEL_MAX_WIDTH = 420;
    private static final int PANEL_MAX_HEIGHT = 260;
    private static final int PANEL_PADDING = 16;
    private static final int HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 18;
    private static final int SCROLL_STEP = 36;
    private static final float SCROLL_LERP = 0.35F;
    private static final float SCROLL_SNAP_EPSILON = 0.35F;
    private static final int CLOSE_RIGHT_MARGIN = 7;
    private static final int CLOSE_TOP_MARGIN = 3;
    private static final int EXTERNAL_LINK_CONFIRM_ID = 0;

    private final GuiScreen parent;
    private final GuideWelcomeContent.LoadedContent content;
    private final VanillaRenderContext renderContext = new VanillaRenderContext(
        LightDarkMode.DARK_MODE,
        LytRect.empty(),
        0);
    private final MinecraftFontMetrics fontMetrics = new MinecraftFontMetrics();

    @Nullable
    private MutableGuide pageCollection;
    @Nullable
    private GuidePage page;
    @Nullable
    private LytDocument document;
    @Nullable
    private GuideIconButton closeButton;
    private int layoutWidth = -1;
    private float scrollY;
    private float targetScrollY;
    private boolean hostStateSaved;
    @Nullable
    private LytDocument parentDocument;
    @Nullable
    private PageCollection parentPageCollection;
    @Nullable
    private String parentPageId;
    @Nullable
    private URI pendingExternalUri;

    public GuideWelcomeScreen(GuiScreen parent, GuideWelcomeContent.LoadedContent content) {
        this.parent = parent;
        this.content = content;
        this.pageCollection = resolvePageCollection();
        this.page = compilePage(content);
        if (this.page != null) {
            this.page.prepareForDisplay();
            this.document = this.page.document();
        }
    }

    @Override
    public void initGui() {
        buttonList.clear();
        layoutDocument();
        mountDocument();
        updateCloseButton();
        targetScrollY = clampScroll(targetScrollY);
        scrollY = clampScroll(scrollY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_ESCAPE) {
            close();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel > 0) {
            setTargetScroll(targetScrollY - SCROLL_STEP);
        } else if (wheel < 0) {
            setTargetScroll(targetScrollY + SCROLL_STEP);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        layoutDocument();
        tickScenes();
        targetScrollY = clampScroll(targetScrollY);
        float delta = targetScrollY - scrollY;
        if (Math.abs(delta) <= SCROLL_SNAP_EPSILON) {
            scrollY = targetScrollY;
        } else {
            scrollY += delta * SCROLL_LERP;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (handleDocumentClick(mouseX, mouseY, mouseButton)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == closeButton) {
            close();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (parent != null) {
            parent.drawScreen(mouseX, mouseY, partialTicks);
        } else {
            drawDefaultBackground();
        }

        drawRect(0, 0, width, height, 0x88000000);

        int panelW = panelWidth();
        int panelH = panelHeight();
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;
        int panelRight = panelX + panelW;
        int panelBottom = panelY + panelH;

        drawRect(panelX, panelY, panelRight, panelBottom, 0xF0181C22);
        drawRect(panelX, panelY, panelRight, panelY + 1, 0xFF586170);
        drawRect(panelX, panelBottom - 1, panelRight, panelBottom, 0xFF586170);
        drawRect(panelX, panelY, panelX + 1, panelBottom, 0xFF586170);
        drawRect(panelRight - 1, panelY, panelRight, panelBottom, 0xFF586170);

        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("guidenh.welcome.title"),
            width / 2,
            panelY + 10,
            0xFFF0F0F0);
        drawCloseButton(mouseX, mouseY, panelRight, panelY);

        renderDocument(mouseX, mouseY);

        if (maxScrollY() > 0) {
            drawScrollbar(panelRight - 8, documentTop(), documentBottom());
        }

        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("guidenh.welcome.close_hint"),
            width / 2,
            panelBottom - 14,
            0xFFB8C0CC);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void navigateTo(PageAnchor anchor) {
        if (parent instanceof GuideUiHost host) {
            closeToParent();
            host.navigateTo(anchor);
        }
    }

    @Override
    public void navigateTo(ResourceLocation guideId, PageAnchor anchor) {
        if (parent instanceof GuideUiHost host) {
            closeToParent();
            host.navigateTo(guideId, anchor);
        }
    }

    @Override
    public void close() {
        closeToParent();
    }

    @Override
    public void openExternalUrl(URI uri) {
        if (ModConfig.ui.confirmExternalLinks) {
            pendingExternalUri = uri;
            mc.displayGuiScreen(createExternalLinkConfirmScreen(uri));
            return;
        }

        browseExternalUrl(uri);
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (id != EXTERNAL_LINK_CONFIRM_ID) {
            return;
        }

        URI uri = pendingExternalUri;
        pendingExternalUri = null;
        if (result && uri != null) {
            browseExternalUrl(uri);
        }
        mc.displayGuiScreen(this);
    }

    @Override
    public boolean copyCodeBlock(String text) {
        return parent instanceof GuideUiHost host && host.copyCodeBlock(text);
    }

    private void closeToParent() {
        markWelcomeClosed();
        if (document != null) {
            document.setHoveredElement(null);
        }
        restoreHostState();
        mc.displayGuiScreen(parent);
    }

    private void markWelcomeClosed() {
        if (ModConfig.ui.welcomePopupEnabled) {
            ModConfig.ui.welcomePopupEnabled = false;
            ModConfig.save();
        }
    }

    private void layoutDocument() {
        if (document == null) {
            return;
        }
        int textWidth = documentWidth();
        if (textWidth <= 0) {
            return;
        }
        if (!document.hasLayout() || layoutWidth != textWidth) {
            document.updateLayout(new LayoutContext(fontMetrics), textWidth);
            layoutWidth = textWidth;
        }
    }

    private void mountDocument() {
        if (document == null || pageCollection == null) {
            return;
        }
        LytHost host = ClientProxy.getLytHost();
        saveHostState(host);
        host.setCurrentPageId(PAGE_ID.toString());
        host.setCurrentPageCollection(pageCollection);
        host.mountDocument(document);
    }

    private void saveHostState(LytHost host) {
        if (hostStateSaved) {
            return;
        }
        parentDocument = host.getDocument();
        parentPageCollection = host.getCurrentPageCollection();
        parentPageId = host.getCurrentPageId();
        hostStateSaved = true;
    }

    private void restoreHostState() {
        if (!hostStateSaved) {
            return;
        }
        LytHost host = ClientProxy.getLytHost();
        host.setCurrentPageId(parentPageId);
        host.setCurrentPageCollection(parentPageCollection);
        host.mountDocument(parentDocument);
        hostStateSaved = false;
    }

    private void tickScenes() {
        if (page == null) {
            return;
        }
        registerRuntimeScenes(page);
        for (LytGuidebookScene scene : page.scenes()) {
            scene.ponderTick();
        }
    }

    private static void registerRuntimeScenes(GuidePage page) {
        LytDocument doc = page.document();
        if (doc == null) {
            return;
        }
        List<LytGuidebookScene> scenes = page.scenes();
        ArrayDeque<LytNode> pending = new ArrayDeque<>();
        pending.add(doc);
        while (!pending.isEmpty()) {
            LytNode node = pending.removeLast();
            if (node instanceof LytGuidebookScene scene && !scenes.contains(scene)) {
                scenes.add(scene);
            }
            List<? extends LytNode> children = node.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                pending.addLast(children.get(i));
            }
        }
    }

    @Nullable
    private GuidePage compilePage(GuideWelcomeContent.LoadedContent loadedContent) {
        if (pageCollection == null) {
            GuideDebugLog.warnAlways("[GuideNH] Failed to compile welcome popup: no guide collection is available");
            return null;
        }
        try {
            ParsedGuidePage parsed = PageCompiler
                .parse(loadedContent.sourcePack(), loadedContent.language(), PAGE_ID, loadedContent.source());
            return PageCompiler.compile(pageCollection, pageCollection.getExtensions(), parsed);
        } catch (Throwable t) {
            GuideDebugLog.error("[GuideNH] Failed to compile welcome popup content", t);
            return PageCompiler.buildErrorGuidePage(
                pageCollection,
                pageCollection.getExtensions(),
                loadedContent.sourcePack(),
                PAGE_ID,
                loadedContent.source(),
                "WELCOME POPUP ERROR",
                t.toString());
        }
    }

    @Nullable
    private static MutableGuide resolvePageCollection() {
        MutableGuide defaultGuide = GuideRegistry.getById(new ResourceLocation(GuideNH.MODID, "guidenh"));
        if (defaultGuide != null) {
            return defaultGuide;
        }

        Collection<MutableGuide> guides = GuideRegistry.getAll();
        for (MutableGuide guide : guides) {
            if (GuideNH.MODID.equals(guide.getDefaultNamespace())) {
                return guide;
            }
        }
        return guides.isEmpty() ? null
            : guides.iterator()
                .next();
    }

    private void renderDocument(int mouseX, int mouseY) {
        if (document == null) {
            drawCenteredString(fontRendererObj, I18n.format("gui.done"), width / 2, documentTop(), 0xFFD0D8E0);
            return;
        }

        int docX = documentX();
        int docY = documentTop();
        int docW = documentWidth();
        int docH = documentHeight();
        if (docW <= 0 || docH <= 0) {
            return;
        }

        DocumentInteractionSnapshot hit = pickDocument(mouseX, mouseY);
        document.setHoveredElement(hit);

        int viewportTop = Math.max(0, Math.round(scrollY));
        renderContext.setLightDarkMode(LightDarkMode.DARK_MODE);
        renderContext.setViewport(new LytRect(0, viewportTop, docW, docH));
        renderContext.setScreenViewport(new LytRect(docX, docY, docW, docH));
        renderContext.setScreenHeight(height);
        renderContext.setDocumentOrigin(docX, docY);
        renderContext.setPreciseScrollOffsetY(scrollY);
        renderContext.setZoom(1.0f);
        // No GL matrix or context scissor here: the primitive pipeline's render
        // engine owns the document->screen transform and the viewport clip.
        try {
            document.render(renderContext);
        } catch (Throwable t) {
            GuideDebugLog.error("[GuideNH] Error rendering welcome popup", t);
        }
    }

    private boolean handleDocumentClick(int mouseX, int mouseY, int button) {
        if (document == null || !isInsideDocument(mouseX, mouseY)) {
            return false;
        }
        DocumentInteractionSnapshot hit = pickDocument(mouseX, mouseY);
        if (hit == null) {
            return false;
        }
        int docX = mouseX - documentX();
        int docY = mouseY - documentTop() + Math.round(scrollY);
        for (LytFlowContent content : interactiveFlowTargets(hit)) {
            if (content instanceof InteractiveElement interactive
                && interactive.mouseClicked(this, docX, docY, button, false)) {
                return true;
            }
        }
        for (LytNode current = hit.node(); current != null; current = current.getParent()) {
            if (current instanceof InteractiveElement interactive
                && interactive.mouseClicked(this, docX, docY, button, false)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private DocumentInteractionSnapshot pickDocument(int mouseX, int mouseY) {
        if (document == null || !isInsideDocument(mouseX, mouseY) || !document.hasLayout()) {
            return null;
        }
        int docX = mouseX - documentX();
        int docY = mouseY - documentTop() + Math.round(scrollY);
        return document.pick(docX, docY);
    }

    private List<LytFlowContent> interactiveFlowTargets(DocumentInteractionSnapshot hit) {
        return hit != null ? hit.interactiveFlowTargets() : List.of();
    }

    private boolean isInsideDocument(int mouseX, int mouseY) {
        return mouseX >= documentX() && mouseX < documentX() + documentWidth()
            && mouseY >= documentTop()
            && mouseY < documentBottom();
    }

    private int panelWidth() {
        return Math.clamp(width - 40, 220, PANEL_MAX_WIDTH);
    }

    private int panelHeight() {
        return Math.clamp(height - 40, 160, PANEL_MAX_HEIGHT);
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int documentX() {
        return panelX() + PANEL_PADDING;
    }

    private int documentTop() {
        return panelY() + HEADER_HEIGHT;
    }

    private int documentBottom() {
        return panelY() + panelHeight() - FOOTER_HEIGHT - 8;
    }

    private int documentWidth() {
        return panelWidth() - PANEL_PADDING * 2 - 10;
    }

    private int documentHeight() {
        return Math.max(1, documentBottom() - documentTop());
    }

    private float maxScrollY() {
        return document != null ? Math.max(0, document.getContentHeight() - documentHeight()) : 0;
    }

    private float clampScroll(float scroll) {
        return Math.max(0, Math.min(maxScrollY(), scroll));
    }

    private void setTargetScroll(float scroll) {
        targetScrollY = clampScroll(scroll);
    }

    private void drawCloseButton(int mouseX, int mouseY, int panelRight, int panelY) {
        updateCloseButton(panelRight, panelY);
        if (closeButton != null) {
            closeButton.drawButton(mc, mouseX, mouseY);
        }
    }

    private void updateCloseButton() {
        int panelRight = (width + panelWidth()) / 2;
        updateCloseButton(panelRight, panelY());
    }

    private void updateCloseButton(int panelRight, int panelY) {
        int closeX = closeX(panelRight);
        int closeY = closeY(panelY);
        if (closeButton == null) {
            closeButton = new GuideIconButton(0, closeX, closeY, GuideIconButton.Role.CLOSE);
        } else {
            closeButton.xPosition = closeX;
            closeButton.yPosition = closeY;
            closeButton.setRole(GuideIconButton.Role.CLOSE);
            closeButton.visible = true;
        }
        if (!buttonList.contains(closeButton)) {
            buttonList.add(closeButton);
        }
    }

    private int closeX(int panelRight) {
        return panelRight - CLOSE_RIGHT_MARGIN - GuideIconButton.WIDTH;
    }

    private int closeY(int panelY) {
        return panelY + CLOSE_TOP_MARGIN;
    }

    private void drawScrollbar(int x, int top, int bottom) {
        int height = Math.max(1, bottom - top);
        drawRect(x, top, x + 3, bottom, 0x33262D38);
        float maxScroll = maxScrollY();
        int contentHeight = document != null ? Math.max(1, document.getContentHeight()) : 1;
        int thumbHeight = Math.max(14, height * documentHeight() / contentHeight);
        int thumbY = top + Math.round((height - thumbHeight) * scrollY / Math.max(1, maxScroll));
        drawRect(x, thumbY, x + 3, thumbY + thumbHeight, 0x99B8C0CC);
    }

    private GuiConfirmOpenLink createExternalLinkConfirmScreen(URI uri) {
        return new GuiConfirmOpenLink(this, uri.toString(), EXTERNAL_LINK_CONFIRM_ID, false) {

            @Override
            protected void keyTyped(char typedChar, int keyCode) {
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    GuideWelcomeScreen.this.confirmClicked(false, EXTERNAL_LINK_CONFIRM_ID);
                    return;
                }
                super.keyTyped(typedChar, keyCode);
            }
        };
    }

    private void browseExternalUrl(URI uri) {
        try {
            Desktop.getDesktop()
                .browse(uri);
        } catch (Exception e) {
            GuideDebugLog.warnAlways("[GuideNH] Failed to open external welcome link {}", uri, e);
        }
    }

}
