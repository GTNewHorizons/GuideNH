# Syntax Completion

GuideNH's page editor completes MDX tags, their attributes, attribute values, markdown syntax, code
fence names and frontmatter keys. None of that is hardcoded: everything the editor offers comes from a
registry that your mod can add to.

There are two ways in, and you normally want both.

## 1. Tag names come from your tag compiler

The editor lists the tag names of every registered `TagCompiler` and `SceneElementTagCompiler`. If your
mod already registers a tag compiler, its tags are offered in the editor with no extra work:

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
            AttributeSyntax.of("id", SyntaxValueKind.TEXT),
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

## 5. What the editor already completes

- MDX tags, scoped to the enclosing container, with container tags completing as `<Name></Name>`.
- Attribute names, which expand to `name=""`, `name={}` or `name={true}` according to the value kind.
- Attribute values, routed by value kind, including recipes, item and block ids, and mod ids.
- Markdown syntax: headings, lists, task lists, quotes, alerts, tables, code blocks, thematic breaks,
  math blocks, and inline emphasis, code, links and images. Each snippet places the caret where you
  continue typing.
- Code fence names, derived from the code block language registry plus the GuideNH specific fences.
- YAML frontmatter keys, and values for the keys that declare a value kind.

Candidates are ranked so the popup pre-selects the closest match: an exact prefix first, then a match on
the identifier after the namespace, then a match anywhere in the name.
