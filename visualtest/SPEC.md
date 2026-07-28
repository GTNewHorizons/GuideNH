# visualtest 视觉测试语料库规格

> 本文件是 fixture 语料库的唯一权威规格。每个测试页面按本规格编写；
> 每个确认的问题类最终翻译为 harness 断言绑定对应文件（棘轮）。
> 语料组织：语义文件夹（不用数字前缀），简单 → 复杂递进，实地测试最后。

## 运行方式

```bash
# 全量渲染语料库（scale=2 为视觉检查默认档）
./gradlew runClient25 \
  -Dguidenh.guide.sources=D:/Projects/GuideNH/visualtest/resourcepack \
  -Dguidenh.headlessRender=true \
  -Dguidenh.renderpage.guide=guidenh:visualtest \
  -Dguidenh.renderpage.allPages=true \
  -Dguidenh.renderpage.out=screenshots_visualtest \
  -Dguidenh.renderpage.width=900 \
  -Dguidenh.renderpage.scale=2

# 单页
./gradlew runClient25 \
  -Dguidenh.guide.sources=D:/Projects/GuideNH/visualtest/resourcepack \
  -Dguidenh.headlessRender=true \
  -Dguidenh.renderpage.guide=guidenh:visualtest \
  -Dguidenh.renderpage.page=guidenh:text/headings.md \
  -Dguidenh.renderpage.out=screenshots_visualtest \
  -Dguidenh.renderpage.width=900 -Dguidenh.renderpage.scale=2
```

页面 id 规则：`guidenh:` + `_en_us/` 下的相对路径（含子文件夹），如 `guidenh:text/headings.md`。

## 编写规范

1. **单焦点**：一个文件只测一类问题；文件内多个用例均为该类问题的变体。
2. **自解释**：每个用例前一行说明文字写"此处应当：……"，使人工/AI 审阅时预期可见。
3. **frontmatter**（参照主指南格式）：
   ```yaml
   ---
   navigation:
     title: <英文标题>
     parent: index.md
     position: <见各文件夹号段>
   ---
   ```
4. 页面语言文件夹统一用 `_en_us`（加载兜底最稳）；CJK 测试内容直接写在页面正文里。
5. 页面保持短（1-3 屏）；分页专项除外。
6. 结构文字用英文（与主指南一致）；被测对象内容按需（CJK 用例用中文）。
7. 每个文件头部注释（`<!-- -->`）写明：测试目标 + 预期不变式清单编号，与本文档条目一一对应。

## 文件夹与号段

| 文件夹 | position 号段 | 主题 |
|---|---|---|
| (root) index.md | 0 | 语料索引：全部页面 + 各自测试目标 |
| text/ | 100-199 | 标题、段落、行内样式、§颜色码、链接、CJK |
| lists/ | 200-299 | 列表 |
| tables/ | 300-399 | 表格 |
| code/ | 400-499 | 代码块 |
| latex/ | 500-599 | LaTeX |
| images/ | 600-699 | 图片、浮动、fullWidth |
| charts/ | 700-799 | 图表 |
| nei/ | 800-899 | NEI 配方框 |
| scenes/ | 900-999 | GameScene |
| pagination/ | 1000-1099 | 分页边界 |
| stress/ | 1100-1199 | 混合压力 |

## 逐文件规格

### text/

**text/headings.md** — 标题层级与分隔线（关联 backlog K2）
- 内容：H1-H4 各两级连续；标题后紧跟正文；标题后紧跟标题（无正文间隔）；长标题折行。
- 不变式：分隔线不穿字（线 y 坐标与文本行 bbox 不相交）；连续标题间距一致；长标题折行后不溢出右边距。

**text/paragraphs.md** — 段落与换行
- 内容：多段落、软换行 vs 空行分段、恰好满行宽度的段落、单字段落。
- 不变式：行间距一致；段间距 = 1.5×行间距（以引擎实际规格为准，断言时用实测基线）；无异常空白带。

