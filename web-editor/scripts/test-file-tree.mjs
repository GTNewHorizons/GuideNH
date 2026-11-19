import fs from "node:fs/promises";
import { test } from "node:test";
import assert from "node:assert/strict";

const nativeFetch = globalThis.fetch;
globalThis.fetch = async (url, ...args) => String(url).startsWith("file:")
  ? new Response(await fs.readFile(url)) : nativeFetch(url, ...args);
const { renderMarkdown } = await import("../public/modules/markdown-renderer.js");

test("file trees use stretchable slots for Unicode and ASCII connectors", () => {
  const html = renderMarkdown('```tree\nroot/\n│   ├── [recipes](recipes/machine.md)\n|   \\-- file.json\n```');
  assert.match(html, /--tree-max-depth:2/);
  assert.match(html, /guide-filetree-slot-vertical/);
  assert.match(html, /guide-filetree-slot-branch/);
  assert.match(html, /guide-filetree-slot-last-branch/);
  assert.match(html, /href="recipes\/machine.md"/);
});

test("root names starting with punctuation are preserved", () => {
  const html = renderMarkdown('```tree\n.config/\n---config.json\n```');
  assert.match(html, /---config.json/);
});
