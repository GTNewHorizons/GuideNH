# Przykłady

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


GuideNH już ships czas wykonania Przykład przewodnik in `wiki/resourcepack/`. Ten strona maps  important Przykład files do  feature areas jeden demonstrate.

## Core Przykład strony

| czas wykonania plik | What it demonstrates |
| --- | --- |
| `.../_en_us/index.md` | frontmatter, element ids, Receptury, element/blok obrazy, command links, tooltips, sceny, Adnotacje, `ImportStructureLib`, `RemoveBlocks`, `BlockAnnotationTemplate`, home-strona recommendation Przykład |
| `.../_en_us/markdown.md` | plain Markdown features, tabele, i a `recommend: 0` Przykład |
| `.../_en_us/rendering.md` | blok-level renderowanie i układ zachowanie |
| `.../_en_us/scene-blocks.md` | static i interaktywny blok scena Przykłady |
| `.../_en_us/japanese.md` | Nawigacja element podrzędny Przykład |
| `.../_en_us/navigation-guide.md` | Nawigacja, linking, i high-priority recommendation Przykład |
| `wiki/resourcepack/assets/gregtech/guidenh/_en_us/index.md` | second przestrzeń nazw Przykład dla cross-przewodnik links i isolated ten sam-nazwa strony |

## zasób Przykłady

| czas wykonania plik | Purpose |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | strona-lokalny obraz Przykład |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | rooted shared struktura zasób dla `<ImportStructure>` i `<RemoveBlocks>` |

## Przykład Snippets

### frontmatter + Nawigacja

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

### względny obraz

````md
![Test Image](test1.png)
````

### Namespaced przewodnik Links

````md
[Ta sama przestrzeń nazw](guide.md)
[Ta sama przestrzeń nazw od katalogu głównego](/guide.md)
[Inna przestrzeń nazw](gregtech:guide.md)
[Inna przestrzeń nazw od katalogu głównego](gregtech:/guide.md)
````

### Rooted struktura zasób

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib scena Import

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

 element podrzędny tagi Ustaw StructureLib domyślne dla  scena i są restored gdy  scena widok is reset.

### zaimportowany struktura Cleanup

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### scena adnotacja podpowiedź

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### Recipe filtr

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## gdy do używać który Przykład

- początek z `markdown.md` Jeśli you są validating parser basics
- używać `index.md` gdy testing mixed czas wykonania features together, including StructureLib podpowiedź, hatch-wyróżniać, i cleanup zachowanie
- używać `scene-blocks.md` gdy you tylko need static blok układ previews
- używać `example_structure.snbt` gdy you need reusable zaimportowany struktura zasób

## Recommended Learning kolejność

1. [Pierwsze kroki](Guide-Page-Format)
2. [Format stron przewodnika](Guide-Page-Format)
3. [Składnia znaczników](Tags-Reference)
4. [GameScene](GameScene)
5. [Annotations](Annotations)
6. [Recipes](Recipes)
