# visualtest 视觉测试语料库规格 v2

> 本文件是 fixture 语料库的唯一权威规格。v2 基于代码级特性普查（TagAttributeRegistry /
> 各 TagCompiler / Lyt* 块类 / Frontmatter），目标：引擎支持的每个特性都有专页详测。
> 组织：语义文件夹（不用数字前缀），简单 → 复杂递进。引擎无分页器（单遍连续布局），
> 原 pagination/ 改题为 overflow/（视口溢出与滚动容器行为）。

## 术语与分诊（先读！）

**真实引擎问题**：在真实游戏内 GUI 也能复现的引擎缺陷（布局错误、特性失效、迁移丢失等）。
**离线渲染问题**：只在无头/离屏捕获路径出现的假象——游戏内正常，例如异步布局未完成即截图、
离屏 FBO 特性、窗口隐藏时序、字体/GL 上下文差异。

**分诊规则（每个发现必须过）**：
1. 发现视觉异常时**默认先假设是离线渲染问题**，用下列方法排除后才允许定性真实引擎问题：
   - **金标准：进游戏肉眼对照**（同一页面在真实客户端是否正常）；
   - 主指南对照：主指南同特性页在离线渲染下是否正常（正常→fixture 写法问题；同样异常→倾向真实）；
   - 日志证据：编译告警/渲染堆栈/异步完成回调是否在截图前发生；
   - 机制推理：该特性是否有异步/延迟初始化路径（如 ELK 布局、结构物化、纹理下载）。
2. 定性结论必须写明证据（"游戏内复现" / "仅离线出现" / "未分诊"）。**未分诊的发现禁止进修复队列**。
3. mermaid 占位框：**已定性真实引擎问题**（用户进游戏对照确认：离线渲染行为与游戏内一致，均只显示占位标签框；疑重构迁移丢失 ELK 布局/异步绘制链）。转入统一修复队列，与其他批量发现一并处理以获耦合视野。
4. 已知离线特有现象（勿再上报）：截图首帧异步内容未就绪、离屏渲染与屏幕渲染的微小像素差。

## 运行方式（自包含快速上手）

### 术语速查
- **无头批量渲染**：`runClient25` + `-Dguidenh.headlessRender=true`，客户端自启世界、逐页离屏渲染、
  写 PNG（+可选 bounds JSON / overlay PNG）后自动退出（exit 0）。
- **看门狗**：`tools/visual-inspection/render_watchdog.py`，防孤儿 JVM（harness 杀 shell 不杀子进程树）。
  超时 exit 124 并 taskkill /T + 孤儿清扫。**禁止裸跑 gradlew 渲染**。
- **列表文件**：`--list` 的唯一合法值是**文件路径**，文件内一行一个 pageId、`#` 注释。
  传页面 id 本身会触发 InvalidPathException（旧版被 FML 静默吞掉卡主菜单；现已加固为显式报错退出）。

### 渲染命令模板（git-bash）

```bash
# 0) 前置：清 daemon（防 busy daemon 连锁），准备列表文件
./gradlew --stop
mkdir -p C:/Temp/opencode/lists && printf 'guidenh:visualtest/mermaid/mindmap.md
' > C:/Temp/opencode/lists/demo.txt

# 1) 渲染（必须 cmd //c + 整条引号：/c 会被 MSYS 转成 C:/；裸 bash 会被解析成 WSL bash）
py -3 tools/visual-inspection/render_watchdog.py --timeout 600 --log C:/Temp/opencode/wd_render.log --   cmd //c "gradlew.bat runClient25     -Dguidenh.guide.sources=D:/Projects/GuideNH/visualtest/resourcepack     -Dguidenh.headlessRender=true     -Dguidenh.renderpage.guide=guidenh:guidenh     -Dguidenh.renderpage.list=C:/Temp/opencode/lists/demo.txt     -Dguidenh.renderpage.out=screenshots_visualtest     -Dguidenh.renderpage.width=900 -Dguidenh.renderpage.scale=2     -Dguidenh.renderpage.bounds=true -Dguidenh.renderpage.overlay=true"
```

### -D 参数全表（parseConfig 实证）

