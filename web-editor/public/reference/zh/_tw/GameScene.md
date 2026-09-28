[English](GameScene)

# 遊戲場景

`<GameScene>` 是 GuideNH 的 3D 預覽標籤，`<Scene>` 是具有相同行為的別名。

## 場景屬性

| 屬性 | 類型 | 預設值 | 意義 |
| --- | --- | --- | --- |
| `width` | integer | `256` | 視窗寬度（像素） |
| `height` | integer | `192` | 視窗高度（像素） |
| `zoom` | float | `1.0` | 相機縮放倍率 |
| `perspective` | string | `isometric-north-east` | 相機預設 |
| `rotateX` | float | auto | 明確 X 旋轉覆蓋值 |
| `rotateY` | float | auto | 明確 Y 旋轉覆蓋值 |
| `rotateZ` | float | auto | 明確 Z 旋轉覆蓋值 |
| `offsetX` | float | auto | 螢幕空間水平平移 |
| `offsetY` | float | auto | 螢幕空間垂直平移 |
| `centerX` | float | auto | 顯式世界旋轉中心 X |
| `centerY` | float | auto | 顯式世界旋轉中心 Y |
| `centerZ` | float | auto | 顯式世界旋轉中心 Z |
| `interactive` | boolean expression | `true` | 是否啟用滑鼠交互 |
| `showBackground` | boolean expression | `true` | 是否顯示場景背景與邊框 |
| `allowLayerSlider` | boolean | `true` | 是否顯示垂直層滑塊 |
| `gridButtonEnabled` | boolean | `true` | 是否顯示地板網格切換按鈕 |
| `showGrid` | boolean | `false` | 地板網格的初始可見性 |

## 方塊統計框

包含方塊的場景預設會啟用方塊統計切換按鈕。需要覆蓋模式、位置、過濾器、可見性或尺寸時，再加入 `<BlockStats>` 子標籤。
清單只會在場景方塊、思索時間軸狀態、StructureLib 選擇或統計配置發生變化時重建；普通渲染幀會重複使用已準備好的行資料。
列表會受 `maxWidth` 和 `maxHeight` 限制；如果省略，它們取"固定的 `224` × `96` 像素"與"場景寬高的 40%"兩者中較大的一個。內容過寬或過高時會出現可拖曳捲軸；滑鼠在統計框上時滾輪會滾動列表，按住 Shift 可以橫向滾動。

自動模式會掃描場景中的非空氣方塊，並盡量解析成玩家通常看到的物品顯示。一個座標內包含多個可見組件的方塊可以貢獻多個物品，
例如安裝對應模組時的 AE2 cable bus 零件和 facade、ForgeMultipart 零件掉落、Carpenters' Blocks cover 或 overlay。
統計會依 `item:meta` 分組、依數量排序。

自動模式清單也可以用 `dock="left"`、`dock="top"`、`dock="right"` 或 `dock="bottom"` 吸附在場景外側。
外側吸附會根據吸附邊的長度自動換列或換行，預留佈局空間，並在右側吸附時避開場景按鈕列。
點選自動統計列表中的物品會用解析到的碰撞箱，以穿透顯示的面覆蓋高亮場景裡所有對應方塊；再次點擊同一物品會取消高亮。
數量透過 ItemStack 自帶的堆疊數量渲染。設定 `showNames={true}` 後會在名稱後面也追加數量，滑鼠懸停物品時 Tooltip 會額外顯示精確方塊數量。

可以用過濾器隱藏常見方塊，或只顯示指定方塊：

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

如果想要展示規劃材料表，而不是場景真實內容，可以使用手動模式：

````md
<GameScene>
  <Block id="minecraft:furnace" />
  <BlockStats mode="manual" corner="topRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:cobblestone" count="8" />
    <BlockStat item="minecraft:furnace" count="1" />
  </BlockStats>
</GameScene>
````

## Debug 模式疊加層

