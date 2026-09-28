# Navigation


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

GuideNH builds son navigation tree depuis page frontmatter.

dans Le dans-game sidebar, expanded ancestor pages stay pinned at Le haut pendant leurs encore-visible
descendants défiler underneath. plusieurs expanded ancestor levels peut stack at once, et chaque sticky
ligne est pushed away seulement lorsque son entire visible subtree scrolls out, similar vers Le VSCode fichier
explorer.

## Navigation Frontmatter

Le `navigation` map controls whether Un page appears dans Le guide tree.

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### champ Reference

| champ | description |
| --- | --- |
| `title` | obligatoire afficher titre |
| `keyword` | facultatif unique recherche keyword ou alias |
| `keywords` | facultatif liste of recherche keywords ou aliases |
| `parent` | facultatif parent page id, résolu like Un guide page lien |
| `position` | facultatif sibling ordering hint |
| `recommend` | facultatif home-page recommendation priority; absent signifie Le page est ne affiché dans Le Recommended panel |
| `priority` | facultatif charger priority pour même-chemin page remplace; par défaut `0` |
| `icon` | facultatif unique élément icon; prend en charge inline `mod:item:meta:{snbt}` tails |
| `icons` | facultatif cycling élément icons liste; entrées peut être simple élément ids avec facultatif inline `:{snbt}` ou `{id, meta?, nbt?}` maps |
| `icon_texture` | facultatif texture icon résolu depuis guide assets |
| `icon_textures` | facultatif cycling texture icon liste |
| `required_mod` | facultatif unique mod id; page est masquées lorsque ceci mod est ne chargé |
| `required_mods` | facultatif liste of mod ids; page est masquées sauf tous listed mods sont chargé |
| `excluded_mod` | facultatif unique mod id; page est masquées lorsque ceci mod est chargé |
| `excluded_mods` | facultatif liste of mod ids; page est masquées lorsque quelconque listed mod est chargé |

### `navigation.position`

`navigation.position` est un facultatif entier utilisé vers ordre sibling pages dans Le navigation tree.

- manquant `position` defaults vers `0`.
- Larger valeurs appear earlier.
- Si deux pages have Le même valeur, Elles sont sorted by titre alphabetically.

### recherche Keywords

`navigation.keyword` adds un recherche alias. `navigation.keywords` adds Un liste of aliases. les deux champs
peut être utilisé together; valeurs sont trimmed et duplicate valeurs sont ignored. Keyword correspond utiliser Le même
langue analyzer et prefix correspondant as titre et page contenu searches, pendant results continue vers afficher
Le normal navigation titre.

```yaml
navigation:
  title: Molecular Assembler
  keyword: assembler
  keywords:
    - molecular assembler
    - autocrafting machine
```

## Home page Recommendations

### `navigation.recommend`

`navigation.recommend` est un facultatif entier utilisé by Le home page Recommended panel.

- Pages seulement appear dans Le Recommended panel lorsque ceci champ est present.
- `0` est valide.
- Larger valeurs appear earlier.
- Si deux pages have Le même valeur, Elles sont sorted by titre alphabetically.
- Le panel works at Le `GuidePage` level, so chaque recommended page entrée jumps directly vers que page.

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## Mod Requirements

utiliser `required_mod` ou `required_mods` vers require un ou plus chargé mods. utiliser `excluded_mod` ou
`excluded_mods` vers masquer Un page lorsque un ou plus incompatible mods sont chargé. lorsque Un condition est ne
met, Le page est excluded depuis Le navigation tree et tous page indices (élément, category, etc.) so it
ne peut pas être found à travers navigation ou recherche.

```yaml
navigation:
  title: Applied Energistics Integration
  parent: index.md
  required_mod: appliedenergistics2

navigation:
  title: Multi-Mod Feature
  parent: index.md
  required_mods:
    - gregtech
    - appliedenergistics2
```

les deux clés peut être combined; Le page est seulement affiché lorsque chaque listed mod est present.

obligatoire et excluded conditions peut aussi être combined. tous obligatoire mods doit être chargé et none of Le
excluded mods peut être chargé.

```yaml
navigation:
  title: Client-only Integration
  parent: index.md
  required_mod: guide_api
  excluded_mods:
    - incompatible_addon
    - legacy_addon
```

## charger Priority

lorsque several chargé ressource packs provide Le même guide page chemin, GuideNH reads Le page
frontmatter premier et chooses Le candidate avec Le highest `navigation.priority`.

```yaml
navigation:
  title: Pack Override
  parent: index.md
  priority: 100
```

Règles:

- manquant `priority` est `0`
- valeurs sont signed Java integers up vers `2147483647`
- higher priority wins
- Si priorities sont equal, Le later processed ressource pack entrée wins, correspondant Minecraft ressource-pack remplacer ordre
- priority seulement decides entre candidates pour Le même page chemin et langue/repli layer

ceci est useful lorsque Un mod ships Un baseline guide page et Un pack wants vers remplacer it sans relying seulement on
ressource-pack ordering.

## Icon Sources

GuideNH chooses navigation/recherche icons dans ceci ordre:

1. `icon_textures` Si at least un texture entrée est configured
2. `icon_texture` Si Le texture fichier charge successfully
3. `icons` Si at least un configured élément resolves successfully
4. `icon` Si Le élément exists
5. non icon Si neither est usable

Texture icons sont lire depuis exécution assets, so relatif page-local files comme `test1.png` fonctionnent.

## parent et racine Nodes

