[English](Recipes)

# 配方

GuideNH 可以直接在指南頁面中渲染合成配方和基於 NEI 的配方。

## 支持的標籤

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

三者使用同一套編譯器和屬性集。

## 標籤語意

| 標籤 | 行為 |
| --- | --- |
| `<Recipe>` | 為目標物品渲染單一配方 |
| `<Usage>` | 使用目標物品渲染單一配方 |
| `<RecipeFor>` | 與單配方行為相同，但命名更易讀 |
| `<RecipeUsage>` | 與單配方行為相同，但命名更易讀 |
| `<RecipesFor>` | 渲染多個匹配配方 |
| `<RecipesUsage>` | 渲染多個匹配配方 |

如果存在多個配方，而你使用單配方形式，GuideNH 預設只會渲染其中一個，除非額外過濾將結果縮小。

## 通用屬性

| 屬性 | 必需 | 意義 |
| --- | --- | --- |
| `id` | 是 | 目標物品引用 |
| `fallbackText` | 否 | 沒有可用配方時顯示的文本 |
| `handlerName` | 否 | 對 handler 名稱做大小寫不敏感的子字串過濾 |
| `handlerId` | 否 | 對 overlay/handler id 做大小寫不敏感的精確過濾 |
| `handlerBlacklist` | 否 | 逗號分隔，將這些 handler 從結果中剔除 |
| `handlerWhitelist` | 否 | 逗號分隔，保留這些 handler（即使被封鎖） |
| `handlerOrder` | 否 | 過濾後 handler 的 0 基索引 |
| `input` | 否 | 輸入物品過濾表達式 |
| `output` | 否 | 輸出物品過濾表達式 |
| `limit` | 否 | 正整數，限制最多渲染的配方數 |

## 物品 id 語法

`id`、`input` 和 `output` 都使用 GuideNH 擴充物品引用格式：

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

通配 meta 寫法：

- `*`
- `32767`
- 大寫標記，例如 `ANY`

## 過濾表達式語法

`input` 和 `output` 支援：

- `,` 表示 OR
- `&` 表示 AND
- `!` 表示 NOT

範例：

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT 可以和這三個物品引用屬性組合使用。 SNBT 複合標籤或清單內部的逗號、`&` 會保留在
引用中，只有最外層的分隔符號才會拆分過濾表達式：

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"特殊石头"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"奖励"}}'
/>
```

## 渲染順序

GuideNH 會依照以下順序嘗試配方：

1. NEI 原生 handler 渲染
2. NEI 槽位資料回退渲染
3. 內建原版合成回退渲染

如果都沒有符合：

- 若設定了 `fallbackText`，則顯示它
- 否則顯示內嵌編寫錯誤

## 範例

### 單一配方

````md
<RecipeFor id="minecraft:crafting_table" />
````

### 多個配方

````md
<RecipesFor id="minecraft:torch" />
````

### Handler 過濾

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Handler 黑名單與白名單

`handlerBlacklist` 將 handler 從結果中剔除，`handlerWhitelist` 再放回來。兩者都接受**英文逗號分隔的列表**，條目會與 handler id、overlay id 或類名比對，不區分大小寫且按包含處理 —— 因此一條就能指名單個 handler 或整個包。

命中黑名單的 handler 會被剔除，**除非**標籤主動要它：`handlerId`、`handlerWhitelist`，以及類名的小寫匹配都能保住它。

**為什麼是列表：單一 handler id 很少獨自出現。 **木板既是合成材料又是燃料，把不想要的 handler id 列出來：

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**一條覆蓋整個包。 **條目同樣以類別名稱做子字串匹配，所以一個模組 id 就能覆蓋它註冊的全部 handler —— 當某個模組的 handler 對這個物品全是噪音時很方便：

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**白名單：在單一頁面上撤銷黑名單。 **配置對所有頁面隱藏了兩個多方塊 handler，在這裡用白名單指名即可放回；寫成列表同理：

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="没有多方块预览。" />
````

**白名單本身不做篩選。 **它只負責把黑名單要剔除的 handler 救回來；要把結果收窄到某個 handler，用 `handlerId` / `handlerName` / `handlerOrder`，要限制渲染數量則用 `limit`。

配置項目 `recipeHandlerBlacklist`（`config/guidenh/guidenh.cfg`）對所有頁面生效，預設包含：

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

頁面本身的清單是**追加**到設定之後的，所以頁面只能比設定隱藏更多、不能更少。改配置會影響所有頁面；在標籤上指名只會影響該頁面。

### 輸入/輸出過濾

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### 回退文本

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## 最佳實踐

- 以可選模組整合時，優先提供 `fallbackText`
- 若已知確切的 NEI handler，優先使用 `handlerId`
- 當同一物品被大量 handler 命中、而只有部分值得展示時，使用 `handlerBlacklist`
- 以下兩個 handler 預設在 `config/guidenh/guidenh.cfg` 中被隱藏，因為它們渲染的是整個多方塊結構；用 `handlerId` 或 `handlerWhitelist` 指名即可顯示
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- 當標籤可能展開出大量配方時，使用 `limit`
- 複雜濾波邏輯最好在標籤旁邊用註解解釋，以便維護

## 即時運行範例

可參考 `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md`，其中包含大量配方範例，包括 handler 過濾、通配 meta 和帶 NBT 的物品 id。

## 行內 NEI 配方連結

使用自訂動作連結，可從正文開啟 NEI 而不繪製配方卡片，語法沿用 `&[文字](sound:...)`。

```md
&[查看焦爐全部配方](nei:handler?handlerId=gt.recipe.cokeoven)
&[查看合成配方](nei:recipes?id=minecraft:iron_ingot)
&[查看物品用途](nei:usage?id=gregtech:gt.blockmachines:236&handlerId=gt.recipe.cokeoven)

