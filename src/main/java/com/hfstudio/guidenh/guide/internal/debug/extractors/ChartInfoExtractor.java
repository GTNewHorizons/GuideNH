package com.hfstudio.guidenh.guide.internal.debug.extractors;

import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.chart.LytBarChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytChartBase;
import com.hfstudio.guidenh.guide.document.block.chart.LytColumnChart;
import com.hfstudio.guidenh.guide.document.block.chart.LytPieChart;
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.HoveredElementInfo;

public class ChartInfoExtractor implements DebugInfoExtractor {

    @Override
    public boolean canHandle(LytNode node) {
        return node instanceof LytChartBase;
    }

    @Override
    public void extract(LytNode node, HoveredElementInfo info) {
        if (node instanceof LytBarChart chart) {
            extractBarChartInfo(chart, info);
        } else if (node instanceof LytColumnChart chart) {
            extractColumnChartInfo(chart, info);
        } else if (node instanceof LytPieChart chart) {
            extractPieChartInfo(chart, info);
        } else if (node instanceof LytChartBase chart) {
            extractBaseChartInfo(chart, info);
        }
    }

    private void extractBarChartInfo(LytBarChart chart, HoveredElementInfo info) {
        info.addExtraInfo("Type: Bar Chart");
        var series = chart.getSeries();
        if (series != null && !series.isEmpty()) {
            info.addExtraInfo("Series: " + series.size());
            if (series.get(0) != null && series.get(0)
                .getName() != null) {
                info.addExtraInfo(
                    "First: " + truncate(
                        series.get(0)
                            .getName(),
                        25));
            }
        }
        var lineOverlays = chart.getLineOverlays();
        if (lineOverlays != null && !lineOverlays.isEmpty()) {
            info.addExtraInfo("Line Overlays: " + lineOverlays.size());
        }
    }

    private void extractColumnChartInfo(LytColumnChart chart, HoveredElementInfo info) {
        info.addExtraInfo("Type: Column Chart");
        var series = chart.getSeries();
        if (series != null && !series.isEmpty()) {
            info.addExtraInfo("Series: " + series.size());
            if (series.get(0) != null && series.get(0)
                .getName() != null) {
                info.addExtraInfo(
                    "First: " + truncate(
                        series.get(0)
                            .getName(),
                        25));
            }
        }
    }

    private void extractPieChartInfo(LytPieChart chart, HoveredElementInfo info) {
        info.addExtraInfo("Type: Pie Chart");
        var slices = chart.getSlices();
        if (slices != null && !slices.isEmpty()) {
            info.addExtraInfo("Slices: " + slices.size());
            if (slices.get(0) != null && slices.get(0)
                .getLabel() != null) {
                info.addExtraInfo(
                    "First: " + truncate(
                        slices.get(0)
                            .getLabel(),
                        25));
            }
        }
    }

    private void extractBaseChartInfo(LytChartBase chart, HoveredElementInfo info) {
        info.addExtraInfo("Type: Chart");
        var title = chart.getTitle();
        if (title != null && !title.isEmpty()) {
            info.addExtraInfo("Title: " + truncate(title, 30));
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text != null ? text : "";
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
