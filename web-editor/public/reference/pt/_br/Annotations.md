# Anotações

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


GuideNH cena Anotações são filho tags dentro de `<GameScene>` / `<Scene>`. Elas renderizar em mundo espaço e pode conter filho Markdown/tag conteúdo que torna-se Uma rico dica.

## Regras gerais

- Anotações somente funcionam dentro de Uma cena
- filho conteúdo torna-se O dica corpo
- Anotações pode ser oculto com O cena UI toggle
- `alwaysOnTop` desenha acima de cena geometria quando Compatível by O anotação Tipo
- todos cena Anotações também aceitam opcional `showWhenStructure`, `showWhenTier`, e `showWhenChannels` condições quando O cena usa `<ImportStructureLib>`

## tags de anotação compatíveis

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<TextAnnotation>`

GuideNH também suporta `<BlockAnnotationTemplate>`, que aplica seu filho Anotações para cada já-placed correspondente bloco em O atual cena.

## Condições do StructureLib

quando Uma cena contém `<ImportStructureLib>`, cada anotação tag pode restrict seu visibilidade para Uma específico
StructureLib estado:

| Atributo | Significado |
| --- | --- |
| `showWhenStructure` | bind O anotação para Uma named `<ImportStructureLib name="...">`; omit it quando O cena somente imports um StructureLib structure |
| `showWhenTier` | tier filtro como `2`, `1..3`, `!2`, ou `1..5,!3` |
| `showWhenChannels` | per-channel filtro como `input:1..3, casing:!2, fluid:4` |

Regras:

- `showWhenTier` e `showWhenChannels` são combined com logical e
- `showWhenChannels` pode mention vários channels em um Atributo
- negated-somente clauses like `!2` mean "qualquer valor except 2"
- O mesmo atributos são também Compatível by `<PlaySound>` e `<BlockAnnotationTemplate>` filho Anotações

Exemplo:

````md
<GameScene interactive={true}>
  <ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />

  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="2..4,!3"
    showWhenChannels="input:1..3, casing:!2"
  >
    Visível apenas para o estado StructureLib selecionado.
  </BlockAnnotation>
</GameScene>
````

## `<BlockAnnotation>`

Destaca um volume de bloco 1x1x1 único.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `pos` | sim | `x y z` vetor |
| `color` | não | `#RRGGBB`, `#AARRGGBB`, ou `transparent` |
| `thickness` | não | espessura da linha decimal |
| `alwaysOnTop` | não | booleano expressão |

Exemplo:

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Destaca o bloco controlador.
</BlockAnnotation>
````

## `<BoxAnnotation>`

Destaca uma caixa arbitrária alinhada aos eixos.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `min` | sim | `x y z` minimum vetor |
| `max` | sim | `x y z` maximum vetor |
| `color` | não | anotação cor |
| `thickness` | não | espessura da linha decimal |
| `alwaysOnTop` | não | booleano expressão |

GuideNH automaticamente swaps min/max coordenadas per axis quando Elas são fornecido em reverse ordem.

Exemplo:

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Destaque de área com meia altura.
</BoxAnnotation>
````

## `<LineAnnotation>`

desenha Uma segmento de linha ou polilinha em mundo espaço.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `from` | sim, a menos que `points` é Defina | `x y z` emício vetor |
| `to` | sim, a menos que `points` é Defina | `x y z` fim vetor |
| `points` | não | Semicolon-separated `x y z` pontos para Uma polilinha; substitui `from` / `to` |
| `color` | não | anotação cor |
| `thickness` | não | espessura da linha decimal |
| `alwaysOnTop` | não | booleano expressão |
| `arrow` | não | `start` ou `end`; omitted significa não seta |
| `showPoints` | não | booleano expressão; mostra cada ponto as Uma small cube |
| `pointColor` | não | Padrão cube cor; omitted usa O linha cor |
| `pointSize` | não | Padrão cube tamanho; omitted usa Uma valor slightly larger que `thickness` |

