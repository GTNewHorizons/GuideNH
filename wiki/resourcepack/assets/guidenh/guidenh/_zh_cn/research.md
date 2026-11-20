---
navigation:
  title: Thaumcraft 研究与绑定
  parent: index.md
  position: 196
  required_mod: Thaumcraft
research_ids:
  - "INFUSION"
  - "NITOR"
bindings:
  - {type: "thaumcraft", id: "INFUSION", anchor: "infusion"}
  - {type: "thaumcraft", id: "INFUSION", anchor: "apparatus"}
categories:
  - other
---

# 研究与绑定

研究 ID 保留原始大小写。悬停魔导手册里的研究条目或 NEI 中 ARI 的研究组件，按 NEI 已配置的“复制标识符”快捷键。ARI 的 `showResearchId` 仅控制 ID 标签与快捷键提示，关闭时仍能复制。`showResearchKey` 独立控制 NEI 的研究组件。

## Infusion

<ResearchLink id="INFUSION" text="打开绑定的注魔指南" />

<ResearchLink id="INFUSION" text="打开魔导手册" target="native" />

<ResearchCard id="INFUSION" title="注魔研究" showParents={true} />

页面顶部、本节与[注魔装置](#apparatus)绑定同一个研究。右键研究链接或按住打开指南键，会显示全部目标；不同锚点仍可分别选择，其他页面的绑定也使用同一目标选择界面。

## Apparatus

<ResearchCard id="INFUSION" target="native" showParents={true} />

本节为 `INFUSION` 提供另一个指南目标。返回[注魔章节](#infusion)，可比较指南跳转和原生跳转。

## Nitor

<BindingCard type="thaumcraft" id="NITOR" target="native" showDesc={true} />

<Row>
  <ResearchIcon id="NITOR" scale={1.5} />
  <BindingIcon type="thaumcraft" id="INFUSION" />
</Row>

图标保留研究身份用于提示和打开指南快捷键，点击不会打开原生界面。原生跳转打开对应分类的研究树并定位研究图标，不会自动解锁。

## 任务与自定义提供者语法

将示例任务 ID 换成整合包中的真实 ID。QuestLink 和 QuestCard 左键始终打开 BQ，右键打开绑定指南，没有指南时回退 BQ。

```mdx
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" title="阶段指南" />
<BindingLink type="your_provider" id="chapter:stage_2" />
```

```yaml
bindings:
  - {type: "betterquesting", id: "01234567-89ab-cdef-0123-456789abcdef", anchor: "任务与自定义提供者语法"}
```

在 NEI 设置 → 工具 → 数据转储中选择**神秘研究 ID**，切换 CSV 或 JSON 后转储完整研究目录。没有 NEI 配方的研究也会列出，无需开启 `showResearchId`。
