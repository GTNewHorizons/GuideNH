# Navegação

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


GuideNH builds seu Navegação tree de página frontmatter.

em O em-game sidebar, expanded ancestor pages stay pinned at O topo enquanto seus ainda-visível
descendants rolar underneath. vários expanded ancestor levels pode stack at once, e cada sticky
linha é pushed away somente quando seu entire visível subtree scrolls out, similar para O VSCode arquivo
explorer.

## Frontmatter de navegação

O `navigation` map controls whether Uma página appears em O guia tree.

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### Referência de campos

| campo | descrição |
| --- | --- |
| `title` | Obrigatório exibir título |
| `keyword` | opcional único pesquisa keyword ou alias |
| `keywords` | opcional lista of Palavras-chave de pesquisa ou aliases |
| `parent` | opcional pai ID da página, resolvido like Uma página do guia link |
| `position` | opcional sibling ordering hint |
| `recommend` | opcional home-página recommendation priority; absent significa O página é não exibido em O Recommended panel |
| `priority` | opcional Prioridade de carregamento para mesmo-caminho página substitui; Padrão `0` |
| `icon` | opcional único item icon; suporta inline `mod:item:meta:{snbt}` tails |
| `icons` | opcional cycling item icons lista; entradas pode ser simples item ids com opcional inline `:{snbt}` ou `{id, meta?, nbt?}` maps |
| `icon_texture` | opcional texture icon resolvido de guia assets |
| `icon_textures` | opcional cycling texture icon lista |
| `required_mod` | opcional único mod id; página é oculto quando Este mod é não carregado |
| `required_mods` | opcional lista of mod ids; página é oculto a menos que todos listed mods são carregado |
| `excluded_mod` | opcional único mod id; página é oculto quando Este mod é carregado |
| `excluded_mods` | opcional lista of mod ids; página é oculto quando qualquer listed mod é carregado |

### `navigation.position`

`navigation.position` é Um opcional inteiro usado para ordem sibling pages em O Navegação tree.

- ausente `position` defaults para `0`.
- Larger valores appear earlier.
- Se dois pages have O mesmo valor, Elas são sorted by título alphabetically.

### Palavras-chave de pesquisa

`navigation.keyword` adds um pesquisa alias. `navigation.keywords` adds Uma lista of aliases. ambos campos
pode ser usado together; valores são trimmed e duplicate valores são ignored. Keyword corresponde usar O mesmo
idioma analyzer e prefix correspondente as título e página conteúdo searches, enquanto results continue para mostrar
O normal Navegação título.

```yaml
navigation:
  title: Molecular Assembler
  keyword: assembler
  keywords:
    - molecular assembler
    - autocrafting machine
```

## Recomendações da página inicial

### `navigation.recommend`

`navigation.recommend` é Um opcional inteiro usado by O home página Recommended panel.

- Pages somente appear em O Recommended panel quando Este campo é present.
- `0` é válido.
- Larger valores appear earlier.
- Se dois pages have O mesmo valor, Elas são sorted by título alphabetically.
- O panel works at O `GuidePage` level, so cada recommended página entrada jumps directly para que página.

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## Requisitos de mods

usar `required_mod` ou `required_mods` para require um ou mais carregado mods. usar `excluded_mod` ou
`excluded_mods` para ocultar Uma página quando um ou mais incompatible mods são carregado. quando Uma condição é não
met, O página é excluded de O Navegação tree e todos página indices (item, category, etc.) so it
não pode ser found por Navegação ou pesquisa.

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

ambos chaves pode ser combined; O página é somente exibido quando cada listed mod é present.

Obrigatório e excluded condições pode também ser combined. todos Obrigatório mods deve ser carregado e none of O
excluded mods pode ser carregado.

```yaml
navigation:
  title: Client-only Integration
  parent: index.md
  required_mod: guide_api
  excluded_mods:
    - incompatible_addon
    - legacy_addon
```

## Prioridade de carregamento

quando several carregado recurso packs fornecer O mesmo página do guia caminho, GuideNH reads O página
frontmatter primeiro e chooses O candidate com O highest `navigation.priority`.

```yaml
navigation:
  title: Pack Override
  parent: index.md
  priority: 100
```

Regras:

- ausente `priority` é `0`
- valores são signed Java integers up para `2147483647`
- higher priority wins
- Se priorities são equal, O later processed pacote de recursos entrada wins, correspondente Minecraft recurso-pack substituir ordem
- priority somente decides entre candidates para O mesmo página caminho e idioma/reserva layer

Este é useful quando Uma mod ships Uma baseline página do guia e Uma pack wants para substituir it sem relying somente on
recurso-pack ordering.

## Fontes de ícones

GuideNH chooses Navegação/pesquisa icons em Este ordem:

1. `icon_textures` Se at least um texture entrada é configured
2. `icon_texture` Se O texture arquivo carrega successfully
3. `icons` Se at least um configured item resolves successfully
4. `icon` Se O item exists
5. não icon Se neither é usable

Texture icons são ler de execução assets, so relativo página-local files como `test1.png` funcionam.

## Nós pais e raiz

- Omit `parent` para criar Uma raiz node.
- Defina `parent: index.md` ou qualquer outro ID da página para criar Uma filho node.
- O pai página deve exist em O mesmo guia Navegação tree.

