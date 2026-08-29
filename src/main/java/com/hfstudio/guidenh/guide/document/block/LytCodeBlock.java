package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Objects;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorVerticalScrollbar;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguage;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightMode;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightResult;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightTheme;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlighter;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeTokenType;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

import lombok.Getter;
import lombok.Setter;

public class LytCodeBlock extends LytVBox implements InteractiveElement, DocumentDragTarget {

    private static final CodeHighlightTheme CODE_THEME = CodeHighlightTheme.GITHUB_DARK_DEFAULT;
    private static final CodeHighlighter CODE_HIGHLIGHTER = new CodeHighlighter();
    private static final CodeHighlightFlowBuilder FLOW_BUILDER = new CodeHighlightFlowBuilder(CODE_THEME);
    private static final ConstantColor CODE_DEFAULT = new ConstantColor(CODE_THEME.colorOf(CodeTokenType.PLAIN));
    private static final ConstantColor CODE_BACKGROUND = new ConstantColor(CODE_THEME.backgroundArgb());
    private static final ConstantColor CODE_BORDER = new ConstantColor(CODE_THEME.borderArgb());
    private static final int BODY_PADDING = 6;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int MIN_SCROLLBAR_THUMB = 14;

    private final LytCodeBlockToolbar toolbar = new LytCodeBlockToolbar();
    private final LytViewportBox bodyViewport = new LytViewportBox();
    private final LytParagraph body = new LytParagraph();

    @Getter
    private String codeText = "";

    @Setter
    @Getter
    private boolean toolbarVisible = true;
    private String normalizedCodeText = "";
    @Getter
    private String languageFenceName = "";
    @Getter
    private String languageDisplayName = "Text";
    @Getter
    private String detectedLanguageId = "text";
    @Getter
    private int preferredBodyWidth;
    @Getter
    private int forcedBodyHeight;
    @Getter
    private int bodyScrollOffsetY;
    private final SmoothFloatState visualBodyScrollOffsetY = new SmoothFloatState();
    /** Visual-scroll delta currently baked into the body's bounds (see computePrimitives). */
    private int appliedVisualDeltaY;
    @Getter
    private boolean draggingBody;
    private int dragLastDocumentY;
    @Getter
    private boolean draggingScrollbar;
    private int scrollbarGrabOffsetY;
    private int lastBodyLineCount;
    @Getter
    private CodeHighlightResult highlightResult = new CodeHighlightResult("text", CodeHighlightMode.PLAIN, List.of());
    private List<LytFlowSpan> highlightedLines = List.of();

    public LytCodeBlock() {
        setPadding(6);
        setGap(4);
        setFullWidth(true);
        setBorder(new BorderStyle(CODE_BORDER, 1));

        body.setMarginTop(0);
        body.setMarginBottom(0);
        body.setPaddingLeft(BODY_PADDING);
        body.setPaddingRight(BODY_PADDING);
        body.setPaddingTop(BODY_PADDING);
        body.setPaddingBottom(BODY_PADDING);
        body.modifyStyle(
            style -> style.whiteSpace(WhiteSpaceMode.PRE)
                .color(CODE_DEFAULT));

        bodyViewport.setFullWidth(true);
        bodyViewport.append(body);
        append(toolbar);
        append(bodyViewport);
        syncToolbar();
    }

    @Override
    public List<? extends LytNode> getChildren() {
        // When toolbar is hidden, exclude it from the children list so the
        // Rust layout engine and PrimitiveCollector do not process it.
        // The internal children list (including toolbar) is still maintained
        // for direct field access (render, mouse click, etc.).
        if (!toolbarVisible) {
            return List.of(bodyViewport);
        }
        return super.getChildren();
    }

    public void setCodeText(String codeText) {
        setCodeContent(languageFenceName, codeText);
    }

    public void setLanguageFenceName(String languageFenceName) {
        setCodeContent(languageFenceName, codeText);
    }

    public void setCodeContent(String languageFenceName, String codeText) {
        String resolvedFenceName = languageFenceName != null ? languageFenceName : "";
        String resolvedCodeText = codeText != null ? codeText : "";
        String resolvedNormalizedCodeText = GuideStringLines.normalizeLineEndings(resolvedCodeText);
        boolean changed = !Objects.equals(this.languageFenceName, resolvedFenceName)
            || !Objects.equals(this.codeText, resolvedCodeText);
        this.languageFenceName = resolvedFenceName;
        this.codeText = resolvedCodeText;
        this.normalizedCodeText = resolvedNormalizedCodeText;
        this.lastBodyLineCount = countBodyLines(resolvedNormalizedCodeText);
        toolbar.setCopyText(this.codeText);
        if (changed) {
            rebuildBody();
        }
    }

