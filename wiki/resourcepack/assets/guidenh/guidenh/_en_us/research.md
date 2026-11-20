---
navigation:
  title: Thaumcraft Research & Bindings
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

# Research & Bindings

Research IDs retain their original case. Hover a research entry in the Thaumonomicon or an ARI research widget in NEI and press NEI's configured **Copy identifier** shortcut. ARI's `showResearchId` controls labels and shortcut hints; copying also works while it is disabled. `showResearchKey` independently controls the NEI research widgets.

## Infusion

<ResearchLink id="INFUSION" text="Open the bound infusion guides" />

<ResearchLink id="INFUSION" text="Open the Thaumonomicon" target="native" />

<ResearchCard id="INFUSION" title="Infusion research" showParents={true} />

The page top, this section and [Infusion apparatus](#apparatus) bind the same research. Right click the research link or hold the open-guide key to see every destination. Different anchors remain separate choices; bindings on other pages use the same destination selector.

## Apparatus

<ResearchCard id="INFUSION" target="native" showParents={true} />

This section provides another guide destination for `INFUSION`. Return to the [infusion section](#infusion) to compare guide and native navigation.

## Nitor

<BindingCard type="thaumcraft" id="NITOR" target="native" showDesc={true} />

<Row>
  <ResearchIcon id="NITOR" scale={1.5} />
  <BindingIcon type="thaumcraft" id="INFUSION" />
</Row>

Icons keep the research identity for tooltips and the open-guide key; they do not open the native screen when clicked. Completed research links can open their entry, while incomplete research opens its research-tree category without unlocking it.

## Quest and custom provider syntax

Replace the example quest ID with one from your pack. QuestLink and QuestCard always open BQ with the left mouse button; right click opens bound guide destinations or falls back to BQ.

```mdx
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" title="Stage guide" />
<BindingLink type="your_provider" id="chapter:stage_2" />
```

```yaml
bindings:
  - {type: "betterquesting", id: "01234567-89ab-cdef-0123-456789abcdef", anchor: "quest-and-custom-provider-syntax"}
```

In NEI Options → Tools → Data Dumps, select **Thaumcraft research IDs**, choose CSV or JSON and press Dump to inspect the complete research catalogue. IDs with no NEI recipes are included; `showResearchId` may remain disabled.
