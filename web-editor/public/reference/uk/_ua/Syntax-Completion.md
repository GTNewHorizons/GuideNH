# Доповнення синтаксису

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


GuideNH's сторінка редактор completes MDX теги, їхні атрибути, Атрибут значення, Markdown синтаксис, code
fence names і frontmatter ключі. None of який is hardcoded: everything  редактор offers comes з a
registry який your mod може Додайте до.

There є два ways in, і you normally want both.

## 1. тег names come з your тег компілятор

 редактор lists  тег names of кожен зареєстрований `TagCompiler` і `SceneElementTagCompiler`. Якщо your
mod вже registers a тег компілятор, його теги є offered in  редактор з no extra працюють:

A `TagCompiler` може be зареєстрований для кожен посібник through
`GuideNhIntegrationRegistry.registerTagCompilerProvider(...)`, і a `SceneElementTagCompiler` through
`registerSceneElementTagCompilerProvider(...)`; both може також be added до один посібник з
`GuideBuilder.extension(...)`. provider is читати коли a посібник is built, so реєструвати it поки your mod is
завантаження rather ніж поки someone is reading.

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

реєструвати it  той самий way you вже do:

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

або для a один посібник:

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

теги компілятор accepts but сторінка authors ніколи записувати - аналізатор вивід наприклад `p`, `h1` або `em` - є не
offered.  library hides його власний з `SyntaxSink.hiddenTags(...)`.

## 2. A синтаксис contributor додає  rest

A тег компілятор не say який атрибути it reads або what those атрибути take. який is what a
`SyntaxContributor` is для. It також declares Markdown snippets, code fence names і frontmatter ключі.

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

реєструвати it globally, або per посібник:

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

Globally зареєстрований contributors є applied перший, so a посібник's власний contributor може перевизначати їх.

 built-in синтаксис який ships з  mod is ordinary contributor (`BuiltinSyntaxContributor`)
зареєстрований as a Типове extension. `GuideBuilder.disableDefaultExtensions()` тому turns  whole
built-in синтаксис off уздовж з  Типове тег compilers.

## 3. значення kinds і значення sources

An Атрибут's `SyntaxValueKind` decides how  редактор completes і quotes його значення:

| тип | Completion | записаний as |
| --- | --- | --- |
| `STRING` | free текст (або  значення declared наступний до  Атрибут) | `"..."` |
| `INT`, `FLOAT` | numeric presets для well known Атрибут names | bare |
| `BOOLEAN` | `true`, `false` | bare |
| `ENUM` |  constants of  enum passed до `AttributeSyntax.of`, або  declared значення | `"..."` |
| `COLOR` | `#rrggbb` plus  symbolic colour names | `"..."` |
| `ITEM_ID`, `BLOCK_ID` | предмет і блок registry names, з icons | `"..."` |
| `ORE_DICT`, `ENTITY_ID`, `KEY_BIND`, `COMMAND` |  відповідний game registry | `"..."` |
| `PAGE_PATH`, `FILE_PATH` | сторінка посібника ids, і files in  посібник ресурси | `"..."` |
| `MOD_ID` | mod ids taken з  предмет registry namespaces | `"..."` |
| `EXPRESSION`, `DOMAIN`, `FORMAT_PATTERN` | звичайний expressions, intervals і patterns | `"..."` |
| `VECTOR3` | free текст | bare |
| `SNBT` | free текст | `{...}` |
| `QUEST_UUID` | free текст; quest ids є не discoverable yet | `"..."` |

Reusing built-in тип означає you do не реєструвати anything: marking an Атрибут as `ITEM_ID` is enough
до get предмет completion для it.

до complete з your власний registry, declare тип і source. тип is just a назва, so you може
declare it where you like:

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

`request` carries  typed текст,  enclosing тег і Атрибут, і  frontmatter ключ, so один
source може serve several атрибути.

## 4. Live дані

source який needs дані з  open посібник або з  document implements
`SyntaxEnvironmentAware`. `prepare` is called once per completion tick, до any request is answered:

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide(), environment.pagePaths(), environment.documentText()
    }
}
```

Збережіть `prepare` cheap: it runs on кожен редактор tick. Cache derived lists і лише rebuild їх коли 
ввід actually changed - який is what  built-in `PagePathValueSource` і `AnchorValueSource` do.

## 5. Insert templates

Completing a тег назва writes `<Name />`, або paired тег з  caret усередині  container. A тег який
needs атрибути або вміст до be useful declares  текст it слід complete as замість цього:

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` puts  caret праворуч після  перший occurrence of  marker it is given, so template може
say where typing continues без counting characters. `InsertTemplate.of(tag, text)` writes  текст
з  caret at його кінець.  template replaces  typed тег назва, so  автор accepts it і carries
on усередині  form який was записаний.

 mod itself declares templates для  теги whose useful form is more ніж  тег назва, для Приклад
`<BlockStat item="" count="1" />`, `<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />` і діаграма
з його перший серія.

## 6. Slots: синтаксис of your власний

Everything над describes синтаксис Цей mod вже parses. для синтаксис of your власний - directive усередині a
блок, small мова in code fence, поле you компілювати yourself - реєструвати a `SyntaxSlot`:

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

реєструвати it globally або per посібник, exactly like contributor:

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

What slot controls:

