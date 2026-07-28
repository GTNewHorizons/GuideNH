# visualtest 视觉测试语料库规格 v2

> 本文件是 fixture 语料库的唯一权威规格。v2 基于代码级特性普查（TagAttributeRegistry /
> 各 TagCompiler / Lyt* 块类 / Frontmatter），目标：引擎支持的每个特性都有专页详测。
> 组织：语义文件夹（不用数字前缀），简单 → 复杂递进。引擎无分页器（单遍连续布局），
> 原 pagination/ 改题为 overflow/（视口溢出与滚动容器行为）。

## 运行方式

```bash
# 全量渲染语料库（scale=2 为视觉检查默认档）
./gradlew runClient25 \
  -Dguidenh.guide.sources=D:/Projects/GuideNH/visualtest/resourcepack \
  -Dguidenh.headlessRender=true \
  -Dguidenh.renderpage.guide=guidenh:visualtest \
  -Dguidenh.renderpage.allPages=true \
  -Dguidenh.renderpage.out=screenshots_visualtest \
  -Dguidenh.renderpage.width=900 -Dguidenh.renderpage.scale=2

# 单页：page id = guidenh: + _en_us/ 下相对路径（含子文件夹）
#   -Dguidenh.renderpage.page=guidenh:mermaid/mindmap.md
```

## 编写规范

1. **单焦点**：一个文件只测一类特性；文件内多个用例为该特性的变体组合。
2. **自解释**：每个用例前一行说明文字写"此处应当：……"，使人工/AI 审阅预期可见。
3. **frontmatter**：
   ```yaml
   ---
   navigation:
     title: <英文标题>
     parent: index.md
     position: <号段内递减，大者靠前>
   ---
   ```
4. 页面统一放 `_en_us/`（加载兜底最稳）；CJK 测试内容直接写正文。
5. 页面保持短（1-3 屏）；overflow/ 与压力页除外。
6. 结构文字用英文；被测对象内容按需。
7. 每文件头部 `<!-- -->` 注释写明：测试目标 + 不变式编号，与本文档条目对应。
8. 语法参考：`wiki/resourcepack/assets/guidenh/guidenh/_en_us/*.md`（官方文档页，
   含真实用例）；不确定的属性名以 `TagAttributeRegistry.java` 为准，禁止臆造。

## 文件夹与号段

| 文件夹 | position 号段 | 主题 |
|---|---|---|
| (root) index.md | 9999 | 语料索引 |
| text/ | 9000-9099 | 标题、段落、行内样式、§码、链接、CJK、行内标签、脚注 |
| lists/ | 8900-8999 | 列表（含任务列表） |
| tables/ | 8800-8899 | 表格（含 widths 元数据、CsvTable） |
| code/ | 8700-8799 | 代码块（语言、宽高属性、特殊 lang 渲染） |
| latex/ | 8600-8699 | LaTeX 行内/展示 |
| images/ | 8500-8599 | 图片、BlockImage、FloatingImage、fullWidth |
| floats/ | 8400-8499 | 浮动系统矩阵（wrap×内容×align×clear） |
| layout/ | 8300-8399 | Row/Column、details、align、ContentTabs、SizeBox |
| charts/ | 8200-8299 | 5 种图表 + 函数图 + 选项 |
| mermaid/ | 8100-8199 | mindmap 双模式/形状/NodeContent、flowchart 嵌套/箭头/边标签 |
| nei/ | 8000-8099 | Recipe 三标签 + ItemGrid |
| scenes/ | 7900-7999 | GameScene 全子标签 |
| meta/ | 7800-7899 | frontmatter 变体、SubPages、CategoryIndex |
| overflow/ | 7700-7799 | 超高元素、SizeBox、恰好满页（无视口分页器） |
| stress/ | 7600-7699 | 混合压力页 |

## 逐文件规格

### text/（行内与文本）

**text/headings.md** — 标题层级与分隔线（backlog K2）
- H1-H6 全级；标题紧跟正文；标题紧跟标题；长标题折行。
- 不变式：分隔线不穿字；连续标题间距一致；长标题不溢出右边距。

**text/paragraphs.md** — 段落与换行
- 多段落、软换行 vs 空行、恰好满行宽段落、单字段落、`<br>` 与 `<br clear>`。
- 不变式：行距/段距一致；无异常空白带。

