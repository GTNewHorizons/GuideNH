package com.hfstudio.guidenh.guide.layout.flow;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.flow.InlineBlockAlignment;
import com.hfstudio.guidenh.guide.document.flow.LytFlowAnchor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowBreak;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.flow.LytSpoilerSpan;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideFontCompat;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.TextStyle;

/**
 * Does inline-flow layout similar to how it is described here:
 * <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Flow_Layout/Block_and_Inline_Layout_in_Normal_Flow">...</a>
 */
public class LineBuilder implements Consumer<LytFlowContent> {

    private static final ThreadLocal<BreakIterator> LINE_BREAK_ITERATOR = ThreadLocal
        .withInitial(BreakIterator::getLineInstance);
    private static final ConstantColor SPOILER_MASK_COLOR = new ConstantColor(ColorUtils.BLACK.getColor());
    public static final char WORD_BREAK_OPPORTUNITY = '\u200B';

    private final LayoutContext context;
    private final List<Line> lines;
    // Contains any floating elements we construct as part of processing flow content
    private final List<LineBlock> floats;
    private final int lineBoxX;
    private final int startY;
    private int innerX;
    private int lineBoxY;
    private final int lineBoxWidth;
    private int remainingLineWidth;
    @Nullable
    private LineElement openLineElement;
    @Nullable
    private LineElement openLineTail;
    private final TextAlignment alignment;
    private final StringBuilder lineBuffer = new StringBuilder();
    private float[] lineBufferPrefixWidths = new float[64];

    /** Reusable CharacterIterator wrapping {@link #lineBuffer} to avoid String allocation in BreakIterator. */
    private final StringBuilderCharIterator charIterator = new StringBuilderCharIterator();

    /**
     * Incremental min-top, max-bottom, and max-right of elements on the current open line.
     * Updated by {@link #appendToOpenLine} and reset by {@link #endLine}.
     * Eliminates the O(N) scan in {@link #endLine} for line-height and line-width.
     */
    private int openLineMinTop = Integer.MAX_VALUE;
    private int openLineMaxBottom;
    private int openLineMaxRight;

    /**
     * Whether the open line ends on a {@link #WORD_BREAK_OPPORTUNITY}, so the next text may wrap before its
     * first character. Needed because {@code <wbr>} becomes a text node of its own.
     */
    private boolean openLineEndsWithBreakOpportunity;

    public LineBuilder(LayoutContext context, int x, int y, int availableWidth, List<Line> lines,
        List<LineBlock> floats, TextAlignment alignment) {
        this.floats = floats;
        this.alignment = alignment;
        this.context = context;
        this.startY = y;
        lineBoxX = x;
        lineBoxY = y;
        lineBoxWidth = availableWidth;
        remainingLineWidth = getAvailableHorizontalSpace();
        this.lines = lines;
    }

    @Override
    public void accept(LytFlowContent content) {
        if (content instanceof LytFlowText text) {
            appendText(text.getText(), content);
        } else if (content instanceof LytFlowBreak) {
            appendBreak(content);
        } else if (content instanceof LytFlowInlineBlock inlineBlock) {
            appendInlineBlock(inlineBlock);
        } else if (content instanceof LytFlowAnchor anchor) {
            // Simply set the current layout position for the anchor
            anchor.setLayoutY(lineBoxY);
        } else {
            throw new IllegalArgumentException("Don't know how to layout flow content: " + content);
        }
    }

    private void appendBreak(@Nullable LytFlowContent flowContent) {
        // Append an empty line with the default style
        if (openLineElement == null) {
            var emptyLine = new LineTextRun(
                "",
                DefaultStyles.BASE_STYLE,
                DefaultStyles.BASE_STYLE,
                DefaultStyles.BASE_STYLE);
            emptyLine.flowContent = flowContent;
            emptyLine.bounds = new LytRect(
                0,
                -context.getAscent(DefaultStyles.BASE_STYLE),
                0,
                context.getLineHeight(DefaultStyles.BASE_STYLE));
            appendToOpenLine(emptyLine);
        }
        endLine();

        // Clear floats, if requested
        if (flowContent instanceof LytFlowBreak flowBreak) {
            if (flowBreak.isClearLeft() || flowBreak.isClearRight()) {
                context.clearFloats(flowBreak.isClearLeft(), flowBreak.isClearRight())
                    .ifPresent(floatBottom -> lineBoxY = Math.max(lineBoxY, floatBottom));
            }
        }
    }

