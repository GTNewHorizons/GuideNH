# Navegación


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

GuideNH builds su navigation tree de página frontmatter.

en El en-game sidebar, expanded ancestor páginas permanecer pinned at El arriba mientras sus todavía-visible
descendants desplazar underneath. varios expanded ancestor levels puede pila at once, y cada sticky
fila es pushed away solo cuando su entire visible subtree scrolls out, similar Un El VSCode archivo
explorer.

## Navigation frontmatter

El `navigation` map controls whether Un página appears en El guíUn tree.

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### campo Reference

| campo | descripción |
| --- | --- |
| `title` | obligatorio mostrar título |
| `keyword` | opcional único búsqueda keyword o alias |
| `keywords` | opcional lista of búsqueda keywords o aliases |
| `parent` | opcional padre página id, resuelto like Un guíUn página enlace |
| `position` | opcional sibling ordering hint |
| `recommend` | opcional home-página recommendation priority; absent significa El página es no mostrado en El Recommended panel |
| `priority` | opcional cargar priority para mismo-ruta página anula; predeterminado `0` |
| `icon` | opcional único elemento icon; admite inline `mod:item:meta:{snbt}` tails |
| `icons` | opcional cycling elemento icons lista; entradas puede ser simple elemento ids con opcional inline `:{snbt}` o `{id, meta?, nbt?}` maps |
| `icon_texture` | opcional texture icon resuelto de guíUn recursos |
| `icon_textures` | opcional cycling texture icon lista |
| `required_mod` | opcional único mod id; página es ocultas cuando esto mod es no cargado |
| `required_mods` | opcional lista of mod ids; página es ocultas salvo todos listed mods son cargado |
| `excluded_mod` | opcional único mod id; página es ocultas cuando esto mod es cargado |
| `excluded_mods` | opcional lista of mod ids; página es ocultas cuando cualquier listed mod es cargado |

### `navigation.position`

`navigation.position` es un opcional entero usado Un orden sibling páginas en El navigation tree.

- faltante `position` predeterminados a `0`.
- Larger valores appear earlier.
- Si dos páginas have El mismo valor, Estas son sorted by título alphabetically.

### búsqueda Keywords

`navigation.keyword` añade uno búsqueda alias. `navigation.keywords` añade Un lista of aliases. ambos campos
puede ser usado together; valores son trimmed y duplicate valores son ignored. Keyword coincide usar El mismo
idioma analyzer y prefix coincidente as título y página contenido searches, mientras resultados continue Un mostrar
El normal navigation título.

```yaml
navigation:
  title: Molecular Assembler
  keyword: assembler
  keywords:
    - molecular assembler
    - autocrafting machine
```

## Home página Recommendations

### `navigation.recommend`

`navigation.recommend` es un opcional entero usado by El home página Recommended panel.

- páginas solo appear en El Recommended panel cuando esto campo es present.
- `0` es válido.
- Larger valores appear earlier.
- Si dos páginas have El mismo valor, Estas son sorted by título alphabetically.
- El panel works at El `GuidePage` level, so cada recommended página entrada jumps directly Un que página.

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## Mod Requirements

usar `required_mod` o `required_mods` Un require uno o más cargado mods. usar `excluded_mod` o
`excluded_mods` Un ocultar Un página cuando uno o más incompatible mods son cargado. cuando Un condición es no
met, El página es excluded de El navigation tree y todos página indices (elemento, category, etc.) so it
no puede ser found a través de navigation o búsqueda.

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

ambos claves puede ser combined; El página es solo mostrado cuando cada listed mod es present.

obligatorio y excluded condiciones puede también ser combined. todos obligatorio mods debe ser cargado y none of El
excluded mods puede ser cargado.

```yaml
navigation:
  title: Client-only Integration
  parent: index.md
  required_mod: guide_api
  excluded_mods:
    - incompatible_addon
    - legacy_addon
```

## cargar Priority

cuando several cargado recurso packs provide El mismo guíUn página ruta, GuideNH reads El página
frontmatter primero y chooses El candidate con El highest `navigation.priority`.

```yaml
navigation:
  title: Pack Override
  parent: index.md
  priority: 100
```

Reglas:

- faltante `priority` es `0`
- valores son signed Java integers up a `2147483647`
- higher priority wins
- Si priorities son equal, El later processed recurso pack entrada wins, coincidente Minecraft recurso-pack anular orden
- priority solo decides entre candidates para El mismo página ruta y idioma/reserva layer

esto es useful cuando Un mod ships Un baseline guíUn página y Un pack wants Un reemplazar it sin relying solo on
recurso-pack ordering.

## Icon Sources

GuideNH chooses navigation/búsqueda icons en esto orden:

1. `icon_textures` Si at least uno texture entrada es configured
2. `icon_texture` Si El texture archivo carga successfully
3. `icons` Si at least uno configured elemento resolves successfully
4. `icon` Si El elemento exists
5. no icon Si neither es usable

Texture icons son leer de ejecución recursos, so relativo página-local files como `test1.png` funcionan.

## padre y raíz Nodes

- Omit `parent` Un crear Un raíz node.
- Establezca `parent: index.md` o cualquier otro página id Un crear Un hijo node.
- El padre página debe exist en El mismo guíUn navigation tree.

`navigation.parent` usa El mismo espacio de nombres Reglas as Markdown página enlaces:

