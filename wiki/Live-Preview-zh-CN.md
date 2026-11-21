[English](Live-Preview)

# 实时预览

GuideNH 支持面向开发的实时预览流程。启用后，GuideNH 可以直接从本地目录读取指南页面和资源，并在游戏运行时重载改动，不需要完整重启。

## 适用场景

当你正在编辑 `wiki/resourcepack/` 或整合包资源包里的运行时指南内容，并希望在游戏内快速看到结果时，可以启用实时预览。

它特别适合：

- Markdown 内容修改
- 导航结构调整
- 页面私有图片
- 指南根目录资源，例如导入结构
- 被 `<ImportPonder>` 引用的 Ponder JSON 文件

## 支持的系统属性

| 属性 | 含义 |
| --- | --- |
| `guideme.<guide_namespace>.<guide_path>.sources` | 单个 guide 使用的开发源根目录 |
| `guideme.<guide_namespace>.<guide_path>.sourcesNamespace` | 从该目录加载文件时使用的可选命名空间覆盖 |
| `guideme.resourcePacks.sources` | 按标准资源包根目录加载并监控的开发资源包目录，可以重复指定 |
| `guideme.resourcePack.sources` | `guideme.resourcePacks.sources` 的别名，可以重复指定 |
| `guideme.showOnStartup` | 可选，启动后在标题界面自动打开的指南或页面 |
| `guideme.validateAtStartup` | 可选，启动后执行一次校验的 guide id 列表，使用逗号分隔 |

## 本仓库的 GuideNH 示例

对于本仓库内置示例指南：

- guide id: `guidenh:guidenh`
- 开发源根目录：`wiki/resourcepack/assets/guidenh/guidenh`

对应的系统属性是：

```text
guideme.guidenh.guidenh.sources=<absolute-path-to-repo>/wiki/resourcepack/assets/guidenh/guidenh
```

GuideNH 也支持更宽的开发源根目录：

| 源根目录 | 页面 id 映射 |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh` | content-root 模式；文件通过 `sourcesNamespace` 映射，默认是 `guidenh` |
| `wiki/resourcepack/assets` | assets-root 模式；`assets/<modid>/guidenh/...` 映射为 `<modid>:...` |
| `wiki/resourcepack` | resourcepack-root 模式；`assets/<modid>/guidenh/...` 映射为 `<modid>:...` |

每个命名空间拥有独立指南，导航栏会汇总已加载的指南。页面 ID 保留命名空间，`gregtech:index.md` 与 `appliedenergistics2:index.md` 不会相互覆盖。

## 多资源包实时预览

当你希望一个或多个目录像标准资源包一样被加载，并监控其下所有 `assets/<modid>/guidenh/...` 指南内容时，使用
`guideme.resourcePacks.sources`。

```text
-Dguideme.resourcePacks.sources=E:/packs/base
-Dguideme.resourcePacks.sources=E:/packs/override
```

这个属性可以重复指定。GuideNH 会按 JVM 参数出现的顺序读取它们，因为 Java 的系统属性本身会把重复的 `-D` key 折叠成最后一个值。

冲突规则如下：

- 不同 modid 通过命名空间隔离，例如 `gregtech:index.md` 和 `appliedenergistics2:index.md` 是两张不同页面
- 同一命名空间、语言和页面路径按 `navigation.priority` 选择，优先级更高者获胜；同值时后扫描的资源包获胜
- 开发资源包先于普通资源包扫描，因此同优先级的普通资源包可以覆盖开发页面
- 开发资源包中的资源文件使用最先包含该资源的配置目录

需要直接覆盖某本指南的资源包页面时，使用单指南的 `guideme.<namespace>.<path>.sources` 参数。

修改这些开发资源包根目录下的 Markdown、Ponder JSON、图片或其他文件时，会触发客户端资源重载，合并指南和场景内容会一起刷新。

## `showOnStartup` 支持的格式

`guideme.showOnStartup` 支持以下形式：

- `guidenh:guidenh`
- `guidenh:guidenh!index.md`
- `guidenh:guidenh!index.md#anchor`

`!` 后面的相对页面 id 会自动按指南命名空间解析，因此 `index.md` 会变成 `guidenh:index.md`。

## 重载时间

每 20 个客户端刻检查一次变更，正常刻速下约为一秒。已配置源目录内的页面和资源可在客户端运行时重载。

## 推荐工作流

预览仓库示例时，将下列路径换成你的仓库位置，并把这个 JVM 参数添加到游戏启动配置中：

```text
-Dguideme.guidenh.guidenh.sources=E:/Github/GuideNH-NH/wiki/resourcepack/assets/guidenh/guidenh
```

需要自动打开示例指南时，再添加 `-Dguideme.showOnStartup=guidenh:guidenh!index.md`。这些参数必须传入游戏进程；仅给 Gradle 进程设置系统属性不会配置其启动的客户端。

1. 在启动器中添加源目录的 JVM 参数，然后启动客户端。
2. 编辑 `wiki/resourcepack/assets/...` 或配置的开发资源包根目录下的运行时指南文件。
3. 等待 watcher 检测到变更并触发资源重载。
4. 如果修改了导航结构或启动目标，可以重新打开或重新访问对应页面。

## 哪些情况仍然需要重启

实时预览适合内容迭代，但以下情况通常仍需要重启或完整资源重载：

- 修改的是代码而不是指南内容
- 修改了构建逻辑
- 新增了配置的开发源目录之外的资源

## 相关页面

- [安装](Installation-zh-CN)
- [快速开始](Getting-Started-zh-CN)
- [游戏场景](GameScene-zh-CN)
- [常见问题](FAQ-zh-CN)
