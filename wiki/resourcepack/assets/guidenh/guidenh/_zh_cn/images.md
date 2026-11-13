---
navigation:
  title: 图片
  parent: index.md
  position: 145
  icon: minecraft:wool:1
categories:
  - widgets
---

# 图片

随包提供的 `test1.png` 为 **32×32** 像素。裁剪框使用原图像素坐标，图片注解使用裁剪后区域内的像素坐标；显示尺寸决定结果在页面中占多大。

## Markdown 图片

![测试图片](test1.png)

图片也可以嵌在段落中：文字前 ![行内图片](test1.png) 文字后。需要指定尺寸、裁剪或区域 tooltip 时，使用 `<FloatingImage>`。

## FloatingImage

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `src` | 必填 | 相对路径、根路径或 `modid:path` 图片引用 |
| `x`, `y` | 裁剪时必填 | 原图中的裁剪起点，非负整数 |
| `width` / `w`, `height` / `h` | 裁剪时必填 | 正整数裁剪尺寸；每个维度只能选一种名称 |
| `displayWidth`, `displayHeight` | 裁剪尺寸 | 最终正整数显示尺寸；只指定一个维度时保持裁剪区域比例 |
| `scaleX`, `scaleY` | `1` | 裁剪尺寸的正数缩放倍率，不能与显示尺寸同时使用 |
| `wrap` | 浮动 | `inline` 将图片嵌入文字行；其他情况下由 `align` 决定浮动方向 |
| `align` | `left` | `left` 或 `right`，`wrap="inline"` 时忽略 |
| `title`, `alt` | 无 | 悬停标题与替代文本 |
| `sound`, `soundSrc` | 无 | 整图音效事件或音频文件路径 |
| `trigger` | `click` | `click` 或 `hover` |

省略全部裁剪属性并提供 `displayWidth` 或 `displayHeight` 可显示整张图。裁剪时必须同时提供 `x`、`y`、宽、高。

完整图片显示为 128×128：

<FloatingImage src="test1.png" displayWidth="128" wrap="inline" title="32x32 原图显示为 128x128" />

从原图裁剪 16×16 区域，并拉伸到 160×80：

<FloatingImage src="test1.png" x="8" y="8" width="16" height="16" displayWidth="160" displayHeight="80" wrap="inline" title="16x16 裁剪显示为 160x80" />

只设置一个显示尺寸时保持比例：

<FloatingImage src="test1.png" x="0" y="0" width="32" height="16" displayWidth="160" wrap="inline" title="32x16 裁剪显示为 160x80" />

缩放倍率是显示尺寸之外的另一种写法：

文字前 <FloatingImage src="minecraft:textures/gui/options_background.png" x="0" y="0" width="32" height="32" scaleX="0.75" scaleY="0.75" wrap="inline" title="显示为 24x24" /> 文字后。

### ImageAnnotation

`<ImageAnnotation>` 是 `<FloatingImage>` 的子标签，正文作为富文本 tooltip。坐标以**裁剪后的图片像素**计，显示尺寸改变时会自动跟随缩放。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `x`, `y` | `0` | 区域起点；省略全部四个坐标时覆盖整图 |
| `w`, `h` | `1` | 存在任意坐标时使用的区域宽高 |
| `border` | `false` | 启用彩色边框 |
| `borderColor` | 随机不透明颜色 | `#RRGGBB` 或 `#AARRGGBB` |
| `borderThickness` | `1` | 边框宽度，以显示像素计 |
| `sound`, `src` | 无 | 区域音效事件或音频文件路径 |
| `trigger` | `click` | `click` 或 `hover` |

悬停图片任意位置：

<FloatingImage src="test1.png" displayWidth="128" wrap="inline">
  <ImageAnnotation>
    此注解覆盖**整张图片**。
  </ImageAnnotation>
</FloatingImage>

两个区域分别覆盖 32×32 原图的左右半边：

<FloatingImage src="test1.png" x="0" y="0" width="32" height="32" displayWidth="160" displayHeight="80" wrap="inline">
  <ImageAnnotation x="0" y="0" w="16" h="32" border borderColor="#FF44FF44" borderThickness="2">
    左半边，随图片一起缩放。
  </ImageAnnotation>
  <ImageAnnotation x="16" y="0" w="16" h="32" border borderColor="#FF4444FF" borderThickness="2">
    右半边，随图片一起缩放。
  </ImageAnnotation>
</FloatingImage>

### 图片音效

音效事件在 `assets/guidenh/sounds.json` 中声明；需在 `assets/guidenh/sounds/` 中提供对应音频文件才能听到声音。区域重叠时，靠后的区域优先。

&[行内音效动作](sound:guidenh:guide.sample_click)

<SoundLink sound="guidenh:guide.sample_click" volume="0.8">**播放示例音效**</SoundLink>

<FloatingImage src="test1.png" displayWidth="128" wrap="inline" sound="guidenh:guide.sample_click">
  <SoundArea x="0" y="0" w="16" h="32" sound="guidenh:guide.sample_left" />
  <SoundArea x="16" y="0" w="16" h="32" sound="guidenh:guide.sample_hover" trigger="hover" />
  <ImageAnnotation x="4" y="4" w="8" h="8" border borderColor="#FFFFCC44" sound="guidenh:guide.sample_click">
    同一区域同时带有 tooltip、边框和点击音效。
  </ImageAnnotation>