    private void appendInlineBlock(LytFlowInlineBlock inlineBlock) {
        var layoutBounds = inlineBlock.getPreferredBounds(context, lineBoxWidth);
        var size = layoutBounds.size();
        var block = inlineBlock.getBlock();
        var marginLeft = block.getMarginLeft();
        var marginRight = block.getMarginRight();
        var marginBottom = block.getMarginBottom();

        // Is there enough space to have this element here?
        var outerWidth = size.width() + marginLeft + marginRight;
        boolean floating = inlineBlock.getAlignment() == InlineBlockAlignment.FLOAT_LEFT
            || inlineBlock.getAlignment() == InlineBlockAlignment.FLOAT_RIGHT;
        if (!floating && outerWidth > remainingLineWidth && isInlineImage(block) && !openLineEndsWithBreakOpportunity) {
            moveGluedTailToNextLine(false);
        }
        ensureSpaceIsAvailable(outerWidth);

        var el = new LineBlock(block);
        el.setLayoutOffset(layoutBounds.x(), layoutBounds.y());
        el.bounds = new LytRect(innerX + marginLeft + layoutBounds.x(), layoutBounds.y(), size.width(), size.height());
        el.flowContent = inlineBlock;

        if (inlineBlock.getAlignment() == InlineBlockAlignment.FLOAT_LEFT) {
            // Float it to the left of the actual text content.
            // endLine will take care of moving any existing text in the line
            el.bounds = el.bounds.withX(getInnerLeftEdge() + marginLeft + layoutBounds.x())
                .withY(lineBoxY + layoutBounds.y());
            // Update the layout of the contained block to update its absolute position
            el.layoutBlock(context, size.width());
            el.floating = true;
            context.addLeftFloat(el.bounds.expand(0, 0, marginRight, marginBottom));
            floats.add(el);
            remainingLineWidth -= outerWidth;
        } else if (inlineBlock.getAlignment() == InlineBlockAlignment.FLOAT_RIGHT) {
            // Float it to the right the actual text content.
            el.bounds = el.bounds.withX(getInnerRightEdge() - el.bounds.width() + marginRight + layoutBounds.x())
                .withY(lineBoxY + layoutBounds.y());
            // Update the layout of the contained block to update its absolute position
            el.layoutBlock(context, size.width());
            el.floating = true;
            context.addRightFloat(el.bounds.expand(marginLeft, 0, 0, marginBottom));
            floats.add(el);
            remainingLineWidth -= outerWidth;
        } else {
            // Treat as a normal inline element for positioning
            var style = inlineBlock.resolveStyle();
            int ascent = context.getAscent(style);
            int textHeight = context.getLineHeight(style);
            int top = layoutBounds.y() - ascent;
            if (isInlineImage(el)) {
                top = -ascent + Math.floorDiv(textHeight - size.height(), 2) + getInlineImageYOffset(el);
            }
            el.bounds = el.bounds.withY(top);
            innerX += size.width();
            appendToOpenLine(el);
            openLineEndsWithBreakOpportunity = false;
            includeVerticalBounds(-ascent, context.getDescent(style));

            // Since no margin is actually accounted for here, the remaining line width should just
            // be reduced
            remainingLineWidth -= size.width();
        }
    }

    private void ensureSpaceIsAvailable(int width) {
        if (width <= remainingLineWidth) {
            return; // Got enough
        }

        // First, try closing out any open line if we don't have enough space
        endLine();

        if (width <= remainingLineWidth) {
            return; // We got enough by ending the current line and advancing to the next
        }

        // If we *still* don't have enough room, we need to advance down to clear floats
        // as long as any float is still open
        var nextFloatEdge = context.getNextFloatBottomEdge(lineBoxY, lineBoxX, lineBoxX + lineBoxWidth);
        while (nextFloatEdge.isPresent()) {
            lineBoxY = nextFloatEdge.getAsInt();
            context.clearFloatsAbove(lineBoxY);
            remainingLineWidth = getAvailableHorizontalSpace();
            if (width <= remainingLineWidth) {
                break; // Finally, we're good!
            }
            nextFloatEdge = context.getNextFloatBottomEdge(lineBoxY, lineBoxX, lineBoxX + lineBoxWidth);
        }
    }

    @Nullable
    private LineElement getEndOfOpenLine() {
        return openLineTail;
    }

