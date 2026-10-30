[English](Annotations)

# 注解

GuideNH 的场景注解是放在游戏场景（`<GameScene>` / `<Scene>`）内部的子标签。它们在世界空间中渲染，并且可以包含子 Markdown/标签内容，后者会成为富 tooltip。

## 通用规则

- 注解只能在场景内部使用
- 子内容会成为 tooltip 正文
- 注解可通过场景 UI 开关隐藏
- `alwaysOnTop` 会在对应注解类型支持时，让注解绘制在场景几何体之上
- 当场景使用 `<ImportStructureLib>` 时，所有场景注解还支持 `showWhenStructure`、`showWhenTier` 和 `showWhenChannels`

## 支持的注解标签

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<OutlineAnnotation>`
- `<TextAnnotation>`

GuideNH 还支持 `<BlockAnnotationTemplate>`，它会把自己的子注解应用到当前场景里所有已经存在且匹配的方块上。

## StructureLib 条件显示

当场景中存在 `<ImportStructureLib>` 时，每一种注解标签都可以限制自己只在特定的 StructureLib 状态下显示：

| 属性 | 含义 |
| --- | --- |
| `showWhenStructure` | 绑定到具名的 `<ImportStructureLib name="...">`；如果场景里只有一个 StructureLib 导入，可以省略 |
| `showWhenTier` | tier 过滤，例如 `2`、`1..3`、`!2`、`1..5,!3` |
| `showWhenChannels` | 按 channel 过滤，例如 `input:1..3, casing:!2, fluid:4` |

规则：

- `showWhenTier` 与 `showWhenChannels` 按 AND 组合
- `showWhenChannels` 可以在一个属性里同时声明多个 channel
- 像 `!2` 这样的纯反选表示“除了 2 以外都匹配”
- 同样的属性也支持 `<PlaySound>` 和 `<BlockAnnotationTemplate>` 的子注解

示例：

````md
<GameScene interactive={true}>
  <ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />

  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="2..4,!3"
    showWhenChannels="input:1..3, casing:!2"
  >
    只会在匹配的 StructureLib 状态下显示。
  </BlockAnnotation>
</GameScene>
````

## `<BlockAnnotation>`

高亮一个 `1x1x1` 方块体积。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `pos` | 是 | `x y z` 向量 |
| `color` | 否 | `#RRGGBB`、`#AARRGGBB` 或 `transparent` |
| `thickness` | 否 | 线宽，float |
| `alwaysOnTop` | 否 | boolean expression |

示例：

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Highlights the controller block.
</BlockAnnotation>
````

## `<BoxAnnotation>`

高亮任意轴对齐包围盒。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `min` | 是 | `x y z` 最小向量 |
| `max` | 是 | `x y z` 最大向量 |
| `color` | 否 | 注解颜色 |
| `thickness` | 否 | 线宽，float |
| `alwaysOnTop` | 否 | boolean expression |

GuideNH 会在每个轴上自动交换反向提供的 min/max 坐标。

示例：

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Half-height area highlight.
</BoxAnnotation>
````

## `<LineAnnotation>`

在世界空间中绘制一条线段或多段折线。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `from` | 是，除非设置了 `points` | `x y z` 起点向量 |
| `to` | 是，除非设置了 `points` | `x y z` 终点向量 |
| `points` | 否 | 用分号分隔的多个 `x y z` 点，用于折线；设置后会覆盖 `from` / `to` |
| `color` | 否 | 注解颜色 |
| `thickness` | 否 | 线宽，float |
| `alwaysOnTop` | 否 | boolean expression |
| `arrow` | 否 | `start` 或 `end`；省略时不显示箭头 |
| `showPoints` | 否 | boolean expression；把所有点显示为小立方体 |
| `pointColor` | 否 | 默认点颜色；省略时使用线颜色 |
| `pointSize` | 否 | 默认点大小；省略时比 `thickness` 稍粗 |

`LineAnnotation` 可以包含 `<LinePoint>` 子标签，用于覆盖单个点的显示样式。
`LinePoint` 支持 `index`、可选的 `show`、可选的 `color` 和可选的 `size`。点编号从 0 开始。
箭头只能放在起点或终点；折线中间点不能设置箭头。

示例：

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Signal path.
</LineAnnotation>
````

带 3D 端点箭头和单独点标记的折线：

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  Signal path through a bend.
</LineAnnotation>
````

## `<DiamondAnnotation>`

在世界坐标位置放置一个面向屏幕的菱形标记。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `pos` | 是 | `x y z` 标记位置 |
| `color` | 否 | 着色；省略时默认亮绿色 |

