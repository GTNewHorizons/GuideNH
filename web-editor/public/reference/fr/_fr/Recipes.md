# Recettes


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

GuideNH peut rendent crafting et NEI-backed recipes directly dans guide pages.

## Pris en charge balises

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

tous trois share Le même compilateur et attribut Définissez.

## balise Semantics

| balise              | Behavior                                               |
|------------------|--------------------------------------------------------|
| `<Recipe>`       | rendent Un unique recipe pour Le cible élément             |
| `<Usage>`        | rendent Un unique recipe using Le cible élément           |
| `<RecipeFor>`    | même unique-recipe behavior, plus author-friendly nom |
| `<RecipeUsage>`  | même unique-recipe behavior, plus author-friendly nom |
| `<RecipesFor>`   | rendent plusieurs correspondant recipes                       |
| `<RecipesUsage>` | rendent plusieurs correspondant recipes                       |

Si plusieurs recipes exist et you utiliser Le unique-recipe forms, GuideNH renders seulement un résultat sauf filters narrow it further.

## courant attributs

| attribut      | obligatoire | Meaning                                           |
|----------------|----------|---------------------------------------------------|
| `id`           | oui      | cible élément reference                             |
| `fallbackText` | non       | texte affiché lorsque non usable recipe est found         |
| `handlerName`  | non       | case-insensitive substring filtre on handler nom |
| `handlerId`    | non       | exact overlay/handler id filtre, case-insensitive |
| `handlerBlacklist` | non   | comma-separated handlers vers drop depuis Le results |
| `handlerWhitelist` | non   | comma-separated handlers vers Conservez, even Si blacklisted |
| `handlerOrder` | non       | 0-based index après handler filtering             |
| `input`        | non       | ingredient filtre expression                      |
| `output`       | non       | résultat filtre expression                          |
| `limit`        | non       | positive entier max nombre of rendu recipes   |

## élément Id syntaxe

Le `id`, `input`, et `output` attributs tous utiliser Le GuideNH extended élément reference format:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard meta valeurs:

- `*`
- `32767`
- uppercase tokens comme `ANY`

## filtre expression syntaxe

Le `input` et `output` filters prise en charge:

- `,` pour ou
- `&` pour et
- `!` pour ne

Exemples:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT peut être combined avec tous trois élément-reference attributs. Commas et ampersands dans an
SNBT compound/liste sont kept dans Le reference; seulement haut-level separators split Le filtre:

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"Special Stone"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"Reward"}}'
/>
```

## rendu ordre

GuideNH tries recipes dans ceci ordre:

1. NEI native handler rendu
2. NEI slot-données repli
3. built-dans vanilla crafting repli

Si nothing correspond:

- `fallbackText` est utilisé lorsque present
- otherwise Un inline authoring erreur est affiché

## Exemples

### unique recipe

````md
<RecipeFor id="minecraft:crafting_table" />
````

### plusieurs recipes

````md
<RecipesFor id="minecraft:torch" />
````

### Handler filtering

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Handler blacklist et whitelist

`handlerBlacklist` drops handlers depuis Le results, `handlerWhitelist` puts eux back. les deux take a
**comma-separated liste**, et Un entrée correspond Le handler id, son overlay id ou son class nom,
case-insensitively et as Un substring — so un entrée peut nom Un unique handler ou Un whole package.

Un handler est dropped lorsque it est named by Un blacklist **sauf** Le balise asks pour it: `handlerId`,
`handlerWhitelist` et Un lowercased class-nom correspondre tous Conservez it.

**Un liste, because un handler id rarely appears alone.** Planks sont les deux Un crafting material et Un fuel, so
liste Le handler ids you do ne want:

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**Un package dans un entrée.** Un entrée est un substring of Le class nom too, so Un mod id covers chaque handler
que mod registers — handy lorsque un mod's handlers sont tous noise pour ceci élément:

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**Whitelist, vers undo Un blacklist pour un page.** Le configuration masque deux multiblock handlers on chaque
page. Un whitelist naming eux brings eux back here, et Un liste works Le même way:

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
````

**Whitelist fait ne filtre on son propre.** It seulement rescues handlers Un blacklist would drop; utiliser
`handlerId` / `handlerName` / `handlerOrder` vers narrow Le résultat vers un handler, et `limit` vers cap how
many sont drawn.

Le configuration fichier `config/guidenh/guidenh.cfg` holds `recipeHandlerBlacklist`, qui s’applique vers chaque
page et by par défaut contient:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

Un page's propre liste est **ajouté** vers Le configured un, so Un page peut masquer plus but ne fewer. Editing Le
configuration modifications chaque page; naming Un handler on Un balise modifications seulement que page.

### entrée/sortie filtering

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### repli texte

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## Best Practices

- utiliser `fallbackText` pour facultatif-mod integrations
- utiliser `handlerId` lorsque you know Le exact NEI handler you want
- utiliser `handlerBlacklist` lorsque un bloc est produced by many handlers et seulement some sont worth showing
- Le deux handlers en dessous sont masquées by par défaut dans `config/guidenh/guidenh.cfg` because Elles rendent Un whole multiblock; nom un avec `handlerId` ou `handlerWhitelist` vers afficher it
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- utiliser `limit` lorsque Un balise could expand dans many recipes
- Conservez complex filtre logic dans comments près Le balise pour maintainability

## Live exécution Exemple

Voir `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` pour extensive recipe samples, including handler filters et wildcard/NBT élément ids.