    private void appendText(String text, LytFlowContent flowContent) {
        String layoutText = GuideFontCompat.preprocessText(text);
        var resolvedStyle = flowContent.resolveStyle();
        var resolvedHoverStyle = flowContent.resolveHoverStyle(resolvedStyle);
        var spoiler = flowContent.findAncestor(LytSpoilerSpan.class);
        ResolvedTextStyle style = resolvedStyle;
        ResolvedTextStyle revealStyle = resolvedStyle;
        ResolvedTextStyle hoverStyle = resolvedHoverStyle;
        if (spoiler != null) {
            style = applySpoilerHiddenStyle(resolvedStyle);
            revealStyle = applySpoilerRevealStyle(resolvedStyle);
            hoverStyle = applySpoilerRevealStyle(resolvedHoverStyle);
        }
        final var finalStyle = style;
        final var finalRevealStyle = revealStyle;
        final var finalHoverStyle = hoverStyle;
        final boolean inlineCode = finalStyle.inlineCode();
        final int textHeight = context.getLineHeight(finalStyle);
        final int textAscent = context.getAscent(finalStyle);

        char lastChar = '\0';
        var endOfOpenLine = getEndOfOpenLine();
        if (endOfOpenLine == null || endOfOpenLine.floating) {
            // Treat the first text in a line or text directly after a float as if it was after a line-break.
            lastChar = '\n';
        } else if (openLineEndsWithBreakOpportunity) {
            lastChar = WORD_BREAK_OPPORTUNITY;
        } else if (endOfOpenLine instanceof LineTextRun textRun && !textRun.text.isEmpty()) {
            lastChar = findLastVisibleChar(textRun.text);
        }

        boolean endsWithBreakOpportunity = iterateRuns(layoutText, finalStyle, lastChar, (run, width, endLine) -> {
            if (!run.isEmpty()) {
                var el = new LineTextRun(run.toString(), finalStyle, finalRevealStyle, finalHoverStyle);
                el.flowContent = flowContent;
                int w = Math.round(width);
                if (inlineCode) {
                    w += LineTextRun.INLINE_CODE_EXTRA_WIDTH;
                }
                el.bounds = new LytRect(innerX, -textAscent, w, textHeight);
                appendToOpenLine(el);
                innerX += w;
                remainingLineWidth -= w;
            }
            if (endLine) {
                endLine();
            }
        });
        openLineEndsWithBreakOpportunity = endsWithBreakOpportunity && openLineElement != null;
    }

    private ResolvedTextStyle applySpoilerHiddenStyle(ResolvedTextStyle style) {
        return TextStyle.builder()
            .backgroundColor(SPOILER_MASK_COLOR)
            .color(SPOILER_MASK_COLOR)
            .underlined(false)
            .wavyUnderline(false)
            .dottedUnderline(false)
            .strikethrough(false)
            .build()
            .mergeWith(style);
    }

    private ResolvedTextStyle applySpoilerRevealStyle(ResolvedTextStyle style) {
        return TextStyle.builder()
            .backgroundColor(SPOILER_MASK_COLOR)
            .build()
            .mergeWith(style);
    }

