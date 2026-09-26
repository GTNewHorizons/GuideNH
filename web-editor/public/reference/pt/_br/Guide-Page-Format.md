# Formato da página do guia

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


GuideNH execução pages são Markdown files analisado com:

- padrão Markdown bloco e inline sintaxe
- YAML frontmatter
- GFM tabelas
- strikethrough
- mark destacar com `==text==`
- GuideNH inline underline extensions: `++text++` (straight underline), `^^text^^` (wavy underline), `::text::` (emphasis dots / dotted underline)
- MDX comments using `{/* ... */}`
- MDX-style personalizado tags

## Markdown compatível

GuideNH pages suporte O comum Markdown features usado em O Exemplo guia:

- headings
- paragraphs
- inline emphasis, bold, strike, e code
- inline mark destacar (`==text==`)
- inline underline (`++text++`), wavy underline (`^^text^^`), e emphasis dots (`::text::`)
- links e imagens
- literal autolinks para direto URLs, `www.` hosts, e email addresses
- reference links e reference imagens
- unordered e ordered lists
- GFM task lists
- blockquotes
- GitHub-style alert blockquotes como `[!NOTE]`
- horizontal Regras
- fenced blocos de código
- indented blocos de código
- GFM tabelas
- footnotes
- lowercase HTML fragments como `<a>`, `<br>`, `<kbd>`, `<sub>`, `<sup>`, e `<details>`
- MDX comments em página texto

Consulte `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` para Uma live sample página.

## destacar

usar `==text==` para inline highlighted texto. usar `<mark color="#8A6A00">text</mark>` quando Uma personalizado destacar cor é needed. O Padrão mark fundo é Uma dark golden yellow escolhido para Mantenha white texto readable.

## blocos de código

execução blocos de código atualmente suporte:

- explícito fence languages como `java`, `lua`, `scala`, `csv`, e `mermaid`
- automático idioma inference quando O fence idioma é omitted
- Uma idioma rótulo exibido acima de O bloco
- Uma topo-direita copy button em O em-game viewer
- lightweight execução sintaxe highlighting para O detected idioma

Exemplo:

````md
```lua
local valor = 42
print(valor)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

Indented blocos de código são também Compatível:

````md
    print("indented code")
````

quando Uma fenced bloco resolves para `mermaid` e O origem é Uma Compatível `mindmap`, GuideNH renders it as Um interativo execução mindmap em vez disso of Uma simples bloco de código.

quando Uma fenced bloco é explicitly marked as `csv`, GuideNH renders it as Uma execução tabela em vez disso of Uma simples bloco de código. Se O fence idioma é omitted, CSV-shaped texto ainda stays Uma bloco de código e somente usa CSV idioma detection para labeling/highlighting.

explícito CSV tabelas pode também fornecer coluna largura hints:

````md
```csv widths=120,80
nome,valor
iron,42
gold,17
```
````

Fence metadata também suporta `header=false` e quoted largura lists:

````md
```csv widths="120,80" header=false
nome,valor
iron,42
gold,17
```
````

direto GFM-style literal autolinks são também Compatível em normal paragraph texto:

````md
Visit https://example.com/docs, www.example.org, or guide@example.com
````

## Mapas mentais Mermaid

GuideNH execução Mermaid suporte é atualmente focused on `mindmap` diagrams:

- fenced ```` ```mermaid ```` blocos
- auto-detected mermaid code fences whose conteúdo starts com `mindmap`
- explícito `<Mermaid>...</Mermaid>` tags
- explícito `<Mermaid src="./diagram.mmd" />` imports
- rico inline Markdown labels dentro de Mermaid node texto
- opcional `<NodeContent id="...">...</NodeContent>` filhos para arbitrary execução blocos dentro de correspondente nodes
- whole-diagram arrastar-para-pan interaction em O em-game viewer
- `layout: tidy-tree` frontmatter dentro de Mermaid origem
- comum mindmap node shapes como square, rounded, circle, bang, cloud, e hexagon
- analisado `::icon(...)` e `:::class` metadata

Exemplo:

