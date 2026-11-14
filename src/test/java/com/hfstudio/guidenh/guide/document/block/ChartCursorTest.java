package com.hfstudio.guidenh.guide.document.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.chart.ChartReferenceLine;
import com.hfstudio.guidenh.guide.document.block.chart.LytChartBase;
import com.hfstudio.guidenh.guide.document.block.chart.LytLineChart;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateTestHarness;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

class ChartCursorTest {

    private record Line(float x1, float y1, float x2, float y2, int color) {}

    private record Text(String value, int x, int y) {}

    private record Circle(float x, float y, float radius) {}

    private static final FontMetrics FONT = new FontMetrics() {

        @Override
        public float getAdvance(int codePoint, ResolvedTextStyle style) {
            return 6;
        }

        @Override
        public int getLineHeight(ResolvedTextStyle style) {
            return 10;
        }
    };

    private final List<Line> lines = new ArrayList<>();
    private final List<Text> texts = new ArrayList<>();
    private final List<LytRect> fills = new ArrayList<>();
    private final List<Circle> circles = new ArrayList<>();
    private final RenderContext context = (RenderContext) Proxy.newProxyInstance(
        RenderContext.class.getClassLoader(),
        new Class<?>[] { RenderContext.class },
        (proxy, method, args) -> {
            switch (method.getName()) {
                case "getStringWidth":
                    return FONT.getStringWidth((String) args[0], (ResolvedTextStyle) args[1]);
                case "getLineHeight":
                    return 10;
                case "drawLine":
                    lines.add(
                        new Line((float) args[0], (float) args[1], (float) args[2], (float) args[3], (int) args[5]));
                    break;
                case "drawText":
                    texts.add(new Text((String) args[0], (int) args[1], (int) args[2]));
                    break;
                case "drawCircleOutline":
                    circles.add(new Circle((float) args[0], (float) args[1], (float) args[2]));
                    break;
                case "fillRect":
                    fills.add(
                        args[0] instanceof LytRect rect ? rect
                            : new LytRect((int) args[0], (int) args[1], (int) args[2], (int) args[3]));
                    break;
                default:
                    break;
            }
            return method.getReturnType() == int.class ? 0 : null;
        });

    @Test
    void columnCursor() {
        followsThePointerInEmptyPlotSpaceAndHighlightsOnlyTheLeftValue("ColumnChart");
    }

    @Test
    void lineCursor() {
        followsThePointerInEmptyPlotSpaceAndHighlightsOnlyTheLeftValue("LineChart");
    }

    @Test
    void scatterCursor() {
        followsThePointerInEmptyPlotSpaceAndHighlightsOnlyTheLeftValue("ScatterChart");
    }

    @Test
    void barCursorFollowsXAndHighlightsOnlyTheBottomValue() {
        var chart = chartMarkup("""
            <BarChart legend="none" showXGrid="false" showYGrid="false" xAxisMin="-100" xAxisMax="100" xAxisUnit="EU/t">
              <Series data="-100,100"/>
              <ReferenceLine x="50"/>
            </BarChart>
            """);
        chart.render(context);
        var axis = lines.stream()
            .filter(l -> l.y1 == l.y2)
            .findFirst()
            .orElseThrow();
        var vertical = lines.stream()
            .filter(l -> l.x1 == l.x2 && l.y2 - l.y1 > 4)
            .findFirst()
            .orElseThrow();
        float y = vertical.y1 + 1;
        float fixedX = axis.x1 + (axis.x2 - axis.x1) * .75f;
        for (float fraction : new float[] { 0, .25f, .5f, .75f, 1 }) {
            float x = axis.x1 + (axis.x2 - axis.x1) * fraction;
            chart.getTooltip(x, y);
            clear();
            chart.render(context);
            var cursor = lines.stream()
                .filter(l -> l.color == 0xFFF2C14E)
                .toList();
            assertFalse(cursor.isEmpty());
            assertTrue(
                cursor.stream()
                    .allMatch(l -> l.x1 == x && l.x2 == x && l.y2 - l.y1 <= 4));
            assertEquals(vertical.y1, cursor.getFirst().y1);
            assertTrue(vertical.y2 - cursor.getLast().y2 <= 3);
            String expected = Integer.toString(Math.round(-100 + 200 * fraction)) + "EU/t";
            var badge = texts.stream()
                .filter(t -> t.value.equals(expected) && t.y > axis.y1)
                .toList()
                .getLast();
            assertTrue(
                fills.stream()
                    .anyMatch(
                        r -> r.x() <= badge.x && r.right() >= badge.x + expected.length() * 6
                            && r.y() <= badge.y
                            && r.bottom() >= badge.y + 10));
            assertTrue(
                lines.stream()
                    .anyMatch(
                        l -> l.color == ChartReferenceLine.DEFAULT_COLOR && Math.abs(l.x1 - fixedX) < .01
                            && l.x1 == l.x2));
        }
        for (float[] outside : new float[][] { { axis.x1 - 1, y }, { axis.x2 + 1, y },
            { axis.x1 + 10, vertical.y1 - 1 }, { axis.x1 + 10, axis.y1 + 1 } }) {
            chart.getTooltip(outside[0], outside[1]);
            clear();
            chart.render(context);
            assertTrue(
                lines.stream()
                    .noneMatch(l -> l.color == 0xFFF2C14E));
        }
        chart.getTooltip(axis.x1 + 10, y);
        chart.onMouseLeave();
        clear();
        chart.render(context);
        assertTrue(
            lines.stream()
                .noneMatch(l -> l.color == 0xFFF2C14E));
        assertTrue(
            lines.stream()
                .anyMatch(l -> l.color == ChartReferenceLine.DEFAULT_COLOR));
    }

