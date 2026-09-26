[English](Navigation)

# 導航

GuideNH 會根據頁面 frontmatter 建立導覽樹。

在遊戲內側邊欄中，已經展開且其子樹仍可見的祖先頁面會固定在頂部。多層祖先可以逐層堆疊固定，只有當對應可見子樹整體滾出後，該 sticky 行才會一起被頂走，行為類似 VSCode 的文件樹。

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

| 字段 | 說明 |
| --- | --- |
| `title` | 必填，顯示標題 |
| `keyword` | 可選，單一搜尋關鍵字或別名 |
| `keywords` | 可選，搜尋關鍵字或別名列表 |
| `parent` | 可選，父頁面 id；解析規則與指南頁面連結相同 |
| `position` | 可選，同級排序提示 |
| `recommend` | 可選，首頁建議優先權；缺省時不會出現在建議面板中 |
| `priority` | 可選，同路徑頁面覆蓋時的載入優先權；預設 `0` |
| `icon` | 可選，單一物品圖示；支援內聯 `mod:item:meta:{snbt}` 尾部 |
| `icons` | 可選，循環物品圖示清單；每項可為可選內嵌 `:{snbt}` 的普通物品 id，也可為 `{id, meta?, nbt?}` map |
| `icon_texture` | 可選，從指南資源解析的紋理圖標 |
| `icon_textures` | 可選，循環紋理圖示列表 |
| `required_mod` | 可選，單一模組 id；此模組未載入時頁面不可見 |
| `required_mods` | 可選，模組 id 列表；列出的全部模組都載入時頁面才可見 |
| `excluded_mod` | 可選，單一模組 id；此模組載入時頁面不可見 |
| `excluded_mods` | 可選，模組 id 列表；其中任一模組載入時頁面不可見 |

### `navigation.position`

`navigation.position` 是用來控制同級頁面順序的可選整數。

- 未填寫時預設按 `0` 處理。
- 數值越大越前面。
- 數值相同時，依標題字母序排序。

### 搜尋關鍵字

`navigation.keyword` 新增一個搜尋別名，`navigation.keywords` 新增多個搜尋別名。兩個欄位可以同時使用；
值會自動移除首尾空白並忽略重複項。關鍵字使用與標題和正文相同的語言分析器及前綴模糊匹配，搜尋結果仍顯示正常的導航標題。

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

`navigation.recommend` 是用於首頁建議面板的可選整數。

- 只有寫了這個欄位的頁面才會出現在推薦面板中。
- `0` 是有效值。
- 數值越大越前面。
- 數值相同時，依標題字母序排序。
- 推薦面板按 `GuidePage` 運作，因此每一項都會直接跳到對應頁面。

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## 模組需求

使用 `required_mod` 或 `required_mods` 可以要求一個或多個模組已載入；使用 `excluded_mod` 或
`excluded_mods` 可以在一個或多個不相容模組載入時隱藏頁面。當條件未滿足時，頁面會從導航樹和所有
頁面索引（物品、分類等）中排除，因此無法透過導覽或搜尋找到該頁面。

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

兩個鍵可以同時使用；只有列出的所有模組都已加載，頁面才會顯示。

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

## 載入優先權

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
- 優先權相同時，後處理的資源包條目覆蓋先處理的，保持 Minecraft 資源包覆蓋順序
- priority 只在同一頁面路徑、同一語言/回退層級的候選之間生效

這適合模組自備基礎指南頁、整合包又希望穩定覆寫它的場景，不必只依賴資源包排序。

## 圖示來源

GuideNH 會依下列順序選擇導航/搜尋圖示：

1. 如果配置了 `icon_textures`，優先使用循環紋理圖標
2. 否則若 `icon_texture` 能成功加載，則使用單張紋理圖標
3. 否則若 `icons` 中至少有一個物品可成功解析，則使用循環物品圖標
4. 否則若 `icon` 對應的物品存在，則使用單一物品圖標
5. 若都不可用，則不顯示圖示

紋理圖示來自執行時間資源，因此像 `test1.png` 這樣的頁面私有相對檔案也能正常運作。

## 父節點與根節點

- 省略 `parent` 會建立一個根節點。
- 設定 `parent: index.md` 或任何其他頁面 id 會建立子節點。
- 父頁必須存在於同一份指南導覽樹。

`navigation.parent` 使用與 Markdown 頁面連結相同的命名空間規則：

- `parent: index.md` 和 `parent: ./index.md` 會在目前頁面命名空間內解析。
- `parent: /index.md` 會從目前頁面命名空間根路徑解析。
- `parent: gregtech:index.md` 或 `parent: gregtech:/index.md` 會明確指向另一個命名空間。

