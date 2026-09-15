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

属性是同一件事的简写，写在一行时更易读：

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

这些标签在页面编译时完成计算：

- `<Expr value="2 + 3 * 4" />` 得到 <Expr value="2 + 3 * 4" />
- `<Expr value="10 / 4" />` 得到 <Expr value="10 / 4" />
- `<IfEq a="七" b="七">两个值相同<Else />两个值不同</IfEq>` 得到 <IfEq a="七" b="七">两个值相同<Else />两个值不同</IfEq>
- `<Len value="Etching Array" />` 得到 <Len value="Etching Array" /> 个字符
- `<Sub value="Etching Array" start="0" length="7" />` 得到 `<Sub value="Etching Array" start="0" length="7" />`

## 条件判断

`<If>` 判断某个参数是否被传入，缺失时走另一分支：

```md
<If test="note">有备注<Else />没有备注</If>
```

<If test="note">有备注<Else />没有备注</If>

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