    @Test
    void mixedPieHoverTakesPriorityOverBarsAndHidesTheAxisCursor() {
        for (String tag : List.of("ColumnChart", "BarChart")) {
            for (String position : List.of("topRight", "bottomLeft", "rightOutside")) {
                for (String direction : List.of("clockwise", "counterclockwise")) {
                    clear();
                    var chart = chartMarkup(
                        "<" + tag
                            + " legend=\"none\" showXGrid=\"false\" showYGrid=\"false\" xAxisMin=\"0\" xAxisMax=\"100\" yAxisMin=\"0\" yAxisMax=\"100\"><Series name=\"Underlying\" data=\"100,100\"/><PieInset size=\"64\" title=\"Share\" position=\""
                            + position
                            + "\" direction=\""
                            + direction
                            + "\" startAngleDeg=\"30\"><Slice name=\"First\" value=\"25\" tooltip=\"Extra detail\"/><Slice name=\"Empty\" value=\"0\"/><Slice name=\"Second\" value=\"75\"/></PieInset></"
                            + tag
                            + ">");
                    chart.render(context);
                    var pie = circles.getLast();
                    for (double fraction : new double[] { .125, .5 }) {
                        double angle = Math.toRadians(30)
                            + fraction * Math.PI * 2 * (direction.equals("clockwise") ? 1 : -1);
                        float x = pie.x + (float) Math.cos(angle) * pie.radius * .6f;
                        float y = pie.y + (float) Math.sin(angle) * pie.radius * .6f;
                        var tooltip = (TextTooltip) chart.getTooltip(x, y)
                            .orElseThrow();
                        assertEquals(
                            fraction < .25 ? "First\n25\n25.0%\nExtra detail" : "Second\n75\n75.0%",
                            tooltip.getText());
                        clear();
                        chart.render(context);
                        assertTrue(
                            lines.stream()
                                .noneMatch(l -> l.color == 0xFFF2C14E),
                            "The pie tooltip owns the pointer");
                    }
                    chart.onMouseLeave();
                }
            }
        }
    }

    @Test
    void fixedReferenceStaysInPlaceAsTheColoredCursorMovesAndLeaves() {
        var chart = (LytLineChart) chart("LineChart");
        chart.getYAxis()
            .setReferenceLines(List.of(new ChartReferenceLine(75, "", ChartReferenceLine.DEFAULT_COLOR)));
        chart.render(context);
        var fixed = dashes().stream()
            .filter(line -> line.color == ChartReferenceLine.DEFAULT_COLOR)
            .findFirst()
            .orElseThrow();
        var border = lines.stream()
            .filter(line -> line.x1 == line.x2)
            .findFirst()
            .orElseThrow();
        float top = Math.min(border.y1, border.y2);
        float bottom = Math.max(border.y1, border.y2);
        for (float fraction : new float[] { .25f, .5f, .75f }) {
            float cursorY = top + (bottom - top) * fraction;
            chart.getTooltip(fixed.x1 + 30, cursorY);
            clear();
            chart.render(context);
            assertTrue(
                dashes().stream()
                    .anyMatch(line -> line.color == 0xFFF2C14E && line.y1 == cursorY));
            assertTrue(
                dashes().stream()
                    .anyMatch(line -> line.color == fixed.color && line.y1 == fixed.y1));
        }
        chart.onMouseLeave();
        clear();
        chart.render(context);
        assertTrue(
            dashes().stream()
                .noneMatch(line -> line.color == 0xFFF2C14E));
        assertTrue(
            dashes().stream()
                .anyMatch(line -> line.color == fixed.color && line.y1 == fixed.y1));
    }

