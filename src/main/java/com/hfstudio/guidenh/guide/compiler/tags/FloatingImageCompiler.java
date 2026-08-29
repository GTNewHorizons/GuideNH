package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.ContentAlign;
import com.hfstudio.guidenh.guide.document.block.ContentWrapMode;
import com.hfstudio.guidenh.guide.document.block.ImageRegionAnnotation;
import com.hfstudio.guidenh.guide.document.block.LytAlignedBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
import com.hfstudio.guidenh.guide.document.block.LytImageBlock;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.flow.InlineBlockAlignment;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.sound.GuideSoundParsers;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

public class FloatingImageCompiler implements TagCompiler {

    public static final String TAG_NAME = "FloatingImage";

    private static final Random RANDOM = new Random(0);

    /**
     * Parsed crop / size specification. With both dimensions given this is a
     * classic crop rectangle (x/y/width/height). With exactly one dimension
     * given (hasWidth XOR hasHeight) it is a whole-image display size: the
     * given dimension is the final display pixel size × scale and the missing
     * dimension is inferred from the image's natural aspect ratio downstream
     * (Rust measure_image and the Java mirror paths).
     */
    private record CropSpec(int x, int y, int width, int height, boolean hasWidth, boolean hasHeight) {}

    private record ScaleSpec(double scaleX, double scaleY) {}

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton(TAG_NAME);
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        String wrapAttr = el.getAttributeString("wrap", null);
        String alignAttr = el.getAttributeString("align", null);

        var wrapMode = ContentWrapMode.fromString(wrapAttr);
        var align = ContentAlign.fromString(alignAttr);

        // Inline wrap: only explicit wrap="inline" goes to the inline‑block path.
        if ("inline".equals(wrapAttr)) {
            var paragraph = new LytParagraph();
            compileInline(compiler, paragraph, el);
            parent.append(paragraph);
            return;
        }

        // Build the image block for all non‑inline modes.
        LytImageBlock imageBlock = buildImageBlock(compiler, parent, el);
        if (imageBlock == null) return;

        // No explicit wrap + left/right align → document float (matching legacy behaviour).
        if (wrapAttr == null && ("left".equals(alignAttr) || "right".equals(alignAttr))) {
            LytDocumentFloat docFloat = new LytDocumentFloat(imageBlock, "right".equals(alignAttr));
            parent.append(docFloat);
            return;
        }

        // Square / tight / through → document float (same as BlockTagCompiler.applyBlockEmbed).
        if (wrapMode.isDocumentFloat()) {
            LytDocumentFloat docFloat = new LytDocumentFloat(imageBlock, align == ContentAlign.RIGHT);
            parent.append(docFloat);
            return;
        }

