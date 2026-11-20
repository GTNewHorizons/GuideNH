# 配方

配方标签用于展示物品或流体的生产方式与用途。机器配方取决于已安装的配方集成；原版合成配方不依赖 NEI。

## 选择标签

| 标签 | 查询方向 | 默认结果数量 |
| --- | --- | --- |
| `<Recipe>` / `<RecipeFor>` | 生产目标 | 一个 |
| `<Usage>` / `<RecipeUsage>` | 消耗目标 | 一个 |
| `<RecipesFor>` | 生产目标 | 全部匹配结果 |
| `<RecipesUsage>` | 消耗目标 | 全部匹配结果 |

六个标签使用相同的属性。用 `limit` 限制结果数量，用 `recipeIndex` 选择特定配方。

## 属性

必须提供且只能提供一个目标：物品使用 `id`，流体使用 `fluid`。流体使用区分大小写的完整 Forge 注册名，例如 `water`、`steam`，不要填写临时展示物品的数字 ID。

| 属性 | 含义 |
| --- | --- |
| `id` | 目标物品引用，与 `fluid` 互斥 |
| `fluid` | 目标流体注册名，与 `id` 互斥 |
| `fallbackText` | 没有可用配方时显示的文本 |
| `handlerName` | 配方处理器显示名称的子串，不区分大小写 |
| `handlerId` | overlay/handler ID 的精确匹配，不区分大小写；也支持处理器简单类名的子串 |
| `handlerBlacklist` | 逗号分隔的处理器排除列表 |
| `handlerWhitelist` | 逗号分隔的处理器保留列表，可覆盖黑名单，但本身不缩小查询范围 |
| `handlerOrder` | 筛选后处理器列表的索引，非负整数，从零开始 |
| `recipeIndex` | 配方索引，非负整数，从零开始 |
| `input` / `output` | 输入/输出物品筛选表达式 |
| `inputFluid` / `outputFluid` | 输入/输出流体筛选表达式 |
| `limit` | 正整数，限制最多显示的配方数量 |

```md
<RecipeFor id="minecraft:crafting_table" />
<RecipesFor id="minecraft:torch" limit="3" />
<RecipeFor fluid="steam" fallbackText="没有可用的蒸汽配方。" />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
```

可选配方集成不可用时显示 `fallbackText`；未设置时显示编写错误。装液容器仍属于物品原料。

## 物品引用

`id`、`input`、`output` 支持以下写法：

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

通配元数据可写为 `*`、`32767` 或 `ANY` 等大写标记。

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
```

## 原料筛选

物品和流体筛选都支持 `,`（或）、`&`（且）、`!`（非）。填写的所有输入/输出筛选都必须匹配。物品 SNBT 复合标签或列表内部的逗号和 `&` 不会拆分物品引用。

```md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
<RecipesFor id="minecraft:iron_ingot" outputFluid="steam,water" />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
```

流体筛选匹配注册名，也会检查附加输出槽中的流体，不匹配数量或 NBT。无效筛选语法会显示编写错误。

## 处理器选择

已知处理器时使用 `handlerId`，按显示名称查找时使用 `handlerName`，按筛选后列表选择时使用 `handlerOrder`。`recipeIndex` 用于选择查询中的配方。

```md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" limit="3" />
<RecipeFor id="minecraft:torch" recipeIndex="0" />
```

黑白名单条目对 handler ID、overlay ID 和类名进行不区分大小写的子串匹配。页面黑名单会追加到 `config/guidenh/guidenh.cfg` 的 `recipeHandlerBlacklist`；显式使用 `handlerId` 或 `handlerWhitelist` 可保留已被排除的处理器。

```md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" limit="6" />
<RecipeFor id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="没有可用的多方块预览。" />
```

以下处理器会渲染完整多方块结构，因此默认排除：

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

可用白名单在某页中启用它们。查询范围较大时应设置 `limit`，避免页面过长。

## 行内 NEI 配方链接

使用 `&[文字](nei:...)` 或 `<NeiRecipeLink>`，从行内文字打开 NEI。

```md
&[查看焦炉全部配方](nei:handler?handlerId=gt.recipe.cokeoven)
&[查看合成配方](nei:recipes?id=minecraft:iron_ingot)
&[查看物品用途](nei:usage?id=gregtech:gt.blockmachines:236&handlerId=gt.recipe.cokeoven)

<NeiRecipeLink handlerId="gt.recipe.cokeoven">查看 NEI</NeiRecipeLink>
<NeiRecipeLink id="minecraft:iron_ingot">查看**合成配方**</NeiRecipeLink>
<NeiRecipeLink mode="usage" id="gregtech:gt.blockmachines:236" handlerId="gt.recipe.cokeoven" title="NEI">查看物品用途</NeiRecipeLink>

&[查看带 SNBT 的书籍配方](nei:recipes?id=minecraft:written_book:0:%7Btitle:%22Guide%2BNEI%22%2Cauthor:GuideNH%7D)
```

`handler` 必须填写 `handlerId` 且禁止 `id`，打开该处理器的全部配方；`recipes` 必须填写 `id`，查找产出该物品的配方；`usage` 必须填写 `id`，查找用途并包含催化剂行为，不受 Ctrl 状态影响。后两种模式均可通过 `handlerId` 限定处理器。

`handlerId` 按 NEI 的 overlay identifier 或 handler ID 忽略大小写精确匹配，不接受 Java 类名或部分标识。

省略命名空间时使用当前页面命名空间，保留注册名大小写、元数据和 SNBT。通配元数据沿用现有具体物品栈规则，不表示查询所有元数据变体。URI 参数仅按严格 UTF-8 解码一次，字面量 `+` 保持为 `+`。SNBT 及 `&`、`#`、`%`、`?`、`)` 等保留字符应进行百分号编码。

重复或未知参数、错误转义、片段、不支持的模式、冲突属性及缺少目标会产生编写错误。仅带 `&` 的动作及 `<NeiRecipeLink>` 是 NEI 入口；普通链接、引用链接和 HTML 锚点不作为别名，也不支持 `nei://`。

NEI 缺失或没有匹配配方时，指南保持打开并显示提示。浏览器和 IDE 预览显示不可点击的文字；在 GuideNH 中打开页面即可使用链接。

## 相关参考

- [标签参考](Tags-Reference)
- [模组兼容](Mod-Compatibility)
- [示例](Examples)
