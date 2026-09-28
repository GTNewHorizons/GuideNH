# Завершение синтаксиса

Редактор страниц GuideNH дополняет теги MDX, их атрибуты, значения атрибутов, синтаксис markdown, код.
имена ограждений и ключи оформления. Ничего из этого не запрограммировано жестко: все, что предлагает редактор, взято из
реестр, в который ваш мод может добавить.

Есть два пути, и обычно вам нужны оба.

## 1. Имена тегов берутся из вашего компилятора тегов.

Редактор перечисляет имена тегов каждого зарегистрированного `TagCompiler` и `SceneElementTagCompiler`. Если ваш
мод уже регистрирует компилятор тегов, его теги предлагаются в редакторе без дополнительной работы:

`TagCompiler` можно зарегистрировать для каждого руководства через
`GuideNhIntegrationRegistry.registerTagCompilerProvider(...)` и `SceneElementTagCompiler` до
`registerSceneElementTagCompilerProvider(...)`; оба также могут быть добавлены в одно руководство с помощью
`GuideBuilder.extension(...)`. Поставщик считывается при создании руководства, поэтому зарегистрируйте его, пока ваш мод находится в разработке.
загрузка, а не пока кто-то читает.

```java
public class MyModMachineTagCompiler implements TagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("MyMachine");
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        // ...
    }
}
```

Зарегистрируйте его так же, как вы это уже сделали:

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

или для одной направляющей:

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

Теги, которые принимает компилятор, но авторы страниц никогда не пишут; выходные данные анализатора, такие как `p`, `h1` или `em`, не являются
предлагается. Библиотека скрывает свою собственную с помощью `SyntaxSink.hiddenTags(...)`.

## 2. Разработчик синтаксиса добавляет остальное.

Компилятор тегов не сообщает, какие атрибуты он считывает или что принимают эти атрибуты. Вот что
`SyntaxContributor` предназначен для. Он также объявляет фрагменты markdown, имена границ кода и ключи оформления.

```java
public class MyModSyntaxContributor implements SyntaxContributor {

    @Override
    public String namespace() {
        return "mymod";
    }

    @Override
    public void contribute(SyntaxSink sink) {
        // Attributes. The value kind decides which value source answers completion.
        sink.attributes(
            "MyMachine",
            AttributeSyntax.of("id", SyntaxValueKind.STRING),
            AttributeSyntax.of("machine", SyntaxValueKind.of("MYMOD_MACHINE")),  // your own kind
            AttributeSyntax.of("tier", SyntaxValueKind.INT),
            AttributeSyntax.of("formed", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("mode", SyntaxValueKind.ENUM, "input", "output", "both"));

        // Tags that wrap content are completed as <Name></Name> with the caret inside.
        sink.containerTags("MyPanel");
        sink.children("MyPanel", "MyMachine", "Tooltip");

        // Values resolved from your own registry at completion time.
        sink.valueSource(new MachineIdValueSource());

        // Extra markdown and fence names.
        sink.markdown(MarkdownSnippet.block("::", "Mod note", ":: note\n", 3));
        sink.fenceLanguages("mymod-diagram");

        // Frontmatter keys, with fixed values or a value kind.
        sink.frontmatterKeys("mymod_machine");
        sink.frontmatterKind("mymod_machine", SyntaxValueKind.of("MYMOD_MACHINE"));
    }
}
```

Зарегистрируйте его глобально или для каждого руководства:

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

сначала применяются глобально зарегистрированные участники, поэтому собственный участник руководства может их переопределить.

Встроенный синтаксис, поставляемый с модом, является обычным участником (`BuiltinSyntaxContributor`).
зарегистрирован как расширение по умолчанию. Таким образом, `GuideBuilder.disableDefaultExtensions()` превращает все
встроенный синтаксис отключен вместе с компиляторами тегов по умолчанию.

## 3. Виды значений и источники значений.

`SyntaxValueKind` атрибута определяет, как редактор завершает работу, и указывает его значение:

| Вид | Завершение | Написано как |
| --- | --- | --- |
| `STRING` | произвольный текст (или значения, объявленные рядом с атрибутом). | `"..."` |
| `INT`, `FLOAT` | числовые настройки для известных имен атрибутов. | голый |
| `BOOLEAN` | `true`, `false` | голый |
| `ENUM` | константы перечисления, переданные в `AttributeSyntax.of`, или объявленные значения. | `"..."` |
| `COLOR` | `#rrggbb` плюс символические названия цветов. | `"..."` |
| `ITEM_ID`, `BLOCK_ID` | имена элементов и блоков реестра со значками. | `"..."` |
| `ORE_DICT`, `ENTITY_ID`, `KEY_BIND`, `COMMAND` | соответствующий реестр игр. | `"..."` |
| `PAGE_PATH`, `FILE_PATH` | идентификаторы страниц руководства и файлы в ресурсах руководства. | `"..."` |
| `MOD_ID` | идентификаторы модов взяты из пространств имен реестра элементов. | `"..."` |
| `EXPRESSION`, `DOMAIN`, `FORMAT_PATTERN` | распространенные выражения, интервалы и закономерности | `"..."` |
| `VECTOR3` | произвольный текст | голый |
| `SNBT` | произвольный текст | `{...}` |
| `QUEST_UUID` | произвольный текст; идентификаторы квестов пока не обнаруживаются | `"..."` |

