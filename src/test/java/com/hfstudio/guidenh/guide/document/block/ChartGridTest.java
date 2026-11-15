package com.hfstudio.guidenh.guide.document.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.chart.AxisRange;
import com.hfstudio.guidenh.guide.document.block.chart.CartesianChartRenderer;
import com.hfstudio.guidenh.guide.document.block.chart.ChartAxisOptions;
import com.hfstudio.guidenh.guide.document.block.chart.ChartReferenceLine;
import com.hfstudio.guidenh.guide.document.block.chart.CornerLegendPosition;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteGraphRenderer;
import com.hfstudio.guidenh.guide.siteexport.site.SiteChartAxis;

class ChartGridTest {

    private record Stroke(float x1, float y1, float x2, float y2, int color) {}

    private final List<Stroke> strokes = new ArrayList<>();
    private final List<String> texts = new ArrayList<>();
    private final RenderContext context = (RenderContext) Proxy.newProxyInstance(
        RenderContext.class.getClassLoader(),
        new Class<?>[] { RenderContext.class },
        (proxy, method, args) -> {
            switch (method.getName()) {
                case "drawLine":
                    strokes.add(
                        new Stroke((float) args[0], (float) args[1], (float) args[2], (float) args[3], (int) args[5]));
                    break;
                case "drawText":
                    texts.add((String) args[0]);
                    break;
                case "getStringWidth":
                    return ((String) args[0]).length() * 6;
                case "getLineHeight":
                    return 10;
                default:
                    break;
            }
            return null;
        });

    @Test
    void numericTicksHaveFaintSparseGridInBothDirections() {
        var axis = axis();
        var plot = new LytRect(30, 20, 100, 100);
        var range = new AxisRange(0, 100, 50);
        CartesianChartRenderer.drawAxes(context, plot, axis, axis, range, range, null, true);
        var grid = strokes.stream()
            .filter(s -> s.color != axis.getAxisColor())
            .toList();
        assertFalse(grid.isEmpty());
        assertTrue(
            grid.stream()
                .allMatch(s -> Math.hypot(s.x2 - s.x1, s.y2 - s.y1) <= 1.01));
        assertTrue(
            grid.stream()
                .allMatch(s -> (s.color >>> 24) <= 29),
            "Grid strokes should remain faint");
        for (int tick : new int[] { 50, 100 }) {
            float y = CartesianChartRenderer.mapY(tick, range, plot);
            assertTrue(
                grid.stream()
                    .anyMatch(s -> s.y1 == y && s.y2 == y));
            float x = CartesianChartRenderer.mapX(tick, range, plot);
            assertTrue(
                grid.stream()
                    .anyMatch(s -> s.x1 == x && s.x2 == x));
        }
        assertTrue(grid.size() <= 66, "Long gaps should separate the short dashes");
    }

    @Test
    void categoryTicksAlsoHaveVerticalGridAndRespectTheVisibilityFlag() {
        var x = axis();
        var y = axis();
        y.setGridVisible(false);
        var plot = new LytRect(30, 20, 100, 100);
        CartesianChartRenderer
            .drawAxes(context, plot, x, y, null, new AxisRange(0, 100, 50), new String[] { "A", "B" }, false);
        var grid = strokes.stream()
            .filter(s -> s.color != x.getAxisColor())
            .toList();
        assertFalse(grid.isEmpty());
        assertTrue(
            grid.stream()
                .anyMatch(s -> s.x1 == 55 && s.x2 == 55));
        assertTrue(
            grid.stream()
                .anyMatch(s -> s.x1 == 105 && s.x2 == 105));
        x.setGridVisible(false);
        strokes.clear();
        CartesianChartRenderer
            .drawAxes(context, plot, x, y, null, new AxisRange(0, 100, 50), new String[] { "A", "B" }, false);
        assertEquals(2, strokes.size(), "Only the two solid axis borders should remain");
    }

    @Test
    void fixedReferenceIsNeutralAndDoesNotAddTextOnTheLine() {
        var axis = axis();
        axis.setReferenceLines(List.of(new ChartReferenceLine(75, "", ChartReferenceLine.DEFAULT_COLOR)));
        CartesianChartRenderer
            .drawReferenceLines(context, new LytRect(30, 20, 100, 100), axis, new AxisRange(0, 100, 50));
        assertFalse(strokes.isEmpty());
        assertTrue(
            strokes.stream()
                .allMatch(s -> s.y1 == 45 && s.y2 == 45));
        assertTrue(texts.isEmpty(), "The fixed reference must not put text inside the plot");
        int color = ChartReferenceLine.DEFAULT_COLOR;
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        assertTrue(Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 24);
        assertTrue((color >>> 24) < 255);
    }

    @Test
    void exportsSparseHorizontalAndVerticalGridForNumericAndCategoryAxes() {
        var axis = SiteChartAxis.from(axis(), 0);
        var style = GuideSiteGraphRenderer.ChartStyle.of(320, 200, 0xff111111, 0xff333333, null)
            .withAxes(axis, axis);
        var series = List.of(
            new GuideSiteGraphRenderer.SeriesData(
                "Power",
                0xff336699,
                new double[] { 0, 100 },
                new double[] { 0, 100 },
                "line"));
        var categories = new String[] { "A", "B" };
        var htmls = List.of(
            GuideSiteGraphRenderer.renderColumnChart(style, categories, series, null, null),
            GuideSiteGraphRenderer
                .renderLineChart(style, categories, series, false, true, CornerLegendPosition.NONE, 120, 64, 0),
            GuideSiteGraphRenderer
                .renderLineChart(style, categories, series, true, true, CornerLegendPosition.NONE, 120, 64, 0),
            GuideSiteGraphRenderer.renderScatterChart(style, series, CornerLegendPosition.NONE, 120, 64, 0));
        for (String html : htmls) {
            var matcher = java.util.regex.Pattern.compile("<line class=\"guide-chart-grid\"[^>]+>")
                .matcher(html);
            var grid = new ArrayList<String>();
            while (matcher.find()) grid.add(matcher.group());
            assertTrue(grid.size() >= 5, html);
            assertTrue(
                grid.stream()
                    .allMatch(s -> s.contains("stroke-dasharray=\"1 9\"") && s.contains("stroke-opacity=\"0.55\"")),
                html);
        }
    }

    private static ChartAxisOptions axis() {
        var axis = new ChartAxisOptions();
        axis.setGridVisible(true);
        axis.setMin(0d);
        axis.setMax(100d);
        axis.setStep(50d);
        return axis;
    }
}
