---
navigation:
  title: RecipesUsage Tag
  position: 9090
---

TEST GOAL / 测试目标：`<RecipesUsage>` 标签（usage 查询）渲染真实配方框

INVARIANTS / 不变式：usage 查询产生 `LytNeiRecipeBox` 配方框；limit 生效；无红色错误文本

Syntax reference: `RecipeCompiler` — `<RecipesUsage id="<item>" limit="<n>" />` shares the `<Recipe>` attribute set; tag names ending in `Usage` run a "recipes that use this item" query (`usageQuery`).

## Stick As Ingredient

Here it should: render up to 3 recipe boxes for recipes that use `minecraft:stick` as an ingredient — a usage query, not a crafting-of query.

<RecipesUsage id="minecraft:stick" limit="3" />

## Planks As Ingredient

Here it should: render up to 2 recipe boxes for recipes that consume `minecraft:planks` — the usage query filters to recipes referencing planks as an ingredient.

<RecipesUsage id="minecraft:planks" limit="2" />

## Iron Ingot As Ingredient

Here it should: render at least one recipe box for recipes that use `minecraft:iron_ingot` as an ingredient.

<RecipesUsage id="minecraft:iron_ingot" limit="2" />