    /**
     * @return whether the text ends on a {@link #WORD_BREAK_OPPORTUNITY} that no later character followed
     */
    private boolean iterateRuns(CharSequence text, ResolvedTextStyle style, char lastChar, LineConsumer consumer) {
        float curLineWidth = 0;

        lineBuffer.setLength(0);
        lineBufferPrefixWidths[0] = 0f;

        // Hoist whitespace mode flags out of the per-character loop to avoid repeated method calls.
        boolean collapseSegmentBreaks = style.whiteSpace()
            .isCollapseSegmentBreaks();
        boolean collapseWhitespace = style.whiteSpace()
            .isCollapseWhitespace();

        boolean lastCharWasWhitespace = Character.isWhitespace(lastChar);
        boolean canBreakAtStart = lastCharWasWhitespace || lastChar == WORD_BREAK_OPPORTUNITY;
        // Line-buffer offset of the latest explicit break opportunity, or -1 if there is none.
        int wordBreakOffset = -1;
        boolean bold = style.bold();
        boolean visibleGlyphSeen = false;

        for (var i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int codePoint = ch;

            if (ch == WORD_BREAK_OPPORTUNITY) {
                wordBreakOffset = lineBuffer.length();
                continue;
            }

            if (GuideFontCompat.isFormattingCodeStart(text, i)) {
                appendCharToLineBuffer(ch, 0f);
                char formatChar = text.charAt(++i);
                appendCharToLineBuffer(formatChar, 0f);
                bold = GuideFontCompat.determineBold(bold, formatChar);
                continue;
            }

            // UTF-16 surrogate handling
            if (Character.isHighSurrogate(ch) && i + 1 < text.length()) {
                // Always consume the next char if it's a low surrogate
                char low = text.charAt(i + 1);
                if (Character.isLowSurrogate(low)) {
                    i++; // Skip the low surrogate
                    codePoint = Character.toCodePoint(ch, low);
                }
            }

            // Handle explicit line breaks
            if (codePoint == '\n') {
                if (collapseSegmentBreaks) {
                    codePoint = ' ';
                } else {
                    consumer.visitRun(lineBuffer, curLineWidth, true);
                    lineBuffer.setLength(0);
                    lineBufferPrefixWidths[0] = 0f;
                    wordBreakOffset = -1;
                    lastCharWasWhitespace = true;
                    remainingLineWidth = getAvailableHorizontalSpace();
                    continue;
                }
            }

            if (Character.isWhitespace(codePoint)) {
                // Skip if the last one was a space already
                if (lastCharWasWhitespace && collapseWhitespace) {
                    continue; // White space collapsing
                }
                lastCharWasWhitespace = true;
            } else {
                lastCharWasWhitespace = false;
            }

            var advance = context.getRenderedAdvance(codePoint, style, visibleGlyphSeen);
            // Break line if necessary
            if (curLineWidth + advance > remainingLineWidth) {
                int precedingBreakOpportunity;

                // BreakIterator will only ever break *AFTER* whitespace, but since we ignore the last break opportunity
                // we need to also ignore that...
                if (lastCharWasWhitespace) {
                    precedingBreakOpportunity = lineBuffer.length();
                } else {
                    // Find break opportunities and include the current character in it.
                    // Append the char temporarily to avoid a string-concatenation allocation.
                    var breakIterator = LINE_BREAK_ITERATOR.get();
                    lineBuffer.append((char) codePoint);
                    charIterator.reset(lineBuffer);
                    breakIterator.setText(charIterator);
                    precedingBreakOpportunity = breakIterator.preceding(lineBuffer.length());
                    lineBuffer.setLength(lineBuffer.length() - 1);
                    // An explicit <wbr> later in the word wins over the last natural opportunity.
                    precedingBreakOpportunity = Math.max(precedingBreakOpportunity, wordBreakOffset);
                }

                // If the preceding text chunk ended on a whitespace, we can break there if the
                // current word does not offer us any opportunity to.
                boolean lineBufferHasVisibleGlyph = containsVisibleGlyph(lineBuffer);
                if (lineBufferHasVisibleGlyph && (precedingBreakOpportunity > 0
                    || precedingBreakOpportunity == 0 && (canBreakAtStart || wordBreakOffset == 0))) {
                    // Determine width up until the break opportunity.
                    float widthAtBreakOpportunity = lineBufferPrefixWidths[precedingBreakOpportunity];

                    consumer
                        .visitRun(lineBuffer.subSequence(0, precedingBreakOpportunity), widthAtBreakOpportunity, true);
                    deleteLineBufferPrefix(precedingBreakOpportunity);
                    int removedChars = precedingBreakOpportunity;
                    if (!lineBuffer.isEmpty() && Character.isWhitespace(lineBuffer.charAt(0))) {
                        deleteLineBufferPrefix(1);
                        removedChars++;
                    }
                    // An opportunity at or before the break has been used; one beyond it still applies.
                    wordBreakOffset = wordBreakOffset > removedChars ? wordBreakOffset - removedChars : -1;
                    curLineWidth = rebuildLineBufferWidths(style);
                    bold = resolveTrailingBold(style, lineBuffer);
                    visibleGlyphSeen = containsVisibleGlyph(lineBuffer);
                } else if (!lineBufferHasVisibleGlyph) {
                    if (lastCharWasWhitespace) {
                        continue;
                    }

                    if (!canBreakAtStart && wordBreakOffset != 0) {
                        // Text glued to an inline image (e.g. punctuation after an item link) takes it along
                        moveGluedTailToNextLine(true);
                    }
                    ensureTextSpaceIsAvailable(advance);

                    // If no wider lane exists (for example, a genuinely narrow table cell), keep
                    // the glyph in the line rather than emitting an empty line and dropping it.
                    curLineWidth += advance;
                    appendCodePointToLineBuffer(codePoint, advance);
                    visibleGlyphSeen = visibleGlyphSeen || advance > 0f;
                    continue;
                } else {
                    // The word touches an inline image at the end of the line (e.g. an item link's icon):
                    // wrap them together instead of splitting the word.
                    if (moveGluedTailToNextLine(true) && curLineWidth + advance <= remainingLineWidth) {
                        curLineWidth += advance;
                        appendCodePointToLineBuffer(codePoint, advance);
                        visibleGlyphSeen = visibleGlyphSeen || advance > 0f;
                        continue;
                    }

                    // We exceeded the line length, but did not find a break opportunity
                    // this causes a forced break mid-word
                    consumer.visitRun(lineBuffer, curLineWidth, true);
                    lineBuffer.setLength(0);
                    lineBufferPrefixWidths[0] = 0f;
                    wordBreakOffset = -1;
                    curLineWidth = 0;
                    bold = style.bold();
                    visibleGlyphSeen = false;
                }
                remainingLineWidth = getAvailableHorizontalSpace();
                // If a white-space character broke the line, ignore it as it
                // would otherwise be at the start of the next line
                if (lastCharWasWhitespace) {
                    continue;
                }
            }
            curLineWidth += advance;
            appendCodePointToLineBuffer(codePoint, advance);
            if (advance > 0f) {
                visibleGlyphSeen = true;
            }
        }

        boolean endsWithBreakOpportunity = wordBreakOffset >= 0 && wordBreakOffset == lineBuffer.length();
        if (!lineBuffer.isEmpty()) {
            consumer.visitRun(lineBuffer, curLineWidth, false);
        }
        return endsWithBreakOpportunity;
    }

