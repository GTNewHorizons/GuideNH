# 模板

GuideNH 支持模板。模板就是存放在保留目录 `templates/` 下的普通指南页面，页面通过 `<Template>` 这个
MDX 标签来转译它：

```md
<Template name="InfoBox" name="Steel" icon="minecraft:iron_ingot" />
```

转译发生在**页面编译期**，而不是查看时。编译器会读取模板页的正文、代入参数，然后交给普通的标签与
Markdown 编译器处理。模板在查看阶段不做任何解析。

由于模板本身就是页面，它天然继承页面已有的一切能力：按语言分目录、`.lang` 整页覆盖、资源包优先级，
以及开发环境下的热更新。

## 模板存放位置

```text
assets/<命名空间>/<指南目录>/
|-- _en_us/
|   |-- index.md
|   `-- templates/
|       |-- InfoBox.md
|       `-- nav/
|           `-- Row.md
`-- _zh_cn/
|   |-- index.md
|   `-- templates/
|       `-- InfoBox.md
```

`templates/` 是保留前缀。模板页面不会出现在 `Special:AllPages`、分类列表、翻译统计和导航树中，
因此读者不会把它们当成正文内容。打开游戏内指南编辑器后，它们会出现在列表与搜索中，方便就地编辑。

### 命名

模板名是 `templates/` 之下的路径去掉 `.md` 后缀，所以 `templates/nav/Row.md` 通过
`name="nav/Row"` 调用。

名称忽略首字母大小写，并把 `_` 视为空格，与 MediaWiki 一致：

- `name="infobox"` 与 `name="InfoBox"` 是同一个模板
- `name="Info_Box"` 与 `name="Info Box"` 是同一个模板
- `name="InfoBox"` 与 `name="infobox"` **不是**同一个模板

## 调用模板

具名参数就是调用上的属性：

```md
<Template name="InfoBox" name="Steel" icon="minecraft:iron_ingot" />
```

`name` 用于指定模板，因此其余属性都是参数。值较长时写成 `<Arg>` 子标签更易读：

```md
<Template name="InfoBox">
  <Arg name="name">钢锭</Arg>
  <Arg name="icon">minecraft:iron_ingot</Arg>
</Template>
```

不带 `name` 的 `<Arg>` 依次填入位置参数，从 1 开始按书写顺序编号：

```md
<Template name="CraftCost" tier="mv">
  <Arg>minecraft:iron_ingot</Arg>
  <Arg>铁锭</Arg>
  <Arg>2 个铁锭</Arg>
</Template>
```

## 参数

在模板内部，用 `<Param>` 读取参数。

| 写法 | 含义 |
| --- | --- |
| `<Param pos="1" />` | 第一个位置参数 |
| `<Param name="icon" />` | 具名参数 `icon` |
| `<Param name="icon" default="minecraft:stone" />` | 具名参数，缺省时取默认值 |
| `<Param name="note">无备注</Param>` | 子标签内容作为默认值 |

在属性里读取参数要加花括号，解析器会把这段作为表达式交给标签：

```md
<ItemImage id={<Param name="icon" default="minecraft:stone" />} />
```

未传入且没有默认值的参数会记入日志，并且不产生任何内容，因此笔误会在日志里暴露，而不是悄悄渲染出
错误结果。

具名参数与位置参数共用一个键空间：模板可以声明 `<Param name="tier" />`，而调用方仍可用位置方式传入。

## 条件判断

`<If>` 判断某个参数是否被传入，这是让模板局部可选的最常用写法。

| 标签 | 行为 |
| --- | --- |
| `<If test="note">…</If>` | 参数 `note` 已传入且非空时为真 |
| `<If test="note">…<Else />…</If>` | 判断为假时走第二个分支 |
| `<If test="note">…<Else>兜底</Else></If>` | `Else` 的子标签也可直接承载兜底内容 |
| `<IfEq a="tier" b="mv">…<Else />…</IfEq>` | 比较两个值，两边都是数字时按数值比较 |
| `<IfExist page="Other/Page.md">…<Else />…</IfExist>` | 判断页面是否存在 |
| `<Switch test="tier">…</Switch>` | 按值选择 `<Case>`，否则用 `<Default>` |

只有被选中的分支才会被编译，因此模板可以把整段标记放进条件里：

```md
<If test="note"><Param name="note" /><Else /></If>
```

```md
<Switch test="tier">
<Case value="lv">低压</Case>
<Case value="mv">中压</Case>
<Default>未知等级</Default>
</Switch>
```

## 取值

这些标签在页面编译时完成计算，并以文本形式输出结果。取值来自 `value` 属性，也可以写成内嵌的
`<Param>`：