**text/inline-styles.md** — 行内样式
- 内容：粗/斜/删除线/行内代码/组合嵌套；行内代码含长 token。
- 不变式：样式不泄漏到后续文本；行内代码背景框不压相邻行。

**text/section-codes.md** — § 颜色码（关联 backlog K1：巨型"§"撑爆图集）
- 内容：§ 全部颜色码逐行展示；§l/§o/§r 组合；超长 § 串（50 个连续 § 码）；正文夹杂孤立 "§" 字面量。
- 不变式：无巨型字形渲染（§ 符号本身不可见或按字面小号渲染）；颜色切换正确；图集不被撑爆（整页字形尺寸一致）。

**text/links.md** — 链接与行内 tooltip
- 内容：页内锚链接、跨页链接、外链样式、带 tooltip 的行内元素。
- 不变式：链接着色+下划线一致；点击区 bbox 与文本 bbox 一致（bounds JSON 可断言）。

**text/cjk-mixed.md** — 中英混排
- 内容：中英混排段落、全角标点行、CJK 长串无空格换行、英文长词（40+ 字符）断行、CJK+行内代码混排。
- 不变式：CJK 在任意位置断行不溢出；英文长词不溢出右边距；混排基线一致。

### lists/

**lists/basic.md** — 列表基础
- 内容：无序 3 级嵌套、有序 3 级嵌套、有序无序混合嵌套。
- 不变式：缩进逐级一致；标记与文本间距一致；嵌套列表不错位。

**lists/rich.md** — 富内容列表项
- 内容：列表项内含多段落、行内代码、链接、展示公式、小表格。
- 不变式：续行与首行文本左对齐；嵌入块不破坏后续列表编号。

### tables/

**tables/basic.md** — 窄表与对齐
- 内容：2 列窄表、3 列左/中/右对齐表。
- 不变式：列分隔线对齐；单元格文本不压线。

**tables/wide.md** — 宽表与长单元格
- 内容：5 列宽表、超长英文单词单元格、多行单元格。
- 不变式：表格总宽 ≤ 页宽；长内容折行不溢出列边界。

**tables/cjk.md** — CJK 表格
- 内容：全 CJK 表头+单元格、中英混合单元格、CJK 长串单元格。
- 不变式：CJK 单元格折行正确；行高一致。

**tables/pagebreak.md** — 跨页表格（与 pagination 联动）
- 内容：行数足以跨页的长表格（含表头）。
- 不变式：跨页断点在行边界；无半行截断；断点前后行距一致。

### code/

**code/blocks.md** — 代码块
- 内容：多语言代码块（xml/java/json/文本）、80+ 列长行、含空行代码块、单行代码块、特殊字符（`<>&"§`）。
- 不变式：长行不溢出代码框（折行或截断策略符合引擎规格）；背景框包裹所有行；语言标签不压代码。

### latex/

**latex/inline.md** — 行内公式（关联 backlog：行内 LaTeX 掉到下一行）
- 内容：行文中嵌入 $E=mc^2$、$\frac{1}{2}$、$\sqrt{x}$ 的行内公式；"contains X and also Y in the same line" 最小复现（照抄 markdown.md 现行失败用例）。
- 不变式：行内公式与同行文字基线对齐、水平位于文本流正确位置（不掉到下一行、不留异常空隙）；分数撑行高时上下行不压字。

**latex/display.md** — 展示公式
- 内容：独立行展示公式、scale=1.5 放大、valign 变体、带 tooltip 公式。
- 不变式：展示公式水平居中；缩放公式不溢出；相邻段落间距一致。

### images/

**images/basic.md** — 图片基础（图片资源放 `_en_us/assets/` 复用主指南示例图或新增纯色测试图）
- 内容：小/中/宽图各一、居中图、图注。
- 不变式：图片不变形；图注与图片间距一致。

**images/float.md** — 浮动图片
- 内容：左浮+右浮图片与环绕文字、连续浮动、浮动后清除。
- 不变式：文字环绕不压图、不留异常空洞；清除浮动后段落顶对齐。

