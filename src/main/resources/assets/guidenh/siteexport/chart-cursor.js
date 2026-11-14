// Keep in sync with assets/guidenh/siteexport/chart-cursor.js (covered by the cursor tests).
const SVG_NS = "http://www.w3.org/2000/svg";
const ACCENT = "#f2c14e";

function formatCursorValue(value, format, unit) {
  const normalized = Math.abs(value) < 1e-9 ? 0 : value;
  let label = Number.isInteger(normalized) ? String(normalized) : normalized.toFixed(2);
  const match = /%(?:\.(\d+))?([fFeEgG])/.exec(format || "");
  if (match) {
    const precision = Math.min(20, Number(match[1] ?? 6));
    const kind = match[2].toLowerCase();
    const formatted = kind === "f" ? normalized.toFixed(precision) : kind === "e" ? normalized.toExponential(precision) : normalized.toPrecision(Math.max(1, precision));
    label = format.replace(match[0], match[2] === match[2].toUpperCase() ? formatted.toUpperCase() : formatted).replaceAll("%%", "%");
  }
  return label + unit;
}

export function installChartCursors(root) {
  for (const svg of root.querySelectorAll("svg.guide-chart, svg.guide-chart-svg")) {
    const meta = svg.querySelector("metadata[data-chart-cursor-domain]");
    if (!meta || svg.chartCursorInstalled) continue;
    const number = (name) => Number(meta.getAttribute(`data-${name}`));
    const left = number("plot-left"), right = number("plot-right");
    const top = number("plot-top"), bottom = number("plot-bottom");
    const vertical = meta.getAttribute("data-cursor-orientation") === "vertical";
    const axis = vertical ? "x" : "y";
    const min = number(`${axis}-min`), max = number(`${axis}-max`);
    if (![left, right, top, bottom, min, max].every(Number.isFinite) || right <= left || bottom <= top || max <= min) continue;
    svg.chartCursorInstalled = true;
    const create = (tag, attributes) => {
      const element = svg.ownerDocument.createElementNS(SVG_NS, tag);
      for (const [name, value] of Object.entries(attributes)) element.setAttribute(name, value);
      return element;
    };
    const overlay = create("g", { "data-chart-cursor": "", "pointer-events": "none", display: "none", "aria-hidden": "true" });
    const line = create("line", { ...(vertical ? { y1: top, y2: bottom } : { x1: left, x2: right }), stroke: ACCENT, "stroke-width": 1, "stroke-dasharray": "4 3" });
    const badge = create("rect", { fill: ACCENT, height: 16, rx: 2 });
    const text = create("text", { "text-anchor": vertical ? "middle" : "end", "dominant-baseline": "central", style: "font-size:10px", "font-family": "inherit", "font-weight": "bold", fill: "#161a22" });
    overlay.append(line, badge, text);
    svg.append(overlay);
    const hide = () => overlay.setAttribute("display", "none");
    svg.addEventListener("mouseleave", hide);
    svg.addEventListener("mousemove", (event) => {
      if (event.target?.closest?.(".guide-chart-pie-inset, .guide-chart-pie-slice")) { hide(); return; }
      const matrix = svg.getScreenCTM();
      if (!matrix) { hide(); return; }
      const pointer = svg.createSVGPoint();
      pointer.x = event.clientX;
      pointer.y = event.clientY;
      const { x, y } = pointer.matrixTransform(matrix.inverse());
      if (x < left || x > right || y < top || y > bottom) { hide(); return; }
      overlay.removeAttribute("display");
      line.setAttribute(vertical ? "x1" : "y1", vertical ? x : y);
      line.setAttribute(vertical ? "x2" : "y2", vertical ? x : y);
      const value = vertical ? min + (x - left) / (right - left) * (max - min) : max - (y - top) / (bottom - top) * (max - min);
      text.textContent = formatCursorValue(value, meta.getAttribute(`data-${axis}-format`), meta.getAttribute(`data-${axis}-unit`) || "");
      text.setAttribute("y", vertical ? bottom + 10 : y);
      text.removeAttribute("textLength");
      const measured = text.getComputedTextLength();
      const textWidth = Math.min(measured, Math.max(1, vertical ? right - left - 8 : left - 12));
      if (measured > textWidth) {
        text.setAttribute("textLength", textWidth);
        text.setAttribute("lengthAdjust", "spacingAndGlyphs");
      }
      const center = Math.max(left + textWidth / 2, Math.min(right - textWidth / 2, x));
      text.setAttribute("x", vertical ? center : left - 7);
      badge.setAttribute("x", vertical ? center - textWidth / 2 - 4 : left - textWidth - 11);
      badge.setAttribute("y", vertical ? bottom + 2 : y - 8);
      badge.setAttribute("width", textWidth + 8);
    });
  }
}
