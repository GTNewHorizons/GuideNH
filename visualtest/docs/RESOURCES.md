# Resources — GuideNH Visual Test System

## Purpose

This document indexes the fixture folder structure with position ranges, the tool scripts under `tools/visual-inspection/`, key source-code packages relevant to the visual test corpus, and the test corpus resource assets.

---

## 1. Fixture Folder / Position Allocation

Each page in the visual test corpus is assigned a `position` value in its frontmatter. Positions within a folder descend from the high end of the range in decrements of 10. Additions to a folder must respect this allocation.

| Folder | Position Range | Topic |
|---|---|---|
| (root) `index.md` | 9999 | Corpus index |
| `text/` | 9000–8930 | Headings, paragraphs, inline styles, section codes, links, CJK, inline game tags, footnotes |
| `lists/` | 8900–8880 | Lists (including task lists) |
| `tables/` | 8800–8760 | Tables (widths metadata, CsvTable) |
| `code/` | 8700–8690 | Code blocks (language, width/height attributes, special-lang rendering) |
| `latex/` | 8600–8590 | LaTeX inline and display |
| `images/` | 8500–8460 | Images, BlockImage, FloatingImage, fullWidth |
| `floats/` | 8400–8370 | Float system matrix (wrap × content × align × clear) |
| `layout/` | 8300–8270 | Row/Column, details, align, ContentTabs |
| `charts/` | 8260–8220 | Bar/Column/Line/Scatter/Pie charts, FunctionGraph, chart options |
| `mermaid/` | 8190–8150 | Mindmap, flowchart, nested subgraphs, NodeContent, large diagrams |
| `nei/` | 8000–7990 | Recipe tags, ItemGrid |
| `scenes/` | 7900–7840 | GameScene sub-tags, structure import |
| `meta/` | 7800–7770 | Frontmatter variants, zoom, SubPages, Category |
| `overflow/` | 7700–7680 | Overflow behaviour (tall elements, exact fit, scroll containers) |
| `stress/` | 7600 | Mixed stress page |

---

## 2. Tool Scripts — `tools/visual-inspection/`

All tools are written for Python 3. Run with `py -3` on Windows.

| Script | Purpose | Entry Point |
|---|---|---|
| `render_watchdog.py` | Wraps headless render commands; prevents orphaned JVMs via process-tree tracking and `taskkill /T /F`. Exits 124 on timeout. | `py -3 tools/visual-inspection/render_watchdog.py [options] -- <command>` |
| `assert_bounds.py` | Bounds-JSON assertion ratchet: evaluates `visualtest/ratchet/assertions.json` against the latest render bounds; TAP output, exit 0/1, optional JUnit XML. CI-ready, not yet wired. | `py -3 tools/visual-inspection/assert_bounds.py --shots <dir> --assertions <file> [--junit <out.xml>]` |
| `screen.py` | Three-tier visual screening pipeline: geometric (Layer 0), VLM (Layer 1), merged report (Layer 2). | `py -3 tools/visual-inspection/screen.py <subcommand> [options]` |
| `.env.example` | Template for VLM configuration. Copy to `.env` and fill in `DASHSCOPE_API_KEY`. | — |
| `README.md` | Inline documentation for `screen.py`: sub-command reference, output JSON schemas, bounds JSON schema, parent–child derivation rules. | — |

### 2.1 `screen.py` Sub-commands

| Sub-command | Layer | Description |
|---|---|---|
| `geometric` | 0 | Mechanical detection: overflow width, zero-size blocks, off-page coordinates, sibling intersection. Requires `-Dguidenh.renderpage.bounds=true` render output. |
| `vlm` | 1 | Vision-Language Model screening: tiles screenshots, calls OpenAI-compatible API (`qwen3-vl-plus`), deduplicates findings across tiles. |
| `report` | 2 | Merges geometric and VLM findings, groups by page, sorts by severity (error → warn → info). |

### 2.2 Configuration (`.env`)

| Key | Default | Description |
|---|---|---|
| `DASHSCOPE_API_KEY` | — | Alibaba Cloud DashScope API key (required). |
| `VLM_BASE_URL` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | OpenAI-compatible endpoint. |
| `VLM_MODEL` | `qwen3-vl-plus` | Visual model. Selected after A/B comparison; `qwen3-vl-plus` had highest recall. |
| `VLM_CONCURRENCY` | `4` | Thread pool size for parallel tile processing. |
| `VLM_TIMEOUT` | `120` | Per-request timeout in seconds. |

---

## 3. Key Source-Code Map

Source root: `src/main/java/com/hfstudio/guidenh/guide/`