| 参数 | 语义 | 默认 | 备注 |
|---|---|---|---|
| `guidenh.guide.sources` | dev 资源包目录（转发为 `guideme.resourcePack.sources`） | 无 | fixture 包注入用，发布无影响 |
| `guidenh.headlessRender` | 激活无头驱动（隐藏窗口+注册驱动） | false | 必须 `=true` |
| `guidenh.renderpage.guide` | 目标指南 id | 必填 | fixture 用 `guidenh:guidenh` |
| `renderpage.page` / `.md` | 单页模式：页面 id / md 文件路径 | — | 与批量模式互斥 |
| `renderpage.allPages` | 批量：指南全部页（**会连主指南，勿用**） | — | — |
| `renderpage.list` | 批量：**列表文件路径**（一行一 id） | — | 见上方术语 |
| `renderpage.out` | 输出目录（相对 run/client_new/） | screenshots | — |
| `renderpage.width` | 页宽 px（100-4096） | 900 | — |
| `renderpage.scale` | 渲染放大（1-4） | 1 | **用 2**（字体/3D 才清晰） |
| `renderpage.lang` | 语言 | en_us | — |
| `renderpage.bounds` | 同时写 bounds JSON（几何初筛数据源） | false | 初筛必须 =true |
| `renderpage.overlay` | 同时写布局 overlay PNG | false | — |
| `renderpage.world` | 使用的存档 | screenshot-world | 已存在于 saves/ |

### 产物与时延
- 产物：`run/client_new/screenshots_visualtest/<页名>_<时间戳>.png`（+.json/+_overlay.png）。
- 时延：冷启动到首张截图 ~3-4min（85 mods 加载）；批量摊薄（实测 5 页 175s）。
- 批次结束日志：`Batch complete: total=N ok=N failed=0`，进程 exit 0 自动退出。

### 进程管理铁律
1. 渲染前 `./gradlew --stop`；渲染后确认 watchdog 报 0 孤儿。
2. **异常后禁止不诊断就用更长超时盲重试**——先看日志定位，再行动。
3. 看门狗 timeout 建议 480-600（超首屏时延即可，批量页均 <1min）。

### 故障分诊表

| 症状 | 判据（grep 日志） | 原因 | 处置 |
|---|---|---|---|
| 卡主菜单循环 | 无 `Registering headless render driver` | 驱动未注册：旧版配置错误被 FML 静默吞掉 | 找 parseConfig 显式报错；`--info` 看 `Starting process` 确认 -D 到达 |
| `Invalid headless render configuration` | 控制台 | -D 拼写错 / list 语义错 | 按报错提示改 |
| `Guide not found` | 日志 | renderpage.guide 错 | 用 `guidenh:guidenh` |
| 窗口可见未隐藏 | 肉眼 | headlessRender 未到达 JVM（引用/转发丢失） | 检查 cmd //c 引用形式 |
| 批次 Page FAIL | `Page FAIL` 行 | 页面编译/渲染错误 | 看同日志堆栈 |

### 初筛员（screen.py 三子命令）

```bash
# 第 0 层 几何（需 bounds=true 渲染产物；规则见 tools/visual-inspection/README.md）
py -3 tools/visual-inspection/screen.py geometric --shots run/client_new/screenshots_visualtest --page-width 1800 --out C:/Temp/opencode/geo.json

# 第 1 层 VLM（需 .env：DASHSCOPE_API_KEY；模型 qwen3-vl-plus【选定】；--dry-run 先验瓦片）
py -3 tools/visual-inspection/screen.py vlm --shots run/client_new/screenshots_visualtest --out C:/Temp/opencode/vlm.json

# 第 2 层 合并报告
py -3 tools/visual-inspection/screen.py report --geo C:/Temp/opencode/geo.json --vlm C:/Temp/opencode/vlm.json --out C:/Temp/opencode/report.md
```
配置：复制 `tools/visual-inspection/.env.example` 为 `.env` 填 key（.env 已 gitignore，禁提交）。


