[English](Images-And-Assets)

# 图片与资源

GuideNH 同时支持普通 Markdown 图片，以及若干运行时专用的视觉元素。

## 资源解析规则

指南资源使用与页面链接相同的解析规则。

| 路径形式 | 示例 | 含义 |
| --- | --- | --- |
| 相对路径 | `test1.png` | 相对于当前页面文件 |
| 根路径 | `/assets/example_structure.snbt` | 相对于当前指南根目录 |
| 显式资源 id | `guidenh:textures/gui/example.png` | 绝对 `modid:path` 查找 |

## Markdown 图片

支持普通 Markdown 图片：

````md
![Example](test1.png)
````

GuideNH 会解析路径，并从指南内容根目录加载对应的二进制资源。

## `FloatingImage`

`<FloatingImage>` 用于渲染“先裁剪、再缩放”的位图区域，既可以配合文字浮动，也可以真正作为段落内联内容使用。它还支持显式 `modid:path` 纹理 id，因此可以直接引用其他模组的纹理资源。

### 属性

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `src` | 是 | 图片路径 |
| `x` | 是 | 原图裁剪起始 X，单位为源图像像素 |
| `y` | 是 | 原图裁剪起始 Y，单位为源图像像素 |
| `width` / `w` | 是 | 原图裁剪宽度，单位为源图像像素；两种写法只能二选一 |
| `height` / `h` | 是 | 原图裁剪高度，单位为源图像像素；两种写法只能二选一 |
| `scaleX` | 否 | 水平显示缩放倍率，默认 `1.0` |
| `scaleY` | 否 | 垂直显示缩放倍率，默认 `1.0` |
| `displayWidth` | 否 | 最终显示宽度，单位为像素；单独使用时按裁剪区域比例计算高度 |
| `displayHeight` | 否 | 最终显示高度，单位为像素；单独使用时按裁剪区域比例计算宽度 |
| `wrap` | 否 | `inline` 表示真正行内放置，其他值使用常规环绕模式 |
| `align` | 否 | 浮动时使用 `left` 或 `right`；`wrap="inline"` 时会被忽略 |
| `title` | 否 | tooltip/title 文本 |
| `sound` | 否 | 整张图片点击时播放的音效事件 |
| `soundSrc` | 否 | 整张图片使用的音效文件路径 |
| `trigger` | 否 | 默认 `click`，也可以写 `hover` 以悬停播放 |

### 说明

- 裁剪时必须同时提供 `x`、`y`、`width` / `w` 与 `height` / `h`
- 全部省略裁剪属性时，可使用 `displayWidth` 或 `displayHeight` 显示整张源图
- `width` 和 `height` 现在表示裁剪区域，不再表示最终显示尺寸
- `scaleX` 与 `scaleY` 会将最终显示尺寸计算为 `cropWidth * scaleX` 与 `cropHeight * scaleY`
- `displayWidth` 或 `displayHeight` 以像素指定最终显示尺寸；只提供其中一个时，另一个尺寸按裁剪区域宽高比自动计算
- 同时提供 `displayWidth` 与 `displayHeight` 时，可按指定尺寸进行非等比拉伸
- `displayWidth` / `displayHeight` 不能与 `scaleX` / `scaleY` 同时使用
- 支持单轴拉伸，只修改一个缩放值即可
- 同时写 `width` 和 `w`，或同时写 `height` 和 `h`，都会渲染可见错误
- 旧版把 `width` / `height` 当作显示尺寸的 `<FloatingImage>` 内容会发生破坏性变更，需要手动迁移
- `src` 可以是相对路径、根路径，或显式 `modid:path` 纹理 id，例如 `minecraft:textures/gui/options_background.png`

### 示例

````md
<FloatingImage
  src="minecraft:textures/gui/options_background.png"
  x="0"
  y="0"
  width="32"
  height="32"
  displayWidth="64"
  displayHeight="64"
  wrap="inline"
  title="Example"
