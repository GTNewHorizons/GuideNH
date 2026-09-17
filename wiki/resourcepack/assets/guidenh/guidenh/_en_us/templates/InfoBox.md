**<Param name="name" default="Untitled" />**

<ItemImage id={<Param name="icon" default="minecraft:stone" />} />

<If test="note"><Param name="note" /><Else /></If>

<NoInclude>

Usage:

```md
<Template name="InfoBox">
  <Arg name="name">Steel Ingot</Arg>
  <Arg name="icon">minecraft:iron_ingot</Arg>
  <Arg name="note">Smelted from iron.</Arg>
</Template>
```

Every argument is optional; `name` falls back to `Untitled`, `icon` to `minecraft:stone`, and an absent
`note` prints nothing at all.

</NoInclude>
