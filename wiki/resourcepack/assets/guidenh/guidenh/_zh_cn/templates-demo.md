---
navigation:
  title: 模板
  parent: index.md
  position: 140
  icon: minecraft:paper
categories:
  - widgets
---

# MDX 模板

本页用于检验模板层。模板是 MDX 标签，因此调用在页面编译时就被处理，查看时不再做任何解析。
语法参考见[模板](Templates-zh-CN)。

## 具名参数与默认值

这段调用：

```md
<Template name="InfoBox">
  <Arg name="name">钢锭</Arg>
  <Arg name="icon">minecraft:iron_ingot</Arg>
</Template>
```

渲染为：

<Template name="InfoBox">
  <Arg name="name">钢锭</Arg>
  <Arg name="icon">minecraft:iron_ingot</Arg>
</Template>

全部参数省略时使用默认值：

<Template name="InfoBox" />

属性是同一件事的简写，写在一行时更易读。第一个 `name` 选择模板，因此要传入模板自己的 `name` 参数
时再写一个：

<Template name="InfoBox" name="金锭" icon="minecraft:gold_ingot" />

## 位置参数

无名的 `<Arg>` 按顺序填入位置槽位：

<Template name="CraftCost" tier="mv">
  <Arg>minecraft:iron_ingot</Arg>
  <Arg>铁锭</Arg>
  <Arg>2 个铁锭</Arg>
</Template>

`tier` 属性由模板内部的 `<Switch>` 决定，未知等级不输出任何内容：

<Template name="CraftCost" tier="zzz">
  <Arg>minecraft:gold_ingot</Arg>
  <Arg>金锭</Arg>
  <Arg>1 个金锭</Arg>
</Template>

## 算术与字符串辅助

这些标签只在模板内部生效，不会在普通页面上渲染，因此下面的例子按模板正文的写法给出。取值来自
`value`，或来自嵌套的 `<Param>`：

```md
<Expr value="2 + 3 * 4" /> 得到 14
<Expr value="10 / 4" /> 得到 2.5
<Len value="Etching Array" /> 得到 13 个字符
<Sub value="Etching Array" start="0" length="7" /> 得到 `Etching`
<Replace value="a-b" from="-" to="+" /> 得到 `a+b`
<Lower value="ABC" /> 得到 `abc`，<Upper value="abc" /> 得到 `ABC`
<Trim value=" spaced " /> 去掉首尾空格
<PadLeft value="7" width="3" pad="0" /> 得到 `007`
<UrlEncode value="a b" /> 得到 `a%20b`
```

日文与从右向左的文字同样适用，`<Len value="鋼鉄" />` 按字符计数。

## 条件判断

`<If>` 判断某个参数是否被传入，缺失时走另一分支：

```md
<If test="note">有备注<Else />没有备注</If>
```

`InfoBox` 正是用这个写法在缺少 `note` 时不输出任何内容。

`<IfEq>` 比较两个值，`<Switch>` 按值选择分支，`<IfExist>` 判断页面是否存在：

```md
<IfEq a="七" b="七">两个值相同<Else />两个值不同</IfEq>
<IfExist page="index.md">页面存在<Else />没有这个页面</IfExist>
```

`CraftCost` 用 `<Switch>` 把 `tier` 参数换成一整行电压等级：

```md
<Switch test="tier">
<Case value="lv">低压</Case>
<Case value="mv">中压</Case>
<Default>未知等级</Default>
</Switch>
```

## 嵌套转译

`CraftCost` 内部包含了 `Row` 模板，因此一次调用会编译两个模板：

<Template name="CraftCost" tier="lv">
  <Arg>minecraft:redstone</Arg>
  <Arg>红石</Arg>
  <Arg>4 个红石粉</Arg>
</Template>

## 编辑模板

模板就是 `templates/` 下的普通页面，像其他页面一样编辑；改动后使用它的页面会自动重新编译。

## 失败处理

未知模板会在调用处报错，本页其余部分照常渲染：

<Template name="NoSuchTemplateHere" />

未知模板之后的文字不受影响。