### 3.1 Compiler Layer (`compiler/`)

| Path | Key Files | Relevance |
|---|---|---|
| `compiler/` | `PageCompiler.java`, `TagCompiler.java`, `Frontmatter.java`, `MdxBlockTagSourceExtractor.java` | Page compilation pipeline; frontmatter parsing; MDX tag extraction |
| `compiler/tags/` | `TableCompiler.java`, `ImageCompiler.java`, `FloatingImageCompiler.java`, `MermaidCompiler.java`, `BreakCompiler.java`, `LatexTagCompiler.java`, `RecipeCompiler.java`, `CodeCompiler.java`, `CsvTableCompiler.java`, `ContentTabsTagCompiler.java`, `DetailsTagCompiler.java`, `BoxTagCompiler.java`, `ListCompiler.java`, `HeadingCompiler.java`, `ParagraphCompiler.java`, `BlockImageCompiler.java`, `ItemImageCompiler.java`, `ItemLinkCompiler.java`, `KeyBindTagCompiler.java`, `PlayerNameTagCompiler.java`, `CommandLinkCompiler.java`, `SoundLinkCompiler.java`, `SubPagesCompiler.java`, `StructureViewCompiler.java`, `FootnoteListCompiler.java`, `ColorTagCompiler.java`, `DelUWaveMarkCompiler.java`, `EmphasisCompiler.java`, `StrongCompiler.java`, `MarkTagCompiler.java`, `KbdTagCompiler.java`, `SubscriptTagCompiler.java`, `SuperscriptTagCompiler.java`, `TooltipTagCompiler.java`, `BlockTagCompiler.java`, `FlowTagCompiler.java`, `DivTagCompiler.java`, `HrCompiler.java`, `FileTreeTagCompiler.java`, `DetailsContentExtractor.java`, `ContentTabsSpec.java`, `CalloutIconSupport.java`, `MdxAttrs.java`, `PreCompiler.java`, `SerializedEnum.java` | Individual tag compilers for every visual test feature |
| `compiler/tags/chart/` | `BarChartCompiler.java`, `ColumnChartCompiler.java`, `LineChartCompiler.java`, `PieChartCompiler.java`, `ScatterChartCompiler.java`, `ChartAttrParser.java`, `ChartChildParser.java`, `CommonChartAttrs.java` | Chart tag compilers (bar, column, line, scatter, pie) |
| `compiler/tags/mediawiki/` | (MediaWiki-related compilers) | MediaWiki syntax support |
| `compiler/tags/functiongraph/` | (Function graph compiler) | FunctionGraph and Plot tags |
| `compiler/tags/mermaid/` | `MermaidCompiler.java` | Mermaid diagram tag compiler |

### 3.2 Document Block Layer (`document/block/`)

| Path | Key Files | Relevance |
|---|---|---|
| `document/block/` | `LytBlock.java`, `LytBox.java`, `LytDocument.java`, `LytParagraph.java`, `LytHeading.java`, `LytList.java`, `LytListItem.java`, `LytTaskListItem.java`, `LytTable.java`, `LytCodeBlock.java`, `LytImage.java`, `LytImageBlock.java`, `LytFloatAwareBlock.java`, `LytDocumentFloat.java`, `LytContentTabsBlock.java`, `LytContentTabsHeader.java`, `LytDetailsBlock.java`, `LytHBox.java`, `LytVBox.java`, `LytSizeBox.java`, `LytViewportBox.java`, `LytWidthBox.java`, `LytAxisBox.java`, `LytAlignedBlock.java`, `LytNode.java`, `LytThematicBreak.java`, `LytItemImage.java`, `LytItemGrid.java`, `LytSlot.java`, `LytSlotGrid.java`, `LytStructureView.java`, `LytButton.java`, `LytAlertBox.java`, `LytQuoteBox.java`, `LytGuiSprite.java`, `LytMermaidCanvas.java`, `LytMermaidMindmap.java`, `LytMermaidMindmapCanvas.java`, `LytMermaidFlowchart.java`, `LytMermaidFlowchartCanvas.java`, `MermaidNodeRenderer.java`, `BorderRenderer.java`, `ContentWrapMode.java`, `ContentAlign.java`, `AlignItems.java`, `LatexVerticalAlign.java`, `LatexRenderOptions.java`, `CodeHighlightFlowBuilder.java`, `ResponsiveVisualSizing.java`, `LytVisitor.java`, `ImageRegionAnnotation.java` | Block types that appear in bounds JSON as `cls` values; layout invariants are asserted against these |
| `document/block/chart/` | `LytBarChart.java`, `LytColumnChart.java`, `LytLineChart.java`, `LytPieChart.java`, `LytScatterChart.java`, `LytChartBase.java`, `CartesianChartRenderer.java`, `ChartAxisOptions.java`, `ChartLegendRenderer.java`, `CornerLegendRenderer.java`, `PieInsetRenderer.java`, `PieSlice.java`, `ChartSeries.java`, `AxisRange.java`, `ChartIcon.java`, `ChartLabelPosition.java`, `ChartLegendPosition.java`, `ChartLegendRenderer.java`, `CornerLegendEntry.java`, `CornerLegendPosition.java`, `PieInsetSpec.java` | Chart rendering and layout blocks |
| `document/block/recipes/` | `LytStandardRecipeBox.java`, `LytGenericRecipeBox.java`, `LytRecipeGalleryRow.java` | Recipe display blocks |
| `document/block/table/` | (Table cell rendering classes) | Table cell layout |
| `document/block/shapes/` | (Shape rendering for annotations) | Shape primitives |

