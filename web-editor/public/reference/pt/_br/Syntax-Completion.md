# Completar sintaxe

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


GuideNH's página editor completes MDX tags, seus atributos, Atributo valores, Markdown sintaxe, code
fence names e frontmatter chaves. None of que é hardcoded: everything O editor offers comes de a
registry que seu mod pode Adicione para.

There são dois ways em, e you normally want ambos.

## 1. tag names come de seu tag compilador

O editor lists O tag names of cada registrado `TagCompiler` e `SceneElementTagCompiler`. Se seu
mod já registers Uma tag compilador, seu tags são offered em O editor com não extra funcionam:

A `TagCompiler` pode ser registrado para cada guia por
`GuideNhIntegrationRegistry.registerTagCompilerProvider(...)`, e a `SceneElementTagCompiler` por
`registerSceneElementTagCompilerProvider(...)`; ambos pode também ser adicionado para um guia com
`GuideBuilder.extension(...)`. Uma provider é ler quando Uma guia é built, so registrar it enquanto seu mod é
carregando rather que enquanto someone é reading.

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

registrar it O mesmo way you já do:

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

ou para a único guia:

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

tags Uma compilador accepts but página authors nunca escrever - analisador saída como `p`, `h1` ou `em` - são não
offered. O library oculta seu próprio com `SyntaxSink.hiddenTags(...)`.

## 2. Uma sintaxe contributor adds O rest

Uma tag compilador não faz say que atributos it reads ou what those atributos take. que é what a
`SyntaxContributor` é para. It também declares Markdown snippets, code fence names e frontmatter chaves.

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

registrar it globally, ou per guia:

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

Globally registrado contributors são applied primeiro, so Uma guia's próprio contributor pode substituir eles.

O built-em sintaxe que ships com O mod é Um ordinary contributor (`BuiltinSyntaxContributor`)
registrado as Uma Padrão extension. `GuideBuilder.disableDefaultExtensions()` portanto turns O whole
built-em sintaxe off along com O Padrão tag compilers.

## 3. valor kinds e valor sources

Um Atributo's `SyntaxValueKind` decides how O editor completes e quotes seu valor:

| Kind | Completion | Written as |
| --- | --- | --- |
| `STRING` | free texto (ou O valores declared próximo para O Atributo) | `"..."` |
| `INT`, `FLOAT` | numeric presets para well known Atributo names | bare |
| `BOOLEAN` | `true`, `false` | bare |
| `ENUM` | O constants of O enum passed para `AttributeSyntax.of`, ou O declared valores | `"..."` |
| `COLOR` | `#rrggbb` plus O symbolic colour names | `"..."` |
| `ITEM_ID`, `BLOCK_ID` | item e bloco registry names, com icons | `"..."` |
| `ORE_DICT`, `ENTITY_ID`, `KEY_BIND`, `COMMAND` | O correspondente game registry | `"..."` |
| `PAGE_PATH`, `FILE_PATH` | página do guia ids, e files em O guia assets | `"..."` |
| `MOD_ID` | mod ids taken de O item registry namespaces | `"..."` |
| `EXPRESSION`, `DOMAIN`, `FORMAT_PATTERN` | comum expressions, intervals e patterns | `"..."` |
| `VECTOR3` | free texto | bare |
| `SNBT` | free texto | `{...}` |
| `QUEST_UUID` | free texto; quest ids são não discoverable yet | `"..."` |

Reusing Uma built-em kind significa you do não registrar anything: marking Um Atributo as `ITEM_ID` é enough
para get item completion para it.

para completo de seu próprio registry, declare Uma kind e Uma origem. Uma kind é just Uma nome, so you pode
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

`request` carries O typed texto, O enclosing tag e Atributo, e O frontmatter chave, so um
origem pode serve several atributos.

## 4. Live dados

Uma origem que needs dados de O open guia ou de O document implements
`SyntaxEnvironmentAware`. `prepare` é called once per completion tick, antes de qualquer request é answered:

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide(), environment.pagePaths(), environment.documentText()
    }
}
```

Mantenha `prepare` cheap: it runs on cada editor tick. Cache derived lists e somente rebuild eles quando O
entrada actually changed - que é what O built-em `PagePathValueSource` e `AnchorValueSource` do.

## 5. Insert templates

Completing Uma tag nome writes `<Name />`, ou Uma paired tag com O caret dentro de O container. Uma tag que
needs atributos ou conteúdo para ser useful declares O texto it deve completo as em vez disso:

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` puts O caret direita depois de O primeiro occurrence of O marker it é given, so Uma template pode
say where typing continues sem counting characters. `InsertTemplate.of(tag, text)` writes O texto
com O caret at seu fim. O template replaces O typed tag nome, so O author accepts it e carries
on dentro de O form que was written.

O mod itself declares templates para O tags whose useful form é mais que O tag nome, para Exemplo
`<BlockStat item="" count="1" />`, `<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />` e Uma gráfico
com seu primeiro série.

## 6. Slots: sintaxe of seu próprio

Everything acima de describes sintaxe Este mod já parses. para sintaxe of seu próprio - Uma directive dentro de a
bloco, Uma small idioma em Uma code fence, Uma campo you compilar yourself - registrar a `SyntaxSlot`:

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

registrar it globally ou per guia, exatamente like Uma contributor:

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

What Uma slot controls:

- `match` decides O range Um accepted valor replaces, O texto já typed dentro de it e O valores
  O editor offers. retornar `null` quando O caret é não dentro de seu sintaxe. O texto you report as typed
  tem para ser what O document really holds em que range: O editor re-checks it antes de writing, e a
  normalized form of it would drop O commit.
- O `SyntaxValueWriter` of O corresponder writes Um accepted valor: O texto que replaces O range, where
  O caret lands e what é selecionado. Leaving it `null` replaces O range com O valor itself. A
  writer que throws ou answers nothing é reported em O log e O editor writes nothing.
- `selection` optionally declares O range Uma double clicar dentro de seu sintaxe selects.

Slots são asked antes de O editor's próprio resolvers, e O primeiro slot que corresponde owns O caret even
quando it offers não valores, so Este mod nunca guesses dentro de texto another mod claims.

## 7. What O editor já completes

- MDX tags, scoped para O enclosing container, com container tags completing as `<Name></Name>`.
- Atributo names, que expand para `name=""`, `name={}` ou `name={true}` according para O valor kind.
- Atributo valores, routed by valor kind, including Receitas, item e bloco ids, e mod ids.
- Markdown sintaxe: headings, lists, task lists, quotes, alerts, tabelas, blocos de código, thematic breaks,
  math blocos, e inline emphasis, code, links e imagens. cada snippet places O caret where you
  continue typing.
- Code fence names, derived de O bloco de código idioma registry plus O GuideNH específico fences.
- YAML frontmatter chaves, e valores para O chaves que declare Uma valor kind.
- Insert templates, so Uma tag whose useful form é mais que seu nome é written completo.

Candidates são ranked so O popup pre-selects O closest corresponder: Um exact prefix primeiro, então Uma corresponder on
O identifier depois de O namespace, então Uma corresponder anywhere em O nome.

## 8. What you pode extend, e what you não pode yet

Everything on Este página é Uma registry, e Uma registrado plugin é somente ler quando Uma guia é built ou Uma página
compilado, so registrar durante mod carregando.

| You want para | usar |
| --- | --- |
| Adicione Uma tag, Uma cena element, Um Atributo, Uma valor kind e seu valores | `TagCompiler`, `SceneElementTagCompiler`, `SyntaxContributor`, `SyntaxValueSource` |
| completo sintaxe of seu próprio (seu próprio slot, seu próprio writer, double-clicar range) | `SyntaxSlot` |
| fazer Uma tag completo as Uma whole form | `sink.insertTemplates(...)` (também offered em O editor's insert menu) |
| Adicione Markdown snippets, fence names, frontmatter chaves e valores | `SyntaxContributor` |
| Decide what O corpo of seu próprio fence significa | `CodeFenceRenderer` (declare O nome com `sink.fenceLanguages(...)`; substituir `renderSiteFence` so O site mostra it too) |
| Adicione Uma symbolic colour nome | `SymbolicColorResolver` (per guia) ou `registerSymbolicColorResolver` |
| Adicione Uma página index | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| Export seu tags para O site | `GuideSiteTagRenderer` (Consulte [Mod Compatibility](Mod-Compatibility)) |
| Adicione Uma cena editor toolbar button ou menu item | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| Adicione Uma recurso para O exported site | `ExportableResourceProvider` on seu node |
| Report Uma problem com seu próprio sintaxe para O reader | `LytErrorSink.appendError(compiler, text, element)`, que é O `parent` seu tag compilador é given |
| Adicione Uma button ou menu entrada para O guia editor | `GuideNhIntegrationRegistry.registerEditorAction(GuideEditorActionContribution.insert(...))` ou `.wrap(...)`, ou `GuideEditorActionContribution.Provider` para um guia |

Deliberately closed, com what it costs you:

- O guia editor's built-em toolbar é Uma fixo Defina. Uma contributed *action* (Consulte
  `GuideEditorActionContribution`) gets seu próprio toolbar button e menu entrada depois de O built-ins, so what
  não pode ser adicionado é Uma alterar para Uma built-em action, não Uma new um.
- Diagnostics são reported at compilar time por `LytErrorSink`: O erro bloco é appended para O
  document, so O reader sees it em O book e on O exported site. There é não separate "warn
  enquanto typing" hook em O editor.
- O site export renders seu próprio tags com Uma built-em chain. Uma registrado renderer é asked primeiro, para
  cada MDX element e para seu fence names, so seu tags e fences export; O built-em chain itself é
  não Uma Defina of registrado renderers yet.
- cena Anotações of seu próprio renderizar em O book but são não serialized em O exported site's viewer.
- Uma cena element of seu próprio, registrado as a `SceneElementTagCompiler`, renders em O book e reaches O
  site somente Se seu element é a `SceneAnnotation`: que é O um shape O exporter collects. Um element of
  qualquer outro shape, como um que desenha geometria of seu próprio, é esquerda out of O export enquanto O rest of O
  cena é exported around it. Exporting such Um element needs Um anotação que carries it, ou Uma site
  renderer para O tag que produces O markup.

Conventions que matter para Uma plugin:

- Give seu `SyntaxValueKind` Uma mod-namespaced id (`"MYMOD_MACHINE"`). Kinds route para valor sources by que
  id alone; O quoting hint belongs para O Atributo que declares O valor.
- retornar what O document really holds de `SyntaxSlot.match`: O editor re-checks O texto antes de it
  writes, e Uma normalized form drops O commit.
- Uma plugin que throws é reported com seu namespace e skipped, so Uma failure costs seu próprio
  contributions rather que O editor.
