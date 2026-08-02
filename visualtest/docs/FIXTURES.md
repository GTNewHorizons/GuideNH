# Fixture Specifications — GuideNH Visual Test System

## Purpose

This document specifies every fixture page in the visual test corpus. Each entry states the test goal and the layout invariants that the page must satisfy. The fixture corpus targets every feature supported by the engine, with dedicated pages arranged from simple to complex within semantic folder groups.

Total page count: **66** (across 15 subdirectories plus root index).

---

## Fixture Writing Conventions

These conventions **must** be followed when authoring or modifying fixture pages:

1. **Single focus**: One file tests one feature family. Multiple test cases within the file are variants of that feature.
2. **Self-describing**: Each test case is preceded by a plain-text description line of the form `Here it should: <expected behaviour>…`, so that the reviewer (human or AI) can see the expected outcome directly in the screenshot.
3. **Frontmatter**:
   ```yaml
   ---
   navigation:
     title: <English title>
     position: <within-folder range, descending>
   ---
   ```
4. All pages reside under `_en_us/` (most reliable fallback). CJK test content is written inline in the body.
5. Pages should be short (1–3 screens), except `overflow/` and `stress/` pages which may be longer.
6. Structural text uses English; tested content uses whatever language is appropriate.
7. Each file **must** begin with an uncommented plain-text line stating the test goal and invariant number (this line appears in the screenshot and serves as self-documentation). **Do not use `<!-- -->`** — HTML comments are rendered as visible text by the engine.
8. `<br clear>` accepts only `left`, `right`, `all`, `none` (see `BreakCompiler.java:21–30`). **`both` is invalid** and renders as garbled text. Float clearing must use `clear="all"`. When writing prose that mentions a tag, enclose the tag in backticks (otherwise MDX treats it as a live tag).
9. Attribute names not covered here must be verified against `TagAttributeRegistry.java` — do not guess.
10. Grammar reference: `wiki/resourcepack/assets/guidenh/guidenh/_en_us/*.md` (official documentation pages contain real usage examples).

---

## Root Index

### `index.md` — Corpus Index Page
- Position 9999. Navigation title for the visual test corpus root. Serves as the entry point when browsing the visual test pages in the guide navigation tree.
- **Invariants**: Loads without error; renders as a valid page in the `guidenh:guidenh` guide.

---

## `text/` — Inline and Text Styling

### `text/headings.md` — Heading Levels and Thematic Breaks (backlog K2)
- H1–H6 all levels; heading immediately followed by body text; heading immediately followed by heading; long heading wrapping.
- **Invariants**: Thematic breaks do not cut through text; consecutive heading spacing is consistent; long headings do not overflow the right margin.

### `text/paragraphs.md` — Paragraphs and Line Breaks
- Multiple paragraphs, soft breaks vs blank lines, paragraph exactly filling a line width, single-word paragraph, `<br>` and `<br clear>` variants.
- **Invariants**: Line spacing and paragraph spacing are consistent; no anomalous blank bands.

### `text/inline-marks.md` — All Inline Marks
- `**bold**` `*italic*` `***bold-italic***` `~~strikethrough~~` `~strikethrough~` `++underline++` `^^wavy^^` `::emphasis dots::` `==highlight==` `` `code` `` `<kbd>` `<sub>` `<sup>` `<span>` and arbitrary nesting.
- **Invariants**: Styles do not leak into subsequent text; nested rendering is correct; decorative lines do not compress adjacent lines.

### `text/section-codes.md` — Section Sign (§) Colour Codes (backlog K1)
- All § colour codes row by row; §l/§o/§r combinations; 50 consecutive § codes; bare `§` literal; `<Color id/color>` for comparison.
- **Invariants**: No giant glyphs; colour switching is correct; glyph size is consistent across the page.

### `text/links.md` — Complete Link Family
- In-page anchor `<a name>` + jump, cross-page `[text](page.md)`, external links, autolinks (bare URL), reference-style `[ref][]`, `&[sound](sound:...)`, `<CommandLink>`, `<SoundLink>`, links with title (tooltip).
- **Invariants**: Colouring and underline are consistent; clickable bounding box equals text bounding box (assertable from bounds JSON).