**text/inline-marks.md** — 全部行内标记
- `**粗**` `*斜*` `***粗斜***` `~~删~~` `~删~` `++下划线++` `^^波浪^^` `::强调点::` `==高亮==` `` `代码` `` `<kbd>` `<sub>` `<sup>` `<span>` 及互相嵌套。
- 不变式：样式不泄漏后续文本；嵌套渲染正确；装饰线不压上下行。

**text/section-codes.md** — § 颜色码（backlog K1）
- § 全色码逐行；§l/§o/§r 组合；50 连续 § 码；孤立 "§" 字面量；`<Color id/color>` 对照。
- 不变式：无巨型字形；颜色切换正确；字形尺寸全页一致。

**text/links.md** — 链接全家族
- 页内锚 `<a name>` + 跳转、跨页 `[text](page.md)`、外链、自动链接（裸 URL）、参考式 `[ref][]`、`&[音效](sound:...)`、`<CommandLink>`、`<SoundLink>`、带 title(tooltip) 链接。
- 不变式：着色/下划线一致；点击区 bbox = 文本 bbox（bounds JSON 可断言）。

**text/cjk-mixed.md** — 中英混排
- 中英混排、全角标点、CJK 无空格长串换行、40+ 字符英文长词、CJK+行内代码/链接混排。
- 不变式：任意断行不溢出；混排基线一致。

**text/inline-game-tags.md** — 游戏内联标签
- `<ItemImage>`（id/scale/label 左右/format）、`<ItemLink>`（showIcon/showText/linksTo）、`<KeyBind>`、`<PlayerName>`、`<Tooltip label>`（行内触发）、`<Spoiler>`。
- 不变式：图标与文本基线对齐；行高不被图标异常撑大；tooltip 触发区正确。

**text/footnotes.md** — 脚注
- `[^a]` 引用多个 + `<FootnoteList>`。
- 不变式：引用渲染为上标链接；列表收集完整。

### lists/

**lists/basic.md** — 列表基础：`*` `-` `+` 无序 3 级、有序 3 级（含 `start`）、混合嵌套。
- 不变式：缩进/标记间距逐级一致。

**lists/rich.md** — 富内容列表项：项内多段落、代码块、链接、展示公式、小表格、图片。
- 不变式：续行左对齐；嵌入块不破坏编号。

**lists/tasks.md** — 任务列表：`- [x]` `- [ ]` 混合、嵌套、富文本标签。
- 不变式：复选框与文本对齐；状态样式区分。

### tables/

**tables/basic.md** — 2 列窄表、3 列对齐表（左/中/右）。
**tables/wide.md** — 5 列宽表、超长英文词单元格、多行单元格。
**tables/cjk.md** — CJK 表头/单元格/混合、CJK 长串。
**tables/metadata.md** — `{: widths="120,80" }` 列宽、宽窄组合。
**tables/csv.md** — `<CsvTable src>` + `csv` 代码块两种形式（header/widths 变体；csv 资源放 `_en_us/assets/`）。
- 不变式（各文件）：总宽 ≤ 页宽；折行不溢出列界；行高一致；列分隔线对齐。

### code/

**code/blocks.md** — 代码块全变体
- 多语言（xml/java/json/python/无语言）、80+ 列长行、空行、单行、特殊字符 `<>&"§`、`width= height=` 固定视口（滚动容器）、缩进代码块。
- 不变式：长行策略符合规格；背景框包裹全行；固定视口出现滚动条而非溢出。

**code/special-langs.md** — 特殊 lang 渲染
- `tree`/`filetree`（含 `{:icon=}` 后缀）、`csv`（对照 tables/csv.md）。
- 不变式：渲染为树/表格而非纯文本代码。

### latex/

**latex/inline.md** — 行内公式（backlog：行内掉行）
- `$E=mc^2$`、`$\frac{1}{2}$`、`$\sqrt{x}$` 行文嵌入；"contains X and also Y in the same line" 最小复现。
- 不变式：基线对齐、水平位置在文本流正确处；分数撑行不压字。

**latex/display.md** — 展示公式
- `$$...$$` 独立行、`<Latex>` block 形式、scale=1.5、valign 变体、color、带 tooltip。
- 不变式：水平居中；缩放不溢出；段距一致。

### images/（图片资源：新增纯色/格子测试 PNG 放 `_en_us/assets/`）