/>
````

## `ImageAnnotation`

`<ImageAnnotation>` 是 `<FloatingImage>` 的子标签，用于为图片的矩形区域附加富文本 tooltip（以及可选的彩色边框）。坐标以**裁剪后子图像像素**为单位，当裁剪后的图像被缩放或拉伸时会自动等比例调整。

### 属性

| 属性 | 必需 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `x` | 否 | — | 区域左边缘（图片像素） |
| `y` | 否 | — | 区域上边缘（图片像素） |
| `w` | 否 | — | 区域宽度（图片像素） |
| `h` | 否 | — | 区域高度（图片像素） |
| `border` | 否 | `false` | 是否显示彩色边框 |
| `borderColor` | 否 | 随机 | 边框颜色（`#RRGGBB` 或 `#RRGGBBAA`） |
| `borderThickness` | 否 | `1` | 边框粗细（显示像素） |
| `sound` | 否 | 无 | 此区域播放的可选音效事件 |
| `src` | 否 | 无 | 可选音效文件路径，会转换为音效事件 id |
| `trigger` | 否 | `click` | `click` 或 `hover` |

### 说明

- 同时省略 `x`、`y`、`w`、`h` 时，注解覆盖**整张图片**
- 若任一坐标存在，省略的坐标默认为 `0`（原点）或 `1`（尺寸）
- 默认**不显示**边框；添加 `border` 或 `border={true}` 属性来启用
- 启用边框但未指定 `borderColor` 时，自动生成随机不透明颜色
- 子 MDX 内容作为 tooltip 正文渲染，支持任意内联/块级元素
- 多个注解区域重叠时，列表中靠后的注解（覆盖在上方）优先响应悬停

### 示例

整图注解：

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation>
    鼠标悬停在图片任意位置都会显示此 tooltip。
  </ImageAnnotation>
</FloatingImage>
````

带可见边框的区域注解：

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FF4444FF" borderThickness="2">
    悬停在**红框区域**内显示此 tooltip。
  </ImageAnnotation>
</FloatingImage>
````

同一图片上的多个注解：

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#44FF44FF">
    左半部分
  </ImageAnnotation>
  <ImageAnnotation x="64" y="0" w="64" h="64" border borderColor="#4444FFFF">
    右半部分
  </ImageAnnotation>
</FloatingImage>
````

图片区域也可以播放音效。只需要音效时可以使用 `<SoundArea>`；如果同一个区域还需要
tooltip 或边框，也可以直接在 `<ImageAnnotation>` 上写 `sound`。

````md
<FloatingImage
  src="test1.png"
  align="left"
  x="0"
  y="0"
  width="128"
  height="128"
  sound="guidenh:image.click"