    public void setLanguageDisplayName(String languageDisplayName) {
        this.languageDisplayName = languageDisplayName != null && !languageDisplayName.isEmpty() ? languageDisplayName
            : "Text";
        syncToolbar();
    }

    public void applyLanguage(CodeBlockLanguage language) {
        if (language == null) {
            detectedLanguageId = "text";
            setLanguageDisplayName("Text");
            return;
        }
        detectedLanguageId = language.id();
        setLanguageDisplayName(language.displayName());
    }

    public void setPreferredBodyWidth(int preferredBodyWidth) {
        this.preferredBodyWidth = Math.max(0, preferredBodyWidth);
        setFullWidth(this.preferredBodyWidth <= 0);
    }

    public void setForcedBodyHeight(int forcedBodyHeight) {
        this.forcedBodyHeight = Math.max(0, forcedBodyHeight);
        // Make the viewport height visible to the Rust layout serializer:
        // propagate the forced height (or -1 for auto) so serialization sees it
        // before Rust measures. Aligns with computeBoxLayout semantics.
        bodyViewport.setExplicitHeight(this.forcedBodyHeight > 0 ? this.forcedBodyHeight : -1);
    }

    public int getBodyLineCount() {
        return lastBodyLineCount;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        // Scrollbar-related interactions are handled by beginDrag/dragTo (mouseDown can start a drag directly).
        if (toolbarVisible && toolbar.mouseClicked(screen, x, y, button, doubleClick)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (button != 0) {
            return false;
        }
        if (toolbarVisible && toolbar.getBounds()
            .contains(documentX, documentY)) {
            return false;
        }
        if (getScrollbarTrackBounds().contains(documentX, documentY)) {
            LytRect thumbBounds = getScrollbarThumbBounds();
            if (!thumbBounds.isEmpty() && thumbBounds.contains(documentX, documentY)) {
                scrollbarGrabOffsetY = documentY - thumbBounds.y();
            } else {
                scrollbarGrabOffsetY = thumbBounds.isEmpty() ? 0 : thumbBounds.height() / 2;
                updateScrollFromMouseY(documentY);
            }
            draggingScrollbar = true;
            return true;
        }
        if (!getBodyViewportBounds().contains(documentX, documentY) || getMaxBodyScroll() <= 0) {
            return false;
        }
        draggingBody = true;
        dragLastDocumentY = documentY;
        return true;
    }

    @Override
    public void dragTo(int documentX, int documentY) {
        if (draggingScrollbar) {
            updateScrollFromMouseY(documentY);
            return;
        }
        if (!draggingBody) {
            return;
        }
        int deltaY = documentY - dragLastDocumentY;
        dragLastDocumentY = documentY;
        setBodyScrollOffset(bodyScrollOffsetY - deltaY);
    }

    @Override
    public void endDrag() {
        draggingBody = false;
        draggingScrollbar = false;
    }

    @Override
    public boolean scroll(int documentX, int documentY, int wheelDelta) {
        if (wheelDelta == 0 || !getBodyViewportBounds().contains(documentX, documentY) || getMaxBodyScroll() <= 0) {
            return false;
        }
        int step = Math.max(12, resolveLineHeight() * 2);
        setBodyScrollOffset(bodyScrollOffsetY - Integer.signum(wheelDelta) * step);
        return true;
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        super.computePrimitives(c);
        c.emit(
            new GuideRenderPrimitive.FillRect(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                CODE_BACKGROUND.resolve(LightDarkMode.current())));

        // Advance the smooth scroll and bake the visual delta into the body's
        // bounds. The collector traverses the body right after this, so the
        // body renders at its animated position and hit-tests stay aligned.
        updateVisualScroll();
        int newDelta = bodyScrollOffsetY - visualBodyScrollOffsetY.rounded();
        if (newDelta != appliedVisualDeltaY && !body.getBounds()
            .isEmpty()) {
            body.moveLayoutPos(0, newDelta - appliedVisualDeltaY);
            appliedVisualDeltaY = newDelta;
        }

        if (getMaxBodyScroll() > 0) {
            LytRect track = getScrollbarTrackBounds();
            if (!track.isEmpty()) {
                c.emit(
                    new GuideRenderPrimitive.FillRect(
                        track.x(),
                        track.y(),
                        track.width(),
                        track.height(),
                        CODE_THEME.scrollbarTrackArgb()));
                LytRect thumb = getScrollbarThumbBounds();
                if (!thumb.isEmpty()) {
                    c.emit(
                        new GuideRenderPrimitive.FillRect(
                            thumb.x(),
                            thumb.y(),
                            thumb.width(),
                            thumb.height(),
                            draggingScrollbar ? CODE_THEME.scrollbarThumbActiveArgb()
                                : CODE_THEME.scrollbarThumbArgb()));
                }
            }
        }
    }

    @Override
    protected void afterExternalLayout() {
        // The writeback reset the body to the unscrolled position; re-apply the
        // current scroll offset and restart the visual-delta bookkeeping.
        updateBodyPosition();
        appliedVisualDeltaY = 0;
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int safeWidth = preferredBodyWidth > 0 ? Math.max(1, Math.min(availableWidth, preferredBodyWidth))
            : Math.max(1, availableWidth);

        int toolbarHeight;
        int bodyY;
        if (toolbarVisible) {
            toolbar.setPreferredWidth(safeWidth);
            LytRect toolbarBounds = toolbar.layout(context, x, y, safeWidth);
            toolbarHeight = toolbarBounds.height() + getGap();
            bodyY = toolbarBounds.bottom() + getGap();
        } else {
            toolbarHeight = 0;
            bodyY = y;
        }
        int bodyAvailableWidth = safeWidth;

        LytRect measuredBody = body.layout(context, x, bodyY, bodyAvailableWidth);
        int contentHeight = measuredBody.height();
        int viewportHeight = forcedBodyHeight > 0 ? forcedBodyHeight : contentHeight;
        if (forcedBodyHeight > 0 && contentHeight > viewportHeight) {
            bodyAvailableWidth = Math.max(1, safeWidth - SCROLLBAR_WIDTH - 4);
            measuredBody = body.layout(context, x, bodyY, bodyAvailableWidth);
            contentHeight = measuredBody.height();
        }

        viewportHeight = forcedBodyHeight > 0 ? forcedBodyHeight : contentHeight;
        bodyViewport.setExplicitHeight(viewportHeight);
        bodyViewport.layout(context, x, bodyY, bodyAvailableWidth);
        setBodyScrollOffset(bodyScrollOffsetY);
        snapVisualScrollToTarget();
        return new LytRect(x, y, safeWidth, toolbarHeight + viewportHeight);
    }

    // ---- derived geometry (computed from current bounds; no layout-time fields) ----

    private int getBodyContentHeight() {
        return body.getBounds()
            .height();
    }

    private LytRect getBodyViewportBounds() {
        LytRect tb = toolbar.getBounds();
        int x = bounds.x() + getBorderLeft().width() + paddingLeft;
        int y = tb.isEmpty() ? bounds.y() + getBorderTop().width() + paddingTop : tb.bottom() + getGap();
        int w = bounds.right() - getBorderRight().width() - paddingRight - x;
        int h;
        if (forcedBodyHeight > 0) {
            h = forcedBodyHeight;
            if (getMaxBodyScroll() > 0) {
                w = Math.max(1, w - SCROLLBAR_WIDTH - 4);
            }
        } else {
            h = getBodyContentHeight();
        }
        return new LytRect(x, y, Math.max(0, w), Math.max(0, h));
    }

    /** Public viewport height accessor (derived; replaces the former layout-time field). */
    public int getBodyViewportHeight() {
        return getBodyViewportBounds().height();
    }

    private int getMaxBodyScroll() {
        if (forcedBodyHeight <= 0) return 0;
        return Math.max(0, getBodyContentHeight() - forcedBodyHeight);
    }

    @Override
    public void render(RenderContext context) {
        updateVisualScroll();
        LytRect ownBounds = getBounds();
        if (ownBounds.isEmpty()) {
            return;
        }
        context.fillRect(ownBounds, CODE_BACKGROUND);

        if (toolbarVisible) {
            toolbar.render(context);
        }

        renderScrollbar(context);
        new BorderRenderer()
            .render(context, ownBounds, getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom());
    }

    private void syncToolbar() {
        toolbar.setLanguageDisplayName(languageDisplayName);
        toolbar.setCopyText(codeText);
    }

    private void rebuildBody() {
        highlightResult = CODE_HIGHLIGHTER.highlight(languageFenceName, normalizedCodeText);
        detectedLanguageId = highlightResult.languageId();
        highlightedLines = FLOW_BUILDER.buildLines(highlightResult);
        body.clearContent();
        for (int i = 0; i < highlightedLines.size(); i++) {
            body.append(highlightedLines.get(i));
            if (i < highlightedLines.size() - 1) {
                body.appendBreak();
            }
        }
    }

    private int countBodyLines(String text) {
        if (text.isEmpty()) {
            return 1;
        }
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    private int resolveLineHeight() {
        return 10;
    }

    private void renderScrollbar(RenderContext context) {
        if (getMaxBodyScroll() <= 0) {
            return;
        }
        LytRect track = getScrollbarTrackBounds();
        if (track.isEmpty()) {
            return;
        }
        context.fillRect(track, CODE_THEME.scrollbarTrackArgb());
        LytRect thumb = getScrollbarThumbBounds();
        if (!thumb.isEmpty()) {
            context.fillRect(
                thumb,
                draggingScrollbar ? CODE_THEME.scrollbarThumbActiveArgb() : CODE_THEME.scrollbarThumbArgb());
        }
    }

    private LytRect getScrollbarTrackBounds() {
        if (getMaxBodyScroll() <= 0) {
            return LytRect.empty();
        }
        LytRect viewport = getBodyViewportBounds();
        int x = viewport.right() + 4;
        return new LytRect(x, viewport.y(), SCROLLBAR_WIDTH, viewport.height());
    }

    private LytRect getScrollbarThumbBounds() {
        LytRect track = getScrollbarTrackBounds();
        if (track.isEmpty()) {
            return LytRect.empty();
        }
        int thumbHeight = Math.max(
            MIN_SCROLLBAR_THUMB,
            track.height() * track.height() / Math.max(track.height(), getBodyContentHeight()));
        thumbHeight = Math.min(thumbHeight, track.height());
        int maxScroll = getMaxBodyScroll();
        int thumbTrack = Math.max(1, track.height() - thumbHeight);
        int thumbY = track.y();
        if (maxScroll > 0) {
            thumbY += (int) ((long) thumbTrack * visualBodyScrollOffsetY.rounded() / maxScroll);
        }
        return new LytRect(track.x(), thumbY, track.width(), thumbHeight);
    }

    private void setBodyScrollOffset(int bodyScrollOffsetY) {
        this.bodyScrollOffsetY = SceneEditorVerticalScrollbar.clamp(bodyScrollOffsetY, 0, getMaxBodyScroll());
        updateBodyPosition();
    }

    private void updateBodyPosition() {
        LytRect viewport = getBodyViewportBounds();
        if (!viewport.isEmpty() && !body.getBounds()
            .isEmpty()) {
            body.moveLayoutPos(
                0,
                viewport.y() - bodyScrollOffsetY
                    - body.getBounds()
                        .y());
            // Bounds now sit at the scroll target; the visual delta restarts
            // from here and is re-baked by computePrimitives each frame.
            appliedVisualDeltaY = 0;
        }
    }

    private void updateScrollFromMouseY(int mouseY) {
        LytRect track = getScrollbarTrackBounds();
        LytRect thumb = getScrollbarThumbBounds();
        if (track.isEmpty() || thumb.isEmpty()) {
            setBodyScrollOffset(0);
            return;
        }
        int thumbTrack = Math.max(1, track.height() - thumb.height());
        int thumbTop = SceneEditorVerticalScrollbar
            .clamp(mouseY - scrollbarGrabOffsetY, track.y(), track.y() + thumbTrack);
        int maxScroll = getMaxBodyScroll();
        setBodyScrollOffset((int) ((long) (thumbTop - track.y()) * maxScroll / thumbTrack));
    }

    private void snapVisualScrollToTarget() {
        visualBodyScrollOffsetY.snapTo(bodyScrollOffsetY);
    }

    private void updateVisualScroll() {
        visualBodyScrollOffsetY
            .updateTowards(bodyScrollOffsetY, 28f, 0.25f, 0.01f, Math.max(128f, getBodyViewportBounds().height() * 2f));
    }
}
