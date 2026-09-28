<Row gap="4">
<ItemImage id={<Param pos="1" />} /> **<Param pos="2" default="Unknown" />** — <Param pos="3" />
</Row>

<Switch test="tier">
<Case value="ulv">Voltage tier: ULV</Case>
<Case value="lv">Voltage tier: LV</Case>
<Case value="mv">Voltage tier: MV</Case>
<Case value="hv">Voltage tier: HV</Case>
<Case value="ev">Voltage tier: EV</Case>
<Case value="iv">Voltage tier: IV</Case>
<Default />
</Switch>

<IncludeOnly><If test="note"><Param name="note" /><Else /></If></IncludeOnly>

<NoInclude>

Usage:

```md
<Template name="CraftCost" tier="mv" note="Craftable in an assembler.">
  <Arg>minecraft:iron_ingot</Arg>
  <Arg>Iron Ingot</Arg>
  <Arg>2 iron ingots</Arg>
</Template>
```

The three unnamed `<Arg>` tags fill `pos` 1, 2, and 3 in order. `tier` picks a voltage line through
`<Switch>`, and an unknown tier prints nothing. `note` is emitted only when the template is transcluded,
because it is wrapped in `<IncludeOnly>`.

</NoInclude>