повторное использование встроенного типа означает, что вы ничего не регистрируете: достаточно пометить атрибут как `ITEM_ID`.
чтобы получить завершение элемента для него.

Чтобы завершить работу из собственного реестра, укажите тип и источник. Вид – это просто имя, поэтому вы можете
объявите это где хотите:

```java
public final class MyModValueKinds {

    public static final SyntaxValueKind MACHINE = SyntaxValueKind.of("MYMOD_MACHINE");
}
```

```java
public class MachineIdValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(MyModValueKinds.MACHINE);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (MyMachine machine : MyMachineRegistry.all()) {
            if (results.size() >= limit) {
                break;
            }
            if (machine.id()
                .startsWith(request.partialText())) {
                results.add(SyntaxSuggestion.withValue(machine.id(), machine.id(), machine.displayName()));
            }
        }
        return results;
    }
}
```

`request` содержит напечатанный текст, включающий тег и атрибут, а также ключ заголовка, поэтому один
источник может обслуживать несколько атрибутов.

## 4. Текущие данные

источник, которому нужны данные из открытого руководства или документа, реализующего
`SyntaxEnvironmentAware`. `prepare` вызывается один раз за такт завершения, прежде чем будет получен ответ на любой запрос:

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide(), environment.pagePaths(), environment.documentText()
    }
}
```

Сохраняйте `prepare` дешевым: он запускается при каждом тике редактора. Кэшируйте производные списки и перестраивайте их только тогда, когда
ввод действительно изменился — это то, что делают встроенные `PagePathValueSource` и `AnchorValueSource`.

## 5. Вставка шаблонов

При завершении имени тега внутри контейнера записывается `<Name />` или парный тег с курсором. Тег, который
для того, чтобы атрибуты или контент были полезными, вместо этого объявляется текст, который он должен заполнить:

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` помещает курсор сразу после первого появления заданного маркера, поэтому шаблон может
укажите, где продолжается ввод без подсчета символов. `InsertTemplate.of(tag, text)` записывает текст
с курсором в конце. Шаблон заменяет введенное имя тега, поэтому автор принимает его и переносит
внутри написанной формы.

Сам мод объявляет шаблоны для тегов, полезная форма которых больше, чем имя тега, например
`<BlockStat item="" count="1" />`, `<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />` и диаграмма.
с первой серией.

## 6. Слоты: собственный синтаксис

Все вышеописанное описывает синтаксис, который этот мод уже анализирует. Для вашего собственного синтаксиса — директива внутри
блок, небольшой язык в ограждении кода, поле, которое вы компилируете сами — зарегистрируйте `SyntaxSlot`:

```java
public class MyModSlot implements SyntaxSlot {

    @Override
    public String namespace() {
        return "mymod";
    }

    @Override
    public SyntaxSlotMatch match(String text, int cursorIndex, GuideSyntaxModel model) {
        // Own the value of one attribute of your own tag, while its quote is still open.
        String attribute = "ports=\"";
        int attributeStart = text.lastIndexOf(attribute, cursorIndex);
        if (attributeStart < 0) {
            return null;
        }
        int from = attributeStart + attribute.length();
        int closingQuote = text.indexOf('"', from);
        if (closingQuote >= 0 && cursorIndex > closingQuote) {
            return null;
        }
        return new SyntaxSlotMatch(
            from,
            cursorIndex,
            text.substring(from, cursorIndex),
            MyPortRegistry.suggestions(),
            suggestion -> SyntaxReplacement.cursorAtEnd(suggestion.value()));
    }
}
```

Зарегистрируйте его глобально или для каждого руководства, точно так же, как участник:

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

Что контролирует слот:

- `match` определяет диапазон, который заменяет принятое значение, уже введенный внутри него текст и значения.
предлагает редактор. Возвращайте `null`, если курсор находится за пределами вашего синтаксиса. Текст, который вы сообщаете как напечатанный
должно быть то, что документ действительно содержит в этом диапазоне: редактор перепроверяет его перед записью, и
нормализованная форма приведет к удалению фиксации.
- `SyntaxValueWriter` совпадения записывает принятое значение: текст, заменяющий диапазон, где
курсор приземляется и что выбрано. Если оставить это значение, `null` заменяет диапазон самим значением. А
о писателе, который ничего не выдает или не отвечает, сообщается в журнале, и редактор ничего не записывает.
- `selection` опционально объявляет диапазон, который выбирает двойной щелчок внутри вашего синтаксиса.

Слоты запрашиваются перед собственными преобразователями редактора, и первый соответствующий слот владеет даже курсором.
когда он не предлагает никаких значений, поэтому этот мод никогда не угадывает текст, заявленный другим модом.

## 7. Что уже делает редактор

