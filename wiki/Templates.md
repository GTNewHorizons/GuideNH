# Templates

GuideNH supports templates. A template is an ordinary guide page stored under a reserved `templates/`
folder, and a page transcludes it with the `<Template>` MDX tag:

```md
<Template name="InfoBox" name="Steel" icon="minecraft:iron_ingot" />
```

Transclusion happens **while the page is compiled**, not while it is viewed. A template's body is read
from its page, its parameters are substituted, and the result is compiled by the ordinary tag and
Markdown compilers. Nothing about templates is resolved at view time.

Because templates are pages, they inherit everything pages already have: per-language folders, the
`.lang` whole-page override, resource-pack priority, and live reload in development.

## Where Templates Live

```text
assets/<namespace>/<guide folder>/
|-- _en_us/
|   |-- index.md
|   `-- templates/
|       |-- InfoBox.md
|       `-- nav/
|           `-- Row.md
`-- _zh_cn/
    |-- index.md
    `-- templates/
        `-- InfoBox.md
```

`templates/` is a reserved prefix. Template pages stay out of `Special:AllPages`, category listings,
translation statistics, and the navigation tree, so readers are never offered a template as content.
Turning on the in-game guide editor makes them browsable and searchable so you can edit them where you
found them.

### Naming

A template's name is its path below `templates/` without the `.md` extension, so `templates/nav/Row.md`
is reached as `name="nav/Row"`.

Names ignore the case of the first character and treat `_` as a space, matching MediaWiki:

- `name="infobox"` and `name="InfoBox"` are the same template
- `name="Info_Box"` and `name="Info Box"` are the same template
- `name="InfoBox"` and `name="infobox"` are **not** the same template

## Calling A Template

A named argument is an attribute on the call:

```md
<Template name="InfoBox" name="Steel" icon="minecraft:iron_ingot" />
```

`name` selects the template, so every other attribute is an argument. Longer values are easier to read
as `<Arg>` children:

```md
<Template name="InfoBox">
  <Arg name="name">Steel Ingot</Arg>
  <Arg name="icon">minecraft:iron_ingot</Arg>
</Template>
```

An `<Arg>` without a name fills the next positional slot, numbered from 1 in the order written:

```md
<Template name="CraftCost" tier="mv">
  <Arg>minecraft:iron_ingot</Arg>
  <Arg>Iron Ingot</Arg>
  <Arg>2 iron ingots</Arg>
</Template>
```

## Parameters

Inside a template, `<Param>` reads an argument.

| Syntax | Meaning |
| --- | --- |
| `<Param pos="1" />` | First positional argument |
| `<Param name="icon" />` | Named argument `icon` |
| `<Param name="icon" default="minecraft:stone" />` | Named argument, or the default when it is absent |
| `<Param name="note">no note</Param>` | The body is used as the default |

Read a parameter inside an attribute with braces, which the parser hands to the tag as an expression:

```md
<ItemImage id={<Param name="icon" default="minecraft:stone" />} />
```

An argument that is not supplied and has no default is reported in the log and contributes nothing, so a
typo is visible in the log rather than silently rendering the wrong thing.

Named and positional arguments share one key space: a template may declare `<Param name="tier" />` and a
call may still pass it positionally.

## Conditionals

`<If>` tests whether an argument was supplied, which is the usual way to make part of a template
optional.

| Tag | Behaviour |
| --- | --- |
| `<If test="note">…</If>` | True when the argument `note` was supplied and is not empty |
| `<If test="note">…<Else />…</If>` | Selects the second branch when the test is false |
| `<If test="note">…<Else>fallback</Else></If>` | The `Else` body may hold the fallback inline |
| `<IfEq a="tier" b="mv">…<Else />…</IfEq>` | Compares two values, numerically when both are numbers |
| `<IfExist page="Other/Page.md">…<Else />…</IfExist>` | Tests whether a page exists |
| `<Switch test="tier">…</Switch>` | Selects a `<Case>` by value, or `<Default>` |

Only the branch that is taken is compiled, so a template may put a whole section of markup behind a
condition:

```md
<If test="note"><Param name="note" /><Else /></If>
```

```md
<Switch test="tier">
<Case value="lv">Low voltage</Case>
<Case value="mv">Medium voltage</Case>
<Default>Unknown tier</Default>
</Switch>
```

## Values