### 3.3 Document Flow Layer (`document/flow/`)

| Path | Key Files | Relevance |
|---|---|---|
| `document/flow/` | `LytFlowText.java`, `LytFlowSpan.java`, `LytFlowLink.java`, `LytFlowAnchor.java`, `LytFlowBreak.java`, `LytFlowContainer.java`, `LytFlowContent.java`, `LytFlowInlineBlock.java`, `LytFlowParent.java`, `LytTooltipSpan.java`, `LytSpoilerSpan.java`, `InlineBlockAlignment.java` | Inline flow content: text runs, links, breaks, inline blocks, tooltips, spoilers |

### 3.4 Layout Engine (`layout/`)

| Path | Key Files | Relevance |
|---|---|---|
| `layout/` | `LayoutBridge.java`, `LayoutContext.java`, `LayoutStyleExtractor.java`, `Layouts.java`, `LayoutTreeSerializer.java`, `LayoutNodeSerializer.java`, `FontMetrics.java`, `FontProvider.java`, `MinecraftFontMetrics.java`, `RustFontMetrics.java`, `SystemFontProvider.java` | Taffy-based layout bridge; font metrics integration with Rust native engine |

### 3.5 LaTeX (`latex/`)

| Path | Key Files | Relevance |
|---|---|---|
| `latex/` | `GuideLatexRenderer.java`, `GuideLatexTextureCache.java` | LaTeX rendering pipeline (inline and display) |

### 3.6 Scene System (`scene/`)

| Path | Key Files | Relevance |
|---|---|---|
| `scene/` | `SceneTagCompiler.java`, `LytGuidebookScene.java`, `GuidebookLevelRenderer.java`, `CameraSettings.java`, `PerspectivePreset.java`, `GuideEntityRenderStateResolver.java`, `GuidebookSceneWeatherEffect.java`, `GuidebookSceneWeatherSupport.java`, `SceneViewportMetrics.java`, `SceneSoundCue.java`, `BlockStatsCorner.java`, `BlockStatsDock.java`, `SnbtPlacement.java`, `StructureLibSceneBinding.java`, `StructureLibSceneCondition.java`, `StructureLibSceneConditionParser.java`, `StructureLibValueCondition.java` | GameScene rendering, camera, weather, particles, entity rendering, SNBT structure import |
| `scene/element/` | Scene element renderers | Individual scene element rendering |
| `scene/annotation/` | Annotation renderers | BlockAnnotation, BoxAnnotation, LineAnnotation, DiamondAnnotation, TextAnnotation |
| `scene/preview/` | Scene preview support | Preview rendering |
| `scene/ponder/` | Ponder timeline support | ImportPonder keyframe timeline |
| `scene/cache/` | Scene cache | Scene rendering cache |
| `scene/snapshot/` | Scene snapshot | Scene state capture |
| `scene/support/` | Supporting scene utilities | — |
| `scene/concurrent/` | Concurrent scene loading | — |
| `scene/level/` | Level setup for scenes | World preparation for scene rendering |

### 3.7 Render Engine (`render/`)

| Path | Key Files | Relevance |
|---|---|---|
| `render/` | `GuideRenderEngine.java`, `GuidePageTexture.java`, `GuidebookSceneRenderer.java`, `GuideText.java`, `GuideGlyphAtlas.java`, `GuideFontCompat.java`, `VanillaRenderContext.java`, `RenderContext.java`, `PrimitiveCollector.java`, `GuideRenderPrimitive.java`, `GlyphRunData.java`, `GlyphRunGroup.java`, `GlyphRunHolder.java`, `GuiSprite.java`, `GuiAssets.java` | Page rendering, glyph atlas, text rendering, scene rendering into GUI |