**images/fullwidth.md** — fullWidth 元素（关联 backlog K4：LytFloatAwareBlock 包装层未设 fullWidth）
- 内容：fullWidth 图片、fullWidth 表格、普通宽度对照组。
- 不变式：fullWidth 元素实际渲染宽度 = 页内容宽（bounds JSON 可断言 width 值）。

### charts/

**charts/basic.md** — 图表（语法参照主指南 charts.md / function-graphs.md）
- 内容：折线、柱状、饼图各一；CSV 数据源；函数图。
- 不变式：图表不溢出容器；坐标轴文字可读不重叠。

### nei/

**nei/recipes.md** — NEI 配方框
- 内容：嵌入 NEI 配方框（参照主指南现有用法）。
- 不变式：配方框完整渲染、裁剪区正确（关联 scale 修复的 glScissor 路径回归哨兵）。

### scenes/

**scenes/blocks.md** — 基础方块场景
- 内容：单方块、多方块朝向（熔炉四向）、非全方块（台阶/楼梯/栅栏）、透明方块（水+玻璃）。
- 不变式：场景内容位于场景背景框内、尺寸匹配（scale 修复回归哨兵）；方块纹理完整。

**scenes/entities.md** — 实体场景（关联 backlog：实体 Y 偏移）
- 内容：实体站立在草方块上（羊/僵尸/玩家）最小复现；NBT 定制实体。
- 不变式：实体底部与支撑方块顶面接触（无悬浮）；实体居中于场景。

**scenes/annotations.md** — 场景注解
- 内容：文字注解、坐标轴标签、DiamondAnnotation。
- 不变式：注解文字位置与目标方块对应（缩放路径回归哨兵：注解也走 documentOrigin）。

**scenes/effects.md** — 天气与粒子
- 内容：雨、雪、billboard 粒子。
- 不变式：效果在场景区域内渲染；无全屏泄漏。

**scenes/import.md** — 结构导入（环境允许时）
- 内容：ImportStructureLib 最小用例（若缺资源则改为占位说明页并在偏差记录标注）。
- 不变式：导入结构完整渲染。

### pagination/

**pagination/tall-element.md** — 超页高元素
- 内容：高度 >1 页的场景、高表格。
- 不变式：不崩溃、不无限循环；元素截断/溢出策略符合引擎规格（断言以规格文字为准）。

**pagination/orphan-heading.md** — 孤行标题
- 内容：构造"标题恰好落在页底、正文在次页"的用例（用填充段落精确控制）。
- 不变式：标题不孤行（随正文移入次页）或按引擎规格处理；无标题与正文跨页分离。

**pagination/exact-fit.md** — 恰好满页与空页边界
- 内容：恰好满一页的内容；两页内容之间无残余空白页。
- 不变式：不产生全空白尾页；满页元素不溢出。

### stress/

**stress/mixed.md** — 混合压力页（最后编写）
- 内容：上述全部语法类别按真实文档密度混合：标题+表格+代码+公式+图+场景+列表同页。
- 不变式：整体无可感知异常（此页以人工/多模态审阅为主，断言从简）。

## backlog 复现映射

| backlog 项 | 复现文件 |
|---|---|
| K1 巨型"§"撑爆图集 | text/section-codes.md |
| K2 标题分隔线穿字 | text/headings.md |
| K4 fullWidth 不满宽 | images/fullwidth.md |
| 实体 Y 偏移悬浮 | scenes/entities.md |
| 行内 LaTeX 掉行 | latex/inline.md |
| recipes.md::gamescene:40 物化失败 | scenes/import.md（如可复现） |
| example_structure.snbt 缺失 | scenes/import.md（补充测试资源时一并处理） |

## 断言翻译指引（棘轮）

1. 问题确认后，先把不变式写成 bounds JSON 上的几何断言（宽/高/相交/包含），进 harness；
2. 几何表达不了的（字形美观、颜色），保留为视觉巡检项，在本文档对应条目标注"仅视觉"；
3. 修复完成 = 对应断言进 harness 且全量门禁绿 + 该 fixture 页截图复核通过。
