import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";
import { renderChart } from "../public/modules/visual-renderers.js";
import { installChartCursors } from "../public/modules/chart-cursor.js";

class Element {
  constructor(tag) { this.tag = tag; this.attributes = {}; this.children = []; this.listeners = {}; this.textContent = ""; }
  setAttribute(name, value) { this.attributes[name] = String(value); }
  getAttribute(name) { return this.attributes[name] ?? null; }
  removeAttribute(name) { delete this.attributes[name]; }
  append(...children) { this.children.push(...children); }
  addEventListener(name, handler) { (this.listeners[name] ??= []).push(handler); }
  dispatch(name, value = {}) { for (const fn of this.listeners[name] ?? []) fn(value); }
  getComputedTextLength() { return this.textContent.length * 6; }
}

function fixture(vertical = false) {
  const svg = new Element("svg");
  const meta = new Element("metadata");
  for (const [name, value] of Object.entries({ "plot-left": 70, "plot-right": 300, "plot-top": 20, "plot-bottom": 160, "y-min": -100, "y-max": 100, "y-unit": "EU/t", "y-format": "%.1f" })) meta.setAttribute(`data-${name}`, value);
  if (vertical) for (const [name, value] of Object.entries({ "cursor-orientation": "vertical", "x-min": -100, "x-max": 100, "x-unit": "EU/t", "x-format": "%.1f" })) meta.setAttribute(`data-${name}`, value);
  svg.ownerDocument = { createElementNS: (_namespace, tag) => new Element(tag) };
  svg.querySelector = (selector) => selector === "metadata[data-chart-cursor-domain]" ? meta : null;
  svg.getBoundingClientRect = () => ({ left: 20, top: 30, width: 640, height: 400 });
  svg.viewBox = { baseVal: { width: 320, height: 200 } };
  svg.getScreenCTM = () => ({ inverse: () => ({ left: 20, top: 30, scale: 2 }) });
  svg.createSVGPoint = () => ({ x: 0, y: 0, matrixTransform(matrix) { return { x: (this.x - matrix.left) / matrix.scale, y: (this.y - matrix.top) / matrix.scale }; } });
  const root = { querySelectorAll: () => [svg] };
  installChartCursors(root);
  const overlay = svg.children[0];
  const [line, badge, text] = overlay.children;
  return { root, svg, overlay, line, badge, text, move: (x, y, target) => svg.dispatch("mousemove", { clientX: 20 + x * 2, clientY: 30 + y * 2, target }) };
}

for (const name of ["ColumnChart", "LineChart", "ScatterChart"]) {
  test(`${name} exposes the actual numeric plot domain for the cursor`, () => {
    const html = renderChart(name, 'yAxisMin="-100" yAxisMax="100" yAxisUnit="EU/t" yAxisTickFormat="%.1f"', '<Series data="-100,100" points="0:-100;1:100"/>');
    assert.match(html, /<metadata data-chart-cursor-domain/);
    assert.match(html, /data-y-min="-100"/);
    assert.match(html, /data-y-max="100"/);
    assert.match(html, /data-y-unit="EU\/t"/);
  });
}

test("pie charts do not offer an axis cursor", () => {
  assert.doesNotMatch(renderChart("PieChart", "", '<Slice value="10"/>'), /data-chart-cursor-domain/);
});

test("BarChart exposes its X value domain for a vertical cursor", () => {
  const html = renderChart("BarChart", 'xAxisMin="-100" xAxisMax="100" xAxisUnit="EU/t" xAxisTickFormat="%.1f"', '<Series data="-100,100"/>');
  assert.match(html, /data-cursor-orientation="vertical"/);
  assert.match(html, /data-x-min="-100"/);
  assert.match(html, /data-x-max="100"/);
  assert.match(html, /data-x-unit="EU\/t"/);
  assert.match(html, /data-x-format="%.1f"/);
});

