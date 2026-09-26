# Syntaxvervollständigung


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

GuideNH's Seite Editor completes MDX Tags, ihre Attribut, Attribut Werte, markdown Syntax, code
fence names und frontmatter Schlüssel. None von dass ist hardcoded: everything Die Editor offers comes von ein
registry dass Ihre mod kann Fügen Sie hinzu zu.

There sind zwei ways in, und you normally want beide.

## 1. Tag names come von Ihre Tag Compiler

Die Editor lists Die Tag names von jede registriert `TagCompiler` und `SceneElementTagCompiler`. Wenn Ihre
mod bereits registers Eine Tag Compiler, seine Tags sind offered in Die Editor mit nein extra funktionieren:

ein `TagCompiler` kann sein registriert für jede Leitfaden durch
`GuideNhIntegrationRegistry.registerTagCompilerProvider(...)`, und ein `SceneElementTagCompiler` durch
`registerSceneElementTagCompilerProvider(...)`; beide kann auch sein added zu eins Leitfaden mit
`GuideBuilder.extension(...)`. Eine provider ist lesen wenn Eine Leitfaden ist built, so registrieren it während Ihre mod ist
Laden rather als während someone ist reading.

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

registrieren it Die gleich way you bereits do:

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

oder für Eine einzeln Leitfaden:

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

Tags Eine Compiler accepts but Seite authors nie schreiben - Parser Ausgabe such als `p`, `h1` oder `em` - sind nicht
offered. Die library hides seine eigene mit `SyntaxSink.hiddenTags(...)`.

## 2. Eine Syntax contributor adds Die rest

Eine Tag Compiler tut nicht say which Attribut it reads oder what those Attribut take. dass ist what ein
`SyntaxContributor` ist für. It auch declares markdown snippets, code fence names und frontmatter Schlüssel.

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

registrieren it globally, oder per Leitfaden:

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

Globally registriert contributors sind applied erste, so Eine Leitfaden's eigene contributor kann Überschreibung sie.

Die built-in Syntax dass ships mit Die mod ist ein ordinary contributor (`BuiltinSyntaxContributor`)
registriert als Eine Standard extension. `GuideBuilder.disableDefaultExtensions()` daher turns Die whole
built-in Syntax off along mit Die Standard Tag compilers.

## 3. Wert kinds und Wert sources

Eine Attribut's `SyntaxValueKind` decides how Die Editor completes und quotes seine Wert:

| Kind | Completion | Written als |
| --- | --- | --- |
| `STRING` | free Text (oder Die Werte declared nächste zu Die Attribut) | `"..."` |
| `INT`, `FLOAT` | numeric presets für well known Attribut names | bare |
| `BOOLEAN` | `true`, `false` | bare |
| `ENUM` | Die constants von Die enum passed zu `AttributeSyntax.of`, oder Die declared Werte | `"..."` |
| `COLOR` | `#rrggbb` plus Die symbolic colour names | `"..."` |
| `ITEM_ID`, `BLOCK_ID` | Element und Block Registrierungsnames, mit icons | `"..."` |
| `ORE_DICT`, `ENTITY_ID`, `KEY_BIND`, `COMMAND` | Die passend game registry | `"..."` |
| `PAGE_PATH`, `FILE_PATH` | Leitfaden Seite ids, und files in Die Leitfaden assets | `"..."` |
| `MOD_ID` | mod ids taken von Die Element Registrierungsnamespaces | `"..."` |
| `EXPRESSION`, `DOMAIN`, `FORMAT_PATTERN` | allgemein expressions, intervals und patterns | `"..."` |
| `VECTOR3` | free Text | bare |
| `SNBT` | free Text | `{...}` |
| `QUEST_UUID` | free Text; quest ids sind nicht discoverable yet | `"..."` |

Reusing Eine built-in kind bedeutet you do nicht registrieren anything: marking Eine Attribut als `ITEM_ID` ist enough
zu get Element completion für it.

zu vollständig von Ihre eigene registry, declare Eine kind und Eine Quelle. Eine kind ist just Eine Name, so you kann
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

`request` carries Die Typd Text, Die enclosing Tag und Attribut, und Die frontmatter Schlüssel, so eins
Quelle kann serve several Attribut.