>
  <SoundArea x="0" y="0" w="64" h="64" sound="guidenh:image.left" />
  <SoundArea x="64" y="0" w="64" h="64" sound="guidenh:image.right" trigger="hover" />
  <ImageAnnotation x="10" y="10" w="40" h="40" border sound="guidenh:image.note">
    这个区域同时拥有 tooltip 内容和点击音效。
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` 会覆盖整张图片。区域音效使用裁剪后子图像坐标，
并遵循与 tooltip 相同的重叠优先级：靠后的区域优先响应。

## 内容嵌入与文字环绕

所有块级标签——`<FloatingImage>`、`<Recipe>`、`<GameScene>`、`<ItemImage>`、`<BlockImage>` 以及
其他基于 `BlockTagCompiler` 的标签——均支持两个可选的布局属性，提供类似 Word 的内容嵌入功能。

| 属性 | 可选值 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | 文字环绕模式 |
| `align` | `left` · `center` · `right` | `left` | 水平对齐方式 |

### 环绕模式

| 模式 | Word 对应 | 块级上下文效果 | 流式上下文效果 |
| --- | --- | --- | --- |
| `inline` | 嵌入型 | 默认堆叠（嵌入型） | 嵌入在文字行中 |
| `square` | 方形 | 文档级浮动，文字在方形框内环绕（方形环绕） | `FLOAT_LEFT` / `FLOAT_RIGHT` |
| `tight` | 紧密型 | 等同 `square`（紧密型） | 等同 `square` |
| `through` | 穿越型 | 等同 `square`（穿越型） | 等同 `square` |
| `top-bottom` | 上下型 | 占满整行宽度；`align` 控制水平位置（上下型） | 行内嵌入（含换行） |
| `behind` | 衬于文字下方 | 对齐的行内槽；渲染在文字下方（衬于文字下方） | 嵌入在文字行中 |
| `front` | 浮于文字上方 | 对齐的行内槽；渲染在文字上方（浮于文字上方） | 嵌入在文字行中 |

### 浮动模式下的对齐

对于 `wrap=square/tight/through`：
- `align=left`（默认）——向**左**浮动，文字填充右侧区域。
- `align=right`——向**右**浮动，文字填充左侧区域。
- `align=center`——居中对齐（无文字环绕）。

### 示例

使用新 `wrap` 属性的左浮动图片：

````md
<FloatingImage
  src="test1.png"
  wrap="square"
  align="left"
  x="0"
  y="0"
  width="128"
  height="128"
  scaleX="0.5"
  scaleY="0.5"
/>

此段落文字将流向图片右侧……
````

右浮动配方：

````md
<Recipe id="minecraft:stone" wrap="square" align="right" />

此处文字将流向配方框左侧……
````

居中物品图标（无文字环绕）：

````md
<ItemImage id="minecraft:diamond" align="center" />
````

右对齐物品图标：

````md
<ItemImage id="minecraft:diamond" align="right" />
````

物品 NBT 可以单独写在 `nbt` 属性中。`id` 中的内联 SNBT 仍然支持；两种写法同时存在时，
独立的 `nbt` 属性最后合并。

````md
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"自定义钻石"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

> **注意** — `wrap="inline"` 现在会让 `<FloatingImage>` 真正作为行内内容放置。
> 在 inline 模式下，`align` 会被忽略，而不是报错。

## 导航纹理图标

frontmatter 可以使用 `icon_texture`，在导航/搜索中显示纹理而不是物品：

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

该文件必须能被解码为图片。路径的解析规则与其他指南资源路径完全一致。

## 非图片资源

GuideNH 页面也可以引用非图片类运行时资源，最常见的是结构文件，例如：

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

这些资源会通过同一套指南资源管线加载，但不是直接作为图片渲染，而是交由自定义标签消费。

## 最佳实践

- 页面私有图片尽量放在使用它们的页面旁边
- 可复用文件尽量放在指南根的 `assets/` 目录中
- 多页面共享文件优先使用根路径 `/assets/...`
- 只有在资源确实是图片时才使用纹理图标

## `BlockImage`

`<BlockImage>` 也遵循与 `<FloatingImage>` 相同的块级嵌入规则，但它显示的是透明背景的 3D
单方块预览，而不是位图图片。它适合在正常正文里直接展示“方块被放在世界里时”的样子。

关键行为：

- 背景透明，边框透明
- 没有场景按钮，没有 layer 滑条，也没有注解编辑面
- 悬停时依然会显示方块选中线框与 tooltip
- `scale` 控制相机缩放
- `perspective` 支持 `isometric-north-east`、`isometric-north-west`、`up`
- `nbt` 用于提供 TileEntity SNBT；`id="mod:block:meta:{...}"` 形式的内联 SNBT 仍然可用，
  但独立 `nbt` 属性更清晰，推荐优先使用

示例：

````md
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## 运行时示例文件

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## 相关页面

- [指南页面格式](Guide-Page-Format-zh-CN)
- [标签参考](Tags-Reference-zh-CN)
- [游戏场景](GameScene-zh-CN)
