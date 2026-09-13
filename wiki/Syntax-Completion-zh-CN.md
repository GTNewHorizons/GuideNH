# 语法智能补全

GuideNH 的页面编辑器会补全 MDX 标签、标签属性、属性取值、Markdown 语法、代码围栏语言与
frontmatter 键。这些**全部来自注册表**，没有任何硬编码：你的模组可以往里注册。

入口有两条，通常两条都会用到。

## 1. 标签名来自你的标签解析器

编辑器会列出所有已注册 `TagCompiler` 与 `SceneElementTagCompiler` 的 `getTagNames()`。如果你的模组
已经注册了标签解析器，它的标签会自动出现在补全里，不需要额外写任何补全代码：

`TagCompiler` 可以通过 `GuideNhIntegrationRegistry.registerTagCompilerProvider(...)` 对所有指南注册，
`SceneElementTagCompiler` 则通过 `registerSceneElementTagCompilerProvider(...)`；两者也可以用
`GuideBuilder.extension(...)` 只加到某个指南上。provider 只在构建指南时被读取，因此请在模组加载阶段注册。

```java
public class MyModMachineTagCompiler implements TagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("MyMachine");
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        // ...
    }
}
```

按原有方式注册即可：

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

或只对某个指南生效：

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

解析器接受但作者从不手写的标签（解析器产出的 `p`、`h1`、`em` 等）不会出现在补全里。本模组自己的这类
标签通过 `SyntaxSink.hiddenTags(...)` 隐藏。

## 2. 用语法贡献者（Contributor）补上其余信息

标签解析器不表达"它读哪些属性"以及"这些属性接受什么值"。这正是 `SyntaxContributor` 的职责，它同时
负责声明 Markdown 片段、代码围栏语言与 frontmatter 键。

```java
public class MyModSyntaxContributor implements SyntaxContributor {

    @Override
    public String namespace() {
        return "mymod";
    }

    @Override
    public void contribute(SyntaxSink sink) {
        // 属性。取值类型决定由哪个取值源来补全。
        sink.attributes(
            "MyMachine",
            AttributeSyntax.of("id", SyntaxValueKind.STRING),
            AttributeSyntax.of("machine", SyntaxValueKind.of("MYMOD_MACHINE")),  // 你自己的类型
            AttributeSyntax.of("tier", SyntaxValueKind.INT),
            AttributeSyntax.of("formed", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("mode", SyntaxValueKind.ENUM, "input", "output", "both"));

        // 会包裹内容的标签补全为 <Name></Name>，光标落在中间。
        sink.containerTags("MyPanel");
        sink.children("MyPanel", "MyMachine", "Tooltip");

        // 补全时从你自己的注册表取值的取值源。
        sink.valueSource(new MachineIdValueSource());

        // 额外的 Markdown 片段与围栏语言。
        sink.markdown(MarkdownSnippet.block("::", "模组提示", ":: note\n", 3));
        sink.fenceLanguages("mymod-diagram");

        // frontmatter 键：固定取值，或声明取值类型。
        sink.frontmatterKeys("mymod_machine");
        sink.frontmatterKind("mymod_machine", SyntaxValueKind.of("MYMOD_MACHINE"));
    }
}
```

全局注册，或只对单个指南注册：

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

全局注册的贡献者先应用，因此指南自己的贡献者可以覆盖它们。

本模组自带的内置语法就是一个普通贡献者（`BuiltinSyntaxContributor`），以默认扩展的方式注册。因此
`GuideBuilder.disableDefaultExtensions()` 会连同默认标签解析器一起把内置语法关掉。

## 3. 取值类型与取值源

属性的 `SyntaxValueKind` 决定编辑器如何补全、以及如何书写它的值：

| 类型 | 补全来源 | 书写形式 |
| --- | --- | --- |
| `STRING` | 自由文本（或属性旁边声明的固定候选） | `"..."` |
| `INT`、`FLOAT` | 常见属性名的数值预设 | 裸值 |
| `BOOLEAN` | `true`、`false` | 裸值 |
| `ENUM` | 传给 `AttributeSyntax.of` 的枚举常量，或声明的固定候选 | `"..."` |
| `COLOR` | `#rrggbb` 以及符号颜色名 | `"..."` |
| `ITEM_ID`、`BLOCK_ID` | 物品/方块注册名，带图标 | `"..."` |
| `ORE_DICT`、`ENTITY_ID`、`KEY_BIND`、`COMMAND` | 对应的游戏注册表 | `"..."` |
| `PAGE_PATH`、`FILE_PATH` | 指南页面 id、指南资源目录中的文件 | `"..."` |
| `MOD_ID` | 取自物品注册表命名空间的模组 id | `"..."` |
| `EXPRESSION`、`DOMAIN`、`FORMAT_PATTERN` | 常用表达式、区间与格式模板 | `"..."` |
| `VECTOR3` | 自由文本 | 裸值 |
| `SNBT` | 自由文本 | `{...}` |
| `QUEST_UUID` | 自由文本；任务 id 目前无法枚举 | `"..."` |