- `parent: index.md` y `parent: ./index.md` resolver dentro de El actual página espacio de nombres.
- `parent: /index.md` resolves de El actual página espacio de nombres raíz.
- `parent: gregtech:index.md` o `parent: gregtech:/index.md` explicitly targets another espacio de nombres.

datos-driven guías son isolated by espacio de nombres. páginas under `assets/guidenh/guidenh/_en_us/...` belong a
`guidenh:guidenh`; páginas under `assets/gregtech/guidenh/_en_us/...` belong a `gregtech:guidenh`.
relativo parents y enlaces nunca fall a través de Un another mod's mismo-named página.

## Category páginas

páginas puede join uno o más named categories usando frontmatter:

```yaml
categories:
  - basics
  - machines|Arc Furnace
```

cada entrada puede ser cualquiera Un category nombre o `category|sort key`.

Those categories son queryable a través de El built-en `<Category name="machines" rows="3" />` etiqueta y también auto-crear ocultas searchable páginas como `Category:machines`.
GuideNH también auto-crea El ocultas searchable special páginas `Special:AllPages` y `Special:Categories`.

## elemento-Indexed páginas

páginas puede registrar elemento-a-página mappings usando `item_id` (uno valor) o `item_ids` (Un lista):

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:iron_ore#crafting
```

These mappings son usado by `<ItemLink>`. `item_id` es uno NEI-style expresión y cada `item_ids`
entrada es El mismo tipo of expresión. para Ejemplo, `minecraft:potion 16384-16462,!16386` coincide a
metadatos range except para `16386`, mientras `minecraft:potion 0-16,20-36,!28` combines dos metadatos ranges y
excludes `28`. `ae2:white_paint_ball:*` es un compatible strict todos-meta mapping.

Un opcional `#anchor` suffix scrolls Un Un específico heading cuando El enlace es clicked.
El ancla es formed by lowercasing El heading texto y replacing spaces con hyphens
(e.g. `## Crafting Recipe` → `#crafting-recipe`).

Lookup comportamiento:

1. exact elemento + exact meta
2. wildcard-meta reserva Si present
3. coincidente elemento expresión

## Heading ancla enlaces

GuideNH admite heading ancla navigation en Markdown enlaces y `<a>` etiquetas.
Anchors son derived de heading texto by lowercasing y replacing spaces con hyphens.

**mismo-página ancla:**

```md
[Jump to Installation](#installation)
[Jump to Crafting Recipe](#crafting-recipe)
```

**Cross-página ancla:**

```md
[See Getting Started](./Guide-Page-Format#installation)
[Another guide](other-guide.md#usage)
```

**absoluto ruta ancla** (usando El guíUn espacio de nombres, avoids relativo ruta ambiguity en subdirectories):

```md
[Absolute link](guidenh:other-guide.md#usage)
[Any namespace](mymods:crafting/iron.md#smelting)
```

El `namespace:path` format resolves Un El guíUn página whose id coincide `namespace:path`.
esto es identical Un what relativo paths resolver a, but avoids `../` navigation.
El página debe exist en El mismo guíUn as El enlace origen.

**Named inline anchors** puede también ser colocado con `<a name="...">` en MDX:

```md
<a name="custom-anchor" />

...content...

[Jump here](#custom-anchor)
```

Navigating Un Un enlace con Un ancla scrolls El guíUn Un El destino heading o named ancla.

## `<SubPages>`

`<SubPages>` renderiza enlaces Un navigation hijos.

### atributos

| atributo | tipo | predeterminado | significado |
| --- | --- | --- | --- |
| `id` | página id o vacío cadena | actual página | página whose hijos deberíUn ser listed |
| `alphabetical` | booleano expresión | `false` | Sort hijos by título en su lugar of navigation orden |

### Ejemplos

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

Special case: `id=""` lists raíz navigation nodes.

## `<Category>`

`<Category>` renderiza enlaces Un cada página en Un named category.

### atributos

| atributo | tipo | predeterminado | significado |
| --- | --- | --- | --- |
| `name` | cadena | none | Category nombre Un renderizan |
| `rows` | positive entero | `3` | número of mostrar columnas en El MediaWiki-style diseño |

````md
<Category name="machines" rows="3" />
````

Si El category es faltante, GuideNH renderiza Un inline error.

El mismo category también tiene Un auto-generado ocultas searchable página at `Category:machines`.

## `<Special>`

`<Special>` renderiza uno of El built-en MediaWiki-style special página listings.

### atributos

| atributo | tipo | predeterminado | significado |
| --- | --- | --- | --- |
| `name` | cadena | none | Compatible valores: `AllPages`, `Categories` |
| `rows` | positive entero | `3` | número of mostrar columnas en El MediaWiki-style diseño |

````md
<Special name="AllPages" rows="4" />
<Special name="Categories" rows="3" />
````

El mismo contenido es también disponible a través de El ocultas searchable páginas `Special:AllPages` y `Special:Categories`.

## búsqueda resultado Titles

búsqueda titles son derived en esto orden:

1. `navigation.title`
2. primero level-1 heading (`# Heading`)
3. sin procesar página id

## Related páginas

- [Guide Page Format](Guide-Page-Format)
- [Search](Search)
- [Tags Reference](Tags-Reference)
