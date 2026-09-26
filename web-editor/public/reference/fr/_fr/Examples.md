# Exemples


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

GuideNH déjà ships Un exécution Exemple guide dans `wiki/resourcepack/`. ceci page maps Le important Exemple files vers Le feature areas Elles demonstrate.

## Core Exemple Pages

| exécution fichier | What it demonstrates |
| --- | --- |
| `.../_en_us/index.md` | frontmatter, élément ids, recipes, élément/bloc images, command liens, tooltips, scènes, annotations, `ImportStructureLib`, `RemoveBlocks`, `BlockAnnotationTemplate`, home-page recommendation Exemple |
| `.../_en_us/markdown.md` | simple markdown features, tableaux, et a `recommend: 0` Exemple |
| `.../_en_us/rendering.md` | bloc-level rendu et mise en page behavior |
| `.../_en_us/scene-blocks.md` | static et interactive bloc scène Exemples |
| `.../_en_us/japanese.md` | navigation enfant Exemple |
| `.../_en_us/navigation-guide.md` | navigation, linking, et high-priority recommendation Exemple |
| `wiki/resourcepack/assets/gregtech/guidenh/_en_us/index.md` | second namespace Exemple pour cross-guide liens et isolated même-nom pages |

## ressource Exemples

| exécution fichier | Purpose |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | page-local image Exemple |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | rooted shared structure ressource pour `<ImportStructure>` et `<RemoveBlocks>` |

## Exemple Snippets

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

### relatif image

````md
![Test Image](test1.png)
````

### Namespaced guide liens

````md
[Même espace de noms](guide.md)
[Même espace de noms depuis la racine](/guide.md)
[Autre espace de noms](gregtech:guide.md)
[Autre espace de noms depuis la racine](gregtech:/guide.md)
````

### Rooted Structure ressource

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

Le enfant balises Définissez StructureLib defaults pour Le scène et sont restored lorsque Le scène view est reset.

### Imported Structure Cleanup

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### scène annotation Tooltip

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### Recipe filtre

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## lorsque vers utiliser qui Exemple

- début avec `markdown.md` Si you sont validating analyseur basics
- utiliser `index.md` lorsque testing mixed exécution features together, including StructureLib tooltip, hatch-surligner, et cleanup behavior
- utiliser `scene-blocks.md` lorsque you seulement need static bloc mise en page previews
- utiliser `example_structure.snbt` lorsque you need Un reusable imported structure ressource

## Recommended Learning ordre

1. [Prise en main](Guide-Page-Format)
2. [Format des pages de guide](Guide-Page-Format)
3. [Référence des balises](Tags-Reference)
4. [GameScene](GameScene)
5. [Annotations](Annotations)
6. [Recipes](Recipes)
