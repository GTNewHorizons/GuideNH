---
navigation:
  title: Recipe Tags — glScissor Sentinel
  position: 8000
---

TEST GOAL: all-attribute variants of `<Recipe>`/`<RecipeFor>`/`<RecipesFor>` + glScissor clipping regression sentinel

INVARIANTS: recipe boxes render in full with correct clipping; fallbackText displays normally; handlerName/handlerOrder filtering works

Known backlog: `recipes.md::gamescene:40` materialization failure exists in the main guide — reproducing similar symptoms on this page is expected, not a regression.

## Basic `<Recipe id>`

Expected: Single recipe box for stick (minecraft:stick).

<Recipe id="minecraft:stick" />

## `<RecipeFor>` — Output Filter

Expected: Recipe box whose result slot matches planks.

<RecipeFor id="minecraft:planks" />

## `<RecipeFor>` — Input Filter

Expected: Recipe box for stick where at least one ingredient is any planks variant.

<RecipeFor id="minecraft:stick" input="minecraft:planks:*" />

## `<RecipesFor>` with Limit

Expected: Up to 2 recipe boxes for planks (any recipe).

<RecipesFor id="minecraft:planks" limit="2" />

## handlerName Filter

Expected: Only shaped crafting recipes for chest; shapeless and other handler types excluded.

<RecipesFor id="minecraft:chest" handlerName="shaped" limit="1" />

## handlerOrder Filter

Expected: First handler recipe (index 0) for iron pickaxe.

<Recipe id="minecraft:iron_pickaxe" handlerOrder="0" />

## handlerOrder Out-of-Range Fallback

Expected: Fallback text displayed because index 999 exceeds available handlers.

<Recipe id="minecraft:iron_pickaxe" handlerOrder="999" fallbackText="No handler at index 999 for iron pickaxe." />

## fallbackText for Unregistered Item

Expected: Fallback text displayed because the item id has no registered recipe.

<Recipe id="minecraft:example_nonexistent_item" fallbackText="This fictional item has no registered recipe." />