    private void ensureTextSpaceIsAvailable(float requiredWidth) {
        if (requiredWidth <= remainingLineWidth) {
            return;
        }

        if (openLineElement != null) {
            endLine();
        }

        var nextFloatBottom = context.getNextFloatBottomEdge(lineBoxY, lineBoxX, lineBoxX + lineBoxWidth);
        while (requiredWidth > remainingLineWidth && nextFloatBottom.isPresent()) {
            int nextY = nextFloatBottom.getAsInt();
            if (nextY <= lineBoxY) {
                break;
            }
            lineBoxY = nextY;
            context.clearFloatsAbove(lineBoxY);
            remainingLineWidth = getAvailableHorizontalSpace();
            nextFloatBottom = context.getNextFloatBottomEdge(lineBoxY, lineBoxX, lineBoxX + lineBoxWidth);
        }
    }

    private void appendCodePointToLineBuffer(int codePoint, float advance) {
        int previousLength = lineBuffer.length();
        float bufferWidth = lineBufferPrefixWidths[previousLength] + advance;
        lineBuffer.appendCodePoint(codePoint);
        int newLength = lineBuffer.length();
        ensureLineBufferPrefixCapacity(newLength + 1);
        for (int i = previousLength + 1; i <= newLength; i++) {
            lineBufferPrefixWidths[i] = bufferWidth;
        }
    }

    private void appendCharToLineBuffer(char character, float advance) {
        int previousLength = lineBuffer.length();
        float bufferWidth = lineBufferPrefixWidths[previousLength] + advance;
        lineBuffer.append(character);
        ensureLineBufferPrefixCapacity(previousLength + 2);
        lineBufferPrefixWidths[previousLength + 1] = bufferWidth;
    }

    private float deleteLineBufferPrefix(int charCount) {
        if (charCount <= 0) {
            return 0f;
        }
        float removedWidth = lineBufferPrefixWidths[charCount];
        int remainingChars = lineBuffer.length() - charCount;
        if (remainingChars > 0) {
            System.arraycopy(lineBufferPrefixWidths, charCount + 1, lineBufferPrefixWidths, 1, remainingChars);
            for (int i = 1; i <= remainingChars; i++) {
                lineBufferPrefixWidths[i] -= removedWidth;
            }
        }
        lineBuffer.delete(0, charCount);
        lineBufferPrefixWidths[0] = 0f;
        return removedWidth;
    }

