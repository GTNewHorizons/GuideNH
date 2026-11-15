const SVG_NS = "http://www.w3.org/2000/svg";
const ACCENT = "#f2c14e";
const installedCharts = new WeakSet();

export function createChartValueFormatter(format = "", unit = "") {
  const template = String(format || "");
  const suffix = String(unit || "");
  const fallback = (value) => (Math.abs(value - Math.round(value)) < 1e-6 ? String(Math.round(value)) : value.toFixed(2)) + suffix;
  if (template.length > 256) return fallback;
  const match = /^((?:[^%]|%%)*)%(?:1\$)?([+ 0-]*)(\d+)?(?:\.(\d+))?([feEgG])((?:[^%]|%%)*)$/.exec(template);
  if (!match) {
    if (template && /^(?:[^%]|%%)*$/.test(template)) return () => template.replaceAll("%%", "%") + suffix;
    return fallback;
  }
  const [, prefix, flags, widthText, precisionText, kind, ending] = match;
  const precision = Number(precisionText ?? 6);
  const width = Number(widthText || 0);
  if (precision > 20 || width > 128 || new Set(flags).size !== flags.length
      || flags.includes("+") && flags.includes(" ") || flags.includes("-") && flags.includes("0")
      || (flags.includes("-") || flags.includes("0")) && !width) return fallback;
  const before = prefix.replaceAll("%%", "%");
  const after = ending.replaceAll("%%", "%") + suffix;
  return (value) => {
    let label;
    if (kind === "f") label = value.toFixed(precision);
    else if (kind.toLowerCase() === "e") label = value.toExponential(precision);
    else {
      const digits = Math.max(1, precision);
      const scientific = value.toExponential(digits - 1);
      const exponent = Number(scientific.split("e")[1]);
      label = exponent < -4 || exponent >= digits ? scientific : value.toFixed(Math.max(0, digits - exponent - 1));
    }
    label = label.replace(/e([+-])(\d)$/, (_match, sign, digit) => `e${sign}0${digit}`);
    if (kind === kind.toUpperCase()) label = label.toUpperCase();
    if (Object.is(value, -0)) label = "-" + label;
    else if (value >= 0) label = (flags.includes("+") ? "+" : flags.includes(" ") ? " " : "") + label;
    if (flags.includes("-")) label = label.padEnd(width, " ");
    else if (flags.includes("0")) {
      const sign = /^[+ -]/.test(label) ? label[0] : "";
      label = sign + label.slice(sign.length).padStart(Math.max(0, width - sign.length), "0");
    } else label = label.padStart(width, " ");
    return before + label + after;
  };
}

export function installChartCursors(root) {
  for (const svg of root.querySelectorAll("svg.guide-chart, svg.guide-chart-svg")) {
    const meta = svg.querySelector("metadata[data-chart-cursor-domain]");
    const view = svg.ownerDocument.defaultView;
    if (!meta || !view || installedCharts.has(svg)) continue;
    const number = (name) => {
      const value = meta.getAttribute(`data-${name}`);
      return value === null ? NaN : Number(value);
    };
    const left = number("plot-left"), right = number("plot-right");
    const top = number("plot-top"), bottom = number("plot-bottom");
    const vertical = meta.getAttribute("data-cursor-orientation") === "vertical";
    const axis = vertical ? "x" : "y";
    const min = number(`${axis}-min`), max = number(`${axis}-max`);
    if (![left, right, top, bottom, min, max].every(Number.isFinite) || right <= left || bottom <= top || max <= min) continue;
    installedCharts.add(svg);
    const formatValue = createChartValueFormatter(meta.getAttribute(`data-${axis}-format`), meta.getAttribute(`data-${axis}-unit`));
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
    const pointer = svg.createSVGPoint();
    let frame = null;
    let lastLabel = null;
    let textWidth = 0;
    const hide = () => {
      if (frame !== null) view.cancelAnimationFrame(frame);
      frame = null;
      overlay.setAttribute("display", "none");
    };
    const update = () => {
      frame = null;
      if (!svg.isConnected) return;
      const matrix = svg.getScreenCTM();
      if (!matrix) { hide(); return; }
      let position;
      try { position = pointer.matrixTransform(matrix.inverse()); }
      catch { hide(); return; }
      const { x, y } = position;
      if (!Number.isFinite(x) || !Number.isFinite(y) || x < left || x > right || y < top || y > bottom) { hide(); return; }
      overlay.removeAttribute("display");
      line.setAttribute(vertical ? "x1" : "y1", vertical ? x : y);
      line.setAttribute(vertical ? "x2" : "y2", vertical ? x : y);
      const fraction = vertical ? (x - left) / (right - left) : (bottom - y) / (bottom - top);
      const label = formatValue((1 - fraction) * min + fraction * max);
      if (label !== lastLabel) {
        text.textContent = label;
        text.removeAttribute("textLength");
        const measured = text.getComputedTextLength();
        textWidth = Math.min(measured, Math.max(1, vertical ? right - left - 8 : left - 12));
        if (measured > textWidth) {
          text.setAttribute("textLength", textWidth);
          text.setAttribute("lengthAdjust", "spacingAndGlyphs");
        }
        badge.setAttribute("width", textWidth + 8);
        lastLabel = label;
      }
      const center = Math.max(left + textWidth / 2, Math.min(right - textWidth / 2, x));
      text.setAttribute("x", vertical ? center : left - 7);
      text.setAttribute("y", vertical ? bottom + 10 : y);
      badge.setAttribute("x", vertical ? center - textWidth / 2 - 4 : left - textWidth - 11);
      badge.setAttribute("y", vertical ? bottom + 2 : y - 8);
    };
    svg.addEventListener("mouseleave", hide);
    svg.addEventListener("mousemove", (event) => {
      if (event.target?.closest?.(".guide-chart-pie-slice")) { hide(); return; }
      pointer.x = event.clientX;
      pointer.y = event.clientY;
      if (frame === null) frame = view.requestAnimationFrame(update);
    });
  }
}
