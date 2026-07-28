package com.hfstudio.guidenh.guide.internal.mermaid;

import com.hfstudio.guidenh.guide.document.block.LytMermaidFlowchart;
import com.hfstudio.guidenh.guide.document.block.LytMermaidMindmap;
import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Shared precomputation of Mermaid diagram layout before the Rust layout engine
 * processes the block. Called both from {@code PreCompiler} (compile-time fenced
 * blocks) and from {@code MermaidScript} (runtime script-dispatch path).
 * <p>
 * Each method creates a GuideText-backed {@link LayoutContext}, invokes the
 * canvas's {@code precomputeLayout} to run the ELK / mindmap layout, and sets
 * {@code preferredHeight} on the canvas so the Rust layout engine allocates the
 * correct canvas height on its first pass (avoiding the height-0-then-correct
 * pattern that causes parent VBox to freeze at a collapsed height).
 */
public final class MermaidLayoutPrecomputer {

    private MermaidLayoutPrecomputer() {
        // utility class
    }

    /**
     * Pre-compute ELK layout for a flowchart block and set preferredHeight on its
     * canvas so Rust's first layout pass allocates the correct height.
     *
     * @param block     the flowchart block whose canvas gets pre-computed
     * @param pageWidth available page content width (used as canvas extents);
     *                  caller should supply a positive value (480 is the standard
     *                  fallback when no runtime width is available)
     */
    public static void precomputeFlowchartLayout(LytMermaidFlowchart block, int pageWidth) {
        int safePageWidth = pageWidth > 0 ? pageWidth : 480;
        LayoutContext layoutCtx = new LayoutContext(new FontMetrics() {
            @Override
            public float getAdvance(int codePoint, ResolvedTextStyle s) {
                return GuideText.measureWidth(new String(Character.toChars(codePoint)), s);
            }

            @Override
            public int getLineHeight(ResolvedTextStyle s) {
                return GuideText.lineHeight(s);
            }
        });
        block.getCanvas().precomputeLayout(layoutCtx, safePageWidth);
    }

    /**
     * Pre-compute mindmap layout and set preferredHeight on the canvas so Rust's
     * first layout pass allocates the correct height.
     *
     * @param block     the mindmap block whose canvas gets pre-computed
     * @param pageWidth available page content width (used as canvas extents);
     *                  caller should supply a positive value (480 is the standard
     *                  fallback when no runtime width is available)
     */
    public static void precomputeMindmapLayout(LytMermaidMindmap block, int pageWidth) {
        int safePageWidth = pageWidth > 0 ? pageWidth : 480;
        LayoutContext layoutCtx = new LayoutContext(new FontMetrics() {
            @Override
            public float getAdvance(int codePoint, ResolvedTextStyle s) {
                return GuideText.measureWidth(new String(Character.toChars(codePoint)), s);
            }

            @Override
            public int getLineHeight(ResolvedTextStyle s) {
                return GuideText.lineHeight(s);
            }
        });
        block.getCanvas().precomputeLayout(layoutCtx, safePageWidth);
    }
}