    private float rebuildLineBufferWidths(ResolvedTextStyle style) {
        ensureLineBufferPrefixCapacity(lineBuffer.length() + 1);
        lineBufferPrefixWidths[0] = 0f;
        float width = 0f;
        boolean bold = style.bold();
        boolean visibleGlyphSeen = false;
        for (int index = 0; index < lineBuffer.length(); index++) {
            char character = lineBuffer.charAt(index);
            if (GuideFontCompat.isFormattingCodeStart(lineBuffer, index)) {
                lineBufferPrefixWidths[index + 1] = width;
                char formatChar = lineBuffer.charAt(index + 1);
                bold = GuideFontCompat.determineBold(bold, formatChar);
                lineBufferPrefixWidths[index + 2] = width;
                index++;
                continue;
            }
            int codePoint = character;
            if (Character.isHighSurrogate(character) && index + 1 < lineBuffer.length()) {
                char low = lineBuffer.charAt(index + 1);
                if (Character.isLowSurrogate(low)) {
                    codePoint = Character.toCodePoint(character, low);
                }
            }
            float advance = context.getRenderedAdvance(codePoint, style, visibleGlyphSeen);
            width += Math.max(0f, advance);
            int charCount = Character.charCount(codePoint);
            for (int offset = 1; offset <= charCount; offset++) {
                lineBufferPrefixWidths[index + offset] = width;
            }
            index += charCount - 1;
            if (advance > 0f) {
                visibleGlyphSeen = true;
            }
        }
        return width;
    }

    private boolean resolveTrailingBold(ResolvedTextStyle style, CharSequence text) {
        boolean bold = style.bold();
        for (int index = 0; index < text.length() - 1; index++) {
            if (!GuideFontCompat.isFormattingCodeStart(text, index)) {
                continue;
            }
            bold = GuideFontCompat.determineBold(bold, text.charAt(index + 1));
            index++;
        }
        return bold;
    }

    private boolean containsVisibleGlyph(CharSequence text) {
        for (int index = 0; index < text.length(); index++) {
            if (GuideFontCompat.isFormattingCodeStart(text, index)) {
                index++;
                continue;
            }
            return true;
        }
        return false;
    }

    private char findLastVisibleChar(CharSequence text) {
        for (int index = text.length() - 1; index >= 0; index--) {
            char character = text.charAt(index);
            if (character == GuideFontCompat.FORMATTING_CHAR && index + 1 < text.length()) {
                continue;
            }
            if (index > 0 && text.charAt(index - 1) == GuideFontCompat.FORMATTING_CHAR) {
                index--;
                continue;
            }
            return character;
        }
        return '\0';
    }

    private void ensureLineBufferPrefixCapacity(int requiredLength) {
        if (requiredLength <= lineBufferPrefixWidths.length) {
            return;
        }
        int newLength = lineBufferPrefixWidths.length;
        while (newLength < requiredLength) {
            newLength <<= 1;
        }
        lineBufferPrefixWidths = Arrays.copyOf(lineBufferPrefixWidths, newLength);
    }

    private void endLine() {
        if (openLineElement == null) {
            return;
        }

        context.clearFloatsAbove(lineBoxY);
        // All element bounds are relative to the shared baseline until the line is placed.
        int maxAscent = Math.max(0, -openLineMinTop);
        int maxDescent = Math.max(0, openLineMaxBottom);
        var lineHeight = Math.max(1, maxAscent + maxDescent);
        var lineWidth = openLineMaxRight;

        var textAreaStart = getInnerLeftEdge();
        var textAreaEnd = getInnerRightEdge();

        // Apply alignment
        int xTranslation = textAreaStart;
        if (alignment == TextAlignment.RIGHT) {
            xTranslation = textAreaEnd - lineWidth;
        } else if (alignment == TextAlignment.CENTER) {
            xTranslation = textAreaStart + ((textAreaEnd - textAreaStart) - lineWidth) / 2;
        }
        int yTranslation = lineBoxY + maxAscent;

        // reposition all line elements
        int actualRight = lineBoxX;
        for (var el = openLineElement; el != null; el = el.next) {
            el.bounds = el.bounds.move(xTranslation, yTranslation);
            // Ensure that inline blocks update their blocks absolute position
            if (el instanceof LineBlock lineBlock) {
                lineBlock.layoutBlock(context, el.bounds.width());
            }

            actualRight = Math.max(actualRight, el.bounds.right());
        }

        var lineBounds = new LytRect(lineBoxX, lineBoxY, actualRight - lineBoxX, lineHeight);
        var line = new Line(lineBounds, openLineElement);
        lines.add(line);

        // Advance vertically
        lineBoxY = line.bounds.bottom();

        // Close out any floats that are above the fold
        context.clearFloatsAbove(lineBoxY);

        // Reset horizontal position
        openLineElement = null;
        openLineTail = null;
        innerX = 0;
        openLineMinTop = Integer.MAX_VALUE;
        openLineMaxBottom = 0;
        openLineMaxRight = 0;
        openLineEndsWithBreakOpportunity = false;

        // Recompute now that floats may have been closed, what the horizontal space really is
        remainingLineWidth = getInnerRightEdge() - getInnerLeftEdge();
    }