### `text/cjk-mixed.md` — Chinese–English Mixed Typesetting
- Mixed CJK and Latin text, fullwidth punctuation, CJK long strings without spaces, 40+ character English long words, CJK mixed with inline code/links.
- **Invariants**: No overflow at any break point; mixed-script baseline is consistent.

### `text/inline-game-tags.md` — In-Game Inline Tags
- `<ItemImage>` (id/scale/label left-right/format), `<ItemLink>` (showIcon/showText/linksTo), `<KeyBind>`, `<PlayerName>`, `<Tooltip label>` (inline trigger), `<Spoiler>`.
- **Invariants**: Icon and text share the same baseline; line height is not inflated by icons; tooltip trigger region is correct.

### `text/footnotes.md` — Footnotes
- `[^a]` references (multiple) + `<FootnoteList>`.
- **Invariants**: References render as superscript links; the list collects all footnotes fully.

---

## `lists/`

### `lists/basic.md` — List Basics
- `*` `-` `+` unordered, 3 levels; ordered, 3 levels (including `start`); mixed nesting.
- **Invariants**: Indentation and marker spacing are consistent per level.

### `lists/rich.md` — Rich List Items
- Items containing multiple paragraphs, code blocks, links, display formulas, small tables, images.
- **Invariants**: Continuation lines left-align; embedded blocks do not break numbering.

### `lists/tasks.md` — Task Lists
- `- [x]` `- [ ]` mixed, nesting, rich text labels.
- **Invariants**: Checkbox and text are aligned; state styles are distinguishable.

---

## `tables/`

### `tables/basic.md`
- 2-column narrow table; 3-column aligned table (left/center/right).

### `tables/wide.md`
- 5-column wide table; extra-long English word cells; multi-line cells.

### `tables/cjk.md`
- CJK headers/cells/mixed content; CJK long strings.

### `tables/metadata.md`
- `{: widths="120,80" }` column widths; wide–narrow combinations.

### `tables/csv.md`
- `<CsvTable src>` and `csv` code block (two forms; header/widths variants; CSV resource in `_en_us/visualtest/assets/`).

**Shared invariants (all table files)**: Total width ≤ page width; wrapping does not overflow column boundaries; row height is consistent; column separators align.

---

## `code/`

### `code/blocks.md` — Code Block Variants
- Multiple languages (xml/java/json/python/none), 80+ character long lines, empty lines, single line, special characters `<>&"§`, `width=` `height=` fixed viewport (scroll container), indented code blocks.
- **Invariants**: Long-line strategy conforms to specification; background box wraps every line; fixed viewport produces scroll bars instead of overflow.

### `code/special-langs.md` — Special Lang Rendering
- `tree` / `filetree` (including `{:icon=}` suffix), `csv` (compare `tables/csv.md`).
- **Invariants**: Rendered as tree/table rather than plain-text code.

---

## `latex/`

### `latex/inline.md` — Inline Formulas (backlog: inline drop)
- `$E=mc^2$`, `$\frac{1}{2}$`, `$\sqrt{x}$` embedded in running text; "contains X and also Y in the same line" minimal repro.
- **Invariants**: Baseline alignment; horizontal position correct in text flow; fraction height does not compress adjacent text.

### `latex/display.md` — Display Formulas
- `$$...$$` standalone lines, `<Latex>` block form, `scale=1.5`, `valign` variants, `color`, tooltip.
- **Invariants**: Horizontally centred; scaling does not overflow; paragraph spacing is consistent.

---

## `images/`

Image resources are in `_en_us/visualtest/assets/` (pure-colour and checkerboard test PNGs).

### `images/basic.md`
- `![alt](src)` small/medium/wide images, centre align, caption (title).

### `images/float.md`
- `wrap="square|tight|through"` × `align="left|right"` images + wrapping text + `<br clear="both">`.

### `images/fullwidth.md`
- fullWidth image/table/code block comparison (backlog K4: `LytFloatAwareBlock` wrapper layer loses fullWidth).