````md
```mermaid
mindmap
  raiz((GuideNH))
    execução
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      arrastar para pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]
    export["Site export"]

<NodeContent id="runtime">
Os nós de execução podem combinar texto, links e blocos.

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_en_us/test1.png)
</NodeContent>
</Mermaid>
````

Mermaid diagrams que são não Compatível at execução yet ainda fall back para regular Mermaid-labeled blocos de código.

## Importação de tabela CSV

GuideNH também suporta execução CSV arquivo imports por Um explícito tag:

````md
<CsvTable src="./markdown-table.csv" />
````

O `src` caminho resolves relativo para O atual página, O mesmo way execução recurso links e cena `src` imports do.

Imported CSV tabelas pode também fornecer largura hints:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

You pode também escrever Uma CSV tabela inline com Um explícito fence:

````md
```csv
nome,valor
iron,42
gold,17
```
````

## Dicas de largura para tabelas Markdown

Ordinary GFM Markdown tabelas pode também fornecer execução coluna largura hints by adding Uma trailing execução Atributo linha immediately depois de O tabela:

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````

Este keeps O tabela itself padrão Markdown enquanto letting GuideNH apply execução-somente preferido coluna widths.

## Listas de tarefas, alertas e notas de rodapé

GuideNH execução também suporta several useful GFM-style behaviors:

- task lists using `- [ ]` e `- [x]`
- GitHub alert blockquotes como `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, e `[!CAUTION]`
- footnote references e definitions

Exemplo:

````md
- [x] done item
- [ ] todo item

> [!NOTE]
> alert body

Footnote ref[^one]

[^one]: tooltip text
````

Footnote references renderizar as dica-style inline markers, e GuideNH appends Uma compact execução footnote lista perto O baixo of O página.

## Personalização da largura da lista

padrão Markdown lists do não define largura controls, but GuideNH execução containers pode constrain eles:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

Este é atualmente O recommended way para customize lista linha largura at execução.

## links e imagens de referência

GuideNH suporta CommonMark reference definitions:

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## Lowercase HTML execução tags

GuideNH execução suporta Uma focused subset of lowercase HTML-style tags directly:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">Open subpage</a><br clear="all" />

<details open>
<summary>More</summary>

Body text
</details>
````

outro bruto HTML fragments ainda fall back para literal texto-style handling em vez disso of browser-grade HTML renderização.

## MDX Comments

GuideNH suporta O MDX comment form e ignores it antes de Markdown compilation:

````md
Texto visível. {/* comentário embutido oculto */}

{/*
multiline comment
*/}

More visible text.
````

## frontmatter

GuideNH reads O primeiro YAML frontmatter bloco e parses Estes known chaves:

| chave | Tipo | Significado |
| --- | --- | --- |
| `navigation` | map | Adds O página para O Navegação tree |
| `categories` | lista of strings | Adds O página para MediaWiki-style categories; cada entrada pode optionally usar `category|sort key` |
| `item_id` | item filtro expressão | A único NEI-style item expressão que makes O página discoverable by `<ItemLink>` |
| `item_ids` | lista of item filtro expressions | lista form of `item_id`; qualquer correspondente expressão makes O página discoverable by `<ItemLink>` |
| `ore_ids` | lista of ore dictionary names | Makes O página discoverable by ore-dictionary itens (e.g. `ingotIron`, `oreCopper`) |
| `quest_ids` | lista of BetterQuesting quest ids | Makes O página discoverable by `<QuestLink>` / `<QuestCard>` e by O open-guia hotkey quando Uma quest é hovered em O BQ GUI. Accepts canonical UUID strings e BetterQuesting's compact Base64 form. somente consumed quando BetterQuesting é carregado. Consulte [Mod Compatibility](Mod-Compatibility) |
| `author` | texto | único author nome. Displayed em O baixo bar. |
| `authors` | lista of strings ou `{name: ...}` maps | vários author names. At most dois são displayed; additional ones são substituído com `...`. Takes precedence sobre `author` Se ambos são present. |
| `date` | texto ou YYYY-MM-DD date | conteúdo creation date. Displayed em O baixo bar. |
| `updated` | texto ou YYYY-MM-DD date | último atualizado date. Displayed em O baixo bar. |
| `zoom` | positive decimal | Per-página conteúdo zoom multiplier (e.g. `1.5` = 150 %). Multiplied com O global `contentZoom` setting em ModConfig. Padrão `1.0`. |
| qualquer outro chave | qualquer YAML valor | Preserved em `additionalProperties` para extensions ou tooling |

### `navigation`

