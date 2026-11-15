# 指南頁面格式

GuideNH 的執行時頁面使用 Markdown 檔案，當前解析支援：

- 標準 Markdown 塊級與行內語法
- YAML frontmatter
- GFM 表格
- 刪除線
- `==text==` 行內高亮
- GuideNH 行內下劃線擴充套件：`++text++`（直下劃線）、`^^text^^`（波浪下劃線）、`::text::`（著重號 / 點狀下劃線）
- `{/* ... */}` 形式的 MDX 註釋
- MDX 風格的自定義標籤

## 已支援的 Markdown

GuideNH 頁面當前支援示例指南里常用的這些 Markdown 能力：

- 標題
- 段落
- 行內強調、粗體、刪除線、行內程式碼
- 行內高亮（`==text==`）
- 行內下劃線（`++text++`）、波浪下劃線（`^^text^^`）和著重號（`::text::`）
- 連結與圖片
- 直接寫出的 URL、`www.` 域名和郵箱自動連結
- 引用式連結與引用式圖片
- 無序列表與有序列表
- 引用塊
- 分隔線
- 圍欄程式碼塊
- 縮排程式碼塊
- GFM 表格
- 帶有標題、列分組、行分組和合並單元格的 HTML 表格
- 純小寫 HTML 片段，例如 `<kbd>`、`<sub>`
- 頁面正文中的 MDX 註釋

可參考 `wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/markdown.md` 檢視實際執行時示例。

## 高亮

使用 `==text==` 可以生成行內高亮文字。需要自定義顏色時，可以使用 `<mark color="#8A6A00">text</mark>`。預設高亮色是偏暗的金黃色，用於在白色文字下保持可讀性。

## 程式碼塊

執行時程式碼塊當前支援：

- 顯式指定圍欄語言，例如 `java`、`lua`、`scala`、`csv`、`mermaid`
- 圍欄語言省略時自動推斷語言
- 在程式碼塊頂部顯示語言標籤
- 在遊戲內右上角提供一鍵複製按鈕
- 對識別出的語言做輕量執行時語法高亮

示例：

````md
```lua
local value = 42
print(value)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

縮排程式碼塊同樣支援：

````md
    print("indented code")
````

當圍欄程式碼塊識別為 `mermaid`，並且內容是當前支援的 `mindmap` 語法時，GuideNH 會把它渲染成可互動的執行時思維導圖，而不是普通程式碼塊。

當圍欄程式碼塊顯式標記為 `csv` 時，GuideNH 會把它渲染成執行時表格，而不是普通程式碼塊。如果不寫圍欄語言，即使內容看起來像 CSV，也仍然保留為程式碼塊，只把語言識別結果用於標籤和高亮。

顯式 CSV 表格也可以提供列寬 hint：

````md
```csv widths=120,80
name,value
iron,42
gold,17
```
````

圍欄 `meta` 也支援 `header=false` 和帶引號的寬度列表：

````md
```csv widths="120,80" header=false
name,value
iron,42
gold,17
```
````

普通段落文字里也支援 GitHub 風格的直接自動連結：

````md
访问 https://example.com/docs、www.example.org 或 guide@example.com
````

## Mermaid 思維導圖

GuideNH 當前的 Mermaid 執行時支援聚焦在 `mindmap`：

- 圍欄 ```` ```mermaid ```` 程式碼塊
- 內容以 `mindmap` 開頭時的自動識別
- 顯式 `<Mermaid>...</Mermaid>` 標籤
- 顯式 `<Mermaid src="./diagram.mmd" />` 資源匯入
- Mermaid 節點文字中的富文字行內 Markdown
- 作為 `<Mermaid>` 子元素的 `<NodeContent id="...">...</NodeContent>`，可為指定節點放入任意執行時塊內容
- 遊戲內整張圖拖拽平移
- Mermaid 源文字里的 `layout: tidy-tree`
- 常見 mindmap 節點形狀，例如方形、圓角、圓形、bang、cloud、hexagon
- `::icon(...)` 與 `:::class` 後設資料解析

