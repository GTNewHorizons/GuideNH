# Rezepte

Die ausführliche Referenz auf dieser Seite ist auf Englisch verfügbar. Tag-Namen, Attribute, IDs und Pfade werden unverändert verwendet.

Use recipe tags to show how an item or fluid is produced or consumed. Machine recipes depend on the installed recipe integrations; vanilla crafting is available without NEI.

## Choosing a tag

| Tag | Query | Default result count |
| --- | --- | --- |
| `<Recipe>` / `<RecipeFor>` | Produces the target | One |
| `<Usage>` / `<RecipeUsage>` | Consumes the target | One |
| `<RecipesFor>` | Produces the target | All matches |
| `<RecipesUsage>` | Consumes the target | All matches |

All six tags accept the same attributes. Use `limit` to cap the result count and `recipeIndex` to select a specific recipe.

## Attributes

Provide exactly one target: `id` for an item or `fluid` for a fluid. Fluid names are exact, case-sensitive Forge registry names such as `water` or `steam`; use registered names rather than temporary numeric display IDs.

| Attribute | Meaning |
| --- | --- |
| `id` | Target item reference; mutually exclusive with `fluid` |
| `fluid` | Target fluid registry name; mutually exclusive with `id` |
| `fallbackText` | Text displayed when no usable recipe is found |
| `handlerName` | Case-insensitive substring of the recipe handler's display name |
| `handlerId` | Exact overlay/handler ID, case-insensitive; also accepts a substring of the handler's simple class name |
| `handlerBlacklist` | Comma-separated handlers to exclude |
| `handlerWhitelist` | Comma-separated handlers to retain despite a blacklist; does not narrow results by itself |
| `handlerOrder` | Nonnegative, zero-based index into the filtered handler list |
| `recipeIndex` | Nonnegative, zero-based recipe index |
| `input` / `output` | Item ingredient/result filter expressions |
| `inputFluid` / `outputFluid` | Fluid ingredient/result filter expressions |
| `limit` | Positive integer maximum result count |

```md
<RecipeFor id="minecraft:crafting_table" />
<RecipesFor id="minecraft:torch" limit="3" />
<RecipeFor fluid="steam" fallbackText="No steam recipe is available." />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
```

If an optional recipe integration is unavailable, `fallbackText` is displayed; without it, the page shows an authoring error. Filled containers remain item ingredients.

## Item references

`id`, `input` and `output` accept these forms:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard metadata may be written as `*`, `32767` or an uppercase token such as `ANY`.

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
```

## Ingredient filters

Both item and fluid filters support `,` for OR, `&` for AND and `!` for NOT. Every supplied input/output filter must match. Commas and ampersands inside an item's SNBT compound or list stay within that reference.

```md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
<RecipesFor id="minecraft:iron_ingot" outputFluid="steam,water" />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
```

Fluid filters match registry names, including fluids in auxiliary output slots. They do not match amounts or NBT. Invalid filter syntax produces an authoring error.

## Handler selection

Use `handlerId` for a known recipe handler, `handlerName` for a display-name match or `handlerOrder` to choose from the filtered list. `recipeIndex` selects a recipe within the query.

```md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" limit="3" />
<RecipeFor id="minecraft:torch" recipeIndex="0" />
```

Blacklist and whitelist entries match handler IDs, overlay IDs and class names as case-insensitive substrings. Page blacklists add to `recipeHandlerBlacklist` in `config/guidenh/guidenh.cfg`. Explicit `handlerId` or `handlerWhitelist` selection can retain a blacklisted handler.

```md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" limit="6" />
<RecipeFor id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
```

The following multiblock handlers are excluded by default because they render a whole structure:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

Use a whitelist to include them on a page. Use `limit` for broad queries to keep pages readable.

## NEI-Rezeptlinks im Fließtext

Mit `&[Text](nei:...)` oder `<NeiRecipeLink>` lässt sich NEI aus dem Fließtext öffnen. Aktionsbeschriftungen sind einfacher Text; das Tag unterstützt formatierte Inhalte.

```md
&[Alle Koksofenrezepte anzeigen](nei:handler?handlerId=gt.recipe.cokeoven)
&[Rezepte anzeigen](nei:recipes?id=minecraft:iron_ingot)
&[Verwendungen anzeigen](nei:usage?id=gregtech:gt.blockmachines:236&handlerId=gt.recipe.cokeoven)

<NeiRecipeLink handlerId="gt.recipe.cokeoven">Alle Koksofenrezepte anzeigen</NeiRecipeLink>
<NeiRecipeLink id="minecraft:iron_ingot">**Rezepte** anzeigen</NeiRecipeLink>
<NeiRecipeLink mode="usage" id="gregtech:gt.blockmachines:236" handlerId="gt.recipe.cokeoven" title="NEI">Verwendungen anzeigen</NeiRecipeLink>

&[Buchrezepte mit SNBT anzeigen](nei:recipes?id=minecraft:written_book:0:%7Btitle:%22Guide%2BNEI%22%2Cauthor:GuideNH%7D)
```

`handler` benötigt `handlerId`, verbietet `id` und öffnet alle Rezepte des gewählten Handlers. `recipes` benötigt `id` und öffnet Rezepte, die diesen Gegenstand erzeugen. `usage` benötigt `id` und öffnet Verwendungen einschließlich Katalysatoren, unabhängig vom Zustand der Strg-Taste. Die letzten beiden Modi erlauben einen optionalen Filter mit `handlerId`.

`handlerId` muss exakt der NEI-Overlay-Kennung oder Handler-ID entsprechen; Groß- und Kleinschreibung spielt keine Rolle. Java-Klassennamen und Teilkennungen werden nicht akzeptiert.

Gegenstands-IDs ohne Namensraum verwenden den Namensraum der aktuellen Seite. Schreibweise des Registrierungsnamens, Metadaten und SNBT bleiben erhalten. Platzhalter für Metadaten folgen den bestehenden Regeln für konkrete Gegenstandsstapel und wählen nicht alle Varianten aus.

URI-Werte werden genau einmal als striktes UTF-8 dekodiert; ein wörtliches `+` bleibt `+`. SNBT und reservierte Zeichen wie `&`, `#`, `%`, `?` und `)` müssen prozentkodiert werden.

Doppelte oder unbekannte Parameter, ungültige Escape-Sequenzen, Fragmente, nicht unterstützte Modi, widersprüchliche Attribute und fehlende Ziele erzeugen Schreibfehler. Nur Aktionen mit `&` und `<NeiRecipeLink>` öffnen NEI; gewöhnliche Links, Referenzlinks, HTML-Anker und `nei://` sind keine alternative Schreibweise.

Fehlt NEI oder gibt es keine passenden Rezepte, bleibt der Guide geöffnet und zeigt eine Meldung. Browser- und IDE-Vorschauen zeigen nicht anklickbaren Text. Öffne die Seite in GuideNH, um den Link zu verwenden.

## Related references

- [Tags Reference](Tags-Reference)
- [Mod Compatibility](Mod-Compatibility)
- [Examples](Examples)
