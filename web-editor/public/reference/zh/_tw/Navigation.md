# 導航

GuideNH 會根據頁面 frontmatter 構建導航樹。

滾動導航欄時，已展開的祖先頁面會固定在頂部，直到其子頁面整體滾出可見區域。多層祖先可同時固定。

## 導航 Frontmatter

`navigation` map 控制一個頁面是否會出現在指南樹中。

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### 欄位說明

| 欄位 | 說明 |
| --- | --- |
| `title` | 必填，顯示標題 |
| `keyword` | 可選，單個搜尋關鍵詞或別名 |
| `keywords` | 可選，搜尋關鍵詞或別名列表 |
| `parent` | 可選，父頁面 id；解析規則與指南頁面連結相同 |
| `position` | 可選，同級排序提示 |
| `recommend` | 可選，首頁推薦優先順序；預設時不會出現在推薦面板中 |
| `priority` | 可選，同路徑頁面覆蓋時的載入優先順序；預設 `0` |
| `icon` | 可選，單個物品圖示；支援內聯 `mod:item:meta:{snbt}` 尾部 |
| `icons` | 可選，迴圈物品圖示列表；每項可為帶可選內聯 `:{snbt}` 的普通物品 id，也可為 `{id, meta?, nbt?}` map |
| `icon_texture` | 可選，從指南資源中解析的紋理圖示 |
| `icon_textures` | 可選，迴圈紋理圖示列表 |
| `icon_fluid` | 可選，單個流體註冊名；與 `icon_fluids` 和物品/紋理圖示源互斥 |
| `icon_fluids` | 可選，非空流體註冊名列表，每秒輪換；與 `icon_fluid` 和物品/紋理圖示源互斥 |
| `required_mod` | 可選，單個模組 id；該模組未載入時頁面不可見 |
| `required_mods` | 可選，模組 id 列表；列出的全部模組都載入時頁面才可見 |
| `excluded_mod` | 可選，單個模組 id；該模組載入時頁面不可見 |
| `excluded_mods` | 可選，模組 id 列表；其中任一模組載入時頁面不可見 |

### `navigation.position`

`navigation.position` 是用於控制同級頁面順序的可選整數。

- 未填寫時預設按 `0` 處理。
- 數值越大越靠前。
- 數值相同時，按標題字母序排序。

### 搜尋關鍵詞

`navigation.keyword` 新增一個搜尋別名，`navigation.keywords` 新增多個搜尋別名。兩個欄位可以同時使用；
值會自動去除首尾空白並忽略重複項。關鍵詞使用與標題和正文相同的語言分析器及字首模糊匹配，搜尋結果仍顯示正常的導航標題。

```yaml
navigation:
  title: 分子装配室
  keyword: 装配室
  keywords:
    - 分子装配
    - 自动合成机器
```

## 首頁推薦

### `navigation.recommend`

`navigation.recommend` 是用於首頁推薦面板的可選整數。

- 只有寫了這個欄位的頁面才會出現在推薦面板中。
- `0` 是有效值。
- 數值越大越靠前。
- 數值相同時，按標題字母序排序。
- 推薦面板按 `GuidePage` 工作，因此每一項都會直接跳轉到對應頁面。

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## 模組需求

使用 `required_mod` 或 `required_mods` 可以要求一個或多個模組已載入；使用 `excluded_mod` 或
`excluded_mods` 可以在一個或多個不相容模組載入時隱藏頁面。當條件未滿足時，頁面會從導航樹和所有
頁面索引（物品、分類等）中排除，因此無法透過導航或搜尋找到該頁面。

```yaml
navigation:
  title: Applied Energistics 集成
  parent: index.md
  required_mod: appliedenergistics2

navigation:
  title: 多模组功能
  parent: index.md
  required_mods:
    - gregtech
    - appliedenergistics2
```

兩個鍵可以同時使用；只有列出的所有模組都已載入，頁面才會顯示。

required 和 excluded 條件也可以同時使用：所有 required 模組都必須載入，並且 excluded 模組都不能載入。

```yaml
navigation:
  title: 仅客户端集成
  parent: index.md
  required_mod: guide_api
  excluded_mods:
    - incompatible_addon
    - legacy_addon
```

## 載入優先順序

當多個已載入資源包提供同一條 guide 頁面路徑時，GuideNH 會先讀取頁面 frontmatter，
然後選擇 `navigation.priority` 最高的候選頁面。

```yaml
navigation:
  title: 整合包覆盖页面
  parent: index.md
  priority: 100
```

規則：