    private void followsThePointerInEmptyPlotSpaceAndHighlightsOnlyTheLeftValue(String tag) {
        var chart = chart(tag);
        chart.render(context);
        var axis = lines.stream()
            .filter(l -> l.y1 == l.y2)
            .max(java.util.Comparator.comparingDouble(l -> l.x2 - l.x1))
            .orElseThrow();
        var vertical = lines.stream()
            .filter(l -> l.x1 == l.x2)
            .findFirst()
            .orElseThrow();
        float top = Math.min(vertical.y1, vertical.y2);
        float bottom = Math.max(vertical.y1, vertical.y2);
        float x = axis.x1 + (axis.x2 - axis.x1) * .53f;

        assertTrue(dashes().isEmpty());
        for (float fraction : new float[] { 0, .25f, .5f, .75f, 1 }) {
            float y = top + (bottom - top) * fraction;
            clear();
            assertTrue(
                chart.getTooltip(x, y)
                    .isEmpty(),
                "The cursor must also work away from data marks");
            chart.render(context);
            assertFalse(dashes().isEmpty(), tag + " must render a horizontal cursor");
            assertTrue(
                dashes().stream()
                    .allMatch(line -> Math.abs(line.y1 - y) < .01 && line.y1 == line.y2));
            assertEquals(axis.x1, dashes().getFirst().x1);
            assertTrue(dashes().getLast().x2 <= axis.x2 && axis.x2 - dashes().getLast().x2 <= 3);
            String expected = Integer.toString(Math.round(100 - 200 * fraction)) + "EU/t";
            var badge = texts.stream()
                .filter(t -> t.value.equals(expected) && t.y >= y - 8 && t.y <= y)
                .findFirst()
                .orElseThrow();
            int textRight = badge.x + expected.length() * 6;
            assertTrue(textRight < axis.x1, "The cursor value belongs on the left axis only");
            assertTrue(
                fills.stream()
                    .anyMatch(
                        r -> r.x() <= badge.x && r.right() >= textRight
                            && r.right() <= axis.x1
                            && r.y() <= badge.y
                            && r.bottom() >= badge.y + 10));
        }

        for (float[] outside : new float[][] { { axis.x1 - 1, top + 20 }, { axis.x2 + 1, top + 20 }, { x, top - 1 },
            { x, bottom + 1 } }) {
            chart.getTooltip(outside[0], outside[1]);
            clear();
            chart.render(context);
            assertTrue(dashes().isEmpty(), "The cursor must hide outside the plot");
        }
        chart.getTooltip(x, (top + bottom) / 2);
        chart.onMouseLeave();
        clear();
        chart.render(context);
        assertTrue(dashes().isEmpty(), "The cursor must clear when leaving the chart");
    }

    private List<Line> dashes() {
        return lines.stream()
            .filter(l -> l.y1 == l.y2 && l.x2 > l.x1 && l.x2 - l.x1 <= 4)
            .toList();
    }

    private void clear() {
        lines.clear();
        texts.clear();
        fills.clear();
        circles.clear();
    }

    private LytChartBase chart(String tag) {
        return chartMarkup(
            "<" + tag
                + " legend=\"none\" showYGrid=\"false\" showXGrid=\"false\" yAxisMin=\"-100\" yAxisMax=\"100\" yAxisUnit=\"EU/t\"><Series name=\"Power\" data=\"-100,100\" points=\"0:-100;1:100\"/></"
                + tag
                + ">");
    }

    private LytChartBase chartMarkup(String markup) {
        var page = new MediaWikiTemplateTestHarness().compile(markup);
        var charts = new ArrayList<LytChartBase>();
        page.document()
            .visit(new LytVisitor() {

                @Override
                public Result beforeNode(LytNode node) {
                    if (node instanceof LytChartBase chart) charts.add(chart);
                    return Result.CONTINUE;
                }
            });
        assertEquals(1, charts.size(), MediaWikiTemplateTestHarness.text(page));
        var chart = charts.getFirst();
        chart.layout(new LayoutContext(FONT), 20, 30, 320);
        return chart;
    }
}