    // How much horizontal space is available in a new line, accounting for active floats that take up space
    private int getAvailableHorizontalSpace() {
        return Math.max(0, getInnerRightEdge() - getInnerLeftEdge());
    }

    // Absolute X coord of the beginning of the text area of the current line box
    private int getInnerLeftEdge() {
        return Math.max(lineBoxX, context.getLeftFloatRightEdgeOr(lineBoxX, lineBoxX, lineBoxX + lineBoxWidth));
    }

    // Absolute X coord of the end of the text area of the current line box
    private int getInnerRightEdge() {
        return Math.min(
            lineBoxX + lineBoxWidth,
            context.getRightFloatLeftEdgeOr(lineBoxX + lineBoxWidth, lineBoxX, lineBoxX + lineBoxWidth));
    }

    private void appendToOpenLine(LineElement el) {
        if (openLineElement != null) {
            if (openLineTail != null) {
                openLineTail.next = el;
            }
        } else {
            openLineElement = el;
        }
        openLineTail = el;
        includeVerticalBounds(el.bounds.y(), el.bounds.bottom());
        int right = el.bounds.right();
        if (right > openLineMaxRight) openLineMaxRight = right;
    }

    private void includeVerticalBounds(int top, int bottom) {
        openLineMinTop = Math.min(openLineMinTop, top);
        openLineMaxBottom = Math.max(openLineMaxBottom, bottom);
    }

    private int getInlineImageYOffset(LineElement element) {
        if (element instanceof LineBlock lineBlock && lineBlock.getBlock() instanceof LytItemImage itemImage) {
            return itemImage.getInlineVerticalOffset();
        }
        return 0;
    }

    private boolean isInlineImage(LineElement element) {
        return element instanceof LineBlock lineBlock && !lineBlock.floating && isInlineImage(lineBlock.getBlock());
    }

    private static boolean isInlineImage(LytBlock block) {
        return block instanceof LytImage
            || block instanceof LytItemImage itemImage && itemImage.isInline() && itemImage.isShowingIcon();
    }

    /**
     * Moves the end of the open line that offers no break opportunity onto a new line: the inline images at its end
     * plus the word directly touching them. Keeps an item link's icon on the same line as its label.
     *
     * @param requireTrailingImage whether the open line has to end on an inline image; otherwise only the last word is
     *                             moved, as it touches an image that is about to be appended
     */
    private boolean moveGluedTailToNextLine(boolean requireTrailingImage) {
        if (openLineElement == null) {
            return false;
        }

        var elements = new ArrayList<LineElement>();
        // Offset of each element from where the previous one ended (inline block margins)
        var offsets = new ArrayList<Integer>();
        int origin = 0;
        for (var el = openLineElement; el != null; el = el.next) {
            elements.add(el);
            offsets.add(el.bounds.x() - origin);
            origin += el.bounds.width();
        }

        int start = elements.size();
        while (start > 0 && isInlineImage(elements.get(start - 1))) {
            start--;
        }
        if (requireTrailingImage && start == elements.size()) {
            return false;
        }

        if (start > 0 && elements.get(start - 1) instanceof LineTextRun run) {
            int wordStart = findGluedWordStart(run);
            if (wordStart > 0) {
                var head = run.text.substring(0, wordStart);
                var headRun = copyTextRun(run, head, run.bounds.x());
                var tailRun = copyTextRun(run, collectFormattingCodes(head) + run.text.substring(wordStart), 0);
                elements.set(start - 1, headRun);
                elements.add(start, tailRun);
                offsets.add(start, 0);
            } else if (wordStart == 0 && start >= 2 && endsWithBreakOpportunity(elements.get(start - 2))) {
                start--;
            }
        }
        if (start == 0 || start == elements.size()) {
            // Either the whole line is one unbreakable unit or there is nothing to move
            return false;
        }

        openLineElement = null;
        openLineTail = null;
        openLineMinTop = Integer.MAX_VALUE;
        openLineMaxBottom = 0;
        openLineMaxRight = 0;
        for (int i = 0; i < start; i++) {
            var el = elements.get(i);
            el.next = null;
            reappendToOpenLine(el);
        }
        endLine();

        int x = 0;
        for (int i = start; i < elements.size(); i++) {
            var el = elements.get(i);
            el.next = null;
            el.bounds = el.bounds.withX(x + offsets.get(i));
            reappendToOpenLine(el);
            x += el.bounds.width();
        }
        innerX = x;
        remainingLineWidth -= x;
        openLineEndsWithBreakOpportunity = false;
        return true;
    }

