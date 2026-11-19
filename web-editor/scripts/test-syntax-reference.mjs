import { test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const originalFetch = globalThis.fetch;
globalThis.fetch = async (input, options) => {
  const url = new URL(input);
  return url.protocol === "file:"
    ? new Response(await readFile(url, "utf8"))
    : originalFetch(input, options);
};
let reference;
let renderMarkdown;
try {
  reference = await import("../public/modules/syntax-reference.js");
  ({ renderMarkdown } = await import("../public/modules/markdown-renderer.js"));
} finally {
  globalThis.fetch = originalFetch;
}

test("reference sections preserve fenced examples and index nested headings", () => {
  const source = "# Reference\nIntro\n{/*\n## Hidden heading\n*/}\n## Examples\n````md\n# Sample page\n```md\n## Sample heading\n```\n````\n~~~md\n# Another example\n~~~\n\t```md\n# Tabbed example\n{/* literal comment */}\n         ```\n#### Details\nText\n## Next";
  const sections = reference.splitSections(source, "Examples");
  assert.deepEqual(sections.map(section => section.title), ["Reference", "Examples", "Next"]);
  assert.match(sections[1].markdown, /# Sample page/);
  assert.match(sections[1].markdown, /literal comment/);
  assert.match(sections[1].markdown, /#### Details/);
  assert.ok(sections[1].anchors.includes("details"));
  assert.equal(sections[1].documentId, "Examples");
});

test("reference links select anchors, recognize wiki language suffixes and fall back safely", () => {
  const sections = reference.splitSections("# Tags\n## `<QuestLink>`\n#### 参数", "Tags-Reference");
  const link = reference.resolveReferenceLink("Tags-Reference-zh-CN.md#questlink", sections, "Examples", "zh_cn");
  assert.equal(link.section, sections[1]);
  assert.equal(link.anchor, "questlink");
  assert.equal(reference.resolveReferenceLink("#%E5%8F%82%E6%95%B0", sections, "Tags-Reference", "zh_cn").section, sections[1]);
  assert.equal(reference.resolveReferenceLink("Templates#parameters", sections, "Tags-Reference", "zh_cn").url, "https://github.com/GTNewHorizons/GuideNH/wiki/Templates-zh-CN#parameters");
  assert.equal(reference.resolveReferenceLink("javascript:alert(1)", sections, "Tags-Reference", "en_us"), null);
  assert.equal(reference.resolveReferenceLink("#%broken", sections, "Tags-Reference", "en_us"), null);
});

test("reference tag headings have usable anchors without rendering code examples as headings", () => {
  const html = renderMarkdown("## `<QuestLink>`\n````md\n# Example\n```\n## Code\n```\n````");
  assert.match(html, /<h2 id="questlink">/);
  assert.equal((html.match(/<h[1-6]\b/g) || []).length, 1);
});

test("Markdown reference previews retain comments and definitions in literal examples", () => {
  const source = "# Guide\n{/* hidden\n## Hidden heading\n*/}\n`{/* inline example */}`\n````md\n{/* fenced example */}\n[reference]: page.md\n```\n# Literal heading\n```\n````\n<pre>{/* pre example */}</pre>";
  const html = renderMarkdown(source);
  assert.doesNotMatch(html, /Hidden heading/);
  assert.match(html, /inline example/);
  assert.match(html, /fenced example/);
  assert.match(html, /\[reference\]: page\.md/);
  assert.match(html, /pre example/);
});
