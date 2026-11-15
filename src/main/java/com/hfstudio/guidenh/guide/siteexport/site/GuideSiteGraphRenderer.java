package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.block.chart.ChartLabelPosition;
import com.hfstudio.guidenh.guide.document.block.chart.ChartLegendPosition;
import com.hfstudio.guidenh.guide.document.block.chart.CornerLegendPosition;
import com.hfstudio.guidenh.guide.document.block.chart.CornerLegendRenderer;
import com.hfstudio.guidenh.guide.document.block.functiongraph.AutoPointLabelMode;
import com.hfstudio.guidenh.guide.document.block.functiongraph.AutoPointSpec;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.guide.document.block.functiongraph.MarkedPoint;
import com.hfstudio.guidenh.guide.internal.csv.CsvTableParser;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeEntry;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeIcon;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeIconKind;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeModel;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLatexShorthand;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapDocument;

/**
 * Generates static HTML and SVG markup for chart, function-graph, file-tree,
 * mermaid-mindmap, and CSV table elements used in the static site export.
 */
public class GuideSiteGraphRenderer {

    // Inline SVGs share the document id namespace, so clip-path ids must be unique across graphs.
    private static final AtomicInteger nextClipId = new AtomicInteger();

    private static final int CHART_DEFAULT_W = 320;
    private static final int CHART_DEFAULT_H = 200;
    private static final int GRAPH_DEFAULT_W = 320;
    private static final int GRAPH_DEFAULT_H = 220;
    /**
     * Match the function-graph font size in the site stylesheet; CSS overrides SVG font-size attributes.
     */
    private static final int GRAPH_TEXT_SIZE = 18;
    private static final int GRAPH_AXIS_PAD_LEFT = 3 * GRAPH_TEXT_SIZE;
    private static final int GRAPH_AXIS_PAD_BOTTOM = GRAPH_TEXT_SIZE + 4;
    private static final int PADDING = 8;
    /**
     * Reserve title height for the larger function-graph font, shared by charts and graphs.
     */
    private static final int TITLE_H = GRAPH_TEXT_SIZE;
    private static final int TITLE_GAP = 4;
    private static final int AXIS_TITLE_H = GRAPH_TEXT_SIZE;
    private static final int AXIS_TITLE_GAP = 4;
    /**
     * Match chart text metrics to the site stylesheet, which overrides SVG font-size attributes.
     */
    private static final int CHART_TEXT_SIZE = 16;
    static final int DEFAULT_TITLE_COLOR = 0xFFE6E6E6;
    static final float DEFAULT_PIE_START_ANGLE_DEG = -90f;
    static final float DEFAULT_BAR_WIDTH_RATIO = 0.7f;
    static final int DEFAULT_GRID_COLOR = 0xFF3A4047;
    private static final int AXIS_PAD_LEFT = 3 * CHART_TEXT_SIZE;
    private static final int AXIS_PAD_BOTTOM = CHART_TEXT_SIZE + 4;
    private static final int LABEL_ABOVE_OFFSET = CHART_TEXT_SIZE / 2 - 2;
    private static final int LABEL_BELOW_OFFSET = CHART_TEXT_SIZE + 2;
    private static final int LABEL_CENTER_OFFSET = CHART_TEXT_SIZE / 3;
    private static final int LEGEND_SWATCH = CHART_TEXT_SIZE - 4;
    private static final int LEGEND_GAP = 6;
    private static final int LEGEND_ROW_H = CHART_TEXT_SIZE + 4;
    private static final int CHART_CHAR_WIDTH = 9;
    private static final int PIE_OUTSIDE_GAP = 6;
    private static final int N_SAMPLES = 1024;
    private static final int AUTO_POINT_MAX_PER_PLOT = 96;
    private static final int AUTO_POINT_MAX_TARGETS_PER_PLOT = 256;
    private static final int AUTO_POINT_SCAN_STEPS = 128;
    private static final int AUTO_POINT_SOLVE_STEPS = 24;
    private static final int AUTO_POINT_LABEL_GAP = 4;
    private static final int CORNER_LEGEND_PADDING_X = 5;
    private static final int CORNER_LEGEND_PADDING_Y = 4;
    private static final int CORNER_LEGEND_GAP = 4;
    private static final int CORNER_LEGEND_ROW_H = GRAPH_TEXT_SIZE + 3;
    private static final int CORNER_LEGEND_MARKER_W = 10;
    private static final int CORNER_LEGEND_MARKER_H = 6;

    private GuideSiteGraphRenderer() {}

    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * Convert ARGB int (0xAARRGGBB) to CSS hex or rgba().
     */
    public static String argbToRgba(int argb) {
        return GuideSiteHtml.argbToRgba(argb);
    }