在 GuideNH Mod 配置中啟用 `enableDebugMode` 選項後，3D 場景預覽將提供以下額外疊加層。

### 網格座標標籤

當 debug 模式**開啟**且地板網格**可見**時，座標標籤將渲染在各網格線旁：

- **X 軸數字**：沿著網格的近側邊緣（預設 `isometric-north-east` 視角下為北/−Z 邊）顯示，
每个整数 X 世界坐标处各有一个标签。
- **Z 轴数字**：沿网格的近侧边缘（东/+X 边）显示，每个整数 Z 世界坐标处各有一个标签。
- **基本方向首字母**（`N`/`S`/`E`/`W`）：繪製在各方向對應網格邊緣的中點處。

座標值使用場景 Level 中儲存的實際世界 X/Z 數值，因此當結構包含負座標方塊時，數字可以為負。

只要 debug 模式激活，**网格切换按钮将始终可用**，不受 `gridButtonEnabled` 属性限制，
随时可显示或隐藏网格及其坐标标签；`showGrid` 属性控制的默认网格可见性不受影响。

### 方塊座標 Tooltip

当 debug 模式**开启**且鼠标悬停在场景内的方块上时，除主 Tooltip 外，还会在其上方渲染
一个额外 Tooltip，以金色文字显示该方块的世界空间坐标 `X, Y, Z`。

若坐标 Tooltip 超出屏幕顶部则自动磁吸到光标下方显示。

## 視角預設

可接受的 `perspective` 值：

- `isometric-north-east`
- `isometric-north-west`
- `up`

未知值會回退到 `isometric-north-east`。

## 內容嵌入與文字環繞

所有區塊級標籤（包括 `<GameScene>`）支援兩個選用屬性，用於控制其在頁面中的嵌入方式，對應 Microsoft Word 的"文字環繞"選項。

| 屬性 | 可選值 | 預設值 | 意義 |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | 文字環繞模式 |
| `align` | `left` · `center` · `right` | `left` | 水平對齊方式 |

### 環繞模式

| 模式 | Word 對應 | 效果 |
| --- | --- | --- |
| `inline` | 嵌入型 | 預設行為：場景獨佔一行（嵌入型） |
| `square` | 方形 | 場景浮動到左側或右側，文字在其周圍方形環繞（方形環繞） |
| `tight` | 緊密型 | 更緊密的環繞；此佈局系統中價於 `square`（緊密） |
| `through` | 穿越型 | 穿越型環繞；此佈局系統中價於 `square`（穿越型） |
| `top-bottom` | 上下型 | 文字僅在上下方，不在側面；`align` 控制水平位置（上下型） |
| `behind` | 襯在文字下方 | 場景渲染在文字下方；`align` 控制水平位置（襯於文字下方） |
| `front` | 浮於文字上方 | 場景渲染在文字上方；`align` 控制水平位置（浮於文字上方） |

### 範例

左浮動場景－後續段落文字環繞在右側：

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

此处文字将环绕在场景右侧……
````

右浮動場景：

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

此处文字将环绕在场景左侧……
````

居中場景（無文字環繞）：

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

行內嵌入（流式上下文）－文字環繞小場景：

````md
一段文字 {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} 右侧继续的文字将自动环绕。
````

## 範例

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## 導入範例

以下這些範例專門涵蓋導入結構時最容易踩坑的場景側行為。

具有明確朝向、旋轉、鏡像和偏移的 StructureLib 導入：

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib
    name="main"
    controller="gregtech:gt.blockmachines:2741"
    facing="north"
    rotation="clockwise_180"
    flip="none"
    offsetX="2"
    offsetY="1"
    offsetZ="-3"
  />
</GameScene>
````

GregTech 控制器現在預設保持未成型，即使導入的多方塊結構本身已經完整：

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" />
</GameScene>
````

只有在你明確希望預覽展示成型狀態時，才設定 `formed={true}`：

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" formed={true} />
</GameScene>
````