### `images/floating-image.md`
- `<FloatingImage>` family: crop x/y/w/h, scaleX/scaleY, `<ImageAnnotation>` hotspots, `<SoundArea>` (annotation render check only, sound not played).

### `images/block-item.md`
- `<BlockImage>` (scale/wrap/align/float) + block-style `ItemImage` comparison.

**Shared invariants (all image files)**: No distortion; wrapping does not compress images or leave anomalous gaps; content after `clear` starts at the float bottom edge; fullWidth real width equals content width (assertable from bounds JSON).

---

## `floats/` — Float System Matrix

### `floats/wrap-modes.md` — Six Wrap Modes
- `square` / `tight` / `through` / `top-bottom` / `behind` / `front` — one example each (image) + wrapping text.
- **Invariants**: Wrap behaviour per mode conforms to `ContentWrapMode` semantics; `behind`/`front` layer ordering is correct.

### `floats/content-types.md` — Any Block Can Float
- Float: table, code block, GameScene, Recipe, chart, Column container (one each).
- **Invariants**: Non-paragraph blocks (via `LytFloatAwareBlock` path) have correct available-width reduction and do not overlap with floats.

### `floats/multi.md` — Multiple Floats and Clearing
- Three consecutive left floats, left–right pair, float immediately followed by float, `<br clear="left|right|both">` variants.
- **Invariants**: Floats do not overlap; after `clear`, text starts below the float bottom edge.

### `floats/in-tabs.md` — ContentTabs × Floats
- `<ContentTabs>` with tabs containing floating images/scenes; floats before and after the tabs block.
- **Invariants**: Floats inside tabs do not leak outside; the tabs block itself (fullWidth + `LytFloatAwareBlock` path) is not anomalously compressed by external floats.

---

## `layout/`

### `layout/row-column.md`
- `<Row>` / `<Column>`: gap variants, `alignItems` four values, fullWidth, nested Row in Column.

### `layout/align.md`
- Block-level align: left/center/right — images, tables, scenes comparison.

### `layout/details.md`
- `<details>` open/closed, embedded tables/code/images, multiple consecutive details.

### `layout/content-tabs.md`
- `<ContentTabs>`: 3–4 tabs (different content types), `title`/`default`/`defaultIndex`/`color`/`icon` variants; appended "Text and PNG Icons" section covers `icon=` (TEXT) and `iconPng=` heading icons.

### `layout/callouts.md` — Callouts and Blockquotes
- Plain blockquote (multi-paragraph); all five GitHub alerts (NOTE/TIP/IMPORTANT/WARNING/CAUTION); custom `{: ... }` quotes with TEXT / ITEM / PNG icons; bare-keyword trap (`> NOTE:` hits the Note alert); alert-marker-alone boundary (literal `[!NOTE]` retained in body); blockquote mixing a nested list and paragraphs.
- **Invariants**: Alerts render a 3px type-coloured left border (NOTE blue / TIP green / IMPORTANT purple / WARNING gold / CAUTION red) plus a title row with the type-coloured symbol icon and i18n label; custom quotes apply `color=` to the 3px left border and render a header row only when `title`/`icon` are present; plain blockquotes keep the grey background with a 2px border and no title row; `> NOTE:` bare keyword still matches the Note alert; `[!NOTE]` alone on its line followed by a blank line leaves the literal marker in the alert body (known boundary, expected behaviour).

**Shared invariants (all layout files)**: Flex spacing and alignment correct; details open/close height correct; tab headers aligned with content; callout/alert boxes keep their accent border and title row; overflow containers produce scroll bars.

---

## `charts/`

### `charts/bar-column.md`
- `<BarChart>` `<ColumnChart>` + `<Series>` (data/points/color/icon) + `<PieInset>`.

### `charts/line-scatter.md`
- `<LineChart>` (numericX) `<ScatterChart>` + `<LineSeries>`.

### `charts/pie.md`
- `<PieChart>` + `<Slice>` (startAngle/clockwise/labelPosition).

### `charts/function.md`
- `<FunctionGraph>`+`<Plot>`+`<Point>`, `<Function>` shorthand, `xRange`/`quadrants`.

