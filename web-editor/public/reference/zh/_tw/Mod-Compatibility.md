# 模組相容

GuideNH 為部分模組內建相容支援。執行時內容提供者及原生懸停整合在目標模組載入時啟用；內容身分與頁面繫結索引在目標模組不存在時仍然可用。

## StructureLib

當 StructureLib 載入時，GuideNH 可以透過 `<ImportStructureLib>` 把多方塊預覽匯入 `<GameScene>`。客戶端指令 `/exportStructure structureLib` 也可以把這些預覽匯出為 PNG 文件截圖。完整指令引數、StructureLib 專屬選項以及相關的 `gameScene` 匯出模式見 [結構匯出](Structure-Export)。

## BetterQuesting

當 [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) 載入時，GuideNH 解鎖三項能力：

1. Page bindings with `type: "betterquesting"` associate a guide with a quest. Identity indexing does not require BetterQuesting; native quest navigation does.
2. 新增兩個標籤：`<QuestLink>`（行內）與 `<QuestCard>`（塊級）。
3. 標準“開啟指南”快捷鍵在 BetterQuesting 任務 GUI 中懸停某任務時也能生效：按住快捷鍵即可按任務 id 跳轉到對應指南頁。

### 按任務 id 索引頁面

在希望與某個或多個任務關聯的指南頁 frontmatter 中新增 `bindings` 列表：

```yaml
---
navigation:
  title: 第二阶段 — 蒸汽时代
bindings:
  - {type: "betterquesting", id: "01234567-89ab-cdef-0123-456789abcdef"}
  - {type: "betterquesting", id: "AAAAAAAAAAAAAAAAAAAMug=="}
---
```

每項的 `type` 填寫 `betterquesting`，`id` 可填寫標準 UUID 或 BetterQuesting 緊湊 Base64 ID；新增 `anchor` 可指向目前指南頁的章節。格式錯誤或為空的條目會被跳過並在日誌中警告。

GuideNH 會優先按緊湊 quest id 解碼，失敗後再回退到標準 UUID 解析。這與 BetterQuesting 使用的 `AAAAAAAAAAAAAAAAAAAMug==` 這類 quest id 格式相容。

不要在同一頁面的 `bindings` 中同時寫入同一個任務的兩種編碼；它們會歸一化成同一個內部 UUID，並被視為重複項。

左鍵開啟 BetterQuesting 任務介面。右鍵開啟繫結的指南頁；沒有繫結指南時，右鍵也開啟 BetterQuesting。多個頁面使用與物品相同的目標選擇介面，不同正文錨點保留為獨立目標。

### 從 BetterQuesting 描述連結到 GuideNH 頁面

BetterQuesting 任務描述可以透過 `[guide]` 標籤跳回 GuideNH 頁面。該標籤只在 BetterQuesting 已載入時由 GuideNH 解析，並會轉成 BetterQuesting 原生的 hyperlink-aware 文字框渲染，因此 BQ 自己的換行、滾動和點選熱區仍保持相容。

目標寫頁面 ID：

```text
[guide]guidenh:navigation-guide[/guide]
```

如果目標頁面存在，顯示文字會替換為該指南頁面的標題。`.md` 字尾可省略，因此當 `guidenh:navigation-guide.md` 存在時，`guidenh:navigation-guide` 也會指向它。

需要自定義顯示文字時使用 `page=`：

```text
[guide page=guidenh:navigation-guide]打开导航指南[/guide]
```

該連結會使用 BetterQuesting 原生連結的藍色與下劃線樣式，滑鼠懸停時顯示 GuideNH tooltip，點選後開啟目標 GuideNH 頁面。也支援頁面錨點：

```text
[guide page=guidenh:navigation-guide#navigation-fields]导航字段[/guide]
```

### `<QuestLink>` 與 `<QuestCard>`

兩個標籤都透過 `id` 接收 BetterQuesting 任務 id，並在頁面掛載時根據玩家進度決定外觀：

| 狀態 | 來源 | 渲染 |
| --- | --- | --- |
| 可見 | 任務已解鎖但未完成 | 普通可點選連結 |
| 完成 | 任務已完成 | 可點選連結，綠色，末尾追加 `✓` |
| 鎖定 | 任務存在但未解鎖，可見性不為 HIDDEN/SECRET | 仍然是可點選的任務連結 / 任務卡片標題，會開啟 BetterQuesting 或跳到索引頁 |
| 隱藏 | 鎖定且可見性為 HIDDEN 或 SECRET | 深灰斜體佔位符，不洩露任務資訊 |
| 缺失 | 任務 id 在資料庫中找不到對應任務 | 紅色斜體佔位符 |