    public static String renderFileTree(String source) {
        FileTreeModel model = FileTreeParser.parse(source);
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-file-tree\">");
        for (FileTreeEntry entry : model.entries()) {
            html.append("<div class=\"guide-file-tree-row\">");
            StringBuilder prefix = new StringBuilder();
            for (SlotKind slot : entry.slots()) {
                switch (slot) {
                    case VERTICAL:
                        prefix.append("│   ");
                        break;
                    case BRANCH:
                        prefix.append("├── ");
                        break;
                    case LAST_BRANCH:
                        prefix.append("└── ");
                        break;
                    default:
                        prefix.append("    ");
                        break;
                }
            }
            if (!prefix.isEmpty()) {
                html.append("<span class=\"guide-file-tree-prefix\">")
                    .append(esc(prefix.toString()))
                    .append("</span>");
            }
            FileTreeIcon icon = entry.icon();
            if (icon != null && icon.kind() == FileTreeIconKind.TEXT) {
                html.append("<span class=\"guide-file-tree-icon\">")
                    .append(esc(icon.value()))
                    .append("</span> ");
            }
            html.append("<span class=\"guide-file-tree-name\">")
                .append(esc(entry.payloadSource()))
                .append("</span>");
            html.append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    public static String renderMermaidTree(MindmapDocument doc) {
        return GuideSiteMermaidRenderer.renderMermaidTree(doc);
    }

    public static String renderMermaidTree(MindmapDocument doc, Map<String, String> nodeHtmlById) {
        return GuideSiteMermaidRenderer.renderMermaidTree(doc, nodeHtmlById);
    }

    public static String renderFlowchart(FlowchartDocument doc) {
        return GuideSiteMermaidRenderer.renderFlowchart(doc);
    }

    public static String renderFlowchart(FlowchartDocument doc, Map<String, String> nodeHtmlById) {
        return GuideSiteMermaidRenderer.renderFlowchart(doc, nodeHtmlById);
    }

    public static String renderCsvTable(String csvSource, boolean hasHeader) {
        List<List<String>> rows = CsvTableParser.parse(csvSource);
        return renderCsvTable(rows, hasHeader, List.of());
    }

    public static String renderCsvTable(List<List<String>> rows, boolean hasHeader) {
        return renderCsvTable(rows, hasHeader, List.of());
    }

    public static String renderCsvTable(String csvSource, boolean hasHeader, List<Integer> widths) {
        List<List<String>> rows = CsvTableParser.parse(csvSource);
        return renderCsvTable(rows, hasHeader, widths);
    }

    public static String renderCsvTable(List<List<String>> rows, boolean hasHeader, List<Integer> widths) {
        if (rows.isEmpty()) {
            return "<div class=\"guide-csv-table-wrap\"><table class=\"guide-csv-table\"></table></div>";
        }
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-csv-table-wrap\"><table class=\"guide-csv-table\">");
        if (widths != null && !widths.isEmpty()) {
            int columnCount = rows.stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);
            html.append("<colgroup>");
            for (int index = 0; index < columnCount; index++) {
                Integer width = index < widths.size() ? widths.get(index) : null;
                html.append("<col");
                if (width != null && width > 0) {
                    html.append(" style=\"width:")
                        .append(width)
                        .append("px\"");
                }
                html.append(">");
            }
            html.append("</colgroup>");
        }
        int start = 0;
        if (hasHeader) {
            html.append("<thead><tr>");
            for (String cell : rows.getFirst()) {
                html.append("<th>")
                    .append(esc(cell))
                    .append("</th>");
            }
            html.append("</tr></thead>");
            start = 1;
        }
        if (start < rows.size()) {
            html.append("<tbody>");
            for (int i = start; i < rows.size(); i++) {
                html.append("<tr>");
                for (String cell : rows.get(i)) {
                    html.append("<td>")
                        .append(esc(cell))
                        .append("</td>");
                }
                html.append("</tr>");
            }
            html.append("</tbody>");
        }
        html.append("</table></div>");
        return html.toString();
    }

    /**
     * Chart series data (name, ARGB color, parallel xs/ys arrays).
     */
    public static class SeriesData {

        /**
         * Series rendered as vertical bars (default).
         */
        public static final String TYPE_COLUMN = "column";
        /**
         * Series rendered as a line overlay on a column chart.
         */
        public static final String TYPE_LINE = "line";

        public final String name;
        public final int color;
        public final double[] xs;
        public final double[] ys;
        /**
         * Rendering type: {@link #TYPE_COLUMN} or {@link #TYPE_LINE}.
         */
        public final String type;

        public SeriesData(String name, int color, double[] xs, double[] ys) {
            this(name, color, xs, ys, TYPE_COLUMN);
        }

        public SeriesData(String name, int color, double[] xs, double[] ys, String type) {
            this.name = name != null ? name : "";
            this.color = color;
            this.xs = xs != null ? xs : new double[0];
            this.ys = ys != null ? ys : new double[0];
            this.type = type != null ? type : TYPE_COLUMN;
        }
    }

    /**
     * Inset pie chart that overlays a corner of a column chart.
     */
    public static class PieInsetData {

        public final List<SliceData> slices;
        /**
         * Diameter of the inset pie in SVG user-space units.
         */
        public final int size;
        /**
         * Corner or side placement: "top-right", "left", "bottom-right", "bottom-left", "right".
         */
        public final String position;
        public final @Nullable String title;

        public PieInsetData(List<SliceData> slices, int size, String position, @Nullable String title) {
            this.slices = slices != null ? slices : new ArrayList<>();
            this.size = Math.max(20, size);
            this.position = position != null ? position : "top-right";
            this.title = title;
        }
    }

    /**
     * Pie-chart slice (label, value, ARGB color).
     */
    public static class SliceData {

        public final String label;
        public final double value;
        public final int color;

        public SliceData(String label, double value, int color) {
            this.label = label != null ? label : "";
            this.value = value;
            this.color = color;
        }
    }

    public static final int DEFAULT_LABEL_COLOR = ColorUtils.ARGB_FFEEEEEE.getColor();

    /**
     * Presentation every exported chart shares: size, frame, title, where its legend goes and how its
     * value labels look. The in-game charts read the same attributes, so a page keeps one appearance.
     *
     * @param width          chart width in pixels, a non-positive value means the default
     * @param height         chart height in pixels, a non-positive value means the default
     * @param background     ARGB chart background
     * @param border         ARGB chart border
     * @param title          optional chart title
     * @param legendPosition where the series legend is drawn
     * @param labelPosition  where each value label is drawn
     * @param labelColor     ARGB colour of the value labels
     */
    public record ChartStyle(int width, int height, int background, int border, @Nullable String title,
        @Nullable ChartLegendPosition legendPosition, ChartLabelPosition labelPosition, int labelColor,
        @Nullable SiteChartAxis xAxis, @Nullable SiteChartAxis yAxis, int titleColor, float barWidthRatio,
        float pieStartAngleDeg, boolean pieClockwise) {

        public ChartStyle {
            legendPosition = legendPosition != null ? legendPosition : ChartLegendPosition.NONE;
            labelPosition = labelPosition != null ? labelPosition : ChartLabelPosition.NONE;
        }

        public ChartStyle withAxes(@Nullable SiteChartAxis xAxis, @Nullable SiteChartAxis yAxis) {
            return new ChartStyle(
                width,
                height,
                background,
                border,
                title,
                legendPosition,
                labelPosition,
                labelColor,
                xAxis,
                yAxis,
                titleColor,
                barWidthRatio,
                pieStartAngleDeg,
                pieClockwise);
        }

        public ChartStyle withBarLayout(int titleColor, float barWidthRatio) {
            return new ChartStyle(
                width,
                height,
                background,
                border,
                title,
                legendPosition,
                labelPosition,
                labelColor,
                xAxis,
                yAxis,
                titleColor,
                barWidthRatio,
                pieStartAngleDeg,
                pieClockwise);
        }

        public SiteChartAxis xAxisOrDefault() {
            return xAxis != null ? xAxis : SiteChartAxis.automatic(DEFAULT_GRID_COLOR);
        }

        public SiteChartAxis yAxisOrDefault() {
            return yAxis != null ? yAxis : SiteChartAxis.automatic(DEFAULT_GRID_COLOR);
        }

        public ChartStyle withPieLayout(float startAngleDeg, boolean clockwise) {
            return new ChartStyle(
                width,
                height,
                background,
                border,
                title,
                legendPosition,
                labelPosition,
                labelColor,
                xAxis,
                yAxis,
                titleColor,
                barWidthRatio,
                startAngleDeg,
                clockwise);
        }

        public static ChartStyle of(int width, int height, int background, int border, @Nullable String title) {
            return new ChartStyle(
                width,
                height,
                background,
                border,
                title,
                ChartLegendPosition.NONE,
                ChartLabelPosition.NONE,
                DEFAULT_LABEL_COLOR,
                null,
                null,
                DEFAULT_TITLE_COLOR,
                DEFAULT_BAR_WIDTH_RATIO,
                DEFAULT_PIE_START_ANGLE_DEG,
                true);
        }

        public ChartStyle withLegend(@Nullable ChartLegendPosition position) {
            return new ChartStyle(
                width,
                height,
                background,
                border,
                title,
                position,
                labelPosition,
                labelColor,
                xAxis,
                yAxis,
                titleColor,
                barWidthRatio,
                pieStartAngleDeg,
                pieClockwise);
        }

        public ChartStyle withLabels(ChartLabelPosition position, int color) {
            return new ChartStyle(
                width,
                height,
                background,
                border,
                title,
                legendPosition,
                position,
                color,
                xAxis,
                yAxis,
                titleColor,
                barWidthRatio,
                pieStartAngleDeg,
                pieClockwise);
        }

        public int layoutWidth() {
            return width > 0 ? width : CHART_DEFAULT_W;
        }

        public int layoutHeight() {
            return height > 0 ? height : CHART_DEFAULT_H;
        }

        public boolean showsLegend() {
            return legendPosition != ChartLegendPosition.NONE;
        }

        public String labelFill() {
            return argbToRgba(labelColor);
        }
    }

    /**
     * Space a legend takes away from the plot and where it is drawn, so every chart reserves its own
     * legend the same way instead of overlapping the plot.
     */
    private record LegendArea(int left, int right, int top, int bottom, int x, int y, int width, int limit, int cols) {}

    // Column chart (vertical bars, categorical X).

    public static String renderColumnChart(int w, int h, int bgColor, int borderColor, String title,
        String[] categories, List<SeriesData> series, boolean showLegend) {
        return renderColumnChart(
            w,
            h,
            bgColor,
            borderColor,
            title,
            categories,
            series,
            showLegend,
            null,
            null,
            ChartLabelPosition.NONE);
    }

    public static String renderColumnChart(int w, int h, int bgColor, int borderColor, String title,
        String[] categories, List<SeriesData> series, boolean showLegend, @Nullable PieInsetData pieInset,
        @Nullable String yAxisUnit, ChartLabelPosition labelPosition) {
        ChartStyle style = ChartStyle.of(w, h, bgColor, borderColor, title)
            .withLegend(showLegend ? ChartLegendPosition.TOP : ChartLegendPosition.NONE)
            .withLabels(labelPosition, DEFAULT_LABEL_COLOR);
        return renderColumnChart(style, categories, series, pieInset, yAxisUnit);
    }

    /**
     * Composite column chart: renders bar series, optional line-overlay series, and an optional
     * pie inset in one of the chart's corners.
     *
     * @param pieInset  optional pie inset; {@code null} for a plain column chart
     * @param yAxisUnit optional unit label shown beside the Y-axis (e.g. "t"); {@code null} to omit
     */
    public static String renderColumnChart(ChartStyle style, String[] categories, List<SeriesData> series,
        @Nullable PieInsetData pieInset, @Nullable String yAxisUnit) {
        int w = style.layoutWidth();
        int h = style.layoutHeight();
        String title = style.title();
        int bgColor = style.background();
        int borderColor = style.border();
        ChartLabelPosition labelPosition = style.labelPosition();
        if (series == null) {
            series = new ArrayList<>();
        }
        if (categories == null) {
            categories = new String[0];
        }

        double yMin = 0;
        double yMax = 0;
        for (SeriesData s : series) {
            for (double v : s.ys) {
                if (v < yMin) yMin = v;
                if (v > yMax) yMax = v;
            }
        }
        if (yMin == yMax) {
            yMax = yMin + 1;
        }
        double yStep = niceStep((yMax - yMin) / 5.0);
        yMin = Math.floor(yMin / yStep) * yStep;
        yMax = Math.ceil(yMax / yStep) * yStep;
        if (yMin == yMax) {
            yMax = yMin + yStep;
        }
        SiteChartAxis yAxis = style.yAxisOrDefault();
        yStep = yAxis.stepFor(yMax - yMin);
        double[] yRange = yAxis.rangeFor(yMin, yMax, yStep);
        yMin = yRange[0];
        yMax = yRange[1];
        if (yMin == yMax) {
            yMax = yMin + yStep;
        }

        boolean pieRightOutside = pieInset != null && isPieInsetRightOutside(pieInset.position);
        LegendArea legend = legendArea(
            style,
            seriesNames(series),
            PADDING + AXIS_PAD_LEFT,
            w - PADDING - (pieRightOutside ? pieInset.size + PIE_OUTSIDE_GAP : 0),
            computeTitleBottom(title),
            h - PADDING - AXIS_PAD_BOTTOM);
        int left = legend.left();
        int right = legend.right();
        int top = legend.top();
        int bottom = legend.bottom();
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        int nCat = Math.max(1, categories.length);
        double clusterW = (double) plotW / nCat;

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w, style.titleColor());
        appendYGridAndLabels(svg, left, right, top, bottom, plotH, yMin, yMax, yAxis);
        appendCategoryXLabels(svg, categories, left, clusterW, bottom);

        // Y-axis unit label (e.g. "(t)" above Y axis)
        if (yAxisUnit != null && !yAxisUnit.isEmpty()) {
            svg.append("<text x=\"")
                .append(left - 3)
                .append("\" y=\"")
                .append(top - 2)
                .append("\" text-anchor=\"end\" font-size=\"")
                .append(CHART_TEXT_SIZE)
                .append("\" fill=\"#B8C2CF\" font-family=\"inherit\">(")
                .append(esc(yAxisUnit))
                .append(")</text>");
        }
        appendAxisLabels(svg, left, right, top, bottom, style);

        // Determine how many COLUMN series there are for bar-width calculation
        int nColSer = (int) series.stream()
            .filter(s -> SeriesData.TYPE_COLUMN.equals(s.type))
            .count();
        if (nColSer == 0) nColSer = 1;
        double barW = clusterW * style.barWidthRatio() / nColSer;
        double gap = (clusterW - barW * nColSer) / 2.0;

        int colIdx = 0;
        String labelFill = style.labelFill();
        for (SeriesData s : series) {
            if (!SeriesData.TYPE_COLUMN.equals(s.type)) continue;
            String fill = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            for (int di = 0; di < len; di++) {
                int ci = (int) s.xs[di];
                if (ci < 0 || ci >= nCat) {
                    continue;
                }
                double value = s.ys[di];
                double bx = left + ci * clusterW + gap + colIdx * barW;
                double by;
                double bh;
                double zeroY = bottom - Math.max(0, -yMin) / (yMax - yMin) * plotH;
                if (value >= 0) {
                    bh = value / (yMax - yMin) * plotH;
                    by = zeroY - bh;
                } else {
                    by = zeroY;
                    bh = -value / (yMax - yMin) * plotH;
                }
                if (bh < 0.5) {
                    bh = 0.5;
                }
                svg.append("<rect class=\"guide-chart-shape\" x=\"")
                    .append(fmtD(bx))
                    .append("\" y=\"")
                    .append(fmtD(by))
                    .append("\" width=\"")
                    .append(fmtD(barW))
                    .append("\" height=\"")
                    .append(fmtD(bh))
                    .append("\" fill=\"")
                    .append(fill)
                    .append("\"><title>")
                    .append(esc(buildChartTip(categories[ci], s.name, value)))
                    .append("</title></rect>");
                if (labelPosition != ChartLabelPosition.NONE) {
                    double labelX = bx + barW / 2;
                    double labelY = by - LABEL_ABOVE_OFFSET;
                    switch (labelPosition) {
                        case BELOW -> labelY = by + bh + LABEL_BELOW_OFFSET;
                        case INSIDE, CENTER -> labelY = by + bh / 2 + LABEL_CENTER_OFFSET;
                        case OUTSIDE -> {
                            if (value < 0) {
                                labelY = by + bh + LABEL_BELOW_OFFSET;
                            }
                        }
                        default -> {}
                    }
                    appendValueLabel(svg, labelX, labelY, "middle", value, labelFill);
                }
            }
            colIdx++;
        }

        // Line-overlay series (same Y scale as column bars)
        for (SeriesData s : series) {
            if (!SeriesData.TYPE_LINE.equals(s.type)) continue;
            String stroke = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            StringBuilder linePts = new StringBuilder();
            for (int di = 0; di < len; di++) {
                int ci = (int) s.xs[di];
                if (ci < 0 || ci >= nCat) continue;
                double cx = left + (ci + 0.5) * clusterW;
                double cy = bottom - (s.ys[di] - yMin) / (yMax - yMin) * plotH;
                if (!linePts.isEmpty()) linePts.append(" ");
                linePts.append(fmtD(cx))
                    .append(",")
                    .append(fmtD(cy));
            }
            if (!linePts.isEmpty()) {
                svg.append("<polyline class=\"guide-chart-shape\" points=\"")
                    .append(linePts)
                    .append("\" stroke=\"")
                    .append(stroke)
                    .append("\" stroke-width=\"2\" fill=\"none\"><title>")
                    .append(esc(s.name))
                    .append("</title></polyline>");
                // Dots at each data point
                for (int di = 0; di < len; di++) {
                    int ci = (int) s.xs[di];
                    if (ci < 0 || ci >= nCat) continue;
                    double cx = left + (ci + 0.5) * clusterW;
                    double cy = bottom - (s.ys[di] - yMin) / (yMax - yMin) * plotH;
                    svg.append("<circle class=\"guide-chart-shape\" cx=\"")
                        .append(fmtD(cx))
                        .append("\" cy=\"")
                        .append(fmtD(cy))
                        .append("\" r=\"3\" fill=\"")
                        .append(stroke)
                        .append("\"><title>")
                        .append(esc(buildChartTip(ci < categories.length ? categories[ci] : "", s.name, s.ys[di])))
                        .append("</title></circle>");
                }
            }
        }

        // Pie inset overlay
        if (pieInset != null && !pieInset.slices.isEmpty()) {
            double total = 0;
            for (SliceData sl : pieInset.slices) total += sl.value;
            if (total <= 0) total = 1;
            int pr = pieInset.size / 2;
            int pcx, pcy;
            String position = normalizePieInsetPosition(pieInset.position);
            pcy = switch (position) {
                case "left" -> {
                    pcx = left + pr + 4;
                    yield top + pr + 4;
                }
                case "right" -> {
                    pcx = w - PADDING - pr;
                    yield top + Math.max(pr, plotH / 2);
                }
                case "bottom-right" -> {
                    pcx = right - pr - 4;
                    yield bottom - pr - 4;
                }
                case "bottom-left" -> {
                    pcx = left + pr + 4;
                    yield bottom - pr - 4;
                }
                default -> {
                    pcx = right - pr - 4;
                    yield top + pr + 4;
                }
            };
            // Semi-transparent backing circle
            svg.append("<circle cx=\"")
                .append(pcx)
                .append("\" cy=\"")
                .append(pcy)
                .append("\" r=\"")
                .append(pr + 2)
                .append("\" fill=\"")
                .append(argbToRgba(bgColor))
                .append("\" opacity=\"0.85\"/>");
            // Pie slices
            double ang = -Math.PI / 2;
            for (SliceData sl : pieInset.slices) {
                double sweep = (sl.value / total) * 2 * Math.PI;
                double ea = ang + sweep;
                double x1 = pcx + pr * Math.cos(ang);
                double y1 = pcy + pr * Math.sin(ang);
                double x2 = pcx + pr * Math.cos(ea);
                double y2 = pcy + pr * Math.sin(ea);
                int la = sweep > Math.PI ? 1 : 0;
                double pct = (sl.value / total) * 100.0;
                svg.append("<path class=\"guide-chart-shape\" d=\"M ")
                    .append(pcx)
                    .append(" ")
                    .append(pcy)
                    .append(" L ")
                    .append(fmtD(x1))
                    .append(" ")
                    .append(fmtD(y1))
                    .append(" A ")
                    .append(pr)
                    .append(" ")
                    .append(pr)
                    .append(" 0 ")
                    .append(la)
                    .append(" 1 ")
                    .append(fmtD(x2))
                    .append(" ")
                    .append(fmtD(y2))
                    .append(" Z\" fill=\"")
                    .append(argbToRgba(sl.color))
                    .append("\" stroke=\"")
                    .append(argbToRgba(bgColor))
                    .append("\" stroke-width=\"0.5\"><title>")
                    .append(
                        esc(
                            sl.label + ": "
                                + formatNum(sl.value)
                                + " ("
                                + String.format(Locale.ROOT, "%.1f", pct)
                                + "%)"))
                    .append("</title></path>");
                ang = ea;
            }
            // Inset title
            if (pieInset.title != null && !pieInset.title.isEmpty()) {
                svg.append("<text x=\"")
                    .append(pcx)
                    .append("\" y=\"")
                    .append(pcy + pr + 9)
                    .append("\" text-anchor=\"middle\" font-size=\"")
                    .append(CHART_TEXT_SIZE)
                    .append("\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                    .append(esc(pieInset.title))
                    .append("</text>");
            }
        }

        appendYAxis(svg, left, top, bottom);
        appendXAxis(svg, left, right, bottom);
        if (legend.width() > 0) {
            renderLegend(svg, series, legend.x(), legend.y(), legend.width(), legend.limit(), legend.cols());
        }
        return svg.append("</svg></div>")
            .toString();
    }

    private static boolean isPieInsetRightOutside(@Nullable String position) {
        return "right".equals(normalizePieInsetPosition(position));
    }

    private static String normalizePieInsetPosition(@Nullable String position) {
        if (position == null) {
            return "top-right";
        }
        return switch (position.trim()
            .toLowerCase(Locale.ROOT)) {
            case "left", "topleft", "top-left", "tl" -> "left";
            case "bottomright", "bottom-right", "br" -> "bottom-right";
            case "bottomleft", "bottom-left", "bl" -> "bottom-left";
            case "right", "rightoutside", "right-outside", "outside", "side" -> "right";
            default -> "top-right";
        };
    }

    // Bar chart (horizontal bars, categorical Y).

    public static String renderBarChart(int w, int h, int bgColor, int borderColor, String title, String[] categories,
        List<SeriesData> series, boolean showLegend) {
        ChartStyle style = ChartStyle.of(w, h, bgColor, borderColor, title)
            .withLegend(showLegend ? ChartLegendPosition.TOP : ChartLegendPosition.NONE);
        return renderBarChart(style, categories, series);
    }

    public static String renderBarChart(ChartStyle style, String[] categories, List<SeriesData> series) {
        int w = style.layoutWidth();
        int h = style.layoutHeight();
        String title = style.title();
        int bgColor = style.background();
        int borderColor = style.border();
        ChartLabelPosition labelPosition = style.labelPosition();
        if (series == null) {
            series = new ArrayList<>();
        }
        if (categories == null) {
            categories = new String[0];
        }

        double xMin = 0;
        double xMax = 0;
        for (SeriesData s : series) {
            for (double v : s.ys) {
                if (v < xMin) xMin = v;
                if (v > xMax) xMax = v;
            }
        }
        if (xMin == xMax) {
            xMax = xMin + 1;
        }
        double xStep = niceStep((xMax - xMin) / 5.0);
        xMin = Math.floor(xMin / xStep) * xStep;
        xMax = Math.ceil(xMax / xStep) * xStep;
        if (xMin == xMax) {
            xMax = xMin + xStep;
        }
        SiteChartAxis barValueAxis = style.xAxisOrDefault();
        xStep = barValueAxis.stepFor(xMax - xMin);
        if (xStep <= 0 || !Double.isFinite(xStep)) {
            xStep = niceStep((xMax - xMin) / 5.0);
        }
        double[] barRange = barValueAxis.rangeFor(xMin, xMax, xStep);
        xMin = barRange[0];
        xMax = barRange[1];
        if (xMin == xMax) {
            xMax = xMin + xStep;
        }

        int titleBottom = computeTitleBottom(title);
        LegendArea legend = legendArea(
            style,
            seriesNames(series),
            PADDING + 36,
            w - PADDING,
            titleBottom,
            h - PADDING - AXIS_PAD_BOTTOM);
        int left = legend.left();
        int right = legend.right();
        int top = legend.top();
        int bottom = legend.bottom();
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        int nCat = Math.max(1, categories.length);
        int nSer = Math.max(1, series.size());
        double rowH = (double) plotH / nCat;
        double barH = rowH * style.barWidthRatio() / nSer;
        double gap = (rowH - barH * nSer) / 2.0;

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w);

        // X grid + labels at bottom
        double xStepG = xStep;
        int nGridX = (int) Math.round((xMax - xMin) / xStepG);
        nGridX = Math.clamp(nGridX, 1, 10);
        for (int gi = 0; gi <= nGridX; gi++) {
            double xv = xMin + gi * (xMax - xMin) / nGridX;
            int gx = left + (int) Math.round((xv - xMin) / (xMax - xMin) * plotW);
            if (barValueAxis.gridVisible()) {
                svg.append("<line x1=\"")
                    .append(gx)
                    .append("\" y1=\"")
                    .append(top)
                    .append("\" x2=\"")
                    .append(gx)
                    .append("\" y2=\"")
                    .append(bottom)
                    .append("\" stroke=\"")
                    .append(argbToRgba(barValueAxis.gridColor()))
                    .append("\" stroke-width=\"1\"/>");
            }
            svg.append("<text x=\"")
                .append(gx)
                .append("\" y=\"")
                .append(bottom + 10)
                .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(barValueAxis.formatTick(xv)))
                .append("</text>");
        }
        appendAxisLabels(svg, left, right, top, bottom, style);

        // Category labels on left (Y axis)
        for (int ci = 0; ci < categories.length; ci++) {
            int cy = top + (int) Math.round((ci + 0.5) * rowH);
            svg.append("<text x=\"")
                .append(left - 4)
                .append("\" y=\"")
                .append(cy + 4)
                .append("\" text-anchor=\"end\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(categories[ci]))
                .append("</text>");
        }

        double xBase = left + Math.max(0, -xMin) / (xMax - xMin) * plotW;
        String labelFill = style.labelFill();
        for (int si = 0; si < series.size(); si++) {
            SeriesData s = series.get(si);
            String fill = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            for (int di = 0; di < len; di++) {
                int ci = (int) s.xs[di];
                if (ci < 0 || ci >= nCat) {
                    continue;
                }
                double value = s.ys[di];
                double by = top + ci * rowH + gap + si * barH;
                double bw;
                double bx;
                if (value >= 0) {
                    bx = xBase;
                    bw = value / (xMax - xMin) * plotW;
                } else {
                    bw = -value / (xMax - xMin) * plotW;
                    bx = xBase - bw;
                }
                if (bw < 0.5) {
                    bw = 0.5;
                }
                svg.append("<rect class=\"guide-chart-shape\" x=\"")
                    .append(fmtD(bx))
                    .append("\" y=\"")
                    .append(fmtD(by))
                    .append("\" width=\"")
                    .append(fmtD(bw))
                    .append("\" height=\"")
                    .append(fmtD(barH))
                    .append("\" fill=\"")
                    .append(fill)
                    .append("\"><title>")
                    .append(esc(buildChartTip(ci < categories.length ? categories[ci] : "", s.name, value)))
                    .append("</title></rect>");
                if (labelPosition != ChartLabelPosition.NONE) {
                    double barEnd = value >= 0 ? bx + bw : bx;
                    double labelY = by + barH / 2 + LABEL_CENTER_OFFSET;
                    switch (labelPosition) {
                        case ABOVE -> labelY = by - LABEL_ABOVE_OFFSET;
                        case BELOW -> labelY = by + barH + LABEL_BELOW_OFFSET;
                        case OUTSIDE -> {
                            double labelX = value >= 0 ? barEnd + 3 : barEnd - 3;
                            appendValueLabel(svg, labelX, labelY, value >= 0 ? "start" : "end", value, labelFill);
                            continue;
                        }
                        default -> {}
                    }
                    appendValueLabel(svg, bx + bw / 2, labelY, "middle", value, labelFill);
                }
            }
        }

        // Axis lines
        svg.append("<line x1=\"")
            .append(fmtD(xBase))
            .append("\" y1=\"")
            .append(top)
            .append("\" x2=\"")
            .append(fmtD(xBase))
            .append("\" y2=\"")
            .append(bottom)
            .append("\" stroke=\"#B8C2CF\" stroke-width=\"1\"/>");
        appendXAxis(svg, left, right, bottom);

        if (legend.width() > 0) {
            renderLegend(svg, series, legend.x(), legend.y(), legend.width(), legend.limit(), legend.cols());
        }
        return svg.append("</svg></div>")
            .toString();
    }

    // Line chart.

    public static String renderLineChart(int w, int h, int bgColor, int borderColor, String title, String[] categories,
        List<SeriesData> series, boolean numericX, boolean showPoints, boolean showLegend) {
        return renderLineChart(
            w,
            h,
            bgColor,
            borderColor,
            title,
            categories,
            series,
            numericX,
            showPoints,
            showLegend,
            CornerLegendPosition.NONE,
            CornerLegendRenderer.DEFAULT_WIDTH,
            CornerLegendRenderer.DEFAULT_HEIGHT,
            CornerLegendRenderer.DEFAULT_BACKGROUND);
    }

    public static String renderLineChart(int w, int h, int bgColor, int borderColor, String title, String[] categories,
        List<SeriesData> series, boolean numericX, boolean showPoints, boolean showLegend,
        CornerLegendPosition cornerLegendPosition, int cornerLegendWidth, int cornerLegendHeight,
        int cornerLegendBackgroundColor) {
        ChartStyle style = ChartStyle.of(w, h, bgColor, borderColor, title)
            .withLegend(showLegend ? ChartLegendPosition.TOP : ChartLegendPosition.NONE);
        return renderLineChart(
            style,
            categories,
            series,
            numericX,
            showPoints,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor);
    }

    public static String renderLineChart(ChartStyle style, String[] categories, List<SeriesData> series,
        boolean numericX, boolean showPoints, CornerLegendPosition cornerLegendPosition, int cornerLegendWidth,
        int cornerLegendHeight, int cornerLegendBackgroundColor) {
        int w = style.layoutWidth();
        int h = style.layoutHeight();
        String title = style.title();
        int bgColor = style.background();
        int borderColor = style.border();
        ChartLabelPosition labelPosition = style.labelPosition();
        if (series == null) {
            series = new ArrayList<>();
        }
        if (categories == null) {
            categories = new String[0];
        }

        double xMin = 0;
        double xMax = 0;
        double yMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        int maxIdx = categories.length;
        for (SeriesData s : series) {
            for (double v : s.ys) {
                if (v < yMin) yMin = v;
                if (v > yMax) yMax = v;
            }
            if (numericX) {
                for (double v : s.xs) {
                    if (v < xMin) xMin = v;
                    if (v > xMax) xMax = v;
                }
            } else {
                for (double v : s.xs) {
                    int idx = (int) v;
                    if (idx >= maxIdx) maxIdx = idx + 1;
                }
            }
        }
        if (!numericX) {
            xMin = 0;
            xMax = Math.max(1, maxIdx - 1);
        }
        if (xMin == xMax) {
            xMax = xMin + 1;
        }
        if (!Double.isFinite(yMin)) {
            yMin = 0;
            yMax = 1;
        }
        if (yMin == yMax) {
            yMin -= 0.5;
            yMax += 0.5;
        }
        double yRange = yMax - yMin;
        yMin -= yRange * 0.05;
        yMax += yRange * 0.05;
        SiteChartAxis lineYAxis = style.yAxisOrDefault();
        double[] declaredY = lineYAxis.rangeFor(yMin, yMax, lineYAxis.stepFor(yMax - yMin));
        if (lineYAxis.min() != null || lineYAxis.max() != null) {
            yMin = declaredY[0];
            yMax = declaredY[1];
        }

        LegendArea legend = legendArea(
            style,
            seriesNames(series),
            PADDING + AXIS_PAD_LEFT,
            w - PADDING,
            computeTitleBottom(title),
            h - PADDING - AXIS_PAD_BOTTOM);
        int left = legend.left();
        int right = legend.right();
        int top = legend.top();
        int bottom = legend.bottom();
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w, style.titleColor());
        appendAxisLabels(svg, left, right, top, bottom, style);

        // Y grid + labels
        double yStep = lineYAxis.stepFor(yMax - yMin);
        if (yStep <= 0 || !Double.isFinite(yStep)) {
            yStep = niceStep((yMax - yMin) / 5.0);
        }
        for (double yv = Math.floor(yMin / yStep) * yStep; yv <= yMax + yStep * 0.01; yv += yStep) {
            int gy = bottom - (int) Math.round((yv - yMin) / (yMax - yMin) * plotH);
            if (gy < top - 2 || gy > bottom + 2) {
                continue;
            }
            if (lineYAxis.gridVisible()) {
                svg.append("<line x1=\"")
                    .append(left)
                    .append("\" y1=\"")
                    .append(gy)
                    .append("\" x2=\"")
                    .append(right)
                    .append("\" y2=\"")
                    .append(gy)
                    .append("\" stroke=\"")
                    .append(argbToRgba(lineYAxis.gridColor()))
                    .append("\" stroke-width=\"1\"/>");
            }
            svg.append("<text x=\"")
                .append(left - 3)
                .append("\" y=\"")
                .append(gy + 4)
                .append("\" text-anchor=\"end\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(lineYAxis.formatTick(yv)))
                .append("</text>");
        }

        // X axis labels
        if (!numericX) {
            for (int ci = 0; ci < maxIdx && ci < categories.length; ci++) {
                int cx = left + (int) Math.round((double) ci / Math.max(1, maxIdx - 1) * plotW);
                svg.append("<text x=\"")
                    .append(cx)
                    .append("\" y=\"")
                    .append(bottom + 10)
                    .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                    .append(esc(categories[ci]))
                    .append("</text>");
            }
        } else {
            double xStep = niceStep((xMax - xMin) / 5.0);
            for (double xv = Math.floor(xMin / xStep) * xStep; xv <= xMax + xStep * 0.01; xv += xStep) {
                int gx = left + (int) Math.round((xv - xMin) / (xMax - xMin) * plotW);
                if (gx < left - 2 || gx > right + 2) {
                    continue;
                }
                svg.append("<line x1=\"")
                    .append(gx)
                    .append("\" y1=\"")
                    .append(top)
                    .append("\" x2=\"")
                    .append(gx)
                    .append("\" y2=\"")
                    .append(bottom)
                    .append("\" stroke=\"#3A4047\" stroke-width=\"1\"/>");
                svg.append("<text x=\"")
                    .append(gx)
                    .append("\" y=\"")
                    .append(bottom + 10)
                    .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                    .append(esc(formatNum(xv)))
                    .append("</text>");
            }
        }

        // Lines + optional points
        String labelFill = style.labelFill();
        for (SeriesData s : series) {
            String stroke = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            if (len == 0) {
                continue;
            }
            StringBuilder pts = new StringBuilder();
            for (int i = 0; i < len; i++) {
                int px = left + (int) Math.round((s.xs[i] - xMin) / (xMax - xMin) * plotW);
                int py = bottom - (int) Math.round((s.ys[i] - yMin) / (yMax - yMin) * plotH);
                if (i > 0) {
                    pts.append(" ");
                }
                pts.append(px)
                    .append(",")
                    .append(py);
            }
            svg.append("<polyline class=\"guide-chart-shape\" points=\"")
                .append(pts)
                .append("\" stroke=\"")
                .append(stroke)
                .append("\" stroke-width=\"1.5\" fill=\"none\"><title>")
                .append(esc(s.name))
                .append("</title></polyline>");
            if (showPoints) {
                for (int i = 0; i < len; i++) {
                    int px = left + (int) Math.round((s.xs[i] - xMin) / (xMax - xMin) * plotW);
                    int py = bottom - (int) Math.round((s.ys[i] - yMin) / (yMax - yMin) * plotH);
                    String pointTip = buildChartTip(
                        numericX ? formatChartValue(s.xs[i])
                            : ((int) s.xs[i] >= 0 && (int) s.xs[i] < categories.length ? categories[(int) s.xs[i]]
                                : ""),
                        s.name,
                        s.ys[i]);
                    svg.append("<circle class=\"guide-chart-shape\" cx=\"")
                        .append(px)
                        .append("\" cy=\"")
                        .append(py)
                        .append("\" r=\"2\" fill=\"")
                        .append(stroke)
                        .append("\"><title>")
                        .append(esc(pointTip))
                        .append("</title></circle>");
                }
            }
            if (labelPosition != ChartLabelPosition.NONE) {
                for (int i = 0; i < len; i++) {
                    int px = left + (int) Math.round((s.xs[i] - xMin) / (xMax - xMin) * plotW);
                    int py = bottom - (int) Math.round((s.ys[i] - yMin) / (yMax - yMin) * plotH);
                    int labelY;
                    switch (labelPosition) {
                        case ABOVE, OUTSIDE -> labelY = py - LABEL_ABOVE_OFFSET;
                        case BELOW -> labelY = py + LABEL_BELOW_OFFSET;
                        case CENTER, INSIDE -> labelY = py + LABEL_CENTER_OFFSET;
                        default -> {
                            continue;
                        }
                    }
                    appendValueLabel(svg, px, labelY, "middle", s.ys[i], labelFill);
                }
            }
        }

        appendYAxis(svg, left, top, bottom);
        appendXAxis(svg, left, right, bottom);
        renderChartCornerLegend(
            svg,
            series,
            left,
            right,
            top,
            bottom,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor,
            true);
        if (legend.width() > 0) {
            renderLegend(svg, series, legend.x(), legend.y(), legend.width(), legend.limit(), legend.cols());
        }
        return svg.append("</svg></div>")
            .toString();
    }

    // Pie chart.

    public static String renderPieChart(int w, int h, int bgColor, int borderColor, String title,
        List<SliceData> slices, boolean showLegend) {
        ChartStyle style = ChartStyle.of(w, h, bgColor, borderColor, title)
            .withLegend(showLegend ? ChartLegendPosition.TOP : ChartLegendPosition.NONE);
        return renderPieChart(style, slices);
    }

    public static String renderPieChart(ChartStyle style, List<SliceData> slices) {
        int w = style.layoutWidth();
        int h = style.layoutHeight();
        String title = style.title();
        int bgColor = style.background();
        int borderColor = style.border();
        ChartLabelPosition labelPosition = style.labelPosition();
        if (slices == null) {
            slices = new ArrayList<>();
        }

        double total = 0;
        for (SliceData s : slices) {
            total += Math.max(0, s.value);
        }
        if (total <= 0) {
            total = 1;
        }

        List<SeriesData> legendItems = new ArrayList<>();
        for (SliceData s : slices) {
            legendItems.add(new SeriesData(s.label, s.color, new double[0], new double[0]));
        }
        LegendArea legend = legendArea(
            style,
            seriesNames(legendItems),
            PADDING,
            w - PADDING,
            computeTitleBottom(title),
            h - PADDING);
        int left = legend.left();
        int right = legend.right();
        int top = legend.top();
        int bottom = legend.bottom();
        int plotW = right - left;
        int plotH = bottom - top;

        int cx = left + plotW / 2;
        int cy = top + plotH / 2;
        int r = Math.min(plotW, plotH) / 2 - 4;
        if (r < 4) {
            r = 4;
        }

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w, style.titleColor());

        double startAngle = Math.toRadians(style.pieStartAngleDeg());
        double sweepDirection = style.pieClockwise() ? 1d : -1d;
        String labelFill = style.labelFill();
        for (SliceData s : slices) {
            double sweep = (s.value / total) * 2 * Math.PI * sweepDirection;
            double endAngle = startAngle + sweep;
            double x1 = cx + r * Math.cos(startAngle);
            double y1 = cy + r * Math.sin(startAngle);
            double x2 = cx + r * Math.cos(endAngle);
            double y2 = cy + r * Math.sin(endAngle);
            int largeArc = sweep > Math.PI ? 1 : 0;
            double pct = total > 0 ? (s.value / total) * 100.0 : 0;
            svg.append("<path class=\"guide-chart-shape\" d=\"M ")
                .append(cx)
                .append(" ")
                .append(cy)
                .append(" L ")
                .append(fmtD(x1))
                .append(" ")
                .append(fmtD(y1))
                .append(" A ")
                .append(r)
                .append(" ")
                .append(r)
                .append(" 0 ")
                .append(largeArc)
                .append(" 1 ")
                .append(fmtD(x2))
                .append(" ")
                .append(fmtD(y2))
                .append(" Z\" fill=\"")
                .append(argbToRgba(s.color))
                .append("\" stroke=\"")
                .append(argbToRgba(bgColor))
                .append("\" stroke-width=\"0.5\"><title>")
                .append(
                    esc(
                        s.label + ": "
                            + formatChartValue(s.value)
                            + " ("
                            + String.format(Locale.ROOT, "%.1f", pct)
                            + "%)"))
                .append("</title></path>");
            if (labelPosition != ChartLabelPosition.NONE) {
                boolean outside = labelPosition == ChartLabelPosition.OUTSIDE
                    || labelPosition == ChartLabelPosition.ABOVE
                    || labelPosition == ChartLabelPosition.BELOW;
                String percent = formatChartPercent(s.value / total);
                String text = outside ? s.label + " " + percent : percent;
                double mid = (startAngle + endAngle) / 2;
                double labelR = outside ? r + 4 : r * 0.6;
                int textWidth = approximateTextWidth(text);
                double labelX = Math
                    .clamp(cx + Math.cos(mid) * labelR - textWidth / 2.0, left, Math.max(left, right - textWidth));
                double labelY = Math
                    .clamp(cy + Math.sin(mid) * labelR + LABEL_CENTER_OFFSET, top + CHART_TEXT_SIZE, bottom);
                appendText(svg, labelX, labelY, "start", text, style.labelFill());
            }
            startAngle = endAngle;
        }

        if (!slices.isEmpty() && legend.width() > 0) {
            renderLegend(svg, legendItems, legend.x(), legend.y(), legend.width(), legend.limit(), legend.cols());
        }
        return svg.append("</svg></div>")
            .toString();
    }

    // Scatter chart.

    public static String renderScatterChart(int w, int h, int bgColor, int borderColor, String title,
        List<SeriesData> series, boolean showLegend) {
        return renderScatterChart(
            w,
            h,
            bgColor,
            borderColor,
            title,
            series,
            showLegend,
            CornerLegendPosition.NONE,
            CornerLegendRenderer.DEFAULT_WIDTH,
            CornerLegendRenderer.DEFAULT_HEIGHT,
            CornerLegendRenderer.DEFAULT_BACKGROUND);
    }

    public static String renderScatterChart(int w, int h, int bgColor, int borderColor, String title,
        List<SeriesData> series, boolean showLegend, CornerLegendPosition cornerLegendPosition, int cornerLegendWidth,
        int cornerLegendHeight, int cornerLegendBackgroundColor) {
        ChartStyle style = ChartStyle.of(w, h, bgColor, borderColor, title)
            .withLegend(showLegend ? ChartLegendPosition.TOP : ChartLegendPosition.NONE);
        return renderScatterChart(
            style,
            series,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor);
    }

    public static String renderScatterChart(ChartStyle style, List<SeriesData> series,
        CornerLegendPosition cornerLegendPosition, int cornerLegendWidth, int cornerLegendHeight,
        int cornerLegendBackgroundColor) {
        int w = style.layoutWidth();
        int h = style.layoutHeight();
        String title = style.title();
        int bgColor = style.background();
        int borderColor = style.border();
        ChartLabelPosition labelPosition = style.labelPosition();
        if (series == null) {
            series = new ArrayList<>();
        }

        double xMin = Double.MAX_VALUE;
        double xMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        for (SeriesData s : series) {
            for (double v : s.xs) {
                if (v < xMin) xMin = v;
                if (v > xMax) xMax = v;
            }
            for (double v : s.ys) {
                if (v < yMin) yMin = v;
                if (v > yMax) yMax = v;
            }
        }
        if (!Double.isFinite(xMin)) {
            xMin = 0;
            xMax = 1;
        }
        if (!Double.isFinite(yMin)) {
            yMin = 0;
            yMax = 1;
        }
        if (xMin == xMax) {
            xMin -= 0.5;
            xMax += 0.5;
        }
        if (yMin == yMax) {
            yMin -= 0.5;
            yMax += 0.5;
        }
        double xPad = (xMax - xMin) * 0.05;
        double yPad = (yMax - yMin) * 0.05;
        xMin -= xPad;
        xMax += xPad;
        yMin -= yPad;
        yMax += yPad;
        SiteChartAxis scatterXAxis = style.xAxisOrDefault();
        SiteChartAxis scatterYAxis = style.yAxisOrDefault();
        double[] declaredX = scatterXAxis.rangeFor(xMin, xMax, scatterXAxis.stepFor(xMax - xMin));
        double[] declaredY = scatterYAxis.rangeFor(yMin, yMax, scatterYAxis.stepFor(yMax - yMin));
        if (scatterXAxis.min() != null || scatterXAxis.max() != null) {
            xMin = declaredX[0];
            xMax = declaredX[1];
        }
        if (scatterYAxis.min() != null || scatterYAxis.max() != null) {
            yMin = declaredY[0];
            yMax = declaredY[1];
        }

        LegendArea legend = legendArea(
            style,
            seriesNames(series),
            PADDING + AXIS_PAD_LEFT,
            w - PADDING,
            computeTitleBottom(title),
            h - PADDING - AXIS_PAD_BOTTOM);
        int left = legend.left();
        int right = legend.right();
        int top = legend.top();
        int bottom = legend.bottom();
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w);

        // X grid + labels
        double xStep = scatterXAxis.stepFor(xMax - xMin);
        if (xStep <= 0 || !Double.isFinite(xStep)) {
            xStep = niceStep((xMax - xMin) / 5.0);
        }
        for (double xv = Math.floor(xMin / xStep) * xStep; xv <= xMax + xStep * 0.01; xv += xStep) {
            int gx = left + (int) Math.round((xv - xMin) / (xMax - xMin) * plotW);
            if (gx < left - 2 || gx > right + 2) {
                continue;
            }
            if (scatterXAxis.gridVisible()) {
                svg.append("<line x1=\"")
                    .append(gx)
                    .append("\" y1=\"")
                    .append(top)
                    .append("\" x2=\"")
                    .append(gx)
                    .append("\" y2=\"")
                    .append(bottom)
                    .append("\" stroke=\"")
                    .append(argbToRgba(scatterXAxis.gridColor()))
                    .append("\" stroke-width=\"1\"/>");
            }
            svg.append("<text x=\"")
                .append(gx)
                .append("\" y=\"")
                .append(bottom + 10)
                .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(scatterXAxis.formatTick(xv)))
                .append("</text>");
        }
        appendYGridAndLabels(svg, left, right, top, bottom, plotH, yMin, yMax, scatterYAxis);
        appendAxisLabels(svg, left, right, top, bottom, style);

        // Scatter points
        String labelFill = style.labelFill();
        for (SeriesData s : series) {
            String fill = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            for (int i = 0; i < len; i++) {
                int px = left + (int) Math.round((s.xs[i] - xMin) / (xMax - xMin) * plotW);
                int py = bottom - (int) Math.round((s.ys[i] - yMin) / (yMax - yMin) * plotH);
                svg.append("<circle class=\"guide-chart-shape\" cx=\"")
                    .append(px)
                    .append("\" cy=\"")
                    .append(py)
                    .append("\" r=\"3\" fill=\"")
                    .append(fill)
                    .append("\"><title>")
                    .append(
                        esc(
                            (s.name.isEmpty() ? "" : s.name + ": ") + "("
                                + formatChartValue(s.xs[i])
                                + ", "
                                + formatChartValue(s.ys[i])
                                + ")"))
                    .append("</title></circle>");
                if (labelPosition != ChartLabelPosition.NONE) {
                    int labelY;
                    switch (labelPosition) {
                        case ABOVE, OUTSIDE -> labelY = py - LABEL_ABOVE_OFFSET;
                        case BELOW -> labelY = py + LABEL_BELOW_OFFSET;
                        default -> {
                            continue;
                        }
                    }
                    appendText(
                        svg,
                        px,
                        labelY,
                        "middle",
                        "(" + formatChartValue(s.xs[i]) + "," + formatChartValue(s.ys[i]) + ")",
                        labelFill);
                }
            }
        }

        appendYAxis(svg, left, top, bottom);
        appendXAxis(svg, left, right, bottom);
        renderChartCornerLegend(
            svg,
            series,
            left,
            right,
            top,
            bottom,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor,
            false);
        if (legend.width() > 0) {
            renderLegend(svg, series, legend.x(), legend.y(), legend.width(), legend.limit(), legend.cols());
        }
        return svg.append("</svg></div>")
            .toString();
    }

    // Function graph.

    public static String renderFunctionGraph(LytFunctionGraph graph) {
        return renderFunctionGraph(graph, null);
    }

    public static String renderFunctionGraph(LytFunctionGraph graph, @Nullable GuideSiteLatexExporter latexExporter) {
        int w = graph.getExplicitWidth() > 0 ? graph.getExplicitWidth() : GRAPH_DEFAULT_W;
        int h = graph.getExplicitHeight() > 0 ? graph.getExplicitHeight() : GRAPH_DEFAULT_H;
        String title = graph.getTitle();
        int bgColor = graph.getBackgroundColor();
        int borderColor = graph.getBorderColor();
        int axisColor = graph.getAxisColor();
        int gridColor = graph.getGridColor();
        boolean showGrid = graph.isShowGrid();
        boolean showAxes = graph.isShowAxes();
        List<FunctionPlot> plots = graph.getPlots();

        double xMin = Double.isNaN(graph.getExplicitXMin()) ? -10 : graph.getExplicitXMin();
        double xMax = Double.isNaN(graph.getExplicitXMax()) ? 10 : graph.getExplicitXMax();
        double yMin = Double.isNaN(graph.getExplicitYMin()) ? Double.NaN : graph.getExplicitYMin();
        double yMax = Double.isNaN(graph.getExplicitYMax()) ? Double.NaN : graph.getExplicitYMax();

        if (Double.isNaN(yMin) || Double.isNaN(yMax)) {
            double autoYMin = Double.MAX_VALUE;
            double autoYMax = -Double.MAX_VALUE;
            for (FunctionPlot plot : plots) {
                for (int i = 0; i <= N_SAMPLES; i++) {
                    double x = xMin + (xMax - xMin) * i / N_SAMPLES;
                    double y = plot.evaluate(x);
                    if (Double.isFinite(y)) {
                        if (y < autoYMin) autoYMin = y;
                        if (y > autoYMax) autoYMax = y;
                    }
                }
            }
            if (!Double.isFinite(autoYMin)) {
                autoYMin = xMin;
                autoYMax = xMax;
            }
            if (autoYMin == autoYMax) {
                autoYMin -= 1;
                autoYMax += 1;
            }
            double margin = (autoYMax - autoYMin) * 0.1;
            if (Double.isNaN(yMin)) yMin = autoYMin - margin;
            if (Double.isNaN(yMax)) yMax = autoYMax + margin;
        }

        return renderFunctionGraphSvg(
            plots,
            graph.getPoints(),
            w,
            h,
            title,
            graph.getXLabel(),
            graph.getYLabel(),
            bgColor,
            borderColor,
            axisColor,
            gridColor,
            showGrid,
            showAxes,
            xMin,
            xMax,
            yMin,
            yMax,
            graph.getCornerLegendPosition(),
            graph.getCornerLegendWidth(),
            graph.getCornerLegendHeight(),
            graph.getCornerLegendBackgroundColor(),
            latexExporter);
    }

    public static String renderFunctionGraphSvg(List<FunctionPlot> plots, List<MarkedPoint> points, int w, int h,
        String title, int bgColor, int borderColor, int axisColor, int gridColor, boolean showGrid, boolean showAxes,
        double xMin, double xMax, double yMin, double yMax) {
        return renderFunctionGraphSvg(
            plots,
            points,
            w,
            h,
            title,
            null,
            null,
            bgColor,
            borderColor,
            axisColor,
            gridColor,
            showGrid,
            showAxes,
            xMin,
            xMax,
            yMin,
            yMax,
            CornerLegendPosition.NONE,
            CornerLegendRenderer.DEFAULT_WIDTH,
            CornerLegendRenderer.DEFAULT_HEIGHT,
            CornerLegendRenderer.DEFAULT_BACKGROUND,
            null);
    }

    public static String renderFunctionGraphSvg(List<FunctionPlot> plots, List<MarkedPoint> points, int w, int h,
        String title, int bgColor, int borderColor, int axisColor, int gridColor, boolean showGrid, boolean showAxes,
        double xMin, double xMax, double yMin, double yMax, CornerLegendPosition cornerLegendPosition,
        int cornerLegendWidth, int cornerLegendHeight, int cornerLegendBackgroundColor,
        @Nullable GuideSiteLatexExporter latexExporter) {

        return renderFunctionGraphSvg(
            plots,
            points,
            w,
            h,
            title,
            null,
            null,
            bgColor,
            borderColor,
            axisColor,
            gridColor,
            showGrid,
            showAxes,
            xMin,
            xMax,
            yMin,
            yMax,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor,
            latexExporter);
    }

    public static String renderFunctionGraphSvg(List<FunctionPlot> plots, List<MarkedPoint> points, int w, int h,
        String title, String xLabel, String yLabel, int bgColor, int borderColor, int axisColor, int gridColor,
        boolean showGrid, boolean showAxes, double xMin, double xMax, double yMin, double yMax,
        CornerLegendPosition cornerLegendPosition, int cornerLegendWidth, int cornerLegendHeight,
        int cornerLegendBackgroundColor, @Nullable GuideSiteLatexExporter latexExporter) {

        int titleBottom = computeTitleBottom(title);
        int leftPad = showAxes ? GRAPH_AXIS_PAD_LEFT : PADDING;
        int bottomPad = showAxes ? GRAPH_AXIS_PAD_BOTTOM : PADDING;
        List<SeriesData> legendItems = new ArrayList<>();
        for (FunctionPlot plot : plots) {
            if (hasText(plot.getLabel())) {
                legendItems.add(new SeriesData(plot.getLabel(), plot.getColor(), new double[0], new double[0]));
            }
        }
        int legendH = computeLegendH(legendItems, !legendItems.isEmpty(), w);

        int left = PADDING + leftPad;
        int right = w - PADDING;
        int top = titleBottom + (hasText(yLabel) ? AXIS_TITLE_H + AXIS_TITLE_GAP : 0);
        int bottom = h - PADDING - bottomPad - legendH - (hasText(xLabel) ? AXIS_TITLE_H + AXIS_TITLE_GAP : 0);
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        StringBuilder svg = openSvg(w, h, "guide-function-graph", bgColor, borderColor);
        // Chart hover JS reads plot bounds and domains from metadata.
        svg.append("<metadata data-plot-domain=\"true\" data-x-min=\"")
            .append(xMin)
            .append("\" data-x-max=\"")
            .append(xMax)
            .append("\" data-y-min=\"")
            .append(yMin)
            .append("\" data-y-max=\"")
            .append(yMax)
            .append("\" data-plot-left=\"")
            .append(left)
            .append("\" data-plot-right=\"")
            .append(right)
            .append("\" data-plot-top=\"")
            .append(top)
            .append("\" data-plot-bottom=\"")
            .append(bottom)
            .append("\"></metadata>");

        String clipId = "fg" + nextClipId.incrementAndGet();
        svg.append("<defs><clipPath id=\"")
            .append(clipId)
            .append("\"><rect x=\"")
            .append(left)
            .append("\" y=\"")
            .append(top)
            .append("\" width=\"")
            .append(plotW)
            .append("\" height=\"")
            .append(plotH)
            .append("\"/></clipPath></defs>");

        appendTitle(svg, title, w);
        appendFunctionGraphLabel(svg, yLabel, left, top - AXIS_TITLE_GAP, false, axisColor, latexExporter);

        String gridCss = argbToRgba(gridColor);
        String axisCss = argbToRgba(axisColor);

        if (showGrid) {
            double xStep = niceStep((xMax - xMin) / 6.0);
            double yStep = niceStep((yMax - yMin) / 6.0);
            for (double xv = Math.floor(xMin / xStep) * xStep; xv <= xMax + xStep * 0.01; xv += xStep) {
                int gx = left + (int) Math.round((xv - xMin) / (xMax - xMin) * plotW);
                if (gx < left || gx > right) {
                    continue;
                }
                svg.append("<line x1=\"")
                    .append(gx)
                    .append("\" y1=\"")
                    .append(top)
                    .append("\" x2=\"")
                    .append(gx)
                    .append("\" y2=\"")
                    .append(bottom)
                    .append("\" stroke=\"")
                    .append(gridCss)
                    .append("\" stroke-width=\"1\"/>");
            }
            for (double yv = Math.floor(yMin / yStep) * yStep; yv <= yMax + yStep * 0.01; yv += yStep) {
                int gy = bottom - (int) Math.round((yv - yMin) / (yMax - yMin) * plotH);
                if (gy < top || gy > bottom) {
                    continue;
                }
                svg.append("<line x1=\"")
                    .append(left)
                    .append("\" y1=\"")
                    .append(gy)
                    .append("\" x2=\"")
                    .append(right)
                    .append("\" y2=\"")
                    .append(gy)
                    .append("\" stroke=\"")
                    .append(gridCss)
                    .append("\" stroke-width=\"1\"/>");
            }
        }

        if (showAxes) {
            double yStep = niceStep((yMax - yMin) / 5.0);
            for (double yv = Math.floor(yMin / yStep) * yStep; yv <= yMax + yStep * 0.01; yv += yStep) {
                int gy = bottom - (int) Math.round((yv - yMin) / (yMax - yMin) * plotH);
                if (gy < top || gy > bottom) {
                    continue;
                }
                svg.append("<text x=\"")
                    .append(left - 3)
                    .append("\" y=\"")
                    .append(gy + 4)
                    .append("\" text-anchor=\"end\" font-size=\"8\" fill=\"")
                    .append(axisCss)
                    .append("\" font-family=\"inherit\">")
                    .append(esc(formatNum(yv)))
                    .append("</text>");
            }
            double xStep = niceStep((xMax - xMin) / 5.0);
            for (double xv = Math.floor(xMin / xStep) * xStep; xv <= xMax + xStep * 0.01; xv += xStep) {
                int gx = left + (int) Math.round((xv - xMin) / (xMax - xMin) * plotW);
                if (gx < left || gx > right) {
                    continue;
                }
                svg.append("<text x=\"")
                    .append(gx)
                    .append("\" y=\"")
                    .append(bottom + 10)
                    .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"")
                    .append(axisCss)
                    .append("\" font-family=\"inherit\">")
                    .append(esc(formatNum(xv)))
                    .append("</text>");
            }
            // Zero-crossing axis lines.
            if (yMin <= 0 && yMax >= 0) {
                int ay = bottom - (int) Math.round((0 - yMin) / (yMax - yMin) * plotH);
                svg.append("<line x1=\"")
                    .append(left)
                    .append("\" y1=\"")
                    .append(ay)
                    .append("\" x2=\"")
                    .append(right)
                    .append("\" y2=\"")
                    .append(ay)
                    .append("\" stroke=\"")
                    .append(axisCss)
                    .append("\" stroke-width=\"1\"/>");
            }
            if (xMin <= 0 && xMax >= 0) {
                int ax = left + (int) Math.round((0 - xMin) / (xMax - xMin) * plotW);
                svg.append("<line x1=\"")
                    .append(ax)
                    .append("\" y1=\"")
                    .append(top)
                    .append("\" x2=\"")
                    .append(ax)
                    .append("\" y2=\"")
                    .append(bottom)
                    .append("\" stroke=\"")
                    .append(axisCss)
                    .append("\" stroke-width=\"1\"/>");
            }
        }

        // Function curves (clipped)
        svg.append("<g clip-path=\"url(#")
            .append(clipId)
            .append(")\">");
        for (FunctionPlot plot : plots) {
            String stroke = argbToRgba(plot.getColor());
            StringBuilder pts = new StringBuilder();
            boolean inSeg = false;
            for (int i = 0; i <= N_SAMPLES; i++) {
                double x = xMin + (xMax - xMin) * i / N_SAMPLES;
                double y = plot.evaluate(x);
                if (!Double.isFinite(y)) {
                    if (inSeg && !pts.isEmpty()) {
                        flushPolyline(svg, pts, stroke, plot);
                        pts.setLength(0);
                        inSeg = false;
                    }
                    continue;
                }
                int px = left + (int) Math.round((x - xMin) / (xMax - xMin) * plotW);
                int py = bottom - (int) Math.round((y - yMin) / (yMax - yMin) * plotH);
                if (inSeg) {
                    pts.append(" ");
                }
                pts.append(px)
                    .append(",")
                    .append(py);
                inSeg = true;
            }
            if (inSeg && !pts.isEmpty()) {
                flushPolyline(svg, pts, stroke, plot);
            }
        }

        renderFunctionGraphAutoPoints(svg, plots, left, right, top, bottom, plotW, plotH, xMin, xMax, yMin, yMax);
        // Explicit marked points
        if (points != null) {
            for (MarkedPoint pt : points) {
                if (pt.getMode() == MarkedPoint.MODE_EXPLICIT) {
                    double pxVal = pt.getValueA();
                    double pyVal = pt.getValueB();
                    if (Double.isFinite(pxVal) && Double.isFinite(pyVal)) {
                        int px = left + (int) Math.round((pxVal - xMin) / (xMax - xMin) * plotW);
                        int py = bottom - (int) Math.round((pyVal - yMin) / (yMax - yMin) * plotH);
                        int pColor = pt.getColor();
                        if (pt.isColorInherit() && !plots.isEmpty()) {
                            int idx = Math.clamp(pt.getPlotIndex(), 0, plots.size() - 1);
                            pColor = plots.get(idx)
                                .getColor();
                        }
                        svg.append("<circle class=\"guide-chart-shape\" cx=\"")
                            .append(px)
                            .append("\" cy=\"")
                            .append(py)
                            .append("\" r=\"3\" fill=\"")
                            .append(argbToRgba(pColor))
                            .append("\"><title>")
                            .append(
                                esc(
                                    (pt.getLabel() != null && !pt.getLabel()
                                        .isEmpty() ? pt.getLabel() + ": " : "") + "("
                                        + formatNum(pxVal)
                                        + ", "
                                        + formatNum(pyVal)
                                        + ")"))
                            .append("</title></circle>");
                    }
                }
            }
        }

        svg.append("</g>");
        renderFunctionGraphCornerLegend(
            svg,
            plots,
            left,
            right,
            top,
            bottom,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor);
        if (!legendItems.isEmpty()) {
            renderLegend(
                svg,
                legendItems,
                left,
                bottom + bottomPad + LEGEND_GAP,
                w - 2 * PADDING,
                bottom + bottomPad + legendH);
        }
        appendFunctionGraphLabel(
            svg,
            xLabel,
            left + plotW / 2,
            bottom + bottomPad + legendH,
            true,
            axisColor,
            latexExporter);
        return svg.append("</svg></div>")
            .toString();
    }

    private static void appendFunctionGraphLabel(StringBuilder svg, @Nullable String text, int x, int baseline,
        boolean centered, int color, @Nullable GuideSiteLatexExporter latexExporter) {
        if (!hasText(text)) {
            return;
        }
        List<FunctionGraphLabelSegment> segments = new ArrayList<>();
        int totalWidth = 0;
        for (MarkdownLatexShorthand.Segment segment : MarkdownLatexShorthand.split(text)) {
            GuideSiteLatexExporter.ExportedLatex exported = segment.isFormula() && latexExporter != null
                ? latexExporter.export(segment.getValue(), color, 100f)
                : null;
            String fallback = segment.isFormula() ? "$$" + segment.getValue() + "$$" : segment.getValue();
            int width = exported != null
                ? Math.max(1, Math.round((float) exported.widthPx() * 10f / exported.referenceHeightPx()))
                : estimateFunctionGraphLabelWidth(fallback);
            int height = exported != null
                ? Math.max(1, Math.round((float) exported.heightPx() * 10f / exported.referenceHeightPx()))
                : 0;
            segments.add(new FunctionGraphLabelSegment(fallback, exported, width, height));
            totalWidth += width;
        }
        int cursorX = centered ? x - totalWidth / 2 : x;
        String cssColor = argbToRgba(color);
        for (FunctionGraphLabelSegment segment : segments) {
            if (segment.exported() == null) {
                svg.append("<text x=\"")
                    .append(cursorX)
                    .append("\" y=\"")
                    .append(baseline)
                    .append("\" font-size=\"8\" fill=\"")
                    .append(cssColor)
                    .append("\" font-family=\"inherit\">")
                    .append(esc(segment.text()))
                    .append("</text>");
            } else {
                svg.append("<image class=\"guide-function-graph-latex\" href=\"")
                    .append(
                        esc(
                            segment.exported()
                                .src()))
                    .append("\" x=\"")
                    .append(cursorX)
                    .append("\" y=\"")
                    .append(baseline - segment.height() + 2)
                    .append("\" width=\"")
                    .append(segment.width())
                    .append("\" height=\"")
                    .append(segment.height())
                    .append("\" preserveAspectRatio=\"none\"/>");
            }
            cursorX += segment.width();
        }
    }

    private static int estimateFunctionGraphLabelWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            width += codePoint <= 0x7F ? 5 : 8;
            offset += Character.charCount(codePoint);
        }
        return width;
    }

    private record FunctionGraphLabelSegment(String text, @Nullable GuideSiteLatexExporter.ExportedLatex exported,
        int width, int height) {}

    private static void renderFunctionGraphAutoPoints(StringBuilder svg, List<FunctionPlot> plots, int left, int right,
        int top, int bottom, int plotW, int plotH, double xMin, double xMax, double yMin, double yMax) {
        for (FunctionPlot plot : plots) {
            AutoPointSpec spec = plot.getAutoPointSpec();
            if (spec == null || !spec.isEnabled()) {
                continue;
            }
            int color = spec.colorInherit() ? plot.getColor() : spec.color();
            int drawn = 0;
            if (!Double.isNaN(spec.everyX())) {
                drawn = renderFunctionGraphAutoPointsEveryX(
                    svg,
                    plot,
                    spec,
                    color,
                    drawn,
                    left,
                    right,
                    top,
                    bottom,
                    plotW,
                    plotH,
                    xMin,
                    xMax,
                    yMin,
                    yMax);
            }
            if (!Double.isNaN(spec.everyY()) && drawn < AUTO_POINT_MAX_PER_PLOT) {
                renderFunctionGraphAutoPointsEveryY(
                    svg,
                    plot,
                    spec,
                    color,
                    drawn,
                    left,
                    right,
                    top,
                    bottom,
                    plotW,
                    plotH,
                    xMin,
                    xMax,
                    yMin,
                    yMax);
            }
        }
    }

    private static int renderFunctionGraphAutoPointsEveryX(StringBuilder svg, FunctionPlot plot, AutoPointSpec spec,
        int color, int drawn, int left, int right, int top, int bottom, int plotW, int plotH, double xMin, double xMax,
        double yMin, double yMax) {
        if (plot.isInverse()) {
            return renderFunctionGraphAutoPointIntersections(
                svg,
                plot,
                spec,
                color,
                spec.everyX(),
                true,
                drawn,
                left,
                right,
                top,
                bottom,
                plotW,
                plotH,
                xMin,
                xMax,
                yMin,
                yMax);
        }
        double step = spec.everyX();
        double value = Math.ceil(xMin / step) * step;
        int targets = 0;
        while (value <= xMax + 1e-9 && drawn < AUTO_POINT_MAX_PER_PLOT && targets < AUTO_POINT_MAX_TARGETS_PER_PLOT) {
            if (appendFunctionGraphAutoPoint(
                svg,
                value,
                plot.evaluate(value),
                color,
                spec.labelMode(),
                left,
                right,
                top,
                bottom,
                plotW,
                plotH,
                xMin,
                xMax,
                yMin,
                yMax)) {
                drawn++;
            }
            value += step;
            targets++;
        }
        return drawn;
    }

    private static int renderFunctionGraphAutoPointsEveryY(StringBuilder svg, FunctionPlot plot, AutoPointSpec spec,
        int color, int drawn, int left, int right, int top, int bottom, int plotW, int plotH, double xMin, double xMax,
        double yMin, double yMax) {
        if (plot.isInverse()) {
            double step = spec.everyY();
            double value = Math.ceil(yMin / step) * step;
            int targets = 0;
            while (value <= yMax + 1e-9 && drawn < AUTO_POINT_MAX_PER_PLOT
                && targets < AUTO_POINT_MAX_TARGETS_PER_PLOT) {
                if (appendFunctionGraphAutoPoint(
                    svg,
                    plot.evaluate(value),
                    value,
                    color,
                    spec.labelMode(),
                    left,
                    right,
                    top,
                    bottom,
                    plotW,
                    plotH,
                    xMin,
                    xMax,
                    yMin,
                    yMax)) {
                    drawn++;
                }
                value += step;
                targets++;
            }
            return drawn;
        }
        double step = spec.everyY();
        double value = Math.ceil(yMin / step) * step;
        int targets = 0;
        while (value <= yMax + 1e-9 && drawn < AUTO_POINT_MAX_PER_PLOT && targets < AUTO_POINT_MAX_TARGETS_PER_PLOT) {
            drawn = renderFunctionGraphAutoPointIntersections(
                svg,
                plot,
                spec,
                color,
                value,
                false,
                drawn,
                left,
                right,
                top,
                bottom,
                plotW,
                plotH,
                xMin,
                xMax,
                yMin,
                yMax);
            value += step;
            targets++;
        }
        return drawn;
    }

    private static int renderFunctionGraphAutoPointIntersections(StringBuilder svg, FunctionPlot plot,
        AutoPointSpec spec, int color, double target, boolean targetX, int drawn, int left, int right, int top,
        int bottom, int plotW, int plotH, double xMin, double xMax, double yMin, double yMax) {
        double independentMin = plot.isInverse() ? yMin : xMin;
        double independentMax = plot.isInverse() ? yMax : xMax;
        double prevIndependent = independentMin;
        double prevValue = functionGraphAutoPointDifference(plot, prevIndependent, target, targetX);
        for (int i = 1; i <= AUTO_POINT_SCAN_STEPS && drawn < AUTO_POINT_MAX_PER_PLOT; i++) {
            double independent = independentMin + (independentMax - independentMin) * i / AUTO_POINT_SCAN_STEPS;
            double value = functionGraphAutoPointDifference(plot, independent, target, targetX);
            if (Double.isFinite(prevValue) && Double.isFinite(value) && prevValue * value <= 0d) {
                double solved = solveFunctionGraphAutoPoint(
                    plot,
                    target,
                    targetX,
                    prevIndependent,
                    independent,
                    prevValue);
                double dataX = plot.isInverse() ? plot.evaluate(solved) : solved;
                double dataY = plot.isInverse() ? solved : plot.evaluate(solved);
                if (appendFunctionGraphAutoPoint(
                    svg,
                    dataX,
                    dataY,
                    color,
                    spec.labelMode(),
                    left,
                    right,
                    top,
                    bottom,
                    plotW,
                    plotH,
                    xMin,
                    xMax,
                    yMin,
                    yMax)) {
                    drawn++;
                }
            }
            prevIndependent = independent;
            prevValue = value;
        }
        return drawn;
    }

    private static double functionGraphAutoPointDifference(FunctionPlot plot, double independent, double target,
        boolean targetX) {
        double dataX = plot.isInverse() ? plot.evaluate(independent) : independent;
        double dataY = plot.isInverse() ? independent : plot.evaluate(independent);
        return (targetX ? dataX : dataY) - target;
    }

    private static double solveFunctionGraphAutoPoint(FunctionPlot plot, double target, boolean targetX, double lo,
        double hi, double fLo) {
        for (int i = 0; i < AUTO_POINT_SOLVE_STEPS; i++) {
            double mid = (lo + hi) * 0.5d;
            double fMid = functionGraphAutoPointDifference(plot, mid, target, targetX);
            if (!Double.isFinite(fMid) || Math.abs(fMid) < 1e-9) {
                return mid;
            }
            if (fLo * fMid <= 0d) {
                hi = mid;
            } else {
                lo = mid;
                fLo = fMid;
            }
        }
        return (lo + hi) * 0.5d;
    }

    private static boolean appendFunctionGraphAutoPoint(StringBuilder svg, double dataX, double dataY, int color,
        AutoPointLabelMode labelMode, int left, int right, int top, int bottom, int plotW, int plotH, double xMin,
        double xMax, double yMin, double yMax) {
        if (!Double.isFinite(dataX) || !Double.isFinite(dataY)) {
            return false;
        }
        int px = left + (int) Math.round((dataX - xMin) / (xMax - xMin) * plotW);
        int py = bottom - (int) Math.round((dataY - yMin) / (yMax - yMin) * plotH);
        if (px < left - 3 || px > right + 3 || py < top - 3 || py > bottom + 3) {
            return false;
        }
        String pointLabel = "(" + formatNum(dataX) + ", " + formatNum(dataY) + ")";
        svg.append("<circle class=\"guide-chart-shape\" cx=\"")
            .append(px)
            .append("\" cy=\"")
            .append(py)
            .append("\" r=\"4\" fill=\"#FFFFFF\"/>");
        svg.append("<circle class=\"guide-chart-shape\" cx=\"")
            .append(px)
            .append("\" cy=\"")
            .append(py)
            .append("\" r=\"3\" fill=\"")
            .append(argbToRgba(color))
            .append("\"><title>")
            .append(esc(pointLabel))
            .append("</title></circle>");
        if (labelMode != null && labelMode != AutoPointLabelMode.NONE) {
            String label = switch (labelMode) {
                case X -> formatNum(dataX);
                case Y -> formatNum(dataY);
                case XY -> pointLabel;
                case NONE -> "";
            };
            int textW = Math.max(1, label.length() * 6);
            int labelX = px + AUTO_POINT_LABEL_GAP;
            if (labelX + textW > right) {
                labelX = px - textW - AUTO_POINT_LABEL_GAP;
            }
            int labelY = py - AUTO_POINT_LABEL_GAP;
            if (labelY - 8 < top) {
                labelY = py + 10;
            }
            svg.append("<text x=\"")
                .append(labelX)
                .append("\" y=\"")
                .append(labelY)
                .append("\" font-size=\"8\" fill=\"#D7DEE7\" font-family=\"inherit\">")
                .append(esc(label))
                .append("</text>");
        }
        return true;
    }

    private static void renderFunctionGraphCornerLegend(StringBuilder svg, List<FunctionPlot> plots, int left,
        int right, int top, int bottom, CornerLegendPosition position, int maxWidth, int maxHeight,
        int backgroundColor) {
        if (position == null || position == CornerLegendPosition.NONE || plots == null || plots.isEmpty()) {
            return;
        }
        int plotW = right - left;
        int plotH = bottom - top;
        if (plotW < 24 || plotH < 12) {
            return;
        }
        int width = clamp(maxWidth > 0 ? maxWidth : CornerLegendRenderer.DEFAULT_WIDTH, 24, plotW);
        int height = clamp(maxHeight > 0 ? maxHeight : CornerLegendRenderer.DEFAULT_HEIGHT, 12, plotH);
        int capacity = Math.max(0, (height - CORNER_LEGEND_PADDING_Y * 2) / CORNER_LEGEND_ROW_H);
        if (capacity <= 0) {
            return;
        }
        List<FunctionPlot> visible = new ArrayList<>();
        for (FunctionPlot plot : plots) {
            if (plot.getLabel() != null && !plot.getLabel()
                .isEmpty()) {
                visible.add(plot);
                if (visible.size() >= capacity) {
                    break;
                }
            }
        }
        if (visible.isEmpty()) {
            return;
        }
        height = Math.min(height, visible.size() * CORNER_LEGEND_ROW_H + CORNER_LEGEND_PADDING_Y * 2);
        int x = switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> left + CORNER_LEGEND_GAP;
            case TOP_RIGHT, BOTTOM_RIGHT, NONE -> right - width - CORNER_LEGEND_GAP;
        };
        int y = switch (position) {
            case TOP_LEFT, TOP_RIGHT, NONE -> top + CORNER_LEGEND_GAP;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> bottom - height - CORNER_LEGEND_GAP;
        };
        x = clamp(x, left, right - width);
        y = clamp(y, top, bottom - height);
        svg.append("<rect x=\"")
            .append(x)
            .append("\" y=\"")
            .append(y)
            .append("\" width=\"")
            .append(width)
            .append("\" height=\"")
            .append(height)
            .append("\" fill=\"")
            .append(argbToRgba(backgroundColor))
            .append("\" stroke=\"rgba(255,255,255,0.4)\" stroke-width=\"1\"/>");
        int rowY = y + CORNER_LEGEND_PADDING_Y + 8;
        int markerX = x + CORNER_LEGEND_PADDING_X;
        int textX = markerX + CORNER_LEGEND_MARKER_W + CORNER_LEGEND_GAP;
        int maxChars = cornerLegendMaxChars(x, width, textX);
        for (FunctionPlot plot : visible) {
            int markerY = rowY - CORNER_LEGEND_MARKER_H / 2 - 2;
            svg.append("<line x1=\"")
                .append(markerX)
                .append("\" y1=\"")
                .append(markerY + CORNER_LEGEND_MARKER_H / 2)
                .append("\" x2=\"")
                .append(markerX + CORNER_LEGEND_MARKER_W)
                .append("\" y2=\"")
                .append(markerY + CORNER_LEGEND_MARKER_H / 2)
                .append("\" stroke=\"")
                .append(argbToRgba(plot.getColor()))
                .append("\" stroke-width=\"1.5\"/>");
            svg.append("<text x=\"")
                .append(textX)
                .append("\" y=\"")
                .append(rowY)
                .append("\" font-size=\"9\" fill=\"#FFFFFF\" font-family=\"inherit\">")
                .append(esc(ellipsize(plot.getLabel(), maxChars)))
                .append("</text>");
            rowY += CORNER_LEGEND_ROW_H;
        }
    }

    private static void renderChartCornerLegend(StringBuilder svg, List<SeriesData> series, int left, int right,
        int top, int bottom, CornerLegendPosition position, int maxWidth, int maxHeight, int backgroundColor,
        boolean lineMarker) {
        if (position == null || position == CornerLegendPosition.NONE || series == null || series.isEmpty()) {
            return;
        }
        int plotW = right - left;
        int plotH = bottom - top;
        if (plotW < 24 || plotH < 12) {
            return;
        }
        int width = clamp(maxWidth > 0 ? maxWidth : CornerLegendRenderer.DEFAULT_WIDTH, 24, plotW);
        int height = clamp(maxHeight > 0 ? maxHeight : CornerLegendRenderer.DEFAULT_HEIGHT, 12, plotH);
        int capacity = Math.max(0, (height - CORNER_LEGEND_PADDING_Y * 2) / CORNER_LEGEND_ROW_H);
        if (capacity <= 0) {
            return;
        }
        List<SeriesData> visible = new ArrayList<>();
        for (SeriesData item : series) {
            if (item != null && item.name != null && !item.name.isEmpty()) {
                visible.add(item);
                if (visible.size() >= capacity) {
                    break;
                }
            }
        }
        if (visible.isEmpty()) {
            return;
        }
        height = Math.min(height, visible.size() * CORNER_LEGEND_ROW_H + CORNER_LEGEND_PADDING_Y * 2);
        int x = switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> left + CORNER_LEGEND_GAP;
            case TOP_RIGHT, BOTTOM_RIGHT, NONE -> right - width - CORNER_LEGEND_GAP;
        };
        int y = switch (position) {
            case TOP_LEFT, TOP_RIGHT, NONE -> top + CORNER_LEGEND_GAP;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> bottom - height - CORNER_LEGEND_GAP;
        };
        x = clamp(x, left, right - width);
        y = clamp(y, top, bottom - height);
        svg.append("<rect x=\"")
            .append(x)
            .append("\" y=\"")
            .append(y)
            .append("\" width=\"")
            .append(width)
            .append("\" height=\"")
            .append(height)
            .append("\" fill=\"")
            .append(argbToRgba(backgroundColor))
            .append("\" stroke=\"rgba(255,255,255,0.4)\" stroke-width=\"1\"/>");
        int rowY = y + CORNER_LEGEND_PADDING_Y + 8;
        int markerX = x + CORNER_LEGEND_PADDING_X;
        int textX = markerX + CORNER_LEGEND_MARKER_W + CORNER_LEGEND_GAP;
        int maxChars = cornerLegendMaxChars(x, width, textX);
        for (SeriesData item : visible) {
            int markerY = rowY - CORNER_LEGEND_MARKER_H / 2 - 2;
            if (lineMarker) {
                svg.append("<line x1=\"")
                    .append(markerX)
                    .append("\" y1=\"")
                    .append(markerY + CORNER_LEGEND_MARKER_H / 2)
                    .append("\" x2=\"")
                    .append(markerX + CORNER_LEGEND_MARKER_W)
                    .append("\" y2=\"")
                    .append(markerY + CORNER_LEGEND_MARKER_H / 2)
                    .append("\" stroke=\"")
                    .append(argbToRgba(item.color))
                    .append("\" stroke-width=\"1.5\"/>");
            } else {
                svg.append("<rect x=\"")
                    .append(markerX + 2)
                    .append("\" y=\"")
                    .append(markerY)
                    .append("\" width=\"")
                    .append(CORNER_LEGEND_MARKER_H)
                    .append("\" height=\"")
                    .append(CORNER_LEGEND_MARKER_H)
                    .append("\" fill=\"")
                    .append(argbToRgba(item.color))
                    .append("\"/>");
            }
            svg.append("<text x=\"")
                .append(textX)
                .append("\" y=\"")
                .append(rowY)
                .append("\" font-size=\"9\" fill=\"#FFFFFF\" font-family=\"inherit\">")
                .append(esc(ellipsize(item.name, maxChars)))
                .append("</text>");
            rowY += CORNER_LEGEND_ROW_H;
        }
    }

    private static void flushPolyline(StringBuilder svg, StringBuilder pts, String stroke) {
        flushPolyline(svg, pts, stroke, (String) null);
    }

    private static void flushPolyline(StringBuilder svg, StringBuilder pts, String stroke, @Nullable String tip) {
        svg.append("<polyline class=\"guide-chart-shape\" points=\"")
            .append(pts)
            .append("\" stroke=\"")
            .append(stroke)
            .append("\" stroke-width=\"1.5\" fill=\"none\"");
        if (tip != null && !tip.isEmpty()) {
            svg.append("><title>")
                .append(esc(tip))
                .append("</title></polyline>");
        } else {
            svg.append("/>");
        }
    }

    private static void flushPolyline(StringBuilder svg, StringBuilder pts, String stroke, FunctionPlot plot) {
        String tip = plot.getLabel() != null && !plot.getLabel()
            .isEmpty() ? plot.getLabel() : plot.getExpressionText();
        svg.append("<polyline class=\"guide-chart-shape\" points=\"")
            .append(pts)
            .append("\" stroke=\"")
            .append(stroke)
            .append("\" stroke-width=\"1.5\" fill=\"none\"")
            .append(" data-plot-label=\"")
            .append(esc(plot.getLabel() != null ? plot.getLabel() : ""))
            .append("\" data-plot-expression=\"")
            .append(esc(plot.getExpressionText()))
            .append("\" data-plot-inverse=\"")
            .append(plot.isInverse())
            .append("\" data-plot-show-function=\"")
            .append(plot.isShowFunction())
            .append("\" data-plot-show-values=\"")
            .append(plot.isShowValues())
            .append("\" data-plot-tooltip=\"")
            .append(esc(plot.getTooltipText() != null ? plot.getTooltipText() : ""))
            .append("\" data-plot-tooltip-html=\"")
            .append(esc(plot.getSiteTooltipHtml() != null ? plot.getSiteTooltipHtml() : ""))
            .append("\">");
        if (!tip.isEmpty()) {
            svg.append("<title>")
                .append(esc(tip))
                .append("</title>");
        }
        svg.append("</polyline>");
    }

    /**
     * Build a short tooltip string for a chart shape. Empty fields are dropped so we get e.g.
     * "Foo: 12" or "Q1 - Foo: 12".
     */
    private static String buildChartTip(String category, String series, double value) {
        StringBuilder sb = new StringBuilder();
        if (category != null && !category.isEmpty()) {
            sb.append(category);
        }
        if (series != null && !series.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" - ");
            }
            sb.append(series);
        }
        if (!sb.isEmpty()) {
            sb.append(": ");
        }
        sb.append(formatChartValue(value));
        return sb.toString();
    }

    private static StringBuilder openSvg(int w, int h, String cls, int bgColor, int borderColor) {
        StringBuilder svg = new StringBuilder();
        svg.append("<div class=\"guide-svg-wrap\"><svg class=\"")
            .append(cls)
            .append("\" width=\"")
            .append(w)
            .append("\" height=\"")
            .append(h)
            .append("\" viewBox=\"0 0 ")
            .append(w)
            .append(" ")
            .append(h)
            .append("\">");
        svg.append("<rect width=\"")
            .append(w)
            .append("\" height=\"")
            .append(h)
            .append("\" fill=\"")
            .append(argbToRgba(bgColor))
            .append("\"");
        if ((borderColor >>> 24) != 0) {
            svg.append(" stroke=\"")
                .append(argbToRgba(borderColor))
                .append("\" stroke-width=\"1\"");
        }
        svg.append("/>");
        return svg;
    }

    private static void appendTitle(StringBuilder svg, String title, int w) {
        appendTitle(svg, title, w, DEFAULT_TITLE_COLOR);
    }

    private static void appendTitle(StringBuilder svg, String title, int w, int titleColor) {
        if (title == null || title.isEmpty()) {
            return;
        }
        svg.append("<text x=\"")
            .append(w / 2)
            .append("\" y=\"")
            .append(PADDING + TITLE_H)
            .append("\" text-anchor=\"middle\" font-size=\"10\" fill=\"")
            .append(argbToRgba(titleColor))
            .append("\" font-family=\"inherit\">")
            .append(esc(title))
            .append("</text>");
    }

    private static void appendYGridAndLabels(StringBuilder svg, int left, int right, int top, int bottom, int plotH,
        double yMin, double yMax) {
        appendYGridAndLabels(
            svg,
            left,
            right,
            top,
            bottom,
            plotH,
            yMin,
            yMax,
            SiteChartAxis.automatic(DEFAULT_GRID_COLOR));
    }

    /**
     * Draws Y ticks and grid lines from the declared axis range and step.
     */
    private static void appendYGridAndLabels(StringBuilder svg, int left, int right, int top, int bottom, int plotH,
        double yMin, double yMax, SiteChartAxis axis) {
        double yStep = axis.stepFor(yMax - yMin);
        if (yStep <= 0 || !Double.isFinite(yStep)) {
            yStep = niceStep((yMax - yMin) / 5.0);
        }
        for (double yv = Math.floor(yMin / yStep) * yStep; yv <= yMax + yStep * 0.01; yv += yStep) {
            int gy = bottom - (int) Math.round((yv - yMin) / (yMax - yMin) * plotH);
            if (gy < top - 2 || gy > bottom + 2) {
                continue;
            }
            if (axis.gridVisible()) {
                svg.append("<line x1=\"")
                    .append(left)
                    .append("\" y1=\"")
                    .append(gy)
                    .append("\" x2=\"")
                    .append(right)
                    .append("\" y2=\"")
                    .append(gy)
                    .append("\" stroke=\"")
                    .append(argbToRgba(axis.gridColor()))
                    .append("\" stroke-width=\"1\"/>");
            }
            svg.append("<text x=\"")
                .append(left - 3)
                .append("\" y=\"")
                .append(gy + 4)
                .append("\" text-anchor=\"end\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(axis.formatTick(yv)))
                .append("</text>");
        }
    }

    private static void appendCategoryXLabels(StringBuilder svg, String[] categories, int left, double clusterW,
        int bottom) {
        for (int ci = 0; ci < categories.length; ci++) {
            int cx = left + (int) Math.round((ci + 0.5) * clusterW);
            svg.append("<text x=\"")
                .append(cx)
                .append("\" y=\"")
                .append(bottom + 10)
                .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(categories[ci]))
                .append("</text>");
        }
    }

    private static void appendYAxis(StringBuilder svg, int left, int top, int bottom) {
        svg.append("<line x1=\"")
            .append(left)
            .append("\" y1=\"")
            .append(top)
            .append("\" x2=\"")
            .append(left)
            .append("\" y2=\"")
            .append(bottom)
            .append("\" stroke=\"#B8C2CF\" stroke-width=\"1\"/>");
    }

    private static void appendXAxis(StringBuilder svg, int left, int right, int bottom) {
        svg.append("<line x1=\"")
            .append(left)
            .append("\" y1=\"")
            .append(bottom)
            .append("\" x2=\"")
            .append(right)
            .append("\" y2=\"")
            .append(bottom)
            .append("\" stroke=\"#B8C2CF\" stroke-width=\"1\"/>");
    }

    /**
     * Reserves legend space on the declared side of the plot. Side width follows the longest name; entries are
     * clipped to the available rows.
     *
     * @param legendNames names in display order
     * @param plotLeft    initial plot left edge
     * @param plotRight   initial plot right edge
     * @param plotTop     initial plot top edge
     * @param plotBottom  initial plot bottom edge, including axis labels
     */
    private static LegendArea legendArea(ChartStyle style, List<String> legendNames, int plotLeft, int plotRight,
        int plotTop, int plotBottom) {
        int itemCount = legendNames != null ? legendNames.size() : 0;
        if (!style.showsLegend() || itemCount <= 0 || plotRight <= plotLeft || plotBottom <= plotTop) {
            return new LegendArea(plotLeft, plotRight, plotTop, plotBottom, 0, 0, 0, 0, 1);
        }
        int availW = Math.max(1, plotRight - plotLeft);
        int itemW = legendItemWidthFor(itemCount, availW);
        int cols = Math.max(1, availW / Math.max(1, itemW));
        int rows = (int) Math.ceil((double) itemCount / cols);
        int rowsH = rows * (LEGEND_ROW_H + 2);
        int sideWidth = sideLegendWidth(legendNames);
        return switch (style.legendPosition()) {
            case TOP -> new LegendArea(
                plotLeft,
                plotRight,
                plotTop + rowsH + LEGEND_GAP,
                plotBottom,
                plotLeft,
                plotTop,
                availW,
                plotTop + rowsH + LEGEND_GAP,
                cols);
            case LEFT -> new LegendArea(
                plotLeft + sideWidth,
                plotRight,
                plotTop,
                plotBottom,
                Math.max(PADDING, plotLeft - AXIS_PAD_LEFT),
                plotTop,
                Math.max(LEGEND_SWATCH + LEGEND_GAP, sideWidth - LEGEND_GAP),
                plotBottom,
                1);
            case RIGHT -> new LegendArea(
                plotLeft,
                Math.max(plotLeft + 1, plotRight - sideWidth),
                plotTop,
                plotBottom,
                plotRight - sideWidth + LEGEND_GAP,
                plotTop,
                Math.max(LEGEND_SWATCH + LEGEND_GAP, sideWidth - LEGEND_GAP),
                plotBottom,
                1);
            default -> new LegendArea(
                plotLeft,
                plotRight,
                plotTop,
                plotBottom - rowsH - LEGEND_GAP,
                plotLeft,
                plotBottom - rowsH,
                availW,
                plotBottom,
                cols);
        };
    }

    private static int legendItemWidth(List<String> legendNames) {
        int longest = 0;
        for (String name : legendNames) {
            longest = Math.max(longest, approximateTextWidth(name));
        }
        return LEGEND_SWATCH + LEGEND_GAP + longest + LEGEND_GAP;
    }

    private static int sideLegendWidth(List<String> legendNames) {
        int needed = legendItemWidth(legendNames);
        return Math.clamp(needed, 6 * CHART_TEXT_SIZE, 18 * CHART_TEXT_SIZE);
    }

    private static int approximateTextWidth(String text) {
        return text != null ? text.length() * CHART_CHAR_WIDTH : 0;
    }

    private static void appendValueLabel(StringBuilder svg, double x, double y, String anchor, double value,
        String fill) {
        appendText(svg, x, y, anchor, formatChartValue(value), fill);
    }

    static String formatChartValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatChartPercent(double ratio) {
        if (Double.isNaN(ratio) || Double.isInfinite(ratio)) {
            return "0%";
        }
        return String.format(Locale.ROOT, "%.1f%%", ratio * 100d);
    }

    private static List<String> seriesNames(List<SeriesData> series) {
        List<String> names = new ArrayList<>(series.size());
        for (SeriesData entry : series) {
            names.add(entry.name);
        }
        return names;
    }

    private static void appendText(StringBuilder svg, double x, double y, String anchor, String text, String fill) {
        svg.append("<text x=\"")
            .append(fmtD(x))
            .append("\" y=\"")
            .append(fmtD(y))
            .append("\" text-anchor=\"")
            .append(anchor)
            .append("\" font-size=\"")
            .append(CHART_TEXT_SIZE)
            .append("\" fill=\"")
            .append(fill)
            .append("\" font-family=\"inherit\">")
            .append(esc(text))
            .append("</text>");
    }

    /**
     * Draws a legend in space reserved with computeLegendH, using the same column count.
     */
    private static void renderLegend(StringBuilder svg, List<SeriesData> series, int x, int y, int availW, int limit) {
        renderLegend(svg, series, x, y, availW, limit, legendColumns(series.size(), availW));
    }

    /**
     * Draws the legend into the space its layout reserved.
     *
     * @param cols columns the reservation was computed with, so the drawn rows cannot outnumber the
     *             reserved ones
     */
    private static void renderLegend(StringBuilder svg, List<SeriesData> series, int x, int y, int availW, int limit,
        int cols) {
        int itemW = Math.max(1, availW / Math.max(1, cols));
        int maxCols = Math.max(1, cols);
        int col = 0;
        int curX = x;
        int curY = y;
        for (SeriesData s : series) {
            if (col >= maxCols) {
                col = 0;
                curX = x;
                curY += LEGEND_ROW_H + 2;
            }
            if (limit > 0 && curY + LEGEND_ROW_H > limit) {
                return;
            }
            svg.append("<rect x=\"")
                .append(curX)
                .append("\" y=\"")
                .append(curY)
                .append("\" width=\"")
                .append(LEGEND_SWATCH)
                .append("\" height=\"")
                .append(LEGEND_SWATCH)
                .append("\" fill=\"")
                .append(argbToRgba(s.color))
                .append("\"/>");
            svg.append("<text x=\"")
                .append(curX + LEGEND_SWATCH + LEGEND_GAP)
                .append("\" y=\"")
                .append(curY + LEGEND_SWATCH - 1)
                .append("\" font-size=\"")
                .append(CHART_TEXT_SIZE)
                .append("\" fill=\"#D7DEE7\" font-family=\"inherit\">")
                .append(
                    esc(ellipsize(s.name, Math.max(1, (itemW - LEGEND_SWATCH - 2 * LEGEND_GAP) / CHART_CHAR_WIDTH))))
                .append("</text>");
            curX += itemW;
            col++;
        }
    }

    private static int computeTitleBottom(String title) {
        if (title == null || title.isEmpty()) {
            return PADDING;
        }
        return PADDING + TITLE_H + TITLE_GAP;
    }

    private static int computeLegendH(List<SeriesData> series, boolean showLegend, int w) {
        if (!showLegend || series == null || series.isEmpty()) {
            return 0;
        }
        int availW = w - 2 * PADDING;
        int cols = legendColumns(series.size(), availW);
        return (int) Math.ceil((double) series.size() / cols) * (LEGEND_ROW_H + 2) + LEGEND_GAP;
    }

    /**
     * Column count shared by legend reservation and rendering.
     */
    private static int legendColumns(int itemCount, int availW) {
        int itemW = legendItemWidthFor(itemCount, availW);
        return Math.max(1, Math.max(1, availW) / itemW);
    }

    private static int legendItemWidthFor(int itemCount, int availW) {
        if (itemCount <= 0) {
            return 1;
        }
        return Math.clamp(Math.max(1, availW) / itemCount, 60, 100 + 4 * CHART_TEXT_SIZE);
    }

    /**
     * Maximum label length that fits within the corner legend box.
     */
    private static int cornerLegendMaxChars(int boxX, int boxWidth, int textX) {
        int room = boxX + boxWidth - CORNER_LEGEND_PADDING_X - textX;
        return Math.max(0, room / CHART_CHAR_WIDTH);
    }

    private static String ellipsize(String text, int maxChars) {
        if (text == null || text.isEmpty() || maxChars <= 0) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return text.substring(0, maxChars);
        }
        return text.substring(0, maxChars - 3) + "...";
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    /**
     * Format a number for SVG axis labels: integer if whole, else limited decimals.
     */
    public static String formatNum(double v) {
        if (v == 0) {
            return "0";
        }
        double absV = Math.abs(v);
        if (absV >= 1e6 || (absV < 1e-3)) {
            return String.format(Locale.ROOT, "%.2e", v);
        }
        if (absV == Math.floor(absV) && absV < 1e5) {
            return String.valueOf((long) v);
        }
        String s = String.format(Locale.ROOT, "%.4f", v);
        int dot = s.indexOf('.');
        if (dot >= 0) {
            int last = s.length() - 1;
            while (last > dot && s.charAt(last) == '0') {
                last--;
            }
            if (last == dot) {
                last--;
            }
            s = s.substring(0, last + 1);
        }
        return s;
    }

    /**
     * Format a double to one decimal place for SVG geometry.
     */
    private static String fmtD(double v) {
        long r = Math.round(v * 10);
        if (r % 10 == 0) {
            return String.valueOf(r / 10);
        }
        return (r / 10) + "." + Math.abs(r % 10);
    }

    /**
     * Draws the X title below the plot and the Y title above it, matching in-game charts.
     */
    private static void appendAxisLabels(StringBuilder svg, int left, int right, int top, int bottom,
        ChartStyle style) {
        String xLabel = style.xAxisOrDefault()
            .label();
        if (xLabel != null && !xLabel.isEmpty()) {
            svg.append("<text x=\"")
                .append(left + (right - left) / 2)
                .append("\" y=\"")
                .append(bottom + AXIS_PAD_BOTTOM - 2)
                .append("\" text-anchor=\"middle\" font-size=\"")
                .append(CHART_TEXT_SIZE)
                .append("\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(xLabel))
                .append("</text>");
        }
        String yLabel = style.yAxisOrDefault()
            .label();
        if (yLabel != null && !yLabel.isEmpty()) {
            svg.append("<text x=\"")
                .append(left)
                .append("\" y=\"")
                .append(top - 2)
                .append("\" text-anchor=\"start\" font-size=\"")
                .append(CHART_TEXT_SIZE)
                .append("\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(yLabel))
                .append("</text>");
        }
    }

    static double niceStepForAxis(double range) {
        return niceStep(range / 5.0);
    }

    /**
     * Choose a nice grid step for the given rough interval.
     */
    private static double niceStep(double rough) {
        if (rough <= 0) {
            return 1;
        }
        double exp = Math.pow(10, Math.floor(Math.log10(rough)));
        double frac = rough / exp;
        if (frac < 1.5) {
            return exp;
        }
        if (frac < 3.5) {
            return 2 * exp;
        }
        if (frac < 7.5) {
            return 5 * exp;
        }
        return 10 * exp;
    }
}
