<IncludeOnly>
<Row gap="4">
<ItemImage id={<Param name="icon" default="minecraft:book" />} /> <Param name="label" default="行" />
</Row>
</IncludeOnly>

<NoInclude>

由其他模板调用，不会直接显示。用法：

```md
<Template name="Row" icon="minecraft:book" label="行" />
```

</NoInclude>
