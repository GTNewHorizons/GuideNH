import { test } from "node:test";
import assert from "node:assert/strict";
import { renderChart } from "../public/modules/visual-renderers.js";

for (const name of ["ColumnChart", "LineChart", "ScatterChart"]) {
  test(`${name} renders a reference independently of the grid`, () => {
    const html = renderChart(name, 'yAxisMin="0" yAxisMax="200" showXGrid="false" showYGrid="false"', '<Series points="0:0;1:200" data="0,200"/><ReferenceLine y="100" label="Target & limit" color="#f2c14e"/>');
    assert.match(html, /class="guide-chart-reference"[^>]*y1="77"[^>]*y2="77"/);
    assert.match(html, /stroke-dasharray="4 3"/);
    assert.match(html, /Target &amp; limit/);
    assert.doesNotMatch(html, /class="guide-chart-grid"/);
  });
}

test("reference omits implicit text so values stay on the axis", () => {
  const html = renderChart("LineChart", 'yAxisMin="-100" yAxisMax="100" yAxisUnit="EU/t"', '<Series data="0,100"/><ReferenceLine y="-50"/>');
  assert.match(html, /class="guide-chart-reference"/);
  assert.doesNotMatch(html, /guide-chart-reference-label/);
});

test("invalid, missing and out-of-range references are omitted", () => {
  const html = renderChart("LineChart", 'yAxisMin="0" yAxisMax="200"', '<Series data="0,200"/><ReferenceLine/><ReferenceLine y="NaN"/><ReferenceLine y="Infinity"/><ReferenceLine y="201"/>');
  assert.doesNotMatch(html, /guide-chart-reference/);
});

test("bar references use the numeric X axis with optional legacy y values", () => {
  const html = renderChart("BarChart", 'xAxisMin="0" xAxisMax="200" showXGrid="false" showYGrid="false"', '<Series data="0,200"/><ReferenceLine x="100"/><ReferenceLine y="50"/><ReferenceLine x="201"/>');
  const lines = [...html.matchAll(/<line class="guide-chart-reference"[^>]*>/g)].map(match => match[0]);
  assert.equal(lines.length, 2);
  assert.match(lines[0], /x1="175"[^>]*y1="18"[^>]*x2="175"[^>]*y2="136"/);
  assert.match(lines[1], /x1="108.5"[^>]*x2="108.5"/);
  assert.doesNotMatch(html, /guide-chart-reference-label/);
});
