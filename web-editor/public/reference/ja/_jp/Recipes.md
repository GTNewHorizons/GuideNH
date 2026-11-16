# レシピ

アイテムや流体の生産方法と用途を表示します。機械レシピには対応する連携 Mod が必要です。バニラのクラフトレシピは NEI なしでも使用できます。

## タグの選択

| タグ | 検索対象 | 既定の件数 |
| --- | --- | --- |
| `<Recipe>` / `<RecipeFor>` | 対象を生産するレシピ | 1 件 |
| `<Usage>` / `<RecipeUsage>` | 対象を消費するレシピ | 1 件 |
| `<RecipesFor>` | 対象を生産するレシピ | 全件 |
| `<RecipesUsage>` | 対象を消費するレシピ | 全件 |

6 つのタグは同じ属性を使用します。件数は `limit`、特定のレシピは `recipeIndex` で指定します。

## 属性

対象には `id`（アイテム）または `fluid`（流体）のどちらか一方だけを指定します。流体には `water`、`steam` など、大文字小文字を区別する完全な Forge 登録名を使用します。一時的な表示アイテムの数値 ID は使用しません。

| 属性 | 意味 |
| --- | --- |
| `id` | 対象アイテムの参照。`fluid` と併用不可 |
| `fluid` | 対象流体の登録名。`id` と併用不可 |
| `fallbackText` | 利用できるレシピがない場合の表示テキスト |
| `handlerName` | ハンドラー表示名の部分一致。大文字小文字を区別しない |
| `handlerId` | overlay/handler ID の完全一致、またはハンドラーの単純クラス名の部分一致。大文字小文字を区別しない |
| `handlerBlacklist` | 除外するハンドラーのカンマ区切りリスト |
| `handlerWhitelist` | ブラックリストから除外するハンドラー。単独では検索範囲を絞らない |
| `handlerOrder` | 絞り込み後のハンドラー位置。0 始まりの非負整数 |
| `recipeIndex` | レシピ位置。0 始まりの非負整数 |
| `input` / `output` | 入力/出力アイテムのフィルター式 |
| `inputFluid` / `outputFluid` | 入力/出力流体のフィルター式 |
| `limit` | 表示件数の上限。正の整数 |

連携が利用できない場合は `fallbackText`、未指定なら編集エラーを表示します。流体入り容器はアイテム材料として扱われます。

## 参照とフィルター

アイテム参照は `modid:name`、`modid:name:meta`、`modid:name:meta:{snbt}` を使用します。メタデータのワイルドカードには `*`、`32767`、`ANY` などの大文字トークンを使用できます。

アイテムと流体のフィルターは `,`（OR）、`&`（AND）、`!`（NOT）を使用します。指定した入力/出力フィルターをすべて満たす必要があります。アイテムの SNBT 複合タグやリスト内の区切り文字は参照を分割しません。流体は登録名を照合し、補助出力スロットも確認します。数量や NBT は照合しません。不正な式は編集エラーになります。

## ハンドラーの選択

既知のハンドラーには `handlerId`、表示名には `handlerName`、絞り込み後の位置には `handlerOrder` を使用します。`recipeIndex` は検索内のレシピを選択します。

ブラックリストとホワイトリストは handler ID、overlay ID、クラス名を大文字小文字を区別せず部分一致で照合します。ページのブラックリストは `config/guidenh/guidenh.cfg` の `recipeHandlerBlacklist` に追加されます。`handlerId` または `handlerWhitelist` を明示すると、除外されたハンドラーを残せます。

構造全体を表示する次のハンドラーは既定で除外されます。ホワイトリストでページごとに有効にできます。

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

## 例

```md
<RecipeFor id="minecraft:crafting_table" />
<RecipesFor id="minecraft:torch" recipeIndex="0" />
<RecipeFor fluid="steam" fallbackText="蒸気のレシピがありません。" />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
<RecipesFor id="minecraft:iron_ingot" outputFluid="steam,water" limit="3" />
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" limit="3" />
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" limit="6" />
<RecipeFor id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="マルチブロックを表示できません。" />
```

広い検索には `limit` を指定してください。[タグリファレンス](Tags-Reference)、[Mod 互換性](Mod-Compatibility)、[例](Examples)も参照してください。
