# 思索動畫時間軸

GuideNH 在遊戲場景（`<GameScene>`）塊中支援思索風格的動畫時間軸。你只需提供一個外部 JSON 檔案來定義關鍵幀、攝像機運動和世界內註釋，GuideNH 就會在 3D 場景下方渲染一個帶播放/暫停控制元件的互動式進度條。

## 快速上手

1. 建立一個思索 JSON 檔案，並將其放入資源包（參見[檔案位置](#檔案位置)）。
2. 在 guide 頁面的遊戲場景（`<GameScene>`）塊中，與 `<ImportStructure>` 並列新增 `<ImportPonder src="..."/>`。

```mdx
<GameScene zoom="4">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **注意：** `<ImportPonder>` 必須位於遊戲場景（`<GameScene>`）塊內，`src` 屬性為必填項。結構資料仍由 `<ImportStructure>` 或 `<ImportStructureLib>` 提供。

## 檔案位置

思索 JSON 檔案遵循與 SNBT 結構檔案相同的資源包路徑規則：

```
assets/<modid>/guidenh/
  _en_us/machines/my_machine.md       ← guide 页面
  _en_us/machines/my_machine.snbt      ← 结构数据
  _en_us/machines/my_machine.json      ← 思索 JSON
```

`src` 屬性支援相對路徑和絕對 ID：

| 示例 | 解析方式 |
|------|----------|
| `src="my_machine.json"` | 相對於當前頁面目錄 |
| `src="mymod:guidenh/_en_us/machines/my_machine.json"` | 絕對路徑（`mymod` 名稱空間） |

## JSON 格式

根物件包含兩個必填欄位：

### 根欄位

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `totalTime` | 整數 | 是 | 動畫總時長（20 刻 = 1 秒），最小為 1。 |
| `keyframes` | 陣列 | 是 | 關鍵幀物件列表。可為空陣列。 |

### JSON Schema 校驗

在 JSON 根物件加入 `"$schema": "guidenh://schema/ponder.json"`，即可在 VS Code、IDEA 和其他支援 JSON Schema 的編輯器中啟用離線補全與校驗。GuideNH 模組內建檔案路徑為 `assets/guidenh/schema/ponder_schema.json`，Wiki 示例資源包也提供一份副本；兩者都不訪問遠端 URL。

### 關鍵幀欄位

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `time` | 整數 | 是 | 該關鍵幀所在刻（0 ≤ time ≤ totalTime）。 |
| `hidden` | 布林值 | 否 | 為 `true` 時，該關鍵幀仍會在對應 `time` 應用相機/NBT/實體/註釋狀態，但進度條不會繪製可見節點，上一關鍵幀這類可見節點導航也會跳過它。 |
| `label` | 字串 | 否 | 懸停在進度條節點上時顯示的標籤及方向箭頭。 |
| `labelKey` | 字串 | 否 | 關鍵幀標籤的翻譯鍵。解析成功時會覆蓋 `label`。 |
| `camera` | 物件 | 否 | 該關鍵幀的攝像機狀態，預設欄位從前一關鍵幀繼承。 |
| `cameraEaseTicks` | 整數 或 null | 否 | 攝像機從**上一個**關鍵幀的位置緩動到當前關鍵幀的時間（刻數）。`null`（預設）= 在整個片段內緩動；`0` = 立即跳轉；`N > 0` = 在 N 刻內緩動，之後保持目標位置。 |
| `layer` | 整數 或 null | 否 | 可見層覆蓋。`null` 顯示所有層；從 1 開始的整數限制到指定層。 |
| `annotations` | 陣列 | 否 | 該關鍵幀啟用時顯示的註釋列表。 |
| `sounds` | 陣列 | 否 | 正向播放時，關鍵幀變為啟用狀態後播放一次的音效列表。 |
| `particles` | 陣列 | 否 | 在正向播放進入該關鍵幀時觸發的執行時粒子突發或預設列表。 |
| `blockChanges` | 陣列 | 否 | 該關鍵幀啟用時執行的方塊替換列表。 |
| `mergeTileNBT` | 陣列 | 否 | 將 SNBT 複合標籤合併到指定座標的方塊實體。 |
| `modifyTileNBT` | 陣列 | 否 | 將某個方塊實體 NBT 路徑設定為指定 SNBT 值。 |
| `removeTileNBT` | 陣列 | 否 | 刪除某個方塊實體 NBT 路徑。 |
| `createEntities` | 陣列 | 否 | 建立可被後續實體 NBT 操作引用的思索專用實體。 |
| `setEntityNBT` | 陣列 | 否 | 用提供的 SNBT 複合標籤替換引用實體的 NBT。 |
| `mergeEntityNBT` | 陣列 | 否 | 將 SNBT 複合標籤合併到引用實體。 |
| `modifyEntityNBT` | 陣列 | 否 | 將引用實體的某個 NBT 路徑設定為指定 SNBT 值。 |
| `removeEntityNBT` | 陣列 | 否 | 刪除引用實體的某個 NBT 路徑。 |
| `removeEntities` | 陣列 | 否 | 透過穩定場景實體登錄檔，按 `ref` 刪除一個或多個思索時間軸實體。 |

隱藏關鍵幀適合做“中間狀態但不加節點”的場景。例如你可以把多次 `modifyTileNBT` 分散到不同 tick，
並把中間關鍵幀標記為隱藏，這樣進度條上仍只保留主要節奏點。

### 攝像機欄位（均為可選）

| 欄位 | 說明 |
|------|------|
| `zoom` | 攝像機縮放（0.1 – 10.0）。 |
| `rotX` | X 軸旋轉角度（度）。 |
| `rotY` | Y 軸旋轉角度（度）。 |
| `rotZ` | Z 軸旋轉角度（度）。 |
| `offX` | 水平平移偏移（螢幕畫素）。 |
| `offY` | 垂直平移偏移（螢幕畫素）。 |

相鄰關鍵幀之間的攝像機使用**緩入/緩出**曲線平滑插值。使用目標關鍵幀的 `cameraEaseTicks` 欄位可以控制緩動時長：

```json
{ "time": 60,  "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   ← 立即跳转
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  ← 在 20 刻内缓动后保持
{ "time": 180, "camera": { "rotY": 270 } }                         ← 在整个片段内缓动（默认）
```

## 方塊變化（blockChanges）

關鍵幀的 `blockChanges` 陣列可在該關鍵幀啟用時替換結構中的方塊。這讓動畫可以呈現前/後對比、新增或移除方塊，或演示機器開機效果。

```json
{
  "time": 60,
  "blockChanges": [
    { "x": 1, "y": 1, "z": 1, "block": "minecraft:lit_furnace", "meta": 4, "particles": true },
    { "x": 1, "y": 2, "z": 1, "block": "minecraft:air", "particles": false },
    {
      "x": 2, "y": 1, "z": 2, "block": "minecraft:chest", "meta": 2,
      "nbt": "{Items:[{Slot:0b,id:\"minecraft:iron_ingot\",Count:8b,Damage:0s}]}"
    }
  ]
}
```

| 欄位 | 型別 | 預設值 | 說明 |
|------|------|--------|------|
| `x`, `y`, `z` | 整數 | — | **必填。** 要修改的方塊座標（結構座標系）。 |
| `block` | 字串 | — | **必填。** 註冊名，如 `"minecraft:furnace"`。使用 `"minecraft:air"` 表示移除方塊。 |
| `meta` | 整數 | `0` | 方塊後設資料（傷害值）。 |
| `particles` | 布林值 | `true` | 在正向播放中觸發該方塊變化時，是否生成基於方塊自身貼圖的粒子效果。設為 `false` 可抑制視覺效果（例如靜默移除方塊）。 |
| `nbt` | 字串 | `null` | 方塊實體的 SNBT 標籤字串，用於箱子、熔爐等。透過 `JsonToNBT` 解析。鍵名必須使用**不帶引號**的標準 SNBT 格式；字串值仍需引號。若方塊沒有方塊實體則忽略該欄位。 |

**支援倒放定位：** 向前或向後拖動進度條時，執行時會先將所有改動過的位置恢復為原始狀態，再從第 0 幀重新應用到當前幀。無論如何定位，顯示的結構始終正確。

> **關於粒子效果：** 粒子效果只在正向播放、關鍵幀第一次啟用時觸發，粒子使用被替換方塊的自身貼圖。定位（倒帶/快進）、重置或初始載入時粒子**不會**觸發，已有粒子會被清除。

---

## 關鍵幀音效

在關鍵幀中新增 `sounds` 陣列，可以讓一個或多個指南音效在該關鍵幀正向播放並變為啟用狀態時播放。
定位和初始載入不會播放關鍵幀音效；重新開始播放會清除播放記錄，使音效可以再次觸發。

```json
{
  "time": 130,
  "label": "熔炉点燃",
  "sounds": [
    { "sound": "guidenh:machine.start", "volume": 0.8 },
    { "src": "guidenh:sounds/machine/hum.ogg", "volume": 0.4, "x": 1.5, "y": 1.5, "z": 1.5 }
  ]
}
```

音效欄位：

| 欄位 | 型別 | 預設值 | 說明 |
|------|------|--------|------|
| `sound` | 字串 | — | 音效事件 id，例如 `guidenh:machine.start`。 |
| `src` | 字串 | — | 音效檔案 id 或路徑；`guidenh:sounds/machine/start.ogg` 會轉換為 `guidenh:machine.start`。 |
| `volume` | 浮點數 | `1.0` | 衰減前的播放音量。 |
| `pitch` | 浮點數 | `1.0` | 播放音高。 |
| `cooldown` | 整數 | `250` | 同一音效再次播放前的最小間隔毫秒數。 |
| `x`, `y`, `z` | 浮點數 | 無 | 可選場景空間音源位置，用於螢幕空間衰減。 |
| `radius` | 浮點數 | 場景較短邊 * 0.75 | 螢幕畫素中的衰減半徑。 |
| `minVolume` | 浮點數 | `0.15` | 最小衰減係數。 |

---

## 關鍵幀粒子

在關鍵幀中新增 `particles` 陣列，可以讓關鍵幀在正向播放時生成一次性粒子突發，或生成僅在該時間軸內生效的天氣覆蓋效果。
這些粒子在倒著拖動時間軸時不會重新補播；定位或重新開始播放時，會先清除已有粒子，再恢復到當前狀態。

普通粒子：

```json
{
  "time": 110,
  "particles": [
    {
      "name": "smoke",
      "x": 1.5,
      "y": 1.85,
      "z": 1.5,
      "vx": 0.0,
      "vy": 0.01,
      "vz": 0.0,
      "size": 0.18,
      "time": 16,
      "amount": 3
    }
  ]
}
```

爆炸預設：

```json
{
  "time": 160,
  "particles": [
    {
      "preset": "explosion",
      "x": 1.5,
      "y": 1.45,
      "z": 1.5,
      "time": 8,
      "power": 2.4
    }
  ]
}
```

天氣預設：

```json
{
  "time": 220,
  "particles": [
    {
      "preset": "rain",
      "weather": "snow",
      "x": [0, 2],
      "z": [0, 2],
      "time": 100,
      "amount": 8
    }
  ]
}
```

粒子欄位：

| 欄位 | 型別 | 預設值 | 說明 |
|------|------|---------|------|
| `preset` | 字串 | 無 | 特殊預設。`explosion` 會生成接近原版的閃光與煙霧爆發。`rain` 用於天氣預設入口。 |
| `weather` | 字串 | `rain` | 僅用於 `preset: "rain"`。支援 `rain` 和 `snow`。 |
| `name` | 字串 | 無 | 普通粒子外觀。支援 `billboard`、`smoke`、`largesmoke`、`explode`、`flash`、`largeexplode`、`hugeexplosion`。 |
| `particle` / `kind` | 字串 | 無 | `name` 的相容別名。 |
| `x`、`z` | 浮點數或陣列 | 場景邊界 | 粒子的世界座標或天氣覆蓋範圍。普通粒子使用單值座標。對 `preset: "rain"`，單值表示單個降水列，陣列按端點對定義矩形覆蓋區域。 |
| `vx`、`vy`、`vz` | 浮點數 | `0.0` | 初速度向量。`motionX/Y/Z` 也可作為別名。 |
| `time` / `lifetime` | 整數 | 依預設而定 | 粒子生命週期，單位為 tick。對 `preset: "rain"` 而言，這裡表示包含開始和結束過渡在內的總天氣時長。 |
| `size` | 浮點數 | 依預設而定 | 普通粒子的半尺寸，單位為方塊。 |
| `amount` | 整數 | 依預設而定 | 普通粒子的生成數量。對 `explosion`，省略時會根據 `power` 自動縮放；對 `preset: "rain"`，這裡表示平均每 tick 的天氣密度。 |
| `power` | 浮點數 | `2.0` | `explosion` 預設的爆炸強度。 |

天氣預設說明：

- `preset: "rain"` 是雨雪共用的天氣預設入口。
- 使用 `weather: "rain"` 生成接近原版正常世界的下雨效果，並帶有少量落地水花。
- 使用 `weather: "snow"` 生成更慢、更輕的飄雪效果。
- 該預設歸時間軸管理，會和 Ponder 的播放、暫停、跳轉、快進一起回放。
- 如果需要不依賴 Ponder 時間軸、始終迴圈的場景天氣，請改用 `<GameScene>` 內的 `<Weather>` 標籤。
- 天氣預設不使用 `y`；垂直生成範圍始終由當前場景邊界自動推導。
- `x: 5, z: 8` 表示單個降水列。陣列按連續兩項組成端點對：
  `x: [1, 5, 10, 12], z: [2, 6, 20, 24]` 表示兩個矩形覆蓋區域。
- 如果某一軸陣列末尾有多出來但無法完整配對的值，這部分會被忽略。
- 執行時會自動補上短暫的開始過渡、穩定段和結束過渡。
- 天氣覆蓋區域會根據當前 `GameScene` 的結構邊界自動推導，而不是寫死一個固定盒子。
- 同一個 `x/z` 列在同一時段不會疊加多種天氣；前面宣告的重疊天氣會優先佔用共享列。

---

## 方塊實體 NBT 操作

當方塊本身不變，只需要改變方塊實體資料時，可以使用 `mergeTileNBT`、`modifyTileNBT`
和 `removeTileNBT`。這些操作支援進度條定位：執行時會先恢復初始方塊實體 NBT，再從第
0 幀重放到當前關鍵幀。

```json
{
  "time": 80,
  "mergeTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "nbt": "{InputTanks:[{Level:{Speed:0.25,Target:0.25,Value:0.0},TankContent:{Amount:250,FluidName:\"minecraft:lava\"}}]}"
    }
  ],
  "modifyTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "path": "InputTanks[0].TankContent.Amount",
      "value": "500"
    }
  ],
  "removeTileNBT": [
    { "x": 2, "y": 1, "z": 2, "path": "InputTanks[0].Level.Target" }
  ]
}
```

| 欄位 | 適用於 | 說明 |
|------|--------|------|
| `x`, `y`, `z` | 全部 | 方塊實體所在的結構座標。 |
| `nbt` | `mergeTileNBT` | 要合併到方塊實體的 SNBT 複合標籤。複合標籤會遞迴合併，其他值會覆蓋舊值。 |
| `path` | `modifyTileNBT`、`removeTileNBT` | 帶列表索引的點分 NBT 路徑，例如 `Items[0].Count` 或 `InputTanks[0].TankContent.Amount`。 |
| `value` | `modifyTileNBT` | 寫入 `path` 的 SNBT 值，例如 `3b`、`500`、`"\"text\""`、`{Count:1b,id:"minecraft:stone"}`。 |

路徑寫法與 Minecraft `/data` 的思路相同：用 `.` 進入複合標籤，用 `[index]` 進入列表。
列表遍歷目前面向常見的“列表內是複合標籤”的結構，例如物品欄、流體罐和配方槽。

---

## 實體操作

使用 `<Entity>` 在場景中放置實體，或透過思索時間軸的 `createEntities` 動作建立實體。
時間軸建立的實體可在後續關鍵幀中透過 `ref` 引用。

```json
{
  "time": 0,
  "createEntities": [
    {
      "ref": "marker",
      "sceneEntityId": "marker",
      "id": "minecraft:pig",
      "x": 1.5, "y": 1.0, "z": 2.5,
      "yaw": 180,
      "nbt": "{CustomName:\"Before\",CustomNameVisible:1b}"
    }
  ]
}
```

| 欄位 | 說明 |
|------|------|
| `ref` | 必填，本思索 JSON 內用於後續操作的引用名。 |
| `sceneEntityId` | 可選，穩定的場景內實體 id，用於騎乘關係、可重放替換、匯入匯出恢復，以及後續刪除同一個邏輯實體。預設會基於 `ref` 生成內部穩定 id。 |
| `id` | 實體 ID，例如 `minecraft:pig`、`Pig` 或場景實體載入器支援的模組實體 ID。 |
| `x`, `y`, `z` | 可選生成位置。未填寫且 `nbt` 沒有 `Pos` 時預設為 `0, 0, 0`。 |
| `yaw`, `pitch` | 可選生成旋轉。未填寫且 `nbt` 沒有 `Rotation` 時預設為 `0, 0`。 |
| `bodyYaw`, `headYaw` | 可選的生物身體 / 頭部 yaw 覆蓋；若只寫 `yaw`，未寫這兩個欄位時會預設跟隨 `yaw`。 |
| `nbt` | 可選，實體建立時應用的 SNBT 複合標籤。 |
| `name`, `nameKey`, `uuid` | 可選，建立預覽玩家實體時使用的玩家檔案欄位。`nameKey` 解析成功時覆蓋 `name`，找不到翻譯時回退到 `name`。 |
| `mount` | 可選，該實體建立後或後續重放時要騎乘的目標載具穩定 `sceneEntityId`。 |
| `unmount` | 可選，布林值；在後續新的 `mount` 應用前，先清除該實體當前穩定騎乘關係。 |

實體建立後，可以使用實體 NBT 操作：

```json
{
  "time": 60,
  "mergeEntityNBT": [
    { "ref": "marker", "nbt": "{Saddle:1b}" }
  ],
  "modifyEntityNBT": [
    { "ref": "marker", "path": "CustomName", "value": "\"After\"" }
  ],
  "removeEntityNBT": [
    { "ref": "marker", "path": "CustomNameVisible" }
  ]
}
```

如果需要替換實體 NBT，而不是合併，可以使用 `setEntityNBT`：

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

實體動作也可以在不改 NBT 的情況下直接調整位置、預覽玩家姿態和穩定騎乘關係：

```json
{
  "time": 80,
  "createEntities": [
    { "ref": "cart", "sceneEntityId": "cart", "id": "minecraft:minecart", "x": 1.5, "y": 1.0, "z": 1.5 },
    { "ref": "rider", "sceneEntityId": "rider", "id": "player", "name": "GuideNH", "x": 1.5, "y": 1.0, "z": 1.5, "mount": "cart" }
  ],
  "modifyEntityNBT": [
    { "ref": "rider", "headYaw": 270.0, "leftArmRotation": "-40 0 0" }
  ]
}
```

若需要取消騎乘或刪除時間軸實體，可以使用 `unmount` 或 `removeEntities`：

```json
{
  "time": 120,
  "modifyEntityNBT": [
    { "ref": "rider", "unmount": true, "x": 2.5, "y": 1.0, "z": 1.5 }
  ],
  "removeEntities": [
    { "ref": "cart" }
  ]
}
```

與方塊實體操作一樣，實體操作會在關鍵幀變化時從頭重放；向後拖動進度條時，
思索時間軸建立的實體會被移除並重新建立到目標時刻的正確狀態。

說明：

- `ref` 用來定位當前要修改或刪除的思索時間軸實體。
- `sceneEntityId` 和 `mount` 用來描述穩定的跨實體關係；`mount` 指向的始終是穩定場景 id，而不是另一個 `ref`。
- 不建議只依賴原始乘客 NBT 來表達跨實體場景關係。真正保證回放、重建、匯入匯出和編輯器預覽重新整理後仍然穩定一致的是這套穩定登錄檔。

---

## 註釋淡入動畫

當播放過程中活躍關鍵幀切換時，疊加類註釋（`text`、`input`）會在 **5 個遊戲刻**（250 ms）內平滑淡入。暫停或拖動定位時，註釋始終以完全不透明度立即顯示。

---

## 註釋型別

每個 Ponder 註釋都支援 `tooltip` 和 `tooltipKey`。`tooltipKey` 會從當前語言檔案中解析；找不到翻譯時回退到原始 `tooltip` 文字。
每個註釋條目需要一個 `type` 欄位，共支援七種型別。

---

### `diamond` — 菱形標記

在世界座標渲染一個 3D 菱形標記。

```json
{
  "type": "diamond",
  "x": 1.5,
  "y": 2.0,
  "z": 1.5,
  "color": "0xFFFF8800",
  "tooltip": "点击此处",
  "alwaysOnTop": false
}
```

| 欄位 | 型別 | 預設值 | 說明 |
|------|------|--------|------|
| `x`, `y`, `z` | float | `0.0` | 菱形尖端的世界座標。 |
| `color` | 字串 | `"0xFF00E000"` | ARGB 顏色，格式為 `"0xAARRGGBB"`。 |
| `tooltip` | 字串 | `""` | 懸停時顯示的回退文字。 |
| `tooltipKey` | 字串 | `""` | 懸停提示的翻譯鍵。解析成功時會覆蓋 `tooltip`。 |
| `alwaysOnTop` | 布林值 | `false` | 為 true 時穿透方塊渲染。 |

---

### `box` — 線框盒子

從 `min` 到 `max` 渲染一個軸對齊線框盒子。

```json
{
  "type": "box",
  "minX": 0.0, "minY": 0.0, "minZ": 0.0,
  "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
  "color": "0x8800FFFF",
  "lineWidth": 1.5,
  "alwaysOnTop": false
}
```

| 欄位 | 型別 | 預設值 | 說明 |
|------|------|--------|------|
| `minX/Y/Z` | float | `0.0` | 最小角座標。 |
| `maxX/Y/Z` | float | `1.0` | 最大角座標。 |
| `color` | 字串 | `"0xFFFFFFFF"` | ARGB 線條顏色。 |
| `lineWidth` | float | 預設值 | GL 線寬。 |
| `alwaysOnTop` | 布林值 | `false` | 穿透方塊渲染。 |

---

### `block` - 整方塊線框

當你想在 Ponder JSON 中使用和普通 `<BlockAnnotation pos="x y z">` 一樣的整方塊標註時，使用 `block`。

```json
{
  "type": "block",
  "pos": [1, 0, 1],
  "color": "0xFFFF8800",
  "lineWidth": 1.5,
  "alwaysOnTop": true
}
```

`blockBox` 和 `block_box` 也可以作為別名。座標可以寫成 `pos: [x, y, z]`，也可以寫成
`x/y/z`，或相容舊風格的 `blockX/blockY/blockZ`。

| 欄位 | 型別 | 預設值 | 說明 |
|------|------|--------|------|
| `pos` | number[3] 或字串 | `[0, 0, 0]` | 方塊座標，格式為 `[x, y, z]` 或 `"x y z"`。 |
| `x`, `y`, `z` | number | `0` | 另一種方塊座標寫法，會向下取整。 |
| `blockX/Y/Z` | 整數 | `0` | 舊風格方塊座標欄位。 |
| `color` | 字串 | `"0xFFFFFFFF"` | ARGB 線框顏色。 |
| `lineWidth` | float | 預設值 | GL 線寬。 |
| `alwaysOnTop` | 布林值 | `false` | 穿透方塊渲染。 |

---

### `line` — 線段或折線

在兩個世界座標之間渲染一條線段，也可以透過 `points` 渲染多段折線。`points` 至少包含兩個有效點時，會優先於 `fromX/Y/Z` 和 `toX/Y/Z`。

```json
{
  "type": "line",
  "fromX": 0.5, "fromY": 0.5, "fromZ": 0.5,
  "toX": 2.5,   "toY": 0.5,   "toZ": 0.5,
  "color": "0xFFFFFF00",
  "arrow": "end",
  "lineWidth": 2.0,
  "alwaysOnTop": true
}
```

折線點可以寫成字串，也可以寫成陣列：

```json
{
  "type": "line",
  "points": [[0.5, 1.2, 0.5], [1.5, 1.8, 1.5], [2.5, 1.2, 2.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

| 欄位 | 型別 | 預設值 | 說明 |
|------|------|--------|------|
| `fromX/Y/Z` | float | `0.0` | 起點座標。 |
| `toX/Y/Z` | float | `1.0` | 終點座標。 |
| `points` | 字串或陣列 | `null` | 折線點。可寫 `"x y z; x y z; ..."` 或 `[[x,y,z], ...]`。 |
| `color` | 字串 | `"0xFFFFFFFF"` | ARGB 線條顏色。 |
| `arrow` | 字串 | `null` | `start` 或 `end`；省略或無法識別時不繪製箭頭。 |
| `lineWidth` | float | 預設值 | GL 線寬。 |
| `alwaysOnTop` | 布林值 | `false` | 穿透方塊渲染。 |

---

### `blockface` — 方塊面疊層

用半透明顏色疊層高亮顯示某個方塊的所有面。

```json
{
  "type": "blockface",
  "pos": [1, 0, 1],
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

`blockFace` 和 `block_face` 也可以作為別名。

| 欄位 | 型別 | 預設值 | 說明 |
|------|------|--------|------|
| `pos` | number[3] 或字串 | `[0, 0, 0]` | 方塊座標，格式為 `[x, y, z]` 或 `"x y z"`。 |
| `x`, `y`, `z` | number | `0` | 另一種方塊座標寫法，會向下取整。 |
| `blockX/Y/Z` | 整數 | `0` | 舊風格方塊座標欄位。 |
| `color` | 字串 | `"0x80FFFFFF"` | ARGB 疊層顏色。 |
| `alwaysOnTop` | 布林值 | `false` | 穿透方塊渲染。 |

---

### `text` — 文字氣泡

在世界座標附近渲染一個帶連線線的文字氣泡框，預設錨定到指定座標正上方。文字內容由關鍵幀驅動：如果想隨時間改變文字，在後續關鍵幀宣告新的 `text` 註釋即可。

```json
{
  "type": "text",
  "x": 1.5,
  "y": 2.5,
  "z": 1.5,
  "textKey": "guidenh.sample.scene.insert_items",
  "color": "0xFF44AAFF",
  "connectorSide": "right",
  "connectorOffset": 8,
  "connectorLength": 12
}
```

使用**獨立模式（independent）**可以在固定螢幕座標處顯示氣泡框（不跟隨世界座標投影）：

```json
{
  "type": "text",
  "text": "独立标签",
  "color": "0xFFFFCC00",
  "backgroundAlpha": 160,
  "independent": true,
  "yOffset": 40
}
```

| 欄位 | 型別 | 預設值 | 說明 |
|------|------|--------|------|
| `x`, `y`, `z` | float | `0.0` | 錨點的世界座標（獨立模式下忽略）。 |
| `text` | 字串 | — | 回退顯示文字。 |
| `textKey` | 字串 | — | 優先從資源包 `lang` 檔案解析的翻譯鍵；解析失敗時回退到 `text`。 |
| `color` | 字串 | `"0xFFAAAAAA"` | ARGB 邊框顏色。 |
| `backgroundAlpha` | 整數 | `204` | 背景透明度，`0` 為完全透明，`255` 為完全不透明。 |
| `maxWidth` | 整數 | `0` | 若 &gt; 0，按此畫素寬度自動換行；`0` 表示單行。 |
| `independent` | 布林值 | `false` | 若為 `true`，位置以場景中心為基準（螢幕座標），而非世界座標投影。 |
| `yOffset` | 整數 | `0` | 相對於場景垂直中心的畫素偏移（正值向下）。與 `independent: true` 配合使用。 |
| `connectorSide` | 字串 | `"bottom"` | `bottom`、`top`、`left`、`right` 或 `none`。獨立模式下忽略。 |
| `connectorOffset` | 整數 | `0` | 沿選定氣泡邊緣偏移連線點；top/bottom 正值向右，left/right 正值向下。 |
| `connectorLength` | 整數 | `6` | 連線線畫素長度；`0` 會隱藏連線線但保留按邊定位。 |
| `hlMinX/Y/Z` | float | `0.0` | 可選的伴生高亮框最小角座標。 |
| `hlMaxX/Y/Z` | float | `1.0` | 可選的伴生高亮框最大角座標。 |
| `highlightColor` | 字串 | `"0x8000FFAA"` | 高亮框顏色（存在 `hlMin/Max` 時自動建立一個 `InWorldBoxAnnotation`）。 |

當任意 `hlMin/Max` 座標存在時，會為該關鍵幀額外建立一個 `InWorldBoxAnnotation`，顏色由 `highlightColor` 指定。適合在講解區域時高亮指定的方塊範圍。

氣泡框背景預設是深色半透明（`#CC0E0E20`），可用 `backgroundAlpha` 調整透明度。世界錨定模式下有連線線；獨立模式下沒有。文字支援完整的 GuideNH 行內富文字語法（Markdown 格式化與 MDX 行內標籤），並帶陰影渲染。

> **富文字支援：** `text` 欄位支援 GuideNH 頁面中所有行內富文字語法：
> `**粗体**`、`*斜体*`、`~~删除线~~`、`<Color id="RED">颜色文本</Color>`、
> `<ItemLink id="minecraft:iron_ingot" />` 以及其他所有行內 MDX 標籤。
> **不支援** Minecraft 原版的 `§` 格式程式碼，請改用上述 MDX 語法。

> 缺少 `text` 欄位或值為空的 `text` 註釋將被靜默忽略。

---

### `input` — 滑鼠操作提示

在世界座標附近渲染一個滑鼠操作圖示（左鍵、右鍵或滾輪），用於提示玩家執行特定互動。

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

帶修飾鍵字首和物品圖示的示例：

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb",
  "modifier": "sneak",
  "item": "minecraft:iron_ingot"
}
```

| 欄位 | 型別 | 預設值 | 說明 |
|------|------|--------|------|
| `x`, `y`, `z` | float | `0.0` | 錨點的世界座標。 |
| `inputType` | 字串 | `"lmb"` | `"lmb"`（左鍵）、`"rmb"`（右鍵）或 `"scroll"`（滾輪），不區分大小寫。 |
| `modifier` | 字串 | `null` | 可選修飾鍵：`"sneak"` 或 `"ctrl"`。在圖示上方顯示字首文字。 |
| `item` | 字串 | `null` | 可選物品註冊 ID（如 `"minecraft:iron_ingot"`）。在滑鼠圖示左側顯示物品圖示。支援 `"modid:item:meta"` 格式指定後設資料。 |

圖示為從 `ponder_widgets.png` 繪製的 16×16 精靈圖。背景為深色半透明（`#CC0E0E20`），邊框為淺藍色（`#80AAAADD`）。指定 `item` 時氣泡框會橫向擴充套件以容納兩個圖示。

---

## 顏色格式

顏色為 ARGB 十六進位制字串。支援 `"0xFFFFFF00"`（帶 `0x` 字首）和 `"FFFF00"`（不帶字首）兩種寫法。

- `FF` alpha = 完全不透明
- `80` alpha = 50% 半透明
- `00` alpha = 完全透明
- `"0xFF00E000"` — 不透明綠色（菱形預設顏色）
- `"0x8022CCFF"` — 半透明藍色
- `"0xFFAAAAAA"` — 淺灰色（文字氣泡邊框預設顏色）

## 播放行為

### 控制元件說明

| 控制元件 | 功能 |
|------|------|
| **◄（上一關鍵幀）** | 跳轉到上一個可見關鍵幀片段的起始位置。隱藏關鍵幀會被跳過。 |
| **▶/⏸（播放/暫停）** | 切換播放狀態；若已播完則重新從頭開始。 |
| **↺（從頭開始）** | 返回第 0 刻並重新播放。 |
| 進度條 | 點選或拖拽可跳轉到任意位置（始終暫停）。 |
| 關鍵幀節點 | 進度條上僅為可見關鍵幀繪製的小刻度標記，懸停時顯示標籤文字。 |

### 初始狀態

當包含 `<ImportPonder>` 的頁面首次開啟時，場景預設**暫停在第 0 刻**。按播放鍵（▶）開始播放。

### 攝像機鎖定

播放**進行中**（未暫停）時：
- 攝像機跟隨關鍵幀插值路徑；拖拽和縮放**被禁用**。
- 層滑條和 StructureLib 滑條**被隱藏**。

播放**暫停**或**結束**時，所有互動完全恢復。

### 關鍵幀節點標籤

懸停在進度條的關鍵幀節點上時：
- 節點稍微放大以表示懸停。
- 若該關鍵幀有 `label`，則在節點旁顯示標籤文字。
- 隱藏關鍵幀不會生成可懸停節點，但當播放或拖動時間軸經過它們時，仍會照常應用對應的時間軸狀態。

### 層控制

活躍關鍵幀的 `layer` 欄位在播放期間覆蓋可見層過濾：
- `null`（或省略）→ 顯示所有層。
- `1`、`2`、`3`……→ 限制到該 1-based 層索引。

## 完整示例

以下示例展示了所有七種註釋型別，跨四個關鍵幀。

### 目錄結構

```
assets/mymod/guidebooks/
  _en_us/machines/grinder.md
  _en_us/machines/grinder.snbt
  _en_us/machines/grinder.json
```

### `grinder.json`

```json
{
  "totalTime": 240,
  "keyframes": [
    {
      "time": 0,
      "label": "总览",
      "camera": { "zoom": 1.5, "rotX": 20, "rotY": 225 },
      "layer": null,
      "annotations": []
    },
    {
      "time": 60,
      "label": "输入仓",
      "camera": { "rotY": 180 },
      "layer": null,
      "annotations": [
        {
          "type": "diamond",
          "x": 0.5, "y": 1.5, "z": 1.5,
          "color": "0xFF44FF44",
          "tooltip": "EV 输入总线",
          "alwaysOnTop": true
        },
        {
          "type": "text",
          "x": 0.5, "y": 3.0, "z": 1.5,
          "text": "在此处放入矿石",
          "color": "0xFF44FF44"
        },
        {
          "type": "input",
          "x": 0.5, "y": 2.0, "z": 1.5,
          "inputType": "rmb"
        }
      ]
    },
    {
      "time": 140,
      "label": "输出侧",
      "camera": { "rotY": 90 },
      "layer": null,
      "annotations": [
        {
          "type": "box",
          "minX": 2.0, "minY": 0.0, "minZ": 0.0,
          "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
          "color": "0x8800AAFF",
          "lineWidth": 1.5
        },
        {
          "type": "line",
          "fromX": 2.0, "fromY": 1.0, "fromZ": 1.5,
          "toX": 2.5, "toY": 1.0, "toZ": 1.5,
          "color": "0xFFFFAA00",
          "lineWidth": 2.0,
          "alwaysOnTop": true
        },
        {
          "type": "blockface",
          "pos": [2, 1, 1],
          "color": "0x8833FF33"
        },
        {
          "type": "text",
          "x": 2.5, "y": 3.0, "z": 1.5,
          "text": "在此处收取矿粉",
          "color": "0xFF00AAFF"
        },
        {
          "type": "input",
          "x": 2.5, "y": 2.0, "z": 1.5,
          "inputType": "lmb"
        }
      ]
    },
    {
      "time": 220,
      "label": "滚轮翻层",
      "camera": { "rotY": 225 },
      "layer": null,
      "annotations": [
        {
          "type": "input",
          "x": 1.5, "y": 2.5, "z": 1.5,
          "inputType": "scroll"
        },
        {
          "type": "text",
          "x": 1.5, "y": 3.5, "z": 1.5,
          "text": "滚动鼠标滚轮查看层",
          "color": "0xFFFFCC00"
        }
      ]
    },
    {
      "time": 240,
      "camera": { "rotY": 225 }
    }
  ]
}
```

### `grinder.snbt`

使用 `<ImportStructure>` 支援的 SNBT/NBT 結構；格式說明見 [GameScene](GameScene#importstructure)。

### `grinder.md`

```mdx
# 磨碎机

<GameScene zoom="4">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

磨碎机将矿石加工成双倍矿粉。按**播放**键查看动态演示。
```

## 注意事項

- 思索進度條繪製在層/StructureLib 滑條上方。播放期間結構滑條被隱藏，暫停後重新出現。
- 即使某些關鍵幀僅更改部分攝像機軸，攝像機插值始終平滑（緩入/緩出）。使用 `cameraEaseTicks` 可以讓攝像機立即跳轉（`0`）或在指定刻數內完成緩動後保持目標位置。
- 註釋屬於單個關鍵幀，僅在該關鍵幀啟用期間（從其 `time` 刻到下一關鍵幀的 `time` 刻之前）顯示。正向播放時疊加文字註釋會平滑淡出後切換。
- Ponder `line` 註釋使用普通 `LineAnnotation` 的同一套執行時渲染器，支援折線和起點/終點箭頭。點立方體標記目前仍是 MDX `LineAnnotation` 專用能力。
- Ponder `text` 註釋使用普通 `TextAnnotation` 的同一套執行時渲染器，支援連線線方向、偏移和長度。動態文字是基於關鍵幀切換的，不會在單個關鍵幀內逐 tick 插值。
- 每個遊戲場景（`<GameScene>`）只有一個 `<ImportPonder>` 標籤生效，多個標籤時後者覆蓋前者。
- `text` 欄位缺失或為空的 `text` 註釋將被靜默跳過。
- `inputType` 欄位缺失或無法識別時預設為 `"lmb"`。
- `blockChanges` 按從第 0 幀到當前幀的順序應用；在多個關鍵幀中修改同一位置的方塊完全正常。
- 方塊實體和實體 NBT 操作也使用同樣的重放模型；向前或向後拖動進度條都能恢復正確狀態。
- `maxWidth` &gt; 0 的 `text` 註釋使用原版字型渲染器自動換行；氣泡框高度會隨多行文字自動調整。
- `nbt` 字串中鍵名必須為**不帶引號**的標準 SNBT 格式（MC 1.7.10 `JsonToNBT` 要求）。字串值仍需引號，例如 `{id:"minecraft:iron_ingot",Count:8b}`。
- `modifyTileNBT` 和 `modifyEntityNBT` 的 `value` 是 SNBT 值，不是 JSON 值。字串值需要在 JSON 內轉義 SNBT 引號：`"value": "\"hello\""`。
