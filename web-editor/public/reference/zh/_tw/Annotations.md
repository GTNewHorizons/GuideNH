[English](Annotations)

# 註解

GuideNH 的場景註解是放在遊戲場景（`<GameScene>` / `<Scene>`）內部的子標籤。它們在世界空間中渲染，並且可以包含子 Markdown/標籤內容，後者會成為富 tooltip。

## 通用规则

- 註解只能在場景內部使用
- 子內容會變成 tooltip 正文
- 註解可透過場景 UI 開關隱藏
- `alwaysOnTop` 會在對應註解類型支持時，讓註解繪製在場景幾何體之上
- 當場景使用 `<ImportStructureLib>` 時，所有場景註解也支援 `showWhenStructure`、`showWhenTier` 和 `showWhenChannels`

## 支持的注解标签

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<TextAnnotation>`

GuideNH 也支援 `<BlockAnnotationTemplate>`，它會把自己的子註解應用到目前場景裡所有已經存在且符合的方塊上。

## StructureLib 条件显示

當場景中存在 `<ImportStructureLib>` 時，每一種註解標籤都可以限制自己只在特定的 StructureLib 狀態下顯示：

| 屬性 | 意義 |
| --- | --- |
| `showWhenStructure` | 綁定到具名的 `<ImportStructureLib name="...">`；如果場景裡只有一個 StructureLib 導入，可以省略 |
| `showWhenTier` | tier 過濾，例如 `2`、`1..3`、`!2`、`1..5,!3` |
| `showWhenChannels` | 按 channel 過濾，例如 `input:1..3, casing:!2, fluid:4` |

規則：

- `showWhenTier` 與 `showWhenChannels` 按 AND 組合
- `showWhenChannels` 可以同時在一個屬性中宣告多個 channel
- 像 `!2` 這樣的純反選表示“除了 2 以外都匹配”
- 同樣的屬性也支持 `<PlaySound>` 和 `<BlockAnnotationTemplate>` 的子註解

範例：

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

高亮一個 `1x1x1` 方塊體積。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `pos` | 是 | `x y z` 向量 |
| `color` | 否 | `#RRGGBB`、`#AARRGGBB` 或 `transparent` |
| `thickness` | 否 | 線寬，float |
| `alwaysOnTop` | 否 | boolean expression |

範例：

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  凸顯控制器方塊。
</BlockAnnotation>
````

## `<BoxAnnotation>`

高亮任意軸對齊包圍盒。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `min` | 是 | `x y z` 最小向量 |
| `max` | 是 | `x y z` 最大向量 |
| `color` | 否 | 註解顏色 |
| `thickness` | 否 | 線寬，float |
| `alwaysOnTop` | 否 | boolean expression |

GuideNH 會在每個軸上自動交換反向提供的 min/max 座標。

範例：

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  半高區域凸顯。
</BoxAnnotation>
````

## `<LineAnnotation>`

在世界空間中繪製一條線段或多段折線。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `from` | 是，除非設定了 `points` | `x y z` 起點向量 |
| `to` | 是，除非設定了 `points` | `x y z` 終點向量 |
| `points` | 否 | 以分號分隔的多個 `x y z` 點，用於折線；設定後會覆蓋 `from` / `to` |
| `color` | 否 | 註解顏色 |
| `thickness` | 否 | 線寬，float |
| `alwaysOnTop` | 否 | boolean expression |
| `arrow` | 否 | `start` 或 `end`；省略時不顯示箭頭 |
| `showPoints` | 否 | boolean expression；顯示所有點為小立方體 |
| `pointColor` | 否 | 預設點顏色；省略時使用線顏色 |
| `pointSize` | 否 | 預設點大小；省略時比 `thickness` 稍粗 |

`LineAnnotation` 可以包含 `<LinePoint>` 子標籤，用於覆蓋單點的顯示樣式。
`LinePoint` 支援 `index`、可選的 `show`、可選的 `color` 和可選的 `size`。點編號從 0 開始。
箭頭只能放在起點或終點；折線中間點不能設定箭頭。

範例：

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  訊號路徑。
</LineAnnotation>
````

帶有 3D 端點箭頭和單獨點標記的折線：

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  穿過彎折處的訊號路徑。
</LineAnnotation>
````

## `<DiamondAnnotation>`

在世界座標位置放置一個面向螢幕的菱形標記。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `pos` | 是 | `x y z` 標示位置 |
| `color` | 否 | 著色；省略時預設亮綠色 |

範例：

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<TextAnnotation>`

繪製場景中的氣泡文字標籤。它可以跟隨一個世界座標錨點，也可以固定在場景中心附近的螢幕座標上。與其他註解不同，它的子內容會作為氣泡正文，而不是懸停 tooltip。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `pos` | 否 | `x y z` 世界座標錨點 |
| `x`, `y`, `z` | 否 | 未提供 `pos` 時可用的獨立座標分量 |
| `text` | 否 | 氣泡文字；省略時使用子 Markdown 內容 |
| `textKey` | 否 | 優先從資源包 `lang` 檔案解析的翻譯鍵；解析失敗時回退到 `text` 或子 Markdown 內容 |
| `color` | 否 | 氣泡邊框顏色，預設為淺灰色 |
| `backgroundAlpha` | 否 | 背景透明度，範圍 `0` 到 `255`，預設 `204` |
| `maxWidth` | 否 | 像素換行寬度；`0` 表示單行 |
| `independent` | 否 | `true` 時固定在螢幕空間 |
| `yOffset` | 否 | `independent={true}` 时相对场景中心的垂直像素偏移 |
| `connectorSide` | 否 | `bottom`、`top`、`left`、`right` 或 `none`；默认 `bottom` |
| `connectorOffset` | 否 | 沿氣泡邊緣偏移連接點；top/bottom 正值向右，left/right 正值向下 |
| `connectorLength` | 否 | 連接線像素長度；預設 `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | 否 | 選用伴生高亮框範圍 |
| `highlightColor` | 否 | 可選高亮框顏色 |

世界錨定的氣泡會繪製一條連接線指向錨點。可用 `connectorSide` 控制連接線接在氣泡的哪一條邊，用 `connectorOffset` 沿邊移動連接點，用 `connectorLength` 調整氣泡與錨點之間的距離。獨立氣泡會水平居中，並使用 `yOffset` 控制垂直位置，且不會繪製連接線。導入思索時間軸的 `text` 註解時也會使用同一個執行時間註解。

範例：

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

固定螢幕座標範例：

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  独立状态文本
</TextAnnotation>
````

## 富 Tooltip 內容

註解的子內容會以普通 GuideNH 內容編譯，因此 tooltip 內可以包含：

- Markdown 段落和標題
- 物品/方塊圖片
- 配方
- 嵌套的非互動場景

範例：

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

當你希望把同一種註解蓋到所有匹配方塊上時使用它。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `id` | 是 | 方塊匹配器，格式為 `modid:block[:meta]` |

規則：

- 模板只會看到它被解析時場景裡已經存在的方塊
- 應放在 `<Block>`、`<ImportStructure>` 或 `<ImportStructureLib>` 之後
- 子註解使用相對於每個匹配方塊的局部座標

範例：

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      範本產生的工具提示
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## 相關頁面

- [遊戲場景](GameScene)
- [範例](Examples)
