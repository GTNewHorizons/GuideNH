---
navigation:
  title: RecipesUsage Tag
  position: 9090
---

TEST GOAL: the `<RecipesUsage>` tag (usage query) renders real recipe boxes

INVARIANTS: the usage query produces `LytNeiRecipeBox` recipe boxes; limit takes effect; no red error text

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
