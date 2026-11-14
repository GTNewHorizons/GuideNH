import assert from "node:assert/strict";
import test from "node:test";
import { renderChart } from "../public/modules/visual-renderers.js";

for (const name of ["ColumnChart", "LineChart", "ScatterChart"]) {
  test(`${name} uses faint sparse grid in both directions and keeps a separate fixed reference`, () => {
    const html = renderChart(name, 'categories="A,B" yAxisMin="0" yAxisMax="100" showXGrid="true" showYGrid="true"', '<Series data="0,100" points="0:0,100:100"/><ReferenceLine y="75"/>');
    const grid = [...html.matchAll(/<line\b[^>]*class="guide-chart-grid"[^>]*>/g)].map(match => match[0]);
    assert.ok(grid.length >= 7);
    assert.ok(grid.every(line => line.includes('stroke-dasharray="1 9"') && line.includes('stroke-opacity="0.55"')));
    assert.match(html, /class="guide-chart-reference"[^>]*stroke="rgba\(156,163,175,0.55\)"/);
    assert.doesNotMatch(html, /guide-chart-reference-label/);
  });
}

test("turning off the grid preserves the fixed reference and cursor domain", () => {
  const html = renderChart("ColumnChart", 'showXGrid="false" showYGrid="false"', '<Series data="0,100"/><ReferenceLine y="75"/>');
  assert.doesNotMatch(html, /class="guide-chart-grid"/);
  assert.match(html, /class="guide-chart-reference"/);
  assert.match(html, /data-chart-cursor-domain/);
});

test("bar chart aligns sparse grid to numeric X ticks and category Y ticks", () => {
  const html = renderChart("BarChart", 'categories="A,B" showXGrid="true" showYGrid="true"', '<Series data="20,80"/>');
  const grid = [...html.matchAll(/<line\b[^>]*class="guide-chart-grid"[^>]*>/g)].map(match => match[0]);
  const coordinate = (line, name) => line.match(new RegExp(`${name}="([^"]+)"`))?.[1];
  assert.equal(grid.filter(line => coordinate(line, "x1") === coordinate(line, "x2")).length, 5);
  assert.equal(grid.filter(line => coordinate(line, "y1") === coordinate(line, "y2")).length, 2);
});