- 未寫 `priority` 時按 `0` 處理
- 取值為 Java int，最大 `2147483647`
- 數值更高者勝出
- 優先順序相同時，後處理的資源包條目覆蓋先處理的，保持 Minecraft 資源包覆蓋順序
- priority 只在同一頁面路徑、同一語言/回退層級的候選之間生效

這適合模組自帶基礎指南頁面、整合包又希望穩定覆蓋它的場景，不必只依賴資源包排序。

## 圖示來源

流體圖示使用 `icon_fluid` 或 `icon_fluids`，只能選擇其中一個，且不能同時設定物品或紋理圖示。它們不會建立流體跳轉繫結；請另填頁面的 `fluid_id` / `fluid_ids`。

未設定流體圖示時，GuideNH 按以下順序選擇物品和紋理圖示：

1. 如果配置了 `icon_textures`，優先使用迴圈紋理圖示
2. 否則若 `icon_texture` 能成功載入，則使用單張紋理圖示
3. 否則若 `icons` 中至少有一個物品可成功解析，則使用迴圈物品圖示
4. 否則若 `icon` 對應的物品存在，則使用單個物品圖示
5. 若都不可用，則不顯示圖示

紋理圖示來自執行時資源，因此像 `test1.png` 這樣的頁面私有相對檔案也能正常工作。

## 父節點與根節點

- 省略 `parent` 會建立一個根節點。
- 設定 `parent: index.md` 或任意其他頁面 id 會建立子節點。
- 父頁面必須存在於同一份指南導航樹中。

`navigation.parent` 使用與 Markdown 頁面連結相同的名稱空間規則：

- `parent: index.md` 和 `parent: ./index.md` 會在當前頁面名稱空間內解析。
- `parent: /index.md` 會從當前頁面名稱空間根路徑解析。
- `parent: gregtech:index.md` 或 `parent: gregtech:/index.md` 會顯式指向另一個名稱空間。

資料驅動指南按名稱空間隔離。`assets/guidenh/guidenh/_zh_cn/...` 下的頁面屬於
`guidenh:guidenh`；`assets/gregtech/guidenh/_zh_cn/...` 下的頁面屬於 `gregtech:guidenh`。
相對 parent 和相對連結都不會回退到其他模組的同名頁面。

## 分類頁面

頁面可以透過 frontmatter 加入一個或多個命名分類：

```yaml
categories:
  - basics
  - machines|Arc Furnace
```

每一項既可以只是分類名，也可以寫成 `分类名|排序名`。

這些分類可透過內建 `<Category name="machines" rows="3" />` 標籤查詢，同時還會自動建立諸如 `Category:machines` 這樣的隱藏可搜尋頁面。
GuideNH 還會自動建立隱藏可搜尋的特殊頁面 `Special:AllPages` 與 `Special:Categories`。

## 物品索引頁面

頁面可使用 `item_id`（單個值）或 `item_ids`（列表）註冊“物品到頁面”的對映：

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:iron_ore#crafting
```

這些對映會被 `<ItemLink>` 使用。`item_id` 是一個 NEI 風格表示式，`item_ids` 的每一項使用相同
表示式語法。例如，`minecraft:potion 16384-16462,!16386` 匹配一個 meta 範圍但排除 `16386`；
`minecraft:potion 0-16,20-36,!28` 會組合兩個 meta 範圍並排除 `28`；`ae2:white_paint_ball:*` 是相容的
嚴格全 meta 對映。

可在條目末尾加 `#anchor` 字尾，點選連結時會自動滾動到指定標題。
錨點由標題文字轉換而來：全部小寫、空格替換為連字元
（例如 `## Crafting Recipe` → `#crafting-recipe`）。

查詢順序如下：

1. 精確物品 + 精確 meta
2. 如果存在，則回退到通配 meta
3. 匹配物品表示式

## 流體索引頁面

頁面用 `fluid_id` 繫結一個流體，或用 `fluid_ids` 繫結多個流體；兩個欄位可以同時使用。名稱精確匹配註冊名，數量和 NBT 不影響繫結。可新增 `#anchor` 指向章節：

```yaml
fluid_id: water
fluid_ids:
  - steam#uses
navigation:
  title: 流体处理
  icon_fluid: water
```

這些繫結供 `<FluidLink>` 和懸停長按 `G` 使用。可識別 NEI 流體展示及裝液容器；空容器繼續使用物品索引。多個目標會顯示選擇列表，流體頁面排在容器物品頁面之前。

## 任務、研究與自定義內容索引

用 `bindings` 將當前頁面關聯到 BetterQuesting 任務、Thaumcraft 研究或其他已註冊型別：