| campo | Obrigatório | Tipo | Observações |
| --- | --- | --- | --- |
| `title` | sim | texto | exibir nome em Navegação e pesquisa título reserva |
| `keyword` | não | texto | um additional pesquisa keyword ou alias; suporta prefix correspondente |
| `keywords` | não | lista of strings | Additional Palavras-chave de pesquisa ou aliases; valores são combined e deduplicated |
| `parent` | não | ID da página | pai ID da página; omitted significa topo-level node |
| `position` | não | inteiro | Sibling sort ordem; Padrão `0`, larger valores appear earlier |
| `priority` | não | inteiro | Prioridade de carregamento para mesmo-caminho página substitui; Padrão `0`, higher wins, equal priority lets O later pacote de recursos entrada win |
| `icon` | não | item id | item icon exibido em Navegação/pesquisa. Accepts `modid:name`, `modid:name:meta`, ou `modid:name:meta:{snbt}`. O inline SNBT tail é O preferido way para attach NBT como Uma personalizado exibir nome. |
| `icons` | não | lista of item ids | lista of item icons para animated cycling (um per second). cada entrada usa O mesmo sintaxe as `icon`, including inline `:{snbt}` tails. quando present takes priority sobre `icon`. |
| `icon_texture` | não | recurso caminho | Texture icon caminho resolvido like qualquer outro recurso link |
| `icon_textures` | não | lista of recurso paths | lista of texture icons para animated cycling (um per second). quando present takes priority sobre `icon_texture`. |
| `required_mod` | não | mod id | oculta O página a menos que Este mod é carregado. |
| `required_mods` | não | lista of mod ids | oculta O página a menos que cada listed mod é carregado. |
| `excluded_mod` | não | mod id | oculta O página quando Este mod é carregado. |
| `excluded_mods` | não | lista of mod ids | oculta O página quando qualquer listed mod é carregado. |

### Exemplo frontmatter

```yaml
item_id: minecraft:potion 16384-16462,!16386
item_ids:
  - ae2:white_paint_ball:*
  - "<minecraft:wool:14>"
navigation:
  title: Root
  parent: index.md
  position: 10
  priority: 0
  icon: minecraft:book:0:{display:{Name:"My Custom Book"}}
  # Use meta/damage to select a specific subtype:
  # icon: minecraft:wool:1       (orange wool, colon form)
  # Cycling icons list — cycles one per second:
  # icons:
  #   - minecraft:wool:1
  #   - minecraft:wool:4:{display:{Name:"Custom Green Wool"}}
  #   - minecraft:wool:14

  icon_texture: test1.png
  # Cycling textures:
  # icon_textures:
  #   - test1.png
  #   - test2.png
categories:
  - basics
  - examples|Examples Overview
ore_ids:
  - ingotIron
  - oreCopper
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
author: ExampleAuthor
date: 2024-01-15
updated: 2024-06-01
```

`categories` accepts qualquer um simples category names ou `category|sort key` entradas.
usar `<Category name="examples" rows="3" />` para renderizar Uma category listing bloco, e
`<Special name="SpecialPages" rows="3" />` para embed O gerado MediaWiki-style special-página index.

para BetterQuesting integration, `quest_ids` accepts qualquer um of Estes formats:

- canonical UUID strings como `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting compact quest ids como `AAAAAAAAAAAAAAAAAAAMug==`

Do não lista ambos forms para O mesmo quest em um página's `quest_ids`; Elas normalize para O mesmo interno UUID e would ser treated as duplicates.

quando qualquer of `author`, `authors`, `date`, ou `updated` é present, GuideNH mostra a
baixo bar em O guia tela (correspondente O topo toolbar style) com direita-aligned
texto like: *conteúdo de MyMod, Author ExampleAuthor, Date 2024-01-15, atualizado 2024-06-01*.

vários authors Exemplo:
```yaml
authors:
  - Alice
  - Bob
  - Charlie   # only Alice and Bob are shown, "..." appended
```
ou com structured entradas:
```yaml
authors:
  - name: Alice
  - name: Bob
```

### `zoom`

O `zoom` chave lets you enlarge ou shrink O conteúdo of a único página sem
affecting qualquer outro página. O valor é Uma positive decimal treated as Uma multiplier:

| Exemplo valor | Effect |
| --- | --- |
| `1.0` (Padrão) | normal tamanho |
| `1.5` | 150 % — conteúdo 50 % larger |
| `0.75` | 75 % — conteúdo 25 % smaller |

O per-página zoom é multiplied com O global **contentZoom** slider em
ModConfig → GuideNH → UI. Este lets server packs Defina Uma sensible baseline enquanto
ainda allowing individual pages para fine-tune layout para narrow ou wide conteúdo.

Exemplo: Defina Este página para exibir at 150 % of O base zoom:

```yaml
zoom: 1.5
navigation:
  title: My Dense Page
