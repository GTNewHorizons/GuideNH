[English](Developer-Integration)

# 开发者集成

本页面向扩展 GuideNH 的模组开发者。指南作者使用内置语法无需 Java 注册；参见 [标签参考](Tags-Reference-zh-CN)。

## 注册指南

```java
import net.minecraft.util.ResourceLocation;
import com.hfstudio.guidenh.guide.Guide;

Guide.builder(new ResourceLocation("yourmod", "guidenh")).build();
```

页面放入 `assets/yourmod/guidenh/`，语言目录以下划线开头。语言回退规则见 [本地化](Localization-zh-CN)。

## 内容绑定提供者

任务、研究和后续自定义内容使用同一套接口。注册提供者后，作者即可使用 `BindingLink`、`BindingCard`、`BindingIcon` 和 `bindings`，无需新增专属标签。

### 注册与职责

| 接口 | 注册入口 | 职责 |
| --- | --- | --- |
| `GuideBindingType` | `GuideNhIntegrationRegistry.global().registerBindingType(type)` | `id()` 声明类型，`normalizeId(String)` 规范化内容 ID；不访问游戏注册表 |
| `GuideBindingProvider` | `GuideNhIntegrationRegistry.global().registerBindingProvider(provider)` | 查询内容目录、生成展示快照、打开原生界面 |
| `GuideBindingHoverProvider` | `GuideNhClientIntegrationRegistry.global().registerBindingHoverProvider(provider)` | 在支持的界面和实际命中区域返回 `BindingHoverTarget(key, bounds)` |

`GuideBindingProvider` 的方法按用途划分：

| 方法 | 作用 |
| --- | --- |
| `type()` | 对应已注册的内容类型 |
| `describe(BindingKey, BindingViewContext)` | 返回该内容的展示快照 |
| `search(String, int, int, BindingViewContext)` | 根据查询文本、偏移量和上限返回一页结果；上限为 `0` 时返回空页 |
| `open(BindingKey, GuideUiHost)` | 打开原生界面，保留来源指南上下文 |
| `revision()` | 标识内容目录版本，供缓存失效使用 |

### 身份、快照与线程

`BindingKey(type, id)` 表达完整身份。类型名去除首尾空白并转为小写，ID 由该类型规范化，研究 ID 保留大小写。`BindingDisplay` 携带状态、名称、简介、可选物品或纹理图标、tooltip、前置 `BindingKey` 列表和原生打开能力；状态使用可扩展 ID。

返回不可变快照，隐藏内容不携带详情。`BindingViewContext.staticView()` 为静态导出提供上下文，不得读取导出者的玩家进度。游戏注册表查询和原生 GUI 操作在客户端线程执行；纯类型规范化及页面索引不访问研究注册表。

`GuideBindingIndex` 保留全部页面与锚点，核心导航处理多目标选择和按住快捷键的状态。悬停提供者只报告当前命中目标。公共 `QuestIndex` API 作为统一索引的兼容入口保留；任务 YAML 使用新的 `bindings`。

### 导出与编辑器

两种网站导出路径均读取静态快照。运行时桥公告 `bindings` 能力，语义和预览查询使用 `filters.type` 指定内容系统，条目保留原始 ID 大小写。预览载荷在标准字段外携带 `type`、`state` 和前置 `BindingKey` 列表。

作者语法与单行 YAML 转义见 [外部内容绑定](Guide-Page-Format-zh-CN#外部内容绑定)，标签行为见 [研究与自定义内容](Tags-Reference-zh-CN#研究与自定义内容)。

## 导出自定义标签

为模组自身编译器提供的标签实现 `GuideSiteTagRenderer`。`getTagNames()` 声明标签名，`render(context, element)` 返回 HTML；返回 `null` 则交给其他渲染器处理。

可以全局注册，或仅注册到一个指南：

```java
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteTagRenderer;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

GuideNhIntegrationRegistry.global().registerSiteTagRenderer(renderer);
Guide.builder(id).extension(GuideSiteTagRenderer.EXTENSION_POINT, renderer).build();
```

`renderer` 是你的实现，`id` 是指南 ID。自定义渲染器先于内置渲染器执行，第一个非空结果生效。返回 HTML 前应转义用户文本，例如使用 `GuideSiteGraphRenderer.esc(text)`。渲染器异常会记录日志并跳过。

渲染上下文提供 `defaultNamespace`、`currentPageId`、`templates`、`sceneResolver`、`compiler`，用于页面相对资源与共享导出服务。

## 绘制嵌入配方

实现 `RecipeHandlerRenderProvider`，在客户端初始化时调用 `GuideNhIntegrationRegistry.global().registerRecipeHandlerRenderProvider(provider)` 注册。`canRenderRecipeHandler(Object)` 判断提供者支持哪些处理器。

指南依次调用 `renderRecipeHandlerBackground(Object, int, boolean)`、绘制配方槽位、调用 `renderRecipeHandlerForeground(Object, int, boolean)`。背景放在第一阶段，槽位角标和其他覆盖绘制放在第二阶段；坐标相对于配方内容区域。处理器的前景依赖不可用的附加槽位时，应遵循 `skipForeground`。

仅实现 `renderRecipeHandler(Object, int, boolean)` 的现有提供者会在背景阶段调用一次。需要将覆盖内容画在槽位上方时，覆盖两个阶段方法。NEI 提供者遵循 NEI 原生绘制顺序，由 `drawForeground` 调用 `drawExtras`，不要重复调用额外绘制。

## 扩展流体提示

实现 `FluidTooltipProvider.appendTooltip(FluidStack, List<String>)`，在客户端初始化时调用 `GuideNhClientIntegrationRegistry.global().registerFluidTooltipProvider(provider)` 注册。提供者在流体名称后追加材料信息，数量、温度、状态和可选的高级注册名由 GuideNH 添加。每个提供者收到独立的流体栈和空列表；提供者失败时不会保留其未完成的提示，也不会阻止其余提供者和默认信息。查询应保持轻量，不要保存可变流体栈。GregTech 材料提示通过此 API 接入，无需 NEI。

## 扩展调试信息

实现 `DebugInfoExtractor` 的 `canHandle(LytNode)`、`extract(LytNode, HoveredElementInfo)`，并按需覆盖 `getPriority()`。优先级越高越早检查，第一个匹配的提取器处理该节点。

```java
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractorRegistry;

DebugInfoExtractorRegistry.register(extractor);
```

在模组初始化时注册你的 `extractor`。提取过程应保持只读和低开销，因为它会在检查页面时执行。作者使用的控件说明见 [调试模式](Debug-Mode-zh-CN)。
