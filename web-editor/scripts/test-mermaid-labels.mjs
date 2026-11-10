import assert from "node:assert/strict";
import test from "node:test";
import { renderMermaidDiagram } from "../public/modules/visual-renderers.js";

const diagrams = [
  { name: "flowchart", source: (label) => `flowchart TD\nA[${label}]`, id: "A", lineHeight: 15 },
  { name: "extended flowchart", source: (label) => `flowchart TD\nA@{ shape: rect, label: ${label} }`, id: "A", lineHeight: 15 },
  { name: "mindmap", source: (label) => `mindmap\n  root((${label}))`, id: "root", lineHeight: 16.2 },
];

function labels(source) {
  const html = renderMermaidDiagram(source);
  return [...html.matchAll(/class="(?:guide-flowchart-node-title|guide-mermaid-node-label)">([\s\S]*?)<\/div>/g)]
    .map((match) => match[1].startsWith("<span>") && match[1].endsWith("</span>") ? match[1].slice(6, -7) : match[1]);
}

function frameHeight(source, options) {
  const match = renderMermaidDiagram(source, options).match(/<foreignObject\b[^>]*\bheight="([\d.]+)"/);
  assert.ok(match, "node should have a label frame");
  return Number(match[1]);
}

for (const diagram of diagrams) {
  test(`${diagram.name}: source entities display once and never become markup`, () => {
    assert.deepEqual(labels(diagram.source('"&lt;b&gt;literal&lt;/b&gt; &amp;lt;i&amp;gt; &#42;&#42;plain&#42;&#42;"')),
      ["&lt;b&gt;literal&lt;/b&gt; &amp;lt;i&amp;gt; &#42;&#42;plain&#42;&#42;"]);
  });

  test(`${diagram.name}: quotes inside Markdown delimiters remain literal`, () => {
    assert.deepEqual(labels(diagram.source('"`"quoted"`"')), ["&quot;quoted&quot;"]);
  });

  test(`${diagram.name}: nested label delimiters are removed only once`, () => {
    assert.deepEqual(labels(diagram.source('"`\'quoted\'`"')), ["&#39;quoted&#39;"]);
  });

  test(`${diagram.name}: all raw break forms preserve consecutive breaks`, () => {
    assert.deepEqual(labels(diagram.source('"alpha<br/>beta<br><br />omega"')), ["alpha<br>beta<br><br>omega"]);
  });

  test(`${diagram.name}: escaped breaks stay literal beside real breaks`, () => {
    assert.deepEqual(labels(diagram.source('"&lt;br/&gt;<br />end"')), ["&lt;br/&gt;<br>end"]);
  });

  test(`${diagram.name}: existing Markdown formatting and links remain supported`, () => {
    assert.deepEqual(labels(diagram.source('"**strong** *em* `code` [page](guide:page)"')),
      ['<strong>strong</strong> <em>em</em> <code>code</code> <a href="guide:page">page</a>']);
  });

  test(`${diagram.name}: unsupported HTML remains escaped`, () => {
    assert.deepEqual(labels(diagram.source('"<img src=x onerror=alert(1)>"')), ["&lt;img src=x onerror=alert(1)&gt;"]);
  });

  test(`${diagram.name}: breaks and entities inside code remain literal`, () => {
    assert.deepEqual(labels(diagram.source('"text `first<br/>&lt;second`<br>end"')),
      ["text <code>first&lt;br/&gt;&amp;lt;second</code><br>end"]);
  });

  test(`${diagram.name}: five explicit lines fit inside the label frame`, () => {
    const available = frameHeight(diagram.source('"first<br/><br>third<br />fourth<br>fifth"'));
    // Mindmap CSS adds an 8px top margin, 1px border, and 6px label padding.
    const needed = 5 * diagram.lineHeight + (diagram.name === "mindmap" ? 15 : 1);
    assert.ok(available >= needed, `five lines with existing CSS spacing need ${needed}px; frame has ${available}px`);
  });

  test(`${diagram.name}: multiline labels preserve the existing rich body allowance`, () => {
    const options = { nodeContents: [{ id: diagram.id, html: "<p>Rich content</p>" }] };
    const baseline = frameHeight(diagram.source('"first"'), options) - diagram.lineHeight;
    const multiline = frameHeight(diagram.source('"first<br/><br>third<br />fourth<br>fifth"'), options) - 5 * diagram.lineHeight;
    assert.ok(multiline >= baseline, `rich body allowance shrank from ${baseline}px to ${multiline}px`);
  });

  test(`${diagram.name}: escaped and code-span breaks do not increase label height`, () => {
    const baseline = frameHeight(diagram.source('"first"'));
    const escaped = frameHeight(diagram.source('"first&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;last"'));
    const code = frameHeight(diagram.source('"text `first<br><br><br><br>last`"'));
    assert.equal(escaped, baseline);
    assert.equal(code, baseline);
  });
}

test("flowchart: semicolons delimit statements outside labels and preserve entities inside labels", () => {
  assert.deepEqual(labels('flowchart TD; A["A &amp; B; C"] --> B["next"]; C["last"]'),
    ["A &amp; B; C", "next", "last"]);
});

test("flowchart: unquoted entity labels do not split into extra nodes", () => {
  assert.deepEqual(labels("flowchart TD\nA[&lt;b&gt;literal&lt;/b&gt;]\nB[end]"),
    ["&lt;b&gt;literal&lt;/b&gt;", "end"]);
});

test("flowchart: plain apostrophes do not swallow the following statement", () => {
  assert.deepEqual(labels("flowchart TD\nA[don't split]\nB[end]"), ["don&#39;t split", "end"]);
});

test("flowchart: quoted closing brackets do not expose label semicolons", () => {
  assert.deepEqual(labels('flowchart TD\nA["literal ]; &amp;"]; B[end]'), ["literal ]; &amp;", "end"]);
});

test("flowchart: inner quotes do not end a Mermaid Markdown string", () => {
  assert.deepEqual(labels('flowchart TD\nA["`"literal ];" &amp;`" ]; B[end]'), ["&quot;literal ];&quot; &amp;", "end"]);
});

test("flowchart: comments containing brackets do not merge later statements", () => {
  assert.deepEqual(labels("flowchart TD\n%% unmatched ([ comment\nA[first] %% another { comment\nB[end]"), ["first", "end"]);
});

test("flowchart: brackets in edge labels do not swallow following nodes", () => {
  const html = renderMermaidDiagram('flowchart TD\nA["alpha"] -->| ( | B["beta"]\nC["next"]');
  assert.deepEqual([...html.matchAll(/data-node-id="([^"]+)"/g)].map((match) => match[1]), ["A", "B", "C"]);
});

test("flowchart: circular nodes grow enough to fit ten explicit lines", () => {
  const label = Array.from({ length: 10 }, () => "line").join("<br>");
  const html = renderMermaidDiagram(`flowchart TD\nA(("${label}"))`);
  const circle = html.match(/<circle\b[^>]*\br="([\d.]+)"/);
  assert.ok(circle, "node should remain a circle");
  assert.ok(frameHeight(`flowchart TD\nA(("${label}"))`) >= 150, "ten 15px lines should fit");
  assert.ok(Number(circle[1]) * 2 >= 150, "circle should enclose the multiline label");
});

test("flowchart: break markup remains inside a text span within the flex title", () => {
  assert.match(renderMermaidDiagram('flowchart TD\nA["first<br>second"]'),
    /class="guide-flowchart-node-title"><span>first<br>second<\/span><\/div>/);
});
