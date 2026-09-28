# Beispiele


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

GuideNH bereits ships Eine Laufzeit Beispiel Leitfaden in `wiki/resourcepack/`. dies Seite maps Die important Beispiel files zu Die feature areas Sie demonstrate.

## Core Beispiel Pages

| Laufzeit Datei | Gezeigte Funktionen |
| --- | --- |
| `.../_en_us/index.md` | frontmatter, Element ids, recipes, Element/Block Bilder, command Links, tooltips, Szenen, Annotationen, `ImportStructureLib`, `RemoveBlocks`, `BlockAnnotationTemplate`, home-Seite recommendation Beispiel |
| `.../_en_us/markdown.md` | einfache Markdown-Funktionen, Tabellen, und ein `recommend: 0` Beispiel |
| `.../_en_us/rendering.md` | Block-level Darstellung und Layout behavior |
| `.../_en_us/scene-blocks.md` | static und interactive Block Szene Beispiele |
| `.../_en_us/japanese.md` | navigation untergeordnetes Element Beispiel |
| `.../_en_us/navigation-guide.md` | navigation, linking, und high-priority recommendation Beispiel |
| `wiki/resourcepack/assets/gregtech/guidenh/_en_us/index.md` | second namespace Beispiel für cross-Leitfaden Links und isolated gleich-Name pages |

## Ressource Beispiele

| Laufzeit Datei | Zweck |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | Seite-lokal Bild Beispiel |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | rooted shared structure Ressource für `<ImportStructure>` und `<RemoveBlocks>` |

## Beispiel Snippets

### Frontmatter und Navigation

```yaml
item_ids:
  - minecraft:book
navigation:
  title: Root
  icon_texture: test1.png
  recommend: 3
```

### Startseitenempfehlung

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

### relativ Bild

````md
![Test Image](test1.png)
````

### Leitfaden-Links mit Namespace

````md
[Gleicher Namespace](guide.md)
[Gleicher Namespace ab der Wurzel](/guide.md)
[Anderer Namespace](gregtech:guide.md)
[Anderer Namespace ab der Wurzel](gregtech:/guide.md)
````

### Strukturobjekt mit Wurzel-Pfad

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib Szene Import

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

Die untergeordnetes Element Tags Setzen Sie StructureLib Standards für Die Szene und sind restored wenn Die Szene view ist reset.

### Bereinigung importierter Strukturen

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### Szene Annotation Tooltip

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### Rezeptfilter

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## wenn zu verwenden Which Beispiel

- Start mit `markdown.md` Wenn you sind validating Parser basics
- verwenden `index.md` wenn testing mixed Laufzeit features together, including StructureLib tooltip, hatch-hervorheben, und cleanup behavior
- verwenden `scene-blocks.md` wenn you nur need static Block Layout previews
- verwenden `example_structure.snbt` wenn you need Eine reusable imported structure Asset

## Empfohlene Lernreihenfolge

1. [Erste Schritte](Guide-Page-Format)
2. [Format der Guide-Seiten](Guide-Page-Format)
3. [Tag-Referenz](Tags-Reference)
4. [GameScene](GameScene)
5. [Annotations](Annotations)
6. [Recipes](Recipes)