These tags compute a value while the page is compiled and emit it as text. Each takes its value from
`value`, or from a nested `<Param>`:

```md
<Len value="Etching Array" /> gives <Len value="Etching Array" />
<Len><Param name="name" /></Len> also works
```

| Tag | Attributes | Result |
| --- | --- | --- |
| `<Expr value="2 + 3 * 4" />` | `value` | Arithmetic with the usual precedence; one comparison gives `1` or `0` |
| `<Len value="abc" />` | `value` | Number of characters |
| `<Sub value="abcdef" start="1" length="3" />` | `value`, `start`, `length` | A slice; `start` may be negative, `length` is optional |
| `<Replace value="a-b" from="-" to="+" />` | `value`, `from`, `to` | Substitution |
| `<Explode value="a,b" delimiter="," index="1" />` | `value`, `delimiter`, `index` | One field; a negative `index` counts from the end |
| `<PadLeft value="7" width="3" pad="0" />` | `value`, `width`, `pad` | Pads on the left, so this gives `007` |
| `<PadRight value="7" width="3" pad="0" />` | `value`, `width`, `pad` | Pads on the right, so this gives `700` |
| `<Lower value="ABC" />` | `value` | Lower case |
| `<Upper value="abc" />` | `value` | Upper case |
| `<Trim value=" a " />` | `value` | Surrounding whitespace removed |
| `<UrlEncode value="a b" />` | `value` | Percent-encoding, so a space becomes `%20` |
| `<Pos value="abc" needle="c" />` | `value`, `needle` | Index of the first occurrence, or `-1` |

`<Sub>` and `<Explode>` index from `0`, matching `<Pos>`.

## Include Control

Three tags decide what a template shows when it is transcluded versus when its own page is viewed.

| Tag | Transcluded | Viewed directly |
| --- | --- | --- |
| `<NoInclude>…</NoInclude>` | dropped | shown |
| `<IncludeOnly>…</IncludeOnly>` | shown | dropped |
| `<OnlyInclude>…</OnlyInclude>` | shown, and everything outside every `OnlyInclude` is dropped | dropped |

`<NoInclude>` is how a template documents its own usage, and `<IncludeOnly>` is how a template carries
markup that only makes sense where it is included.

A control tag has to stand on its own line. Written inline in the middle of a sentence it behaves like any
MDX tag and takes the rest of that text as its body.

```md
<NoInclude>

Usage: `<Template name="InfoBox" name="Steel" />`

</NoInclude>
```

## Nested Templates

A template body may call another template, and a wrapper can forward an argument it was given:

```md
<Template name="Inner"><Arg name="v"><Param name="v" /></Arg></Template>
```

The name is resolved while the outer template is being expanded, so one call can expand several
templates.

## Code And Literal Text

Braces and tags inside a fenced code block, an inline code span, or a `<pre>`/`<Code>` body are left
alone. This is how you document template syntax on a page without expanding it:

````md
```md
<Template name="InfoBox" name="Steel" />
```
````

The same applies inside `<Mermaid>` and `<FileTree>` bodies, whose content is not markdown.

## Template Pages Are Pages

A template file is an ordinary page, so two things about it matter.

Its **frontmatter is not transcluded**. A leading `---` block is stripped before the body is used, so
navigation keys written on a template page never leak into the pages that call it.

Its **content is compiled where it is included**. A tag in a template body is compiled exactly as if you
had written it at the call site, so a template may emit `<ItemImage>`, `<GameScene>`, headings, lists,
tables, or any other tag.

## Editing Templates

Editing a template page in game rebuilds the pages that use it, without reloading the whole guide. The
dependency is recorded while compiling, so it also follows a chain: if `CraftCost` includes `Row`,
editing `Row` rebuilds the pages that call `CraftCost`.

## Limits

Expansion is bounded so a mistake cannot hang the client.

| Limit | Value |
| --- | --- |
| Nesting depth | 32 |
| Inclusions per page | 4096 |

A template that includes itself is detected and reported instead of looping, and a failed call reports
an error where the call is written while the rest of the page still renders. Problems are written to the
`GuideNH-MediaWikiTemplate` logger.

## See Also

- [Guide Page Format](Guide-Page-Format) for the page format itself
- [Tags Reference](Tags-Reference) for the tags a template may emit
- [Localization](Localization) for how a template is translated