<NeiRecipeLink handlerId="gt.recipe.cokeoven">查看 NEI</NeiRecipeLink>
<NeiRecipeLink id="minecraft:iron_ingot">查看**合成配方**</NeiRecipeLink>
<NeiRecipeLink mode="usage" id="gregtech:gt.blockmachines:236" handlerId="gt.recipe.cokeoven" title="NEI">查看物品用途</NeiRecipeLink>

&[查看帶 SNBT 的書籍配方](nei:recipes?id=minecraft:written_book:0:%7Btitle:%22Guide%2BNEI%22%2Cauthor:GuideNH%7D)
```

`handler` 需要 `handlerId` 且禁止 `id`，開啟該處理器的所有配方；`recipes` 需要 `id`，查找產出配方；`usage` 需要 `id`，查找包含催化劑行為的用途，不受 Ctrl 狀態影響。後兩種模式可選填 `handlerId`。

`<NeiRecipeLink>` 僅接受 `id`、`handlerId`、`mode` 與提示 `title`。省略 `mode` 時，有 `id` 則為 `recipes`，否則為 `handler`。必須提供可見的行內子內容；動作連結文字為純文字，標籤支援富文字。

處理器以 overlay identifier 或 handler ID 忽略大小寫精確比對，不沿用配方卡片的類別名稱子字串備援。包含普通與序列處理器，不會合併共用 Java 類別的不同配方分類。

省略命名空間時使用目前頁面命名空間，保留註冊名稱大小寫、metadata 與 SNBT。萬用 metadata 沿用既有具體物品堆疊規則。URI 僅以嚴格 UTF-8 解碼一次；`+` 保持原樣。SNBT 及 `&`、`#`、`%`、`?`、`)` 應使用百分比編碼。

重複或未知參數、錯誤跳脫、片段、不支援的模式、衝突屬性及缺少目標會回報編寫錯誤。僅 `&` 動作與 `<NeiRecipeLink>` 為入口；不接受一般連結、參照連結、HTML 錨點或 `nei://` 別名。

僅在點擊時解析物品並查詢 NEI。NEI 缺失或無結果時保留指南並顯示在地化訊息。瀏覽器與 IDE 預覽顯示不可點擊文字，提示在 GuideNH 中開啟。