## 4. Live Daten

Eine Quelle dass needs Daten von Die open Leitfaden oder von Die document implements
`SyntaxEnvironmentAware`. `prepare` ist called once per completion tick, vor beliebig request ist answered:

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide(), environment.pagePaths(), environment.documentText()
    }
}
```

Beibehalten `prepare` cheap: it runs auf jede Editor tick. Cache derived lists und nur rebuild sie wenn Die
Eingabe actually changed - dass ist what Die built-in `PagePathValueSource` und `AnchorValueSource` do.

## 5. Insert templates

Completing Eine Tag Name writes `<Name />`, oder Eine paired Tag mit Die caret innerhalb Die container. Eine Tag dass
needs Attribut oder Inhalt zu sein useful declares Die Text it sollte vollständig als stattdessen:

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` puts Die caret rechts nach Die erste occurrence von Die marker it ist given, so Eine template kann
say where typing continues ohne counting characters. `InsertTemplate.of(tag, text)` writes Die Text
mit Die caret at seine Ende. Die template replaces Die Typd Tag Name, so Die author accepts it und carries
auf innerhalb Die form dass was written.

Die mod itself declares templates für Die Tags whose useful form ist mehr als Die Tag Name, für Beispiel
`<BlockStat item="" count="1" />`, `<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />` und Eine Diagramm
mit seine erste Serie.

## 6. Slots: Syntax von Ihre eigene

Everything über describes Syntax dies mod bereits parses. für Syntax von Ihre eigene - Eine directive innerhalb ein
Block, Eine small Sprache in Eine code fence, Eine Feld you kompilieren yourself - registrieren ein `SyntaxSlot`:

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

registrieren it globally oder per Leitfaden, genau like Eine contributor:

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

What Eine slot controls:

- `match` decides Die range Eine accepted Wert replaces, Die Text bereits Typd innerhalb it und Die Werte
  Die Editor offers. zurückgeben `null` wenn Die caret ist nicht innerhalb Ihre Syntax. Die Text you report als Typd
  hat zu sein what Die document really holds in dass range: Die Editor re-checks it vor writing, und ein
  normalized form von it would drop Die commit.
- Die `SyntaxValueWriter` von Die übereinstimmen writes Eine accepted Wert: Die Text dass replaces Die range, where
  Die caret lands und what ist ausgewählt. Leaving it `null` replaces Die range mit Die Wert itself. ein
  writer dass throws oder answers nothing ist reported in Die log und Die Editor writes nothing.
- `selection` optionally declares Die range Eine double Klick innerhalb Ihre Syntax selects.

Slots sind asked vor Die Editor's eigene resolvers, und Die erste slot dass stimmt überein owns Die caret even
wenn it offers nein Werte, so dies mod nie guesses innerhalb Text another mod claims.

## 7. What Die Editor bereits completes

- MDX Tags, scoped zu Die enclosing container, mit container Tags completing als `<Name></Name>`.
- Attribut names, which expand zu `name=""`, `name={}` oder `name={true}` according zu Die Wert kind.
- Attribut Werte, routed by Wert kind, including recipes, Element und Block ids, und mod ids.
- Markdown Syntax: headings, lists, task lists, quotes, alerts, Tabellen, code Blöcke, thematic breaks,
  math Blöcke, und inline emphasis, code, Links und Bilder. jede snippet places Die caret where you
  continue typing.
- Code fence names, derived von Die code Block Sprache registry plus Die GuideNH bestimmten fences.
- YAML frontmatter Schlüssel, und Werte für Die Schlüssel dass declare Eine Wert kind.
- Insert templates, so Eine Tag whose useful form ist mehr als seine Name ist written vollständig.

Candidates sind ranked so Die popup pre-selects Die closest übereinstimmen: Eine exact prefix erste, dann Eine übereinstimmen auf
Die identifier nach Die namespace, dann Eine übereinstimmen anywhere in Die Name.

## 8. What you kann extend, und what you kann nicht yet

Everything auf dies Seite ist ein registry, und Eine registriert plugin ist nur lesen wenn Eine Leitfaden ist built oder Eine Seite
kompiliert, so registrieren during mod Laden.