示例：

````md
```mermaid
mindmap
  root((GuideNH))
    Runtime
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      Drag to pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [首页](./index.md)"]
    runtime["运行时块"]
    export["站点导出"]

<NodeContent id="runtime">
运行时节点里可以混排普通文本、链接和块内容。

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./test1.png)
</NodeContent>
</Mermaid>
````

當前執行時尚未支援的 Mermaid 圖型別，會繼續按帶有 Mermaid 標籤的普通程式碼塊顯示。

## CSV 表格匯入

GuideNH 也支援透過顯式標籤在執行時匯入 CSV 檔案：

````md
<CsvTable src="./markdown-table.csv" />
````

`src` 路徑會像普通執行時資源連結和場景 `src` 匯入一樣，相對當前頁面解析。

匯入的 CSV 表格同樣可以提供列寬 hint：

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

也可以直接在 Markdown 裡透過顯式 `csv` 圍欄寫內聯表格：

````md
```csv
name,value
iron,42
gold,17
```
````

## Markdown 表格

### 列寬

普通 GFM Markdown 表格也可以在表格前放置一行執行時屬性，提供列寬 hint：

````md
{: widths="120,80" }
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
````

這樣表格本體依然保持標準 Markdown，只是在 GuideNH 執行時應用列的首選寬度。

### 跨行、跨列與單元格屬性

- 在單元格後多寫一個 `|` 就多佔一列：`||` 跨兩列，`|||` 跨三列。
- 單元格以 `^^` 開頭時合併到上方單元格，內容在原單元格內換行。
- 兩側冒號控制單元格對齊：`: 左对齐`、`右对齐 :`、`: 居中 :`。
- 末尾 IAL（`{: ... }`）支援 `color`、`background` / `bgcolor`、安全 `style`、`align`、`valign`、`id` 和 `class`。用 `{:name: ...}` 定義可複用屬性；危險 CSS 與外部 URL 會被忽略。
- 物理行末尾的 `\` 將下一行接到相同單元格。以分隔行（`|--|--|`）開頭可建立無表頭表格。
- 單元格支援普通行內 Markdown 和 MDX，包括連結、圖片、強調及 LaTeX。HTML 表格還支援 `<thead>`、`<tbody>`、`<tfoot>` 行組。

```markdown
{:warning: style="background: #4b2418; color: #ffd8c2; font-weight: bold"}
| 阶段 | 产物 | 备注 |
| --- | --- | --- |
| : 第一阶段 : | 2 ATP ||
| ^^ 下一步 | 3 ATP | {: warning } |
| 合计 ||| {: style="text-align: right; background: #26364d"}
```

### HTML 表格

需要顯式行組或多行表頭時，可使用 HTML 表格。`<caption>` 會顯示在表格上方。使用 `<colgroup>` 和 `<col>` 設定各列的畫素或百分比首選寬度；`width` 接受畫素或百分比值（可選 `px` 字尾），`span` 會將同一寬度重複應用到多個列。使用 `<thead>`、`<tbody>` 和 `<tfoot>` 對行分組。表頭和資料單元格支援 `colspan`、`rowspan`、`align`、`valign`；`th` 還支援 `scope`、`abbr` 和 `headers`。站點匯出會保留 `width`、`border`、`cellpadding`、`cellspacing`、`frame`、`rules`、`summary`、`bgcolor` 和 `bordercolor` 等常用表格屬性。表格級別的 `widths` 屬性也可以提供以逗號分隔的畫素寬度 hint：

````md
<table widths="150,70,70,150">
  <caption>燃料消耗速率</caption>
  <colgroup>
    <col width="150" />
    <col width="70" span="2" />
    <col width="150" />
  </colgroup>
  <thead>
    <tr>
      <th scope="col" rowspan="2" align="left">燃料</th>
      <th scope="colgroup" colspan="2" align="center">燃烧速率</th>
      <th scope="col" rowspan="2" align="right">备注</th>
    </tr>
    <tr>
      <th scope="col" align="right">木炭</th>
      <th scope="col" align="right">煤炭</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th scope="row" rowspan="2" align="left">固体燃料</th>
      <td align="right">42</td>
      <td align="right">17</td>
      <td rowspan="2" align="left">每刻</td>
    </tr>
    <tr>
      <td colspan="2" align="right">合计 59</td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <th scope="rowgroup" align="left">总计</th>
      <td colspan="3" align="right">59 单位</td>
    </tr>
  </tfoot>
</table>
````

## 任務列表、提示塊與腳註

GuideNH 執行時還支援幾種常見的 GFM 風格行為：

- 使用 `- [ ]` 和 `- [x]` 的任務列表
- GitHub 風格提示引用塊，例如 `[!NOTE]`、`[!TIP]`、`[!IMPORTANT]`、`[!WARNING]`、`[!CAUTION]`
- 腳註引用與定義

示例：

````md
- [x] 已完成
- [ ] 待处理

> [!NOTE]
> 这里是提示内容

脚注引用[^one]

[^one]: 这里是脚注内容
````

腳註引用會在正文中渲染成 tooltip 風格標記，並在頁面下方追加一個緊湊的腳註列表。

## 列表寬度自定義

標準 Markdown 列表本身沒有寬度控制，但 GuideNH 執行時容器可以約束列表寬度：

````md
<Column width="220">
- 较窄的列表项
- 另一条较窄的列表项
</Column>
````

這是當前執行時裡自定義列表行寬的推薦寫法。

## 引用式連結與圖片

GuideNH 支援 CommonMark 引用定義：

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## 原生 HTML 片段

純小寫 HTML 標籤會按字面量內聯或塊級 HTML 內容處理，而不會當成 GuideNH 執行時標籤：

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
````

## MDX 註釋

GuideNH 支援 MDX 註釋寫法，並會在真正編譯 Markdown 前忽略它們：

````md
Visible text. {/* hidden inline comment */}

{/*
multiline comment
*/}

More visible text.
````

## Frontmatter

GuideNH 會讀取第一個 YAML frontmatter 塊，並解析這些已知鍵：

| 鍵 | 型別 | 含義 |
| --- | --- | --- |
| `navigation` | map | 將頁面加入導航樹 |
| `categories` | 字串列表 | 將頁面加入 MediaWiki 風格分類；每一項可選使用 `分类名|排序名` |
| `item_id` | 單個物品篩選表示式 | 單個 NEI 風格物品表示式，讓頁面可被 `<ItemLink>` 發現 |
| `item_ids` | 物品篩選表示式列表 | `item_id` 的列表形式，任一表示式匹配即可讓頁面被 `<ItemLink>` 發現 |
| `fluid_id` / `fluid_ids` | string / string list | 流體註冊名繫結，可帶 #anchor |
| `ore_ids` | 礦辭名列表 | 讓頁面可被礦辭物品（如 `ingotIron`、`oreCopper`）索引 |
| `quest_ids` | BetterQuesting 任務 id 列表 | 讓頁面可被 `<QuestLink>` / `<QuestCard>` 以及 BQ 任務 GUI 中的開啟指南快捷鍵發現。支援標準 UUID 字串和 BetterQuesting 的緊湊 Base64 形式。僅在 BetterQuesting 載入時生效。參見 [模組相容](Mod-Compatibility) |
| `author` | string | 單一作者名稱。顯示在底部欄中。 |
| `authors` | 字串列表或 `{name: ...}` 對映列表 | 多位作者名稱。最多顯示兩位，多餘的用 `...` 替代。與 `author` 同時存在時以 `authors` 為準。 |
| `date` | 字串或 YYYY-MM-DD 日期 | 內容建立日期。顯示在底部欄中。 |
| `updated` | 字串或 YYYY-MM-DD 日期 | 最後更新日期。顯示在底部欄中。 |
| `zoom` | 正浮點數 | 單頁內容縮放倍數（如 `1.5` 表示 150%）。與 ModConfig 中全域性 `contentZoom` 設定相乘。預設 `1.0`。 |
| 其他任意鍵 | 任意 YAML 值 | 供擴充套件或編寫工具使用 |

### 原生流體繫結與導航圖示

`fluid_id` 接受單個流體註冊名繫結，`fluid_ids` 接受字串列表；兩者可以共存，合併時去重。新增 `#anchor` 可以指向章節，不同錨點保留為獨立目標。繫結精確匹配註冊名，不匹配顯示數量或 NBT。

