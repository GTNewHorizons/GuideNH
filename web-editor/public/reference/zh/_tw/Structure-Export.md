# 結構匯出


`/exportStructure` 用於把 StructureLib 預覽或 GuideNH 已載入的 GameScene 匯出為 PNG 截圖。指令只在客戶端註冊。`structureLib` 子指令只有在檢測到 StructureLib 已載入時才能使用。

匯出使用正交渲染。輸出解析度會隨著結構或場景大小變化，但配置的方塊比例保持穩定。預設比例為每個方塊 `128` 畫素。

## 指令格式

```text
/exportStructure structureLib [controller] [options...]
/exportStructure structureLib @file.json
/exportStructure structureLib --config file.json

/exportStructure gameScene [options...]
/exportStructure gameScene @file.json
/exportStructure gameScene --config file.json
```

`structureLib` 的 `controller` 使用 `modid:name` 或 `modid:name:meta`。省略時會嘗試發現所有 StructureLib 控制器。

`gameScene` 不使用位置引數，會匯出當前已載入 guide 中編譯出的全部 GameScene。

## 通用引數

| 引數 | 說明 |
| --- | --- |
| `--out <dir>` | 輸出目錄。預設是 `screenshots/structurelib/<timestamp>/` 或 `screenshots/gameScene/<timestamp>/`。 |
| `--pixelsPerBlock <int>` | 每個世界方塊對應的畫素密度。預設是 `128`。 |
| `--scale <float>` | 乘到 `pixelsPerBlock` 上的縮放倍率。 |
| `--layers <expr\|each\|all>` | 控制渲染哪些 Y 層。預設是 `all`。 |
| `--view <preset>` | 相機預設。StructureLib 預設是 `isometric-south-east`。GameScene 預設尊重場景自己的相機。 |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | 精細相機覆蓋。GameScene 模式中，只要顯式傳入視角或旋轉引數，就會從場景相機切換為自動適配的匯出相機。 |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | 相機覆蓋的相容別名。 |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG 背景。預設是 `transparent`。 |
| `--maxPixels <long>` | 單張圖片允許的最大畫素數。預設是 `655360000`。使用 `-1` 表示無限制。 |
| `--batchSize <int>` | 每完成多少個結果就重新整理一次 `manifest.json`。預設是 `16`。 |
| `--force` | 允許生成超過 256 張截圖。 |
| `--dry-run` | 只生成計劃和 manifest，不寫 PNG。 |
| `--config <file>` / `@file.json` | 讀取 JSON 配置。 |

## StructureLib 引數

| 引數 | 說明 |
| --- | --- |
| `--tier <expr>` | 主 tier 值。 |
| `--channel <name=expr>` | 指定 StructureLib channel 的值。可以重複使用。 |
| `--facing <list>` | 批次匯出朝向。 |
| `--rotation <list>` | 批次匯出旋轉。 |
| `--flip <list>` | 批次匯出翻轉。 |
| `--orientation <facing:rotation:flip,...>` | 顯式指定朝向組合。 |
| `--gt-active-controller` | 僅 GregTech。儘可能把控制器渲染為機器執行中的紋理。 |
| `--gt-place-hatches` | 僅 GregTech。啟用正常 GT Hatch channel 邏輯來放置只允許 Hatch 的預覽位置；省略時預設仍優先 fallback casing。 |

## GameScene 引數

| 引數 | 說明 |
| --- | --- |
| `--show-annotations` | 渲染場景註解，包括 in-world 註解和 overlay 註解。預設 `false`。 |
| `--show-grid` | 渲染場景地面網格。預設 `false`。 |

GameScene 模式預設尊重每個場景自己配置的相機。只有使用 `--view`、`--yaw`、`--pitch`、`--roll`、`--rotateX`、`--rotateY` 或 `--rotateZ` 時，才會切換為自動適配匯出視角。

## 數字過濾

`--tier`、`--channel` 和 `--layers` 都使用同一種數字過濾語法。

```text
0
0-12
0-12,!5
!0,1
```

`0` 匹配單個值，`0-12` 匹配閉區間，`!` 用於排除，逗號用於組合多個 token。

## 層

`--layers all` 會把完整結構或場景匯出為一張圖。

`--layers 0-12,!5` 會匯出一張圖，只顯示匹配的層。