```md
<Len value="Etching Array" /> 得到 <Len value="Etching Array" />
<Len><Param name="name" /></Len> 也可以
```

| 标签 | 属性 | 结果 |
| --- | --- | --- |
| `<Expr value="2 + 3 * 4" />` | `value` | 按常规优先级计算，单个比较式返回 `1` 或 `0` |
| `<Len value="abc" />` | `value` | 字符数 |
| `<Sub value="abcdef" start="1" length="3" />` | `value`、`start`、`length` | 切片，`start` 可为负数，`length` 可省略 |
| `<Replace value="a-b" from="-" to="+" />` | `value`、`from`、`to` | 替换 |
| `<Explode value="a,b" delimiter="," index="1" />` | `value`、`delimiter`、`index` | 取其中一项，`index` 为负数时从末尾数起 |
| `<PadLeft value="7" width="3" pad="0" />` | `value`、`width`、`pad` | 左侧补位，此处得到 `007` |
| `<PadRight value="7" width="3" pad="0" />` | `value`、`width`、`pad` | 右侧补位，此处得到 `700` |
| `<Lower value="ABC" />` | `value` | 转为小写 |
| `<Upper value="abc" />` | `value` | 转为大写 |
| `<Trim value=" a " />` | `value` | 去掉首尾空白 |
| `<UrlEncode value="a b" />` | `value` | 百分号编码，空格变成 `%20` |
| `<Pos value="abc" needle="c" />` | `value`、`needle` | 首次出现的位置，找不到为 `-1` |

`<Sub>` 与 `<Explode>` 的索引都从 `0` 开始，与 `<Pos>` 一致。

## 包含控制

三个标签决定模板在被转译时与直接查看自身页面时分别显示什么。

| 标签 | 被转译时 | 直接查看时 |
| --- | --- | --- |
| `<NoInclude>…</NoInclude>` | 丢弃 | 显示 |
| `<IncludeOnly>…</IncludeOnly>` | 显示 | 丢弃 |
| `<OnlyInclude>…</OnlyInclude>` | 显示，且所有 `OnlyInclude` 之外的内容都会被丢弃 | 丢弃 |

`<NoInclude>` 用来写模板自己的用法说明，`<IncludeOnly>` 用来承载只在被包含处才有意义的标记。

包含控制标签必须独占一行。写在句子中间时，它会像任何 MDX 标签那样把后面的文字当作自己的内容。

```md
<NoInclude>

用法：`<Template name="InfoBox" name="Steel" />`

</NoInclude>
```

## 嵌套模板

模板正文里可以调用另一个模板，外层模板还能把自己收到的参数转发下去：

```md
<Template name="Inner"><Arg name="v"><Param name="v" /></Arg></Template>
```

内层模板在外层展开过程中解析，因此一次调用可以展开多个模板。

## 代码与字面文本

围栏代码块、行内代码，以及 `<pre>`/`<Code>` 内容体中的花括号与标签不会被展开。这正是「在页面上讲解
模板语法而不触发展开」的办法：

````md
```md
<Template name="InfoBox" name="Steel" />
```
````

`<Mermaid>` 与 `<FileTree>` 的内容体同理，因为它们的内容不是 markdown。

## 模板页就是页面

模板文件是普通页面，因此有两点需要注意。

**前言不会被转译**。使用正文之前会先剥掉开头的 `---` 块，所以写在模板页上的导航键不会泄漏到
调用它的页面里。

**内容在被包含处编译**。模板正文中的标签会像你直接写在调用处一样被编译，因此模板可以输出
`<ItemImage>`、`<GameScene>`、标题、列表、表格，或任何其他标签。

## 编辑模板

在游戏内编辑模板页会重建使用它的页面，无需重载整个指南。这个依赖关系在编译时被记录，并且会沿着链条
传递：如果 `CraftCost` 包含了 `Row`，那么编辑 `Row` 会重建调用 `CraftCost` 的页面。

## 上限

展开设有上限，避免一处笔误把客户端卡死。

| 上限 | 数值 |
| --- | --- |
| 嵌套深度 | 32 |
| 每页包含次数 | 4096 |

模板包含自身会被识别并上报，而不会陷入循环；失败的调用会在调用处报错，同时页面其余部分照常渲染。
问题会写入 `GuideNH-MediaWikiTemplate` 日志。

## 另请参阅

- [指南页面格式](Guide-Page-Format-zh-CN)：页面格式本身
- [标签参考](Tags-Reference-zh-CN)：模板可以输出的标签
- [本地化](Localization-zh-CN)：模板如何翻译