    private void reappendToOpenLine(LineElement el) {
        appendToOpenLine(el);
        if (el instanceof LineBlock && el.flowContent != null) {
            var style = el.flowContent.resolveStyle();
            includeVerticalBounds(-context.getAscent(style), context.getDescent(style));
        }
    }

    /**
     * @return where the word at the end of the run starts, or -1 if the run ends on a break opportunity
     */
    private int findGluedWordStart(LineTextRun run) {
        if (run.text.isEmpty() || run.style.inlineCode() || Character.isWhitespace(findLastVisibleChar(run.text))) {
            return -1;
        }
        var breakIterator = LINE_BREAK_ITERATOR.get();
        breakIterator.setText(run.text);
        return Math.max(0, breakIterator.preceding(run.text.length()));
    }

    private boolean endsWithBreakOpportunity(LineElement element) {
        if (element instanceof LineTextRun run) {
            return Character.isWhitespace(findLastVisibleChar(run.text));
        }
        return !isInlineImage(element);
    }

    private LineTextRun copyTextRun(LineTextRun run, String text, int x) {
        var copy = new LineTextRun(text, run.style, run.revealStyle, run.hoverStyle);
        copy.flowContent = run.flowContent;
        copy.bounds = new LytRect(x, run.bounds.y(), measureText(text, run.style), run.bounds.height());
        return copy;
    }

    private int measureText(CharSequence text, ResolvedTextStyle style) {
        float width = 0f;
        boolean visibleGlyphSeen = false;
        for (int index = 0; index < text.length(); index++) {
            if (GuideFontCompat.isFormattingCodeStart(text, index)) {
                index++;
                continue;
            }
            int codePoint = Character.codePointAt(text, index);
            float advance = context.getRenderedAdvance(codePoint, style, visibleGlyphSeen);
            width += Math.max(0f, advance);
            index += Character.charCount(codePoint) - 1;
            if (advance > 0f) {
                visibleGlyphSeen = true;
            }
        }
        return Math.round(width);
    }

    /**
     * @return the formatting codes in the text, so text split off after it keeps rendering the same
     */
    private static String collectFormattingCodes(CharSequence text) {
        var codes = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            if (GuideFontCompat.isFormattingCodeStart(text, index)) {
                codes.append(text, index, index + 2);
                index++;
            }
        }
        return codes.toString();
    }

    public void end() {
        endLine();
    }

    public LytRect getBounds() {
        int width = 0;
        for (var line : lines) {
            width = Math.max(width, line.bounds.width());
        }
        return new LytRect(lineBoxX, startY, width, lineBoxY - startY);
    }

    @FunctionalInterface
    private interface LineConsumer {

        void visitRun(CharSequence run, float width, boolean end);
    }

    /**
     * A {@link CharacterIterator} that wraps a {@link StringBuilder} without copying it to a {@link String}.
     * Reuse by calling {@link #reset} before each use.
     */
    private static class StringBuilderCharIterator implements CharacterIterator {

        private StringBuilder sb;
        private int begin;
        private int end;
        private int pos;

        /** Points this iterator at the full content of {@code buf}. */
        public void reset(StringBuilder buf) {
            this.sb = buf;
            this.begin = 0;
            this.end = buf.length();
            this.pos = 0;
        }

        @Override
        public char first() {
            pos = begin;
            return current();
        }

        @Override
        public char last() {
            if (end == begin) {
                pos = end;
                return DONE;
            }
            pos = end - 1;
            return current();
        }

        @Override
        public char current() {
            return (pos >= begin && pos < end) ? sb.charAt(pos) : DONE;
        }

        @Override
        public char next() {
            if (pos < end - 1) {
                pos++;
                return current();
            }
            pos = end;
            return DONE;
        }

        @Override
        public char previous() {
            if (pos > begin) {
                pos--;
                return current();
            }
            return DONE;
        }

        @Override
        public char setIndex(int position) {
            if (position < begin || position > end) {
                throw new IllegalArgumentException("Invalid index: " + position);
            }
            pos = position;
            return current();
        }

        @Override
        public int getBeginIndex() {
            return begin;
        }

        @Override
        public int getEndIndex() {
            return end;
        }

        @Override
        public int getIndex() {
            return pos;
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }
}