```

página layout é recomputed at O zoom-adjusted largura, so texto wrapping e todos
bloco geometria remain correct at qualquer zoom level.

## Resolução de links

GuideNH resolves ids e paths using Estes Regras:

### página links

| entrada | Significado |
| --- | --- |
| `subpage.md` | relativo para O atual página, em O atual página namespace |
| `./subpage.md` | relativo para O atual página, em O atual página namespace |
| `/guide.md` | rooted para O atual página namespace, equivalent para `currentmod:guide.md` |
| `gregtech:guide.md` | explícito namespace; opens `gregtech:guidenh` quando O atual guia caminho é `guidenh` |
| `gregtech:/guide.md` | explícito namespace plus rooted caminho, normalized para `gregtech:guide.md` |
| `subpage.md#anchor` | página plus anchor fragment |
| `guidenh:other.md#anchor` | explícito `modid:path#anchor` |
| `https://example.com` | externo HTTP/HTTPS link |

página links são isolated by namespace. Uma link written de `assets/guidenh/guidenh/_en_us/index.md` as
`[Guide](guide.md)` resolves para `guidenh:guide.md`; O mesmo texto em
`assets/gregtech/guidenh/_en_us/index.md` resolves para `gregtech:guide.md`. Se que página é ausente em O
atual namespace, GuideNH reports it as Uma broken link em vez disso of falling back para another mod's página.

explícito `modid:path` links pode cross de um mod's dados-driven guia para another. O guia id é derived de
O destino página namespace e O atual guia caminho, so Uma link de `guidenh:guidenh` para `gregtech:guide.md`
opens página `gregtech:guide.md` em guia `gregtech:guidenh`.

Anchor fragments rolar O guia para Uma heading whose texto lowercased e spaces substituído com hyphens
corresponde O fragment (e.g. `#crafting-recipe` scrolls para `## Crafting Recipe`), ou para a `<a name="...">` anchor.

### recurso links

Assets usar O mesmo resolution Regras as links. para Exemplo:

- `test1.png` resolves relativo para O atual página arquivo.
- `/assets/example_structure.snbt` resolves para O guia's recurso raiz.
- `guidenh:textures/gui/example.png` resolves as Um explícito recurso location.

## Sintaxe de referência de itens

Navegação `icon` e `icons`, along com tags que aceitam Um item id, usar ordinary item references:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Um omitted `meta` defaults para `0`. Um SNBT tail starts at O primeiro `{` e é analisado as item NBT. Where wildcard
metadata é Compatível, `*` pode ser combined com O SNBT tail.

Exemplos:

```text
minecraft:diamond
minecraft:wool:14
minecraft:written_book:0:{title:TestBook,author:GuideNH}
minecraft:written_book:*:{title:TestBook,author:GuideNH}
```

### Expressões de índice de itens

`item_id` accepts um NEI-style expressão e `item_ids` accepts Uma YAML lista of O mesmo expressions. cada
`item_ids` entrada é independent; O página é linked quando qualquer entrada corresponde. Whitespace combines terms, `|`
combines alternatives, e `,` combines Regras dentro de um term.

- `minecraft:lava` performs Uma case-insensitive partial registry-id corresponder, so it também corresponde `minecraft:lava_bucket`.
- `<minecraft:wool:14>` strictly corresponde um item e meta valor.
- `ae2:white_paint_ball:*`, `:32767`, e uppercase meta tokens como `:ANY` são compatible strict todos-meta forms.
- `16384-16462,!16386` corresponde Uma metadata range enquanto excluding `16386`.
- `!minecraft:portal` excludes Uma correspondente registry id.
- `r/^m\\w{6}ft$/` usa Uma Java regular expressão against O registry id.

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:crafting_table
  - appliedenergistics2:item.ItemMultiMaterial:1
  - "minecraft:written_book:*:{title:TestBook,author:GuideNH},!minecraft:written_book:0"
  - "<minecraft:wool:14>"
  - wrench|hammer
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:diamond#Usage
```

em Este Exemplo, whitespace combines condições, `,` combines Regras dentro de um term, `!` excludes Uma corresponder, e
`|` separates alternatives. O primeiro expressão portanto accepts potion metadata de `16384` por `16462`
except `16386`, ou qualquer metadata of `ae2:white_paint_ball`. O second expressão demonstrates Uma wildcard item
reference em Uma comma-separated expressão com Uma reverse (`!`) regra. O último dois entradas mostrar Uma metadata union
com `28` excluded e Um item mapping que opens O `Usage` heading anchor.

Um opcional `#anchor` suffix opens Uma correspondente página at Uma heading anchor. Exact item e explícito-meta mappings usar
O direto index primeiro; expressions são evaluated somente quando needed.

## Tratamento de erros

Se Uma página fails para analisar, GuideNH cria Um erro página em vez disso of crashing O guia. inválido tags, ids, e atributos são reported inline as guia-renderizado erro texto.

## Páginas relacionadas

- [Navigation](Navigation)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
