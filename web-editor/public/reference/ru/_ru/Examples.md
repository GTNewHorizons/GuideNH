# Примеры

GuideNH уже содержит пример руководства по среде выполнения в `wiki/resourcepack/`. На этой странице важные файлы примеров сопоставлены с областями функций, которые они демонстрируют.

## Основные примеры страниц

| Файл времени выполнения | Что он демонстрирует |
| --- | --- |
| `.../_en_us/index.md` | заголовок, идентификаторы предметов, рецепты, изображения предметов/блоков, ссылки на команды, всплывающие подсказки, сцены, аннотации, `ImportStructureLib`, `RemoveBlocks`, `BlockAnnotationTemplate`, пример рекомендации на домашней странице. |
| `.../_en_us/markdown.md` | простые функции markdown, таблицы и пример `recommend: 0`. |
| `.../_en_us/rendering.md` | поведение рендеринга и макета на уровне блоков. |
| `.../_en_us/scene-blocks.md` | примеры статических и интерактивных блочных сцен. |
| `.../_en_us/japanese.md` | пример дочерней навигации |
| `.../_en_us/navigation-guide.md` | пример навигации, ссылок и рекомендаций с высоким приоритетом. |
| `wiki/resourcepack/assets/gregtech/guidenh/_en_us/index.md` | второй пример пространства имен для перекрестных ссылок и изолированных одноимённых страниц. |

## Примеры активов

| Файл времени выполнения | Назначение |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | пример изображения на локальной странице |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | актив корневой общей структуры для `<ImportStructure>` и `<RemoveBlocks>`. |

## Примеры фрагментов

### Фронтальная часть + Навигация

```yaml
item_ids:
  - minecraft:book
navigation:
  title: Корень
  icon_texture: test1.png
  recommend: 3
```

### Домашняя рекомендация

```yaml
navigation:
  title: Основы Markdown
  parent: index.md
  recommend: 0
```

```yaml
navigation:
  title: Навигация и индекс
  parent: index.md
  recommend: 5
```

### относительное изображение

````md
![Тестовое изображение](test1.png)
````

### Ссылки руководства в пространстве имен

````md
[То же пространство имён](guide.md)
[То же пространство имён из корня](/guide.md)
[Другое пространство имён](gregtech:guide.md)
[Другое пространство имён из корня](gregtech:/guide.md)
````

### Актив корневой структуры

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### Импорт сцены StructureLib

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

дочерние теги устанавливают значения по умолчанию для сцены в StructureLib и восстанавливаются при сбросе вида сцены.

### Очистка импортированной структуры

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### Подсказка к аннотациям сцены

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Активированный маяк
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### Фильтр рецептов

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## Когда какой пример использовать

- начните с `markdown.md`, если вы проверяете основы синтаксического анализатора.
- используйте `index.md` при совместном тестировании смешанных функций среды выполнения, включая всплывающую подсказку StructureLib, выделение штриховки и поведение очистки.
- используйте `scene-blocks.md`, если вам нужен только предварительный просмотр макета статического блока.
- используйте `example_structure.snbt`, если вам нужен многократно используемый импортированный ресурс структуры.

## Рекомендуемый порядок обучения

1. [Начало работы](Guide-Page-Format)
2. [Формат страницы руководства](Guide-Page-Format)
3. [Справочник тегов](Tags-Reference)
4. [GameScene](GameScene)
5. [Аннотации](Annotations)
6. [Рецепты](Recipes)
