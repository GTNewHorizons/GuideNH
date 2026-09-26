# Приклади

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


GuideNH вже ships a виконання Приклад посібник in `wiki/resourcepack/`. Цей сторінка maps  important Приклад files до  feature areas Вони demonstrate.

## Core Приклад сторінки

| виконання файл | What it demonstrates |
| --- | --- |
| `.../_en_us/index.md` | frontmatter, предмет ids, Рецепти, предмет/блок зображення, command links, tooltips, сцени, Анотації, `ImportStructureLib`, `RemoveBlocks`, `BlockAnnotationTemplate`, home-сторінка recommendation Приклад |
| `.../_en_us/markdown.md` | plain Markdown features, таблиці, і a `recommend: 0` Приклад |
| `.../_en_us/rendering.md` | блок-level відтворення і структура поведінка |
| `.../_en_us/scene-blocks.md` | static і інтерактивний блок сцена Приклади |
| `.../_en_us/japanese.md` | Навігація дочірній елемент Приклад |
| `.../_en_us/navigation-guide.md` | Навігація, linking, і high-priority recommendation Приклад |
| `wiki/resourcepack/assets/gregtech/guidenh/_en_us/index.md` | second простір імен Приклад для cross-посібник links і isolated той самий-назва сторінки |

## ресурс Приклади

| виконання файл | Purpose |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | сторінка-локальний зображення Приклад |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | rooted shared структура ресурс для `<ImportStructure>` і `<RemoveBlocks>` |

## Приклад Snippets

### frontmatter + Навігація

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

### відносний зображення

````md
![Test Image](test1.png)
````

### Namespaced посібник Links

````md
[Той самий простір імен](guide.md)
[Той самий простір імен від кореня](/guide.md)
[Інший простір імен](gregtech:guide.md)
[Інший простір імен від кореня](gregtech:/guide.md)
````

### Rooted структура ресурс

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib сцена Import

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

 дочірній елемент теги Установіть StructureLib типові для  сцена і є restored коли  сцена перегляд is reset.

### імпортований структура Cleanup

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### сцена анотація підказка

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### Recipe фільтр

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## коли до використовувати який Приклад

- початок з `markdown.md` Якщо you є validating аналізатор basics
- використовувати `index.md` коли testing mixed виконання features together, including StructureLib підказка, hatch-виділяти, і cleanup поведінка
- використовувати `scene-blocks.md` коли you лише need static блок структура previews
- використовувати `example_structure.snbt` коли you need reusable імпортований структура ресурс

## Recommended Learning порядок

1. [Початок роботи](Guide-Page-Format)
2. [Формат сторінки посібника](Guide-Page-Format)
3. [Довідник тегів](Tags-Reference)
4. [GameScene](GameScene)
5. [Annotations](Annotations)
6. [Recipes](Recipes)
