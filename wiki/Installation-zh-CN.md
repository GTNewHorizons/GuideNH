[English](Installation)

# 安装

将适用于 Minecraft 1.7.10 的 GuideNH 和 GTNHLib 放入客户端的 `mods/` 目录。结构世界放置及管理员命令需要服务端也安装 GuideNH；详见 [服务端集成](Server-Integration-zh-CN)。

## 添加本地指南

客户端启动后会创建空目录 `config/guidenh/DefaultGuide/`。将 UTF-8 页面放入语言目录，资源放入指南的 `assets/` 目录：

```text
config/guidenh/DefaultGuide/
`-- <modid>/
    `-- guidenh/
        |-- assets/
        |   `-- example_structure.snbt
        |-- _en_us/
        |   `-- index.md
        `-- _zh_cn/
            `-- index.md
```

按 `G` 打开指南，添加或编辑资源后按 `F3+T` 重载。首个页面示例见 [快速开始](Getting-Started-zh-CN)。

## 使用资源包

普通资源包使用 `assets/<modid>/guidenh/` 布局。`config/guidenh/DefaultGuide.zip` 同样接受完整资源包布局；与 `DefaultGuide/` 同时存在时优先加载 ZIP：

```text
config/guidenh/DefaultGuide.zip
`-- assets/
    `-- <modid>/
        `-- guidenh/
            |-- assets/
            |   `-- example_structure.snbt
            |-- _en_us/
            |   `-- index.md
            `-- _zh_cn/
                `-- index.md
```

## 修改仓库示例

示例页面位于 `wiki/resourcepack/assets/`。将 `wiki/resourcepack/` 作为资源包加载，或按 [实时预览](Live-Preview-zh-CN) 配置 JVM 参数，直接读取该目录。构建模组不会自动打包这些示例。
