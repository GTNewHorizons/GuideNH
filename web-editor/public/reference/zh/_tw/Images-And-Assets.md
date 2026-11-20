# 圖片與資源

GuideNH 同時支援普通 Markdown 圖片，以及若干執行時專用的視覺元素。

## 資源解析規則

指南資源使用與頁面連結相同的解析規則。

| 路徑形式 | 示例 | 含義 |
| --- | --- | --- |
| 相對路徑 | `test1.png` | 相對於當前頁面檔案 |
| 根路徑 | `/assets/example_structure.snbt` | 相對於當前指南根目錄 |
| 顯式資源 id | `guidenh:textures/gui/example.png` | 絕對 `modid:path` 查詢 |

## Markdown 圖片

支援普通 Markdown 圖片：

````md
![Example](test1.png)
````

GuideNH 會解析路徑，並從指南內容根目錄載入對應的二進位制資源。

## `FloatingImage`

使用 `wrap="inline"` 時，圖片與周圍文字垂直居中，所在行會動態增高以完整容納圖片。上下相鄰文字行保持原有行高，不設定固定的行高上限。如果希望超高圖片獨佔一行，請將標籤放在獨立段落中，並在前後留出空行。

`<FloatingImage>` 用於渲染“先裁剪、再縮放”的點陣圖區域，既可以配合文字浮動，也可以真正作為段落內聯內容使用。它還支援顯式 `modid:path` 紋理 id，因此可以直接引用其他模組的紋理資源。

### 屬性

| 屬性 | 必需 | 含義 |
| --- | --- | --- |
| `src` | 是 | 圖片路徑 |
| `x` | 裁剪時必需 | 原圖裁剪起始 X，單位為源影象畫素 |
| `y` | 裁剪時必需 | 原圖裁剪起始 Y，單位為源影象畫素 |
| `width` / `w` | 裁剪時必需 | 原圖裁剪寬度，單位為源影象畫素；兩種寫法只能二選一 |
| `height` / `h` | 裁剪時必需 | 原圖裁剪高度，單位為源影象畫素；兩種寫法只能二選一 |
| `scaleX` | 否 | 水平顯示縮放倍率，預設 `1.0` |
| `scaleY` | 否 | 垂直顯示縮放倍率，預設 `1.0` |
| `displayWidth` | 否 | 最終顯示寬度，單位為畫素；單獨使用時按裁剪區域比例計算高度 |
| `displayHeight` | 否 | 最終顯示高度，單位為畫素；單獨使用時按裁剪區域比例計算寬度 |
| `wrap` | 否 | `inline` 放在文字行內；否則由 `align` 決定浮動方向 |
| `align` | 否 | 浮動時使用 `left` 或 `right`；`wrap="inline"` 時會被忽略 |
| `title` | 否 | tooltip/title 文字 |
| `alt` | 否 | 圖片替代文字 |
| `sound` | 否 | 整張圖片點選時播放的音效事件 |
| `soundSrc` | 否 | 整張圖片使用的音效檔案路徑 |
| `trigger` | 否 | 預設 `click`，也可以寫 `hover` 以懸停播放 |

### 說明

- 裁剪時必須同時提供 `x`、`y`、`width` / `w` 與 `height` / `h`
- 全部省略裁剪屬性時，可使用 `displayWidth` 或 `displayHeight` 顯示整張源圖
- `width` 與 `height` 表示源圖裁剪尺寸；顯示尺寸使用 `displayWidth` / `displayHeight` 或縮放倍率。
- `scaleX` 與 `scaleY` 會將最終顯示尺寸計算為 `cropWidth * scaleX` 與 `cropHeight * scaleY`
- `displayWidth` 或 `displayHeight` 以畫素指定最終顯示尺寸；只提供其中一個時，另一個尺寸按裁剪區域寬高比自動計算
- 同時提供 `displayWidth` 與 `displayHeight` 時，可按指定尺寸進行非等比拉伸
- `displayWidth` / `displayHeight` 不能與 `scaleX` / `scaleY` 同時使用
- 支援單軸拉伸，只修改一個縮放值即可
- 同時寫 `width` 和 `w`，或同時寫 `height` 和 `h`，都會渲染可見錯誤
- `src` 可以是相對路徑、根路徑，或顯式 `modid:path` 紋理 id，例如 `minecraft:textures/gui/options_background.png`

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

`<ImageAnnotation>` 是 `<FloatingImage>` 的子標籤，用於為圖片的矩形區域附加富文字 tooltip（以及可選的彩色邊框）。座標以**裁剪後子影象畫素**為單位，當裁剪後的影象被縮放或拉伸時會自動等比例調整。

### 屬性

| 屬性 | 必需 | 預設值 | 含義 |
| --- | --- | --- | --- |
| `x` | 否 | — | 區域左邊緣（圖片畫素） |
| `y` | 否 | — | 區域上邊緣（圖片畫素） |
| `w` | 否 | — | 區域寬度（圖片畫素） |
| `h` | 否 | — | 區域高度（圖片畫素） |
| `border` | 否 | `false` | 是否顯示彩色邊框 |
| `borderColor` | 否 | 隨機 | 邊框顏色（`#RRGGBB` 或 `#AARRGGBB`） |
| `borderThickness` | 否 | `1` | 邊框粗細（顯示畫素） |
| `sound` | 否 | 無 | 此區域播放的可選音效事件 |
| `src` | 否 | 無 | 可選音效檔案路徑，會轉換為音效事件 id |
| `trigger` | 否 | `click` | `click` 或 `hover` |

