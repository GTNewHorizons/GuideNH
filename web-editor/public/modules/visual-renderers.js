import { attr, colorValue, escapeHtml, numberAttr, parseAttributes, splitComma } from "./html-utils.js";

const PALETTE = Array.from({ length: 8 }, (_entry, index) => `var(--chart-series-${index + 1})`);


function stripMermaidFrontmatter(source) {
  let text = String(source || "").replace(/^\uFEFF/, "").replace(/\r\n/g, "\n").trim();
  if (text.startsWith("---")) {
    const end = text.indexOf("\n---", 3);
    if (end >= 0) text = text.slice(end + 4).trim();
  }
  return text;
}

function mermaidLabelHtml(value) {
  let html = escapeHtml(value);
  html = html.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
  html = html.replace(/\*([^*]+)\*/g, "<em>$1</em>");
  html = html.replace(/`([^`]+)`/g, "<code>$1</code>");
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');
  return html;
}

function mermaidNodeLine(line, index) {
  let text = String(line || "").trim();
  const icon = (text.match(/::icon\(([^)]*)\)/i) || [null, ""])[1];
  const positionMatch = text.match(/::pos\(\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*\)/i);
  const classes = [...text.matchAll(/:::([\w-]+)/g)].map((match) => match[1]);
  text = text.replace(/::icon\([^)]*\)|::pos\([^)]*\)|:::[\w-]+/gi, "").trim();
  const idMatch = text.match(/^([A-Za-z_][\w-]*)\s*(.*)$/);
  const explicitId = Boolean(idMatch && idMatch[2] && /^(?:\(\(|\{\{|\[\[|\(\[|\[\(|\{\||\|\}|\(|\[|\{)/.test(idMatch[2]));
  let id = explicitId ? idMatch[1] : "";
  let label = explicitId ? idMatch[2].trim() : text;
  let shape = "square";
  const shapeMatch = label.match(/^(\(\(|\{\{|\[\[|\(\[|\[\(|\{\||\|\}|\(|\[|\{)([\s\S]*?)(\)\)|\}\}|\]\]|\]|\}|\)|\]\)|\)\])$/);
  if (shapeMatch) {
    label = shapeMatch[2].trim();
    shape = ({ "((": "circle", "{{": "hexagon", "[[": "subprocess", "([": "stadium", "[(": "stadium", "{|": "cylinder", "|}": "cylinder", "(": "rounded", "[": "square", "{": "diamond" })[shapeMatch[1]] || "square";
  }
  label = label.replace(/^["']|["']$/g, "").trim();
  if (!id) id = label.replace(/[^\w]+/g, "-").replace(/^-|-$/g, "").toLowerCase() || "node-" + index;
  return { id, label: label || "Mermaid", shape, icon, classes, position: positionMatch ? { x: Number(positionMatch[1]), y: Number(positionMatch[2]) } : null, children: [], parent: null };
}

function renderMindmap(source, options = {}) {
  const lines = String(source || "").split("\n");
  const nodes = [];
  const stack = [];
  for (const line of lines) {
    if (!line.trim() || /^\s*(?:mindmap|%%)/i.test(line)) continue;
    const indent = (line.match(/^\s*/) || [""])[0].replace(/\t/g, "  ").length;
    const node = mermaidNodeLine(line, nodes.length);
    node.depth = Math.floor(indent / 2);
    while (stack.length && stack[stack.length - 1].depth >= node.depth) stack.pop();
    node.parent = stack.length ? stack[stack.length - 1].id : null;
    if (stack.length) stack[stack.length - 1].children.push(node.id);
    stack.push(node);
    nodes.push(node);
  }
  if (!nodes.length) return '<div class="guide-diagram-empty">Mermaid diagram is empty.</div>';
  const byId = new Map(nodes.map((node) => [node.id, node]));
  const root = nodes.find((node) => !node.parent) || nodes[0];
  const content = new Map((options.nodeContents || []).map((entry) => [String(entry.id || ""), entry.html || ""]));
  const tidy = options.layoutTidy === true || /\blayout\s*:\s*tidy-tree\b/i.test(source);
  const size = new Map(nodes.map((node) => {
    const rich = Boolean(content.get(node.id));
    return [node.id, { width: rich ? Math.max(320, Math.min(380, [...node.label].length * 7 + 34)) : Math.max(140, Math.min(300, [...node.label].length * 7 + 34)), height: rich ? 210 : 52 }];
  }));
  const positions = layoutMindmap(nodes, byId, root, size, tidy);
  for (const node of nodes) {
    if (!node.position) continue;
    const candidate = node.position;
    const nodeSize = size.get(node.id);
    const overlaps = nodes.some((other) => {
      if (other.id === node.id) return false;
      const position = positions.get(other.id);
      const otherSize = size.get(other.id);
      return candidate.x < position.x + otherSize.width + 12 && candidate.x + nodeSize.width + 12 > position.x
        && candidate.y < position.y + otherSize.height + 12 && candidate.y + nodeSize.height + 12 > position.y;
    });
    if (!overlaps) positions.set(node.id, { x: candidate.x, y: candidate.y });
  }
  const minX = Math.min(...nodes.map((node) => positions.get(node.id).x));
  const minY = Math.min(...nodes.map((node) => positions.get(node.id).y));
  for (const point of positions.values()) { point.x += 32 - minX; point.y += 32 - minY; }
  const width = Math.max(420, ...nodes.map((node) => positions.get(node.id).x + size.get(node.id).width + 32));
  const height = Math.max(170, ...nodes.map((node) => positions.get(node.id).y + size.get(node.id).height + 32));
  const connectors = nodes.filter((node) => node.parent).map((node) => {
    const parent = byId.get(node.parent); const p = positions.get(parent.id); const n = positions.get(node.id); const ps = size.get(parent.id); const ns = size.get(node.id);
    const right = n.x >= p.x;
    const px = tidy ? p.x + ps.width / 2 : right ? p.x + ps.width : p.x;
    const py = tidy ? p.y + ps.height : p.y + ps.height / 2;
    const nx = tidy ? n.x + ns.width / 2 : right ? n.x : n.x + ns.width;
    const ny = tidy ? n.y : n.y + ns.height / 2;
    const path = tidy ? `M ${px} ${py} V ${(py + ny) / 2} H ${nx} V ${ny}` : `M ${px} ${py} H ${(px + nx) / 2} V ${ny} H ${nx}`;
    return '<path class="guide-mermaid-connector" d="' + path + '"/>';
  }).join("");
  const boxes = nodes.map((node) => {
    const p = positions.get(node.id); const s = size.get(node.id); const accent = node.classes.includes("success") ? "var(--preview-alert-tip)" : node.classes.includes("warning") ? "var(--preview-alert-warning)" : "var(--preview-diagram-accent)";
    const body = content.get(node.id) || ""; const icon = node.icon ? '<span class="guide-mermaid-node-icon">' + escapeHtml(node.icon) + "</span>" : "";
    const label = '<div class="guide-mermaid-node-label">' + icon + "<span>" + mermaidLabelHtml(node.label) + "</span></div>";
    const rich = body ? '<div class="guide-mermaid-node-rich">' + body + "</div>" : "";
    return '<g transform="translate(' + p.x + "," + p.y + ')" class="guide-mermaid-node-group" data-node-id="' + escapeHtml(node.id) + '"><title>' + escapeHtml(node.label) + '</title><rect class="guide-mermaid-node" width="' + s.width + '" height="' + s.height + '" rx="5" style="--guide-mermaid-accent:' + accent + '"/><rect class="guide-mermaid-node-accent" width="3" height="' + s.height + '" style="fill:' + accent + '"/><foreignObject x="12" y="6" width="' + (s.width - 18) + '" height="' + (s.height - 10) + '"><div xmlns="http://www.w3.org/1999/xhtml" class="guide-mermaid-node-content">' + label + rich + "</div></foreignObject></g>";
  }).join("");
  const viewportWidth = Number(options.width) > 0 ? Number(options.width) : width;
  const viewportHeight = Number(options.height) > 0 ? Number(options.height) : Math.min(540, height);
  return '<div class="guide-mermaid-diagram guide-mermaid-mindmap" data-guide-diagram-width="' + width + '" data-guide-diagram-height="' + height + '" data-guide-diagram-zoom="1" style="width:' + viewportWidth + 'px;max-width:100%;height:' + viewportHeight + 'px"><div class="guide-mermaid-label">MERMAID · mindmap</div><svg viewBox="0 0 ' + width + " " + height + '" role="img" aria-label="Mermaid mindmap">' + connectors + boxes + "</svg></div>";
}

function layoutMindmap(nodes, byId, root, size, tidy) {
  const positions = new Map();
  const gap = 34;
  if (tidy) {
    const subtreeWidths = new Map();
    const levelHeights = new Map();
    const measure = (node, depth) => {
      levelHeights.set(depth, Math.max(levelHeights.get(depth) || 0, size.get(node.id).height));
      const childWidths = node.children.map((id) => measure(byId.get(id), depth + 1));
      const childrenWidth = childWidths.reduce((sum, width) => sum + width, 0) + Math.max(0, childWidths.length - 1) * gap;
      const width = Math.max(size.get(node.id).width, childrenWidth);
      subtreeWidths.set(node.id, width);
      return width;
    };
    measure(root, 0);
    const levelY = new Map();
    let nextY = 0;
    for (const depth of [...levelHeights.keys()].sort((left, right) => left - right)) { levelY.set(depth, nextY); nextY += levelHeights.get(depth) + 76; }
    const place = (node, left, depth) => {
      const span = subtreeWidths.get(node.id);
      positions.set(node.id, { x: left + (span - size.get(node.id).width) / 2, y: levelY.get(depth) });
      const childSpan = node.children.reduce((sum, id) => sum + subtreeWidths.get(id), 0) + Math.max(0, node.children.length - 1) * gap;
      let childLeft = left + (span - childSpan) / 2;
      for (const id of node.children) { place(byId.get(id), childLeft, depth + 1); childLeft += subtreeWidths.get(id) + gap; }
    };
    place(root, 0, 0);
    return positions;
  }

  const sides = { left: [], right: [] };
  root.children.forEach((id, index) => sides[index % 2 ? "left" : "right"].push(byId.get(id)));
  const depths = { left: new Map(), right: new Map() };
  const heights = new Map();
  const measure = (node, side, depth) => {
    depths[side].set(depth, Math.max(depths[side].get(depth) || 0, size.get(node.id).width));
    const childrenHeight = node.children.reduce((sum, id) => sum + measure(byId.get(id), side, depth + 1), 0) + Math.max(0, node.children.length - 1) * gap;
    const height = Math.max(size.get(node.id).height, childrenHeight);
    heights.set(node.id, height);
    return height;
  };
  const total = {};
  for (const side of ["left", "right"]) {
    const branches = sides[side];
    total[side] = branches.reduce((sum, node) => sum + measure(node, side, 1), 0) + Math.max(0, branches.length - 1) * gap;
  }
  const canvasHeight = Math.max(size.get(root.id).height, total.left, total.right);
  positions.set(root.id, { x: 0, y: (canvasHeight - size.get(root.id).height) / 2 });
  const depthX = { left: new Map(), right: new Map() };
  for (const side of ["left", "right"]) {
    let nextX = side === "right" ? size.get(root.id).width + 90 : -90;
    for (const depth of [...depths[side].keys()].sort((left, right) => left - right)) {
      if (side === "left") nextX -= depths.left.get(depth);
      depthX[side].set(depth, nextX);
      nextX += side === "right" ? depths.right.get(depth) + 90 : -90;
    }
    const place = (node, depth, top) => {
      const span = heights.get(node.id);
      positions.set(node.id, { x: depthX[side].get(depth), y: top + (span - size.get(node.id).height) / 2 });
      const childSpan = node.children.reduce((sum, id) => sum + heights.get(id), 0) + Math.max(0, node.children.length - 1) * gap;
      let childTop = top + (span - childSpan) / 2;
      for (const id of node.children) { place(byId.get(id), depth + 1, childTop); childTop += heights.get(id) + gap; }
    };
    let branchTop = (canvasHeight - total[side]) / 2;
    for (const node of sides[side]) { place(node, 1, branchTop); branchTop += heights.get(node.id) + gap; }
  }
  return positions;
}

function flowNode(text) {
  const source = String(text || "").trim();
  const special = source.match(/^([A-Za-z_][\w-]*)\s*@\{\s*([^}]*)\}/);
  if (special) {
    const fields = {};
    for (const part of special[2].split(",")) { const item = part.match(/^\s*([\w-]+)\s*:\s*["']?([\s\S]*?)["']?\s*$/); if (item) fields[item[1]] = item[2].replace(/^["']|["']$/g, ""); }
    const className = (source.match(/:::([\w-]+)/) || [null, ""])[1];
    return { id: special[1], label: fields.label || special[1], shape: fields.shape || "rect", icon: fields.icon || "", classes: className ? [className] : [] };
  }
  const classes = (String(text || "").match(/:::([\w-]+)/) || [null, ""])[1];
  const clean = String(text || "").replace(/:::([\w-]+)/, "").trim();
  const idMatch = clean.match(/^([A-Za-z_][\w-]*)/); if (!idMatch) return null;
  const id = idMatch[1]; const rest = clean.slice(id.length).trim();
  const pair = rest.match(/^(\(\(|\{\{|\[\[|\(\[|\[\(|\{|\[|\()([\s\S]*?)(\)\)|\}\}|\]\]|\)\]|\]\)|\}|\]|\))$/);
  const open = pair ? pair[1] : ""; const label = (pair ? pair[2] : id).replace(/^["']|["']$/g, "").trim();
  return { id, label: label || id, shape: ({ "((": "circle", "{{": "hexagon", "[[": "subprocess", "([": "stadium", "[(": "stadium", "{": "diamond", "[": "rect", "(": "rounded" })[open] || "rect", icon: "", classes: classes ? [classes] : [] };
}

function flowShape(shape, x, y, w, h, fill, stroke) {
  const s = String(shape || "rect").toLowerCase();
  if (s === "diamond") return '<path d="M ' + (x + w / 2) + " " + y + " L " + (x + w) + " " + (y + h / 2) + " L " + (x + w / 2) + " " + (y + h) + " L " + x + " " + (y + h / 2) + ' Z" fill="' + fill + '" stroke="' + stroke + '" class="guide-flowchart-shape"/>';
  if (s === "hexagon") return '<path d="M ' + (x + 10) + " " + y + " H " + (x + w - 10) + " L " + (x + w) + " " + (y + h / 2) + " L " + (x + w - 10) + " " + (y + h) + " H " + (x + 10) + " L " + x + " " + (y + h / 2) + ' Z" fill="' + fill + '" stroke="' + stroke + '" class="guide-flowchart-shape"/>';
  if (s === "cylinder") return '<path d="M ' + x + " " + (y + 7) + " Q " + (x + w / 2) + " " + y + " " + (x + w) + " " + (y + 7) + " V " + (y + h - 7) + " Q " + (x + w / 2) + " " + (y + h) + " " + x + " " + (y + h - 7) + ' Z" fill="' + fill + '" stroke="' + stroke + '" class="guide-flowchart-shape"/>';
  if (s === "subprocess") return '<rect x="' + x + '" y="' + y + '" width="' + w + '" height="' + h + '" rx="3" fill="' + fill + '" stroke="' + stroke + '" class="guide-flowchart-shape"/><rect x="' + (x + 5) + '" y="' + y + '" width="' + (w - 10) + '" height="' + h + '" rx="2" fill="none" stroke="' + stroke + '" class="guide-flowchart-shape"/>';
  if (s === "cloud") return '<path d="M ' + (x + 16) + " " + (y + h - 8) + " C " + (x - 4) + " " + (y + h - 12) + " " + (x + 4) + " " + (y + 8) + " " + (x + 24) + " " + (y + 12) + " C " + (x + 42) + " " + (y - 2) + " " + (x + w - 2) + " " + (y + 2) + " " + (x + w - 8) + " " + (y + h / 2) + " C " + (x + w + 8) + " " + (y + h + 4) + " " + (x + 12) + " " + (y + h + 4) + " " + (x + 16) + " " + (y + h - 8) + ' Z" fill="' + fill + '" stroke="' + stroke + '" class="guide-flowchart-shape"/>';
  if (s === "double-circle") return '<circle cx="' + (x + w / 2) + '" cy="' + (y + h / 2) + '" r="' + (Math.min(w, h) / 2 - 1) + '" fill="' + fill + '" stroke="' + stroke + '" class="guide-flowchart-shape"/><circle cx="' + (x + w / 2) + '" cy="' + (y + h / 2) + '" r="' + (Math.min(w, h) / 2 - 5) + '" fill="none" stroke="' + stroke + '" class="guide-flowchart-shape"/>';
  if (s === "circle") return '<circle cx="' + (x + w / 2) + '" cy="' + (y + h / 2) + '" r="' + (Math.min(w, h) / 2 - 1) + '" fill="' + fill + '" stroke="' + stroke + '" class="guide-flowchart-shape"/>';
  return '<rect x="' + x + '" y="' + y + '" width="' + w + '" height="' + h + '" rx="' + (s === "stadium" ? h / 2 : s === "rounded" ? 8 : 3) + '" fill="' + fill + '" stroke="' + stroke + '" class="guide-flowchart-shape"/>';
}

function renderFlowchart(source, options = {}) {
  const lines = String(source || "").split(/\n|;/).map((line) => line.replace(/%%.*$/, "").trim()).filter(Boolean);
  const declaration = lines.find((line) => /^(?:flowchart|graph)\b/i.test(line)) || "";
  const direction = (declaration.match(/^(?:flowchart|graph)\s+(TB|BT|LR|RL)\b/i) || [null, "TB"])[1].toUpperCase();
  const nodes = new Map(); const edges = []; const defs = new Map(); const assigned = new Map();
  const ensure = (id) => { if (!nodes.has(id)) nodes.set(id, { id, label: id, shape: "rect", icon: "", classes: [] }); return nodes.get(id); };
  const add = (node) => {
    if (!node) return;
    const current = ensure(node.id);
    if (node.label !== node.id || node.shape !== "rect" || node.icon || (node.classes && node.classes.length)) Object.assign(current, node);
  };
  for (const line of lines) {
    if (/^(?:flowchart|graph)\b|^subgraph\b|^end$|^direction\b/i.test(line)) continue;
    const classDef = line.match(/^classDef\s+([\w-]+)\s+(.+)$/i);
    if (classDef) { const style = {}; classDef[2].split(",").forEach((part) => { const item = part.split(":"); if (item.length > 1) style[item[0].trim()] = item.slice(1).join(":").trim(); }); defs.set(classDef[1], style); continue; }
    const classLine = line.match(/^class\s+([\w-]+)\s+([\w-]+)/i); if (classLine) { assigned.set(classLine[1], classLine[2]); continue; }
    if (/^linkStyle\b/i.test(line)) continue;
    const edge = line.match(/^(.+?)\s*(?:-->|---|==>|-.->|--)\s*(?:\|[^|]*\|\s*)?(.+)$/);
    if (edge) { const a = flowNode(edge[1]); const b = flowNode(edge[2]); add(a); add(b); if (a && b) edges.push({ from: a.id, to: b.id }); } else add(flowNode(line));
  }
  if (!nodes.size) nodes.set("flowchart", { id: "flowchart", label: "Flowchart", shape: "rect", icon: "", classes: [] });
  for (const [id, cls] of assigned) ensure(id).classes.push(cls);
  const content = new Map((options.nodeContents || []).map((entry) => [String(entry.id || ""), entry.html || ""]));
  const incoming = new Map([...nodes.keys()].map((id) => [id, 0])); edges.forEach((edge) => incoming.set(edge.to, (incoming.get(edge.to) || 0) + 1));
  const rank = new Map(); const queue = [...nodes.values()].filter((node) => !incoming.get(node.id)).map((node) => node.id);
  if (!queue.length) queue.push(nodes.keys().next().value);
  while (queue.length) { const id = queue.shift(); const level = rank.get(id) || 0; edges.filter((edge) => edge.from === id).forEach((edge) => { rank.set(edge.to, Math.max(rank.get(edge.to) || 0, level + 1)); incoming.set(edge.to, incoming.get(edge.to) - 1); if (!incoming.get(edge.to)) queue.push(edge.to); }); }
  for (const id of nodes.keys()) if (!rank.has(id)) rank.set(id, 0);
  const groups = new Map(); for (const node of nodes.values()) { const level = rank.get(node.id); if (!groups.has(level)) groups.set(level, []); groups.get(level).push(node); }
  const horizontal = direction === "LR" || direction === "RL";
  const positions = layoutFlowchart(groups, content, horizontal, direction);
  const width = Math.max(420, ...[...positions.values()].map((position) => position.x + position.width + 32));
  const height = Math.max(170, ...[...positions.values()].map((position) => position.y + position.height + 32));
  const defsSvg = '<defs><marker id="guide-arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M 0 0 L 8 4 L 0 8 z" fill="var(--preview-diagram-connector)"/></marker></defs>';
  const connectors = edges.map((edge) => { const a = positions.get(edge.from); const b = positions.get(edge.to); if (!a || !b) return ""; const x1 = horizontal ? (direction === "RL" ? a.x : a.x + a.width) : a.x + a.width / 2; const y1 = horizontal ? a.y + a.height / 2 : (direction === "BT" ? a.y : a.y + a.height); const x2 = horizontal ? (direction === "RL" ? b.x + b.width : b.x) : b.x + b.width / 2; const y2 = horizontal ? b.y + b.height / 2 : (direction === "BT" ? b.y + b.height : b.y); const mid = horizontal ? (x1 + x2) / 2 : (y1 + y2) / 2; const path = horizontal ? `M ${x1} ${y1} H ${mid} V ${y2} H ${x2}` : `M ${x1} ${y1} V ${mid} H ${x2} V ${y2}`; return '<path class="guide-flowchart-connector" d="' + path + '" marker-end="url(#guide-arrow)"/>'; }).join("");
  const boxes = [...nodes.values()].map((node) => { const p = positions.get(node.id); const style = defs.get(node.classes[0]) || {}; const fill = style.fill || "var(--preview-diagram-node)"; const stroke = style.stroke || "var(--preview-diagram-accent)"; const color = style.color || "var(--preview-diagram-text)"; const body = content.get(node.id) || ""; const rich = body ? '<div class="guide-flowchart-rich">' + body + "</div>" : ""; const title = '<div class="guide-flowchart-node-title">' + (node.icon ? '<span class="guide-flowchart-icon">' + escapeHtml(node.icon) + "</span>" : "") + mermaidLabelHtml(node.label) + "</div>"; const label = '<div xmlns="http://www.w3.org/1999/xhtml" class="guide-flowchart-label' + (body ? " has-rich-content" : "") + '" style="color:' + escapeHtml(color) + '">' + title + rich + "</div>"; return '<g class="guide-flowchart-node-group" data-node-id="' + escapeHtml(node.id) + '">' + flowShape(node.shape, p.x, p.y, p.width, p.height, escapeHtml(fill), escapeHtml(stroke)) + '<foreignObject x="' + (p.x + 10) + '" y="' + (p.y + 8) + '" width="' + (p.width - 20) + '" height="' + (p.height - 16) + '">' + label + "</foreignObject></g>"; }).join("");
  const viewportWidth = Number(options.width) > 0 ? Number(options.width) : width; const viewportHeight = Number(options.height) > 0 ? Number(options.height) : Math.min(540, height);
  return '<div class="guide-mermaid-diagram guide-mermaid-flowchart" data-guide-diagram-width="' + width + '" data-guide-diagram-height="' + height + '" data-guide-diagram-zoom="1" style="width:' + viewportWidth + 'px;max-width:100%;height:' + viewportHeight + 'px"><div class="guide-mermaid-label">MERMAID · flowchart</div><svg viewBox="0 0 ' + width + " " + height + '" role="img" aria-label="Mermaid flowchart">' + defsSvg + connectors + boxes + "</svg></div>";
}

function layoutFlowchart(groups, content, horizontal, direction) {
  const nodeSize = (node) => {
    if (content.get(node.id)) return { width: 320, height: 210 };
    if (["circle", "double-circle"].includes(node.shape)) { const diameter = Math.max(82, Math.min(130, [...node.label].length * 10 + 32)); return { width: diameter, height: diameter }; }
    return { width: Math.max(140, Math.min(240, [...node.label].length * 8 + 36)), height: 60 };
  };
  const sizes = new Map([...groups.values()].flat().map((node) => [node.id, nodeSize(node)]));
  const levels = [...groups.keys()].sort((left, right) => left - right);
  const crossExtent = (group) => group.reduce((sum, node) => sum + (horizontal ? sizes.get(node.id).height : sizes.get(node.id).width), 0) + Math.max(0, group.length - 1) * 42;
  const widestGroup = Math.max(...levels.map((level) => crossExtent(groups.get(level))));
  const positions = new Map();
  let main = 32;
  for (const level of levels) {
    const group = groups.get(level);
    let cross = 32 + (widestGroup - crossExtent(group)) / 2;
    let mainExtent = 0;
    for (const node of group) {
      const size = sizes.get(node.id);
      positions.set(node.id, { x: horizontal ? main : cross, y: horizontal ? cross : main, ...size });
      cross += (horizontal ? size.height : size.width) + 42;
      mainExtent = Math.max(mainExtent, horizontal ? size.width : size.height);
    }
    main += mainExtent + 84;
  }
  if (direction === "BT" || direction === "RL") {
    const extent = Math.max(...[...positions.values()].map((position) => horizontal ? position.x + position.width : position.y + position.height));
    for (const position of positions.values()) {
      if (horizontal) position.x = 32 + extent - position.x - position.width;
      else position.y = 32 + extent - position.y - position.height;
    }
  }
  return positions;
}

function renderMermaidUnavailable(options = {}) {
  const sourceLabel = String(options.sourceLabel || "").trim();
  const label = sourceLabel ? `Unable to load Mermaid source: ${sourceLabel}` : "Mermaid source is unavailable";
  const width = Number(options.width) > 0 ? Number(options.width) : 420;
  const height = Number(options.height) > 0 ? Number(options.height) : 150;
  return '<div class="guide-mermaid-diagram guide-mermaid-unavailable" data-guide-diagram-width="' + width + '" data-guide-diagram-height="' + height + '" data-guide-diagram-zoom="1" style="width:' + width + 'px;max-width:100%;height:' + height + 'px"><div class="guide-mermaid-label">MERMAID</div><div class="guide-mermaid-unavailable-message">' + escapeHtml(label) + '</div></div>';
}

export function renderMermaidDiagram(source, options = {}) {
  const fence = String.fromCharCode(96) + String.fromCharCode(96) + String.fromCharCode(96);
  const normalized = stripMermaidFrontmatter(String(source || "").replace(new RegExp(fence + "(?:mermaid)?", "gi"), ""));
  const raw = normalized.replace(/<NodeContent\b[^>]*>[\s\S]*?<\/NodeContent>/gi, "").trim();
  if (!raw) return renderMermaidUnavailable(options);
  const first = raw.split("\n").map((line) => line.trim()).find((line) => /^(?:mindmap|flowchart|graph)\b/i.test(line)) || "";
  if (/^mindmap\b/i.test(first)) return renderMindmap(raw, { ...options, layoutTidy: /\blayout\s*:\s*tidy-tree\b/i.test(String(source || "")) });
  if (/^(?:flowchart|graph)\b/i.test(first)) return renderFlowchart(raw, options);
  return '<pre class="guide-code guide-mermaid-fallback"><code class="language-mermaid">' + escapeHtml(raw) + "</code></pre>";
}

export function renderChart(name, rawAttributes, body) {
  const attributes = parseAttributes(rawAttributes);
  const width = numberAttr(attributes, 320, "width");
  const height = numberAttr(attributes, 200, "height");
  const title = attr(attributes, "title") || "";
  const background = colorValue(attr(attributes, "background"), "var(--preview-chart-bg)");
  const border = colorValue(attr(attributes, "border"), "var(--preview-chart-border)");
  const titleColor = colorValue(attr(attributes, "titleColor"), "var(--preview-chart-title)");
  const labelColor = colorValue(attr(attributes, "labelColor"), "var(--preview-chart-label)");
  const children = readChartChildren(body);
  const chartBody = name.toLowerCase() === "piechart" ? renderPie(children.slices, width, height, attributes) : renderCartesian(name, children, width, height, attributes);
  const legendPosition = String(attr(attributes, "legend") || "top").toLowerCase();
  const className = `guide-chart guide-chart-layout-legend-${legendPosition}`;
  return `<section class="${className}" style="width:${width}px;max-width:100%;border-color:${border};background:${background};--guide-chart-label-color:${labelColor}">${title ? `<h4 style="color:${titleColor}">${escapeHtml(title)}</h4>` : ""}${chartBody}</section>`;
}

function readChartChildren(body) {
  const series = [];
  const slices = [];
  const lines = [];
  const insets = [];
  const insetPattern = /<PieInset\b([^>]*)>([\s\S]*?)<\/PieInset>/gi;
  let insetMatch;
  while ((insetMatch = insetPattern.exec(body || ""))) {
    const insetAttributes = parseAttributes(insetMatch[1]);
    const insetSlices = [];
    for (const sliceMatch of insetMatch[2].matchAll(/<Slice\b([^>]*?)(?:\/>|>[^<]*<\/Slice>)/gi)) {
      const attributes = parseAttributes(sliceMatch[1]);
      insetSlices.push({ name: String(attr(attributes, "name") || ""), color: colorValue(attr(attributes, "color"), PALETTE[insetSlices.length % PALETTE.length]), value: Number(attr(attributes, "value")) || 0 });
    }
    insets.push({ attributes: insetAttributes, slices: insetSlices });
  }
  const pattern = /<(Series|LineSeries|Slice)\b([^>]*?)(?:\/>|>([\s\S]*?)<\/\1>)/gi;
  let match;
  while ((match = pattern.exec(body || ""))) {
    const attributes = parseAttributes(match[2]);
    const nestedPoints = [...String(match[3] || "").matchAll(/<Point\b([^>]*)\/?>(?:\s*<\/Point>)?/gi)].map((pointMatch) => {
      const pointAttributes = parseAttributes(pointMatch[1]);
      const x = Number(attr(pointAttributes, "x", "xValue", "index"));
      const y = Number(attr(pointAttributes, "y", "value"));
      return Number.isFinite(x) && Number.isFinite(y) ? { x, y, label: String(attr(pointAttributes, "label") || "") } : null;
    }).filter(Boolean);
    const item = { name: String(attr(attributes, "name") || ""), color: colorValue(attr(attributes, "color"), PALETTE[(series.length + slices.length + lines.length) % PALETTE.length]), values: parseValues(attr(attributes, "data", "values")), points: [...parsePoints(attr(attributes, "points")), ...nestedPoints], value: Number(attr(attributes, "value")) || 0, attributes };
    if (match[1].toLowerCase() === "slice") slices.push(item);
    else if (match[1].toLowerCase() === "lineseries") lines.push(item);
    else series.push(item);
  }
  return { series, slices, lines, insets };
}

function parseValues(value) {
  return splitComma(value).map(Number).filter(Number.isFinite);
}

function parsePoints(value) {
  const text = String(value ?? "").trim();
  if (!text) return [];
  const chunks = text.split(/[;|]/).map((item) => item.trim()).filter(Boolean);
  const points = [];
  for (const chunk of chunks) {
    const numbers = chunk.match(/-?(?:\d+\.?\d*|\.\d+)/g)?.map(Number) || [];
    if (numbers.length >= 2 && /[:\s]/.test(chunk)) {
      for (let index = 0; index + 1 < numbers.length; index += 2) points.push({ x: numbers[index], y: numbers[index + 1] });
    } else if (numbers.length >= 2 && chunks.length === 1) {
      for (let index = 0; index + 1 < numbers.length; index += 2) points.push({ x: numbers[index], y: numbers[index + 1] });
    }
  }
  return points;
}

function renderPie(slices, width, height, attributes) {
  const centerX = width / 2;
  const centerY = height / 2 + 7;
  const radius = Math.max(25, Math.min(width, height) / 2 - 52);
  const total = slices.reduce((sum, slice) => sum + Math.max(0, slice.value), 0) || 1;
  let angle = (Number(attr(attributes, "startAngle", "startAngleDeg")) || -90) * Math.PI / 180;
  const clockwise = booleanAttribute(attributes, "clockwise", true);
  const direction = String(attr(attributes, "direction") || "clockwise").toLowerCase() === "counterclockwise" || !clockwise ? -1 : 1;
  const paths = [];
  const labelPosition = String(attr(attributes, "labelPosition") || "none").toLowerCase();
  for (const slice of slices) {
    const delta = Math.max(0, slice.value) / total * Math.PI * 2;
    const end = angle + direction * delta;
    const large = delta > Math.PI ? 1 : 0;
    const startPoint = [centerX + radius * Math.cos(angle), centerY + radius * Math.sin(angle)];
    const endPoint = [centerX + radius * Math.cos(end), centerY + radius * Math.sin(end)];
    const percentage = total ? slice.value / total * 100 : 0;
    const labelRadius = ["inside", "center"].includes(labelPosition) ? radius * .58 : radius + 16;
    const label = labelPosition === "none" ? "" : `<text x="${centerX + labelRadius * Math.cos(angle + direction * delta / 2)}" y="${centerY + labelRadius * Math.sin(angle + direction * delta / 2)}" text-anchor="middle" class="guide-chart-value-label">${escapeHtml(slice.name)}${labelPosition === "outside" ? ` ${percentage.toFixed(1)}%` : ` ${slice.value}`}</text>`;
    paths.push(`<path class="guide-chart-shape guide-chart-pie-slice" data-chart-value="${slice.value}" data-chart-percentage="${percentage.toFixed(2)}" d="M ${centerX} ${centerY} L ${startPoint[0]} ${startPoint[1]} A ${radius} ${radius} 0 ${large} ${direction > 0 ? 1 : 0} ${endPoint[0]} ${endPoint[1]} Z" fill="${slice.color}" stroke="var(--preview-chart-bg)" stroke-width="1"><title>${escapeHtml(slice.name)} · ${slice.value} (${percentage.toFixed(1)}%)</title></path>${label}`);
    angle = end;
  }
  const legend = slices.map((slice) => `<span class="guide-chart-legend"><i style="background:${slice.color}"></i>${escapeHtml(slice.name)} <small>${slice.value}</small></span>`).join("");
  const legendPosition = String(attr(attributes, "legend") || "bottom").toLowerCase();
  const legendMarkup = legendPosition === "none" ? "" : `<div class="guide-chart-legend-row guide-chart-legend-${legendPosition}">${legend}</div>`;
  return `${legendPosition === "top" ? legendMarkup : ""}<svg class="guide-chart-svg" viewBox="0 0 ${width} ${height - 30}" role="img">${paths.join("")}</svg>${legendPosition === "top" ? "" : legendMarkup}`;
}

function renderCartesian(name, children, width, height, attributes) {
  const categories = splitComma(attr(attributes, "categories"));
  const chartName = name.toLowerCase();
  const isLine = chartName === "linechart" || chartName === "scatterchart";
  const isBar = chartName === "barchart";
  const series = isLine ? (children.series.length ? children.series : children.lines) : (children.series.length ? children.series : [{ name: "Values", color: PALETTE[0], values: [] }]);
  const lineSeries = isLine ? series : children.lines;
  const values = series.flatMap((item) => item.values).concat(lineSeries.flatMap((item) => item.values));
  const pointValues = [...series, ...lineSeries].flatMap((item) => item.points || []).map((point) => point.y);
  const allValues = values.concat(pointValues);
  const yMin = Number.isFinite(Number(attr(attributes, "yAxisMin"))) ? Number(attr(attributes, "yAxisMin")) : Math.min(0, ...allValues, 0);
  const yMax = Number.isFinite(Number(attr(attributes, "yAxisMax"))) ? Number(attr(attributes, "yAxisMax")) : Math.max(1, ...allValues, 1);
  const numericX = booleanAttribute(attributes, "numericX", chartName === "scatterchart" || (!categories.length && [...series, ...lineSeries].some((item) => item.points.length > 0)));
  const pointXValues = [...series, ...lineSeries].flatMap((item) => item.points || []).map((point) => point.x);
  const xMin = Number.isFinite(Number(attr(attributes, "xAxisMin"))) ? Number(attr(attributes, "xAxisMin")) : (isBar ? 0 : Math.min(0, ...pointXValues, 0));
  const xMax = Number.isFinite(Number(attr(attributes, "xAxisMax"))) ? Number(attr(attributes, "xAxisMax")) : (isBar ? Math.max(1, ...allValues, 1) : Math.max(1, ...pointXValues, 1));
  const left = 42;
  const top = 18;
  const right = 12;
  const bottom = 34;
  const plotWidth = Math.max(80, width - left - right);
  const plotHeight = Math.max(70, height - top - bottom - 30);
  const showYGrid = booleanAttribute(attributes, "showYGrid", true);
  const showXGrid = booleanAttribute(attributes, "showXGrid", false);
  const grid = Array.from({ length: 5 }, (_item, index) => {
    const y = top + plotHeight - index * plotHeight / 4;
    const value = yMin + (yMax - yMin) * index / 4;
    return `${showYGrid ? `<line x1="${left}" y1="${y}" x2="${left + plotWidth}" y2="${y}" class="guide-chart-grid"/>` : ""}<text x="${left - 7}" y="${y + 4}" text-anchor="end" class="guide-chart-axis-label">${formatChartNumber(value)}${escapeHtml(attr(attributes, "yAxisUnit") || "")}</text>`;
  }).join("");
  const count = Math.max(categories.length, ...series.map((item) => Math.max(item.values.length, item.points.length)), ...lineSeries.map((item) => Math.max(item.values.length, item.points.length)), 1);
  const categoryLabels = numericX
    ? Array.from({ length: 5 }, (_item, index) => `${showXGrid ? `<line x1="${left + index * plotWidth / 4}" y1="${top}" x2="${left + index * plotWidth / 4}" y2="${top + plotHeight}" class="guide-chart-grid"/>` : ""}<text x="${left + index * plotWidth / 4}" y="${top + plotHeight + 21}" text-anchor="middle" class="guide-chart-axis-label">${formatChartNumber(xMin + (xMax - xMin) * index / 4)}</text>`).join("")
    : isBar
    ? Array.from({ length: count }, (_item, index) => `<text x="${left - 7}" y="${top + (index + .5) * plotHeight / count + 4}" text-anchor="end" class="guide-chart-axis-label">${escapeHtml(categories[index] || String(index + 1))}</text>`).join("")
    : Array.from({ length: count }, (_item, index) => `${showXGrid ? `<line x1="${left + (index + .5) * plotWidth / count}" y1="${top}" x2="${left + (index + .5) * plotWidth / count}" y2="${top + plotHeight}" class="guide-chart-grid"/>` : ""}<text x="${left + (index + .5) * plotWidth / count}" y="${top + plotHeight + 21}" text-anchor="middle" class="guide-chart-axis-label">${escapeHtml(categories[index] || String(index + 1))}</text>`).join("");
  const labelPosition = String(attr(attributes, "labelPosition") || "none").toLowerCase();
  const xAxisLabel = attr(attributes, "xAxisLabel", "xLabel");
  const yAxisLabel = attr(attributes, "yAxisLabel", "yLabel");
  const axisLabels = `${xAxisLabel ? `<text x="${left + plotWidth / 2}" y="${top + plotHeight + 32}" text-anchor="middle" class="guide-chart-axis-label guide-chart-axis-title">${escapeHtml(xAxisLabel)}${attr(attributes, "xAxisUnit") ? ` (${escapeHtml(attr(attributes, "xAxisUnit"))})` : ""}</text>` : ""}${yAxisLabel ? `<text x="12" y="${top + plotHeight / 2}" text-anchor="middle" transform="rotate(-90 12 ${top + plotHeight / 2})" class="guide-chart-axis-label guide-chart-axis-title">${escapeHtml(yAxisLabel)}${attr(attributes, "yAxisUnit") ? ` (${escapeHtml(attr(attributes, "yAxisUnit"))})` : ""}</text>` : ""}`;
  let marks = "";
  const renderLines = (items) => items.map((item) => {
    const points = item.points.length ? item.points : item.values.map((value, index) => ({ x: index, y: value }));
    const xPosition = (point) => numericX ? left + (point.x - xMin) / Math.max(1e-9, xMax - xMin) * plotWidth : left + (point.x + .5) * plotWidth / count;
    const yPosition = (point) => top + (yMax - point.y) / Math.max(1e-9, yMax - yMin) * plotHeight;
    const path = points.map((point, index) => `${index ? "L" : "M"} ${xPosition(point)} ${yPosition(point)}`).join(" ");
    const pointString = points.map((point) => `${xPosition(point)},${yPosition(point)}`).join(" ");
    const line = chartName === "scatterchart" ? "" : `<path class="guide-chart-shape guide-chart-line" data-chart-series="${escapeHtml(item.name)}" d="${path}" fill="none" stroke="${item.color}" stroke-width="2"><title>${escapeHtml(item.name)}</title></path>`;
    const dots = booleanAttribute(attributes, "showPoints", true) ? points.map((point) => `<circle class="guide-chart-shape guide-chart-point" cx="${xPosition(point)}" cy="${yPosition(point)}" r="3.8" fill="${item.color}" stroke="var(--preview-chart-bg)" stroke-width=".8"><title>${escapeHtml(item.name)}: ${point.y}</title></circle>`).join("") : "";
    const polyline = pointString ? `<polyline class="guide-chart-shape" data-plot-label="${escapeHtml(item.name)}" points="${pointString}" fill="none" stroke="transparent" stroke-width="10"/>` : "";
    return line + polyline + dots;
  }).join("");
  if (isLine) {
    marks = renderLines(series);
  } else if (isBar) {
    const groupHeight = plotHeight / count;
    const barHeight = Math.max(4, groupHeight * .72 / Math.max(1, series.length));
    marks = series.map((item, seriesIndex) => item.values.map((value, index) => {
      const y = top + index * groupHeight + groupHeight * .14 + seriesIndex * barHeight;
      const barWidth = Math.max(0, (value - Math.min(0, xMin)) / Math.max(1e-9, xMax - Math.min(0, xMin)) * plotWidth);
      const label = labelPosition !== "none" ? `<text x="${left + barWidth + 4}" y="${y + barHeight / 2 + 3}" class="guide-chart-value-label">${formatChartNumber(value)}</text>` : "";
      return `<rect class="guide-chart-shape guide-chart-bar" x="${left}" y="${y}" width="${barWidth}" height="${barHeight - 2}" rx="2" fill="${item.color}"><title>${escapeHtml(item.name)}: ${value}</title></rect>${label}`;
    }).join("")).join("");
  } else {
    const groupWidth = plotWidth / count;
    const barWidth = Math.max(4, groupWidth * .72 / Math.max(1, series.length));
    marks = series.map((item, seriesIndex) => item.values.map((value, index) => {
      const x = left + index * groupWidth + groupWidth * .14 + seriesIndex * barWidth;
      const zeroY = top + (yMax - 0) / Math.max(1e-9, yMax - yMin) * plotHeight;
      const valueY = top + (yMax - value) / Math.max(1e-9, yMax - yMin) * plotHeight;
      const y = Math.min(zeroY, valueY);
      const label = labelPosition !== "none" ? `<text x="${x + (barWidth - 2) / 2}" y="${labelPosition === "below" ? Math.max(zeroY, valueY) + 12 : Math.min(zeroY, valueY) - 4}" text-anchor="middle" class="guide-chart-value-label">${formatChartNumber(value)}</text>` : "";
      return `<rect class="guide-chart-shape guide-chart-column" x="${x}" y="${y}" width="${barWidth - 2}" height="${Math.max(0, Math.abs(zeroY - valueY))}" rx="2" fill="${item.color}"><title>${escapeHtml(item.name)}: ${value}</title></rect>${label}`;
    }).join("")).join("") + (lineSeries.length ? renderLines(lineSeries) : "");
  }
  const legend = [...new Map([...series, ...lineSeries].map((item) => [item.name, item])).values()].map((item) => `<span class="guide-chart-legend"><i style="background:${item.color}"></i>${escapeHtml(item.name)}</span>`).join("");
  const legendPosition = String(attr(attributes, "legend") || "top").toLowerCase();
  const legendMarkup = legendPosition === "none" ? "" : `<div class="guide-chart-legend-row guide-chart-legend-${legendPosition}">${legend}</div>`;
  const cornerLegend = renderCornerLegend(attributes, [...new Map([...series, ...lineSeries].map((item) => [item.name, item])).values()], width, height);
  const insetMarkup = children.insets.map((inset) => renderPieInset(inset, width, height)).join("");
  return `${legendPosition === "top" ? legendMarkup : ""}<svg class="guide-chart-svg" viewBox="0 0 ${width} ${height - 30}" role="img">${grid}${marks}${categoryLabels}${axisLabels}${cornerLegend}${insetMarkup}</svg>${legendPosition === "top" ? "" : legendMarkup}`;
}

function renderCornerLegend(attributes, series, width = 320, height = 200) {
  const position = String(attr(attributes, "cornerLegend") || "none").toLowerCase();
  if (position === "none" || !series.length) return "";
  const anchor = position.includes("left") ? "start" : "end";
  const x = position.includes("left") ? 8 : Math.max(8, width - 8);
  const y = position.includes("bottom") ? Math.max(14, height - 30) : 14;
  const rows = series.map((item, index) => `<text x="${x}" y="${y + index * 12}" text-anchor="${anchor}" class="guide-chart-corner-legend"><tspan fill="${item.color}">●</tspan> ${escapeHtml(item.name)}</text>`).join("");
  return `<g class="guide-chart-corner-legend-group">${rows}</g>`;
}

function formatChartNumber(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return "";
  return Number.isInteger(number) ? String(number) : number.toFixed(2).replace(/0+$/, "").replace(/\.$/, "");
}

function renderPieInset(inset, width, height) {
  const size = Math.max(28, Math.min(120, numberAttr(inset.attributes, 60, "size")));
  const position = String(attr(inset.attributes, "position") || "topRight").toLowerCase();
  const x = position.includes("left") ? 8 : width - size - 8;
  const y = position.includes("bottom") ? height - size - 38 : 8;
  const total = inset.slices.reduce((sum, slice) => sum + Math.max(0, slice.value), 0) || 1;
  let angle = (Number(attr(inset.attributes, "startAngleDeg", "startAngle")) || -90) * Math.PI / 180;
  const paths = [];
  const radius = size / 2 - 3;
  for (const slice of inset.slices) {
    const delta = Math.max(0, slice.value) / total * Math.PI * 2;
    const end = angle + delta;
    const startPoint = [size / 2 + radius * Math.cos(angle), size / 2 + radius * Math.sin(angle)];
    const endPoint = [size / 2 + radius * Math.cos(end), size / 2 + radius * Math.sin(end)];
    const percentage = slice.value / total * 100;
    paths.push(`<path class="guide-chart-shape guide-chart-pie-slice" d="M ${size / 2} ${size / 2} L ${startPoint[0]} ${startPoint[1]} A ${radius} ${radius} 0 ${delta > Math.PI ? 1 : 0} 1 ${endPoint[0]} ${endPoint[1]} Z" fill="${slice.color}" stroke="var(--preview-chart-bg)" stroke-width=".6"><title>${escapeHtml(slice.name)} · ${slice.value} (${percentage.toFixed(1)}%)</title></path>`);
    angle = end;
  }
  const title = attr(inset.attributes, "title") ? `<text x="${size / 2}" y="${size + 11}" text-anchor="middle" class="guide-chart-axis-label">${escapeHtml(attr(inset.attributes, "title"))}</text>` : "";
  return `<g transform="translate(${x},${y})" class="guide-chart-pie-inset"><rect width="${size}" height="${size + (title ? 16 : 0)}" rx="4" fill="var(--preview-chart-bg)" stroke="var(--preview-chart-border)"/>${paths.join("")}${title}</g>`;
}

export function renderFunctionGraph(rawAttributes, body, expressionOverride = null) {
  const header = readFunctionGraphHeader(body);
  const attributes = { ...header.attributes, ...parseAttributes(rawAttributes) };
  const width = numberAttr(attributes, 360, "width");
  const height = numberAttr(attributes, 220, "height");
  const graphBackground = colorValue(attr(attributes, "background"), "var(--preview-chart-bg)");
  const graphBorder = colorValue(attr(attributes, "border"), "var(--preview-chart-border)");
  const graphGrid = colorValue(attr(attributes, "gridColor"), "var(--preview-chart-grid)");
  const graphAxis = colorValue(attr(attributes, "axisColor"), "var(--preview-chart-axis)");
  const graphBody = header.body;
  const plots = expressionOverride ? [{ expression: expressionOverride, color: colorValue(attr(attributes, "color"), PALETTE[0]), label: "", inverse: booleanAttribute(attributes, "inverse", false) }] : readPlots(graphBody);
  const quadrants = String(attr(attributes, "quadrants") || "").toLowerCase();
  const explicitX = attr(attributes, "xRange") !== undefined || attr(attributes, "xMin") !== undefined || attr(attributes, "xMax") !== undefined;
  const explicitY = attr(attributes, "yRange") !== undefined || attr(attributes, "yMin") !== undefined || attr(attributes, "yMax") !== undefined;
  const defaultX = quadrants === "all" || /[23]/.test(quadrants) ? -6 : 0;
  const defaultY = quadrants === "all" || /[34]/.test(quadrants) ? -4 : 0;
  const xRange = attr(attributes, "xRange") ?? `${attr(attributes, "xMin") ?? defaultX}..${attr(attributes, "xMax") ?? 6}`;
  const yRange = attr(attributes, "yRange") ?? `${attr(attributes, "yMin") ?? defaultY}..${attr(attributes, "yMax") ?? 4}`;
  let [minX, maxX] = splitGraphRange(xRange, defaultX, 6);
  let [minY, maxY] = splitGraphRange(yRange, defaultY, 4);
  if (!explicitY && !quadrants && plots.some((plot) => samplePlotMinimum(plot, minX, maxX) < 0)) {
    minX = explicitX ? minX : -6;
    minY = -4;
  }
  const markedPoints = readFunctionPoints(graphBody, plots, minX, maxX, minY, maxY);
  const left = 38;
  const top = 15;
  const plotWidth = width - left - 12;
  const plotHeight = height - top - 35;
  const mapX = (value) => left + (value - minX) / (maxX - minX) * plotWidth;
  const mapY = (value) => top + (maxY - value) / (maxY - minY) * plotHeight;
  const grid = [];
  for (let x = Math.ceil(minX); x <= maxX; x += 1) grid.push(`<line x1="${mapX(x)}" y1="${top}" x2="${mapX(x)}" y2="${top + plotHeight}" class="guide-chart-grid"/>`);
  for (let y = Math.ceil(minY); y <= maxY; y += 1) grid.push(`<line x1="${left}" y1="${mapY(y)}" x2="${left + plotWidth}" y2="${mapY(y)}" class="guide-chart-grid"/>`);
  const paths = plots.map((plot) => {
    const evaluator = compileExpression(plot.expression);
    const points = [];
    const sampled = [];
    let previousValid = false;
    for (let index = 0; index <= 180; index += 1) {
      const input = (plot.inverse ? minY + (maxY - minY) * index / 180 : minX + (maxX - minX) * index / 180);
      if (plot.domain && !inDomain(input, plot.domain)) continue;
      const output = evaluator(input);
      const x = plot.inverse ? output : input;
      const y = plot.inverse ? input : output;
      if (Number.isFinite(y) && Number.isFinite(x) && Math.abs(y) < 100000 && Math.abs(x) < 100000) {
        points.push(`${previousValid ? "L" : "M"} ${mapX(x)} ${mapY(y)}`);
        sampled.push({ x, y });
        previousValid = true;
      } else previousValid = false;
    }
    if (!points.length) return "";
    const polylinePoints = sampled.map((point) => `${mapX(point.x)},${mapY(point.y)}`).join(" ");
    const metadata = `data-plot-label="${escapeHtml(plot.label)}" data-plot-expression="${escapeHtml(plot.expression)}" data-plot-inverse="${plot.inverse ? "true" : "false"}" data-plot-show-function="${plot.showFunction === false ? "false" : "true"}" data-plot-show-values="${plot.showValues === false ? "false" : "true"}" data-plot-tooltip="${escapeHtml(plot.tooltip || "")}"`;
    return `<path class="guide-chart-shape guide-function-path" ${metadata} d="${points.join(" ")}" fill="none" stroke="${plot.color}" stroke-width="2"><title>${escapeHtml(plot.expression)}</title></path><polyline class="guide-chart-shape guide-function-polyline" ${metadata} points="${polylinePoints}" fill="none" stroke="transparent" stroke-width="10"><title>${escapeHtml(plot.expression)}</title></polyline>`;
  }).join("");
  const legend = plots.filter((plot) => plot.label).map((plot) => `<span class="guide-chart-legend"><i style="background:${plot.color}"></i>${escapeHtml(plot.label)}</span>`).join("");
  const pointMarkup = markedPoints.map((point) => `<circle class="guide-chart-shape guide-function-point" cx="${mapX(point.x)}" cy="${mapY(point.y)}" r="4" fill="${point.color}" stroke="var(--preview-chart-bg)" stroke-width="1"><title>${escapeHtml(point.label || `${point.x}, ${point.y}`)}</title></circle>`).join("");
  const showGrid = booleanAttribute(attributes, "showGrid", true);
  const showAxes = booleanAttribute(attributes, "showAxes", true);
  const axisLabels = `${attr(attributes, "xLabel", "xAxisLabel") ? `<text x="${left + plotWidth / 2}" y="${top + plotHeight + 30}" text-anchor="middle" class="guide-chart-axis-label guide-chart-axis-title">${escapeHtml(attr(attributes, "xLabel", "xAxisLabel"))}</text>` : ""}${attr(attributes, "yLabel", "yAxisLabel") ? `<text x="12" y="${top + plotHeight / 2}" text-anchor="middle" transform="rotate(-90 12 ${top + plotHeight / 2})" class="guide-chart-axis-label guide-chart-axis-title">${escapeHtml(attr(attributes, "yLabel", "yAxisLabel"))}</text>` : ""}`;
  const cornerLegend = renderCornerLegend(attributes, plots, width, height);
  const metadata = `<metadata data-plot-domain data-x-min="${minX}" data-x-max="${maxX}" data-y-min="${minY}" data-y-max="${maxY}" data-plot-left="${left}" data-plot-right="${left + plotWidth}" data-plot-top="${top}" data-plot-bottom="${top + plotHeight}"></metadata>`;
  return `<section class="guide-function-graph" style="width:${width}px;max-width:100%;border-color:${graphBorder};background:${graphBackground};--guide-chart-grid-color:${graphGrid};--guide-chart-axis-color:${graphAxis}">${attr(attributes, "title") ? `<h4>${escapeHtml(attr(attributes, "title"))}</h4>` : ""}<svg class="guide-chart-svg guide-function-graph" viewBox="0 0 ${width} ${height - 25}" role="img">${metadata}${showGrid ? grid.join("") : ""}${showAxes ? `<line x1="${left}" y1="${mapY(0)}" x2="${left + plotWidth}" y2="${mapY(0)}" class="guide-chart-axis"/><line x1="${mapX(0)}" y1="${top}" x2="${mapX(0)}" y2="${top + plotHeight}" class="guide-chart-axis"/>` : ""}${paths}${pointMarkup}${axisLabels}${cornerLegend}</svg>${legend && String(attr(attributes, "cornerLegend") || "none").toLowerCase() === "none" ? `<div class="guide-chart-legend-row">${legend}</div>` : ""}</section>`;
}

function booleanAttribute(attributes, name, fallback) {
  const value = attr(attributes, name);
  if (value === undefined) return fallback;
  return ![false, "false", "0", "no", "off"].includes(value);
}

function splitGraphRange(value, fallbackMin, fallbackMax) {
  const match = String(value ?? "").match(/^\s*(.+?)\.\.(.+?)\s*$/);
  if (!match) return [fallbackMin, fallbackMax];
  const left = resolveGraphNumber(match[1]);
  const right = resolveGraphNumber(match[2]);
  return Number.isFinite(left) && Number.isFinite(right) && left !== right ? [Math.min(left, right), Math.max(left, right)] : [fallbackMin, fallbackMax];
}

function samplePlotMinimum(plot, minX, maxX) {
  const evaluator = compileExpression(plot.expression);
  let minimum = Number.POSITIVE_INFINITY;
  for (let index = 0; index <= 48; index += 1) {
    const input = minX + (maxX - minX) * index / 48;
    const output = plot.inverse ? input : evaluator(input);
    if (Number.isFinite(output)) minimum = Math.min(minimum, output);
  }
  return minimum;
}

function inDomain(value, expression) {
  const text = String(expression || "").trim();
  if (text.includes("..")) {
    const [left, right] = splitGraphRange(text, Number.NaN, Number.NaN);
    if (Number.isFinite(left) && Number.isFinite(right)) return value >= left && value <= right;
  }
  for (const condition of text.split(",")) {
    const match = condition.trim().match(/^x\s*(<=|>=|<|>)\s*(-?(?:\d+\.?\d*|\.\d+|pi|tau))$/i);
    if (!match) continue;
    const target = resolveGraphNumber(match[2]);
    if (match[1] === "<" && !(value < target) || match[1] === "<=" && !(value <= target) || match[1] === ">" && !(value > target) || match[1] === ">=" && !(value >= target)) return false;
  }
  return true;
}

function readFunctionPoints(body, plots, minX = -6, maxX = 6, minY = -4, maxY = 4) {
  const points = [];
  for (const line of String(body || "").split(/\r?\n/)) {
    const cleaned = line.replace(/\s+#.*$/, "").trim();
    const fixed = cleaned.match(/^:\s*(-?(?:\d+\.?\d*|\.\d+))\s*,\s*(-?(?:\d+\.?\d*|\.\d+))\s*$/);
    if (fixed) points.push({ x: Number(fixed[1]), y: Number(fixed[2]), color: PALETTE[1], label: "" });
    const anchored = cleaned.match(/^@\s*plot\s*=\s*(\d+)\s+(atX|atY)\s*=\s*(-?(?:\d+\.?\d*|\.\d+))\s*$/i);
    if (anchored) {
      const plotIndex = Number(anchored[1]);
      const anchorValue = Number(anchored[3]);
      const evaluator = compileExpression(plots[plotIndex]?.expression || "0");
      let x = anchored[2].toLowerCase() === "atx" ? anchorValue : 0;
      let y = anchored[2].toLowerCase() === "aty" ? anchorValue : evaluator(x);
      if (anchored[2].toLowerCase() === "aty") {
        let distance = Number.POSITIVE_INFINITY;
        for (let index = 0; index <= 180; index += 1) {
          const candidate = -10 + index / 180 * 20;
          const delta = Math.abs(evaluator(candidate) - y);
          if (delta < distance) { distance = delta; x = candidate; }
        }
      }
      if (Number.isFinite(x) && Number.isFinite(y)) points.push({ x, y, color: PALETTE[1], label: "" });
    }
  }
  for (const plot of plots) {
    const everyX = Number(plot.pointEveryX);
    const everyY = Number(plot.pointEveryY);
    const evaluator = compileExpression(plot.expression);
    if (Number.isFinite(everyX) && everyX > 0) {
      const start = Math.ceil(minX / everyX) * everyX;
      for (let x = start; x <= maxX + everyX / 2; x += everyX) {
        const y = evaluator(x);
        if (!Number.isFinite(y) || (plot.domain && !inDomain(x, plot.domain))) continue;
        points.push({ x, y, color: plot.color, label: plot.autoPointLabel === "x" ? `x = ${formatChartNumber(x)}` : plot.autoPointLabel === "y" ? `y = ${formatChartNumber(y)}` : "" });
      }
    }
    if (Number.isFinite(everyY) && everyY > 0) {
      const start = Math.ceil(minY / everyY) * everyY;
      for (let y = start; y <= maxY + everyY / 2; y += everyY) {
        let bestX = minX;
        let bestDistance = Number.POSITIVE_INFINITY;
        for (let index = 0; index <= 120; index += 1) {
          const candidate = minX + (maxX - minX) * index / 120;
          const value = evaluator(candidate);
          const distance = Math.abs(value - y);
          if (Number.isFinite(distance) && distance < bestDistance) { bestDistance = distance; bestX = candidate; }
        }
        if (Number.isFinite(bestDistance) && bestDistance < Math.max(.2, everyY * .2)) points.push({ x: bestX, y, color: plot.color, label: plot.autoPointLabel === "y" ? `y = ${formatChartNumber(y)}` : plot.autoPointLabel === "x" ? `x = ${formatChartNumber(bestX)}` : "" });
      }
    }
  }
  for (const match of String(body || "").matchAll(/<Point\b([^>]*?)\/?>(?:[\s\S]*?<\/Point>)?/gi)) {
    const attributes = parseAttributes(match[1]);
    let x = Number(attr(attributes, "x"));
    let y = Number(attr(attributes, "y"));
    const plotIndex = Number(attr(attributes, "plot"));
    if (!Number.isFinite(x) && Number.isFinite(plotIndex) && attr(attributes, "atX") !== undefined) {
      x = Number(attr(attributes, "atX"));
      const evaluator = compileExpression(plots[plotIndex]?.expression || "0");
      y = evaluator(x);
    }
    if (!Number.isFinite(y) && Number.isFinite(plotIndex) && attr(attributes, "atY") !== undefined) {
      y = Number(attr(attributes, "atY"));
      const evaluator = compileExpression(plots[plotIndex]?.expression || "0");
      let candidate = 0;
      let distance = Number.POSITIVE_INFINITY;
      for (let index = 0; index <= 180; index += 1) {
        const value = -10 + index / 180 * 20;
        const delta = Math.abs(evaluator(value) - y);
        if (delta < distance) { distance = delta; candidate = value; }
      }
      x = candidate;
    }
    if (Number.isFinite(x) && Number.isFinite(y)) points.push({ x, y, color: colorValue(attr(attributes, "color"), PALETTE[1]), label: attr(attributes, "label") });
  }
  return points;
}

function readPlots(body) {
  const plots = [];
  const pattern = /<(Plot|Function)\b([^>]*?)(?:\/>|>([\s\S]*?)<\/\1>)/gi;
  let match;
  while ((match = pattern.exec(body || ""))) {
    const attributes = parseAttributes(match[2]);
    const expression = attr(attributes, "expr") || attr(attributes, "expression");
    if (expression) plots.push({ expression: String(expression), color: colorValue(attr(attributes, "color"), PALETTE[plots.length % PALETTE.length]), label: String(attr(attributes, "label") || ""), domain: attr(attributes, "domain"), inverse: booleanAttribute(attributes, "inverse", false), pointEveryX: attr(attributes, "pointEveryX"), pointEveryY: attr(attributes, "pointEveryY"), autoPointLabel: attr(attributes, "autoPointLabel"), showFunction: booleanAttribute(attributes, "showFunction", true), showValues: booleanAttribute(attributes, "showValues", true), tooltip: attr(attributes, "tooltip"), tooltipHtml: match[3]?.trim() || "" });
  }
  if (!plots.length) {
    for (const line of String(body || "").split(/\r?\n/)) {
      const cleaned = line.replace(/\s+#.*$/, "").trim();
      if (!cleaned || cleaned.startsWith(":") || cleaned.startsWith("@") || /^(?:[\w-]+\s*=)/.test(cleaned)) continue;
      const [expressionPart, attributePart = ""] = cleaned.split("|");
      const expression = expressionPart.trim();
      if (!expression) continue;
      const attributes = parseAttributes(attributePart);
      plots.push({ expression, color: colorValue(attr(attributes, "color"), PALETTE[plots.length % PALETTE.length]), label: String(attr(attributes, "label") || ""), domain: attr(attributes, "domain"), inverse: booleanAttribute(attributes, "inverse", false), pointEveryX: attr(attributes, "pointEveryX"), pointEveryY: attr(attributes, "pointEveryY"), autoPointLabel: attr(attributes, "autoPointLabel"), showFunction: booleanAttribute(attributes, "showFunction", true), showValues: booleanAttribute(attributes, "showValues", true), tooltip: attr(attributes, "tooltip"), tooltipHtml: "" });
    }
  }
  return plots.length ? plots : [{ expression: "x", color: PALETTE[0], label: "" }];
}

function readFunctionGraphHeader(body) {
  const lines = String(body || "").split(/\r?\n/);
  const first = lines.findIndex((line) => line.trim());
  if (first < 0) return { attributes: {}, body: String(body || "") };
  const line = lines[first].trim();
  if (!/^(?:[\w-]+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s]+)\s*)+$/.test(line)) return { attributes: {}, body: String(body || "") };
  return { attributes: parseAttributes(line), body: lines.slice(0, first).concat(lines.slice(first + 1)).join("\n") };
}

function resolveGraphNumber(value) {
  const text = String(value || "").trim().toLowerCase();
  const signedConstant = text.match(/^([+-])?(pi|tau)$/);
  if (signedConstant) {
    const magnitude = signedConstant[2] === "pi" ? Math.PI : Math.PI * 2;
    return signedConstant[1] === "-" ? -magnitude : magnitude;
  }
  const number = Number(text);
  return Number.isFinite(number) ? number : NaN;
}

function compileExpression(expression) {
  let source = String(expression || "0").replace(/^[xy]\s*=\s*/, "").replaceAll("π", "pi").replaceAll("τ", "tau");
  source = source.replace(/\|([^|]+)\|/g, "abs($1)").replace(/√\s*([A-Za-z0-9_.]+)/g, "sqrt($1)").replace(/∛\s*([A-Za-z0-9_.]+)/g, "cbrt($1)");
  source = source.replace(/(\([^()]+\)|\b[\w.]+)\s*!/g, "fact($1)").replaceAll("^", "**");
  source = source.replace(/\bpi\b/gi, "Math.PI").replace(/\btau\b/gi, "(Math.PI*2)").replace(/\bphi\b/gi, "((1+Math.sqrt(5))/2)").replace(/\be\b/g, "Math.E");
  source = source.replace(/\b(ln|log|log2|log10|sin|cos|tan|asin|acos|atan|atan2|sinh|cosh|tanh|sqrt|cbrt|abs|sign|exp|floor|ceil|round|pow|min|max|hypot)\s*\(/g, (_match, name) => name === "ln" ? "Math.log(" : name === "sign" ? "Math.sign(" : `Math.${name}(`);
  source = source.replace(/\bmod\s*\(/g, "mod(").replace(/√\s*\(/g, "Math.sqrt(").replace(/∛\s*\(/g, "Math.cbrt(");
  source = source.replace(/(\d|\)|Math\.PI|Math\.E)\s*(?=(?:x\b|Math\.PI|Math\.E|\())/g, "$1*");
  source = source.replace(/(x|\)|Math\.PI|Math\.E)\s*(?=\d)/g, "$1*");
  if (!/^[\w\s+\-*/%().,!]+$/.test(source)) return () => NaN;
  try {
    const fn = new Function("x", "fact", "mod", `"use strict"; return (${source});`);
    const factorial = (value) => {
      const numeric = Number(value);
      if (!Number.isFinite(numeric) || numeric < 0 || numeric > 170) return NaN;
      if (Number.isInteger(numeric)) { let result = 1; for (let index = 2; index <= numeric; index += 1) result *= index; return result; }
      return Math.exp((numeric - .5) * Math.log(numeric) - numeric + .5 * Math.log(2 * Math.PI));
    };
    return (x) => Number(fn(x, factorial, (left, right) => left % right));
  } catch (_) {
    return () => NaN;
  }
}
