---
navigation:
  title: Mermaid NodeContent Rich Blocks
  parent: visualtest/index.md
  position: 8160
---

<!--
测试目标：<NodeContent id> 富内容节点（节点内嵌格式化文本 / 列表）
不变式：富内容在节点框内布局正确
-->

Expected: A mindmap where the "runtime" node contains formatted text with an item image, and the "preview" node contains a list.

<Mermaid width="400" height="300">
mindmap
  root["GuideNH Features"]
    runtime["Runtime Blocks"]
    preview["Scene Preview"]

<NodeContent id="runtime">
Runtime nodes support **bold text**, *italic text*, and inline <ItemImage id="minecraft:diamond" />.

Here is a second paragraph inside a runtime node.
</NodeContent>

<NodeContent id="preview">
Preview content can contain lists:

- Block images
- Entity views
- Annotations

And also **formatted** text.
</NodeContent>
</Mermaid>

## NodeContent in Flowchart

Expected: A flowchart node with NodeContent renders a crafting recipe block inside it.

<Mermaid width="600" height="400">
flowchart LR
  subgraph Craft["Crafting"]
    Ingredients[Ingredients]
    Result[Result]
  end

  Ingredients --> Result

<NodeContent id="Result">
<RecipeFor id="minecraft:crafting_table" />
</NodeContent>
</Mermaid>