示例：

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<OutlineAnnotation>`

在方块和实体周围绘制类似发光效果的轮廓描边。轮廓贴合每个目标实际渲染出的形状，即使被前方的方块或实体遮挡也依然可见，相互接触的轮廓会合并成一个整体。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `pos` | 否 | 要描边的方块 `x y z`；默认 `0 0 0`，在 `<BlockAnnotationTemplate>` 内即为匹配到的方块 |
| `min` / `max` | 否 | 区域的方块角坐标（包含两端），区域内的每个方块都会被描边 |
| `entity` | 否 | 要描边的 `<Entity>` 的 `sceneEntityId` |
| `target` | 否 | `blocks`（默认）、`entities` 或 `all`：区域内描边哪些内容；没有区域时 `entities` 会描边场景中的所有实体 |
| `mode` | 否 | `auto`（默认）、`geometry`、`cutout` 或 `bounds` |
| `color` | 否 | 轮廓颜色；省略时为白色，实体则使用其计分板队伍颜色 |
| `thickness` | 否 | 轮廓宽度，单位为 GUI 像素，范围 `0.25` 到 `8`，默认 `1` |
| `alwaysOnTop` | 否 | boolean expression；被遮挡的部分保持完整亮度而不会变暗 |
| `pulse` | 否 | boolean expression；开启呼吸灯效果 |
| `pulsePeriod` | 否 | 呼吸周期，单位秒，默认 `2` |

轮廓模式：

- `auto` 按目标自动选择。封闭模型（完整方块、玻璃、冰、流体、玻璃板、多部件与线缆方块、OBJ 和 TESR 模型）使用几何体轮廓，因此玻璃这类透明纹理会描出整个方块的立方体轮廓，而不是玻璃边框的纹理边界。贴图式渲染（花、树苗、作物、火把、铁轨、梯子、藤蔓、红石）和实体使用纹理透明度，轮廓贴合画出的形状而不是四边形面片。不渲染任何内容的方块使用其选择框。
- `geometry` 始终使用渲染出的几何体。
- `cutout` 始终使用纹理透明度。
- `bounds` 描边方块的选择框和实体的碰撞箱。

规则：

- 只在轮廓外侧绘制描边，不会给目标本身着色
- 被其他方块或实体遮挡的部分依然可见，并像其他被遮挡的注解一样变暗；`alwaysOnTop` 会让它们保持完整亮度
- 隐身的实体同样会被描边
- 相互接触或重叠的轮廓会合并为一条轮廓线，不同注解之间也会合并
- 半透明颜色和柔和边缘会与场景正确混合，边缘不会变黑
- 子内容会成为 tooltip；悬停任意被描边的方块或实体即可显示
- 在 `<BlockAnnotationTemplate>` 内，`pos` 相对于每个匹配到的方块，所有匹配方块的轮廓会合并成一个整体
- 导出的网站会用 WebGL 绘制相同的轮廓

示例：

````md
<GameScene zoom={3} interactive={true}>
  <Block id="minecraft:glass" />
  <Block id="minecraft:red_flower" x="2" />
  <Entity id="minecraft:sheep" sceneEntityId="sheep" x="1.5" z="2.5" />
  <OutlineAnnotation pos="0 0 0" color="#55FFFF">
    玻璃会被描成完整的立方体轮廓。
  </OutlineAnnotation>
  <OutlineAnnotation pos="2 0 0" color="#FFFF55" thickness="1.5" />
  <OutlineAnnotation entity="sheep" color="#FFAA00" alwaysOnTop={true} pulse={true} />
</GameScene>
````

描边结构中所有匹配的方块：

````md
<BlockAnnotationTemplate id="minecraft:glass">
  <OutlineAnnotation color="#AAFFFF" />
</BlockAnnotationTemplate>
````

## `<TextAnnotation>`

绘制场景中的气泡文本标签。它可以跟随一个世界坐标锚点，也可以固定在场景中心附近的屏幕坐标上。与其他注解不同，它的子内容会作为气泡正文，而不是悬停 tooltip。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `pos` | 否 | `x y z` 世界坐标锚点 |
| `x`, `y`, `z` | 否 | 未提供 `pos` 时可用的独立坐标分量 |
| `text` | 否 | 气泡文本；省略时使用子 Markdown 内容 |
| `textKey` | 否 | 优先从资源包 `lang` 文件解析的翻译键；解析失败时回退到 `text` 或子 Markdown 内容 |
| `color` | 否 | 气泡边框颜色，默认浅灰色 |
| `backgroundAlpha` | 否 | 背景透明度，范围 `0` 到 `255`，默认 `204` |
| `maxWidth` | 否 | 像素换行宽度；`0` 表示单行 |
| `independent` | 否 | `true` 时固定在屏幕空间 |
| `yOffset` | 否 | `independent={true}` 时相对场景中心的垂直像素偏移 |
| `connectorSide` | 否 | `bottom`、`top`、`left`、`right` 或 `none`；默认 `bottom` |
| `connectorOffset` | 否 | 沿气泡边缘偏移连接点；top/bottom 正值向右，left/right 正值向下 |
| `connectorLength` | 否 | 连接线像素长度；默认 `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | 否 | 可选伴生高亮框范围 |
| `highlightColor` | 否 | 可选高亮框颜色 |

世界锚定的气泡会绘制一条连接线指向锚点。可以用 `connectorSide` 控制连接线接在气泡的哪条边，用 `connectorOffset` 沿边移动连接点，用 `connectorLength` 调整气泡与锚点之间的距离。独立气泡会水平居中，并使用 `yOffset` 控制垂直位置，且不会绘制连接线。导入思索时间线的 `text` 注解时也会使用同一个运行时注解。

示例：

````md
<TextAnnotation
  pos="1.5 2 1.5"
  textKey="guidenh.sample.scene.insert_items"
  color="#FF44AAFF"
  maxWidth={120}
  backgroundAlpha={180}
  connectorSide="right"
  connectorOffset={8}
  connectorLength={12}
>
  在这里放入**优先级**物品。
</TextAnnotation>
````

固定屏幕坐标示例：

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  独立状态文本
</TextAnnotation>
````

## 富 Tooltip 内容

注解的子内容会按普通 GuideNH 内容编译，因此 tooltip 内可以包含：

- Markdown 段落和标题
- 物品/方块图片
- 配方
- 嵌套的非交互场景

示例：

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

当你希望把同一种注解盖到所有匹配方块上时使用它。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 方块匹配器，格式为 `modid:block[:meta]` |

规则：

- 模板只会看到它被解析时场景里已经存在的方块
- 应放在 `<Block>`、`<ImportStructure>` 或 `<ImportStructureLib>` 之后
- 子注解使用相对于每个匹配方块的局部坐标

示例：

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Template-generated tooltip
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## 相关页面

- [游戏场景](GameScene-zh-CN)
- [示例](Examples-zh-CN)