這個預設值同樣適用於直接透過 `<Block>` 放置的控制器，包括依賴周圍機械方塊的 GregTech 控制器：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="gregtech:gt.blockmachines:2741" />
  <Block id="gregtech:gt.blockmachines:1000" x="3" formed={true} />
</GameScene>
````

純方塊搭出來的簡單佈局仍然完全相容，後續如果替換成 GT 多方塊控制器，也能沿用同一套場景檢測邏輯：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:water" />
  <Block id="minecraft:water" x="-1" />
  <Block id="minecraft:water" x="1" />
  <Block id="minecraft:grass" z="1" />
  <Block id="minecraft:grass" x="1" z="1" />
  <Block id="minecraft:glass" z="2" />
  <Block id="minecraft:glass" x="1" z="2" />
</GameScene>
````

## 場景子元素

GuideNH 目前註冊了以下場景子標籤：

- `<Block>`
- `<ImportStructure>`
- `<ImportStructureLib>`
- `<IsometricCamera>`
- `<BlockStats>`
- `<PlaySound>`
- `<RemoveBlocks>`
- `<RemoveEntity>`
- `<ReplaceBlock>`
- `<PlaceBlock>`
- `<BlockAnnotationTemplate>`
- `<Entity>`
- 各類註解標籤，如 `<BoxAnnotation>` 和 `<LineAnnotation>`

## 場景音效

`<PlaySound>` 可放在 `<GameScene>` 內，用於透過場景互動或時間軸進入時播放音效。
支援的觸發方式：

- `click`，預設值
- `hover`，滑鼠進入場景時觸發一次
- `enter`，場景首次渲染時觸發一次

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

提供 `x`、`y`、`z` 時，音量會從投影後的場景座標到點擊點或場景中心進行螢幕空間衰減。
`radius` 預設是場景較短邊的 75%，`minVolume` 預設是 `0.15`。

## `<BlockStats>` 和 `<BlockStat>`

宣告或自訂方塊統計疊加框。包含方塊的場景即使省略該子標籤，也會啟用自動統計切換按鈕；如果包含 `<BlockStat>` 子項，則切換到手動統計模式。

`<BlockStats>` 屬性：

| 屬性 | 必需 | 預設值 | 意義 |
| --- | --- | --- | --- |
| `visible` | 否 | config，預設 `false` | 初始是否顯示統計框 |
| `buttonEnabled` | 否 | config，預設 `true` | 是否顯示方塊統計切換按鈕 |
| `mode` | 否 | `auto` | `auto` 或 `manual`；子 `<BlockStat>` 會強製手動模式 |
| `corner` | 否 | `topRight` | 統計框角落：`topRight`、`topLeft`、`bottomRight` 或 `bottomLeft` |
| `dock` | 否 | `inside` | 自動列表可吸附到 `inside`、`left`、`top`、`right` 或 `bottom`；手動模式始終使用內部統計框框始終使用內部統計框框始終使用內部統計框框使用內部統計框來始終使用內部統計框 |
| `showNames` | 否 | `false` | 是否在圖示旁顯示名稱；開啟後名稱後也會追加數量 |
| `filterMode` | 否 | `blacklist` | `blacklist` 或 `whitelist` |
| `filter` | 否 | 空 | 物品鍵，例如 `minecraft:stone` 或 `minecraft:stone:0`，可用空格、逗號或分號分隔 |
| `maxWidth` | 否 | 取 `224` 像素與場景寬度 40% 中較大者 | 統計框最大寬度，超出後出現水平捲軸 |
| `maxHeight` | 否 | 取 `96` 像素與場景高度 40% 中較大者 | 統計框最大高度，超出後出現垂直捲軸 |

`<BlockStat>` 屬性：

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `item` | 是，除非使用 `id` | 清單中顯示的物品 id |
| `id` | 是，除非使用 `item` | 既有物品堆疊屬性寫法 |
| `count` | 否 | 顯示數量；省略時該行顯示一次，寫入 `count="0"` 則隱藏該行 |

範例：

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

