# 註解

GuideNH 的場景註解是放在遊戲場景（`<GameScene>` / `<Scene>`）內部的子標籤。它們在世界空間中渲染，並且可以包含子 Markdown/標籤內容，後者會成為富 tooltip。

## 通用規則

- 註解只能在場景內部使用
- 子內容會成為 tooltip 正文
- 註解可透過場景 UI 開關隱藏
- `alwaysOnTop` 會在對應註解型別支援時，讓註解繪製在場景幾何體之上
- 當場景使用 `<ImportStructureLib>` 時，所有場景註解還支援 `showWhenStructure`、`showWhenTier` 和 `showWhenChannels`
- 遊戲和 Site Export 使用相同的彩虹漸變規則與迴圈週期。

## 支援的註解標籤

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<OutlineAnnotation>`
- `<TextAnnotation>`
- `<InputAnnotation>`

GuideNH 還支援 `<BlockAnnotationTemplate>`，它會把自己的子註解應用到當前場景裡所有已經存在且匹配的方塊上。

## StructureLib 條件顯示

當場景中存在 `<ImportStructureLib>` 時，每一種註解標籤都可以限制自己只在特定的 StructureLib 狀態下顯示：

| 屬性 | 含義 |
| --- | --- |
| `showWhenStructure` | 繫結到具名的 `<ImportStructureLib name="...">`；如果場景裡只有一個 StructureLib 匯入，可以省略 |
| `showWhenTier` | tier 過濾，例如 `2`、`1..3`、`!2`、`1..5,!3` |
| `showWhenChannels` | 按 channel 過濾，例如 `input:1..3, casing:!2, fluid:4` |

規則：

- `showWhenTier` 與 `showWhenChannels` 按 AND 組合
- `showWhenChannels` 可以在一個屬性裡同時宣告多個 channel
- 像 `!2` 這樣的純反選表示“除了 2 以外都匹配”
- 同樣的屬性也支援 `<PlaySound>` 和 `<BlockAnnotationTemplate>` 的子註解

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

高亮一個 `1x1x1` 方塊體積。

| 屬性 | 必需 | 含義 |
| --- | --- | --- |
| `pos` | 是 | `x y z` 向量 |
| `color` | 否 | `#RRGGBB`、`#AARRGGBB` 或 `transparent` |
| `colorMode` | 否 | `solid`（預設）或 `rainbow`；彩虹圍繞註解投影中心旋轉 360°，替換 RGB 並保留 `color` 的透明度 |
| `colorPeriod` | 否 | 彩虹迴圈週期，單位秒，必須為正且有限，預設 `2.4`；非法值會報告頁面錯誤 |
| `thickness` | 否 | 線寬，float |
| `alwaysOnTop` | 否 | boolean expression |


示例：

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Highlights the controller block.
</BlockAnnotation>
<BlockAnnotation pos="4 0 2" color="#80FFFFFF" colorMode="rainbow" colorPeriod="4.8" />
````

## `<BoxAnnotation>`

高亮任意軸對齊包圍盒。

| 屬性 | 必需 | 含義 |
| --- | --- | --- |
| `min` | 是 | `x y z` 最小向量 |
| `max` | 是 | `x y z` 最大向量 |
| `color` | 否 | 註解顏色 |
| `colorMode` | 否 | `solid`（預設）或 `rainbow`；彩虹圍繞註解投影中心旋轉 360°，替換 RGB 並保留 `color` 的透明度 |
| `colorPeriod` | 否 | 彩虹迴圈週期，單位秒，必須為正且有限，預設 `2.4`；非法值會報告頁面錯誤 |
| `thickness` | 否 | 線寬，float |
| `alwaysOnTop` | 否 | boolean expression |


GuideNH 會在每個軸上自動交換反向提供的 min/max 座標。

示例：

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Half-height area highlight.
</BoxAnnotation>
<BoxAnnotation min="2 0 0" max="4 1.5 0.5" color="#80FFFFFF" colorMode="rainbow" colorPeriod="2.4" />
````

## `<LineAnnotation>`

在世界空間中繪製一條線段或多段折線。

| 屬性 | 必需 | 含義 |
| --- | --- | --- |
| `from` | 是，除非設定了 `points` | `x y z` 起點向量 |
| `to` | 是，除非設定了 `points` | `x y z` 終點向量 |
| `points` | 否 | 用分號分隔的多個 `x y z` 點，用於折線；設定後會覆蓋 `from` / `to` |
| `color` | 否 | 註解顏色 |
| `thickness` | 否 | 線寬，float |
| `alwaysOnTop` | 否 | boolean expression |
| `arrow` | 否 | `start` 或 `end`；省略時不顯示箭頭 |
| `showPoints` | 否 | boolean expression；把所有點顯示為小立方體 |
| `pointColor` | 否 | 預設點顏色；省略時使用線顏色 |
| `pointSize` | 否 | 預設點大小；省略時比 `thickness` 稍粗 |

