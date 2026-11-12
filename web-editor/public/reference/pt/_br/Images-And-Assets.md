# Imagens e recursos

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


GuideNH suporta ambos normal Markdown imagens e several execução-específico visual elements.

## recurso Resolution Regras

guia assets resolver com O mesmo Regras usado by página links.

| caminho form | Exemplo | Significado |
| --- | --- | --- |
| relativo | `test1.png` | relativo para O atual página arquivo |
| rooted | `/assets/example_structure.snbt` | relativo para O atual guia raiz |
| explícito recurso id | `guidenh:textures/gui/example.png` | absoluto `modid:path` lookup |

## Markdown imagens

normal Markdown imagens são Compatível:

````md
![Example](test1.png)
````

GuideNH resolves O caminho e carrega O binary recurso de O guia conteúdo raiz.

## `FloatingImage`

Com `wrap="inline"`, a imagem fica centralizada verticalmente em relação ao texto ao redor, e a linha aumenta de altura para acomodar a imagem inteira. As linhas de texto adjacentes mantêm a altura normal; não há um limite máximo fixo para a altura da linha. Para que uma imagem muito alta ocupe uma linha própria, coloque a tag em um parágrafo separado, com uma linha em branco antes e depois.

`<FloatingImage>` renders Uma cropped bitmap region que pode decimal com texto ou sit truly inline dentro de a
paragraph. It também accepts explícito `modid:path` texture ids, so it pode reference texture assets de
outro mods directly.

### atributos

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `src` | sim | imagem caminho |
| `x` | ao recortar | crop emício X em origem-imagem pixels |
| `y` | ao recortar | crop emício Y em origem-imagem pixels |
| `width` / `w` | ao recortar | crop largura em origem-imagem pixels; exatamente um forma deve ser usado |
| `height` / `h` | ao recortar | crop altura em origem-imagem pixels; exatamente um forma deve ser usado |
| `scaleX` | não | horizontal exibir multiplier, Padrão `1.0` |
| `scaleY` | não | vertical exibir multiplier, Padrão `1.0` |
| `displayWidth` | não | final exibir largura em pixels; preserves O crop aspect ratio quando usado alone |
| `displayHeight` | não | final exibir altura em pixels; preserves O crop aspect ratio quando usado alone |
| `wrap` | não | `inline` coloca a imagem na linha; caso contrário, `align` escolhe o lado flutuante |
| `align` | não | `left` ou `right` para floating placement; ignored quando `wrap="inline"` |
| `title` | não | dica/título texto |
| `alt` | não | Texto alternativo |
| `sound` | não | sound event played by O whole imagem |
| `soundSrc` | não | sound caminho do arquivo para O whole imagem |
| `trigger` | não | `click` by Padrão, ou `hover` para passar o mouse playback |

### Observações

- `x`, `y`, `width` / `w`, e `height` / `h` são todos Obrigatório together quando cropping
- quando O crop atributos são todos omitted, `displayWidth` ou `displayHeight` exibe O completo origem imagem
- `width` e `height` indicam o recorte da origem. Use `displayWidth` / `displayHeight` ou fatores de escala para o tamanho exibido.
- `scaleX` e `scaleY` calcular O final exibir tamanho as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` ou `displayHeight` sets O final exibir tamanho em pixels; quando somente um é present, O outro dimension é calculado de O crop aspect ratio
- providing ambos `displayWidth` e `displayHeight` permite intentional non-proportional stretching
- `displayWidth` / `displayHeight` não pode ser combined com `scaleX` / `scaleY`
- único-axis stretching é Compatível by setting somente um scale differently
- `width` com `w`, ou `height` com `h`, é inválido e renders Uma visível erro
- `src` pode ser relativo, rooted, ou Um explícito `modid:path` texture id como `minecraft:textures/gui/options_background.png`

### Exemplo

````md
<FloatingImage
  src="minecraft:textures/gui/options_background.png"
  x="0"
  y="0"
  width="32"
  height="32"
  displayWidth="64"
  displayHeight="64"
  wrap="inline"
  title="Example"
/>
````

## `ImageAnnotation`

`<ImageAnnotation>` é Uma filho element of `<FloatingImage>` que attaches Uma rico-texto dica (e
Um opcional colored borda) para Uma rectangular region of O imagem. coordenadas são specified em
**cropped-imagem pixels** e são automaticamente proportionally scaled quando O cropped imagem é resized
ou stretched.

### atributos

| Atributo | Obrigatório | Padrão | Significado |
| --- | --- | --- | --- |
| `x` | não | — | esquerda edge of O region em imagem pixels |
| `y` | não | — | topo edge of O region em imagem pixels |
| `w` | não | — | largura of O region em imagem pixels |
| `h` | não | — | altura of O region em imagem pixels |
| `border` | não | `false` | mostrar Uma colored borda around O region |
| `borderColor` | não | random | borda cor (`#RRGGBB` ou `#AARRGGBB`) |
| `borderThickness` | não | `1` | borda thickness em exibir pixels |
| `sound` | não | none | opcional sound event played para Este region |
| `src` | não | none | opcional sound caminho do arquivo; converted para Uma sound event id |
| `trigger` | não | `click` | `click` ou `hover` |