## `<Block>`

在預覽世界中放置一個方塊。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `id` | 是，除非提供了 `ore` | 方塊 id |
| `ore` | 否 | 礦山辭名；第一個配對結果必須能解析成方塊物品 |
| `x` | 否 | 世界座標 X，整數，預設 `0` |
| `y` | 否 | 世界座標 Y，整數，預設 `0` |
| `z` | 否 | 世界座標 Z，整數，預設 `0` |
| `meta` | 否 | 方块 metadata，整数 |
| `facing` | 否 | `down`、`up`、`north`、`south`、`west`、`east` |
| `nbt` | 否 | SNBT TileEntity compound |
| `formed` | 否 | 是否讓此結構控制器在預覽同步時以成型狀態處理；預設 `false` |

說明：

- 同時提供 `ore` 和 `id` 時，優先使用 `ore`；若安裝了 GregTech，選取的結果還會先經過 `GTOreDictUnificator.setStack(...)` 統一化
- 若省略 `meta`，且 `ore` 解析出的物品攜帶具體且非通配符的 damage，則會先使用該值，再回退到 `facing`
- 若省略 `meta`，部分方塊會依據 `facing` 推導出合理預設值
- 若 `nbt` 能成功建立 TileEntity，預覽中會使用該實體
- 當 GregTech 控制器即使結構完整也需要保持未成型時，可設定 `formed={false}`

範例：

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

把外部結構檔載入到場景中。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `src` | 是 | 結構資源路徑 |
| `x` | 否 | 平移 X，整數（`offsetX` 的別名） |
| `y` | 否 | 平移 Y，整數（`offsetY` 的別名） |
| `z` | 否 | 平移 Z，整數（`offsetZ` 的別名） |
| `offsetX` | 否 | 平移 X，整數（優先於 `x`） |
| `offsetY` | 否 | 平移 Y，整數，會被限制在 `[0, 世界高度-1]`（優先於 `y`） |
| `offsetZ` | 否 | 平移 Z，整數（優先於 `z`） |
| `formed` | 否 | 是否讓導入結構中的控制器在預覽同步時依成型狀態處理；預設 `false` |

支援的格式：

- SNBT 文本
- gzip 壓縮的二進位 NBT
- 未壓縮的二進位 NBT

必須包含的結構鍵：

- `palette`
- `blocks`

範例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
<ImportStructure src="/assets/example_structure.snbt" formed={false} />
````

## `<ImportStructureLib>`

透過控制器 id 導入 StructureLib 多方塊預覽。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `controller` | 是 | 控制器方塊 id，格式為 `modid:block[:meta]` |
| `name` | 否 | 可選綁定名，供註解、模板和音效上的 `showWhenStructure` 使用 |
| `piece` | 否 | StructureLib piece 名稱覆蓋值 |
| `facing` | 否 | 傳給導入器的朝向覆蓋值 |
| `rotation` | 否 | 傳給導入器的旋轉覆蓋值 |
| `flip` | 否 | 傳給導入器的鏡像覆蓋值 |
| `channel` | 否 | 支援頻道結構的頻道整數覆蓋值 |
| `offsetX` | 否 | 所有放置方塊的 X 偏移，整數，預設 `0` |
| `offsetY` | 否 | 所有放置方塊的 Y 偏移，整數，會被限制在 `[0, 世界高度-1]`，預設 `0` |
| `offsetZ` | 否 | 所有放置方塊的 Z 偏移，整數，預設 `0` |
| `formed` | 否 | 是否讓導入的 StructureLib 控制器在預覽同步時按成型狀態處理；預設 `false` |

說明：

