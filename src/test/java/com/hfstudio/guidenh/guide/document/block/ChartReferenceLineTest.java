package com.hfstudio.guidenh.guide.document.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.block.chart.LytBarChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytLineChart;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateTestHarness;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteGraphRenderer;
import com.hfstudio.guidenh.guide.siteexport.site.SiteChartAxis;

class ChartReferenceLineTest {

    @Test
    void exportedMixedChartsIdentifyPieSectorsForIndependentHover() {
        var style = GuideSiteGraphRenderer.ChartStyle.of(360, 200, 0xff111111, 0xff333333, null);
        var series = java.util.List.of(
            new GuideSiteGraphRenderer.SeriesData(
                "Output",
                0xff336699,
                new double[] { 0, 1 },
                new double[] { 50, 150 },
                "bar"));
        var inset = new GuideSiteGraphRenderer.PieInsetData(
            java.util.List.of(
                new GuideSiteGraphRenderer.SliceData("Primary", 70, 0xff336699),
                new GuideSiteGraphRenderer.SliceData("Secondary", 30, 0xfff2c14e)),
            64,
            "topRight",
            "Share");
        for (var html : java.util.List.of(
            GuideSiteGraphRenderer.renderColumnChart(style, new String[] { "A", "B" }, series, inset, null),
            GuideSiteGraphRenderer.renderBarChart(style, new String[] { "A", "B" }, series, inset))) {
            assertTrue(html.contains("guide-chart-pie-slice"), html);
            assertTrue(html.contains("<title>Primary: 70 (70.0%)</title>"), html);
        }
    }

    @Test
    void barReferencesCompileAndExportOnTheXValueAxis() {
        var page = new MediaWikiTemplateTestHarness().compile("""
            <BarChart categories="A,B" xAxisMin="0" xAxisMax="200">
              <Series data="50,150"/>
              <ReferenceLine x="100"/>
              <ReferenceLine y="50"/>
            </BarChart>
            """);
        var charts = new java.util.ArrayList<LytBarChart>();
        page.document()
            .visit(new LytVisitor() {

                @Override
                public Result beforeNode(LytNode node) {
                    if (node instanceof LytBarChart chart) charts.add(chart);
                    return Result.CONTINUE;
                }
            });
        var chart = charts.getFirst();
        assertEquals(
            2,
            chart.getXAxis()
                .getReferenceLines()
                .size());
        var style = GuideSiteGraphRenderer.ChartStyle.of(320, 200, 0xff111111, 0xff333333, null)
            .withAxes(SiteChartAxis.from(chart.getXAxis(), 0), null);
        var html = GuideSiteGraphRenderer.renderBarChart(
            style,
            new String[] { "A", "B" },
            java.util.List.of(
                new GuideSiteGraphRenderer.SeriesData(
                    "Output",
                    0xff336699,
                    new double[] { 0, 1 },
                    new double[] { 50, 150 },
                    "bar")));
        assertTrue(html.contains("data-cursor-orientation=\"vertical\""), html);
        assertTrue(html.contains("data-x-max=\"200.0\""), html);
        var refs = java.util.regex.Pattern
            .compile(
                "<line class=\"guide-chart-reference\" x1=\"([^\"]+)\" y1=\"([^\"]+)\" x2=\"([^\"]+)\" y2=\"([^\"]+)\"")
            .matcher(html);
        int count = 0;
        while (refs.find()) {
            assertEquals(refs.group(1), refs.group(3));
            assertTrue(Double.parseDouble(refs.group(4)) > Double.parseDouble(refs.group(2)));
            count++;
        }
        assertEquals(2, count);
        assertFalse(html.contains("guide-chart-reference-label"));

        var element = new com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement();
        element.setName("BarChart");
        var reference = new com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement();
        reference.setName("ReferenceLine");
        reference.addAttribute("x", "100");
        element.children()
            .add(reference);
        assertEquals(
            1,
            SiteChartAxis.read(element, "xAxis", "showXGrid", "xGridColor", 0)
                .referenceLines()
                .size());
        assertTrue(
            SiteChartAxis.read(element, "yAxis", "showYGrid", "yGridColor", 0)
                .referenceLines()
                .isEmpty());
    }

    @Test
    void compilesReferencesWithoutTreatingThemAsSeries() {
        var page = new MediaWikiTemplateTestHarness().compile("""
            <LineChart categories="A,B" yAxisMin="0" yAxisMax="200" showYGrid="false">
              <Series data="50,150" />
              <ReferenceLine y="100" label="Target" color="#f2c14e" />
              <ReferenceLine y="201" />
            </LineChart>
            """);
        var charts = new java.util.ArrayList<LytLineChart>();
        page.document()
            .visit(new LytVisitor() {

                @Override
                public Result beforeNode(LytNode node) {
                    if (node instanceof LytLineChart chart) charts.add(chart);
                    return Result.CONTINUE;
                }
            });
        assertEquals(1, charts.size(), MediaWikiTemplateTestHarness.text(page));
        var chart = charts.getFirst();
        assertEquals(
            1,
            chart.getSeries()
                .size());
        assertEquals(
            2,
            chart.getYAxis()
                .getReferenceLines()
                .size());
        assertEquals(
            100,
            chart.getYAxis()
                .getReferenceLines()
                .get(0)
                .y());
        assertFalse(
            MediaWikiTemplateTestHarness.text(page)
                .contains("Expected <Series>"));

        var style = GuideSiteGraphRenderer.ChartStyle.of(320, 200, 0xff111111, 0xff333333, null)
            .withAxes(null, SiteChartAxis.from(chart.getYAxis(), 0));
        var html = GuideSiteGraphRenderer.renderLineChart(
            style,
            new String[] { "A", "B" },
            java.util.List.of(
                new GuideSiteGraphRenderer.SeriesData(
                    "A",
                    0xff336699,
                    new double[] { 0, 1 },
                    new double[] { 50, 150 },
                    "line")),
            false,
            true,
            com.hfstudio.guidenh.guide.document.block.chart.CornerLegendPosition.NONE,
            120,
            64,
            0);
        assertTrue(html.contains("stroke-dasharray=\"4 3\""), html);
        assertTrue(html.contains(">Target</text>"), html);
        assertFalse(html.contains(">201</text>"), html);
    }

    @Test
    void readsReferenceChildrenDuringSiteExport() {
        var chart = new com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement();
        chart.setName("ColumnChart");
        var reference = new com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement();
        reference.setName("ReferenceLine");
        reference.addAttribute("y", "-10.5");
        chart.children()
            .add(reference);
        var axis = SiteChartAxis.read(chart, "yAxis", "showYGrid", "yGridColor", 0);
        assertEquals(
            -10.5,
            axis.referenceLines()
                .getFirst()
                .y());
        assertTrue(
            SiteChartAxis.read(chart, "xAxis", "showXGrid", "xGridColor", 0)
                .referenceLines()
                .isEmpty());
    }

    @Test
    void rejectsNonFiniteOrMissingValues() {
        var text = MediaWikiTemplateTestHarness.textOf("""
            <LineChart categories="A,B">
              <Series data="50,150" />
              <ReferenceLine y="NaN" />
              <ReferenceLine />
            </LineChart>
            """);
        assertTrue(text.contains("ReferenceLine requires a finite y value"), text);
    }
}