### `charts/options.md`
- Legend five positions, `cornerLegend`, axis label/min/max/step/unit/tickFormat, grid toggle and colour.

**Shared invariants (all chart files)**: Content does not overflow the container; axis text does not overlap; legend does not overlap chart area.

---

## `mermaid/`

### `mermaid/mindmap.md` — Mindmap Dual-Mode
- Default mode (root-centred, alternating left–right) + TIDY_TREE mode; node shapes (rounded/circle/hexagon/cloud/explosion); **multi-level nested child nodes** (≥4 levels).
- **Invariants**: Tree connections correct; nodes do not overlap; deep nesting does not interleave levels.

### `mermaid/flowchart.md` — Flowchart
- Node shapes (stadium/rounded/diamond/rect/cylinder/subprocess/double-circle); arrow styles (solid/dashed/dotted × triangle/circle/cross head); edge labels; `classDef`/`linkStyle`.
- **Invariants**: Arrow direction correct; labels do not cross lines; all shapes rendered.

### `mermaid/subgraphs.md` — Nested Subgraphs
- 2-level and 3-level nested subgraphs (≤4-level colour cap); edges crossing subgraph boundaries.
- **Invariants**: Nesting box containment correct; hierarchical colouring; cross-boundary edges do not pierce boxes.

### `mermaid/node-content.md` — Rich Node Content
- `<NodeContent id>` nodes with formatted text/lists/small images inside the node box.
- **Invariants**: Rich content lays out correctly within the node bounding box.

### `mermaid/large.md` — Large Diagram (20+ Nodes)
- Container-relative scaling and initial pan state; behaviour when content exceeds container.
- **Invariants**: Initial viewport is reasonable; page height is not inflated infinitely.

---

## `nei/`

### `nei/recipes.md`
- `<Recipe id>`, `<RecipeFor input/output filter>`, `<RecipesFor limit>`, `handlerName`/`handlerOrder`, `fallbackText` (when no recipe matches).
- **Invariants**: Recipe box renders completely; clipping is correct (glScissor regression sentinel); fallback text displays properly.

### `nei/item-grid.md`
- `<ItemGrid>` with multiple item grid (Row/Column children).

---

## `scenes/` — GameScene Sub-Tags

### `scenes/blocks.md`
- `<Block>` (id/meta/facing/nbt/formed), `<PlaceBlock>`, `<ReplaceBlock>`, `<RemoveBlocks>` (regression sentinel: scale fix).

### `scenes/entities.md`
- `<Entity>` (NBT variants), `<RemoveEntity>` (backlog: entity Y-offset reproduction).

### `scenes/annotations.md`
- Five annotation types (Block/Box/Line/Diamond/Text) + `<LinePoint>` + `<BlockAnnotationTemplate>`.

### `scenes/effects.md`
- `<Particle>`, `<Weather>` rain/snow, `<PlaySound>` (render presence only).

### `scenes/camera.md`
- `perspective` three values, `rotateX/Y/Z`, `offset`/`center`, `<IsometricCamera>`, `zoom`.

### `scenes/import.md`
- `<ImportStructure>` (SNBT resource in `assets/`), `<ImportStructureLib>` + `<Tier>`/`<Channel>`/`<Facing>`/`<Rotation>`/`<Flip>`/`<Orientation>`/GT markers.

### `scenes/ponder.md`
- `<ImportPonder>` keyframe timeline.

**Shared invariants (all scene files)**: Scene content is within the background box; dimensions match; entities are on the ground; annotations are correctly positioned; effects do not escape the scene area.

---

## `meta/`

### `meta/frontmatter.md`
- `navigation.icon` / `icon_texture` / `icons` variants, `categories`, `item_ids`, `author`/`date` (sidebar not visible — pass criterion is no-load-error + correct body rendering).

### `meta/zoom-small.md`
- Frontmatter `zoom: 0.8`.

### `meta/error-parse.md`

- **Purpose**: Whole-page and attribute-level error rendering (R2-5 ratchet).
- **Contents**: An intentionally malformed `<ItemImage id={} />` tag that raises an
  attribute error during compilation, plus two color references
  (`<Color id="red">` tag and `§4` section code) on the same page.
