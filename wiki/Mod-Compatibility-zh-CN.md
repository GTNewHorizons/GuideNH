# 模组兼容

GuideNH 为部分模组内置了条件性兼容支持。每项集成只在目标模组加载时启用；原生内容提供者和悬停集成只在目标模组存在时启用。纯内容绑定身份与页面索引在目标模组缺席时仍可使用。

## StructureLib

当 StructureLib 加载时，GuideNH 可以通过 `<ImportStructureLib>` 把多方块预览导入 `<GameScene>`。客户端指令 `/exportStructure structureLib` 也可以把这些预览导出为 PNG 文档截图。完整指令参数、StructureLib 专属选项以及相关的 `gameScene` 导出模式见 [结构导出](Structure-Export-zh-CN)。

## BetterQuesting

当 [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) 加载时，GuideNH 解锁三项能力：

1. 通过 `bindings` 中的 `type: "betterquesting"` 将指南页关联到任务。页面索引不要求安装 BetterQuesting，任务界面跳转需要安装它。
2. 新增两个标签：`<QuestLink>`（行内）与 `<QuestCard>`（块级）。
3. 标准“打开指南”快捷键在 BetterQuesting 任务 GUI 中悬停某任务时也能生效：按住快捷键即可按任务 id 跳转到对应指南页。

### 按任务 id 索引页面

在希望与某个或多个任务关联的指南页 frontmatter 中添加 `bindings` 列表：

```yaml
---
navigation:
  title: 第二阶段 — 蒸汽时代
bindings:
  - {type: "betterquesting", id: "01234567-89ab-cdef-0123-456789abcdef"}
  - {type: "betterquesting", id: "AAAAAAAAAAAAAAAAAAAMug=="}
---
```

