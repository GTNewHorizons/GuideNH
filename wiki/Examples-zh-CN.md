[English](Examples)

# 示例

GuideNH 在 `wiki/resourcepack/` 中内置了一份运行时示例指南。本页将重要示例文件与它们所展示的功能对应起来。

## 核心示例页面

下表文件位于 `wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/`。

| 文件 | 用途 |
| --- | --- |
| `index.md` | 分类入口 |
| `markdown.md` | Markdown 语法 |
| `markdown-table.md` | 表格与合并单元格 |
| `rendering.md` | 块级布局 |
| `images.md` | 图片与纹理裁剪 |
| `tooltips.md` | 悬停说明 |
| `recipes.md` | 物品/流体配方及筛选 |
| `charts.md` | 图表、参考线及流体图标 |
| `function-graph.md` | 函数绘图 |
| `content-embed.md` | 内容嵌入与文字环绕 |
| `ponder.md` | 动画时间线 |
| `scene-blocks.md` | 方块与材料清单 |
| `scene-import.md` | 结构导入与方块编辑 |
| `scene-entities.md` | 实体预览 |
| `scene-camera.md` | 相机控制 |
| `scene-annotations.md` | 场景注解 |
| `scene-particles.md` | 粒子与天气 |
| `navigation-guide.md` | 导航、索引绑定与图标 |
| `page-lang-key.md` | 整页翻译 |
| `templates-demo.md` | 模板与包含控制 |
| `debug.md` | 布局与交互检查 |
| `japanese.md` | 语言与导航示例 |

模板定义在 `templates/` 子目录。`wiki/resourcepack/assets/gregtech/guidenh/` 演示另一个命名空间的页面与跨指南链接。

## 资源示例

| 运行时文件 | 用途 |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | 页面私有图片示例 |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | 供 `<ImportStructure>` 和 `<RemoveBlocks>` 使用的共享根结构资源 |

## 示例片段

### Frontmatter + Navigation

```yaml
item_ids:
  - minecraft:book
navigation:
  title: Root
  icon_texture: test1.png
  recommend: 3
```

### 首页推荐

```yaml
navigation:
  title: Markdown Basics
  parent: index.md
  recommend: 0
```

```yaml
navigation:
  title: Navigation & Index
  parent: index.md
  recommend: 5
```

### 相对图片

````md
![Test Image](test1.png)
````

### 带命名空间的指南链接

````md
[同命名空间](guide.md)
[同命名空间根路径](/guide.md)
[其他命名空间](gregtech:guide.md)
[其他命名空间根路径](gregtech:/guide.md)
````

### 根路径结构资源

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib 场景导入

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:1000">
    <Tier value="4" />
    <Channel name="hatch" value="1" />
    <Facing value="north" />
    <Rotation value="normal" />
    <Flip value="none" />
    <GregTechActiveController />
    <GregTechPlaceHatches />
  </ImportStructureLib>
</GameScene>
````

子标签会设置 StructureLib 场景默认值，点击 reset view 时会恢复这些默认值。

### 导入结构后的清理

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### 场景注解 Tooltip

````md
<GameScene>
  <DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
    ### Activated Beacon
    <RecipeFor id="minecraft:furnace" />
  </DiamondAnnotation>
</GameScene>
````

### 配方过滤

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### 数量格式化

使用 `<Amount>` 表示数量。`unit` 可省略，默认只显示数字；`fluid` 遵循 GTNHLib 的 `mB`/`L` 偏好。

````md
<Amount value="250" />
<Amount value="250" unit="fluid" />/s
<Amount value="1200000" format="compact" /> 个物品
````

## 什么时候该用哪个示例

- 若你在验证解析器基础能力，先看 `markdown.md`
- 若你要一起测试混合运行时特性，包括 StructureLib tooltip、舱口高亮和清理逻辑，用 `index.md`
- 若你只需要静态方块布局预览，用 `structure.md`
- 若你需要可复用的导入结构资源，用 `example_structure.snbt`
- 若要测试模板转译、条件判断与包含控制，用 `templates-demo.md` 配合 `templates/` 目录。参见[模板](Templates-zh-CN)

## 推荐学习顺序

1. [快速开始](Getting-Started-zh-CN)
2. [指南页面格式](Guide-Page-Format-zh-CN)
3. [标签参考](Tags-Reference-zh-CN)
4. [游戏场景](GameScene-zh-CN)
5. [注解](Annotations-zh-CN)
6. [配方](Recipes-zh-CN)