**images/basic.md** — `![alt](src)` 小/中/宽图、居中 align、图注(title)。
**images/float.md** — `wrap="square|tight|through"` × `align="left|right"` 图片 + 环绕文字 + `<br clear="both">`。
**images/fullwidth.md** — fullWidth 图片/表格/代码块对照（backlog K4：LytFloatAwareBlock 包装层丢 fullWidth）。
**images/floating-image.md** — `<FloatingImage>` 全家：裁剪 x/y/w/h、scaleX/scaleY、`<ImageAnnotation>` 热区、`<SoundArea>`（标注不发声，仅查渲染）。
**images/block-item.md** — `<BlockImage>`（scale/wrap/align/float）+ 块状 ItemImage 对照。
- 不变式：不变形；环绕不压图无异常空洞；clear 后顶对齐；fullWidth 实宽=内容宽（bounds 可断言）。

### floats/（浮动系统矩阵——用户点名）

**floats/wrap-modes.md** — wrap 六模式
- `square`/`tight`/`through`/`top-bottom`/`behind`/`front` 各一例（图片）+ 文字环绕。
- 不变式：各模式环绕行为符合 ContentWrapMode 语义；behind/front 层序正确。

**floats/content-types.md** — 任意块可浮
- 浮动：表格、代码块、GameScene、Recipe、图表、Column 容器（各一）。
- 不变式：非段落块（LytFloatAwareBlock 路径）可用宽度缩减正确、不与浮动重叠。

**floats/multi.md** — 多浮动与清除
- 连续左浮 3 个、左右对浮、浮动紧跟浮动、`<br clear="left|right|both">` 三变体。
- 不变式：浮动间不重叠；clear 后文字从浮动底边以下开始。

**floats/in-tabs.md** — ContentTabs × 浮动（用户点名 contentTab 浮动）
- `<ContentTabs>` 内 Tab 里放浮动图片/场景；tabs 块前后再放浮动。
- 不变式：tab 内浮动不泄漏到 tab 外；tabs 块自身（fullWidth+LytFloatAwareBlock 路径）不受外部浮动异常压缩。

### layout/

**layout/row-column.md** — `<Row>`/`<Column>`：gap 变体、alignItems 四值、fullWidth、嵌套 Row in Column。
**layout/align.md** — 块级 align：left/center/right 图片、表格、场景对照。
**layout/details.md** — `<details>` open/closed、内嵌表格/代码/图片、连续多个。
**layout/content-tabs.md** — `<ContentTabs>`：3-4 Tab（不同内容类型）、title/default/defaultIndex/color/icon 变体。
**layout/size-box.md** — 固定宽高滚动容器（代码块 width/height 之外的通用形式，如存在；若仅代码块支持则并入 code/blocks.md 并在偏差记录说明）。
- 不变式：flex 间距/对齐正确；details 开合高度正确；Tab 头与内容对齐；溢出容器出滚动条。

### charts/

**charts/bar-column.md** — `<BarChart>` `<ColumnChart>` + `<Series>`（data/points/color/icon）+ `<PieInset>`。
**charts/line-scatter.md** — `<LineChart>`（numericX）`<ScatterChart>` + `<LineSeries>`。
**charts/pie.md** — `<PieChart>` + `<Slice>`（startAngle/clockwise/labelPosition）。
**charts/function.md** — `<FunctionGraph>`+`<Plot>`+`<Point>`、`<Function>` 简写、xRange/quadrants。
**charts/options.md** — legend 五值、cornerLegend、轴 label/min/max/step/unit/tickFormat、grid 开关与颜色。
- 不变式：不溢出容器；轴文字不重叠；图例不压图。

### mermaid/（用户点名：含嵌套子节点）

**mermaid/mindmap.md** — mindmap 双模式（默认根居中左右交替 + TIDY_TREE）、节点形状（圆角/圆/六边/云/爆炸）、**多层嵌套子节点**（≥4 层）。
- 不变式：树连线正确、节点不重叠、深层嵌套不串层。

**mermaid/flowchart.md** — flowchart：节点形状（stadium/rounded/diamond/rect/cylinder/subprocess/double-circle）、箭头样式（实线/虚线/点线 × 三角/圆/叉头）、边标签、classDef/linkStyle。
- 不变式：箭头方向正确、标签不压线、形状渲染齐全。

**mermaid/subgraphs.md** — **嵌套 subgraph**（用户点名）：2 层、3 层嵌套（≤4 层配色上限）、subgraph 间跨边。
- 不变式：嵌套框包含关系正确、配色分层、跨边不穿框。

