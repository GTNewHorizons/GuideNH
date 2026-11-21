import assert from "node:assert/strict";
import test from "node:test";
import fs from "node:fs/promises";
import vm from "node:vm";

const nativeFetch = globalThis.fetch;
globalThis.fetch = async (url, ...args) => String(url).startsWith("file:")
  ? new Response(await fs.readFile(url)) : nativeFetch(url, ...args);
const { renderMarkdown } = await import("../public/modules/markdown-renderer.js");

test("fluid tooltips retain runtime lines as literal text", () => {
  const html = renderMarkdown('<FluidImage id="water" amount={1000} />', {
    resolveFluidPreview: () => ({ displayName: "Water", tooltipLines: ["Water", "§9Amount: 1,000 mB", "§cTemperature: 300 K", "§aState: Liquid", '<img src=x onerror="bad()">', "**literal**"] }),
  });
  assert.match(html, /Temperature: 300 K/);
  assert.match(html, /State: Liquid/);
  assert.match(html, /Amount: 1,000 mB/);
  assert.match(html, /&lt;img src=x onerror=&quot;bad\(\)&quot;&gt;/);
  assert.match(html, /\*\*literal\*\*/);
  assert.doesNotMatch(html, /<img src=x|§/);
});

test("hidden fluid tooltips and disconnected previews remain usable", () => {
  const hidden = renderMarkdown('<FluidLink id="water" showTooltip={false} />', {
    resolveFluidPreview: () => ({ displayName: "Water", tooltipLines: ["Temperature: 300 K"] }),
  });
  assert.doesNotMatch(hidden, /data-guide-tooltip-content|Temperature:/);
  const fallback = renderMarkdown('<FluidImage id="water" />');
  assert.match(fallback, /water/);
  assert.doesNotMatch(fallback, /Temperature:|State:|1000 L/);
});

test("exported scene input annotations retain fluid units and literal tooltip lines", async () => {
  const source = await fs.readFile(new URL("../../src/main/resources/assets/guidenh/siteexport/model-viewer/modelViewer.js", import.meta.url), "utf8");
  const start = source.indexOf("function createInputAnnotationNode(");
  const end = source.indexOf("\nfunction createOverlayAnnotationNode(", start);
  const createNode = vm.runInNewContext(source.slice(start, end) + ";createInputAnnotationNode", { siteText: () => "Click" });
  const document = {
    createElement: () => ({ children: [], append(child) { this.children.push(child); }, setAttribute() {} }),
  };
  const node = createNode(document, { fluid: { id: "water", amount: 0, amountText: "0 mB", tooltipLines: ["Water", "§cTemperature: 300 K", "<script>literal</script>"] } });
  const body = node.children[0];
  assert.equal(body.title, "Water\nTemperature: 300 K\n<script>literal</script>");
  assert.equal(body.children.find(child => child.className === "scene-input-annotation-amount").textContent, "0 mB");
  const legacy = createNode(document, { fluid: { id: "water", amount: 144 } });
  assert.equal(legacy.children[0].children.find(child => child.className === "scene-input-annotation-amount").textContent, "144 L");
});