`--layers each` 會按照實際 Y 層逐層匯出。

按層過濾渲染時會強制渲染暴露方塊面，避免相鄰層隱藏後出現缺面。

## StructureLib Tier 和 Channel

當省略 `--tier` 和 `--channel` 時，匯出器會分析控制器，並按可用的統一 tier 每個匯出一張圖。統一 tier 會同時應用到主 tier 和每個發現的 StructureLib channel。自動統一 tier 匯出每個控制器/朝向最多 100 張。

當提供 `--tier` 但省略 `--channel` 時，請求的 tier 會同時驅動每個發現的 channel，並按 channel 自身範圍裁剪。

當顯式提供一個或多個 `--channel` 時，會使用這些 channel 值。多個 tier 和 channel 會按笛卡爾積組合。

```text
/exportStructure structureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

## GregTech 選項

預設情況下，GregTech 整合會把可選 Hatch 位置保留為正常 fallback casing。強制 Hatch 元素，例如 Muffler 位置，仍會渲染為指定 Hatch。

當你希望按 GT 正常 StructureLib Hatch channel 邏輯放置截圖用 Hatch 時，使用 `--gt-place-hatches`。例如宣告為 `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` 的元素，可以放置所需 Hatch 預覽並更新紋理，而不是隻顯示 fallback casing。

當控制器需要顯示機器執行中的紋理時，使用 `--gt-active-controller`。匯出器仍會同步預覽狀態，但不會把機器檢查失敗當作截圖失敗。

## 朝向

批次語法：

```text
--facing north,south --rotation normal,clockwise --flip none
```

顯式語法：

```text
--orientation north:normal:none,south:clockwise:none
```

兩種形式可以一起使用。當 StructureLib alignment limits 拒絕某個組合時，該組合會被跳過。沒有指定朝向時使用控制器預設值。

## 視角

支援的預設包括：

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

可以進一步微調：

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

## JSON 配置

StructureLib 示例：

```json
{
  "controller": "gregtech:gt.blockmachines:1234",
  "out": "screenshots/structurelib/demo",
  "pixelsPerBlock": 128,
  "scale": 1.0,
  "tier": "1-4",
  "channels": {
    "coil": "1-4",
    "casing": "1,2"
  },
  "layers": "0-12,!5",
  "orientation": "north:normal:none,south:clockwise:none",
  "view": "isometric-south-east",
  "background": "transparent",
  "maxPixels": 655360000,
  "batchSize": 16,
  "gtActiveController": false,
  "gtPlaceHatches": false,
  "force": false,
  "dryRun": false
}
```

GameScene 示例：

```json
{
  "out": "screenshots/gameScene/demo",
  "pixelsPerBlock": 128,
  "scale": 1.0,
  "layers": "all",
  "background": "transparent",
  "maxPixels": 655360000,
  "batchSize": 16,
  "showAnnotations": false,
  "showGrid": false,
  "force": false,
  "dryRun": false
}
```

執行配置：

```text
/exportStructure structureLib @my_structurelib_export.json
/exportStructure gameScene @my_gamescene_export.json
```

相對配置路徑會從當前工作目錄和 `config/guidenh/structure_exports/` 查詢。

## 輸出和 Manifest

每個匯出目錄包含 PNG 檔案和 `manifest.json`。

StructureLib 圖片名以控制器 `ItemStack` 的顯示名開頭，字尾包含 tier、channel、layers、orientation 和 view。

GameScene 圖片名包含 guide ID、page ID、scene 序號、layers、相機模式，以及可選的 annotation/grid 字尾。

檔名會按 Windows 規則安全化。`manifest.json` 會記錄輸出路徑、圖片尺寸、選擇的變體、warnings 和 errors。

## 效能

預設情況下，匯出計劃超過 256 張截圖會被拒絕，除非使用 `--force`。

預設單張圖片不能超過 `655360000` 畫素。這個預設值按 200x100x200 級別機器和投影方塊 `128` 畫素預算估算。超限時可以降低 `--pixelsPerBlock` 或 `--scale`，使用 `--layers` 縮小範圍，顯式提高 `--maxPixels`，或者使用 `--maxPixels -1` 關閉畫素上限。

當圖片超過 GPU 紋理大小時，會使用分塊 framebuffer 渲染。