`LineAnnotation` 可以包含 `<LinePoint>` 子標籤，用於覆蓋單個點的顯示樣式。
`LinePoint` 支援 `index`、可選的 `show`、可選的 `color` 和可選的 `size`。點編號從 0 開始。
箭頭只能放在起點或終點；折線中間點不能設定箭頭。

示例：

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Signal path.
</LineAnnotation>
````

帶 3D 端點箭頭和單獨點標記的折線：

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

在世界座標位置放置一個面向螢幕的菱形標記。

| 屬性 | 必需 | 含義 |
| --- | --- | --- |
| `pos` | 是 | `x y z` 標記位置 |
| `color` | 否 | 著色；省略時預設亮綠色 |

示例：

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<OutlineAnnotation>`

在方塊和實體周圍繪製類似發光效果的輪廓描邊。輪廓貼合每個目標實際渲染出的形狀，即使被前方的方塊或實體遮擋也依然可見，相互接觸的輪廓會合併成一個整體。

| 屬性 | 必需 | 含義 |
| --- | --- | --- |
| `pos` | 否 | 要描邊的方塊 `x y z`；預設 `0 0 0`，在 `<BlockAnnotationTemplate>` 內即為匹配到的方塊 |
| `min` / `max` | 否 | 區域的方塊角座標（包含兩端），區域內的每個方塊都會被描邊 |
| `entity` | 否 | 要描邊的 `<Entity>` 的 `sceneEntityId` |
| `target` | 否 | `blocks`（預設）、`entities` 或 `all`：區域內描邊哪些內容；沒有區域時 `entities` 會描邊場景中的所有實體 |
| `mode` | 否 | `auto`（預設）、`geometry`、`cutout` 或 `bounds` |
| `color` | 否 | 輪廓顏色；省略時為白色，實體則使用其計分板隊伍顏色 |
| `colorMode` | 否 | `solid`（預設）或 `rainbow`；彩虹圍繞註解投影中心旋轉 360°，替換 RGB 並保留 `color` 的透明度 |
| `colorPeriod` | 否 | 彩虹迴圈週期，單位秒，必須為正且有限，預設 `2.4`；非法值會報告頁面錯誤 |
| `thickness` | 否 | 輪廓寬度，單位為 GUI 畫素，範圍 `0.25` 到 `8`，預設 `1` |
| `alwaysOnTop` | 否 | boolean expression；被遮擋的部分保持完整亮度而不會變暗 |
| `pulse` | 否 | boolean expression；開啟呼吸燈效果 |
| `pulsePeriod` | 否 | 呼吸週期，單位秒，預設 `2` |


輪廓模式：

- `auto` 按目標自動選擇。封閉模型（完整方塊、玻璃、冰、流體、玻璃板、多部件與線纜方塊、OBJ 和 TESR 模型）使用幾何體輪廓，因此玻璃這類透明紋理會描出整個方塊的立方體輪廓，而不是玻璃邊框的紋理邊界。貼圖式渲染（花、樹苗、作物、火把、鐵軌、梯子、藤蔓、紅石）和實體使用紋理透明度，輪廓貼合畫出的形狀而不是四邊形面片。不渲染任何內容的方塊使用其選擇框。
- `geometry` 始終使用渲染出的幾何體。
- `cutout` 始終使用紋理透明度。
- `bounds` 描邊方塊的選擇框和實體的碰撞箱。

規則：

- 只在輪廓外側繪製描邊，不會給目標本身著色
- 被其他方塊或實體遮擋的部分依然可見，並像其他被遮擋的註解一樣變暗；`alwaysOnTop` 會讓它們保持完整亮度
- 隱身的實體同樣會被描邊
- 相互接觸或重疊的輪廓會合併為一條輪廓線，不同註解之間也會合並
- 半透明顏色和柔和邊緣會與場景正確混合，邊緣不會變黑
- 子內容會成為 tooltip；懸停任意被描邊的方塊或實體即可顯示
- 在 `<BlockAnnotationTemplate>` 內，`pos` 相對於每個匹配到的方塊，所有匹配方塊的輪廓會合併成一個整體
- 匯出的網站會用 WebGL 繪製相同的輪廓

示例：

````md
<GameScene zoom={3} interactive={true}>
  <Block id="minecraft:glass" />
  <Block id="minecraft:red_flower" x="2" />
  <Entity id="minecraft:sheep" sceneEntityId="sheep" x="1.5" z="2.5" />
  <OutlineAnnotation pos="0 0 0" color="#55FFFF">
    玻璃会被描成完整的立方体轮廓。
  </OutlineAnnotation>
  <OutlineAnnotation pos="2 0 0" color="#80FFFFFF" colorMode="rainbow" colorPeriod="2.4" thickness="1.5" />
  <OutlineAnnotation entity="sheep" color="#FFAA00" alwaysOnTop={true} pulse={true} />
</GameScene>
````

描邊結構中所有匹配的方塊：

````md
<BlockAnnotationTemplate id="minecraft:glass">
  <OutlineAnnotation color="#AAFFFF" />
</BlockAnnotationTemplate>
````

## `<TextAnnotation>`

繪製場景中的氣泡文字標籤。它可以跟隨一個世界座標錨點，也可以固定在場景中心附近的螢幕座標上。與其他註解不同，它的子內容會作為氣泡正文，而不是懸停 tooltip。

| 屬性 | 必需 | 含義 |
| --- | --- | --- |
| `pos` | 否 | `x y z` 世界座標錨點 |
| `x`, `y`, `z` | 否 | 未提供 `pos` 時可用的獨立座標分量 |
| `text` | 否 | 氣泡文字；省略時使用子 Markdown 內容 |
| `textKey` | 否 | 優先從資源包 `lang` 檔案解析的翻譯鍵；解析失敗時回退到 `text` 或子 Markdown 內容 |
| `color` | 否 | 氣泡邊框顏色，預設淺灰色 |
| `backgroundAlpha` | 否 | 背景透明度，範圍 `0` 到 `255`，預設 `204` |
| `maxWidth` | 否 | 畫素換行寬度；`0` 表示單行 |
| `independent` | 否 | `true` 時固定在螢幕空間 |
| `yOffset` | 否 | `independent={true}` 時相對場景中心的垂直畫素偏移 |
| `connectorSide` | 否 | `bottom`、`top`、`left`、`right` 或 `none`；預設 `bottom` |
| `connectorOffset` | 否 | 沿氣泡邊緣偏移連線點；top/bottom 正值向右，left/right 正值向下 |
| `connectorLength` | 否 | 連線線畫素長度；預設 `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | 否 | 可選伴生高亮框範圍 |
| `highlightColor` | 否 | 可選高亮框顏色 |