左鍵開啟 BetterQuesting 任務介面。右鍵開啟繫結的指南頁；沒有繫結指南時，右鍵也開啟 BetterQuesting。多個頁面使用與物品相同的目標選擇介面，不同正文錨點保留為獨立目標。

屬性表與示例參見 [標籤參考](Tags-Reference#questlink)。

### 隱藏任務的處理

GuideNH 永不渲染處於 `HIDDEN` 或 `SECRET` 狀態且仍處於鎖定的任務的標題與描述。佔位文字採用翻譯鍵，便於整合包做本地化：

| 翻譯鍵 | 中文預設 |
| --- | --- |
| `guidenh.binding.state.hidden` | `隐藏内容` |
| `guidenh.binding.state.missing` | `找不到内容` |
| `guidenh.compat.bq.open_in_guide` | `在指南中打开` |

鎖定但非隱藏的任務，在 `<QuestLink>` 以及 `<QuestCard>` 的可點選標題上仍可顯示描述 tooltip，這與 BetterQuesting 自身在鎖定任務 tooltip 中暴露描述的行為一致。可透過 `showTooltip={false}`關閉該 tooltip；隱藏任務不會暴露任何 tooltip。

> [!NOTE]
> 解鎖或完成任務後重新開啟指南，以重新整理顯示的任務狀態。

### “開啟指南”快捷鍵整合

預設開啟指南快捷鍵（`G`，可在 `key.guidenh.open_guide` 中重新繫結）在 BetterQuesting 載入時獲得第二種觸發路徑。當 BetterQuesting 任務圖 GUI 開啟時：

1. 滑鼠懸停在面板中的某個任務按鈕上
2. 按住開啟指南快捷鍵

若任意已註冊指南透過 `bindings` 索引了該任務 id，GuideNH 將跳轉到對應頁面（如果指南尚未開啟則同時開啟）。若沒有任何頁面索引當前懸停的 id，則快捷鍵不會執行任何操作 —— 不會回退到開啟 BQ 任務 GUI，因為這恰好是 BQ 已經展示的內容。

該路徑獨立於物品 tooltip 觸發流，揹包內懸停物品仍按既有的物品 / 礦辭索引流程處理。

### BetterQuesting 缺席時的行為

- `<QuestLink>` 與 `<QuestCard>` 不會註冊；使用了它們的頁面會以標準“未知標籤”錯誤形式呈現，直到你移除這些標籤。
- `bindings` 仍會建立索引；原生任務跳轉與 BQ 懸停目標不可用。
- BetterQuesting 描述連結僅在安裝 BetterQuesting 時可用。

面向可選 BetterQuesting 整合的頁面可設定 `navigation.required_mod: betterquesting`，避免未安裝該模組時顯示未知標籤錯誤。

模組開發者擴充套件標籤及站點匯出的說明見 [開發者整合](Developer-Integration)。


## Thaumcraft 與 AspectRecipeIndex

### 使用研究繫結

Thaumcraft 研究繫結是可選整合。GuideNH 提供研究標籤、依據完成情況的展示與原生跳轉。在研究樹節點、研究條目標題或 ARI 的 NEI 研究圖示上按住開啟指南鍵（預設 `G`），可開啟全部匹配的指南目標。沒有繫結時快捷鍵不執行操作；物品槽繼續使用原有物品操作。

### 檢視與複製研究 ID

研究 ID 的顯示、查詢和複製由 AspectRecipeIndex 提供。`ARIConfig.showResearchId` 預設 `false`，僅控制 ID 標籤和快捷鍵提示，關閉時仍可複製。懸停可見的研究樹條目、研究頁標題或 ARI 的 NEI 研究元件，使用 NEI 已配置的“複製識別符號”快捷鍵（`copy.identifier`）及其 Ctrl 修飾鍵，剪貼簿會得到原始 ID。`showResearchKey` 獨立控制 NEI 研究元件是否顯示。研究頁的複製區域限定為標題，正文物品仍使用原有複製操作。

### 匯出研究目錄

在 NEI 設定 → 工具 → 資料轉儲中選擇**神秘研究 ID**，切換 CSV 或 JSON 後點選轉儲。全部註冊研究儲存至遊戲目錄下的 `dumps/thaumcraftresearch.csv` 或 `dumps/thaumcraftresearch.json`。UTF-8 目錄包含 ID、分類 ID/名稱、標題、簡介、普通/隱藏前置、兄弟研究、標誌和物品/紋理圖示，覆蓋沒有 NEI 配方的研究，不匯出玩家進度。關閉 `showResearchId` 時仍可匯出。

### 進度與返回導航

研究繫結 ID 區分大小寫。重新開啟指南可重新整理進度；資源或世界變更會使研究目錄快取失效。原生瀏覽不會解鎖或購買研究。從研究條目返回研究樹後，按 Escape 返回保留狀態的指南。