复用内置类型不需要注册任何东西：把属性标成 `ITEM_ID`，它就有物品补全。

要从你自己的注册表补全，就声明一个类型和一个取值源。类型只是一个标识，按你的习惯声明即可：

```java
public final class MyModValueKinds {

    public static final SyntaxValueKind MACHINE = SyntaxValueKind.of("MYMOD_MACHINE");
}
```

```java
public class MachineIdValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(MyModValueKinds.MACHINE);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (MyMachine machine : MyMachineRegistry.all()) {
            if (results.size() >= limit) {
                break;
            }
            if (machine.id()
                .startsWith(request.partialText())) {
                results.add(SyntaxSuggestion.withValue(machine.id(), machine.id(), machine.displayName()));
            }
        }
        return results;
    }
}
```

`request` 携带已输入文本、外层标签与属性名、以及 frontmatter 键，因此一个取值源可以服务多个属性。

`SyntaxValueKind` 按值比较：两处独立声明同一个类型会路由到同一批取值源，所以类型不需要集中定义，
也不需要依赖本模组提供的任何枚举类。

## 4. 实时数据

需要读取当前指南或文档内容的取值源实现 `SyntaxEnvironmentAware`。`prepare` 在每次补全 tick 中、任何
请求被应答之前调用一次：

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide()、environment.pagePaths()、environment.documentText()
    }
}
```

`prepare` 要写得足够轻：它每个编辑器 tick 都会执行。把派生列表缓存起来，只在输入真正变化时重建——
内置的 `PagePathValueSource` 与 `AnchorValueSource` 就是这么做的。

## 5. 插入模板

补全标签名默认写入 `<Name />`，或者写入成对标签并把光标放在容器内部。如果某个标签必须带上属性或内容才
有意义，就声明它应该补全成什么文本：

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` 把光标放在给定标记第一次出现的位置之后，因此模板不需要手数字符数就能说明接下来在哪里继续
输入。`InsertTemplate.of(tag, text)` 则把光标放在文本末尾。模板会替换已输入标签名，作者确认后就直接落
在所写好的形式里。

本模组自身也为"光有标签名不够用"的标签声明了模板，例如 `<BlockStat item="" count="1" />`、
`<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />`，以及带第一个数据系列的图表。

## 6. 槽位：你自己的语法

以上都是本模组已经解析的语法。若要补全你自己的语法——块内的指令、代码围栏里的迷你语言、你自己编译的字
段——注册一个 `SyntaxSlot`：

```java
public class MyModSlot implements SyntaxSlot {

    @Override
    public String namespace() {
        return "mymod";
    }

    @Override
    public SyntaxSlotMatch match(String text, int cursorIndex, GuideSyntaxModel model) {
        // 只接管你自己标签里某个属性的取值，且引号尚未闭合。
        String attribute = "ports=\"";
        int attributeStart = text.lastIndexOf(attribute, cursorIndex);
        if (attributeStart < 0) {
            return null;
        }
        int from = attributeStart + attribute.length();
        int closingQuote = text.indexOf('"', from);
        if (closingQuote >= 0 && cursorIndex > closingQuote) {
            return null;
        }
        return new SyntaxSlotMatch(
            from,
            cursorIndex,
            text.substring(from, cursorIndex),
            MyPortRegistry.suggestions(),
            suggestion -> SyntaxReplacement.cursorAtEnd(suggestion.value()));
    }
}
```

注册方式与贡献者完全一致：

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

槽位能控制的内容：

- `match` 决定"确认候选项后替换哪一段范围"、"这段里已经输入了什么"、"编辑器提供哪些取值"。光标不在你的
  语法里时返回 `null`。你报告为"已输入"的文本必须是文档里该范围真实的内容：编辑器写入前会再次核对，若报
  的是规范化后的形式，这次确认会被丢弃。
- match 里的 `SyntaxValueWriter` 决定确认后写入什么：替换范围的文本、光标落点、以及选中范围。留空
  （`null`）表示用取值本身替换该范围。writer 抛异常或不返回内容时会在日志中报告，编辑器不写入任何内容。
- `selection` 可选声明双击你的语法时选中哪一段。