**结构约定（实证修正）**：引擎只扫描固定 folder `guidenh`（DataDrivenGuideLoader.AUTO_GUIDE_FOLDER），
独立 guide 不可行。fixture 挂在自有资源包的 `assets/guidenh/guidenh/_en_us/visualtest/` 子树下，
页面合并进 `guidenh:guidenh` 指南（仅开发期 -D 注入，不影响发布）。页面 id 规则：
`guidenh:visualtest/<子文件夹>/<文件>.md`（如 `guidenh:visualtest/mermaid/mindmap.md`）。
frontmatter **不写 parent**（实证：parent 按页面所在文件夹相对解析，写 `visualtest/index.md` 会被解析成 `visualtest/<子文件夹>/visualtest/index.md` 报 unknown parent；visualtest/index.md 写 `parent: index.md` 会自指成 cycle，疑似阻塞启动链）。fixture 页全部省略 parent，渲染按页面 id 进行，导航树形态对 fixture 无意义。

```bash
# 渲染语料库指定页面。注意：--list 是【列表文件路径】（一行一个 pageId，# 为注释），不是逗号分隔 id；
# 误传页面 id 会触发 InvalidPathException 被 FML 静默吞掉、卡主菜单（已在 parseConfig 加固为显式报错）。allPages 会连主指南一起渲染，不用。
py -3 tools/visual-inspection/render_watchdog.py --timeout 600 --log C:/Temp/opencode/wd_render.log -- cmd //c "gradlew.bat runClient25 -Dguidenh.guide.sources=D:/Projects/GuideNH/visualtest/resourcepack -Dguidenh.headlessRender=true -Dguidenh.renderpage.guide=guidenh:guidenh -Dguidenh.renderpage.list=C:/Temp/opencode/lists/mermaid.txt -Dguidenh.renderpage.out=screenshots_visualtest -Dguidenh.renderpage.width=900 -Dguidenh.renderpage.scale=2"
# note: in git-bash you must use cmd //c and quote the whole command (/c gets mangled to C:/ by MSYS; bare bash resolves to WSL bash)
```

### 游戏内实测（真实客户端对照）

**已验证的部署方式：DefaultGuide 注入**（mod 自带机制，日志实证 "Registered DefaultGuide resource pack"，
且经此路径的无头渲染实测 exit 0）。GuideNH 启动时把 `config/guidenh/DefaultGuide/` 直接注入 FML
资源包列表（`DefaultGuideResourcePackManager.java`），不经资源包仓库，任何实例都生效。

- **开发实例**：已建 junction `run/client_new/config/guidenh/DefaultGuide` → `visualtest/resourcepack`
  （单一事实源，fixture 改动即时反映到游戏内）。
- **其他实例（含真实 GTNH 安装）**：把 `visualtest/resourcepack` 下的 `assets/` 整个复制到该实例的
  `config/guidenh/DefaultGuide/` 下即可（`pack.mcmeta`/`pack.png` 可不带）。
- **页面归属**：fixture 页面与主指南合并进同一 guide `guidenh:guidenh`。visualtest 各页面无 `parent`，
  是导航树根节点，出现在指南 GUI 左侧导航栏根级列表，点击直达。
- **打开方式**：游戏内 `/guidenhc open guidenh:guidenh`（客户端命令，`GuideNhClientCommand.java:56,78,144-158`；
  不支持指定具体页面），或指南物品打开后从导航栏进入。

**已证伪的路径（勿用）**：把包放进 `resourcepacks/` 目录（即使 `pack.mcmeta` 齐全、且在 options.txt
启用）在开发实例**不会被加载**——两次无头渲染实测均 "Guide not found: guidenh:guidenh"，ResourceManager
重载列表中无对应 FileResourcePack（疑开发环境仓库扫描问题，未深究）。`run/client_new/resourcepacks/visualtest`
junction 保留无害但不要依赖。

> 本小节服务于"金标准：进游戏肉眼对照"的分诊流程。发现渲染异常时，以游戏内实测结果为最终判据。

## 编写规范

1. **单焦点**：一个文件只测一类特性；文件内多个用例为该特性的变体组合。
2. **自解释**：每个用例前一行说明文字写"此处应当：……"，使人工/AI 审阅预期可见。
3. **frontmatter**：
   ```yaml
   ---
   navigation:
     title: <英文标题>
     position: <号段内递减，大者靠前>
   ---
   ```