### Observações

- omitting todos four of `x`, `y`, `w`, `h` makes O anotação cover O **whole imagem**
- Se qualquer of O four é present, O remaining omitted ones Padrão para `0` (origin) ou `1` (tamanho)
- borda é **não exibido by Padrão**; Adicione `border` ou `border={true}` para enable it
- quando `borderColor` é omitted e `border` é ativado, Uma random fully-opaque cor é usado
- filho MDX conteúdo é renderizado as O dica corpo e pode incluir qualquer inline/bloco elements
- later Anotações (lower em O lista) take passar o mouse priority sobre earlier ones quando regions overlap

### Exemplo

Whole-imagem anotação:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation>
    Passe o cursor sobre a imagem para ver esta dica.
  </ImageAnnotation>
</FloatingImage>
````

Region anotação com Uma visível borda:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="2" y="2" w="15" h="10" border borderColor="#FFFF4444" borderThickness="2">
    Esta é a dica da **região destacada**.
  </ImageAnnotation>
</FloatingImage>
````

vários regions on um imagem:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="0" y="0" w="16" h="32" border borderColor="#FF44FF44">
    Left half
  </ImageAnnotation>
  <ImageAnnotation x="16" y="0" w="16" h="32" border borderColor="#FF4444FF">
    Right half
  </ImageAnnotation>
</FloatingImage>
````

imagem regions pode também play sounds. usar `<SoundArea>` quando you somente need sound, ou put `sound`
directly on `<ImageAnnotation>` quando O mesmo region também tem Uma dica ou borda.

````md
<FloatingImage
  displayWidth="128" displayHeight="128" src="test1.png"
  align="left"
  x="0"
  y="0"
  width="32"
  height="32"
  sound="guidenh:guide.sample_click"
>
  <SoundArea x="0" y="0" w="16" h="32" sound="guidenh:guide.sample_left" />
  <SoundArea x="16" y="0" w="16" h="32" sound="guidenh:guide.sample_hover" trigger="hover" />
  <ImageAnnotation x="2" y="2" w="10" h="10" border sound="guidenh:guide.sample_click">
    Esta região contém o texto da dica e um som de clique.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers O whole imagem. Region sounds usar cropped-imagem coordenadas
e obey O mesmo overlap priority as tooltips: later regions win.

## Incorporação de conteúdo e quebra de texto

Tags baseadas em `BlockTagCompiler`, como `<Recipe>`, `<GameScene>` e `<BlockImage>`, aceitam `wrap` e `align`.

`<FloatingImage>` tem regras próprias: `wrap="inline"` coloca a imagem na linha; caso contrário, `align="left"` ou `"right"` escolhe o lado flutuante. `<ItemImage>` é um ícone em linha; use `<Column>` ou `<Row>` para alinhá-lo.

| Atributo | Padrão | Significado |
| --- | --- | --- |
| `wrap` | `inline` | `inline`: posição normal. `square`, `tight`, `through`: flutuar à esquerda ou à direita. `top-bottom`, `behind`, `front`: atualmente posição normal alinhada, sem camadas separadas de texto. |
| `align` | `left` | `left`, `center` ou `right` no contexto de bloco. Ao flutuar, `right` escolhe a direita e os demais valores a esquerda. Para centralizar: `wrap="inline" align="center"`. |
| `float` | — | `left` / `right` legado no texto quando faltam `wrap` e `align`. Prefira esses atributos explícitos. |

```mdx
<BlockImage id="minecraft:stone" wrap="square" align="right" />

<BlockImage id="minecraft:diamond_block" wrap="inline" align="center" />

<Column align="center">
  <ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
</Column>
```

## Navegação Texture Icons

frontmatter pode usar `icon_texture` para mostrar Uma texture em vez disso of Um item em Navegação/pesquisa:

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

O arquivo deve decode as Um imagem. O caminho é resolvido like qualquer outro guia recurso caminho.

## Non-imagem Assets

GuideNH pages pode também reference non-imagem execução assets, especially structure files, para Exemplo:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

Estes assets são carregado por O mesmo guia recurso pipeline but são consumed by personalizado tags rather que renderizado directly as imagens.

## Best Practices

- Mantenha página-local imagens perto O página que usa eles
- Mantenha reusable files under O guia raiz `assets/` pasta
- prefer rooted `/assets/...` paths para shared files referenced by vários pages
- usar texture icons somente para real imagem assets

## `BlockImage`

`<BlockImage>` mostra um bloco colocado como prévia 3D transparente e aceita os atributos de alinhamento e fluxo de texto acima.

chave behavior:

- transparent fundo e borda
- não cena buttons, não layer slider, não anotação authoring surface
- passar o mouse ainda mostra O selecionado bloco outline e dica
- `scale` alterações câmera zoom e defaults para `4`
- `perspective` accepts `isometric-north-east`, `isometric-north-west`, e `up`
- `nbt` supplies tile-entity SNBT; inline `id="mod:block:meta:{...}"` SNBT ainda works, but O
  standalone `nbt` Atributo é easier para ler e é preferido

Exemplo:

````md
<BlockImage id="minecraft:stone" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## execução Exemplo Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## Páginas relacionadas

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
