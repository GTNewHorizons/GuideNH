# Uzupełnianie składni

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


GuideNH's strona edytor completes MDX tagi, ich atrybuty, Atrybut wartości, Markdown składnia, code
fence names i frontmatter klucze. None of który is hardcoded: everything  edytor offers comes z a
registry który your mod może Dodaj do.

There są dwa ways in, i you normally want both.

## 1. tag names come z your tag kompilator

 edytor lists  tag names of każdy zarejestrowany `TagCompiler` i `SceneElementTagCompiler`. Jeśli your
mod już registers tag kompilator, jego tagi są offered in  edytor z no extra działa:

A `TagCompiler` może be zarejestrowany dla każdy przewodnik through
`GuideNhIntegrationRegistry.registerTagCompilerProvider(...)`, i a `SceneElementTagCompiler` through
`registerSceneElementTagCompilerProvider(...)`; both może także be added do jeden przewodnik z
`GuideBuilder.extension(...)`. provider is czytać gdy przewodnik is built, so rejestrować it podczas your mod is
ładowanie rather niż podczas someone is reading.

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

rejestrować it  ten sam way you już do:

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

lub dla pojedynczy przewodnik:

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

tagi kompilator accepts but strona authors nigdy zapisywać - parser wyjście takie jak `p`, `h1` lub `em` - są nie
offered.  library hides jego własny z `SyntaxSink.hiddenTags(...)`.

## 2. składnia contributor dodaje  rest

tag kompilator nie say który atrybuty it reads lub what those atrybuty take. który is what a
`SyntaxContributor` is dla. It także declares Markdown snippets, code fence names i frontmatter klucze.

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

rejestrować it globally, lub per przewodnik:

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

Globally zarejestrowany contributors są applied pierwszy, so przewodnik's własny contributor może nadpisywać ich.

 built-in składnia który ships z  mod is ordinary contributor (`BuiltinSyntaxContributor`)
zarejestrowany as Domyślne extension. `GuideBuilder.disableDefaultExtensions()` dlatego turns  whole
built-in składnia off wzdłuż z  Domyślne tag compilers.

## 3. wartość kinds i wartość sources

Atrybut's `SyntaxValueKind` decides how  edytor completes i quotes jego wartość:

| rodzaj | Completion | zapisany as |
| --- | --- | --- |
| `STRING` | free tekst (lub  wartości declared następny do  Atrybut) | `"..."` |
| `INT`, `FLOAT` | numeric presets dla well known Atrybut names | bare |
| `BOOLEAN` | `true`, `false` | bare |
| `ENUM` |  constants of  enum passed do `AttributeSyntax.of`, lub  declared wartości | `"..."` |
| `COLOR` | `#rrggbb` plus  symbolic colour names | `"..."` |
| `ITEM_ID`, `BLOCK_ID` | element i blok registry names, z icons | `"..."` |
| `ORE_DICT`, `ENTITY_ID`, `KEY_BIND`, `COMMAND` |  pasujący game registry | `"..."` |
| `PAGE_PATH`, `FILE_PATH` | strona przewodnika ids, i files in  przewodnik zasoby | `"..."` |
| `MOD_ID` | mod ids taken z  element registry namespaces | `"..."` |
| `EXPRESSION`, `DOMAIN`, `FORMAT_PATTERN` | wspólny expressions, intervals i patterns | `"..."` |
| `VECTOR3` | free tekst | bare |
| `SNBT` | free tekst | `{...}` |
| `QUEST_UUID` | free tekst; quest ids są nie discoverable yet | `"..."` |

Reusing built-in rodzaj oznacza you do nie rejestrować anything: marking Atrybut as `ITEM_ID` is enough
do get element completion dla it.

do complete z your własny registry, declare rodzaj i source. rodzaj is just nazwa, so you może
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

`request` carries  typed tekst,  enclosing tag i Atrybut, i  frontmatter klucz, so jeden
source może serve several atrybuty.

## 4. Live dane

source który needs dane z  open przewodnik lub z  document implements
`SyntaxEnvironmentAware`. `prepare` is called once per completion tick, przed any request is answered:

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide(), environment.pagePaths(), environment.documentText()
    }
}
```

Zachowaj `prepare` cheap: it runs on każdy edytor tick. Cache derived lists i tylko rebuild ich gdy 
wejście actually changed - który is what  built-in `PagePathValueSource` i `AnchorValueSource` do.

## 5. Insert templates

Completing tag nazwa writes `<Name />`, lub paired tag z  caret wewnątrz  container. tag który
needs atrybuty lub treść do be useful declares  tekst it powinien complete as zamiast tego:

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` puts  caret prawo po  pierwszy occurrence of  marker it is given, so template może
say where typing continues bez counting characters. `InsertTemplate.of(tag, text)` writes  tekst
z  caret at jego koniec.  template replaces  typed tag nazwa, so  autor accepts it i carries
on wewnątrz  form który was zapisany.

 mod itself declares templates dla  tagi whose useful form is more niż  tag nazwa, dla Przykład
`<BlockStat item="" count="1" />`, `<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />` i wykres
z jego pierwszy seria.

## 6. Slots: składnia of your własny

Everything nad describes składnia Ten mod już parses. dla składnia of your własny - directive wewnątrz a
blok, small język in code fence, pole you kompilować yourself - rejestrować a `SyntaxSlot`:

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

rejestrować it globally lub per przewodnik, exactly like contributor:

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

What slot controls:

- `match` decides  range accepted wartość replaces,  tekst już typed wewnątrz it i  wartości
   edytor offers. zwracać `null` gdy  caret is nie wewnątrz your składnia.  tekst you report as typed
  ma do be what  document really holds in który range:  edytor re-checks it przed writing, i a
  normalized form of it would drop  commit.
-  `SyntaxValueWriter` of  pasować writes accepted wartość:  tekst który replaces  range, where
   caret lands i what is wybrany. Leaving it `null` replaces  range z  wartość itself. A
  writer który throws lub answers nothing is reported in  log i  edytor writes nothing.
- `selection` optionally declares  range double kliknięcie wewnątrz your składnia selects.

Slots są asked przed  edytor's własny resolvers, i  pierwszy slot który pasuje owns  caret even
gdy it offers no wartości, so Ten mod nigdy guesses wewnątrz tekst another mod claims.

## 7. What  edytor już completes

- MDX tagi, scoped do  enclosing container, z container tagi completing as `<Name></Name>`.
- Atrybut names, który expand do `name=""`, `name={}` lub `name={true}` according do  wartość rodzaj.
- Atrybut wartości, routed by wartość rodzaj, including Receptury, element i blok ids, i mod ids.
- Markdown składnia: headings, lists, task lists, quotes, alerts, tabele, bloki kodu, thematic breaks,
  math bloki, i inline emphasis, code, links i obrazy. każdy snippet places  caret where you
  continue typing.
- Code fence names, derived z  blok kodu język registry plus  GuideNH określony fences.
- YAML frontmatter klucze, i wartości dla  klucze który declare wartość rodzaj.
- Insert templates, so tag whose useful form is more niż jego nazwa is zapisany complete.

Candidates są ranked so  popup pre-selects  closest pasować: exact prefix pierwszy, następnie pasować on
 identifier po  przestrzeń nazw, następnie pasować anywhere in  nazwa.

## 8. What you może extend, i what you nie może yet

Everything on Ten strona is registry, i zarejestrowany plugin is tylko czytać gdy przewodnik is built lub strona
skompilowany, so rejestrować during mod ładowanie.

| You want do | używać |
| --- | --- |
| Dodaj tag, scena element, Atrybut, wartość rodzaj i jego wartości | `TagCompiler`, `SceneElementTagCompiler`, `SyntaxContributor`, `SyntaxValueSource` |
| Complete składnia of your własny (your własny slot, your własny writer, double-kliknięcie range) | `SyntaxSlot` |
| Make tag complete as whole form | `sink.insertTemplates(...)` (także offered in  edytor's insert menu) |
| Dodaj Markdown snippets, fence names, frontmatter klucze i wartości | `SyntaxContributor` |
| Decide what  body of your własny fence oznacza | `CodeFenceRenderer` (declare  nazwa z `sink.fenceLanguages(...)`; nadpisywać `renderSiteFence` so  site shows it too) |
| Dodaj symbolic colour nazwa | `SymbolicColorResolver` (per przewodnik) lub `registerSymbolicColorResolver` |
| Dodaj strona index | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| Export your tagi do  site | `GuideSiteTagRenderer` (Zobacz [Mod Compatibility](Mod-Compatibility)) |
| Dodaj scena edytor toolbar button lub menu element | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| Dodaj zasób do  exported site | `ExportableResourceProvider` on your node |
| Report problem z your własny składnia do  reader | `LytErrorSink.appendError(compiler, text, element)`, który is  `parent` your tag kompilator is given |
| Dodaj button lub menu wpis do  przewodnik edytor | `GuideNhIntegrationRegistry.registerEditorAction(GuideEditorActionContribution.insert(...))` lub `.wrap(...)`, lub `GuideEditorActionContribution.Provider` dla jeden przewodnik |

Deliberately closed, z what it costs you:

-  przewodnik edytor's built-in toolbar is stały Ustaw. contributed *action* (Zobacz
  `GuideEditorActionContribution`) gets jego własny toolbar button i menu wpis po  built-ins, so what
  nie może be added is zmiana do built-in action, nie nowy jeden.
- Diagnostics są reported at kompilować czas through `LytErrorSink`:  błąd blok is appended do 
  document, so  reader sees it in  book i on  exported site. There is no separate "warn
  podczas typing" hook in  edytor.
-  site export renders jego własny tagi z built-in chain. zarejestrowany renderer is asked pierwszy, dla
  każdy MDX element i dla your fence names, so your tagi i fences export;  built-in chain itself is
  nie Ustaw of zarejestrowany renderers yet.
- scena Adnotacje of your własny renderować in  book but są nie serialized do  exported site's viewer.
- scena element of your własny, zarejestrowany as a `SceneElementTagCompiler`, renders in  book i reaches 
  site tylko Jeśli jego element is a `SceneAnnotation`: który is  jeden shape  exporter collects. element of
  any other shape, takie jak jeden który rysuje geometrią of jego własny, is lewo out of  export podczas  rest of 
  scena is exported around it. Exporting such element needs adnotacja który carries it, lub site
  renderer dla  tag który produces  markup.

Conventions który matter dla plugin:

- Give your `SyntaxValueKind` mod-namespaced id (`"MYMOD_MACHINE"`). Kinds route do wartość sources by który
  id alone;  quoting hint belongs do  Atrybut który declares  wartość.
- zwracać what  document really holds z `SyntaxSlot.match`:  edytor re-checks  tekst przed it
  writes, i normalized form drops  commit.
- plugin który throws is reported z jego przestrzeń nazw i skipped, so failure costs your własny
  contributions rather niż  edytor.