4. 页面统一放 `_en_us/`（加载兜底最稳）；CJK 测试内容直接写正文。
5. 页面保持短（1-3 屏）；overflow/ 与压力页除外。
6. 结构文字用英文；被测对象内容按需。
7. 每文件头部用**一行普通文本**写明：测试目标 + 不变式编号（会显示在截图里，兼作自解释）。**禁止 `<!-- -->`**——实证会被当正文渲染。
8. 语法参考：`wiki/resourcepack/assets/guidenh/guidenh/_en_us/*.md`（官方文档页，
9. `<br clear>` 合法值只有 left/right/all/none（BreakCompiler.java:21-30），**禁止 both**（会渲染错误文本）；浮动清除统一用 `clear="all"`。写散文引述标签必须放反引号（否则 MDX 当活标签）。
   含真实用例）；不确定的属性名以 `TagAttributeRegistry.java` 为准，禁止臆造。

## 文件夹与号段

| 文件夹 | position 号段 | 主题 |
|---|---|---|
| (root) index.md | 9999 | 语料索引 |
| text/ | 9000-8930 | 标题、段落、行内样式、§码、链接、CJK、行内标签、脚注 |
| lists/ | 8900-8880 | 列表（含任务列表） |
| tables/ | 8800-8760 | 表格（含 widths 元数据、CsvTable） |
| code/ | 8700-8690 | 代码块（语言、宽高属性、特殊 lang 渲染） |
| latex/ | 8600-8590 | LaTeX 行内/展示 |
| images/ | 8500-8460 | 图片、BlockImage、FloatingImage、fullWidth |
| floats/ | 8400-8370 | 浮动系统矩阵（wrap×内容×align×clear） |
| layout/ | 8300-8270 | Row/Column、details、align、ContentTabs（SizeBox 不存在，已并入 code/blocks.md） |
| charts/ | 8260-8220 | 5 种图表 + 函数图 + 选项 |
| mermaid/ | 8190-8150 | mindmap 双模式/形状/NodeContent、flowchart 嵌套/箭头/边标签 |
| nei/ | 8000-7990 | Recipe 三标签 + ItemGrid |
| scenes/ | 7900-7840 | GameScene 全子标签 + test-structure.snbt |
| meta/ | 7800-7770（辅助页 1-2） | frontmatter 变体、SubPages、Category |
| overflow/ | 7700-7680 | 超高元素、恰好满页、滚动容器（无视口分页器） |
| stress/ | 7600 | 混合压力页 |

> position 分配规则：每文件夹内从号段顶端递减、间隔 10；新增文件夹时更新本表。

## 裁决台账（2026-07-28 首轮：63 页全渲染 + geometric 53 + VLM 810 → K3 裁决）

初筛管道：geometric 53 条 + VLM 810 条（含 overlay 误扫）→ pass1 意图分诊（INTENDED 15 / KNOWN 28 /
SUSPECT 129 / FP 128）→ K3 截图逐簇裁决。以下结论全部有截图证据。

### A. 真实引擎问题（截图证实，进统一修复队列）

