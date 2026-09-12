# Syntax Completion

GuideNH's page editor completes MDX tags, their attributes, attribute values, markdown syntax, code
fence names and frontmatter keys. None of that is hardcoded: everything the editor offers comes from a
registry that your mod can add to.

There are two ways in, and you normally want both.

## 1. Tag names come from your tag compiler

The editor lists the tag names of every registered `TagCompiler` and `SceneElementTagCompiler`. If your
mod already registers a tag compiler, its tags are offered in the editor with no extra work:

A `TagCompiler` can be registered for every guide through
`GuideNhIntegrationRegistry.registerTagCompilerProvider(...)`, and a `SceneElementTagCompiler` through
`registerSceneElementTagCompilerProvider(...)`; both can also be added to one guide with
`GuideBuilder.extension(...)`. A provider is read when a guide is built, so register it while your mod is
loading rather than while someone is reading.

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

Register it the same way you already do:

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

or for a single guide:

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

Tags a compiler accepts but page authors never write - parser output such as `p`, `h1` or `em` - are not
offered. The library hides its own with `SyntaxSink.hiddenTags(...)`.

## 2. A syntax contributor adds the rest

A tag compiler does not say which attributes it reads or what those attributes take. That is what a
`SyntaxContributor` is for. It also declares markdown snippets, code fence names and frontmatter keys.

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

Register it globally, or per guide:

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

Globally registered contributors are applied first, so a guide's own contributor can override them.

The built-in syntax that ships with the mod is an ordinary contributor (`BuiltinSyntaxContributor`)
registered as a default extension. `GuideBuilder.disableDefaultExtensions()` therefore turns the whole
built-in syntax off along with the default tag compilers.

## 3. Value kinds and value sources

An attribute's `SyntaxValueKind` decides how the editor completes and quotes its value:

| Kind | Completion | Written as |
| --- | --- | --- |
| `STRING` | free text (or the values declared next to the attribute) | `"..."` |
| `INT`, `FLOAT` | numeric presets for well known attribute names | bare |
| `BOOLEAN` | `true`, `false` | bare |
| `ENUM` | the constants of the enum passed to `AttributeSyntax.of`, or the declared values | `"..."` |
| `COLOR` | `#rrggbb` plus the symbolic colour names | `"..."` |
| `ITEM_ID`, `BLOCK_ID` | item and block registry names, with icons | `"..."` |
| `ORE_DICT`, `ENTITY_ID`, `KEY_BIND`, `COMMAND` | the matching game registry | `"..."` |
| `PAGE_PATH`, `FILE_PATH` | guide page ids, and files in the guide assets | `"..."` |
| `MOD_ID` | mod ids taken from the item registry namespaces | `"..."` |
| `EXPRESSION`, `DOMAIN`, `FORMAT_PATTERN` | common expressions, intervals and patterns | `"..."` |
| `VECTOR3` | free text | bare |
| `SNBT` | free text | `{...}` |
| `QUEST_UUID` | free text; quest ids are not discoverable yet | `"..."` |

Reusing a built-in kind means you do not register anything: marking an attribute as `ITEM_ID` is enough
to get item completion for it.

To complete from your own registry, declare a kind and a source. A kind is just a name, so you can
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

`request` carries the typed text, the enclosing tag and attribute, and the frontmatter key, so one
source can serve several attributes.

## 4. Live data

A source that needs data from the open guide or from the document implements
`SyntaxEnvironmentAware`. `prepare` is called once per completion tick, before any request is answered:

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide(), environment.pagePaths(), environment.documentText()
    }
}
```

Keep `prepare` cheap: it runs on every editor tick. Cache derived lists and only rebuild them when the
input actually changed - that is what the built-in `PagePathValueSource` and `AnchorValueSource` do.

## 5. Insert templates

Completing a tag name writes `<Name />`, or a paired tag with the caret inside the container. A tag that
needs attributes or content to be useful declares the text it should complete as instead:

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` puts the caret right after the first occurrence of the marker it is given, so a template can
say where typing continues without counting characters. `InsertTemplate.of(tag, text)` writes the text
with the caret at its end. The template replaces the typed tag name, so the author accepts it and carries
on inside the form that was written.