```yaml
fluid_id: water
fluid_ids:
  - water#coolant
  - steam#uses
navigation:
  title: 流体处理
  icon_fluid: water
```

`navigation.icon_fluid` 使用一個註冊名，`icon_fluids` 使用非空註冊名列表並每秒輪換。兩個欄位互斥，也不能與物品或紋理圖示源混用。它們只設定頁面圖示，不建立頁面繫結。流體是否可用需在執行中的遊戲驗證，離線預覽無法檢查登錄檔。

### `navigation`

| 欄位 | 必需 | 型別 | 說明 |
| --- | --- | --- | --- |
| `title` | 是 | string | 導航顯示名稱，也可作為搜尋標題後備值 |
| `keyword` | 否 | string | 一個額外的搜尋關鍵詞或別名，支援字首模糊匹配 |
| `keywords` | 否 | string 列表 | 額外的搜尋關鍵詞或別名；多個欄位會合並並去重 |
| `parent` | 否 | page id | 父頁面 id；省略時為頂級節點 |
| `position` | 否 | integer | 同級排序順序，預設 `0`，數值越大越靠前 |
| `recommend` | 否 | integer | 首頁推薦優先順序；填寫後才顯示在推薦面板，數值越大越靠前 |
| `priority` | 否 | integer | 同路徑頁面覆蓋時的載入優先順序；預設 `0`，數值更高者勝出，相同優先順序時後處理的資源包條目覆蓋先處理的 |
| `icon` | 否 | item id | 導航和搜尋中顯示的物品圖示。支援 `modid:name`、`modid:name:meta`、`modid:name:meta:{snbt}`。如需附加自定義名稱等 NBT，推薦直接寫在 `icon` 後面的內聯 `:{snbt}` 尾部。 |
| `icons` | 否 | item id 列表 | 迴圈輪播的物品圖示列表（每秒切換一次）。每一項語法同 `icon`，支援內聯 `:{snbt}`。存在時優先於 `icon` 使用。 |
| `icon_texture` | 否 | asset path | 紋理圖示路徑，按普通資源路徑解析 |
| `icon_textures` | 否 | asset path 列表 | 迴圈輪播的紋理圖示列表（每秒切換一次）。存在時優先於 `icon_texture` 使用。 |
| `icon_fluid` | 否 | string | 單個流體註冊名，與 `icon_fluids` 和物品/紋理圖示源互斥 |
| `icon_fluids` | 否 | string 列表 | 非空註冊名列表，每秒輪換，與 `icon_fluid` 和物品/紋理圖示源互斥 |
| `required_mod` | 否 | 模組 id | 僅在該模組已載入時顯示頁面。 |
| `required_mods` | 否 | 模組 id 列表 | 僅在列出的全部模組均已載入時顯示頁面。 |
| `excluded_mod` | 否 | 模組 id | 該模組載入時隱藏頁面。 |
| `excluded_mods` | 否 | 模組 id 列表 | 其中任一模組載入時隱藏頁面。 |

