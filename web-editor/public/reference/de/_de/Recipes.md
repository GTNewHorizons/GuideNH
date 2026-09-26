# Rezepte


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

GuideNH kann rendern crafting und NEI-backed recipes directly innerhalb Leitfaden pages.

## unterstützt Tags

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

alle drei share Die gleich Compiler und Attribut Setzen Sie.

## Tag Semantics

| Tag              | Behavior                                               |
|------------------|--------------------------------------------------------|
| `<Recipe>`       | rendern Eine einzeln recipe für Die Ziel Element             |
| `<Usage>`        | rendern Eine einzeln recipe using Die Ziel Element           |
| `<RecipeFor>`    | gleich einzeln-recipe behavior, mehr author-friendly Name |
| `<RecipeUsage>`  | gleich einzeln-recipe behavior, mehr author-friendly Name |
| `<RecipesFor>`   | rendern mehrere passend recipes                       |
| `<RecipesUsage>` | rendern mehrere passend recipes                       |

Wenn mehrere recipes exist und you verwenden Die einzeln-recipe forms, GuideNH rendert nur eins Ergebnis außer Filter narrow it further.

## allgemein Attribut

| Attribut      | erforderlich | Bedeutung                                           |
|----------------|----------|---------------------------------------------------|
| `id`           | ja      | Ziel Element reference                             |
| `fallbackText` | nein       | Text angezeigt wenn nein usable recipe ist found         |
| `handlerName`  | nein       | case-insensitive substring Filter auf handler Name |
| `handlerId`    | nein       | exact overlay/handler id Filter, case-insensitive |
| `handlerBlacklist` | nein   | comma-separated handlers zu drop von Die results |
| `handlerWhitelist` | nein   | comma-separated handlers zu Beibehalten, even Wenn blacklisted |
| `handlerOrder` | nein       | 0-based index nach handler filtering             |
| `input`        | nein       | ingredient Filter Ausdruck                      |
| `output`       | nein       | Ergebnis Filter Ausdruck                          |
| `limit`        | nein       | positive Ganzzahl max Zahl von gerendert recipes   |

## Element Id Syntax

Die `id`, `input`, und `output` Attribut alle verwenden Die GuideNH extended Element reference format:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard meta Werte:

- `*`
- `32767`
- uppercase tokens such als `ANY`

## Filter Ausdruck Syntax

Die `input` und `output` Filter Unterstützung:

- `,` für oder
- `&` für und
- `!` für nicht

Beispiele:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT kann sein kombiniert mit alle drei Element-reference Attribut. Commas und ampersands innerhalb ein
SNBT compound/Liste sind kept innerhalb Die reference; nur oben-level separators split Die Filter:

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"Special Stone"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"Reward"}}'
/>
```

## Darstellung Reihenfolge

GuideNH tries recipes in dies Reihenfolge:

1. NEI native handler Darstellung
2. NEI slot-Daten Fallback
3. built-in vanilla crafting Fallback

Wenn nothing stimmt überein:

- `fallbackText` ist verwendet wenn present
- otherwise Eine inline authoring Fehler ist angezeigt

## Beispiele

### einzeln recipe

````md
<RecipeFor id="minecraft:crafting_table" />
````

### mehrere recipes

````md
<RecipesFor id="minecraft:torch" />
````

### Handler filtering

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Handler blacklist und whitelist

`handlerBlacklist` drops handlers von Die results, `handlerWhitelist` puts sie back. beide take ein
**comma-separated Liste**, und Eine Eintrag stimmt überein Die handler id, seine overlay id oder seine class Name,
case-insensitively und als Eine substring — so eins Eintrag kann Name Eine einzeln handler oder Eine whole package.

Eine handler ist dropped wenn it ist named by Eine blacklist **außer** Die Tag asks für it: `handlerId`,
`handlerWhitelist` und Eine lowercased class-Name übereinstimmen alle Beibehalten it.

**Eine Liste, becaVerwenden Sie eins handler id rarely appears alone.** Planks sind beide Eine crafting material und Eine fuel, so
Liste Die handler ids you do nicht want:

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**Eine package in eins Eintrag.** Eine Eintrag ist ein substring von Die class Name too, so Eine mod id covers jede handler
dass mod registers — handy wenn eins mod's handlers sind alle noise für dies Element:

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**Whitelist, zu undo Eine blacklist für eins Seite.** Die configuration hides zwei multiblock handlers auf jede
Seite. Eine whitelist naming sie brings sie back here, und Eine Liste works Die gleich way:

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
````

**Whitelist tut nicht Filter auf seine eigene.** It nur rescues handlers Eine blacklist would drop; verwenden
`handlerId` / `handlerName` / `handlerOrder` zu narrow Die Ergebnis zu eins handler, und `limit` zu cap how
many sind drawn.

Die configuration Datei `config/guidenh/guidenh.cfg` holds `recipeHandlerBlacklist`, das auf zu jede
Seite und by Standard enthält:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

Eine Seite's eigene Liste ist **added** zu Die configured eins, so Eine Seite kann ausblenden mehr but nicht fewer. Editing Die
configuration Änderungen jede Seite; naming Eine handler auf Eine Tag Änderungen nur dass Seite.

### Eingabe/Ausgabe filtering

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### Fallback Text

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## Best Practices

- verwenden `fallbackText` für optional-mod integrations
- verwenden `handlerId` wenn you know Die exact NEI handler you want
- verwenden `handlerBlacklist` wenn eins Block ist produced by many handlers und nur some sind worth showing
- Die zwei handlers unter sind verborgen by Standard in `config/guidenh/guidenh.cfg` becaVerwenden Sie Sie rendern Eine whole multiblock; Name eins mit `handlerId` oder `handlerWhitelist` zu anzeigen it
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- verwenden `limit` wenn Eine Tag could expand in many recipes
- Beibehalten complex Filter logic in comments nahe Die Tag für maintainability

## Live Laufzeit Beispiel

Siehe `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` für extensive recipe samples, including handler Filter und wildcard/NBT Element ids.