每项的 `type` 填写 `betterquesting`，`id` 支持标准 UUID 或紧凑 Base64；`anchor` 可选，指向当前指南页的章节。无效绑定会跳过并记录日志。完整格式见 [外部内容绑定](Guide-Page-Format-zh-CN#外部内容绑定)。

两种任务 ID 编码会归一化为同一个 UUID。相同任务、页面和锚点合并；同一任务仍可绑定到不同页面或章节。

### 从 BetterQuesting 描述链接到 GuideNH 页面

BetterQuesting 任务描述可以通过 `[guide]` 标签跳回 GuideNH 页面。该标签只在 BetterQuesting 已加载时由 GuideNH 解析，并会转成 BetterQuesting 原生的 hyperlink-aware 文本框渲染，因此 BQ 自己的换行、滚动和点击热区仍保持兼容。

目标写页面 ID：

```text
[guide]guidenh:navigation-guide[/guide]
```

如果目标页面存在，显示文本会替换为该指南页面的标题。`.md` 后缀可省略，因此当 `guidenh:navigation-guide.md` 存在时，`guidenh:navigation-guide` 也会指向它。

需要自定义显示文本时使用 `page=`：

```text
[guide page=guidenh:navigation-guide]打开导航指南[/guide]
```

该链接会使用 BetterQuesting 原生链接的蓝色与下划线样式，鼠标悬停时显示 GuideNH tooltip，点击后打开目标 GuideNH 页面。也支持页面锚点：

```text
[guide page=guidenh:navigation-guide#navigation-fields]导航字段[/guide]
```

### `<QuestLink>` 与 `<QuestCard>`

两个标签都通过 `id` 接收 BetterQuesting 任务 id，并在页面挂载时根据玩家进度决定外观：

| 状态 | 来源 | 渲染 |
| --- | --- | --- |
| 可见 | 任务已解锁但未完成 | 普通可点击链接 |
| 完成 | 任务已完成 | 可点击链接，绿色 |
| 锁定 | 任务存在但未解锁，可见性不为 HIDDEN/SECRET | 仍然是可点击的任务链接 / 任务卡片标题，会打开 BetterQuesting 或跳到索引页 |
| 隐藏 | 锁定且可见性为 HIDDEN 或 SECRET | 深灰斜体占位符，不泄露任务信息 |
| 缺失 | 任务 id 在数据库中找不到对应任务 | 红色斜体占位符 |

左键打开 BetterQuesting 任务界面。右键打开绑定的指南页；没有绑定指南时，右键也打开 BetterQuesting。多个页面使用与物品相同的目标选择界面，不同正文锚点保留为独立目标。

属性表与示例参见 [标签参考](Tags-Reference-zh-CN#questlink)。

### 隐藏任务的处理

GuideNH 永不渲染处于 `HIDDEN` 或 `SECRET` 状态且仍处于锁定的任务的标题与描述。占位文本采用翻译键，便于整合包做本地化：

| 翻译键 | 中文默认 |
| --- | --- |
| `guidenh.binding.state.hidden` | `隐藏内容` |
| `guidenh.binding.state.missing` | `找不到内容` |
| `guidenh.compat.bq.open_in_guide` | `在指南中打开` |

锁定但非隐藏的任务，在 `<QuestLink>` 以及 `<QuestCard>` 的可点击标题上仍可显示描述 tooltip，这与 BetterQuesting 自身在锁定任务 tooltip 中暴露描述的行为一致。可通过 `showTooltip={false}`关闭该 tooltip；隐藏任务不会暴露任何 tooltip。

> [!NOTE]
> 解锁或完成任务后重新打开指南，以刷新显示的任务状态。

### “打开指南”快捷键集成

默认打开指南快捷键（`G`，可在 `key.guidenh.open_guide` 中重新绑定）在 BetterQuesting 加载时获得第二种触发路径。当 BetterQuesting 任务图 GUI 打开时：

1. 鼠标悬停在面板中的某个任务按钮上
2. 按住打开指南快捷键

若任意已注册指南通过 `bindings` 索引了该任务 id，GuideNH 将跳转到对应页面（如果指南尚未打开则同时开启）。若没有任何页面索引当前悬停的 id，则快捷键不会执行任何操作 —— 不会回退到打开 BQ 任务 GUI，因为这恰好是 BQ 已经展示的内容。

该路径独立于物品 tooltip 触发流，背包内悬停物品仍按既有的物品 / 矿辞索引流程处理。

### BetterQuesting 缺席时的行为

- `<QuestLink>` 与 `<QuestCard>` 不会注册；使用了它们的页面会以标准“未知标签”错误形式呈现，直到你移除这些标签。
- `bindings` 仍会被索引；原生任务跳转与 BQ 悬停目标不可用。
- BetterQuesting 描述链接仅在安装 BetterQuesting 时可用。

面向可选 BetterQuesting 集成的页面可设置 `navigation.required_mod: betterquesting`，避免未安装该模组时显示未知标签错误。

模组开发者扩展标签及站点导出的说明见 [开发者集成](Developer-Integration-zh-CN)。

## Thaumcraft 与 AspectRecipeIndex

### 使用研究绑定

Thaumcraft 研究绑定是可选集成。GuideNH 提供研究标签、依据完成情况的展示与原生跳转。在研究树节点、研究条目标题或 ARI 的 NEI 研究图标上按住打开指南键（默认 `G`），可打开全部匹配的指南目标。没有绑定时快捷键不执行操作；物品槽继续使用原有物品操作。

### 查看与复制研究 ID

研究 ID 的显示、查询和复制由 AspectRecipeIndex 提供。`ARIConfig.showResearchId` 默认 `false`，仅控制 ID 标签和快捷键提示，关闭时仍可复制。悬停可见的研究树条目、研究页标题或 ARI 的 NEI 研究组件，使用 NEI 已配置的“复制标识符”快捷键（`copy.identifier`）及其 Ctrl 修饰键，剪贴板会得到原始 ID。`showResearchKey` 独立控制 NEI 研究组件是否显示。研究页的复制区域限定为标题，正文物品仍使用原有复制操作。

### 导出研究目录

在 NEI 设置 → 工具 → 数据转储中选择**神秘研究 ID**，切换 CSV 或 JSON 后点击转储。全部注册研究保存至游戏目录下的 `dumps/thaumcraftresearch.csv` 或 `dumps/thaumcraftresearch.json`。UTF-8 目录包含 ID、分类 ID/名称、标题、简介、普通/隐藏前置、兄弟研究、标志和物品/纹理图标，覆盖没有 NEI 配方的研究，不导出玩家进度。关闭 `showResearchId` 时仍可导出。

### 进度与返回导航

研究绑定 ID 区分大小写。重新打开指南可刷新进度；资源或世界变更会使研究目录缓存失效。原生浏览不会解锁或购买研究。从研究条目返回研究树后，按 Escape 返回保留状态的指南。