槽位会在编辑器自身的解析器之前被询问；第一个命中的槽位即拥有光标，即使它当下没有可提供的取值——因此本模
组不会在别的模组声明的文本里乱猜。

## 7. 编辑器当前已覆盖的语法

- MDX 标签，按外层容器收窄；会包裹内容的标签补全为 `<Name></Name>`。
- 属性名，按取值类型展开为 `name=""`、`name={}` 或 `name={true}`。
- 属性取值，按取值类型路由，包括配方、物品/方块 id 与模组 id。
- Markdown 语法：标题、列表、任务列表、引用、提示块、表格、代码块、分隔线、公式块，以及行内的强调、
  行内代码、链接与图片。每个片段都会把光标放在你接下来要继续输入的位置。
- 代码围栏语言，来自代码块语言注册表加上 GuideNH 特有的几种围栏。
- YAML frontmatter 键，以及声明了取值类型的键的取值。
- 插入模板：光有标签名不够用的标签会一次性补全成完整形式。

候选项会排序，弹窗默认选中最近的一个：先精确前缀，再匹配命名空间之后的标识，最后才是名称中任意位置
匹配。

## 8. 可扩展的范围，以及目前还不行的地方

本页所有内容都是注册表；注册进去的插件只在构建指南或编译页面时被读取，因此请在模组加载阶段注册。

| 你想做的事 | 用什么 |
| --- | --- |
| 新增标签、场景元素、属性、取值类型及其取值 | `TagCompiler`、`SceneElementTagCompiler`、`SyntaxContributor`、`SyntaxValueSource` |
| 补全你自己的语法（自己的槽位、自己的写入方式、双击范围） | `SyntaxSlot` |
| 让某个标签一次性补全成完整形式 | `sink.insertTemplates(...)`（同时出现在编辑器插入菜单里） |
| 新增 Markdown 片段、围栏名、frontmatter 键与取值 | `SyntaxContributor` |
| 决定你自己围栏的内容如何解析 | `CodeFenceRenderer`（围栏名同时用 `sink.fenceLanguages(...)` 声明；重写 `renderSiteFence` 让站点也显示） |
| 新增符号颜色名 | `SymbolicColorResolver`（单指南）或 `registerSymbolicColorResolver`（全局） |
| 新增页面索引 | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| 把自己的标签导出到站点 | `GuideSiteTagRenderer`（见 [模组兼容](Mod-Compatibility-zh-CN)） |
| 给场景编辑器加工具栏按钮或菜单项 | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| 让导出站点多一份资源 | 在你的节点上实现 `ExportableResourceProvider` |
| 把你自己的语法问题报告给读者 | `LytErrorSink.appendError(compiler, text, element)`，即标签编译器拿到的 `parent` |
| 给指南编辑器加按钮或菜单项 | `GuideNhIntegrationRegistry.registerEditorAction(GuideEditorActionContribution.insert(...))` 或 `.wrap(...)`，或实现 `GuideEditorActionContribution.Provider` 只加到某个指南 |

刻意保持封闭的部分，以及代价：

- 指南编辑器的内置工具栏是固定集合。贡献的**动作**（见 `GuideEditorActionContribution`）会在内置项之后获得自己的工具栏按钮与菜单项，因此无法做的是改动某个内置动作，而不是新增一个。
- 诊断在编译期通过 `LytErrorSink` 上报：错误块会被追加进文档，因此书内与导出站点都会显示。编辑器里没有单独的“边打字边告警”钩子。
- 站点导出用自己的内置链渲染自带标签。注册的渲染器会先被询问（对每个 MDX 元素、以及你自己的围栏名），所以你的标签与围栏都能导出；但内置链本身还不是一组注册式渲染器。
- 你自己的场景注解能在书内渲染，但不会被序列化进导出站点的查看器。
- 你自己注册为 `SceneElementTagCompiler` 的场景元素能在书内渲染，但只有它的元素是 `SceneAnnotation` 时才进得了站点：导出器只收集这一种形态。其他形态（例如自行绘制几何体的元素）会被排除在导出之外，而场景其余部分照常导出。要让这类元素导出，需要一个能承载它的注解，或为产出该标签的站点渲染器提供标记。

插件需要注意的约定：

- `SyntaxValueKind` 的 id 请带你的模组命名空间（如 `"MYMOD_MACHINE"`）。取值来源只按 id 路由；是否需要引号属于声明该属性的那一方。
- `SyntaxSlot.match` 请返回文档里真实存在的那段文本：编辑器写入前会重新核对，规范化后的形式会让这次确认被丢弃。
- 插件抛异常会被记录（带命名空间）并跳过，因此失败只影响你自己的贡献，不会拖垮编辑器。