世界錨定的氣泡會繪製一條連線線指向錨點。可以用 `connectorSide` 控制連線線接在氣泡的哪條邊，用 `connectorOffset` 沿邊移動連線點，用 `connectorLength` 調整氣泡與錨點之間的距離。獨立氣泡會水平居中，並使用 `yOffset` 控制垂直位置，且不會繪製連線線。匯入思索時間線的 `text` 註解時也會使用同一個執行時註解。

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

固定螢幕座標示例：

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  独立状态文本
</TextAnnotation>
````

## 富 Tooltip 內容

註解的子內容會按普通 GuideNH 內容編譯，因此 tooltip 內可以包含：

- Markdown 段落和標題
- 物品/方塊圖片
- 配方
- 巢狀的非互動場景

示例：

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<InputAnnotation>` 流體目標

InputAnnotation 可以指定 `item` 或 `fluid`，不能同時填寫。流體目標可選填 `amount`（非負整數）和 `nbt`（SNBT），這兩個引數要求同時提供 `fluid`。位置、inputType、modifier 與 StructureLib 可見條件沿用現有規則。

流體懸停資訊包含名稱、溫度及氣體/液體狀態，載入 GregTech 時也會顯示依 GT 設定產生的材料提示。明確填入的數量遵循 L/mB 單位設定。ExportSite 的輸入標註保留相同文字作為瀏覽器懸停提示，並保留格式化的數量。

```mdx
<GameScene width="180" height="120">
  <Block x="0" y="0" z="0" id="minecraft:stone" />
  <InputAnnotation pos="0.5 1.5 0.5" inputType="rmb" fluid="water" amount={1000} />
</GameScene>
```

註解只顯示操作圖示和流體資訊，不會填充容器或改變場景世界。編輯、儲存、過載和匯出需要保留註冊名、數量與 NBT。可放置的流體方塊仍須在 Block / PlaceBlock 中使用真實方塊 ID。

## `<BlockAnnotationTemplate>`

當你希望把同一種註解蓋到所有匹配方塊上時使用它。

| 屬性 | 必需 | 含義 |
| --- | --- | --- |
| `id` | 是 | 方塊匹配器，格式為 `modid:block[:meta]` |

規則：

- 模板只會看到它被解析時場景裡已經存在的方塊
- 應放在 `<Block>`、`<ImportStructure>` 或 `<ImportStructureLib>` 之後
- 子註解使用相對於每個匹配方塊的區域性座標

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

## 相關頁面

- [遊戲場景](GameScene)
- [示例](Examples)