`LineAnnotation` pode conter `<LinePoint>` filhos para substituir ponto marker styling. `LinePoint`
usa `index`, opcional `show`, opcional `color`, e opcional `size`. pontos são zero-indexed.
Arrows pode somente ser placed on O emício ou fim of O linha; intermediate polilinha pontos não pode carry
arrows.

Exemplo:

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Caminho do sinal.
</LineAnnotation>
````

polilinha com Uma 3D endpoint seta e selecionado ponto markers:

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  Caminho do sinal por uma curva.
</LineAnnotation>
````

## `<DiamondAnnotation>`

Places Uma tela-facing diamond marker at Uma mundo posição.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `pos` | sim | `x y z` marker posição |
| `color` | não | tint cor; omitted defaults para bright green |

Exemplo:

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<TextAnnotation>`

desenha Uma speech-bubble texto rótulo sobre O cena. It pode qualquer um follow Uma espaço do mundo anchor ponto ou
stay fixo relativo para O cena centro. Unlike O outro anotação tags, seu filho conteúdo é O
bubble texto itself rather que Uma passar o mouse dica.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `pos` | não | `x y z` espaço do mundo anchor vetor |
| `x`, `y`, `z` | não | Alternative espaço do mundo anchor components quando `pos` é omitted |
| `text` | não | Bubble texto; filho Markdown é usado quando omitted |
| `textKey` | não | chave de tradução resolvido de recurso-pack `lang` files antes de falling back para `text` ou filho Markdown |
| `color` | não | Bubble borda cor; defaults para light grey |
| `backgroundAlpha` | não | fundo opacity de `0` para `255`; defaults para `204` |
| `maxWidth` | não | Wrap largura em pixels; `0` keeps a único linha |
| `independent` | não | `true` keeps O bubble fixo em tela espaço |
| `yOffset` | não | Pixel offset de O cena centro quando `independent={true}` |
| `connectorSide` | não | `bottom`, `top`, `left`, `right`, ou `none`; defaults para `bottom` |
| `connectorOffset` | não | Pixel offset along O bubble edge; positive moves direita para topo/baixo e down para esquerda/direita |
| `connectorLength` | não | Pixel length of O connector linha; defaults para `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | não | opcional companion destacar box bounds |
| `highlightColor` | não | opcional destacar box cor |

mundo-anchored bubbles desenhar Uma connector linha para seus anchor. usar `connectorSide` para escolher que
edge of O bubble pontos at O anchor, `connectorOffset` para move O attachment ponto along que
edge, e `connectorLength` para control O gap entre O bubble e anchor. Independent bubbles
são centered horizontally em O cena e usar `yOffset` para vertical placement. Elas do não desenhar a
connector. O mesmo execução anotação é também usado quando importing Animação Ponder `text` Anotações.

Exemplo:

````md
<TextAnnotation
  pos="1.5 2 1.5"
  textKey="guidenh.sample.scene.insert_items"
  color="#FF44AAFF"
  maxWidth={120}
  backgroundAlpha={180}
  connectorSide="right"
  connectorOffset={8}
  connectorLength={12}
>
  Insira itens aqui com **prioridade**.
</TextAnnotation>
````

fixo espaço da tela Exemplo:

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  Independent status text
</TextAnnotation>
````

## rico dica conteúdo

anotação filhos são compilado as normal GuideNH conteúdo, so tooltips pode conter:

- Markdown paragraphs e headings
- item/bloco imagens
- Receitas
- nested non-interativo cenas

Exemplo:

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

usar it quando you want para stamp O mesmo anotação onto cada correspondente bloco.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `id` | sim | bloco matcher em `modid:block[:meta]` form |

Regras:

- O template somente sees blocos que já exist quando it é analisado
- colocar it depois de `<Block>`, `<ImportStructure>`, ou `<ImportStructureLib>` tags que deve feed it
- filho Anotações usar local coordenadas relativo para cada correspondente bloco

Exemplo:

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Tooltip gerado pelo modelo
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## Páginas relacionadas

- [GameScene](GameScene)
- [Examples](Examples)
