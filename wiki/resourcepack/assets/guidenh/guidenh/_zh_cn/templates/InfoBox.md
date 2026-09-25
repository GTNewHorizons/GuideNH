**<Param name="name" default="未命名" />**

<ItemImage id={<Param name="icon" default="minecraft:stone" />} />

<If test="note"><Param name="note" /><Else /></If>

<NoInclude>

用法：

```md
<Template name="InfoBox">
  <Arg name="name">钢锭</Arg>
  <Arg name="icon">minecraft:iron_ingot</Arg>
  <Arg name="note">由铁锭烧炼而成。</Arg>
</Template>
```

所有参数都可省略；`name` 默认为 `未命名`，`icon` 默认为 `minecraft:stone`，而 `note` 缺失时
不输出任何内容。

</NoInclude>
