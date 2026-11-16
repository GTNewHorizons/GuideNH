# 模組相容

GuideNH 為部分模組內建了條件性相容支援。每項整合只在目標模組載入時啟用；目標模組不存在時，相關的標籤、索引與按鍵繫結均保持靜默，指南其餘功能不受影響。

## StructureLib

當 StructureLib 載入時，GuideNH 可以透過 `<ImportStructureLib>` 把多方塊預覽匯入 `<GameScene>`。客戶端指令 `/exportStructure structureLib` 也可以把這些預覽匯出為 PNG 文件截圖。完整指令引數、StructureLib 專屬選項以及相關的 `gameScene` 匯出模式見 [結構匯出](Structure-Export)。

## BetterQuesting

當 [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) 載入時，GuideNH 解鎖三項能力：

1. 頁面前言新增 `quest_ids` 鍵，按 BetterQuesting 任務 id 建立索引。
2. 新增兩個標籤：`<QuestLink>`（行內）與 `<QuestCard>`（塊級）。
3. 標準“開啟指南”快捷鍵在 BetterQuesting 任務 GUI 中懸停某任務時也能生效：按住快捷鍵即可按任務 id 跳轉到對應指南頁。

### 按任務 id 索引頁面

在希望與某個或多個任務關聯的指南頁 frontmatter 中新增 `quest_ids` 列表：

```yaml
---
navigation:
  title: 第二阶段 — 蒸汽时代
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
---
```

值可以是標準 UUID 字串，也可以是 BetterQuesting 的緊湊 Base64 quest id。格式錯誤或為空的條目會被跳過並在日誌中警告。

GuideNH 會優先按緊湊 quest id 解碼，失敗後再回退到標準 UUID 解析。這與 BetterQuesting 使用的 `AAAAAAAAAAAAAAAAAAAMug==` 這類 quest id 格式相容。

不要在同一頁面的 `quest_ids` 中同時寫入同一個任務的兩種編碼；它們會歸一化成同一個內部 UUID，並被視為重複項。

一旦某個任務 id 被某頁索引，`<QuestLink>` 與 `<QuestCard>` 的點選行為會改為跳轉到該指南頁，而不是直接開啟 BetterQuesting 的任務 GUI。

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

兩個標籤都透過 `id` 接收 BetterQuesting 任務 id，並在編譯時根據玩家進度決定外觀：

| 狀態 | 來源 | 渲染 |
| --- | --- | --- |
| 可見 | 任務已解鎖但未完成 | 普通可點選連結 |
| 完成 | 任務已完成 | 可點選連結，綠色，末尾追加 `✓` |
| 鎖定 | 任務存在但未解鎖，可見性不為 HIDDEN/SECRET | 仍然是可點選的任務連結 / 任務卡片標題，會開啟 BetterQuesting 或跳到索引頁 |
| 隱藏 | 鎖定且可見性為 HIDDEN 或 SECRET | 深灰斜體佔位符，不洩露任務資訊 |
| 缺失 | 任務 id 在資料庫中找不到對應任務 | 紅色斜體佔位符 |

對可見 / 完成 / 鎖定狀態，點選目標的優先順序為：

- 若任務 id 出現在某頁的 `quest_ids` 中，則跳轉該指南頁
- 否則按 BetterQuesting 原生的父介面鏈開啟任務書中的任務介面

屬性表與示例參見 [標籤參考](Tags-Reference#questlink)。

### 隱藏任務的處理

GuideNH 永不渲染處於 `HIDDEN` 或 `SECRET` 狀態且仍處於鎖定的任務的標題與描述。佔位文字採用翻譯鍵，便於整合包做本地化：

| 翻譯鍵 | 中文預設 |
| --- | --- |
| `guidenh.compat.bq.locked` | `未解锁任务` |
| `guidenh.compat.bq.hidden` | `隐藏任务` |
| `guidenh.compat.bq.missing` | `未知任务` |
| `guidenh.compat.bq.open_in_guide` | `在指南中打开` |

鎖定但非隱藏的任務，在 `<QuestLink>` 以及 `<QuestCard>` 的可點選標題上仍可顯示描述 tooltip，這與 BetterQuesting 自身在鎖定任務 tooltip 中暴露描述的行為一致。可透過 `show_tooltip="false"`（或 `showTooltip={false}`）關閉該 tooltip；隱藏任務不會暴露任何 tooltip。

> [!NOTE]
> 解鎖或完成任務後重新開啟指南，以重新整理顯示的任務狀態。

### “開啟指南”快捷鍵整合

預設開啟指南快捷鍵（`G`，可在 `key.guidenh.open_guide` 中重新繫結）在 BetterQuesting 載入時獲得第二種觸發路徑。當 BetterQuesting 任務圖 GUI 開啟時：

1. 滑鼠懸停在面板中的某個任務按鈕上
2. 按住開啟指南快捷鍵

若任意已註冊指南透過 `quest_ids` 索引了該任務 id，GuideNH 將跳轉到對應頁面（如果指南尚未開啟則同時開啟）。若沒有任何頁面索引當前懸停的 id，則快捷鍵不會執行任何操作 —— 不會回退到開啟 BQ 任務 GUI，因為這恰好是 BQ 已經展示的內容。

該路徑獨立於物品 tooltip 觸發流，揹包內懸停物品仍按既有的物品 / 礦辭索引流程處理。

### BetterQuesting 缺席時的行為

- `<QuestLink>` 與 `<QuestCard>` 不會註冊；使用了它們的頁面會以標準“未知標籤”錯誤形式呈現，直到你移除這些標籤。
- `quest_ids` 不會建立任務跳轉，任務懸停快捷鍵也不可用。
- BetterQuesting 描述連結僅在安裝 BetterQuesting 時可用。

面向可選 BetterQuesting 整合的頁面可設定 `navigation.required_mod: betterquesting`，避免未安裝該模組時顯示未知標籤錯誤。

模組開發者擴充套件標籤及站點匯出的說明見 [開發者整合](Developer-Integration)。
