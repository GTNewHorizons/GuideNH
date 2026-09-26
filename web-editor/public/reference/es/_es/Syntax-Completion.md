# sintaxis Completion


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

GuideNH's página editor completes MDX etiquetas, sus atributos, atributo valores, markdown sintaxis, code
fence names y frontmatter claves. None of que es hardcoded: everything El editor offers comes de a
registry que su mod puede Añada a.

There son dos ways en, y you normally want ambos.

## 1. etiqueta names come de su etiqueta compilador

El editor lists El etiqueta names of cada registrado `TagCompiler` y `SceneElementTagCompiler`. Si su
mod ya registers Un etiqueta compilador, su etiquetas son offered en El editor con no extra funcionan:

A `TagCompiler` puede ser registrado para cada guíUn a través de
`GuideNhIntegrationRegistry.registerTagCompilerProvider(...)`, y a `SceneElementTagCompiler` a través de
`registerSceneElementTagCompilerProvider(...)`; ambos puede también ser añadido Un uno guíUn con
`GuideBuilder.extension(...)`. Un provider es leer cuando Un guíUn es built, so registrar it mientras su mod es
cargando rather que mientras someone es reading.

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

registrar it El mismo way you ya do:

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

o para a único guía:

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

etiquetas Un compilador accepts but página authors nunca escribir - analizador salida como `p`, `h1` o `em` - son no
offered. El library oculta su propio con `SyntaxSink.hiddenTags(...)`.

## 2. Un sintaxis contributor añade El rest

Un etiqueta compilador hace no say que atributos it reads o what those atributos take. que es what a
`SyntaxContributor` es para. It también declares markdown snippets, code fence names y frontmatter claves.

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

registrar it globally, o per guía:

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

Globally registrado contributors son applied primero, so Un guía's propio contributor puede anular ellos.

El built-en sintaxis que ships con El mod es un ordinary contributor (`BuiltinSyntaxContributor`)
registrado as Un predeterminado extension. `GuideBuilder.disableDefaultExtensions()` por tanto turns El whole
built-en sintaxis off a lo largo de con El predeterminado etiqueta compilers.

## 3. valor kinds y valor sources

Un atributo's `SyntaxValueKind` decides how El editor completes y quotes su valor:

| tipo | Completion | escrito as |
| --- | --- | --- |
| `STRING` | free texto (o El valores declared siguiente Un El atributo) | `"..."` |
| `INT`, `FLOAT` | numeric presets para well known atributo names | bare |
| `BOOLEAN` | `true`, `false` | bare |
| `ENUM` | El constants of El enum passed a `AttributeSyntax.of`, o El declared valores | `"..."` |
| `COLOR` | `#rrggbb` plus El symbolic colour names | `"..."` |
| `ITEM_ID`, `BLOCK_ID` | elemento y bloque registry names, con icons | `"..."` |
| `ORE_DICT`, `ENTITY_ID`, `KEY_BIND`, `COMMAND` | El coincidente game registry | `"..."` |
| `PAGE_PATH`, `FILE_PATH` | guíUn página ids, y files en El guíUn recursos | `"..."` |
| `MOD_ID` | mod ids taken de El elemento registry namespaces | `"..."` |
| `EXPRESSION`, `DOMAIN`, `FORMAT_PATTERN` | común expressions, intervals y patterns | `"..."` |
| `VECTOR3` | free texto | bare |
| `SNBT` | free texto | `{...}` |
| `QUEST_UUID` | free texto; quest ids son no discoverable yet | `"..."` |

Reusing Un built-en tipo significa you do no registrar anything: marking Un atributo as `ITEM_ID` es enough
Un get elemento completion para it.

Un completa de su propio registry, declare Un tipo y Un origen. Un tipo es just Un nombre, so you puede
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

`request` carries El typed texto, El enclosing etiqueta y atributo, y El frontmatter clave, so uno
origen puede serve several atributos.

## 4. Live datos

Un origen que needs datos de El open guíUn o de El document implements
`SyntaxEnvironmentAware`. `prepare` es called once per completion tick, antes de cualquier request es answered:

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide(), environment.pagePaths(), environment.documentText()
    }
}
```

Conserve `prepare` cheap: it runs on cada editor tick. Cache derived lists y solo rebuild ellos cuando El
entrada actually changed - que es what El built-en `PagePathValueSource` y `AnchorValueSource` do.

## 5. Insert templates

Completing Un etiqueta nombre escribe `<Name />`, o Un paired etiqueta con El caret dentro de El container. Un etiqueta que
needs atributos o contenido Un ser useful declares El texto it deberíUn completa as en su lugar:

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` puts El caret derecha después de El primero occurrence of El marker it es given, so Un template puede
say where typing continues sin counting characters. `InsertTemplate.of(tag, text)` escribe El texto
con El caret at su fin. El template replaces El typed etiqueta nombre, so El autor accepts it y carries
on dentro de El form que was escrito.

El mod itself declares templates para El etiquetas whose useful form es más que El etiqueta nombre, para Ejemplo
`<BlockStat item="" count="1" />`, `<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />` y Un gráfico
con su primero serie.

## 6. Slots: sintaxis of su propio

Everything encima de describes sintaxis esto mod ya parses. para sintaxis of su propio - Un directive dentro de a
bloque, Un small idioma en Un code fence, Un campo you compilar yourself - registrar a `SyntaxSlot`:

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

registrar it globally o per guía, exactamente like Un contributor:

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

What Un slot controls:

- `match` decides El range Un accepted valor replaces, El texto ya typed dentro de it y El valores
  El editor offers. devolver `null` cuando El caret es no dentro de su sintaxis. El texto you report as typed
  tiene Un ser what El document really holds en que range: El editor re-checks it antes de writing, y a
  normalized form of it would drop El commit.
- El `SyntaxValueWriter` of El coincidir escribe Un accepted valor: El texto que replaces El range, where
  El caret lands y what es seleccionado. Leaving it `null` replaces El range con El valor itself. A
  writer que throws o answers nothing es reported en El log y El editor escribe nothing.
- `selection` optionally declares El range Un double clic dentro de su sintaxis selects.

Slots son asked antes de El editor's propio resolvers, y El primero slot que coincide owns El caret even
cuando it offers no valores, so esto mod nunca guesses dentro de texto another mod claims.

## 7. What El editor ya completes

- MDX etiquetas, scoped Un El enclosing container, con container etiquetas completing as `<Name></Name>`.
- atributo names, que expand a `name=""`, `name={}` o `name={true}` according Un El valor tipo.
- atributo valores, routed by valor tipo, including recipes, elemento y bloque ids, y mod ids.
- Markdown sintaxis: headings, lists, task lists, quotes, alerts, tablas, code bloques, thematic breaks,
  math bloques, y inline emphasis, code, enlaces y imágenes. cada snippet places El caret where you
  continue typing.
- Code fence names, derived de El code bloque idioma registry plus El GuideNH específico fences.
- YAML frontmatter claves, y valores para El claves que declare Un valor tipo.
- Insert templates, so Un etiqueta whose useful form es más que su nombre es escrito completa.

Candidates son ranked so El popup pre-selects El closest coincidir: Un exact prefix primero, entonces Un coincidir on
El identifier después de El espacio de nombres, entonces Un coincidir anywhere en El nombre.

## 8. What you puede extend, y what you no puede yet

Everything on esto página es un registry, y Un registrado plugin es solo leer cuando Un guíUn es built o Un página
compilado, so registrar durante mod cargando.

| You want a | usar |
| --- | --- |
| Añada Un etiqueta, Un escena element, Un atributo, Un valor tipo y su valores | `TagCompiler`, `SceneElementTagCompiler`, `SyntaxContributor`, `SyntaxValueSource` |
| completa sintaxis of su propio (su propio slot, su propio writer, double-clic range) | `SyntaxSlot` |
| hacer Un etiqueta completa as Un whole form | `sink.insertTemplates(...)` (también offered en El editor's insert menu) |
| Añada markdown snippets, fence names, frontmatter claves y valores | `SyntaxContributor` |
| Decide what El cuerpo of su propio fence significa | `CodeFenceRenderer` (declare El nombre con `sink.fenceLanguages(...)`; anular `renderSiteFence` so El site muestra it too) |
| Añada Un symbolic colour nombre | `SymbolicColorResolver` (per guía) o `registerSymbolicColorResolver` |
| Añada Un página index | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| Export su etiquetas Un El site | `GuideSiteTagRenderer` (Consulta [Mod Compatibility](Mod-Compatibility)) |
| Añada Un escena editor toolbar button o menu elemento | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| Añada Un recurso Un El exported site | `ExportableResourceProvider` on su node |
| Report Un problem con su propio sintaxis Un El reader | `LytErrorSink.appendError(compiler, text, element)`, que es El `parent` su etiqueta compilador es given |
| Añada Un button o menu entrada Un El guíUn editor | `GuideNhIntegrationRegistry.registerEditorAction(GuideEditorActionContribution.insert(...))` o `.wrap(...)`, o `GuideEditorActionContribution.Provider` para uno guía |

Deliberately closed, con what it costs you:

- El guíUn editor's built-en toolbar es un fijo Establezca. Un contributed *action* (Consulta
  `GuideEditorActionContribution`) gets su propio toolbar button y menu entrada después de El built-ins, so what
  no puede ser añadido es un cambiar Un Un built-en action, no Un nuevo uno.
- Diagnostics son reported at compilar tiempo a través de `LytErrorSink`: El error bloque es appended Un El
  document, so El reader sees it en El book y on El exported site. There es no separate "warn
  mientras typing" hook en El editor.
- El site export renderiza su propio etiquetas con Un built-en chain. Un registrado renderer es asked primero, para
  cada MDX element y para su fence names, so su etiquetas y fences export; El built-en chain itself es
  no Un Establezca of registrado renderers yet.
- escena anotaciones of su propio renderizan en El book but son no serialized en El exported site's viewer.
- Un escena element of su propio, registrado as a `SceneElementTagCompiler`, renderiza en El book y reaches El
  site solo Si su element es un `SceneAnnotation`: que es El uno shape El exporter collects. Un element of
  cualquier otro shape, como uno que dibuja geometría of su propio, es izquierda out of El export mientras El rest of El
  escena es exported around it. Exporting such Un element needs Un anotación que carries it, o Un site
  renderer para El etiqueta que produces El markup.

Conventions que matter para Un plugin:

- Give su `SyntaxValueKind` Un mod-namespaced id (`"MYMOD_MACHINE"`). Kinds route Un valor sources by que
  id alone; El quoting hint belongs Un El atributo que declares El valor.
- devolver what El document really holds de `SyntaxSlot.match`: El editor re-checks El texto antes de it
  escribe, y Un normalized form drops El commit.
- Un plugin que throws es reported con su espacio de nombres y skipped, so Un failure costs su propio
  contributions rather que El editor.
