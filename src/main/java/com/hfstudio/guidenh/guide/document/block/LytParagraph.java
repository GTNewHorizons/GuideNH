package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContainer;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.flow.LytSpoilerSpan;
import com.hfstudio.guidenh.guide.document.interaction.FlowInteractionPath;
import com.hfstudio.guidenh.guide.internal.debug.DebugFlowContainer;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.flow.FlowBuilder;
import com.hfstudio.guidenh.guide.layout.flow.LineElement;
import com.hfstudio.guidenh.guide.layout.flow.LineTextRun;
import com.hfstudio.guidenh.guide.render.GlyphRunData;
import com.hfstudio.guidenh.guide.render.GlyphRunGroup;
import com.hfstudio.guidenh.guide.render.GlyphRunHolder;
import com.hfstudio.guidenh.guide.render.GuideGlyphAtlas;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextStyle;

import lombok.Getter;
import lombok.Setter;

public class LytParagraph extends LytBlock implements LytFlowContainer, DebugFlowContainer, GlyphRunHolder {

    protected final FlowBuilder content = new FlowBuilder();

    // Rich glyph output from Rust cosmic-text shaping (per-span runs + decorations)
    @Nullable
    private GlyphRunData glyphData;

    @Override
    public void setGlyphData(@Nullable GlyphRunData data) {
        this.glyphData = data;
    }

    @Override
    public @Nullable GlyphRunData getGlyphData() {
        return glyphData;
    }

    /**
     * Render through the primitive pipeline when either a Rust-shaped glyph run
     * is available (mode 1: plain single-style paragraphs, rich multi-style
     * paragraphs and paragraphs with inline blocks) or every span is statically
     * styled (mode 2: per-span {@link GuideText} emission, the fallback when no
     * glyph run was produced). Dynamic-style paragraphs (obfuscated, spoiler,
     * float-aligned inline blocks) keep legacy HostDraw rendering.
     */
    @Override
    public boolean usePrimitives() {
        return (glyphData != null && !glyphData.runs()
            .isEmpty()) || isStaticMultiStyle();
    }

    @Override
    public List<? extends LytNode> getChildren() {
        // Surface inline blocks (icons, formulas, etc. embedded in the flow
        // content) as real tree children: the serializer pairs them with the
        // U+FFFC placeholders in the paragraph text, and the render collector
        // traverses them like any other child.
        return getInlineBlocks();
    }

    /**
     * The inner blocks of this paragraph's {@code LytFlowInlineBlock} wrappers,
     * in document order. Empty for plain-text paragraphs.
     */
    public List<LytBlock> getInlineBlocks() {
        List<LytBlock> out = new ArrayList<>();
        for (LytFlowContent fc : getContent()) {
            collectInlineBlocks(fc, out);
        }
        return out;
    }