### Frontmatter 示例

```yaml
item_ids:
  - minecraft:book
fluid_id: water#coolant
navigation:
  title: Guide Overview
  parent: index.md
  position: 10
  recommend: 0
  icon: minecraft:book
categories:
  - basics
  - examples|Overview
author: ExampleAuthor
date: 2024-01-15
updated: 2024-06-01
```

`categories` 既支援普通分類名，也支援 `分类名|排序名` 形式。
可以使用 `<Category name="examples" rows="3" />` 渲染分類列表塊，
並使用 `<Special name="SpecialPages" rows="3" />` 嵌入自動生成的 MediaWiki 風格特殊頁索引。

語言目錄下的 `templates/` 目錄是模板頁的保留位置。模板頁的寫法與普通頁面相同，但不會出現在列表
與導航樹中。參見[模板](Templates)。

對於 BetterQuesting 整合，`quest_ids` 支援兩種格式：

- 標準 UUID 字串，例如 `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting 的緊湊 quest id，例如 `AAAAAAAAAAAAAAAAAAAMug==`

不要在同一頁面的 `quest_ids` 中同時填寫同一個任務的兩種寫法；它們會歸一化成同一個內部 UUID，並被視為重複項。

當 `author`、`authors`、`date` 或 `updated` 中任意一項存在時，GuideNH 會在
指南介面底部顯示一個與頂部工具欄風格一致的底部欄，靠右對齊顯示形如：
*內容來自 我的模組，作者 示例作者，日期 2024-01-15，更新日期 2024-06-01* 的資訊。

多作者示例：
```yaml
authors:
  - 爱丽丝
  - 鲍勃
  - 查理   # 只显示爱丽丝和鲍勃，后跟 ...