</FloatingImage>

## ItemImage

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `id` | 必填 | 物品 ID，可附元数据与内联 SNBT |
| `ore` | 无 | 矿物词典选择器，优先于同时提供的 `id` |
| `nbt` | 无 | 物品堆叠 SNBT，覆盖内联 SNBT 中的同名键 |
| `scale` | `1` | 图标缩放倍率 |
| `showTooltip` | `true` | 显示物品 tooltip |
| `noTooltip` | `false` | 兼容的旧开关，推荐使用 `showTooltip` |
| `showIcon` | `true` | 显示图标 |
| `label` | 无 | 在图标 `left` 或 `right` 侧显示物品名 |
| `format` | 斜体物品名 | 标签格式，`%s` 代入物品名 |
| `yOffset` | `0` | 行内物品及标签整体的垂直偏移，以 `scale=1` 的像素计 |
| `labelYOffset` | `0` | 仅标签文字的额外垂直偏移，以 `scale=1` 的像素计 |

偏移随图标倍率缩放，正值向下。自定义富文本 tooltip 应使用 `<Tooltip label="...">` 及其子内容，示例见 [Tooltip](./tooltips.md)。

### 缩放与 NBT

<Row>
  <ItemImage id="minecraft:diamond" scale="1" />
  <ItemImage id="minecraft:diamond" scale="2" />
  <ItemImage id="minecraft:diamond" scale="3" />
  <ItemImage id="minecraft:diamond" scale="4" />
</Row>

<ItemImage id="minecraft:diamond" nbt='{display:{Name:"自定义钻石"}}' />

<ItemImage id="minecraft:iron_ingot" ore="ingotIron" label="right" />

### 标签与对齐

- 默认偏移：<ItemImage id="minecraft:diamond" label="right" />
- 整体向下：<ItemImage id="minecraft:diamond" label="right" yOffset="2" />
- 仅标签向上：<ItemImage id="minecraft:diamond" label="right" labelYOffset="-2" />
- 名称位于左侧：<ItemImage id="minecraft:iron_ingot" label="left" />
- 粗体名称：<ItemImage id="minecraft:gold_ingot" label="right" format="**%s**" />
- 删除线：<ItemImage id="minecraft:rotten_flesh" label="right" format="~~%s~~" />
- 下划线：<ItemImage id="minecraft:emerald" label="right" format="__%s__" />
- 波浪下划线：<ItemImage id="minecraft:blaze_rod" label="right" format="^^%s^^" />
- 点状下划线与固定文字：<ItemImage id="minecraft:ender_pearl" label="right" format="::自定义标签::" />
- 只显示标签：<ItemImage id="minecraft:diamond" showIcon={false} label="right" />
- 关闭物品 tooltip：<ItemImage id="minecraft:emerald" label="right" showTooltip={false} />

## BlockImage

`<BlockImage>` 以透明背景显示放置在世界中的方块，保留方块名称 tooltip 和悬停描边。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `id` / `ore` | 至少一个 | 方块引用或矿物词典选择器 |
| `meta` | 来自 `id` | 显式覆盖方块元数据 |
| `nbt` | 无 | TileEntity SNBT，覆盖内联 SNBT 中的同名键 |
| `scale` | `4` | 相机缩放倍率 |
| `width`, `height` | 分别为 `128` | 预览视口像素尺寸 |
| `perspective` | `isometric-north-east` | `isometric-north-east`、`isometric-north-west` 或 `up` |
| `wrap` | `inline` | 块级内容环绕模式 |
| `align` | `left` | 块级上下文中的 `left`、`center` 或 `right` |
| `float` | 无 | 流式上下文兼容的 `left` / `right`，推荐使用 `wrap` 和 `align` |

<Row>
  <BlockImage id="minecraft:stone" scale="2" />
  <BlockImage id="minecraft:stone" />
  <BlockImage id="minecraft:stone" scale="6" />
</Row>

<Row>
  <BlockImage id="minecraft:furnace" scale="2.5" perspective="isometric-north-east" />
  <BlockImage id="minecraft:furnace" scale="2.5" perspective="isometric-north-west" />
  <BlockImage id="minecraft:chest" scale="2.5" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
</Row>

箱子使用 **TileEntity** NBT；`ItemImage nbt` 使用**物品堆叠** NBT。更多布局示例见[内容嵌入](./content-embed.md)。

## ItemLink

纯文字：<ItemLink id="minecraft:chest" />

左侧图标：<ItemLink id="minecraft:chest" showIcon="left" />

右侧图标并关闭 tooltip：<ItemLink id="minecraft:diamond" showIcon="right" showTooltip={false} />

矿物词典查询：<ItemLink ore="stickWood" />

跳转其他页面：<ItemLink id="minecraft:diamond" linksTo="./markdown.md#标题" />

页内锚点：<ItemLink id="minecraft:diamond" linksTo="#itemlink" />