`navigation.parent` usa O mesmo namespace Regras as Markdown página links:

- `parent: index.md` e `parent: ./index.md` resolver dentro de O atual página namespace.
- `parent: /index.md` resolves de O atual página namespace raiz.
- `parent: gregtech:index.md` ou `parent: gregtech:/index.md` explicitly targets another namespace.

dados-driven guias são isolated by namespace. Pages under `assets/guidenh/guidenh/_en_us/...` belong para
`guidenh:guidenh`; pages under `assets/gregtech/guidenh/_en_us/...` belong para `gregtech:guidenh`.
relativo parents e links nunca fall por para another mod's mesmo-named página.

## Páginas de categorias

Pages pode join um ou mais named categories using frontmatter:

```yaml
categories:
  - basics
  - machines|Arc Furnace
```

cada entrada pode ser qualquer um Uma category nome ou `category|sort key`.

Those categories são queryable por O built-em `<Category name="machines" rows="3" />` tag e também auto-criar oculto searchable pages como `Category:machines`.
GuideNH também auto-cria O oculto searchable special pages `Special:AllPages` e `Special:Categories`.

## Páginas indexadas por itens

Pages pode registrar item-para-página mappings using `item_id` (um valor) ou `item_ids` (Uma lista):

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:iron_ore#crafting
```

Estes mappings são usado by `<ItemLink>`. `item_id` é um NEI-style expressão e cada `item_ids`
entrada é O mesmo kind of expressão. para Exemplo, `minecraft:potion 16384-16462,!16386` corresponde a
metadata range except para `16386`, enquanto `minecraft:potion 0-16,20-36,!28` combines dois metadata ranges e
excludes `28`. `ae2:white_paint_ball:*` é Uma compatible strict todos-meta mapping.

Um opcional `#anchor` suffix scrolls para Uma específico heading quando O link é clicked.
O anchor é formed by lowercasing O heading texto e replacing spaces com hyphens
(e.g. `## Crafting Recipe` → `#crafting-recipe`).

Lookup behavior:

1. exact item + exact meta
2. wildcard-meta reserva Se present
3. correspondente item expressão

## links de âncora de títulos

GuideNH suporta heading anchor Navegação em Markdown links e `<a>` tags.
Anchors são derived de heading texto by lowercasing e replacing spaces com hyphens.

**mesmo-página anchor:**

```md
[Jump to Installation](#installation)
[Jump to Crafting Recipe](#crafting-recipe)
```

**Cross-página anchor:**

```md
[See Getting Started](./Guide-Page-Format#installation)
[Another guide](other-guide.md#usage)
```

**absoluto caminho anchor** (using O guia namespace, avoids relativo caminho ambiguity em subdirectories):

```md
[Absolute link](guidenh:other-guide.md#usage)
[Any namespace](mymods:crafting/iron.md#smelting)
```

O `namespace:path` format resolves para O página do guia whose id corresponde `namespace:path`.
Este é identical para what relativo paths resolver para, but avoids `../` Navegação.
O página deve exist em O mesmo guia as O link origem.

**Named inline anchors** pode também ser placed com `<a name="...">` em MDX:

```md
<a name="custom-anchor" />

...content...

[Jump here](#custom-anchor)
```

Navigating para Uma link com Um anchor scrolls O guia para O destino heading ou named anchor.

## `<SubPages>`

`<SubPages>` renders links para Navegação filhos.

### atributos

| Atributo | Tipo | Padrão | Significado |
| --- | --- | --- | --- |
| `id` | ID da página ou vazio texto | atual página | página whose filhos deve ser listed |
| `alphabetical` | booleano expressão | `false` | Sort filhos by título em vez disso of Navegação ordem |

### Exemplos

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

Special case: `id=""` lists raiz Navegação nodes.

## `<Category>`

`<Category>` renders links para cada página em Uma named category.

### atributos

| Atributo | Tipo | Padrão | Significado |
| --- | --- | --- | --- |
| `name` | texto | none | Category nome para renderizar |
| `rows` | positive inteiro | `3` | número of exibir columns em O MediaWiki-style layout |

````md
<Category name="machines" rows="3" />
````

Se O category é ausente, GuideNH renders Um inline erro.

O mesmo category também tem Um auto-gerado oculto searchable página at `Category:machines`.

## `<Special>`

`<Special>` renders um of O built-em MediaWiki-style special página listings.

### atributos

| Atributo | Tipo | Padrão | Significado |
| --- | --- | --- | --- |
| `name` | texto | none | Compatível valores: `AllPages`, `Categories` |
| `rows` | positive inteiro | `3` | número of exibir columns em O MediaWiki-style layout |

````md
<Special name="AllPages" rows="4" />
<Special name="Categories" rows="3" />
````

O mesmo conteúdo é também disponível por O oculto searchable pages `Special:AllPages` e `Special:Categories`.

## Títulos dos resultados de pesquisa

pesquisa titles são derived em Este ordem:

1. `navigation.title`
2. primeiro level-1 heading (`# Heading`)
3. bruto ID da página

## Páginas relacionadas

- [Guide Page Format](Guide-Page-Format)
- [Search](Search)
- [Tags Reference](Tags-Reference)
