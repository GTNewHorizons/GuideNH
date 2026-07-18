package com.hfstudio.guidenh.guide.internal.welcome;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

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
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.DocumentInteractionSnapshot;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.host.LytHost;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipLines;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipRenderSupport;
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
    private static final float SCROLL_SHARPNESS = 28F;
    private static final float SCROLL_SNAP_EPSILON = 0.01F;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int CLOSE_RIGHT_MARGIN = 7;
    private static final int CLOSE_TOP_MARGIN = 3;
    private static final int EXTERNAL_LINK_CONFIRM_ID = 0;

    private final GuiScreen parent;
    private final GuideWelcomeContent.LoadedContent content;
    private final VanillaRenderContext renderContext = new VanillaRenderContext(
        LightDarkMode.DARK_MODE,
        LytRect.empty(),
        0);
    private final VanillaRenderContext contentTooltipRenderContext = new VanillaRenderContext(
        LightDarkMode.LIGHT_MODE,
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
    private long lastScrollUpdateNanos;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffsetY;
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
        lastScrollUpdateNanos = 0;
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
        scrollY = clampScroll(scrollY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && startScrollbarDrag(mouseX, mouseY)) {
            return;
        }
        if (handleDocumentClick(mouseX, mouseY, mouseButton)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingScrollbar && clickedMouseButton == 0) {
            updateScrollFromMouseY(mouseY);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        if (draggingScrollbar && state != -1) {
            draggingScrollbar = false;
            return;
        }
        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == closeButton) {
            close();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateVisualScroll();
        if (parent != null) {
            parent.drawScreen(-1, -1, partialTicks);
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
            drawScrollbar();
        }

        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("guidenh.welcome.close_hint"),
            width / 2,
            panelBottom - 14,
            0xFFB8C0CC);

        drawHoverTooltip(mouseX, mouseY);
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
        renderContext.setScreenHeight(height);
        renderContext.setDocumentOrigin(docX, docY);
        renderContext.setPreciseScrollOffsetY(scrollY);
        renderContext.setZoom(1.0f);
        renderContext.pushScissor(new LytRect(docX, docY, docW, docH));
        GL11.glPushMatrix();
        GL11.glTranslatef(docX, docY, 0f);
        GL11.glTranslatef(0f, -scrollY, 0f);
        try {
            document.render(renderContext);
        } catch (Throwable t) {
            GuideDebugLog.error("[GuideNH] Error rendering welcome popup", t);
        } finally {
            GL11.glPopMatrix();
            renderContext.restoreExternalRenderState();
            renderContext.popScissor();
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

    private void updateVisualScroll() {
        long now = System.nanoTime();
        if (lastScrollUpdateNanos == 0) {
            lastScrollUpdateNanos = now;
            return;
        }
        float deltaSeconds = Math.min(0.05F, (now - lastScrollUpdateNanos) / 1_000_000_000F);
        lastScrollUpdateNanos = now;
        float delta = targetScrollY - scrollY;
        if (Math.abs(delta) <= SCROLL_SNAP_EPSILON) {
            scrollY = targetScrollY;
            return;
        }
        float blend = 1F - (float) Math.exp(-SCROLL_SHARPNESS * deltaSeconds);
        scrollY += delta * blend;
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

    private void drawScrollbar() {
        int[] thumb = scrollbarThumbRect();
        int x = thumb[0];
        int top = thumb[4];
        int height = thumb[5];
        drawRect(x, top, x + SCROLLBAR_WIDTH, top + height, 0x33262D38);
        int color = draggingScrollbar ? 0xFFFFFFFF : 0x99B8C0CC;
        drawRect(x, thumb[1], x + SCROLLBAR_WIDTH, thumb[1] + thumb[3], color);
    }

    private int[] scrollbarThumbRect() {
        int top = documentTop();
        int height = Math.max(1, documentBottom() - top);
        int contentHeight = document != null ? Math.max(1, document.getContentHeight()) : 1;
        int thumbHeight = Math.max(14, height * documentHeight() / contentHeight);
        int thumbY = top + Math.round((height - thumbHeight) * scrollY / Math.max(1F, maxScrollY()));
        return new int[] { panelX() + panelWidth() - 8, thumbY, SCROLLBAR_WIDTH, thumbHeight, top, height };
    }

    private boolean startScrollbarDrag(int mouseX, int mouseY) {
        if (maxScrollY() <= 0) {
            return false;
        }
        int[] thumb = scrollbarThumbRect();
        int x = thumb[0];
        int top = thumb[4];
        if (mouseX < x || mouseX >= x + thumb[2] || mouseY < top || mouseY >= top + thumb[5]) {
            return false;
        }
        if (mouseY >= thumb[1] && mouseY < thumb[1] + thumb[3]) {
            scrollbarGrabOffsetY = mouseY - thumb[1];
        } else {
            scrollbarGrabOffsetY = thumb[3] / 2;
            updateScrollFromMouseY(mouseY);
        }
        draggingScrollbar = true;
        return true;
    }

    private void updateScrollFromMouseY(int mouseY) {
        int[] thumb = scrollbarThumbRect();
        int track = Math.max(1, thumb[5] - thumb[3]);
        int relativeY = Math.clamp(mouseY - scrollbarGrabOffsetY - thumb[4], 0, track);
        scrollY = relativeY * maxScrollY() / track;
        targetScrollY = scrollY;
    }

    private void drawHoverTooltip(int mouseX, int mouseY) {
        if (closeButton != null && mouseX >= closeButton.xPosition
            && mouseY >= closeButton.yPosition
            && mouseX < closeButton.xPosition + closeButton.width
            && mouseY < closeButton.yPosition + closeButton.height) {
            drawHoveringText(List.of(closeButton.getTooltip()), mouseX, mouseY, fontRendererObj);
            return;
        }

        GuideTooltip tooltip = findDocumentTooltip(mouseX, mouseY);
        if (tooltip instanceof ItemTooltip itemTooltip) {
            drawItemTooltip(itemTooltip, mouseX, mouseY);
        } else if (tooltip instanceof TextTooltip textTooltip) {
            String text = textTooltip.getText()
                .replace("\\n", "\n");
            drawHoveringText(List.of(text.split("\n", -1)), mouseX, mouseY, fontRendererObj);
        } else if (tooltip instanceof ContentTooltip contentTooltip) {
            drawContentTooltip(contentTooltip, mouseX, mouseY);
        }
    }

    @Nullable
    private GuideTooltip findDocumentTooltip(int mouseX, int mouseY) {
        DocumentInteractionSnapshot hit = pickDocument(mouseX, mouseY);
        if (hit == null) {
            return null;
        }
        int docX = mouseX - documentX();
        int docY = mouseY - documentTop() + Math.round(scrollY);
        for (LytFlowContent content : interactiveFlowTargets(hit)) {
            Optional<GuideTooltip> tooltip = GuideScreen.tryGetTooltip(content, docX, docY);
            if (tooltip.isPresent()) {
                return tooltip.get();
            }
        }
        for (LytNode current = hit.node(); current != null; current = current.getParent()) {
            Optional<GuideTooltip> tooltip = GuideScreen.tryGetTooltip(current, docX, docY);
            if (tooltip.isPresent()) {
                return tooltip.get();
            }
        }
        return null;
    }

    private void drawItemTooltip(ItemTooltip tooltip, int mouseX, int mouseY) {
        ItemStack stack = tooltip.getStack();
        if (stack == null) {
            return;
        }
        List<String> lines = GuideItemTooltipLines.build(tooltip, mc);
        FontRenderer font = GuideItemTooltipRenderSupport.resolveFont(stack, fontRendererObj);
        drawHoveringText(lines, mouseX, mouseY, font);
    }

    private void drawContentTooltip(ContentTooltip tooltip, int mouseX, int mouseY) {
        int padding = 4;
        int left = padding;
        int top = padding;
        int right = width - padding;
        int bottom = height - padding;
        LytRect box = tooltip.layout(Math.max(80, (right - left) * 4 / 5));
        int tooltipWidth = box.width();
        int tooltipHeight = box.height();
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + tooltipWidth + padding > right) {
            x = mouseX - tooltipWidth - 12;
        }
        x = Math.clamp(x, left, Math.max(left, right - tooltipWidth));
        y = Math.clamp(y, top, Math.max(top, bottom - tooltipHeight));

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        zLevel = 300F;
        itemRender.zLevel = 300F;
        int background = 0xF0100010;
        drawGradientRect(
            x - padding,
            y - padding,
            x + tooltipWidth + padding,
            y + tooltipHeight + padding,
            background,
            background);
        drawGradientRect(x - padding, y - padding, x + tooltipWidth + padding, y - padding + 1, 0x505000FF, 0x505000FF);
        drawGradientRect(
            x - padding,
            y + tooltipHeight + padding - 1,
            x + tooltipWidth + padding,
            y + tooltipHeight + padding,
            0x5028007F,
            0x5028007F);

        contentTooltipRenderContext.setViewport(new LytRect(0, 0, tooltipWidth, tooltipHeight));
        contentTooltipRenderContext.setScreenHeight(height);
        contentTooltipRenderContext.setDocumentOrigin(x, y);
        contentTooltipRenderContext.setScrollOffsetY(0);
        contentTooltipRenderContext.setZoom(1F);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 300F);
        try {
            tooltip.getContent()
                .render(contentTooltipRenderContext);
        } catch (Throwable t) {
            GuideDebugLog.warnAlways("[GuideNH] Error rendering welcome tooltip", t);
        } finally {
            GL11.glPopMatrix();
            contentTooltipRenderContext.restoreExternalRenderState();
            zLevel = 0F;
            itemRender.zLevel = 0F;
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
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