        // Behind / front / top‑bottom → aligned block path (matching
        // BlockTagCompiler.applyBlockEmbed: lines 105‑108), not a document float.
        LytBlock result = imageBlock;
        if (align != ContentAlign.LEFT) {
            result = new LytAlignedBlock(result, align);
        }
        parent.append(PageCompiler.wrapFloatAwareIfNeeded(result));
    }

    @Override
    public void compileFlowContext(PageCompiler compiler, LytFlowParent parent, MdxJsxTextElement el) {
        compileInline(compiler, parent, el);
    }

    /**
     * Inline path: build the image block and wrap in a {@link LytFlowInlineBlock}
     * with FLOAT_LEFT / FLOAT_RIGHT / INLINE alignment.
     * <p>
     * Used by:
     * <ul>
     * <li>{@link #compileFlowContext} – the parent is the actual flow container</li>
     * <li>{@link #compileBlockContext} inline fallback – the parent is a freshly
     * created {@link LytParagraph} that will be appended to the block container</li>
     * </ul>
     */
    private void compileInline(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        LytImageBlock block = buildImageBlock(compiler, parent, el);
        if (block == null) return;

        String wrap = el.getAttributeString("wrap", null);
        String align = el.getAttributeString("align", "left");
        var inlineBlock = new LytFlowInlineBlock();
        inlineBlock.setBlock(block);
        if ("inline".equals(wrap)) {
            inlineBlock.setAlignment(InlineBlockAlignment.INLINE);
            parent.append(inlineBlock);
            return;
        }
        switch (align) {
            case "left" -> {
                inlineBlock.setAlignment(InlineBlockAlignment.FLOAT_LEFT);
                block.setMarginRight(5);
                block.setMarginBottom(5);
            }
            case "right" -> {
                inlineBlock.setAlignment(InlineBlockAlignment.FLOAT_RIGHT);
                block.setMarginLeft(5);
                block.setMarginBottom(5);
            }
            default -> {
                parent.append(compiler.createErrorFlowContent("Invalid align. Must be left or right.", el));
                return;
            }
        }
        parent.append(inlineBlock);
    }

    /**
     * Shared block-building logic used by both
     * {@link #compileBlockContext(PageCompiler, LytBlockContainer, MdxJsxFlowElement)}
     * (document‑float path) and
     * {@link #compileInline(PageCompiler, LytFlowParent, MdxJsxElementFields)}
     * (inline path).
     *
     * @return the fully‑configured {@link LytImageBlock}, or {@code null} on parse failure
     */
    @Nullable
    private static LytImageBlock buildImageBlock(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        var src = el.getAttributeString("src", null);
        if (src == null || src.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "FloatingImage requires a non-empty src attribute.", el);
            return null;
        }
        var align = el.getAttributeString("align", "left");
        var title = el.getAttributeString("title", null);
        var alt = el.getAttributeString("alt", null);
        CropSpec crop = parseCropSpec(compiler, errorSink, el);
        ScaleSpec scale = parseScaleSpec(compiler, errorSink, el);
        if (crop == null || scale == null) {
            return null;
        }

        LytImageBlock block = new LytImageBlock();
        block.setStyleClass("FloatingImage");
        block.setStyle(LytParagraph.PLACEHOLDER_STYLE);
        block.appendText("[FloatingImage]");
        block.setAlign(align);
        if (title != null) {
            block.setTitle(title);
        }
        if (alt != null) {
            block.setAlt(alt);
        }
        block.setCropX(crop.x());
        block.setCropY(crop.y());
        // F-N1 single-parameter mode (width-only / height-only) is a
        // whole-image display size: no crop is applied and the missing explicit
        // dimension stays -1 so downstream measurement infers it from the
        // natural aspect ratio.
        if (crop.hasWidth() && crop.hasHeight()) {
            block.setCropWidth(crop.width());
            block.setCropHeight(crop.height());
        } else {
            block.setCropWidth(-1);
            block.setCropHeight(-1);
        }
        block.setScaleX(scale.scaleX());
        block.setScaleY(scale.scaleY());

        // Resolve the image src to a string identifier for later script use without
        // loading the actual asset at compile time.
        String resolvedSrc = null;
        try {
            var imageId = IdUtils.resolveLink(src, compiler.getPageId());
            resolvedSrc = imageId.toString();
        } catch (IllegalArgumentException e) {
            GuideDebugLog.error("[GuideNH] [FloatingImageCompiler] Invalid image id: {}", src);
            if (block.getTitle() == null) {
                block.setTitle("Invalid image URL: " + src);
            }
        }
        block.setSrc(resolvedSrc);

        var wholeImageSound = GuideSoundParsers.parseAttributes(compiler, errorSink, el, "soundSrc");
        if (wholeImageSound != null) {
            var soundAnnotation = new ImageRegionAnnotation(false, ConstantColor.WHITE, 1);
            soundAnnotation.setSound(wholeImageSound);
            soundAnnotation.setSoundTrigger(parseTrigger(compiler, errorSink, el));
            block.addAnnotation(soundAnnotation);
        }

        // Parse <ImageAnnotation> child elements.
        var children = el.children();
        if (children != null) {
            for (var child : children) {
                if (child instanceof MdxJsxElementFields annEl && "ImageAnnotation".equals(annEl.name())) {
                    var ann = parseImageAnnotation(compiler, errorSink, annEl);
                    block.addAnnotation(ann);
                } else if (child instanceof MdxJsxElementFields soundEl && "SoundArea".equals(soundEl.name())) {
                    var ann = parseSoundArea(compiler, errorSink, soundEl);
                    if (ann != null) {
                        block.addAnnotation(ann);
                    }
                }
            }
        }

        // Forward crop dimensions × scale as explicit size for Rust measure_image
        // so that scaleX/scaleY are reflected in the final measured size.
        // F-N1: in single-parameter mode the given dimension × scale is the
        // explicit display size (missing axis inferred from natural aspect
        // ratio); the missing dimension stays -1.
        if (crop.hasWidth()) {
            block.setExplicitWidth((int) Math.round(crop.width() * scale.scaleX()));
        } else {
            block.setExplicitWidth(-1);
        }
        if (crop.hasHeight()) {
            block.setExplicitHeight((int) Math.round(crop.height() * scale.scaleY()));
        } else {
            block.setExplicitHeight(-1);
        }

        return block;
    }

    /**
     * Parses a single {@code <ImageAnnotation>} child element into an {@link ImageRegionAnnotation}.
     * Returns {@code null} only when a fatal parse error occurs.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li>{@code x}, {@code y}, {@code w}, {@code h}: region in image pixels; omitting all four
     * means the annotation covers the whole image.</li>
     * <li>{@code border}: boolean flag; presence (or {@code {true}}) enables the border.</li>
     * <li>{@code borderColor}: {@code #RRGGBB} or {@code #AARRGGBB}; omit for a random color.</li>
     * <li>{@code borderThickness}: integer pixel thickness, default 1.</li>
     * </ul>
     * Child MDX content is compiled as the rich-text tooltip body.
     */
    @NotNull
    private static ImageRegionAnnotation parseImageAnnotation(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields annEl) {
        ImageRegionAnnotation ann = parseImageAnnotationRegion(compiler, errorSink, annEl, true);

        // Compile tooltip rich-text content from child elements.
        var contentBox = new LytVBox();
        compiler.compileBlockTagChildren(annEl, contentBox);
        if (!contentBox.getChildren()
            .isEmpty()) {
            ann.setTooltip(new ContentTooltip(contentBox));
        }
        ann.setSound(GuideSoundParsers.parseAttributes(compiler, errorSink, annEl));
        ann.setSoundTrigger(parseTrigger(compiler, errorSink, annEl));

        return ann;
    }

    private static ImageRegionAnnotation parseSoundArea(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        var sound = GuideSoundParsers.parseAttributes(compiler, errorSink, el);
        if (sound == null) {
            errorSink.appendError(compiler, "SoundArea requires a sound or src attribute.", el);
            return null;
        }
        ImageRegionAnnotation ann = parseImageAnnotationRegion(compiler, errorSink, el, false);
        ann.setSound(sound);
        ann.setSoundTrigger(parseTrigger(compiler, errorSink, el));
        return ann;
    }

    private static ImageRegionAnnotation parseImageAnnotationRegion(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el, boolean allowBorder) {
        int x = MdxAttrs.getInt(compiler, errorSink, el, "x", -1);
        int y = MdxAttrs.getInt(compiler, errorSink, el, "y", -1);
        int w = MdxAttrs.getInt(compiler, errorSink, el, "w", -1);
        int h = MdxAttrs.getInt(compiler, errorSink, el, "h", -1);
        boolean wholeImage = x < 0 && y < 0 && w < 0 && h < 0;

        boolean showBorder = allowBorder && MdxAttrs.getBoolean(compiler, errorSink, el, "border", false);
        int borderThickness = allowBorder ? MdxAttrs.getInt(compiler, errorSink, el, "borderThickness", 1) : 1;

        ColorValue borderColor;
        if (allowBorder && el.getAttribute("borderColor") != null) {
            borderColor = MdxAttrs.getColor(compiler, errorSink, el, "borderColor", ConstantColor.WHITE);
        } else {
            borderColor = allowBorder ? new ConstantColor(0xFF000000 | RANDOM.nextInt(0x1000000)) : ConstantColor.WHITE;
        }

        if (wholeImage) {
            return new ImageRegionAnnotation(showBorder, borderColor, borderThickness);
        }

        int ax = Math.max(x, 0);
        int ay = Math.max(y, 0);
        int aw = Math.max(1, w < 0 ? 1 : w);
        int ah = Math.max(1, h < 0 ? 1 : h);
        return new ImageRegionAnnotation(ax, ay, aw, ah, showBorder, borderColor, borderThickness);
    }

    private static GuideSoundTrigger parseTrigger(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        return GuideSoundTrigger
            .parse(MdxAttrs.getString(compiler, errorSink, el, "trigger", null), GuideSoundTrigger.CLICK);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        var title = el.getAttributeString("title", null);
        if (title != null) {
            sink.appendText(el, title);
        }
    }

    public static int parseIntAttr(MdxJsxElementFields el, String name, int def) {
        var s = el.getAttributeString(name, null);
        if (s == null || s.isEmpty()) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    @Nullable
    private static CropSpec parseCropSpec(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        String widthValue = el.getAttributeString("width", null);
        String widthAlias = el.getAttributeString("w", null);
        String heightValue = el.getAttributeString("height", null);
        String heightAlias = el.getAttributeString("h", null);
        if (widthValue != null && widthAlias != null) {
            errorSink.appendError(compiler, "FloatingImage cannot use both width and w.", el);
            return null;
        }
        if (heightValue != null && heightAlias != null) {
            errorSink.appendError(compiler, "FloatingImage cannot use both height and h.", el);
            return null;
        }
        boolean hasWidth = (widthValue != null && !widthValue.trim()
            .isEmpty()) || (widthAlias != null
                && !widthAlias.trim()
                    .isEmpty());
        boolean hasHeight = (heightValue != null && !heightValue.trim()
            .isEmpty()) || (heightAlias != null
                && !heightAlias.trim()
                    .isEmpty());
        // F-N1: a single explicit dimension (width-only or height-only) is a
        // valid whole-image display size; only the "both missing" case is an
        // error.
        if (!hasWidth && !hasHeight) {
            errorSink.appendError(compiler, "FloatingImage requires width or w, and height or h.", el);
            return null;
        }
        Integer x = parseOptionalIntAttr(compiler, errorSink, el, "x", 0);
        Integer y = parseOptionalIntAttr(compiler, errorSink, el, "y", 0);
        Integer width = parseAliasedIntAttr(compiler, errorSink, el, "width", "w");
        Integer height = parseAliasedIntAttr(compiler, errorSink, el, "height", "h");
        if (x == null || y == null) {
            return null;
        }
        if ((hasWidth && width == null) || (hasHeight && height == null)) {
            return null;
        }
        if (x < 0 || y < 0 || (hasWidth && width <= 0) || (hasHeight && height <= 0)) {
            errorSink.appendError(
                compiler,
                "FloatingImage crop values must be non-negative and width/height must be positive.",
                el);
            return null;
        }
        return new CropSpec(x, y, hasWidth ? width : -1, hasHeight ? height : -1, hasWidth, hasHeight);
    }

    @Nullable
    private static ScaleSpec parseScaleSpec(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        Double scaleX = parseDoubleAttr(compiler, errorSink, el, "scaleX", 1.0d);
        Double scaleY = parseDoubleAttr(compiler, errorSink, el, "scaleY", 1.0d);
        if (scaleX == null || scaleY == null) {
            return null;
        }
        if (scaleX <= 0.0d || scaleY <= 0.0d) {
            errorSink.appendError(compiler, "FloatingImage scaleX and scaleY must be positive.", el);
            return null;
        }
        return new ScaleSpec(scaleX, scaleY);
    }

    /**
     * Parses an aliased integer attribute (primary name or alias) as optional.
     * Returns {@code null} when the attribute is absent — a valid state under
     * F-N1 single-parameter mode where exactly one dimension is required (the
     * "both missing" error is reported by {@link #parseCropSpec}). A present
     * but malformed value appends an error and returns {@code null}.
     */
    @Nullable
    private static Integer parseAliasedIntAttr(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String primaryName, String aliasName) {
        String primaryValue = el.getAttributeString(primaryName, null);
        String aliasValue = el.getAttributeString(aliasName, null);
        if (primaryValue != null && aliasValue != null) {
            errorSink
                .appendError(compiler, "FloatingImage cannot use both " + primaryName + " and " + aliasName + ".", el);
            return null;
        }
        String resolved = primaryValue != null ? primaryValue : aliasValue;
        if (resolved == null || resolved.trim()
            .isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(resolved.trim());
        } catch (NumberFormatException ex) {
            errorSink.appendError(compiler, "FloatingImage " + primaryName + " must be an integer.", el);
            return null;
        }
    }

    @Nullable
    private static Integer parseRequiredIntAttr(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String name) {
        String value = el.getAttributeString(name, null);
        if (value == null || value.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "FloatingImage requires x, y, width or w, and height or h.", el);
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            errorSink.appendError(compiler, "FloatingImage " + name + " must be an integer.", el);
            return null;
        }
    }

    @Nullable
    private static Integer parseOptionalIntAttr(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String name, int defaultValue) {
        String value = el.getAttributeString(name, null);
        if (value == null || value.trim()
            .isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            errorSink.appendError(compiler, "FloatingImage " + name + " must be an integer.", el);
            return null;
        }
    }

    @Nullable
    private static Double parseDoubleAttr(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String name, double defaultValue) {
        String value = el.getAttributeString(name, null);
        if (value == null || value.trim()
            .isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            errorSink.appendError(compiler, "FloatingImage " + name + " must be a number.", el);
            return null;
        }
    }
}