```
或使用結構化寫法：
```yaml
authors:
  - name: 爱丽丝
  - name: 鲍勃
```

### `zoom`

`zoom` 欄位可以單獨放大或縮小某一頁的內容，而不影響其他頁面。其值為正浮點數，作為倍數使用：

| 示例值 | 效果 |
| --- | --- |
| `1.0`（預設） | 正常大小 |
| `1.5` | 150%，內容放大 50% |
| `0.75` | 75%，內容縮小 25% |

單頁 `zoom` 與 ModConfig → GuideNH → UI 中的全域性 **contentZoom** 設定相乘生效。
這使模組包可以設定合理的基準縮放，同時允許個別頁面根據內容寬窄自行微調。

示例：將本頁設定為基準縮放的 150%：

```yaml
zoom: 1.5
navigation:
  title: 我的密集页面
```

縮放會以調整後的寬度重新計算頁面佈局，因此無論縮放級別如何，文字換行和所有塊佈局都能保持正確。

## 連結解析規則

GuideNH 按以下規則解析 id 和路徑：

### 頁面連結

| 輸入 | 含義 |
| --- | --- |
| `subpage.md` | 相對當前頁面，並使用當前頁面名稱空間 |
| `./subpage.md` | 相對當前頁面，並使用當前頁面名稱空間 |
| `/guide.md` | 相對當前頁面名稱空間根路徑，等價於 `currentmod:guide.md` |
| `gregtech:guide.md` | 顯式名稱空間；當前指南路徑為 `guidenh` 時會開啟 `gregtech:guidenh` |
| `gregtech:/guide.md` | 顯式名稱空間加根路徑，會規範化為 `gregtech:guide.md` |
| `subpage.md#anchor` | 頁面加錨點片段 |
| `guidenh:other.md#anchor` | 顯式 `modid:path#anchor` |
| `https://example.com` | 外部 HTTP/HTTPS 連結 |

頁面連結按名稱空間隔離。例如在 `assets/guidenh/guidenh/_zh_cn/index.md` 中寫
`[Guide](guide.md)` 會解析為 `guidenh:guide.md`；同樣的文字放在
`assets/gregtech/guidenh/_zh_cn/index.md` 中會解析為 `gregtech:guide.md`。如果當前名稱空間下找不到目標頁面，
GuideNH 會報告壞鏈，而不會回退到其他模組的同名頁面。