- `match` decides  range accepted значення replaces,  текст вже typed усередині it і  значення
   редактор offers. повертати `null` коли  caret is не усередині your синтаксис.  текст you report as typed
  має до be what  document really holds in який range:  редактор re-checks it до writing, і a
  normalized form of it would drop  commit.
-  `SyntaxValueWriter` of  відповідати writes accepted значення:  текст який replaces  range, where
   caret lands і what is вибраний. Leaving it `null` replaces  range з  значення itself. A
  writer який throws або answers nothing is reported in  log і  редактор writes nothing.
- `selection` optionally declares  range double натискання усередині your синтаксис selects.

Slots є asked до  редактор's власний resolvers, і  перший slot який відповідає owns  caret even
коли it offers no значення, so Цей mod ніколи guesses усередині текст another mod claims.

## 7. What  редактор вже completes

- MDX теги, scoped до  enclosing container, з container теги completing as `<Name></Name>`.
- Атрибут names, який expand до `name=""`, `name={}` або `name={true}` according до  значення тип.
- Атрибут значення, routed by значення тип, including Рецепти, предмет і блок ids, і mod ids.
- Markdown синтаксис: headings, lists, task lists, quotes, alerts, таблиці, блоки коду, thematic breaks,
  math блоки, і inline emphasis, code, links і зображення. кожен snippet places  caret where you
  continue typing.
- Code fence names, derived з  блок коду мова registry plus  GuideNH певний fences.
- YAML frontmatter ключі, і значення для  ключі який declare a значення тип.
- Insert templates, so a тег whose useful form is more ніж його назва is записаний complete.

Candidates є ranked so  popup pre-selects  closest відповідати: exact prefix перший, потім відповідати on
 identifier після  простір імен, потім відповідати anywhere in  назва.

## 8. What you може extend, і what you не може yet

Everything on Цей сторінка is registry, і зареєстрований plugin is лише читати коли a посібник is built або a сторінка
скомпільований, so реєструвати during mod завантаження.

| You want до | використовувати |
| --- | --- |
| Додайте a тег, a сцена element, an Атрибут, a значення тип і його значення | `TagCompiler`, `SceneElementTagCompiler`, `SyntaxContributor`, `SyntaxValueSource` |
| Complete синтаксис of your власний (your власний slot, your власний writer, double-натискання range) | `SyntaxSlot` |
| Make a тег complete as whole form | `sink.insertTemplates(...)` (також offered in  редактор's insert menu) |
| Додайте Markdown snippets, fence names, frontmatter ключі і значення | `SyntaxContributor` |
| Decide what  body of your власний fence означає | `CodeFenceRenderer` (declare  назва з `sink.fenceLanguages(...)`; перевизначати `renderSiteFence` so  site shows it too) |
| Додайте symbolic colour назва | `SymbolicColorResolver` (per посібник) або `registerSymbolicColorResolver` |
| Додайте a сторінка index | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| Export your теги до  site | `GuideSiteTagRenderer` (Дивіться [Mod Compatibility](Mod-Compatibility)) |
| Додайте a сцена редактор toolbar button або menu предмет | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| Додайте a ресурс до  exported site | `ExportableResourceProvider` on your node |
| Report problem з your власний синтаксис до  reader | `LytErrorSink.appendError(compiler, text, element)`, який is  `parent` your тег компілятор is given |
| Додайте button або menu запис до  посібник редактор | `GuideNhIntegrationRegistry.registerEditorAction(GuideEditorActionContribution.insert(...))` або `.wrap(...)`, або `GuideEditorActionContribution.Provider` для один посібник |

Deliberately closed, з what it costs you:

-  посібник редактор's built-in toolbar is a фіксований Установіть. contributed *action* (Дивіться
  `GuideEditorActionContribution`) gets його власний toolbar button і menu запис після  built-ins, so what
  не може be added is a змінити до built-in action, не новий один.
- Diagnostics є reported at компілювати час through `LytErrorSink`:  помилка блок is appended до 
  document, so  reader sees it in  book і on  exported site. There is no separate "warn
  поки typing" hook in  редактор.
-  site export renders його власний теги з built-in chain. зареєстрований renderer is asked перший, для
  кожен MDX element і для your fence names, so your теги і fences export;  built-in chain itself is
  не a Установіть of зареєстрований renderers yet.
- сцена Анотації of your власний відтворювати in  book but є не serialized до  exported site's viewer.
- A сцена element of your власний, зареєстрований as a `SceneElementTagCompiler`, renders in  book і reaches 
  site лише Якщо його element is a `SceneAnnotation`: який is  один shape  exporter collects. element of
  any other shape, наприклад один який малює геометрією of його власний, is ліворуч out of  export поки  rest of 
  сцена is exported around it. Exporting such element needs an анотація який carries it, або site
  renderer для  тег який produces  markup.

Conventions який matter для plugin:

- Give your `SyntaxValueKind` mod-namespaced id (`"MYMOD_MACHINE"`). Kinds route до значення sources by який
  id alone;  quoting hint belongs до  Атрибут який declares  значення.
- повертати what  document really holds з `SyntaxSlot.match`:  редактор re-checks  текст до it
  writes, і normalized form drops  commit.
- plugin який throws is reported з його простір імен і skipped, so failure costs your власний
  contributions rather ніж  редактор.