資料驅動指南依命名空間隔離。 `assets/guidenh/guidenh/_zh_cn/...` 下的頁面屬於
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

這些分類可透過內建 `<Category name="machines" rows="3" />` 標籤查詢，同時也會自動建立諸如 `Category:machines` 這樣的隱藏式可搜尋頁面。
GuideNH 也會自動建立隱藏可搜尋的特殊頁面 `Special:AllPages` 與 `Special:Categories`。

## 物品索引頁面

頁面可使用 `item_id`（單一值）或 `item_ids`（清單）註冊「物品到頁面」的對應：

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:iron_ore#crafting
```

這些映射會被 `<ItemLink>` 使用。 `item_id` 是一個 NEI 風格表達式，`item_ids` 的每一項使用相同
表達式語法。例如，`minecraft:potion 16384-16462,!16386` 符合一個 meta 範圍但排除 `16386`；
`minecraft:potion 0-16,20-36,!28` 會組合兩個 meta 範圍並排除 `28`；`ae2:white_paint_ball:*` 是相容的
嚴格全 meta 映射。

可在條目末尾加上 `#anchor` 後綴，點擊連結時會自動捲動到指定標題。
錨點由標題文字轉換而來：全部小寫、空格替換為連字符
（例如 `## Crafting Recipe` → `#crafting-recipe`）。

找出順序如下：

1. 精確物品 + 精確 meta
2. 如果存在，則回退到通配 meta
3. 符合物品表達式

## 標題錨點鏈接

GuideNH 在 Markdown 連結和 `<a>` 標籤中支援標題錨點跳轉。
錨點由標題文字全部小寫並將空格替換為連字符後產生。

**同頁錨點：**

```md
[跳转到安装章节](#installation)
[跳转到合成配方](#crafting-recipe)
```

**跨頁錨點：**

```md
[查看入门教程](./Guide-Page-Format#installation)
[其他页面](other-guide.md#usage)
```

**絕對路徑錨點**（使用指南命名空間，可避免子目錄中相對路徑歧義）：

```md
[绝对路径](guidenh:other-guide.md#usage)
[任意命名空间](mymods:crafting/iron.md#smelting)
```

`namespace:path` 格式直接符合 ID 為 `namespace:path` 的頁面。
效果與相對路徑相同，但無需使用 `../` 導覽。
目標頁必須與連結來源處於同一份指南中。

**命名內嵌錨點**也可透過 MDX 的 `<a name="...">` 放置：

```md
<a name="custom-anchor" />

...内容...

[跳转到这里](#custom-anchor)
```

跳轉帶錨點的連結時，指南會自動捲動到目標標題或命名錨點處。

## `<SubPages>`

`<SubPages>` 會渲染導覽子頁面連結清單。

### 屬性

| 屬性 | 類型 | 預設值 | 意義 |
| --- | --- | --- | --- |
| `id` | page id 或空字串 | 目前頁面 | 列出其子頁面的頁面 id |
| `alphabetical` | boolean expression | `false` | 按標題字母排序，而不是按導航順序排序 |

### 範例

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

特殊情況：`id=""` 會列出所有根導航節點。

## `<Category>`

`<Category>` 會渲染某個命名分類下的全部頁面連結。

### 屬性

| 屬性 | 類型 | 預設值 | 意義 |
| --- | --- | --- | --- |
| `name` | string | 無 | 要渲染的分類名 |
| `rows` | 正整數 | `3` | MediaWiki 風格佈局中的顯示列數 |

````md
<Category name="machines" rows="3" />
````

如果分類不存在，GuideNH 會顯示內聯錯誤。

同一個分類也會自動擁有一個隱藏可搜尋頁面 `Category:machines`。

## `<Special>`

`<Special>` 用於渲染內建的 MediaWiki 風格特殊頁面清單。

### 屬性

| 屬性 | 類型 | 預設值 | 意義 |
| --- | --- | --- | --- |
| `name` | string | 無 | 支援的值：`AllPages`、`Categories` |
| `rows` | 正整數 | `3` | MediaWiki 風格佈局中的顯示列數 |

````md
<Special name="AllPages" rows="4" />
<Special name="Categories" rows="3" />
````

相同內容也可以透過隱藏可搜尋頁面 `Special:AllPages` 與 `Special:Categories` 存取。

## 搜尋結果標題

搜尋標題依下列順序決定：

1. `navigation.title`
2. 第一個一階標題（`# Heading`）
3. 原始頁 id

## 相關頁面

- [指南頁面格式](Guide-Page-Format)
- [搜尋](Search)
- [標籤參考](Tags-Reference)
