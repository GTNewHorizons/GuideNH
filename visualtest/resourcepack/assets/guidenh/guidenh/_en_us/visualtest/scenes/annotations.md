---
navigation:
  title: Scene Annotations
  position: 7880
---

TEST GOAL / 测试目标：五种注解 `<BlockAnnotation>` / `<BoxAnnotation>` / `<LineAnnotation>` / `<DiamondAnnotation>` / `<TextAnnotation>` + `<LinePoint>` + `<BlockAnnotationTemplate>`

INVARIANTS / 不变式：注解对位正确、颜色/粗细生效、hover tooltip 触发区不偏移

## DiamondAnnotation Marker

Expected: Gold diamond (#FFD24C) at the center of the beacon top (gray outer ring by design); tooltip shows "Activated Beacon" text (interactive, out-of-scope in static screenshots). Second diamond at (2.5, 1.5, 0.5) is red.

<GameScene width="256" height="160" zoom={4} interactive={true}>
  <Block id="minecraft:diamond_block" x="-1" z="-1" />
  <Block id="minecraft:diamond_block" z="-1" />
  <Block id="minecraft:diamond_block" x="1" z="-1" />
  <Block id="minecraft:diamond_block" x="-1" />
  <Block id="minecraft:diamond_block" />
  <Block id="minecraft:diamond_block" x="1" />
  <Block id="minecraft:diamond_block" x="-1" z="1" />
  <Block id="minecraft:diamond_block" z="1" />
  <Block id="minecraft:diamond_block" x="1" z="1" />
  <Block id="minecraft:beacon" y="1" />
  <DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
    Activated Beacon
  </DiamondAnnotation>
  <DiamondAnnotation pos="2.5 1.5 0.5" color="#FF0000">
    Red Marker
  </DiamondAnnotation>
</GameScene>

## BoxAnnotation And BlockAnnotation

Expected: Red semi-transparent box wraps half an iron block; cyan block annotation on gold block at (2,0,2) with alwaysOnTop.

<GameScene width="256" height="160" zoom={4} interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:gold_block" x="2" z="2" />
  <BoxAnnotation color="#ee3333" min="0 1 0" max="0.5 1.6 0.5" thickness="0.04">
    Box wraps half a block
  </BoxAnnotation>
  <BlockAnnotation color="#33ddee" pos="2 0 2" alwaysOnTop={true}>
    Block annotation always on top
  </BlockAnnotation>
</GameScene>

## LineAnnotation With Polyline And Arrow

Expected: Yellow line from (0.5,1.2,0.5) to (2.5,1.2,2.5); blue polyline with three points and end arrow; first and second points shown as cubes.

<GameScene width="256" height="160" zoom={4} interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:gold_block" x="2" z="2" />
  <LineAnnotation color="#ffd24c" from="0.5 1.2 0.5" to="2.5 1.2 2.5" thickness="0.06">
    Simple line
  </LineAnnotation>
  <LineAnnotation color="#66ccff" points="0.5 1.8 2.5; 1.5 2.25 1.5; 2.5 1.8 0.5" thickness="0.08" arrow="end">
    <LinePoint index="0" show color="#66ccff" />
    <LinePoint index="1" show color="#ff8844" size="0.12" />
    Polyline with arrow
  </LineAnnotation>
</GameScene>

## TextAnnotation Independent And Anchored

Expected: Independent label at screen top; world-anchored label at (1.5,2.0,1.5) with connector side right.

<GameScene width="256" height="160" zoom={4} interactive={true}>
  <Block id="minecraft:furnace" />
  <Block id="minecraft:chest" x="2" />
  <TextAnnotation text="Independent label" color="#FFFFCC00" backgroundAlpha={140} independent={true} yOffset={30} />
  <TextAnnotation text="Anchored label" color="#FF44AAFF" maxWidth={100} connectorSide="right" pos="1.5 2.0 1.5" />
</GameScene>

## BlockAnnotationTemplate

Expected: All four logs at corners receive a red diamond annotation — template applies to every matching block automatically.

<GameScene zoom={3} interactive={true}>
  <Block id="minecraft:log" />
  <Block id="minecraft:log" x="2" />
  <Block id="minecraft:log" z="2" />
  <Block id="minecraft:log" x="2" z="2" />
  <Block id="minecraft:stone" x="1" z="1" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Log block
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
