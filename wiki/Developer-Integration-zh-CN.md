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

## 扩展调试信息

实现 `DebugInfoExtractor` 的 `canHandle(LytNode)`、`extract(LytNode, HoveredElementInfo)`，并按需覆盖 `getPriority()`。优先级越高越早检查，第一个匹配的提取器处理该节点。

```java
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractorRegistry;

DebugInfoExtractorRegistry.register(extractor);
```

在模组初始化时注册你的 `extractor`。提取过程应保持只读和低开销，因为它会在检查页面时执行。作者使用的控件说明见 [调试模式](Debug-Mode-zh-CN)。
