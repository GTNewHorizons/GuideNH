[English](Recipes)

# 配方

GuideNH 可以直接在指南页面中渲染合成配方和基于 NEI 的配方。

## 支持的标签

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

三者使用同一套编译器和属性集。

## 标签语义

| 标签 | 行为 |
| --- | --- |
| `<Recipe>` | 为目标物品渲染单个配方 |
| `<Usage>` | 使用目标物品渲染单个配方 |
| `<RecipeFor>` | 与单配方行为相同，但命名更易读 |
| `<RecipeUsage>` | 与单配方行为相同，但命名更易读 |
| `<RecipesFor>` | 渲染多个匹配配方 |
| `<RecipesUsage>` | 渲染多个匹配配方 |

如果存在多个配方，而你使用单配方形式，GuideNH 默认只会渲染其中一个，除非额外过滤将结果缩小。

## 通用属性

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 目标物品引用 |
| `fallbackText` | 否 | 没有可用配方时显示的文本 |
| `handlerName` | 否 | 对 handler 名称做大小写不敏感的子串过滤 |
| `handlerId` | 否 | 对 overlay/handler id 做大小写不敏感的精确过滤 |
| `handlerBlacklist` | 否 | 逗号分隔，将这些 handler 从结果中剔除 |
| `handlerWhitelist` | 否 | 逗号分隔，保留这些 handler（即使被拉黑） |
| `handlerOrder` | 否 | 过滤后 handler 的 0 基索引 |
| `input` | 否 | 输入物品过滤表达式 |
| `output` | 否 | 输出物品过滤表达式 |
| `limit` | 否 | 正整数，限制最多渲染的配方数 |

## 物品 id 语法

`id`、`input` 和 `output` 都使用 GuideNH 扩展物品引用格式：

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

通配 meta 写法：

- `*`
- `32767`
- 大写标记，例如 `ANY`

## 过滤表达式语法

`input` 和 `output` 支持：

- `,` 表示 OR
- `&` 表示 AND
- `!` 表示 NOT

示例：

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT 可以和这三个物品引用属性组合使用。SNBT 复合标签或列表内部的逗号、`&` 会保留在
引用中，只有最外层的分隔符才会拆分过滤表达式：

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"特殊石头"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"奖励"}}'
/>
```

## 渲染顺序

GuideNH 会按以下顺序尝试配方：

1. NEI 原生 handler 渲染
2. NEI 槽位数据回退渲染
3. 内置原版合成回退渲染

如果都没有匹配：

- 若设置了 `fallbackText`，则显示它
- 否则显示内联编写错误

## 示例

### 单个配方

````md
<RecipeFor id="minecraft:crafting_table" />
````

### 多个配方

````md
<RecipesFor id="minecraft:torch" />
````

### Handler 过滤

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Handler 黑名单与白名单

`handlerBlacklist` 将 handler 从结果中剔除，`handlerWhitelist` 再放回来。两者都接受**英文逗号分隔的列表**，条目会与 handler id、overlay id 或类名比对，不区分大小写且按包含处理 —— 因此一条就能指名单个 handler 或整个包。

命中黑名单的 handler 会被剔除，**除非**标签主动要它：`handlerId`、`handlerWhitelist`，以及类名的小写匹配都能保住它。

**为什么是列表：单个 handler id 很少独自出现。**木板既是合成材料又是燃料，把不想要的 handler id 列出来：

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**一条覆盖整个包。**条目同样按类名做子串匹配，所以一个模组 id 就能覆盖它注册的全部 handler —— 当某个模组的 handler 对这个物品全是噪音时很方便：

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**白名单：在单个页面上撤销黑名单。**配置对所有页面隐藏了两个多方块 handler，在这里用白名单指名即可放回；写成列表同理：

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="没有多方块预览。" />
````

**白名单本身不做筛选。**它只负责把黑名单要剔除的 handler 救回来；要把结果收窄到某个 handler，用 `handlerId` / `handlerName` / `handlerOrder`，要限制渲染数量则用 `limit`。

配置项 `recipeHandlerBlacklist`（`config/guidenh/guidenh.cfg`）对所有页面生效，默认包含：

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

页面自身的列表是**追加**到配置之后的，所以页面只能比配置隐藏更多、不能更少。改配置会影响所有页面；在标签上指名只影响该页面。

### 输入/输出过滤

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### 回退文本

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## 最佳实践

- 面向可选模组整合时，优先提供 `fallbackText`
- 若已知确切的 NEI handler，优先使用 `handlerId`
- 当同一物品被大量 handler 命中、而只有部分值得展示时，使用 `handlerBlacklist`
- 以下两个 handler 默认在 `config/guidenh/guidenh.cfg` 中被隐藏，因为它们渲染的是整个多方块结构；用 `handlerId` 或 `handlerWhitelist` 指名即可显示
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- 当标签可能展开出大量配方时，使用 `limit`
- 复杂过滤逻辑最好在标签旁边用注释解释，便于维护

## 实时运行示例

可参考 `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md`，其中包含大量配方示例，包括 handler 过滤、通配 meta 和带 NBT 的物品 id。

## 行内 NEI 配方链接

使用自定义动作链接，可以在正文中打开 NEI，而不渲染配方卡片。语法沿用已有的 `&[文字](sound:...)`。

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

`<NeiRecipeLink>` 仅接受 `id`、`handlerId`、`mode` 和可选提示 `title`。省略 `mode` 时，有 `id` 则使用 `recipes`，否则使用 `handler`。必须提供可见的行内子内容。富文本使用标签形式；自定义动作的文字按纯文本处理。

处理器按 overlay identifier 或 handler ID 忽略大小写精确匹配。与现有配方卡片的 `handlerId` 筛选不同，此链接不使用类名子串回退。查询包含普通和串行处理器，共享 Java 类的不同配方类别不会被合并。

省略命名空间时使用当前页面命名空间，保留注册名大小写、元数据和 SNBT。通配元数据沿用现有具体物品栈规则，不表示查询所有元数据变体。URI 参数仅按严格 UTF-8 解码一次，字面量 `+` 保持为 `+`。SNBT 及 `&`、`#`、`%`、`?`、`)` 等保留字符应进行百分号编码。

重复或未知参数、错误转义、片段、不支持的模式、冲突属性及缺少目标会产生编写错误。仅带 `&` 的动作及 `<NeiRecipeLink>` 是 NEI 入口；普通链接、引用链接和 HTML 锚点不作为别名，也不支持 `nei://`。

仅在点击时解析物品并查询 NEI。NEI 缺失或没有结果时保留指南并给出本地化提示。Web Editor、导出站点和 IDE 预览显示不可点击文字，提示在 GuideNH 中打开指南。