| You want zu | verwenden |
| --- | --- |
| Fügen Sie hinzu Eine Tag, Eine Szene element, Eine Attribut, Eine Wert kind und seine Werte | `TagCompiler`, `SceneElementTagCompiler`, `SyntaxContributor`, `SyntaxValueSource` |
| vollständig Syntax von Ihre eigene (Ihre eigene slot, Ihre eigene writer, double-Klick range) | `SyntaxSlot` |
| Make Eine Tag vollständig als Eine whole form | `sink.insertTemplates(...)` (auch offered in Die Editor's insert menu) |
| Fügen Sie hinzu markdown snippets, fence names, frontmatter Schlüssel und Werte | `SyntaxContributor` |
| Decide what Die Inhalt von Ihre eigene fence bedeutet | `CodeFenceRenderer` (declare Die Name mit `sink.fenceLanguages(...)`; Überschreibung `renderSiteFence` so Die site zeigt it too) |
| Fügen Sie hinzu Eine symbolic colour Name | `SymbolicColorResolver` (per Leitfaden) oder `registerSymbolicColorResolver` |
| Fügen Sie hinzu Eine Seite index | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| Export Ihre Tags zu Die site | `GuideSiteTagRenderer` (Siehe [Mod Compatibility](Mod-Compatibility)) |
| Fügen Sie hinzu Eine Szene Editor toolbar button oder menu Element | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| Fügen Sie hinzu Eine Ressource zu Die exported site | `ExportableResourceProvider` auf Ihre Knoten |
| Report Eine problem mit Ihre eigene Syntax zu Die reader | `LytErrorSink.appendError(compiler, text, element)`, which ist Die `parent` Ihre Tag Compiler ist given |
| Fügen Sie hinzu Eine button oder menu Eintrag zu Die Leitfaden Editor | `GuideNhIntegrationRegistry.registerEditorAction(GuideEditorActionContribution.insert(...))` oder `.wrap(...)`, oder `GuideEditorActionContribution.Provider` für eins Leitfaden |

Deliberately closed, mit what it costs you:

- Die Leitfaden Editor's built-in toolbar ist ein festen Setzen Sie. Eine contributed *action* (Siehe
  `GuideEditorActionContribution`) gets seine eigene toolbar button und menu Eintrag nach Die built-ins, so what
  kann nicht sein added ist ein ändern zu Eine built-in action, nicht Eine new eins.
- Diagnostics sind reported at kompilieren time durch `LytErrorSink`: Die Fehler Block ist appended zu Die
  document, so Die reader sees it in Die book und auf Die exported site. There ist nein separate "warn
  während typing" hook in Die Editor.
- Die site export rendert seine eigene Tags mit Eine built-in chain. Eine registriert renderer ist asked erste, für
  jede MDX element und für Ihre fence names, so Ihre Tags und fences export; Die built-in chain itself ist
  nicht Eine Setzen Sie von registriert renderers yet.
- Szene Annotationen von Ihre eigene werden im Die book but sind nicht serialized in Die exported site's viewer.
- Eine Szene element von Ihre eigene, registriert als ein `SceneElementTagCompiler`, rendert in Die book und reaches Die
  site nur Wenn seine element ist ein `SceneAnnotation`: dass ist Die eins shape Die exporter collects. Eine element von
  beliebig andere shape, such als eins dass zeichnet Geometrie von seine eigene, ist Links out von Die export während Die rest von Die
  Szene ist exported around it. Exporting such Eine element needs Eine Annotation dass carries it, oder Eine site
  renderer für Die Tag dass produces Die markup.

Conventions dass matter für Eine plugin:

- Give Ihre `SyntaxValueKind` Eine mod-namespaced id (`"MYMOD_MACHINE"`). Kinds route zu Wert sources by dass
  id alone; Die quoting hint belongs zu Die Attribut dass declares Die Wert.
- zurückgeben what Die document really holds von `SyntaxSlot.match`: Die Editor re-checks Die Text vor it
  writes, und Eine normalized form drops Die commit.
- Eine plugin dass throws ist reported mit seine namespace und skipped, so Eine failure costs Ihre eigene
  contributions rather als Die Editor.
