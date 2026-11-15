# 配方

配方標籤用於展示物品或流體的生產方式與用途。機器配方取決於已安裝的配方整合；原版合成配方不依賴 NEI。

## 選擇標籤

| 標籤 | 查詢方向 | 預設結果數量 |
| --- | --- | --- |
| `<Recipe>` / `<RecipeFor>` | 生產目標 | 一個 |
| `<Usage>` / `<RecipeUsage>` | 消耗目標 | 一個 |
| `<RecipesFor>` | 生產目標 | 全部匹配結果 |
| `<RecipesUsage>` | 消耗目標 | 全部匹配結果 |

六個標籤使用相同的屬性。用 `limit` 限制結果數量，用 `recipeIndex` 選擇特定配方。

## 屬性

必須提供且只能提供一個目標：物品使用 `id`，流體使用 `fluid`。流體使用區分大小寫的完整 Forge 註冊名，例如 `water`、`steam`，不要填寫臨時展示物品的數字 ID。

| 屬性 | 含義 |
| --- | --- |
| `id` | 目標物品引用，與 `fluid` 互斥 |
| `fluid` | 目標流體註冊名，與 `id` 互斥 |
| `fallbackText` | 沒有可用配方時顯示的文字 |
| `handlerName` | 配方處理器顯示名稱的子串，不區分大小寫 |
| `handlerId` | overlay/handler ID 的精確匹配，不區分大小寫；也支援處理器簡單類名的子串 |
| `handlerBlacklist` | 逗號分隔的處理器排除列表 |
| `handlerWhitelist` | 逗號分隔的處理器保留列表，可覆蓋黑名單，但本身不縮小查詢範圍 |
| `handlerOrder` | 篩選後處理器列表的索引，非負整數，從零開始 |
| `recipeIndex` | 配方索引，非負整數，從零開始 |
| `input` / `output` | 輸入/輸出物品篩選表示式 |
| `inputFluid` / `outputFluid` | 輸入/輸出流體篩選表示式 |
| `limit` | 正整數，限制最多顯示的配方數量 |

```md
<RecipeFor id="minecraft:crafting_table" />
<RecipesFor id="minecraft:torch" limit="3" />
<RecipeFor fluid="steam" fallbackText="没有可用的蒸汽配方。" />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
```

可選配方整合不可用時顯示 `fallbackText`；未設定時顯示編寫錯誤。裝液容器仍屬於物品原料。

## 物品引用

`id`、`input`、`output` 支援以下寫法：

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

通配後設資料可寫為 `*`、`32767` 或 `ANY` 等大寫標記。

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
```

## 原料篩選

物品和流體篩選都支援 `,`（或）、`&`（且）、`!`（非）。填寫的所有輸入/輸出篩選都必須匹配。物品 SNBT 複合標籤或列表內部的逗號和 `&` 不會拆分物品引用。

```md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
<RecipesFor id="minecraft:iron_ingot" outputFluid="steam,water" />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
```

流體篩選匹配註冊名，也會檢查附加輸出槽中的流體，不匹配數量或 NBT。無效篩選語法會顯示編寫錯誤。

## 處理器選擇

已知處理器時使用 `handlerId`，按顯示名稱查詢時使用 `handlerName`，按篩選後列表選擇時使用 `handlerOrder`。`recipeIndex` 用於選擇查詢中的配方。

```md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" limit="3" />
<RecipeFor id="minecraft:torch" recipeIndex="0" />
```

黑白名單條目對 handler ID、overlay ID 和類名進行不區分大小寫的子串匹配。頁面黑名單會追加到 `config/guidenh/guidenh.cfg` 的 `recipeHandlerBlacklist`；顯式使用 `handlerId` 或 `handlerWhitelist` 可保留已被排除的處理器。

```md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" limit="6" />
<RecipeFor id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="没有可用的多方块预览。" />
```

以下處理器會渲染完整多方塊結構，因此預設排除：

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

可用白名單在某頁中啟用它們。查詢範圍較大時應設定 `limit`，避免頁面過長。

## 相關參考

- [標籤參考](Tags-Reference)
- [模組相容](Mod-Compatibility)
- [示例](Examples)