- 導入結構會從場景 `0 0 0` 開始；控制器不會被強制放在 `0 0 0`
- 若有足夠元數據，該標籤會啟用 StructureLib 專用 tooltip、艙口高亮和頻道滑桿 UI
- 控制器匹配支援 GTNH 風格 `modid:block:meta`
- 當場景裡有多個 StructureLib 導入，而且其他標籤需要只針對其中一個結構狀態時，請明確提供 `name`
- `facing`、`rotation` 與 `flip` 使用和 StructureLib 導出一致的朝向詞彙；若請求的組合不被控制器允許，GuideNH 會自動回退到第一個有效對齊
- GregTech 控制器預覽預設朝向現在會相對舊預覽方向繞 Y 軸旋轉 180 度，也就是預設顯示為舊朝向的背面
- 若希望導入後的 GregTech 控制器保持未成型，請設定 `formed={false}`；這比故意提供殘缺 NBT 更穩定

範例：

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
<ImportStructureLib controller="gregtech:gt.blockmachines:15411" formed={false} />
````

依結構狀態控制註解和音效的範例：

````md
<GameScene interactive={true}>
  <ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
  <ImportStructureLib name="aux" controller="gregtech:gt.blockmachines:15412" />

  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="2..4,!3"
    showWhenChannels="input:1..3, casing:!2"
  >
    只会在匹配的 `main` 状态下显示。
  </BlockAnnotation>

  <PlaySound
    sound="guidenh:machine.start"
    trigger="click"
    showWhenStructure="aux"
    showWhenTier="1..2"
  />
</GameScene>
````

StructureLib 預設值也可以寫成子標籤。這些預設值會成為場景的初始互動狀態，
所以使用者拖曳 tier 或 channel 滑條後，點選重設檢視按鈕會回到這些預設值。

| 子標籤 | 意義 |
| --- | --- |
| `<Tier value="1" />` | 主 tier 值。 |
| `<Channel name="channelName" value="1" />` | 具名 StructureLib channel 覆蓋值；可重複。 |
| `<Facing value="north" />` | 預設朝向。 |
| `<Rotation value="normal" />` | 默认旋转。 |
| `<Flip value="none" />` | 默认翻转 / 镜像。 |
| `<Orientation value="north:normal:none" />` | 在一個標籤內同時指定朝向、旋轉、翻轉。 |
| `<GregTechActiveController />` | 僅 GregTech：盡可能使用機器啟動狀態的控制器貼圖。 |
| `<GregTechPlaceHatches />` | 僅 GregTech：為僅允許倉室的位置放置真實 GT 倉室；省略時仍會使用 GT survival preview，但空倉室位置默認回退為外殼。 |

對 GregTech 控制器，GuideNH 現在會使用與匯出指令一致的 StructureLib survival preview 路徑。
這可以修復普通 `construct()` 無法填充的倉室專用位置，同時預設保留外殼 fallback 行為。

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:1000">
    <Tier value="4" />
    <Channel name="voltage" value="4" />
    <Facing value="north" />
    <Rotation value="normal" />
    <Flip value="none" />
    <GregTechActiveController />
    <GregTechPlaceHatches />
  </ImportStructureLib>
</GameScene>
````

## `<IsometricCamera>`

明確指定等軸相機的 yaw/pitch/roll。

若省略該標籤，場景會繼續使用 `<GameScene>` 的 `perspective` 預設。預設的
`isometric-north-east` 預設等價於：

````md
<IsometricCamera yaw="225" pitch="30" />
````

| 屬性 | 意義 |
| --- | --- |
| `yaw` | float |
| `pitch` | float |
| `roll` | float |

範例：

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

移除所有已放置且符合目標方塊 id 的方塊。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `id` | 是 | 要移除的方塊 id，使用 `modid:block[:meta]` 格式 |

這在導入結構後非常有用，適合為了展示清晰度而隱藏某些方塊。

