package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Exports LaTeX formulas as PNG assets for the static site.
 *
 * <p>
 * <b>Typeset-at-target-size.</b> Formulas are typeset directly at
 * {@code fontSize = GuideText.BASE_FONT_SIZE × userScale} pixels
 * ({@code setSize(fontSize)}), so the exported {@code widthPx}/{@code heightPx}/
 * {@code depthPx} ARE the target display pixel dimensions; the site HTML uses
 * them verbatim. No reference-string calibration, no scaling conversion.
 */
public class GuideSiteLatexExporter {

    private final GuideSiteAssetRegistry assets;
    private final Map<String, ExportedLatex> exports = new HashMap<>();

    public GuideSiteLatexExporter(GuideSiteAssetRegistry assets) {
        this.assets = assets;
    }

    public ExportedLatex export(String formula, int fillColorArgb, float fontSize) {
        if (formula == null || formula.trim()
            .isEmpty()) {
            return null;
        }
        float safeFontSize = Math.max(1f, fontSize);
        String key = fillColorArgb + ":" + safeFontSize + ":" + formula;
        ExportedLatex cached = exports.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            TeXIcon icon = createIcon(formula, fillColorArgb, safeFontSize);
            BufferedImage image = renderImage(icon);
            String src = GuideSitePageAssetExporter.ROOT_PREFIX + assets.writePngAsync("latex", image);
            ExportedLatex exported = new ExportedLatex(
                src,
                icon.getIconWidth(),
                icon.getIconHeight(),
                Math.max(0, (int) Math.ceil(icon.getTrueIconDepth())));
            exports.put(key, exported);
            return exported;
        } catch (ParseException e) {
            GuideDebugLog.error(
                "[GuideNH] [GuideSiteLatexExporter] Failed to parse LaTeX formula '{}': {}",
                formatFormulaForLog(formula),
                e.getMessage());
            return null;
        } catch (Exception e) {
            GuideDebugLog.warn(
                "[GuideNH] [GuideSiteLatexExporter] Failed to export LaTeX formula '{}': {}",
                formatFormulaForLog(formula),
                e.getMessage(),
                e);
            return null;
        }
    }

    /** Escapes control characters so one export warning stays on one log line. */
    private String formatFormulaForLog(String formula) {
        return formula.replace("\\", "\\\\")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
    }

    private TeXIcon createIcon(String formula, int fillColorArgb, float fontSize) throws ParseException {
        TeXFormula texFormula = new TeXFormula(formula);
        TeXIcon icon = texFormula.new TeXIconBuilder().setStyle(TeXConstants.STYLE_DISPLAY)
            .setSize(fontSize)
            .setFGColor(new Color(fillColorArgb, true))
            .build();
        // Two-arg form (trueValues): keep the intended 2px/side insets. The
        // single-arg setInsets(Insets) delegates to setInsets(insets, false)
        // and silently inflates every side by (int)(0.18f*size). Same
        // pitfall/fix as GuideLatexRenderer.
        icon.setInsets(new Insets(2, 2, 2, 2), true);
        icon.setForeground(new Color(fillColorArgb, true));
        return icon;
    }

    private BufferedImage renderImage(TeXIcon icon) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setColor(new Color(ColorUtils.TRANSPARENT.getColor(), true));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }

        return image;
    }

    public static class ExportedLatex {

        private final String src;
        private final int widthPx;
        private final int heightPx;
        private final int depthPx;

        public ExportedLatex(String src, int widthPx, int heightPx, int depthPx) {
            this.src = src;
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            this.depthPx = depthPx;
        }

        public String src() {
            return src;
        }

        public int widthPx() {
            return widthPx;
        }

        public int heightPx() {
            return heightPx;
        }

        public int depthPx() {
            return depthPx;
        }
    }
}
