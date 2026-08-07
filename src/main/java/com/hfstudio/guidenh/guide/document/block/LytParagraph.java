package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ConstantColor;
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
import com.hfstudio.guidenh.guide.render.GlyphRunData;
import com.hfstudio.guidenh.guide.render.GlyphRunGroup;
import com.hfstudio.guidenh.guide.render.GlyphRunHolder;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextStyle;

import lombok.Getter;
import lombok.Setter;

public class LytParagraph extends LytBlock implements LytFlowContainer, DebugFlowContainer, GlyphRunHolder {

    private final List<LytFlowContent> flowContent = new ArrayList<>();

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
     * Render through the primitive pipeline when a Rust-shaped glyph run is
     * available. Opaque paragraphs (§k/obfuscated, float-aligned inline blocks)
     * keep legacy HostDraw rendering via {@link #render(RenderContext)}.
     */
    @Override
    public boolean usePrimitives() {
        return !flowContent.isEmpty();
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
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        // Obfuscated (§k) paragraphs: render per-frame random characters via
        // GuideText at the Rust-computed paragraph bounds. Layout geometry
        // comes from Rust (single authority); animation is a rendering concern.
        if (hasObfuscatedStyles(getContent())) {
            emitObfuscatedText(c);
            return;
        }
        if (glyphData != null && !glyphData.runs()
            .isEmpty()) {
            // Span backgrounds (highlight / inline-code) behind the glyphs;
            // underline / strikethrough on top.
            for (GuideRenderPrimitive.FillRect bg : glyphData.backgrounds()) {
                c.emit(bg);
            }
            List<LytFlowContent> spanOwners = hasSpoiler() ? collectSpanOwners() : null;
            List<GlyphRunGroup> runs = glyphData.runs();
            for (int si = 0; si < runs.size(); si++) {
                GlyphRunGroup group = runs.get(si);
                if (spanOwners != null && si < spanOwners.size() && isSpoilerHidden(spanOwners.get(si))) {
                    emitSpoilerMask(c, group);
                } else {
                    c.emit(
                        new GuideRenderPrimitive.DrawGlyphRun(
                            group.glyphs(),
                            group.argb(),
                            group.shear(),
                            resolveStyle().dropShadow()));
                }
            }
            for (GuideRenderPrimitive.FillRect line : glyphData.lines()) {
                c.emit(line);
            }
            // Wavy / dotted decorations (kind 4/5) draw on top, after the
            // plain underline / strikethrough lines.
            for (GuideRenderPrimitive.DrawDecorationLine decoration : glyphData.decorations()) {
                c.emit(decoration);
            }
            return;
        }
        // Fallback: glyph data unavailable — emit text through GuideText so the
        // paragraph renders as visible text instead of silent blank.
        emitTextFallback(c);
    }

    private boolean hasSpoiler() {
        for (LytFlowContent fc : getContent()) {
            if (hasSpoilerIn(fc)) return true;
        }
        return false;
    }

    private static boolean hasSpoilerIn(LytFlowContent fc) {
        if (fc instanceof LytSpoilerSpan) return true;
        if (fc instanceof LytFlowSpan span) {
            for (LytFlowContent child : span.getChildren()) {
                if (hasSpoilerIn(child)) return true;
            }
        }
        return false;
    }

    private boolean isSpoilerHidden(LytFlowContent owner) {
        LytSpoilerSpan spoiler = owner.findAncestor(LytSpoilerSpan.class);
        if (spoiler == null) return false;
        if (owner instanceof LytSpoilerSpan) spoiler = (LytSpoilerSpan) owner;
        boolean hovered = hoveredPath != null && hoveredPath.containsPrimaryOrDescendant(owner);
        boolean revealed = revealedPath != null && revealedPath.containsOrAncestors(owner);
        return !hovered && !revealed;
    }

    private static void emitSpoilerMask(PrimitiveCollector c, GlyphRunGroup group) {
        if (group.glyphs()
            .isEmpty()) return;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = 0, maxY = 0;
        for (var g : group.glyphs()) {
            minX = Math.min(minX, g.x());
            minY = Math.min(minY, g.y());
            maxX = Math.max(maxX, g.x() + g.w());
            maxY = Math.max(maxY, g.y() + g.h());
        }
        c.emit(
            new GuideRenderPrimitive.FillRect(
                Math.round(minX) - 1,
                Math.round(minY) - 1,
                Math.round(maxX - minX) + 2,
                Math.round(maxY - minY) + 2,
                0xFF000000));
    }

    /**
     * Emit obfuscated (§k) text via GuideText with per-frame random characters.
     * Rust provides layout geometry (paragraph bounds); rendering is handled
     * here as a native GuideText call — no LineBuilder dependency.
     */
    private void emitObfuscatedText(PrimitiveCollector c) {
        StringBuilder text = new StringBuilder();
        for (LytFlowContent fc : getContent()) {
            collectObfuscatedText(fc, text);
        }
        if (text.isEmpty()) return;
        StringBuilder random = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                random.append(ch);
            } else {
                random.append(randomObfuscatedChar());
            }
        }
        GuideText.emitText(c, random.toString(), bounds.x(), bounds.y(), resolveStyle());
    }

    private static void collectObfuscatedText(LytFlowContent fc, StringBuilder out) {
        if (fc instanceof LytFlowText ft) {
            out.append(ft.getText());
        } else if (fc instanceof LytFlowInlineBlock) {
            out.append(' '); // placeholder space for inline blocks
        } else if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                collectObfuscatedText(child, out);
            }
        }
    }

    /**
     * Fallback emission: collect all plain text from flow content and emit via
     * GuideText (atlas-backed glyph run). Used when glyphData is null or empty
     * so the paragraph renders visible text instead of silent blank.
     */
    private void emitTextFallback(PrimitiveCollector c) {
        StringBuilder text = new StringBuilder();
        for (LytFlowContent fc : getContent()) {
            collectPlainText(fc, text);
        }
        if (text.isEmpty()) return;
        GuideText.emitText(c, text.toString(), bounds.x(), bounds.y(), resolveStyle());
    }

    /**
     * Collect plain (non-obfuscated) text from a flow-content subtree.
     */
    private static void collectPlainText(LytFlowContent fc, StringBuilder out) {
        if (fc instanceof LytFlowText ft) {
            out.append(ft.getText());
        } else if (fc instanceof LytFlowInlineBlock) {
            out.append(' '); // placeholder space for inline blocks
        } else if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                collectPlainText(child, out);
            }
        }
    }

    private static char randomObfuscatedChar() {
        // Fast per-frame random character from ASCII letters and digits,
        // matching Minecraft's §k visual style.
        long t = System.nanoTime();
        int idx = (int) (t % 62);
        if (idx < 26) return (char) ('A' + idx);
        if (idx < 52) return (char) ('a' + idx - 26);
        return (char) ('0' + idx - 52);
    }

    /**
     * Obfuscated-only detection: {@code §k} content cannot be baked into a
     * static glyph run (per-frame random animation). Spoiler spans are NOT
     * included — they get glyph runs with a render-time overlay.
     */
    public static boolean hasObfuscatedStyles(Iterable<LytFlowContent> content) {
        for (LytFlowContent fc : content) {
            if (hasObfuscatedIn(fc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasObfuscatedIn(LytFlowContent fc) {
        if (fc instanceof LytFlowSpan span) {
            for (LytFlowContent child : span.getChildren()) {
                if (hasObfuscatedIn(child)) {
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
        flowContent.add(child);
        child.setParent(this);
    }

    @Override
    public boolean isCulled(LytRect viewport) {
        return super.isCulled(viewport);
    }

    @Override
    public LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        // Apply padding to paragraph content
        x += paddingLeft;
        availableWidth -= paddingLeft + paddingRight;
        y += paddingTop;

        // Paragraph geometry is Rust's sole authority — all paragraphs skip
        // the expensive LineBuilder pass. Inline block children still need
        // their sizes computed here (the serializer reads them before Rust
        // takes over); positions are assigned later by the Rust inline post-pass.
        for (LytBlock ib : getInlineBlocks()) {
            ib.layout(context, 0, 0, availableWidth);
        }
        int h = paddingTop + paddingBottom + 10; // minimal estimate
        return new LytRect(x - paddingLeft, y - paddingTop, availableWidth, h);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
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
                            g.h(),
                            g.lineIndex()));
                }
                movedGroups.add(new GlyphRunGroup(moved, group.argb(), group.shear()));
            }
            // Decoration rects are absolute document coordinates too — they must
            // follow the same move.
            glyphData = new GlyphRunData(
                movedGroups,
                moveRects(glyphData.backgrounds(), deltaX, deltaY),
                moveRects(glyphData.lines(), deltaX, deltaY),
                moveRects(glyphData.separators(), deltaX, deltaY),
                moveDecorations(glyphData.decorations(), deltaX, deltaY));
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

    private static List<GuideRenderPrimitive.DrawDecorationLine> moveDecorations(
        List<GuideRenderPrimitive.DrawDecorationLine> decorations, int deltaX, int deltaY) {
        if (decorations.isEmpty() || (deltaX == 0 && deltaY == 0)) {
            return decorations;
        }
        List<GuideRenderPrimitive.DrawDecorationLine> moved = new ArrayList<>(decorations.size());
        for (var d : decorations) {
            moved.add(
                new GuideRenderPrimitive.DrawDecorationLine(
                    d.x() + deltaX,
                    d.y() + deltaY,
                    d.w(),
                    d.h(),
                    d.argb(),
                    d.kind()));
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
        return super.pickNode(x, y);
    }

    @Override
    public void render(RenderContext context) {
        // All block-tree rendering goes through computePrimitives (usePrimitives
        // always returns true when content exists). This legacy-path fallback is
        // only reached by direct render() callers outside the document pipeline
        // (tooltip / annotation / editor chains, e.g. ContentTooltip content and
        // TextAnnotation rich content).
        if (flowContent.isEmpty()) return;
        if (glyphData != null && !glyphData.runs()
            .isEmpty()) return;
        StringBuilder text = new StringBuilder();
        for (LytFlowContent fc : getContent()) {
            collectPlainText(fc, text);
        }
        if (!text.isEmpty()) {
            context.drawText(text.toString(), bounds.x(), bounds.y(), resolveStyle());
        }
    }

    @Override
    public @Nullable FlowInteractionPath pickContent(int x, int y) {
        if (glyphData != null && !glyphData.runs()
            .isEmpty()) {
            var hit = pickFromGlyphRuns(x, y);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    @Nullable
    private FlowInteractionPath pickFromGlyphRuns(int x, int y) {
        List<LytFlowContent> spanOwners = collectSpanOwners();
        var runs = glyphData.runs();
        for (int si = 0; si < runs.size(); si++) {
            var group = runs.get(si);
            for (var g : group.glyphs()) {
                if (x >= g.x() && x <= g.x() + g.w() && y >= g.y() && y <= g.y() + g.h()) {
                    LytFlowContent owner = si < spanOwners.size() ? spanOwners.get(si) : null;
                    if (owner != null) {
                        return FlowInteractionPath.fromPrimary(owner);
                    }
                    return null;
                }
            }
        }
        return null;
    }

    private List<LytFlowContent> collectSpanOwners() {
        List<LytFlowContent> owners = new ArrayList<>();
        for (LytFlowContent fc : getContent()) {
            collectSpanOwnersRecursive(fc, owners);
        }
        return owners;
    }

    private static void collectSpanOwnersRecursive(LytFlowContent fc, List<LytFlowContent> out) {
        if (fc instanceof LytFlowText || fc instanceof LytFlowInlineBlock) {
            out.add(fc);
        } else if (fc instanceof LytFlowSpan span) {
            for (LytFlowContent child : span.getChildren()) {
                collectSpanOwnersRecursive(child, out);
            }
        }
    }

    public @Nullable LytRect getFirstTextRunBounds() {
        if (glyphData != null && !glyphData.runs()
            .isEmpty()) {
            return firstLineBoundsFromGlyphs();
        }
        return null;
    }

    @Nullable
    private LytRect firstLineBoundsFromGlyphs() {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = 0, maxY = 0;
        boolean found = false;
        for (GlyphRunGroup group : glyphData.runs()) {
            for (var g : group.glyphs()) {
                if (g.lineIndex() != 0) continue;
                found = true;
                minX = Math.min(minX, g.x());
                minY = Math.min(minY, g.y());
                maxX = Math.max(maxX, g.x() + g.w());
                maxY = Math.max(maxY, g.y() + g.h());
            }
        }
        if (!found) return null;
        return new LytRect(Math.round(minX), Math.round(minY), Math.round(maxX - minX), Math.round(maxY - minY));
    }

    @Override
    public Stream<LytRect> enumerateContentBounds(LytFlowContent content) {
        return Stream.empty();
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
        return flowContent;
    }

    public boolean isEmpty() {
        return flowContent.isEmpty();
    }

    public void clearContent() {
        flowContent.clear();
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
        return null;
    }

    @Override
    public List<FlowContentEntry> getAllFlowContent() {
        return List.of();
    }
}