- теги MDX, область действия которых ограничена включающим контейнером, при этом теги контейнера завершаются как `<Name></Name>`.
- имена атрибутов, которые расширяются до `name=""`, `name={}` или `name={true}` в зависимости от типа значения.
- Значения атрибутов, упорядоченные по типу значения, включая рецепты, идентификаторы предметов и блоков, а также идентификаторы модов.
- Синтаксис Markdown: заголовки, списки, списки задач, цитаты, оповещения, таблицы, блоки кода, тематические разрывы,
математические блоки, встроенные выделения, код, ссылки и изображения. Каждый фрагмент помещает курсор туда, где вы
продолжайте печатать.
- Имена ограждений кода, полученные из реестра языка кодовых блоков, а также определенных ограждений GuideNH.
- YAML ключи заголовка и значения для ключей, которые объявляют тип значения.
- Вставляйте шаблоны, чтобы тег, полезная форма которого больше, чем его имя, был записан полностью.

Кандидаты ранжируются таким образом, что всплывающее окно предварительно выбирает наиболее близкое совпадение: сначала точный префикс, затем совпадение
идентификатор после пространства имен, затем совпадение в любом месте имени.

## 8. Что можно продлить, а что пока нельзя

Все на этой странице представляет собой реестр, и зарегистрированный плагин читается только при создании руководства или страницы.
скомпилировано, поэтому зарегистрируйтесь во время загрузки мода.

| Вы хотите | Используйте |
| --- | --- |
| добавьте тег, элемент сцены, атрибут, тип значения и его значения. | `TagCompiler`, `SceneElementTagCompiler`, `SyntaxContributor`, `SyntaxValueSource` |
| собственный полный синтаксис (свой слот, свой писатель, диапазон двойного щелчка). | `SyntaxSlot` |
| Сделайте тег завершенным как целую форму | `sink.insertTemplates(...)` (также предлагается в меню вставки редактора) |
| добавьте фрагменты markdown, имена ограждений, ключи и значения заголовка. | `SyntaxContributor` |
| Решите, что означает тело вашего забора | `CodeFenceRenderer` (объявите имя с помощью `sink.fenceLanguages(...)`; переопределите `renderSiteFence`, чтобы сайт тоже отображал его) |
| добавьте символическое название цвета. | `SymbolicColorResolver` (для каждого руководства) или `registerSymbolicColorResolver` |
| Добавьте индекс страницы | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| экспортируйте теги на сайт. | `GuideSiteTagRenderer` (см. [Совместимость модов](Mod-Compatibility)) |
| добавьте кнопку или элемент меню на панель инструментов редактора сцен. | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| добавьте ресурс на экспортированный сайт. | `ExportableResourceProvider` на вашем узле |
| Сообщите читателю о проблеме с собственным синтаксисом. | `LytErrorSink.appendError(compiler, text, element)`, то есть `parent`, присвоенный вашему компилятору тегов. |
| добавьте кнопку или пункт меню в редактор руководств. | `GuideNhIntegrationRegistry.registerEditorAction(GuideEditorActionContribution.insert(...))` или `.wrap(...)` или `GuideEditorActionContribution.Provider` для одного руководства. |

Намеренно закрыто, чего вам это будет стоить:

- Встроенная панель инструментов редактора руководств представляет собой фиксированный набор. Внесенное *действие* (см.
`GuideEditorActionContribution`) получает собственную кнопку на панели инструментов и пункт меню после встроенных, и что
невозможно добавить — это изменение встроенного действия, а не новое.
- Диагностика сообщается во время компиляции через `LytErrorSink`: блок ошибок добавляется к
документ, чтобы читатель видел его в книге и на экспортированном сайте. Отдельного предупреждения нет.
крючок при наборе текста в редакторе.
- При экспорте сайта отображаются собственные теги со встроенной цепочкой. Сначала запрашивается зарегистрированный рендерер,
каждый элемент MDX и имена ваших ограждений, поэтому ваши теги и ограждения экспортируются; сама встроенная цепочка
пока не зарегистрирован набор рендереров.
- аннотации к сценам вашего собственного рендеринга в книге, но не сериализуются в программе просмотра экспортированного сайта.
- ваш собственный элемент сцены, зарегистрированный как `SceneElementTagCompiler`, визуализируется в книге и достигает
сайт, только если его элементом является `SceneAnnotation`: это единственная фигура, которую собирает экспортер. Элемент
любая другая фигура, например фигура, рисующая собственную геометрию, исключается из экспорта, в то время как остальная часть
вокруг него экспортируется сцена. Для экспорта такого элемента необходима аннотация, содержащая его, или сайт.
средство визуализации тега, создающего разметку.

Соглашения, важные для плагина:

- Присвойте вашему `SyntaxValueKind` идентификатор в пространстве имен мода (`"MYMOD_MACHINE"`). Виды направляются к источникам ценности таким образом
только идентификатор; подсказка о кавычках принадлежит атрибуту, который объявляет значение.
- Возвращает то, что на самом деле содержит документ, из `SyntaxSlot.match`: редактор повторно проверяет текст перед ним.
записывает, а нормализованная форма удаляет фиксацию.
- Плагин, который выдает ошибку, сообщается с его пространством имен и пропускается, поэтому за сбой придется платить самостоятельно.
вклад, а не редактор.