| # | 问题 | 证据（页） |
|---|---|---|
| A1 | mermaid 占位框（已定性，疑迁移丢失） | mermaid/* + stress |
| A2 | JSX `<table align>` 空列崩溃 LytTable.java:176 | layout/align（用例已禁用待恢复） |
| A3 | **ContentTabs 头高度≈0，内容与 tab 条重叠**；icon 压标题 | layout/content-tabs、floats/in-tabs、stress；geometric ContentTabsHeader h=0 ×10 佐证 |
| A4 | **details 正文左缘裁切约 1 字符**（"T→is""D→iamond""S→ystem"） | layout/details |
| A5 | **角落图例在 TL/BL/BR 被绘图区边缘裁剪**（TR 正常） | charts/options |
| A6 | **展示 LaTeX 不居中**（全部左对齐，违反居中不变式） | latex/display、stress |
| A7 | `<Latex>` 行内公式掉到下一行（backlog 旧录，截图实证） | latex/inline 全页 |
| A8 | **`$...$`/`$$...$$` 简写完全不解析，原样输出**（新发现） | latex/inline、stress |
| A9 | **ImportStructure 后 ReplaceBlock/RemoveBlocks 未作用于导入方块**（三场景渲染一致） | scenes/import |
| A10 | **RecipeFor/RecipesFor/handler 过滤/fallbackText 全部只渲染 "[Recipe]" 占位**（仅 `<Recipe id>` 正常） | nei/recipes |
| A11 | **fullWidth Column 内代码块不撑满容器**（K4 家族实证；表格能撑满） | images/fullwidth |
| A12 | **任务列表 `- [x]` 无复选框/无标记渲染**（新发现） | stress（lists/tasks 待复核） |
| A13 | **§ 颜色/格式码完全不生效原样输出**（`<Color>` 标签正常对照） | text/section-codes 全页 |

### B. 未复现 → 进游戏验证（金标准）

- K1 巨型 §：50 连续 § 全部正常尺寸，**未复现** → 进游戏对照主指南 markdown.md
- K2 标题分隔线穿字：headings 页分隔线干净，**未复现** → 进游戏验证
- 实体 Y 偏移悬浮：entities 页羊/苦力怕/玩家贴地正常，**未复现** → 进游戏验证
- GameScene tab 内浮动占满整行、文字不环绕：证据较弱，标 suspect 待复核

### C. Fixture 缺陷（已修/修复中）

- C1 `clear="both"` 非法值 ×47（合法值 left/right/all/none）→ **已修**（8 文件）
- C2 `<FloatingImage>` 缺 x/y 编译报错（floats/in-tabs、wrap-modes）→ 修复中
- C3 content-tabs.md 的 ContentTabs 直接含文本节点（7:31 编译错误）→ 修复中
- C4 headings.md `---` 与文字同行导致直出 → 修复中
- C5 entities.md 场景视角太窄，第二实体出画 → 修复中
- C6 cjk/tables/headings "超长"用例长度不足触发折行 → 加长（弱优先级）
- C7 stress `{: widths=}` 直出 → 待对照 tables/metadata.md 重渲染后定性（引擎 vs 写法）

### D. 离线基建问题

- 无头批量渲染累积 OOM（63 页批次 ~42 页后 heap space；分流：批次 ≤40 页）
- 初筛员把 `*_overlay.png` 当独立页面扫描（125 vs 63 页，成本翻倍+噪声）→ screen.py 需过滤

### E. VLM 系统性误报（初筛员 prompt 调优输入）

- 页面右缘"文字硬截"（~11 条/10 页）：文字排到页宽边界的正常排版+瓦片边界伪影
- 瓦片接缝伪影 ~20 条；bounds overlay 辅助线当内容
- 调优：右缘裁剪 finding 需文字明显截半才报；overlay 不扫

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
**tables/csv.md** — `<CsvTable src>` + `csv` 代码块两种形式（header/widths 变体；csv 资源放 `_en_us/visualtest/assets/`）。
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

### images/（图片资源：新增纯色/格子测试 PNG 放 `_en_us/visualtest/assets/`）

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
**meta/indexes.md** — `<SubPages>`、`<Category name>`（代码实证 `<CategoryIndex>` 无编译器，真实标签是 `<Category name>`，CategoryCompiler.java:22-23）。
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
| mermaid 占位框（**已确认真实引擎问题**：游戏内与离线一致只渲染占位标签框，疑 ELK/异步绘制链迁移丢失） | mermaid/*.md；已进游戏对照定性 |
| JSX `<table align>` 空 columns 布局崩溃（**真实引擎崩溃**：NoSuchElementException at LytTable.layoutColumns LytTable.java:176；tr/td JSX 解析出 0 列；疑 BlockTagCompiler align 代理与 TableCompiler align 语义冲突） | layout/align.md JSX 表格用例已文字化禁用，修复后恢复 |
| 无头批量渲染累积 OOM（**离线基建问题**：63 页批次在第 ~42 页后 Java heap space（NEI-worker 线程先 OOM），41/63 成功后连续失败；疑似跨页资源未释放——截图世界/纹理/NEI 工件泄漏。分流策略：批次 ≤40 页或分批重跑） | 全量渲染分两批实证；修复候选：每页渲染后强制释放场景世界+纹理缓存 |

## 断言翻译指引（棘轮）

1. 问题确认后先把不变式写成 bounds JSON 几何断言进 harness；
2. 几何表达不了的标"仅视觉"；
3. 修复完成 = 断言进 harness + 全量门禁绿 + 该 fixture 截图复核通过。
