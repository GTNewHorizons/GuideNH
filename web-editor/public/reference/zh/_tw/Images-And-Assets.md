[English](Images-And-Assets)

# 圖片與資源

GuideNH 同时支持普通 Markdown 图片，以及若干运行时专用的视觉元素。

## 資源解析規則

指南资源使用与页面链接相同的解析规则。

| 路徑形式 | 範例 | 意義 |
| --- | --- | --- |
| 相對路徑 | `test1.png` | 相對於目前頁面文件 |
| 根路徑 | `/assets/example_structure.snbt` | 相對於目前指南根目錄 |
| 明確資源 id | `guidenh:textures/gui/example.png` | 絕對 `modid:path` 查找 |

## Markdown 圖片

支持普通 Markdown 图片：

````md
![Example](test1.png)
````

GuideNH 會解析路徑，並從指南內容根目錄載入對應的二進位資源。

## `FloatingImage`

`<FloatingImage>` 用於渲染「先裁切、再縮放」的點陣圖區域，既可以配合文字浮動，也可以真正作為段落內嵌內容使用。它還支援顯式 `modid:path` 紋理 id，因此可以直接引用其他模組的紋理資源。

### 屬性

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `src` | 是 | 圖片路徑 |
| `x` | 是 | 原圖裁切起始 X，單位為來源影像像素 |
| `y` | 是 | 原圖裁切起始 Y，單位為來源影像像素 |
| `width` / `w` | 是 | 原圖裁切寬度，單位為來源影像像素；兩種寫法只能二選一 |
| `height` / `h` | 是 | 原圖裁切高度，單位為來源影像像素；兩種寫法只能二選一 |
| `scaleX` | 否 | 水平顯示縮放倍率，預設 `1.0` |
| `scaleY` | 否 | 垂直顯示縮放倍率，預設 `1.0` |
| `displayWidth` | 否 | 最終顯示寬度，單位為像素；單獨使用時以裁切區域比例計算高度 |
| `displayHeight` | 否 | 最終顯示高度，單位為像素；單獨使用時以裁切區域比例計算寬度 |
| `wrap` | 否 | `inline` 表示真正行內放置，其他值使用常規環繞模式 |
| `align` | 否 | 浮動時使用 `left` 或 `right`；`wrap="inline"` 時會被忽略 |
| `title` | 否 | tooltip/title 文本 |
| `sound` | 否 | 整張圖片點擊時播放的音效事件 |
| `soundSrc` | 否 | 整張圖片所使用的音效檔路徑 |
| `trigger` | 否 | 預設 `click`，也可以寫 `hover` 以懸停播放 |

### 說明

- 裁切時必須同時提供 `x`、`y`、`width` / `w` 與 `height` / `h`
- 全部省略裁切屬性時，可使用 `displayWidth` 或 `displayHeight` 顯示整張來源圖
- `width` 和 `height` 現在表示裁切區域，不再表示最終顯示尺寸
- `scaleX` 與 `scaleY` 會將最終顯示尺寸計算為 `cropWidth * scaleX` 與 `cropHeight * scaleY`
- `displayWidth` 或 `displayHeight` 以像素指定最終顯示尺寸；只提供其中一個時，另一個尺寸按裁切區域寬高比自動計算
- 同時提供 `displayWidth` 與 `displayHeight` 時，可依指定尺寸非等比拉伸
- `displayWidth` / `displayHeight` 不能與 `scaleX` / `scaleY` 同時使用
- 支援單軸拉伸，只修改一個縮放值即可
- 同時寫 `width` 和 `w`，或同時寫 `height` 和 `h`，都會渲染可見錯誤
- 舊版把 `width` / `height` 當作顯示尺寸的內容會發生破壞性變更，需要手動遷移
- `src` 可以是相對路徑、根路徑，或明確 `modid:path` 紋理 id，例如 `minecraft:textures/gui/options_background.png`

### 範例

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

`<ImageAnnotation>` 是 `<FloatingImage>` 的子標籤，用於為圖片的矩形區域附加富文本 tooltip（以及可選的彩色邊框）。座標以**裁剪後子影像像素**為單位，當裁剪後的影像被縮放或拉伸時會自動等比例調整。

### 屬性

| 屬性 | 必需 | 預設值 | 意義 |
| --- | --- | --- | --- |
| `x` | 否 | — | 區域左邊緣（圖片像素） |
| `y` | 否 | — | 區域上緣（圖片像素） |
| `w` | 否 | — | 區域寬度（圖片像素） |
| `h` | 否 | — | 區域高度（圖片像素） |
| `border` | 否 | `false` | 是否顯示彩色邊框 |
| `borderColor` | 否 | 隨機 | 邊框顏色（`#RRGGBB` 或 `#AARRGGBB`） |
| `borderThickness` | 否 | `1` | 邊框粗細（顯示像素） |
| `sound` | 否 | 無 | 此區域播放的選用音效事件 |
| `src` | 否 | 無 | 選用音效檔路徑，會轉換為音效事件 id |
| `trigger` | 否 | `click` | `click` 或 `hover` |

### 說明

- 同時省略 `x`、`y`、`w`、`h` 時，註解覆蓋**整張圖片**
- 若任一座標存在，省略的座標預設為 `0`（原點）或 `1`（尺寸）
- 預設**不顯示**邊框；新增 `border` 或 `border={true}` 屬性來啟用
- 啟用邊框但未指定 `borderColor` 時，自動產生隨機不透明顏色
- 子 MDX 內容作為 tooltip 正文渲染，支援任意內聯/區塊級元素
- 多個註解區域重疊時，列表中靠後的註解（覆蓋在上方）優先響應懸停