    private static void collectInlineBlocks(LytFlowContent fc, List<LytBlock> out) {
        if (fc instanceof LytFlowInlineBlock ib && ib.getBlock() != null) {
            out.add(ib.getBlock());
        } else if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                collectInlineBlocks(child, out);
            }
        }
    }

    @Override
    protected void onExternalLayoutApplied(LytRect oldBounds, LytRect newBounds) {
        // Flow lines are laid out in absolute document coordinates by the Java
        // pass; when the external (Rust) engine re-anchors this paragraph, the
        // flow content must follow — otherwise legacy-rendered paragraphs
        // (opaque leaves, e.g. code block bodies) draw offset from their bounds
        // and clip rects, and hover hit-tests diverge from the glyph quads.
        int dx = newBounds.x() - oldBounds.x();
        int dy = newBounds.y() - oldBounds.y();
        if (dx != 0 || dy != 0) {
            content.move(dx, dy);
        }
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        if (glyphData != null && !glyphData.runs()
            .isEmpty()) {
            // Span backgrounds (highlight / inline-code) behind the glyphs;
            // underline / strikethrough on top.
            for (GuideRenderPrimitive.FillRect bg : glyphData.backgrounds()) {
                c.emit(bg);
            }
            int atlasTex = GuideGlyphAtlas.instance()
                .getTextureId();
            for (GlyphRunGroup group : glyphData.runs()) {
                c.emit(new GuideRenderPrimitive.DrawGlyphRun(atlasTex, group.glyphs(), group.argb(), group.shear()));
            }
            for (GuideRenderPrimitive.FillRect line : glyphData.lines()) {
                c.emit(line);
            }
            return;
        }
        if (isStaticMultiStyle()) {
            emitStaticSpans(c);
        }
    }

    /**
     * Mode 2 rendering: emit each flow {@link LineTextRun} through
     * {@link GuideText} at its flow-layout position, plus the decorations the
     * glyph run cannot express (span backgrounds, underline, strikethrough).
     * Hover styles are intentionally ignored (matches the glyph-run path);
     * wavy/dotted underline and spoiler reveal are not yet implemented here.
     */
    private void emitStaticSpans(PrimitiveCollector c) {
        LightDarkMode mode = LightDarkMode.current();
        content.forEachTextRun(run -> {
            ResolvedTextStyle style = run.style;
            LytRect rect = run.bounds;
            boolean inlineCode = style.inlineCode();
            ColorValue backgroundColor = style.backgroundColor();
            int width = rect.width();
            int height = rect.height();
            if (width > 0 && height > 0) {
                if (inlineCode) {
                    int backgroundArgb = mode == LightDarkMode.DARK_MODE ? LineTextRun.INLINE_CODE_BACKGROUND_DARK
                        : LineTextRun.INLINE_CODE_BACKGROUND_LIGHT;
                    c.emit(new GuideRenderPrimitive.FillRect(rect.x(), rect.y() - 1, width, height, backgroundArgb));
                } else if (backgroundColor != null) {
                    c.emit(
                        new GuideRenderPrimitive.FillRect(
                            rect.x() - 1,
                            rect.y() - 1,
                            width + 2,
                            height,
                            backgroundColor.resolve(mode)));
                }
            }
            int textX = inlineCode ? rect.x() + LineTextRun.INLINE_CODE_PAD_X : rect.x();
            GuideText.emitText(c, run.text, textX, rect.y(), style);
            int textColor = GuideText.resolveColor(style);
            if (style.underlined()) {
                c.emit(
                    new GuideRenderPrimitive.FillRect(
                        rect.x(),
                        rect.y() + GuideText.lineHeight(style) - 1,
                        width,
                        1,
                        textColor));
            }
            if (style.strikethrough()) {
                c.emit(
                    new GuideRenderPrimitive.FillRect(
                        rect.x(),
                        rect.y() + GuideText.lineHeight(style) / 2,
                        width,
                        1,
                        textColor));
            }
        });
    }

    /**
     * Mode 2 eligibility: no Rust glyph run (opaque leaf — guaranteed by the
     * call sites), no dynamic styles, no inline blocks, and either multiple
     * distinct resolved styles or any color/decoration the single-style glyph
     * run would drop. {@code LayoutNodeSerializer.isOpaqueText} uses the
     * narrower {@link #hasMultipleStylesOrDecorations} view of the same
     * traversal so plain single-color paragraphs keep the glyph-run path,
     * while already-opaque single-color paragraphs (PRE_WRAP code bodies)
     * still render per-span here.
     */
    private boolean isStaticMultiStyle() {
        return isStaticMultiStyleContent(getContent());
    }

    /**
     * Static multi-style classification over a flow content tree. Returns
     * {@code false} for dynamic content (spoiler spans, {@code §k}/obfuscated
     * styles, float-aligned inline blocks — these keep legacy rendering) and
     * for paragraphs containing inline blocks (those keep the Rust glyph run +
     * inline post-pass). Hover styles do not count as dynamic.
     */
    public static boolean isStaticMultiStyleContent(Iterable<LytFlowContent> content) {
        return classifyStaticContent(content, true);
    }

    /**
     * Serializer-facing variant of {@link #isStaticMultiStyleContent}: counts
     * only what the single-style glyph run structurally cannot express
     * (multiple resolved styles, or any decoration). A single styled color is
     * deliberately ignored here so plain paragraphs keep the glyph-run path;
     * opaque single-color paragraphs (e.g. PRE_WRAP code bodies) still render
     * per-span via mode 2, which applies the full color check.
     */
    public static boolean hasMultipleStylesOrDecorations(Iterable<LytFlowContent> content) {
        return classifyStaticContent(content, false);
    }

    private static boolean classifyStaticContent(Iterable<LytFlowContent> content, boolean includeColor) {
        Set<ResolvedTextStyle> styles = new HashSet<>();
        for (LytFlowContent fc : content) {
            if (!collectStaticLeafStyles(fc, styles)) {
                return false;
            }
        }
        if (styles.size() >= 2) {
            return true;
        }
        for (ResolvedTextStyle style : styles) {
            if (hasDecoration(style)) {
                return true;
            }
            if (includeColor && style.color() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Depth-first leaf style collection. Returns {@code false} when the subtree
     * contains dynamic content or an inline block (any alignment), which
     * disqualifies the whole paragraph from mode 2.
     */
    private static boolean collectStaticLeafStyles(LytFlowContent fc, Set<ResolvedTextStyle> out) {
        if (fc instanceof LytSpoilerSpan) {
            return false; // spoiler reveal is dynamic
        }
        if (fc instanceof LytFlowInlineBlock) {
            return false; // inline blocks keep the glyph-run / legacy paths
        }
        if (fc instanceof LytFlowSpan span) {
            for (LytFlowContent child : span.getChildren()) {
                if (!collectStaticLeafStyles(child, out)) {
                    return false;
                }
            }
            return true;
        }
        if (fc instanceof LytFlowText text) {
            if (text.getText()
                .contains("§k")) {
                return false; // obfuscated runs are dynamic
            }
            ResolvedTextStyle style = fc.resolveStyle();
            if (style.obfuscated()) {
                return false;
            }
            out.add(style);
        }
        return true;
    }

    /**
     * Dynamic-style detection for the serializer: content the baked-at-layout
     * glyph run cannot express (spoiler reveal, {@code §k}/obfuscated). Such
     * paragraphs must stay opaque (legacy rendering) — a glyph run would render
     * spoilers in plain text and draw {@code §k} literally. Hover styles do not
     * count as dynamic.
     */
    public static boolean hasDynamicStyles(Iterable<LytFlowContent> content) {
        for (LytFlowContent fc : content) {
            if (hasDynamicStylesIn(fc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDynamicStylesIn(LytFlowContent fc) {
        if (fc instanceof LytSpoilerSpan) {
            return true;
        }
        if (fc instanceof LytFlowSpan span) {
            for (LytFlowContent child : span.getChildren()) {
                if (hasDynamicStylesIn(child)) {
                    return true;
                }
            }
        }
        if (fc instanceof LytFlowText text) {
            if (text.getText()
                .contains("§k")) {
                return true;
            }
            if (fc.resolveStyle()
                .obfuscated()) {
                return true;
            }
        }
        return false;
    }

    /** Decorations the single-style glyph run cannot express. */
    private static boolean hasDecoration(ResolvedTextStyle style) {
        return style.underlined() || style.strikethrough()
            || style.wavyUnderline()
            || style.dottedUnderline()
            || style.backgroundColor() != null
            || style.inlineCode();
    }

    @Getter
    @Setter
    protected int paddingLeft;
    @Getter
    @Setter
    protected int paddingTop;
    @Getter
    @Setter
    protected int paddingRight;
    @Getter
    @Setter
    protected int paddingBottom;

    @Nullable
    protected FlowInteractionPath hoveredPath = FlowInteractionPath.empty();
    @Nullable
    protected FlowInteractionPath revealedPath = FlowInteractionPath.empty();

    @Override
    public void append(LytFlowContent child) {
        content.append(child);
        child.setParent(this);
    }

    @Override
    public boolean isCulled(LytRect viewport) {
        // If we have floating content, account for its bounding box exceeding our content box
        if (content.floatsIntersect(viewport)) {
            return false;
        }

        return super.isCulled(viewport);
    }

    @Override
    public LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        // Apply padding to paragraph content
        x += paddingLeft;
        availableWidth -= paddingLeft + paddingRight;
        y += paddingTop;

        var style = resolveStyle();

        var bounds = content.computeLayout(context, x, y, availableWidth, style.alignment());

        if (paddingBottom != 0) {
            return bounds.withHeight(bounds.height() + paddingBottom);
        }
        return bounds;
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        content.move(deltaX, deltaY);
        // The Rust-baked glyph run uses absolute document coordinates — it must
        // follow the paragraph's bounds (scroll replay, smooth scrolling) or the
        // text detaches from the paragraph's background/clip/hover geometry.
        if (glyphData != null && !glyphData.runs()
            .isEmpty()) {
            List<GlyphRunGroup> movedGroups = new ArrayList<>(
                glyphData.runs()
                    .size());
            for (GlyphRunGroup group : glyphData.runs()) {
                List<GuideRenderPrimitive.PlacedGlyph> moved = new ArrayList<>(
                    group.glyphs()
                        .size());
                for (var g : group.glyphs()) {
                    moved.add(
                        new GuideRenderPrimitive.PlacedGlyph(
                            g.atlasKey(),
                            g.x() + deltaX,
                            g.y() + deltaY,
                            g.w(),
                            g.h()));
                }
                movedGroups.add(new GlyphRunGroup(moved, group.argb(), group.shear()));
            }
            // Decoration rects are absolute document coordinates too — they must
            // follow the same move.
            glyphData = new GlyphRunData(
                movedGroups,
                moveRects(glyphData.backgrounds(), deltaX, deltaY),
                moveRects(glyphData.lines(), deltaX, deltaY));
        }
    }

    private static List<GuideRenderPrimitive.FillRect> moveRects(List<GuideRenderPrimitive.FillRect> rects, int deltaX,
        int deltaY) {
        if (rects.isEmpty() || (deltaX == 0 && deltaY == 0)) {
            return rects;
        }
        List<GuideRenderPrimitive.FillRect> moved = new ArrayList<>(rects.size());
        for (var r : rects) {
            moved.add(new GuideRenderPrimitive.FillRect(r.x() + deltaX, r.y() + deltaY, r.w(), r.h(), r.argb()));
        }
        return moved;
    }

    @Override
    public void onMouseEnter(@Nullable LytFlowContent hoveredContent) {
        super.onMouseEnter(hoveredContent);
        this.hoveredPath = FlowInteractionPath.fromPrimary(hoveredContent);
    }

    public void setInteractionPaths(@Nullable FlowInteractionPath hoveredPath,
        @Nullable FlowInteractionPath revealedPath) {
        this.hoveredPath = hoveredPath != null ? hoveredPath : FlowInteractionPath.empty();
        this.revealedPath = revealedPath != null ? revealedPath : FlowInteractionPath.empty();
    }

    @Override
    public void onMouseLeave() {
        super.onMouseLeave();
        this.hoveredPath = FlowInteractionPath.empty();
        this.revealedPath = FlowInteractionPath.empty();
    }

    @Override
    public @Nullable LytNode pickNode(int x, int y) {
        // If we are the host for any floating elements, those can exceed our own bounds
        var fl = content.pickFloatingElement(x, y);
        if (fl != null) {
            return this;
        }

        return super.pickNode(x, y);
    }

    @Override
    public void render(RenderContext context) {
        // Since we overwrite isCulled, we render even if our actual line content is culled, for floats
        if (context.intersectsViewport(bounds)) {
            content.render(context, hoveredPath, revealedPath);
        }

        content.renderFloats(context, hoveredPath, revealedPath);
    }

    @Override
    public @Nullable FlowInteractionPath pickContent(int x, int y) {
        return content.pickPath(x, y);
    }

    public @Nullable LytRect getFirstLineBounds() {
        return content.getFirstLineBounds();
    }

    public @Nullable LytRect getFirstTextRunBounds() {
        return content.getFirstTextRunBounds();
    }

    @Override
    public Stream<LytRect> enumerateContentBounds(LytFlowContent content) {
        return this.content.enumerateContentBounds(content);
    }

    @Override
    protected LytVisitor.Result visitChildren(LytVisitor visitor, boolean includeOutOfTreeContent) {
        if (super.visitChildren(visitor, includeOutOfTreeContent) == LytVisitor.Result.STOP) {
            return LytVisitor.Result.STOP;
        }

        for (var flowContent : getContent()) {
            flowContent.visit(visitor);
        }

        return LytVisitor.Result.CONTINUE;
    }

    public Iterable<LytFlowContent> getContent() {
        return content.getContent();
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public void clearContent() {
        content.clear();
    }

    /**
     * Quick shorthand to create a paragrpah of plain text.
     */
    public static LytParagraph of(String text) {
        var paragraph = new LytParagraph();
        paragraph.appendText(text);
        return paragraph;
    }

    /**
     * The text style used for loading placeholders: gray, italic, obfuscated.
     */
    public static final TextStyle LOADING_STYLE = TextStyle.builder()
        .italic(true)
        .obfuscated(true)
        .color(new ConstantColor(0xFF808080))
        .build();

    /**
     * Creates a placeholder paragraph with distinctive "loading" visual style
     * (gray, italic, obfuscated text) so pending materialization is obvious.
     */
    public static LytParagraph loading(String text) {
        var paragraph = new LytParagraph();
        paragraph.setStyle(LOADING_STYLE);
        paragraph.appendText(text);
        return paragraph;
    }

    /** Warm amber-yellow italic text for placeholder blocks awaiting async materialization. */
    public static final TextStyle PLACEHOLDER_STYLE = TextStyle.builder()
        .italic(true)
        .color(new ConstantColor(0xFFE8A317))
        .build();

    /** Red text style for inline error messages. */
    public static final TextStyle ERROR_STYLE = TextStyle.builder()
        .color(SymbolicColor.ERROR_TEXT)
        .build();

    /** Creates a placeholder paragraph (amber, italic) for deferred content. */
    public static LytParagraph placeholder(String text) {
        var paragraph = new LytParagraph();
        paragraph.setStyle(PLACEHOLDER_STYLE);
        paragraph.appendText(text);
        return paragraph;
    }

    /** Creates an error paragraph (red text) for inline error reporting. */
    public static LytParagraph error(String text) {
        var paragraph = new LytParagraph();
        paragraph.setStyle(ERROR_STYLE);
        paragraph.appendText(text);
        return paragraph;
    }

    // Debug implementation

    @Override
    @Nullable
    public FlowContentEntry pickFlowContent(int x, int y) {
        LineElement element = content.pick(x, y);
        if (element != null) {
            return new FlowContentEntry(element.getFlowContent(), element.bounds);
        }
        return null;
    }

    @Override
    public List<FlowContentEntry> getAllFlowContent() {
        List<FlowContentEntry> entries = new ArrayList<>();
        for (LytFlowContent flowContent : getContent()) {
            content.enumerateContentBounds(flowContent)
                .forEach(bounds -> entries.add(new FlowContentEntry(flowContent, bounds)));
        }
        return entries;
    }
}
