# Exemplos

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


GuideNH já ships Uma execução Exemplo guia em `wiki/resourcepack/`. Este página maps O important Exemplo files para O feature areas Elas demonstrate.

## Core Exemplo Pages

| execução arquivo | What it demonstrates |
| --- | --- |
| `.../_en_us/index.md` | frontmatter, item ids, Receitas, item/bloco imagens, command links, tooltips, cenas, Anotações, `ImportStructureLib`, `RemoveBlocks`, `BlockAnnotationTemplate`, home-página recommendation Exemplo |
| `.../_en_us/markdown.md` | simples Markdown features, tabelas, e a `recommend: 0` Exemplo |
| `.../_en_us/rendering.md` | bloco-level renderização e layout behavior |
| `.../_en_us/scene-blocks.md` | static e interativo bloco cena Exemplos |
| `.../_en_us/japanese.md` | Navegação filho Exemplo |
| `.../_en_us/navigation-guide.md` | Navegação, linking, e high-priority recommendation Exemplo |
| `wiki/resourcepack/assets/gregtech/guidenh/_en_us/index.md` | second namespace Exemplo para cross-guia links e isolated mesmo-nome pages |

## recurso Exemplos

| execução arquivo | Purpose |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | página-local imagem Exemplo |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | rooted shared structure recurso para `<ImportStructure>` e `<RemoveBlocks>` |

## Exemplo Snippets

### frontmatter + Navegação

```yaml
item_ids:
  - minecraft:book
navigation:
  title: Root
  icon_texture: test1.png
  recommend: 3
```

### Home Recommendation

```yaml
navigation:
  title: Markdown Basics
  parent: index.md
  recommend: 0
```

```yaml
navigation:
  title: Navigation & Index
  parent: index.md
  recommend: 5
```

### relativo imagem

````md
![Test Image](test1.png)
````

### Namespaced guia links

````md
[Mesmo namespace](guide.md)
[Mesmo namespace a partir da raiz](/guide.md)
[Outro namespace](gregtech:guide.md)
[Outro namespace a partir da raiz](gregtech:/guide.md)
````

### Rooted Structure recurso

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib cena Import

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:1000">
    <Tier value="4" />
    <Channel name="hatch" value="1" />
    <Facing value="north" />
    <Rotation value="normal" />
    <Flip value="none" />
    <GregTechActiveController />
    <GregTechPlaceHatches />
  </ImportStructureLib>
</GameScene>
````

O filho tags Defina StructureLib defaults para O cena e são restored quando O cena view é reset.

### Imported Structure Cleanup

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### cena anotação dica

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### Recipe filtro

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## quando para usar que Exemplo

- emício com `markdown.md` Se you são validating analisador basics
- usar `index.md` quando testing mixed execução features together, including StructureLib dica, hatch-destacar, e cleanup behavior
- usar `scene-blocks.md` quando you somente need static bloco layout previews
- usar `example_structure.snbt` quando you need Uma reusable imported structure recurso

## Recommended Learning ordem

1. [Primeiros passos](Guide-Page-Format)
2. [Formato das páginas de guia](Guide-Page-Format)
3. [Referência de tags](Tags-Reference)
4. [GameScene](GameScene)
5. [Annotations](Annotations)
6. [Recipes](Recipes)