### 3.8 Other Relevant Packages

| Path | Key Files | Relevance |
|---|---|---|
| `document/` | `LytRect.java`, `LytSize.java`, `LytPoint.java`, `LytErrorSink.java`, `DefaultStyles.java` | Geometry primitives, error reporting |
| `color/` | (Color handling) | Section-code colour processing |
| `extensions/` | (Guide extensions) | Extension support |
| `indices/` | (Guide index builders) | SubPages, Category index building |
| `navigation/` | (Navigation tree) | Navigation tree structure |
| `mediawiki/` | (MediaWiki import) | MediaWiki source conversion |
| `sound/` | (Sound system) | SoundLink support |
| `siteexport/` | (Site export) | HTML/site export |
| `style/` | (Style system) | Styling infrastructure |
| `internal/` | (Internal utilities) | Internal helpers |

### 3.9 Rust Native Engine

The layout engine is implemented in Rust. Source: `layout-engine/`. Built DLL:

| Variant | Path |
|---|---|
| Redirected build | `E:/build_out/guide_nh_rust/release/guide_layout_engine.dll` |
| In-tree fallback | `layout-engine/target/release/guide_layout_engine.dll` |

Key native entry points relevant to visual tests include `measure.rs` (glyph measurement), `inline_post_pass` (LaTeX baseline correction), and the Taffy integration for flexbox layout.

---

## 4. Test Corpus Resources

Fixture resource pack root: `visualtest/resourcepack/`

### 4.1 Pack Metadata

| File | Description |
|---|---|
| `pack.mcmeta` | Resource pack descriptor |
| `pack.png` | Pack icon |

### 4.2 Page Tree

All fixture pages live under `assets/guidenh/guidenh/_en_us/visualtest/`. The full list of 63 fixture files by subdirectory:

| Subdirectory | Files |
|---|---|
| `text/` | `headings.md`, `paragraphs.md`, `inline-marks.md`, `section-codes.md`, `links.md`, `cjk-mixed.md`, `inline-game-tags.md`, `footnotes.md` |
| `lists/` | `basic.md`, `rich.md`, `tasks.md` |
| `tables/` | `basic.md`, `wide.md`, `cjk.md`, `metadata.md`, `csv.md` |
| `code/` | `blocks.md`, `special-langs.md` |
| `latex/` | `inline.md`, `display.md` |
| `images/` | `basic.md`, `float.md`, `fullwidth.md`, `floating-image.md`, `block-item.md` |
| `floats/` | `wrap-modes.md`, `content-types.md`, `multi.md`, `in-tabs.md` |
| `layout/` | `row-column.md`, `align.md`, `details.md`, `content-tabs.md` |
| `charts/` | `bar-column.md`, `line-scatter.md`, `pie.md`, `function.md`, `options.md` |
| `mermaid/` | `mindmap.md`, `flowchart.md`, `subgraphs.md`, `node-content.md`, `large.md` |
| `nei/` | `recipes.md`, `item-grid.md` |
| `scenes/` | `blocks.md`, `entities.md`, `annotations.md`, `effects.md`, `camera.md`, `import.md`, `ponder.md` |
| `meta/` | `frontmatter.md`, `zoom-small.md`, `zoom-large.md`, `indexes.md`, `indexes-sub-a.md`, `indexes-sub-b.md` |
| `overflow/` | `tall-element.md`, `exact-fit.md`, `scroll-containers.md` |
| `stress/` | `mixed.md` |
| (root) | `index.md` |

### 4.3 Asset Files

Supporting assets are stored under `assets/guidenh/guidenh/_en_us/visualtest/assets/`:

| File | Description | Used By |
|---|---|---|
| `red-64.png` | 64×64 red solid image | `images/` fixtures |
| `checker-128.png` | 128×128 checkerboard pattern | `images/` fixtures |
| `tall-64x256.png` | 64×256 tall image | `images/` fixtures |
| `wide-256x64.png` | 256×64 wide image | `images/` fixtures |
| `test-structure.snbt` | SNBT structure for ImportStructure test | `scenes/import.md` |
| `test-table.csv` | CSV table data | `tables/csv.md` (via `csv` code block resource) |

### 4.4 Injection Mechanism

The fixture pack is injected at development time via the `-Dguidenh.guide.sources` JVM parameter, which is forwarded to `-Dguideme.resourcePack.sources`. The pack is merged into the `guidenh:guidenh` guide. For in-game verification, a junction or copy must be placed at `config/guidenh/DefaultGuide/` (see `DefaultGuideResourcePackManager.java`).
