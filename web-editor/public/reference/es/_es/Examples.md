# Ejemplos


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

GuideNH ya ships Un ejecución Ejemplo guíUn en `wiki/resourcepack/`. esto página maps El important Ejemplo files Un El feature areas Estas demonstrate.

## Core Ejemplo páginas

| ejecución archivo | What it demonstrates |
| --- | --- |
| `.../_en_us/index.md` | frontmatter, elemento ids, recipes, elemento/bloque imágenes, command enlaces, tooltips, escenas, anotaciones, `ImportStructureLib`, `RemoveBlocks`, `BlockAnnotationTemplate`, home-página recommendation Ejemplo |
| `.../_en_us/markdown.md` | simple markdown features, tablas, y a `recommend: 0` Ejemplo |
| `.../_en_us/rendering.md` | bloque-level renderizado y diseño comportamiento |
| `.../_en_us/scene-blocks.md` | static y interactivo bloque escena Ejemplos |
| `.../_en_us/japanese.md` | navigation hijo Ejemplo |
| `.../_en_us/navigation-guide.md` | navigation, linking, y high-priority recommendation Ejemplo |
| `wiki/resourcepack/assets/gregtech/guidenh/_en_us/index.md` | second espacio de nombres Ejemplo para cross-guíUn enlaces y isolated mismo-nombre páginas |

## recurso Ejemplos

| ejecución archivo | Purpose |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | página-local imagen Ejemplo |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | rooted shared estructura recurso para `<ImportStructure>` y `<RemoveBlocks>` |

## Ejemplo Snippets

### frontmatter + Navigation

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

### relativo imagen

````md
![Test Image](test1.png)
````

### Namespaced guíUn enlaces

````md
[Mismo espacio de nombres](guide.md)
[Mismo espacio de nombres desde la raíz](/guide.md)
[Otro espacio de nombres](gregtech:guide.md)
[Otro espacio de nombres desde la raíz](gregtech:/guide.md)
````

### Rooted estructura recurso

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib escena Import

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

El hijo etiquetas Establezca StructureLib predeterminados para El escena y son restored cuando El escena vista es reset.

### importado estructura Cleanup

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### escena anotación Tooltip

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

## cuando Un usar que Ejemplo

- inicio con `markdown.md` Si you son validating analizador basics
- usar `index.md` cuando testing mixed ejecución features together, including StructureLib tooltip, hatch-resaltar, y cleanup comportamiento
- usar `scene-blocks.md` cuando you solo need static bloque diseño previews
- usar `example_structure.snbt` cuando you need Un reusable importado estructura recurso

## Recommended Learning orden

1. [Primeros pasos](Guide-Page-Format)
2. [Formato de páginas de guía](Guide-Page-Format)
3. [Referencia de etiquetas](Tags-Reference)
4. [GameScene](GameScene)
5. [Annotations](Annotations)
6. [Recipes](Recipes)
