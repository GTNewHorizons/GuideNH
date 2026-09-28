<Row gap="4">
<ItemImage id={<Param pos="1" />} /> **<Param pos="2" default="未知" />** — <Param pos="3" />
</Row>

<Switch test="tier">
<Case value="ulv">电压等级：ULV</Case>
<Case value="lv">电压等级：LV</Case>
<Case value="mv">电压等级：MV</Case>
<Case value="hv">电压等级：HV</Case>
<Case value="ev">电压等级：EV</Case>
<Case value="iv">电压等级：IV</Case>
<Default />
</Switch>

<IncludeOnly><If test="note"><Param name="note" /><Else /></If></IncludeOnly>

<NoInclude>

用法：

```md
<Template name="CraftCost" tier="mv" note="可在组装机中制作。">
  <Arg>minecraft:iron_ingot</Arg>
  <Arg>铁锭</Arg>
  <Arg>2 个铁锭</Arg>
</Template>
```

三个无名的 `<Arg>` 依次填入 `pos` 1、2、3。`tier` 通过 `<Switch>` 选择电压等级行，未知等级不输出
内容。`note` 因为被 `<IncludeOnly>` 包裹，只在被转译时输出。

</NoInclude>