```yaml
bindings:
  - {type: "thaumcraft", id: "INFUSION", anchor: "apparatus"}
  - {type: "betterquesting", id: "01234567-89ab-cdef-0123-456789abcdef", anchor: "quest"}
```

同一個內容對應多個頁面或章節時，GuideNH 會開啟與物品相同的目標選擇介面。相同頁面和錨點合併，不同錨點仍可單獨選擇；省略 `anchor` 表示頁面頂部。完整欄位和引號寫法見 [Frontmatter](Guide-Page-Format#外部內容繫結)。

`QuestLink` 和 `QuestCard` 標題左鍵開啟 BetterQuesting；右鍵開啟繫結指南，沒有指南時回退 BetterQuesting。研究和通用連結、卡片預設優先開啟指南，左鍵動作可以用 `target` 選擇，見 [標籤參考](Tags-Reference#研究與自定義內容)。

懸停支援的任務按鈕、研究條目或內容圖示，按住開啟指南鍵（預設 `G`）也能選擇繫結指南。沒有繫結時快捷鍵不執行操作。各模組的懸停區域和原生介面返回規則見 [模組相容](Mod-Compatibility)。

## 標題錨點連結

GuideNH 在 Markdown 連結和 `<a>` 標籤中支援標題錨點跳轉。
錨點由標題文字全部小寫並將空格替換為連字元後生成。

**同頁面錨點：**

```md
[跳转到安装章节](#installation)
[跳转到合成配方](#crafting-recipe)
```

**跨頁面錨點：**

```md
[查看入门教程](./getting-started.md#installation)
[其他页面](other-guide.md#usage)
```

**絕對路徑錨點**（使用指南名稱空間，可避免子目錄中相對路徑歧義）：

```md
[绝对路径](guidenh:other-guide.md#usage)
[任意命名空间](mymods:crafting/iron.md#smelting)
```

`namespace:path` 格式直接匹配 ID 為 `namespace:path` 的頁面。
效果與相對路徑相同，但無需使用 `../` 導航。
目標頁面必須與連結來源處於同一份指南中。

**命名內聯錨點**也可透過 MDX 的 `<a name="...">` 放置：

```md
<a name="custom-anchor" />

...内容...

[跳转到这里](#custom-anchor)
```

跳轉帶錨點的連結時，指南會自動滾動到目標標題或命名錨點處。

## `<SubPages>`

`<SubPages>` 會渲染導航子頁面連結列表。

### 屬性

| 屬性 | 型別 | 預設值 | 含義 |
| --- | --- | --- | --- |
| `id` | page id 或空字串 | 當前頁面 | 列出其子頁面的頁面 id |
| `alphabetical` | boolean expression | `false` | 按標題字母排序，而不是按導航順序排序 |

### 示例

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

特殊情況：`id=""` 會列出所有根導航節點。

## `<Category>`

`<Category>` 會渲染某個命名分類下的全部頁面連結。

### 屬性

| 屬性 | 型別 | 預設值 | 含義 |
| --- | --- | --- | --- |
| `name` | string | 無 | 要渲染的分類名 |
| `rows` | 正整數 | `3` | MediaWiki 風格佈局中的顯示列數 |

````md
<Category name="machines" rows="3" />
````

如果分類不存在，GuideNH 會顯示內聯錯誤。

同一個分類還會自動擁有一個隱藏可搜尋頁面 `Category:machines`。

## `<Special>`

`<Special>` 用於渲染內建的 MediaWiki 風格特殊頁面列表。

### 屬性

| 屬性 | 型別 | 預設值 | 含義 |
| --- | --- | --- | --- |
| `name` | string | 無 | 特殊頁面名稱；用 `SpecialPages` 瀏覽目錄，例如 `AllPages`、`Categories`、`Templates` |
| `rows` | 正整數 | `3` | MediaWiki 風格佈局中的顯示列數 |
| `page` | page id | 無 | 限定需要頁面目標的特殊列表 |
| `prefix` | string | 無 | `AllPagesWithPrefix` 的標題/頁面 ID 字首 |
| `language` | string | 無 | 翻譯列表的語言篩選 |
| `query` | string | 無 | 列表搜尋文字 |

````md
<Special name="AllPages" rows="4" />
<Special name="Categories" rows="3" />
````

相同內容也可以透過隱藏可搜尋頁面 `Special:AllPages` 與 `Special:Categories` 訪問。

## 搜尋結果標題

搜尋標題按以下順序確定：

1. `navigation.title`
2. 第一個一級標題（`# Heading`）
3. 原始頁面 id

## 相關頁面

- [指南頁面格式](Guide-Page-Format)
- [搜尋](Search)
- [標籤參考](Tags-Reference)