範例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<ReplaceBlock>`

將已放置且符合來源方塊 id（以及可選的 TileEntity NBT 部分匹配）的方塊替換為新方塊。
搜尋範圍可以是全域（所有已填滿方塊），也可以限定於一個軸對齊的包圍盒。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `from` | 是 | 要符合的來源方塊，使用 `modid:block[:meta]` 格式 |
| `from_nbt` | 否 | 部分 SNBT 複合標籤；僅當方塊的 TileEntity NBT 包含所有列出的鍵時才匹配 |
| `to` | 是 | 取代目標方塊，使用 `modid:block[:meta]` 格式 |
| `to_nbt` | 否 | 套用於替換方塊的 SNBT TileEntity 複合標籤 |
| `x` | 否 | 包围盒起始 X；只要 `x/y/z/dx/dy/dz` 中任意一个存在，就启用包围盒模式 |
| `y` | 否 | 包圍盒起始 Y |
| `z` | 否 | 包圍盒起始 Z |
| `dx` | 否 | 包围盒长度，沿 X 轴（默认 `1`）|
| `dy` | 否 | 包圍盒高度，沿 Y 軸（預設 `1`）|
| `dz` | 否 | 包圍盒寬度，沿 Z 軸（預設 `1`）|
| `formed` | 否 | 是否讓替換結果中的控制器在預覽同步時依成型狀態處理；預設 `false` |

說明：

- 若 `x/y/z/dx/dy/dz` 均未提供，则全局扫描所有已填充方块
- `from_nbt` 为**部分**匹配：仅检查模式中列出的键，TileEntity 中额外的键将被忽略
- 替换使用与 `<Block>` 相同的方块放置流程，支持 GregTech MetaTile 及 BartWorks 方块
- 若替换结果放入了控制器，`formed={false}` 可让该控制器在预览中保持未成型

範例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

用單一方塊類型填滿一個軸對齊的包圍盒，覆蓋原有方塊。
與 `<Block>`（針對單一位置）不同，`<PlaceBlock>` 透過 `dx`/`dy`/`dz` 支援多方塊區域，對應順序 X/Y/Z 軸上的長寬、高、軸上的長度 X/Y/Z 順序。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `id` | 是 | 方塊 id，使用 `modid:block[:meta]` 格式 |
| `nbt` | 否 | 套用於每個放置方塊的 SNBT TileEntity 複合標籤 |
| `x` | 否 | 區域起始 X，預設 `0` |
| `y` | 否 | 區域起始 Y，預設 `0` |
| `z` | 否 | 區域起始 Z，預設 `0` |
| `dx` | 否 | 區域長度，沿 X 軸，預設 `1` |
| `dy` | 否 | 區域高度，沿 Y 軸，預設 `1` |
| `dz` | 否 | 區域寬度，沿 Z 軸，預設 `1` |
| `formed` | 否 | 是否讓放置出的控制器在預覽同步時依成型狀態處理；預設 `false` |

說明：

- 包圍盒內所有方塊都會被無條件放置（不檢查原有方塊）
- NBT 複合標籤在每次放置時獨立複製
- 使用與 `<Block>` 相同的方塊放置流程，完整支援 GregTech MetaTile 及 BartWorks 方塊
- 若區域內放置了一個或多個控制器，`formed={false}` 會讓這些控制器都保持未成型

範例：

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
<PlaceBlock id="gregtech:gt.blockmachines:15411" dx="3" dz="3" formed={false} />
````

## `<BlockAnnotationTemplate>`

將一個或多個子註解擴展到目前場景中所有已經存在的符合方塊上。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `id` | 是 | 方塊匹配器，格式為 `modid:block[:meta]` |

規則：

- 應將其放在待匹配方塊或導入結構之後
- 匹配發生在解析時，針對的是當前場景狀態
- 子註解使用相對於每個匹配方塊的局部座標

範例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

在預覽場景中加入實體。

這些屬性遵循類似 summon 的實體放置與 SNBT 資料語意。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `id` | 是 | 實體類型 id；支援舊式名稱如 `Sheep`、現代原版 id 如 `minecraft:sheep`，也支援 `modid.entityName` 或 `modid:entityName` 形式的模組實體 id |
| `x` | 否 | 實體中心點 X，float，預設 `0.5` |
| `y` | 否 | 實體底部 Y，float，預設 `0` |
| `z` | 否 | 實體中心點 Z，float，預設 `0.5` |
| `rotationY` | 否 | yaw，角度，預設 `-45` |
| `rotationX` | 否 | pitch，角度，預設 `0` |
| `data` | 否 | summon 風格 SNBT，會在生成前併入實體 NBT |
| `sceneEntityId` | 否 | 穩定的場景內實體 id，供後續 `<Entity>` / `<RemoveEntity>` 操作以及導入場景快照恢復時引用 |
| `mount` | 否 | 此實體生成後要騎乘的目標載具穩定 `sceneEntityId` |
| `unmount` | 否 | 布林表達式，產生後或狀態重播時清除該實體目前的穩定騎乘關係 |
| `name` | 否 | 当 `id` 是 `player`、`fakeplayer`、`minecraft:player` 或 `minecraft:fakeplayer` 时使用的预览玩家名 |
| `uuid` | 否 | 使用上述玩家 id 時的預覽玩家 UUID |
| `showName` | 否 | 控制預覽玩家頭頂名牌的布林表達式；對玩家預覽 id 預設 `true` |
| `showCape` | 否 | 控制預覽玩家披風顯示的布林表達式；對玩家預覽 id 預設 `true` |
| `headRotation` | 否 | 預覽玩家頭部旋轉，格式為 `x y z` 角度 |
| `leftArmRotation` | 否 | 預覽玩家左臂旋轉，格式為 `x y z` 角度 |
| `rightArmRotation` | 否 | 預覽玩家右臂旋轉，格式為 `x y z` 角度 |
| `leftLegRotation` | 否 | 預覽玩家左腿旋轉，格式為 `x y z` 角度 |
| `rightLegRotation` | 否 | 預覽玩家右腿旋轉，格式為 `x y z` 角度 |
| `capeRotation` | 否 | 預覽玩家披風旋轉，格式為 `x y z` 角度；預設靜止站立角度 `6 0 0` |

說明：

- 實體包圍盒會參與場景自動居中與可見層篩選
- 當預覽世界尚未準備好時，實體創建會優雅回退，並在首次渲染時綁定
- `sceneEntityId` 不是必填，但只要後續要刪除、重建、重新騎乘或從快取恢復同一個邏輯實體，強烈建議填寫
- 同一個 `sceneEntityId` 可以關聯多個運行時實體；`<RemoveEntity sceneEntityId="..."/>` 會一起移除目前已登記到該穩定 id 的全部實體
- `mount` 透過穩定場景 id 建立騎乘關係，而不是依賴原始 NBT 乘客鏈，因此回放、導入匯出、預覽重建和 Ponder 拖曳時間軸時都能穩定恢復
- `unmount={true}` 會先清除該實體目前的穩定騎乘關係，再應用後續新的騎乘關係
- 玩家預覽 id 會建立客戶端側 fake remote player，以重複使用一般玩家渲染器和皮膚管線
- 若玩家預覽未提供 `name` 也未提供 `uuid`，GuideNH 會回退到 `Steve` 和原始預設皮膚
- 只提供 `name` 時，GuideNH 會先嘗試解析真實在線 profile 以加載皮膚和披風；失敗時回退到穩定離線 UUID
- 只提供 `uuid` 時，GuideNH 會產生佔位顯示名，並繼續嘗試從 profile 解析皮膚
- `showName={false}` 會隱藏頭頂名稱，但仍走普通玩家渲染路徑
- `showCape={false}` 會隱藏披風，同時仍保留一般玩家渲染路徑和 Forge hook
- 玩家姿態屬性使用三個以空格分隔的浮點數，對應模型 `X Y Z` 角度
- 若省略頭部/四肢旋轉，沿用原版 idle 姿態；若省略 `capeRotation`，回退到站立靜止披風角度 `6 0 0`
- 玩家預覽在解析時需要活動的客戶端世界，因為 Minecraft 的玩家實體構造器無法脫離 world 創建
- 懸停實體時會顯示其本地化顯示名；若提供了自訂名，則顯示自訂名

範例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

穩定 id 騎乘範例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
</GameScene>
````

取消騎乘與刪除範例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
  <Entity id="player" x="3" y="1" sceneEntityId="rider" unmount={true} name="GuideNH" />
  <RemoveEntity sceneEntityId="horse" />
 </GameScene>
 ````

嬰兒實體範例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>
````

预览玩家姿态示例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity
    id="player"
    y="1"
    name="ArtherSnow"
    headRotation="0 20 0"
    rightArmRotation="-35 0 0"
    leftArmRotation="10 0 -12"
    rightLegRotation="8 0 0"
    leftLegRotation="-8 0 0"
    capeRotation="12 0 0"
  />
</GameScene>
````

預覽玩家名稱與披風範例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## `<RemoveEntity>`

依穩定 `sceneEntityId` 移除目前已登記至該 id 的全部執行時間實體。

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `sceneEntityId` | 是 | 需要移除的穩定場景實體 id |
| `unmount` | 否 | 在移除前先清除穩定騎乘關係的布林運算式 |

說明：

- 這是場景標籤側對應 Ponder `removeEntities` 的刪除能力
- 刪除走穩定 id 索引，不需要每幀全量掃描全部實體
- 如果多個導入或重播的實體共用同一個 `sceneEntityId`，會一起移除

範例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:pig" y="1" sceneEntityId="demoPig" />
  <RemoveEntity sceneEntityId="demoPig" />
</GameScene>
````

## Weather

`<Weather>` 可直接為 `GameScene` 加入動畫雨雪。與 Ponder 的天氣預設不同，場景天氣不歸時間軸管理：
它會在普通場景渲染時持續循環，不帶淡入淡出，也不能單獨暫停或拖曳。底層渲染仍復用與 Ponder
天氣相同的降水幾何路徑，因此本地預覽與 site export 的效果保持一致。

| 屬性 | 預設值 | 說明 |
| --- | --- | --- |
| `weather` / `type` | `rain` | 天氣類型。支援 `rain`、`snow`。 |
| `x`、`z` | 場景邊界 | 覆蓋的降水列。單值表示一列；陣列以端點對定義一個或多個矩形區域。 |
| `density` | 依類型決定 | 覆蓋密度。數值越高，保留的降水列越多；數值越低，效果越稀疏。 |

說明：

- `<Weather>` 不使用 `y`；垂直範圍由當前場景邊界以及每一列中最高的降水遮擋方塊共同推導。
- 如果某一軸陣列結尾有無法配對的多餘值，這部分會被忽略。
- 在同一個天氣聲明內，同一個 `x/z` 列不會疊加雨和雪。多個天氣標籤發生重疊時，前面聲明的標籤優先佔用共用欄位。
- 同一個 `GameScene` 中，不同且不重疊的列可以同時渲染雨和雪。

範例：

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
````

## 相機中心行為

若未明確提供 `centerX/Y/Z`，GuideNH 會根據已放置方塊的包圍盒自動居中場景。若設定了任意一個顯式中心座標，則自動居中會被停用，未提供的其餘座標預設 `0`。

## 互動說明

當 `interactive={true}` 時，場景支援旋轉、平移、縮放、重設、註解開關，以及指南介面暴露出的其他互動控制。

- 跨越多個 Y 層的場景會在底部上方顯示可見層滑桿
- 若 StructureLib 元資料提供相關訊息，場景底部也可能出現艙口高亮切換按鈕和頻道滑桿
- 註解懸停優先權高於方塊懸停；當沒有懸停註解熱點時，方塊 tooltip 會正常顯示
- StructureLib 懸停會把方塊名稱放在 tooltip 第一行，把結構專用文字放在第二行開始；按住 `Shift` 時還會展開替換候選項

## 相關頁面

- [註](Annotations)
- [結構導出](Structure-Export)
- [配方](Recipes)
- [範例](Examples)