The mod itself declares templates for the tags whose useful form is more than the tag name, for example
`<BlockStat item="" count="1" />`, `<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />` and a chart
with its first series.

## 6. Slots: syntax of your own

Everything above describes syntax this mod already parses. For syntax of your own - a directive inside a
block, a small language in a code fence, a field you compile yourself - register a `SyntaxSlot`:

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

Register it globally or per guide, exactly like a contributor:

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

What a slot controls:

- `match` decides the range an accepted value replaces, the text already typed inside it and the values
  the editor offers. Return `null` when the caret is not inside your syntax. The text you report as typed
  has to be what the document really holds in that range: the editor re-checks it before writing, and a
  normalized form of it would drop the commit.
- The `SyntaxValueWriter` of the match writes an accepted value: the text that replaces the range, where
  the caret lands and what is selected. Leaving it `null` replaces the range with the value itself. A
  writer that throws or answers nothing is reported in the log and the editor writes nothing.
- `selection` optionally declares the range a double click inside your syntax selects.

Slots are asked before the editor's own resolvers, and the first slot that matches owns the caret even
when it offers no values, so this mod never guesses inside text another mod claims.

## 7. What the editor already completes

- MDX tags, scoped to the enclosing container, with container tags completing as `<Name></Name>`.
- Attribute names, which expand to `name=""`, `name={}` or `name={true}` according to the value kind.
- Attribute values, routed by value kind, including recipes, item and block ids, and mod ids.
- Markdown syntax: headings, lists, task lists, quotes, alerts, tables, code blocks, thematic breaks,
  math blocks, and inline emphasis, code, links and images. Each snippet places the caret where you
  continue typing.
- Code fence names, derived from the code block language registry plus the GuideNH specific fences.
- YAML frontmatter keys, and values for the keys that declare a value kind.
- Insert templates, so a tag whose useful form is more than its name is written complete.

Candidates are ranked so the popup pre-selects the closest match: an exact prefix first, then a match on
the identifier after the namespace, then a match anywhere in the name.

## 8. What you can extend, and what you cannot yet

Everything on this page is a registry, and a registered plugin is only read when a guide is built or a page
compiled, so register during mod loading.

| You want to | Use |
| --- | --- |
| Add a tag, a scene element, an attribute, a value kind and its values | `TagCompiler`, `SceneElementTagCompiler`, `SyntaxContributor`, `SyntaxValueSource` |
| Complete syntax of your own (your own slot, your own writer, double-click range) | `SyntaxSlot` |
| Make a tag complete as a whole form | `sink.insertTemplates(...)` (also offered in the editor's insert menu) |
| Add markdown snippets, fence names, frontmatter keys and values | `SyntaxContributor` |
| Decide what the body of your own fence means | `CodeFenceRenderer` (declare the name with `sink.fenceLanguages(...)`; override `renderSiteFence` so the site shows it too) |
| Add a symbolic colour name | `SymbolicColorResolver` (per guide) or `registerSymbolicColorResolver` |
| Add a page index | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| Export your tags to the site | `GuideSiteTagRenderer` (see [Mod Compatibility](Mod-Compatibility)) |
| Add a scene editor toolbar button or menu item | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| Add a resource to the exported site | `ExportableResourceProvider` on your node |

Deliberately closed, with what it costs you:

- The guide editor's toolbar is a fixed set: a contributed template appears in the insert menu (the
  Templates submenu) and through completion, but not as a toolbar button.
- The site export renders its own tags with a built-in chain. A registered renderer is asked first, for
  every MDX element and for your fence names, so your tags and fences export; the built-in chain itself is
  not a set of registered renderers yet.
- Scene annotations of your own render in the book but are not serialized into the exported site's viewer.

Conventions that matter for a plugin:

- Give your `SyntaxValueKind` a mod-namespaced id (`"MYMOD_MACHINE"`). Kinds route to value sources by that
  id alone; the quoting hint belongs to the attribute that declares the value.
- Return what the document really holds from `SyntaxSlot.match`: the editor re-checks the text before it
  writes, and a normalized form drops the commit.
- A plugin that throws is reported with its namespace and skipped, so a failure costs your own
  contributions rather than the editor.