test("bar cursor follows X and highlights the bottom value after SVG scaling", () => {
  const f = fixture(true);
  for (const [x, value] of [[70, "-100.0EU/t"], [127.5, "-50.0EU/t"], [185, "0.0EU/t"], [242.5, "50.0EU/t"], [300, "100.0EU/t"]]) {
    f.move(x, 90);
    assert.notEqual(f.overlay.getAttribute("display"), "none");
    assert.equal(f.line.getAttribute("x1"), String(x));
    assert.equal(f.line.getAttribute("x2"), String(x));
    assert.equal(f.line.getAttribute("y1"), "20");
    assert.equal(f.line.getAttribute("y2"), "160");
    assert.equal(f.text.textContent, value);
    assert.ok(Number(f.text.getAttribute("y")) > 160);
    assert.ok(Number(f.badge.getAttribute("y")) >= 160);
  }
  for (const [x, y] of [[69, 90], [301, 90], [185, 19], [185, 161]]) {
    f.move(x, y);
    assert.equal(f.overlay.getAttribute("display"), "none");
  }
  f.move(185, 90);
  f.svg.dispatch("mouseleave");
  assert.equal(f.overlay.getAttribute("display"), "none");
});

test("mixed pie shapes keep their tooltip and suppress the host cursor", () => {
  for (const name of ["ColumnChart", "BarChart"]) {
    const html = renderChart(name, "", '<Series data="100,100"/><PieInset><Slice name="Primary" value="70" tooltip="Extra detail"/><Slice name="Secondary" value="30"/></PieInset>');
    assert.match(html, /guide-chart-pie-slice[^>]*><title>Primary · 70 \(70.0%\)[\s\S]*Extra detail<\/title>/);
  }
  for (const vertical of [false, true]) {
    const f = fixture(vertical);
    f.move(180, 90);
    f.move(180, 90, { closest: selector => selector.includes("pie") ? {} : null });
    assert.equal(f.overlay.getAttribute("display"), "none");
    f.move(180, 90);
    assert.notEqual(f.overlay.getAttribute("display"), "none");
  }
});

test("cursor follows free plot space after SVG scaling and puts text only on the left", () => {
  const f = fixture();
  assert.equal(f.overlay.getAttribute("display"), "none");
  for (const [y, value] of [[20, "100.0EU/t"], [55, "50.0EU/t"], [90, "0.0EU/t"], [125, "-50.0EU/t"], [160, "-100.0EU/t"]]) {
    f.move(180, y);
    assert.notEqual(f.overlay.getAttribute("display"), "none");
    assert.equal(f.line.getAttribute("y1"), String(y));
    assert.equal(f.line.getAttribute("y2"), String(y));
    assert.equal(f.line.getAttribute("x1"), "70");
    assert.equal(f.line.getAttribute("x2"), "300");
    assert.equal(f.line.getAttribute("stroke-dasharray"), "4 3");
    assert.equal(f.text.textContent, value);
    assert.ok(Number(f.text.getAttribute("x")) < 70);
    assert.ok(Number(f.badge.getAttribute("x")) + Number(f.badge.getAttribute("width")) < 70);
    assert.equal(f.overlay.children.filter(el => el.tag === "text").length, 1);
    assert.equal(f.overlay.getAttribute("pointer-events"), "none");
  }
});

test("cursor hides beyond any plot edge and on mouse leave", () => {
  const f = fixture();
  for (const [x, y] of [[69, 90], [301, 90], [180, 19], [180, 161]]) {
    f.move(180, 90);
    f.move(x, y);
    assert.equal(f.overlay.getAttribute("display"), "none");
  }
  f.move(180, 90);
  f.svg.dispatch("mouseleave");
  assert.equal(f.overlay.getAttribute("display"), "none");
  installChartCursors(f.root);
  assert.equal(f.svg.children.length, 1, "Repeated hydration must not duplicate cursors");
});

test("cursor coordinates respect SVG letterboxing from a minimum CSS height", () => {
  const f = fixture();
  f.svg.getBoundingClientRect = () => ({ left: 20, top: 30, width: 640, height: 500 });
  f.svg.getScreenCTM = () => ({ inverse: () => ({ left: 20, top: 80, scale: 2 }) });
  f.svg.dispatch("mousemove", { clientX: 380, clientY: 260 });
  assert.equal(f.line.getAttribute("y1"), "90");
  assert.equal(f.text.textContent, "0.0EU/t");
});

test("editor and site export use the same cursor behavior", async () => {
  assert.equal(await readFile(new URL("../public/modules/chart-cursor.js", import.meta.url), "utf8"), await readFile(new URL("../../src/main/resources/assets/guidenh/siteexport/chart-cursor.js", import.meta.url), "utf8"));
});