- Omit `parent` vers créer Un racine node.
- Définissez `parent: index.md` ou quelconque autre page id vers créer Un enfant node.
- Le parent page doit exist dans Le même guide navigation tree.

`navigation.parent` utilise Le même namespace Règles as Markdown page liens:

- `parent: index.md` et `parent: ./index.md` résoudre dans Le actuel page namespace.
- `parent: /index.md` resolves depuis Le actuel page namespace racine.
- `parent: gregtech:index.md` ou `parent: gregtech:/index.md` explicitly targets another namespace.

données-driven guides sont isolated by namespace. Pages under `assets/guidenh/guidenh/_en_us/...` belong vers
`guidenh:guidenh`; pages under `assets/gregtech/guidenh/_en_us/...` belong vers `gregtech:guidenh`.
relatif parents et liens jamais fall à travers vers another mod's même-named page.

## Category Pages

Pages peut join un ou plus named categories using frontmatter:

```yaml
categories:
  - basics
  - machines|Arc Furnace
```

chaque entrée peut être l’un ou l’autre Un category nom ou `category|sort key`.

Those categories sont queryable à travers Le built-dans `<Category name="machines" rows="3" />` balise et aussi auto-créer masquées searchable pages comme `Category:machines`.
GuideNH aussi auto-crée Le masquées searchable special pages `Special:AllPages` et `Special:Categories`.

## élément-Indexed Pages

Pages peut enregistrer élément-vers-page mappings using `item_id` (un valeur) ou `item_ids` (Un liste):

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:iron_ore#crafting
```

These mappings sont utilisé by `<ItemLink>`. `item_id` est un NEI-style expression et chaque `item_ids`
entrée est Le même kind of expression. pour Exemple, `minecraft:potion 16384-16462,!16386` correspond a
metadata range except pour `16386`, pendant `minecraft:potion 0-16,20-36,!28` combines deux metadata ranges et
excludes `28`. `ae2:white_paint_ball:*` est un compatible strict tous-meta mapping.

Un facultatif `#anchor` suffix scrolls vers Un spécifique heading lorsque Le lien est clicked.
Le anchor est formed by lowercasing Le heading texte et replacing spaces avec hyphens
(e.g. `## Crafting Recipe` → `#crafting-recipe`).

Lookup behavior:

1. exact élément + exact meta
2. wildcard-meta repli Si present
3. correspondant élément expression

## Heading Anchor liens

GuideNH prend en charge heading anchor navigation dans Markdown liens et `<a>` balises.
Anchors sont derived depuis heading texte by lowercasing et replacing spaces avec hyphens.

**même-page anchor:**

```md
[Jump to Installation](#installation)
[Jump to Crafting Recipe](#crafting-recipe)
```

**Cross-page anchor:**

```md
[See Getting Started](./Guide-Page-Format#installation)
[Another guide](other-guide.md#usage)
```

**absolu chemin anchor** (using Le guide namespace, avoids relatif chemin ambiguity dans subdirectories):

```md
[Absolute link](guidenh:other-guide.md#usage)
[Any namespace](mymods:crafting/iron.md#smelting)
```

Le `namespace:path` format resolves vers Le guide page whose id correspond `namespace:path`.
ceci est identical vers what relatif paths résoudre vers, but avoids `../` navigation.
Le page doit exist dans Le même guide as Le lien source.

**Named inline anchors** peut aussi être placed avec `<a name="...">` dans MDX:

```md
<a name="custom-anchor" />

...content...

[Jump here](#custom-anchor)
```

Navigating vers Un lien avec Un anchor scrolls Le guide vers Le cible heading ou named anchor.

## `<SubPages>`

`<SubPages>` renders liens vers navigation enfants.

### attributs

| attribut | type | par défaut | Meaning |
| --- | --- | --- | --- |
| `id` | page id ou vide chaîne | actuel page | page whose enfants devrait être listed |
| `alphabetical` | booléen expression | `false` | Sort enfants by titre à la placer of navigation ordre |

### Exemples

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

Special case: `id=""` lists racine navigation nodes.

## `<Category>`

`<Category>` renders liens vers chaque page dans Un named category.

### attributs

| attribut | type | par défaut | Meaning |
| --- | --- | --- | --- |
| `name` | chaîne | none | Category nom vers rendent |
| `rows` | positive entier | `3` | nombre of afficher columns dans Le MediaWiki-style mise en page |

````md
<Category name="machines" rows="3" />
````

Si Le category est manquant, GuideNH renders Un inline erreur.

Le même category aussi a Un auto-généré masquées searchable page at `Category:machines`.

## `<Special>`

`<Special>` renders un of Le built-dans MediaWiki-style special page listings.

### attributs

| attribut | type | par défaut | Meaning |
| --- | --- | --- | --- |
| `name` | chaîne | none | Pris en charge valeurs: `AllPages`, `Categories` |
| `rows` | positive entier | `3` | nombre of afficher columns dans Le MediaWiki-style mise en page |

````md
<Special name="AllPages" rows="4" />
<Special name="Categories" rows="3" />
````

Le même contenu est aussi disponible à travers Le masquées searchable pages `Special:AllPages` et `Special:Categories`.

## recherche résultat Titles

recherche titles sont derived dans ceci ordre:

1. `navigation.title`
2. premier level-1 heading (`# Heading`)
3. brut page id

## Related Pages

- [Guide Page Format](Guide-Page-Format)
- [Search](Search)
- [Tags Reference](Tags-Reference)
