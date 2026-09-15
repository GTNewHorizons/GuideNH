---
navigation:
  title: Templates
  parent: index.md
  position: 140
  icon: minecraft:paper
categories:
  - widgets
---

# MDX Templates

This page exercises the template layer. Templates are MDX tags, so a call is compiled while the page is
compiled and nothing is resolved at view time. See [Templates](Templates) for the reference.

## Named Arguments With Defaults

This call:

```md
<Template name="InfoBox">
  <Arg name="name">Steel Ingot</Arg>
  <Arg name="icon">minecraft:iron_ingot</Arg>
</Template>
```

renders as:

<Template name="InfoBox">
  <Arg name="name">Steel Ingot</Arg>
  <Arg name="icon">minecraft:iron_ingot</Arg>
</Template>

Omitting every argument falls back to the defaults:

<Template name="InfoBox" />

An attribute is shorthand for the same thing, which is easier to read on one line:

<Template name="InfoBox" name="Gold Ingot" icon="minecraft:gold_ingot" />

## Positional Arguments

Unnamed `<Arg>` tags fill the positional slots in order:

<Template name="CraftCost" tier="mv">
  <Arg>minecraft:iron_ingot</Arg>
  <Arg>Iron Ingot</Arg>
  <Arg>2 iron ingots</Arg>
</Template>

The `tier` attribute is resolved by a `<Switch>` inside the template, and an unknown tier prints nothing:

<Template name="CraftCost" tier="zzz">
  <Arg>minecraft:gold_ingot</Arg>
  <Arg>Gold Ingot</Arg>
  <Arg>1 gold ingot</Arg>
</Template>

## Arithmetic And String Helpers

These tags compute while the page is compiled. A value comes from `value`, or from a nested `<Param>`:

- `<Expr value="2 + 3 * 4" />` gives <Expr value="2 + 3 * 4" />
- `<Expr value="10 / 4" />` gives <Expr value="10 / 4" />
- `<Expr value="7 &gt; 5" />` gives <Expr value="7 &gt; 5" />, because a comparison yields `1` or `0`
- `<Len value="Etching Array" />` gives <Len value="Etching Array" /> characters
- `<Sub value="Etching Array" start="0" length="7" />` gives `<Sub value="Etching Array" start="0" length="7" />`
- `<Replace value="a-b" from="-" to="+" />` gives <Replace value="a-b" from="-" to="+" />
- `<Explode value="a,b,c" delimiter="," index="1" />` gives <Explode value="a,b,c" delimiter="," index="1" />
- `<PadLeft value="7" width="3" pad="0" />` gives <PadLeft value="7" width="3" pad="0" />
- `<PadRight value="7" width="3" pad="0" />` gives <PadRight value="7" width="3" pad="0" />
- `<Lower value="ABC" />` gives <Lower value="ABC" /> and `<Upper value="abc" />` gives <Upper value="abc" />
- `<Trim value=" spaced " />` gives `<Trim value=" spaced " />`
- `<UrlEncode value="a b" />` gives <UrlEncode value="a b" />
- `<Pos value="abc" needle="c" />` gives <Pos value="abc" needle="c" />

Right-to-left scripts and Japanese names exercise the same tags:

- `<Upper value="steel ingot" /><Lower value="STEEL INGOT" /><Len value="鋼鉄" />

## Conditionals

`<If>` tests whether an argument was supplied, so a missing value takes the fallback branch:

```md
<If test="note">note is present<Else />no note was supplied</If>
```

<If test="note">note is present<Else />no note was supplied</If>

`<IfEq>` compares two values, `<Switch>` picks a branch by value, and `<IfExist>` asks whether a page
exists:

- `<IfEq a="iron" b="iron">same<Else />different</IfEq>` gives <IfEq a="iron" b="iron">same<Else />different</IfEq>
- `<IfExist page="index.md">the page exists<Else />no such page</IfExist>` gives <IfExist page="index.md">the page exists<Else />no such page</IfExist>
- `<IfExist page="no/Such/Page.md">the page exists<Else />no such page</IfExist>` gives <IfExist page="no/Such/Page.md">the page exists<Else />no such page</IfExist>

```md
<Switch test="tier">
<Case value="lv">Low voltage</Case>
<Case value="mv">Medium voltage</Case>
<Default>Unknown tier</Default>
</Switch>
```

## Include Control

`<NoInclude>` holds documentation that stays on the template's own page, `<IncludeOnly>` holds markup that
only makes sense where the template is included, and `<OnlyInclude>` narrows transclusion to its own body.
The templates in this pack use the first two; see `InfoBox` and `Row`.

`OnlyPart` is the smallest possible example of `<OnlyInclude>`: only the line inside the tag reaches a page that includes it.

## Nested Transclusion

`CraftCost` includes the `Row` template internally, so one call compiles two templates:

<Template name="CraftCost" tier="lv">
  <Arg>minecraft:redstone</Arg>
  <Arg>Redstone</Arg>
  <Arg>4 redstone dust</Arg>
</Template>

## Editing A Template

Because a template is an ordinary page under `templates/`, it is edited like any other page, and the pages
that use it are recompiled automatically when it changes.

## Failure Handling

An unknown template reports an error where the call is, and the rest of this page still renders:

<Template name="NoSuchTemplateHere" />

Text after the unknown template is unaffected.
