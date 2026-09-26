# Syntaxisaanvulling

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


GuideNH's pagina editor completes MDX tags, their attributen, Attribuut waarden, Markdown syntaxis, code
fence names en frontmatter keys. None of that is hardcoded: everything De editor offers comes van a
registry that your mod kan Voeg toe naar.

There are two ways in, en you normally want both.

## 1. tag names come van your tag compiler

De editor lists De tag names of iedere registered `TagCompiler` en `SceneElementTagCompiler`. Als your
mod al registers Een tag compiler, its tags are offered in De editor met no extra work:

A `TagCompiler` kan be registered voor iedere gids through
`GuideNhIntegrationRegistry.registerTagCompilerProvider(...)`, en a `SceneElementTagCompiler` through
`registerSceneElementTagCompilerProvider(...)`; both kan ook be added naar one gids met
`GuideBuilder.extension(...)`. Een provider is read wanneer Een gids is built, so register it terwijl your mod is
loading rather than terwijl someone is reading.

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

Register it De zelfde way you al do:

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

of voor Een enkele gids:

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

tags Een compiler accepts but pagina authors never write - parser output such as `p`, `h1` of `em` - are niet
offered. De library hides its own met `SyntaxSink.hiddenTags(...)`.

## 2. Een syntaxis contributor adds De rest

Een tag compiler doet niet say which attributen it reads of what those attributen take. That is what a
`SyntaxContributor` is voor. It ook declares Markdown snippets, code fence names en frontmatter keys.

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

Register it globally, of per gids:

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

Globally registered contributors are applied first, so Een gids's own contributor kan override them.

De built-in syntaxis that ships met De mod is Een ordinary contributor (`BuiltinSyntaxContributor`)
registered as Een Standaard extension. `GuideBuilder.disableDefaultExtensions()` therefore turns De whole
built-in syntaxis off along met De Standaard tag compilers.

## 3. waarde kinds en waarde sources

Een Attribuut's `SyntaxValueKind` decides how De editor completes en quotes its waarde:

| Kind | Completion | Written as |
| --- | --- | --- |
| `STRING` | free tekst (of De waarden declared next naar De Attribuut) | `"..."` |
| `INT`, `FLOAT` | numeric presets voor well known Attribuut names | bare |
| `BOOLEAN` | `true`, `false` | bare |
| `ENUM` | De constants of De enum passed naar `AttributeSyntax.of`, of De declared waarden | `"..."` |
| `COLOR` | `#rrggbb` plus De symbolic colour names | `"..."` |
| `ITEM_ID`, `BLOCK_ID` | item en blok registry names, met icons | `"..."` |
| `ORE_DICT`, `ENTITY_ID`, `KEY_BIND`, `COMMAND` | De matching game registry | `"..."` |
| `PAGE_PATH`, `FILE_PATH` | gids­pagina ids, en files in De gids assets | `"..."` |
| `MOD_ID` | mod ids taken van De item registry namespaces | `"..."` |
| `EXPRESSION`, `DOMAIN`, `FORMAT_PATTERN` | algemene expressions, intervals en patterns | `"..."` |
| `VECTOR3` | free tekst | bare |
| `SNBT` | free tekst | `{...}` |
| `QUEST_UUID` | free tekst; quest ids are niet discoverable yet | `"..."` |

Reusing Een built-in kind betekent you do niet register anything: marking Een Attribuut as `ITEM_ID` is enough
naar get item completion voor it.

naar complete van your own registry, declare Een kind en Een source. Een kind is just Een naam, so you kan
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

`request` carries De typed tekst, De enclosing tag en Attribuut, en De frontmatter key, so one
source kan serve several attributen.

## 4. Live data

Een source that needs data van De open gids of van De document implements
`SyntaxEnvironmentAware`. `prepare` is called once per completion tick, voor any request is answered:

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide(), environment.pagePaths(), environment.documentText()
    }
}
```

Behoud `prepare` cheap: it runs on iedere editor tick. Cache derived lists en alleen rebuild them wanneer De
input actually changed - that is what De built-in `PagePathValueSource` en `AnchorValueSource` do.

## 5. Insert templates

Completing Een tag naam writes `<Name />`, of Een paired tag met De caret binnen De container. Een tag that
needs attributen of inhoud naar be useful declares De tekst it zou moeten complete as instead:

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` puts De caret right na De first occurrence of De marker it is given, so Een template kan
say where typing continues zonder counting characters. `InsertTemplate.of(tag, text)` writes De tekst
met De caret at its einde. De template replaces De typed tag naam, so De author accepts it en carries
on binnen De form that was written.

De mod itself declares templates voor De tags whose useful form is more than De tag naam, voor example
`<BlockStat item="" count="1" />`, `<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />` en Een chart
met its first series.

## 6. Slots: syntaxis of your own

Everything above describes syntaxis Deze mod al parses. voor syntaxis of your own - Een directive binnen a
blok, Een small taal in Een code fence, Een field you compile yourself - register a `SyntaxSlot`:

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

Register it globally of per gids, exactly like Een contributor:

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

What Een slot controls:

- `match` decides De range Een accepted waarde replaces, De tekst al typed binnen it en De waarden
  De editor offers. Return `null` wanneer De caret is niet binnen your syntaxis. De tekst you report as typed
  has naar be what De document really holds in that range: De editor re-checks it voor writing, en a
  normalized form of it would drop De commit.
- De `SyntaxValueWriter` of De match writes Een accepted waarde: De tekst that replaces De range, where
  De caret lands en what is geselecteerd. Leaving it `null` replaces De range met De waarde itself. A
  writer that throws of answers nothing is reported in De log en De editor writes nothing.
- `selection` optionally declares De range Een double klikken binnen your syntaxis selects.

Slots are asked voor De editor's own resolvers, en De first slot that matches owns De caret even
wanneer it offers no waarden, so Deze mod never guesses binnen tekst another mod claims.

## 7. What De editor al completes

- MDX tags, scoped naar De enclosing container, met container tags completing as `<Name></Name>`.
- Attribuut names, which expand naar `name=""`, `name={}` of `name={true}` according naar De waarde kind.
- Attribuut waarden, routed by waarde kind, including Recepten, item en blok ids, en mod ids.
- Markdown syntaxis: headings, lists, task lists, quotes, alerts, tabellen, codeblokken, thematic breaks,
  math blokken, en inline emphasis, code, links en afbeeldingen. elke snippet places De caret where you
  continue typing.
- Code fence names, derived van De codeblok taal registry plus De GuideNH specific fences.
- YAML frontmatter keys, en waarden voor De keys that declare Een waarde kind.
- Insert templates, so Een tag whose useful form is more than its naam is written complete.

Candidates are ranked so De popup pre-selects De closest match: Een exact prefix first, then Een match on
De identifier na De namespace, then Een match anywhere in De naam.

## 8. What you kan extend, en what you kan niet yet

Everything on Deze pagina is Een registry, en Een registered plugin is alleen read wanneer Een gids is built of Een pagina
compiled, so register during mod loading.

| You want naar | gebruiken |
| --- | --- |
| Voeg toe Een tag, Een scène element, Een Attribuut, Een waarde kind en its waarden | `TagCompiler`, `SceneElementTagCompiler`, `SyntaxContributor`, `SyntaxValueSource` |
| Complete syntaxis of your own (your own slot, your own writer, double-klikken range) | `SyntaxSlot` |
| Make Een tag complete as Een whole form | `sink.insertTemplates(...)` (ook offered in De editor's insert menu) |
| Voeg toe Markdown snippets, fence names, frontmatter keys en waarden | `SyntaxContributor` |
| Decide what De body of your own fence betekent | `CodeFenceRenderer` (declare De naam met `sink.fenceLanguages(...)`; override `renderSiteFence` so De site shows it too) |
| Voeg toe Een symbolic colour naam | `SymbolicColorResolver` (per gids) of `registerSymbolicColorResolver` |
| Voeg toe Een pagina index | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| Export your tags naar De site | `GuideSiteTagRenderer` (see [Mod Compatibility](Mod-Compatibility)) |
| Voeg toe Een scène editor toolbar button of menu item | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| Voeg toe Een resource naar De exported site | `ExportableResourceProvider` on your node |
| Report Een problem met your own syntaxis naar De reader | `LytErrorSink.appendError(compiler, text, element)`, which is De `parent` your tag compiler is given |
| Voeg toe Een button of menu entry naar De gids editor | `GuideNhIntegrationRegistry.registerEditorAction(GuideEditorActionContribution.insert(...))` of `.wrap(...)`, of `GuideEditorActionContribution.Provider` voor one gids |

Deliberately closed, met what it costs you:

- De gids editor's built-in toolbar is Een vast Stel in. Een contributed *action* (see
  `GuideEditorActionContribution`) gets its own toolbar button en menu entry na De built-ins, so what
  kan niet be added is Een wijzigen naar Een built-in action, niet Een new one.
- Diagnostics are reported at compile time through `LytErrorSink`: De error blok is appended naar De
  document, so De reader sees it in De book en on De exported site. There is no separate "warn
  terwijl typing" hook in De editor.
- De site export renders its own tags met Een built-in chain. Een registered renderer is asked first, voor
  iedere MDX element en voor your fence names, so your tags en fences export; De built-in chain itself is
  niet Een Stel in of registered renderers yet.
- scène Annotaties of your own renderen in De book but are niet serialized into De exported site's viewer.
- Een scène element of your own, registered as a `SceneElementTagCompiler`, renders in De book en reaches De
  site alleen Als its element is a `SceneAnnotation`: that is De one shape De exporter collects. Een element of
  any other shape, such as one that draws geometry of its own, is left out of De export terwijl De rest of De
  scène is exported around it. Exporting such Een element needs Een annotatie that carries it, of Een site
  renderer voor De tag that produces De markup.

Conventions that matter voor Een plugin:

- Give your `SyntaxValueKind` Een mod-namespaced id (`"MYMOD_MACHINE"`). Kinds route naar waarde sources by that
  id alone; De quoting hint belongs naar De Attribuut that declares De waarde.
- Return what De document really holds van `SyntaxSlot.match`: De editor re-checks De tekst voor it
  writes, en Een normalized form drops De commit.
- Een plugin that throws is reported met its namespace en skipped, so Een failure costs your own
  contributions rather than De editor.
