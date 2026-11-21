# Exemples

La référence détaillée de cette page est disponible en anglais. Les noms des balises, attributs, identifiants et chemins s’utilisent sans traduction.

GuideNH ships a runtime example guide in `wiki/resourcepack/`. This page maps the important example files to the feature areas they demonstrate.

## Core Example Pages

These files are in `wiki/resourcepack/assets/guidenh/guidenh/_en_us/`.

| File | Purpose |
| --- | --- |
| `index.md` | Category overview |
| `markdown.md` | Markdown syntax |
| `markdown-table.md` | Tables and merged cells |
| `rendering.md` | Block layout |
| `images.md` | Images and cropped textures |
| `tooltips.md` | Hover content |
| `recipes.md` | Item/fluid recipes and filters |
| `charts.md` | Charts, reference lines and fluid icons |
| `function-graph.md` | Function plots |
| `content-embed.md` | Embedding and text wrapping |
| `ponder.md` | Animation timelines |
| `scene-blocks.md` | Blocks and material lists |
| `scene-import.md` | Structure imports and block edits |
| `scene-entities.md` | Entity previews |
| `scene-camera.md` | Camera controls |
| `scene-annotations.md` | Scene annotations |
| `scene-particles.md` | Particles and weather |
| `navigation-guide.md` | Navigation, bindings and icons |
| `page-lang-key.md` | Whole-page translations |
| `templates-demo.md` | Templates and include control |
| `debug.md` | Layout and interaction diagnostics |
| `japanese.md` | Language and navigation example |

Template definitions are in `templates/`. `wiki/resourcepack/assets/gregtech/guidenh/` demonstrates another namespace and cross-guide links.

## Asset Examples

| Runtime file | Purpose |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | page-local image example |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | rooted shared structure asset for `<ImportStructure>` and `<RemoveBlocks>` |

## Example Snippets

### Frontmatter + Navigation

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

### Relative Image

````md
![Test Image](test1.png)
````

### Namespaced Guide Links

````md
[Same namespace](guide.md)
[Same namespace from root](/guide.md)
[Other namespace](gregtech:guide.md)
[Other namespace from root](gregtech:/guide.md)
````

### Rooted Structure Asset

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib Scene Import

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

The child tags set StructureLib defaults for the scene and are restored when the scene view is reset.

### Imported Structure Cleanup

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### Scene Annotation Tooltip

````md
<GameScene>
  <DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
    ### Activated Beacon
    <RecipeFor id="minecraft:furnace" />
  </DiamondAnnotation>
</GameScene>
````

### Recipe Filter

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### Amount formatting

Use `<Amount>` for quantities. The unit is optional and defaults to a suffix-free number; `fluid` follows the GTNHLib `mB`/`L` preference.

````md
<Amount value="250" />
<Amount value="250" unit="fluid" />/s
<Amount value="1200000" format="compact" /> items
````

## When To Use Which Example

- start with `markdown.md` for Markdown syntax
- use `index.md` to browse examples by category
- use `scene-blocks.md` when you only need static block layout previews
- use `example_structure.snbt` when you need a reusable imported structure asset
- use `templates-demo.md` with the `templates/` folder for reusable content, conditionals and include control. See [Templates](Templates)

## Recommended Learning Order

1. [Getting Started](Getting-Started)
2. [Guide Page Format](Guide-Page-Format)
3. [Tags Reference](Tags-Reference)
4. [GameScene](GameScene)
5. [Annotations](Annotations)
6. [Recipes](Recipes)
