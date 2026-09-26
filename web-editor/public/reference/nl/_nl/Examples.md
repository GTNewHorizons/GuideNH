# Voorbeelden

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


GuideNH al ships Een runtime example gids in `wiki/resourcepack/`. Deze pagina maps De important example files naar De feature areas they demonstrate.

## Core Example Pages

| runtime bestand | What it demonstrates |
| --- | --- |
| `.../_en_us/index.md` | frontmatter, item ids, Recepten, item/blok afbeeldingen, command links, tooltips, scènes, Annotaties, `ImportStructureLib`, `RemoveBlocks`, `BlockAnnotationTemplate`, home-pagina recommendation example |
| `.../_en_us/markdown.md` | plain Markdown features, tabellen, en a `recommend: 0` example |
| `.../_en_us/rendering.md` | blok-level rendering en layout behavior |
| `.../_en_us/scene-blocks.md` | static en interactief blok scène Voorbeelden |
| `.../_en_us/japanese.md` | Navigatie kind example |
| `.../_en_us/navigation-guide.md` | Navigatie, linking, en high-priority recommendation example |
| `wiki/resourcepack/assets/gregtech/guidenh/_en_us/index.md` | second namespace example voor cross-gids links en isolated zelfde-naam pages |

## asset Voorbeelden

| runtime bestand | Purpose |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | pagina-lokale afbeelding example |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | rooted shared structure asset voor `<ImportStructure>` en `<RemoveBlocks>` |

## Example Snippets

### frontmatter + Navigatie

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

### Relative afbeelding

````md
![Test Image](test1.png)
````

### Namespaced gids Links

````md
[Dezelfde naamruimte](guide.md)
[Dezelfde naamruimte vanaf de hoofdmap](/guide.md)
[Andere naamruimte](gregtech:guide.md)
[Andere naamruimte vanaf de hoofdmap](gregtech:/guide.md)
````

### Rooted Structure asset

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib scène Import

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

De kind tags Stel in StructureLib defaults voor De scène en are restored wanneer De scène view is reset.

### Imported Structure Cleanup

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### scène annotatie tooltip

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### Recipe filter

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## wanneer naar gebruiken Which Example

- begin met `markdown.md` Als you are validating parser basics
- gebruiken `index.md` wanneer testing mixed runtime features together, including StructureLib tooltip, hatch-markeren, en cleanup behavior
- gebruiken `scene-blocks.md` wanneer you alleen need static blok layout previews
- gebruiken `example_structure.snbt` wanneer you need Een reusable imported structure asset

## Recommended Learning Order

1. [Aan de slag](Guide-Page-Format)
2. [Guide-paginavorm](Guide-Page-Format)
3. [Tagreferentie](Tags-Reference)
4. [GameScene](GameScene)
5. [Annotations](Annotations)
6. [Recipes](Recipes)