### 範例

整圖註解：

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation>
    鼠标悬停在图片任意位置都会显示此 tooltip。
  </ImageAnnotation>
</FloatingImage>
````

有可見邊框的區域註解：

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    悬停在**红框区域**内显示此 tooltip。
  </ImageAnnotation>
</FloatingImage>
````

同一圖片上的多個註解：

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#FF44FF44">
    左半部分
  </ImageAnnotation>
  <ImageAnnotation x="64" y="0" w="64" h="64" border borderColor="#FF4444FF">
    右半部分
  </ImageAnnotation>
</FloatingImage>
````

圖片區也可以播放音效。只需要音效時可以使用 `<SoundArea>`；如果同一個區域還需要
tooltip 或邊框，也可以直接在 `<ImageAnnotation>` 上寫 `sound`。

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

`<FloatingImage sound="...">` 會覆蓋整張圖片。區域音效使用裁剪後子影像座標，
並遵循與 tooltip 相同的重疊優先權：靠後的區域優先回應。

## 內容嵌入與文字環繞

所有區塊級標籤－`<FloatingImage>`、`<Recipe>`、`<GameScene>`、`<ItemImage>`、`<BlockImage>` 以及
其他基於 `BlockTagCompiler` 的標籤－皆支援兩個可選的佈局屬性，提供類似 Word 的內容嵌入功能。

| 屬性 | 可选值 | 默认值 | 意義 |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | 文字環繞模式 |
| `align` | `left` · `center` · `right` | `left` | 水平對齊方式 |

### 環繞模式

| 模式 | Word 對應 | 區塊級上下文效果 | 流式上下文效果 |
| --- | --- | --- | --- |
| `inline` | 嵌入型 | 預設堆疊（嵌入型） | 嵌入在文字行中 |
| `square` | 方形 | 文件級浮動，文字在方形框內環繞（方形環繞） | `FLOAT_LEFT` / `FLOAT_RIGHT` |
| `tight` | 緊密型 | 等同 `square`（緊密型） | 等同 `square` |
| `through` | 穿越型 | 等同 `square`（穿越型） | 等同 `square` |
| `top-bottom` | 上下型 | 佔滿整行寬度；`align` 控制水平位置（上下型） | 行內嵌入（含換行） |
| `behind` | 襯於文字下方 | 對齊的行內槽；渲染在文字下方（襯於文字下方） | 嵌入在文字行中 |
| `front` | 浮於文字上方 | 對齊的行內槽；渲染在文字上方（浮於文字上方） | 嵌入在文字行中 |

### 浮動模式的對齊

對於 `wrap=square/tight/through`：
- `align=left`（預設）－向**左**浮動，文字填入右側區域。
- `align=right`－向**右**浮動，文字填滿左側區域。
- `align=center`－居中對齊（無文字環繞）。

### 範例

使用新 `wrap` 屬性的左浮動圖片：

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

右浮動配方：

````md
<Recipe id="minecraft:stone" wrap="square" align="right" />

此处文字将流向配方框左侧……
````

居中物品圖示（無文字環繞）：

````md
<ItemImage id="minecraft:diamond" align="center" />
````

右對齊物品圖示：

````md
<ItemImage id="minecraft:diamond" align="right" />
````

物品 NBT 可以單獨寫在 `nbt` 屬性中。 `id` 中的內聯 SNBT 仍然支持；兩種寫法同時存在時，
獨立的 `nbt` 屬性最後合併。

````md
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"自定义钻石"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

> **注意** — `wrap="inline"` 現在會讓 `<FloatingImage>` 真正作為行內內容放置。
> 在 inline 模式下，`align` 會被忽略，而不是報錯。

## 導航紋理圖標

frontmatter 可以使用 `icon_texture`，在導航/搜尋中顯示紋理而不是物品：

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

該檔案必須能解碼為圖片。路徑的解析規則與其他指南資源路徑完全一致。

## 非圖片資源

GuideNH 頁面也可以引用非圖片類別運行時資源，最常見的是結構文件，例如：

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

這些資源會透過同一套指南資源管線加載，但不是直接作為圖片渲染，而是交由自訂標籤消費。

## 最佳實踐

- 頁面私有圖片盡量放在使用它們的頁面旁邊
- 可重複使用檔案盡量放在指南根的 `assets/` 目錄中
- 多頁面共享檔案優先使用根路徑 `/assets/...`
- 只有在資源確實是圖片時才使用紋理圖標

## `BlockImage`

`<BlockImage>` 也遵循與 `<FloatingImage>` 相同的區塊級嵌入規則，但它顯示的是透明背景的 3D
單方塊預覽，而非點陣圖。它適合在正常正文裡直接展示「方塊被放在世界裡時」的樣子。

關鍵行為：

- 背景透明，邊框透明
- 沒有場景按鈕，沒有 layer 滑條，也沒有註解編輯面
- 懸停時仍會顯示方塊選取線框與 tooltip
- `scale` 控制相機縮放，預設值為 `4`
- `perspective` 支援 `isometric-north-east`、`isometric-north-west`、`up`
- `nbt` 用於提供 TileEntity SNBT；`id="mod:block:meta:{...}"` 形式的內聯 SNBT 仍然可用，
但獨立 `nbt` 屬性較清晰，推薦優先使用

範例：

````md
<BlockImage id="minecraft:stone" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## 運行時範例文件

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## 相關頁面

- [指南頁面格式](Guide-Page-Format)
- [標籤參考](Tags-Reference)
- [遊戲場景](GameScene)