**mermaid/node-content.md** — `<NodeContent id>` 富内容节点（节点内嵌格式化文本/列表/小图）。
- 不变式：富内容在节点框内布局正确。

**mermaid/large.md** — 大图（20+ 节点）：容器内缩放/平移初始状态、超出容器时行为。
- 不变式：初始视口合理、不无限撑高页面。

### nei/

**nei/recipes.md** — `<Recipe id>`、`<RecipeFor input/output 过滤>`、`<RecipesFor limit>`、handlerName/handlerOrder、fallbackText（无配方时）。
**nei/item-grid.md** — `<ItemGrid>` 多物品网格（Row/Column 子项）。
- 不变式：配方框完整渲染、裁剪正确（glScissor 回归哨兵）；fallback 文本正常。

### scenes/（GameScene 全子标签；语法参照主指南 scene-*.md）

**scenes/blocks.md** — `<Block>`（id/meta/facing/nbt/formed）、`<PlaceBlock>`、`<ReplaceBlock>`、`<RemoveBlocks>`（回归哨兵：scale 修复）。
**scenes/entities.md** — `<Entity>`（NBT 变体）、`<RemoveEntity>`（backlog：实体 Y 偏移复现）。
**scenes/annotations.md** — 五种注解（Block/Box/Line/Diamond/Text）+ `<LinePoint>` + `<BlockAnnotationTemplate>`。
**scenes/effects.md** — `<Particle>`、`<Weather>` 雨雪、`<PlaySound>`（仅查渲染存在性）。
**scenes/camera.md** — perspective 三值、rotateX/Y/Z、offset/center、`<IsometricCamera>`、zoom。
**scenes/import.md** — `<ImportStructure>`（snbt 资源放 assets/）、`<ImportStructureLib>` + `<Tier>`/`<Channel>`/`<Facing>`/`<Rotation>`/`<Flip>`/`<Orientation>`/GT 标记（环境不允许的记偏差）。
**scenes/ponder.md** — `<ImportPonder>` 关键帧时间线（若依赖外部资源则最小用例+偏差记录）。
- 不变式：场景内容在背景框内、尺寸匹配；实体贴地；注解对位；效果不出场景区。

### meta/

**meta/frontmatter.md** — navigation.icon/icon_texture/icons 变体、categories、item_ids、author/date（侧边栏不可见时以加载无错+正文正常为准）。
**meta/zoom-small.md** — frontmatter `zoom: 0.8`。
**meta/zoom-large.md** — frontmatter `zoom: 1.5`。
**meta/indexes.md** — `<SubPages>`、`<CategoryIndex category>`。
- 不变式：加载无错；zoom 页渲染缩放正确；索引列表完整。

### overflow/（原 pagination/，引擎无分页器）

**overflow/tall-element.md** — 高度 >1 屏的 GameScene、超长表格、超大 mermaid。
- 不变式：不崩溃不死循环；整体渲染完整（长截图机制下全高输出）。

**overflow/exact-fit.md** — 恰好整数屏内容、末尾单元素页。
- 不变式：无异常尾部空白；满屏元素不溢出。

**overflow/scroll-containers.md** — 固定视口（代码块 width/height、LytSizeBox 路径）内嵌超长内容。
- 不变式：容器内滚动而非页面级溢出。

### stress/

**stress/mixed.md** — 全语法混合压力页（最后编写）：标题+表格+代码+公式+图+浮动+tabs+图表+mermaid+场景同页。
- 不变式：整体无可感知异常（以人工/多模态审阅为主）。

## backlog 复现映射

| backlog 项 | 复现文件 |
|---|---|
| K1 巨型"§" | text/section-codes.md |
| K2 标题分隔线穿字 | text/headings.md |
| K4 fullWidth 不满宽 | images/fullwidth.md |
| 实体 Y 偏移 | scenes/entities.md |
| 行内 LaTeX 掉行 | latex/inline.md |
| recipes.md::gamescene:40 物化失败 | scenes/import.md |
| example_structure.snbt 缺失 | scenes/import.md（补测试资源） |

## 断言翻译指引（棘轮）

1. 问题确认后先把不变式写成 bounds JSON 几何断言进 harness；
2. 几何表达不了的标"仅视觉"；
3. 修复完成 = 断言进 harness + 全量门禁绿 + 该 fixture 截图复核通过。
