[English](FAQ)

# 常见问题

## 我应该在哪里编辑内置运行时指南？

编辑 `wiki/resourcepack/assets/` 下的 UTF-8 页面。单独加载示例资源包，或使用 [实时预览](Live-Preview-zh-CN)；构建模组不会自动打包这些文件。

## 如果我想放一个不会打包进 jar 的客户端本地指南，应该放在哪里？

放在 `config/guidenh/DefaultGuide/<modid>/guidenh/` 下。这个运行时目录会在客户端自动创建，并且使用不带外层 `assets/` 的原生命名空间根布局。

## 为什么这个仓库同时有 wiki 页面和运行时 Markdown 页面？

`wiki/*.md` 下的文件是面向人的 GitHub Wiki 文档。`wiki/resourcepack/...` 下的文件则是 GuideNH 游戏内实际消费的运行时指南页面。

## 两边可以复用同一份 Markdown 吗？

不能直接复用。运行时指南 Markdown 可能包含 GitHub Wiki 无法理解的 GuideNH 专用 MDX 标签。应把运行时语法保留在运行时指南树中，在 wiki 中只通过代码围栏展示它们。

## 我怎样才能快速预览指南改动？

在游戏启动配置中添加 [实时预览](Live-Preview-zh-CN) 说明的 JVM 参数，然后编辑配置的源目录。需要自动打开指南时，再设置 `guideme.showOnStartup`。