顯式 `modid:path` 連結可以跨到另一個模組的資料驅動指南。目標 guide id 會由目標頁面名稱空間和當前指南路徑推導，
所以從 `guidenh:guidenh` 連結到 `gregtech:guide.md` 時，會開啟 `gregtech:guidenh` 中的
`gregtech:guide.md` 頁面。

錨點片段會使指南滾動到對應標題（標題文字全部小寫、空格替換為連字元，例如 `#crafting-recipe` 對應 `## Crafting Recipe`），或滾動到 `<a name="...">` 命名錨點處。

### 資源連結

資源使用與頁面連結相同的解析規則。例如：

- `test1.png` 相對當前頁面檔案解析
- `/assets/example_structure.snbt` 解析到指南資源根目錄
- `guidenh:textures/gui/example.png` 作為顯式資源定位符解析

## 物品引用語法

導航 `icon`、`icons` 以及接受 item id 的標籤使用普通物品引用語法：

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

省略 `meta` 時預設使用 `0`。SNBT 從第一個 `{` 開始，並會被解析為物品 NBT。在支援通配 meta 的位置，
`*` 可以與 SNBT 尾部組合使用。

示例：

```text
minecraft:diamond
minecraft:wool:14
minecraft:written_book:0:{title:TestBook,author:GuideNH}
minecraft:written_book:*:{title:TestBook,author:GuideNH}
```

### 物品索引表示式

`item_id` 接受一個 NEI 風格表示式，`item_ids` 接受同一表示式語法的 YAML 列表。`item_ids` 中每項彼此獨立，任一項匹配即可關聯該頁面。空格用於組合條件，`|` 用於組合備選條件，`,` 用於組合一個條件中的規則。

- `minecraft:lava` 會對登錄檔 id 做不區分大小寫的部分匹配，因此也會匹配 `minecraft:lava_bucket`。
- `<minecraft:wool:14>` 嚴格匹配一個物品及其 meta。
- `ae2:white_paint_ball:*`、`:32767` 與 `:ANY` 等大寫 meta 標記是相容的嚴格全 meta 寫法。
- `16384-16462,!16386` 匹配 meta 範圍，同時排除 `16386`。
- `!minecraft:portal` 排除匹配該登錄檔 id 的物品。
- `r/^m\\w{6}ft$/` 對登錄檔 id 使用 Java 正規表示式。

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:crafting_table
  - appliedenergistics2:item.ItemMultiMaterial:1
  - "minecraft:written_book:*:{title:TestBook,author:GuideNH},!minecraft:written_book:0"
  - "<minecraft:wool:14>"
  - wrench|hammer
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:diamond#Usage
```

此例中，空格用於組合條件，`,` 用於組合同一項中的規則，`!` 用於排除匹配項，`|` 用於分隔備選表示式。
因此第一個表示式會匹配 `16384` 至 `16462` 的藥水 meta（排除 `16386`），或匹配任意 meta 的
`ae2:white_paint_ball`。第二個表示式展示了通配物品引用如何與逗號規則和 `!` 反向排除組合使用。
最後兩項展示了多個 meta 範圍的並集並排除 `28`，以及開啟 `Usage` 標題錨點的物品對映。

可在末尾追加 `#anchor`，使匹配頁面開啟到特定標題錨點。精確物品和顯式 meta 對映優先走直接索引，只有必要時才會計算表示式。

## 錯誤處理

如果頁面解析失敗，GuideNH 會生成錯誤頁，而不是讓整個指南崩潰。無效標籤、id 和屬性會以內聯指南錯誤文字的形式顯示出來。

## 相關頁面

- [導航](Navigation)
- [圖片與資源](Images-And-Assets)
- [標籤參考](Tags-Reference)