- **Invariants**: The attribute error text renders in red `ERROR_TEXT`
  (rgb(255,0,0)), not gray body text; the Color-tag and section-code references
  render their respective reds — proving span colors work generally and isolating
  the single-style serialization path (PF13). If a future change makes the error
  text gray again, this page catches it by eye or pixel scan.

### `meta/zoom-large.md`
- Frontmatter `zoom: 1.5`.

### `meta/indexes.md`
- `<SubPages>`, `<Category name>` (note: `<CategoryIndex>` has no compiler; the real tag is `<Category name>`, per `CategoryCompiler.java:22–23`).

### `meta/indexes-sub-a.md`
- Child page used by `indexes.md` for SubPages testing. Supports the SubPages feature verification.

### `meta/indexes-sub-b.md`
- Child page used by `indexes.md` for SubPages testing. Supports the SubPages feature verification.

### `meta/special.md`
- `<Special>` tag family (MediaWikiSpecialGeneratedBlock F7a): `<Special name="SpecialPages" />` grouped special-page catalog (static, always non-empty); `<Special name="Categories" rows="2" />` category grid (CategoryIndex-derived); `<Special name="AllPages" rows="3" />` page grid (index-backed; empty-state fallback allowed).
- **Invariants**: SpecialPages renders the grouped 2-column catalog with group headers, clickable link rows and top/bottom 1px borders; all Special blocks render through the primitives/GuideText path (no 8×8 MC pixel font); no compile errors.

**Invariants (all meta files)**: Loads without error; zoom pages render at correct scale; index lists are complete.

---

## `overflow/` — Overflow Behaviour

The engine has no paginator. Former `pagination/` renamed to `overflow/`.

### `overflow/tall-element.md`
- GameScene taller than 1 screen, extra-long table, oversized mermaid.
- **Invariants**: No crash or infinite loop; full height rendered correctly (long-screenshot mechanism produces full-height output).

### `overflow/exact-fit.md`
- Content fitting exactly an integer number of screens; single element at page end.
- **Invariants**: No anomalous trailing blank space; full-screen elements do not overflow.

### `overflow/scroll-containers.md`
- Fixed viewports (code block width/height, `LytSizeBox` path) with embedded extra-long content.
- **Invariants**: Scrolling occurs inside the container rather than page-level overflow.

---

## `stress/`

### `stress/mixed.md` — Full-Syntax Mixed Stress Page
- Headings + tables + code + formulas + images + floats + tabs + charts + mermaid + scenes on a single page.
- **Invariants**: No perceptible anomalies overall (human/VLM review).

---

## Appendix: Backlog-to-Fixture Mapping

| Backlog Item | Reproduction File |
|---|---|
| K1 Giant `§` glyph | `text/section-codes.md` |
| K2 Thematic break through text | `text/headings.md` |
| K4 fullWidth not filling width | `images/fullwidth.md` |
| Entity Y-offset | `scenes/entities.md` |
| Inline LaTeX line drop | `latex/inline.md` |
| ImportStructure materialisation failure at `scene/import.md` recipes.md::gamescene:40 | `scenes/import.md` |
| Missing `example_structure.snbt` | `scenes/import.md` (supplementary test resource) |
| ~~Mermaid placeholder box~~ (**RESOLVED** R4-R7: mermaid now renders fully in-game and offline; was ELK/async render chain migration loss, fixed across mermaid fix waves) | `mermaid/*.md` |
| JSX `<table align>` empty-column layout crash (**real engine crash**: `NoSuchElementException` at `LytTable.layoutColumns` `LytTable.java:176`; tr/td JSX parses to 0 columns; suspected semantic conflict between `BlockTagCompiler.align` proxy and `TableCompiler.align`) | `layout/align.md` JSX table cases disabled in text, to be restored after fix |
| Batch OOM (**infrastructure issue**: ~page 42 of 63 triggers Java heap space, NEI-worker thread first; 41/63 succeeded then consecutive failures; suspected cross-page resource leaking. Mitigation: batches ≤40 pages) | Full corpus split into two batches |