### 說明

- 同時省略 `x`、`y`、`w`、`h` 時，註解覆蓋**整張圖片**
- 若任一座標存在，省略的座標預設為 `0`（原點）或 `1`（尺寸）
- 預設**不顯示**邊框；新增 `border` 或 `border={true}` 屬性來啟用
- 啟用邊框但未指定 `borderColor` 時，自動生成隨機不透明顏色
- 子 MDX 內容作為 tooltip 正文渲染，支援任意內聯/塊級元素
- 多個註解區域重疊時，列表中靠後的註解（覆蓋在上方）優先響應懸停

### 示例

整圖註解：

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation>
    鼠标悬停在图片任意位置都会显示此 tooltip。
  </ImageAnnotation>
</FloatingImage>
````

帶可見邊框的區域註解：

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="2" y="2" w="15" h="10" border borderColor="#FFFF4444" borderThickness="2">
    悬停在**红框区域**内显示此 tooltip。
  </ImageAnnotation>
</FloatingImage>
````

同一圖片上的多個註解：

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="0" y="0" w="16" h="32" border borderColor="#FF44FF44">
    左半部分
  </ImageAnnotation>
  <ImageAnnotation x="16" y="0" w="16" h="32" border borderColor="#FF4444FF">
    右半部分
  </ImageAnnotation>
</FloatingImage>
````

圖片區域也可以播放音效。只需要音效時可以使用 `<SoundArea>`；如果同一個區域還需要
tooltip 或邊框，也可以直接在 `<ImageAnnotation>` 上寫 `sound`。

````md
<FloatingImage
  displayWidth="128" displayHeight="128" src="test1.png"
  align="left"
  x="0"
  y="0"
  width="32"
  height="32"
  sound="guidenh:guide.sample_click"
>
  <SoundArea x="0" y="0" w="16" h="32" sound="guidenh:guide.sample_left" />
  <SoundArea x="16" y="0" w="16" h="32" sound="guidenh:guide.sample_hover" trigger="hover" />
  <ImageAnnotation x="2" y="2" w="10" h="10" border sound="guidenh:guide.sample_click">
    这个区域同时拥有 tooltip 内容和点击音效。
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` 會覆蓋整張圖片。區域音效使用裁剪後子影象座標，
並遵循與 tooltip 相同的重疊優先順序：靠後的區域優先響應。

## 內容嵌入與文字環繞

由 `BlockTagCompiler` 編譯的塊級標籤（包括 `<Recipe>`、`<GameScene>` 和 `<BlockImage>`）接受 `wrap` 與 `align`。

`<FloatingImage>` 使用獨立規則：`wrap="inline"` 放在文字行內，否則由 `align="left"` 或 `"right"` 決定浮動方向。`<ItemImage>` 是行內圖示，需要佈局對齊時應放入 `<Column>` 或 `<Row>`。

| 屬性 | 預設值 | 說明 |
| --- | --- | --- |
| `wrap` | `inline` | `inline`：常規放置；`square`、`tight`、`through`：左右浮動；`top-bottom`、`behind`、`front` 當前均按常規對齊方式放置，不建立獨立文字圖層。 |
| `align` | `left` | 塊級上下文接受 `left`、`center`、`right`。浮動環繞模式下，`right` 向右浮動，其餘值向左浮動；居中請用 `wrap="inline" align="center"`。 |
| `float` | — | 相容行內上下文中的 `left` / `right`，僅在未提供 `wrap` 和 `align` 時生效；推薦顯式使用後兩者。 |

```mdx
<BlockImage id="minecraft:stone" wrap="square" align="right" />

<BlockImage id="minecraft:diamond_block" wrap="inline" align="center" />

<Column align="center">
  <ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
</Column>
```

## 導航紋理圖示

frontmatter 可以使用 `icon_texture`，在導航/搜尋中顯示紋理而不是物品：

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

該檔案必須能被解碼為圖片。路徑的解析規則與其他指南資源路徑完全一致。

## 非圖片資源

GuideNH 頁面也可以引用非圖片類執行時資源，最常見的是結構檔案，例如：

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

這些資源會透過同一套指南資源管線載入，但不是直接作為圖片渲染，而是交由自定義標籤消費。

## 最佳實踐

- 頁面私有圖片儘量放在使用它們的頁面旁邊
- 可複用檔案儘量放在指南根的 `assets/` 目錄中
- 多頁面共享檔案優先使用根路徑 `/assets/...`
- 只有在資源確實是圖片時才使用紋理圖示

## `BlockImage`

`<BlockImage>` 顯示透明背景的已放置方塊 3D 預覽，支援上面的塊級佈局屬性，包括水平對齊和文字浮動環繞。

關鍵行為：

- 背景透明，邊框透明
- 沒有場景按鈕，沒有 layer 滑條，也沒有註解編輯面
- 懸停時依然會顯示方塊選中線框與 tooltip
- `scale` 控制相機縮放，預設值為 `4`
- `perspective` 支援 `isometric-north-east`、`isometric-north-west`、`up`
- `nbt` 用於提供 TileEntity SNBT；`id="mod:block:meta:{...}"` 形式的內聯 SNBT 仍然可用，
  但獨立 `nbt` 屬性更清晰，推薦優先使用

示例：

````md
<BlockImage id="minecraft:stone" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## 執行時示例檔案

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## 相關頁面

- [指南頁面格式](Guide-Page-Format)
- [標籤參考](Tags-Reference)
- [遊戲場景](GameScene)
