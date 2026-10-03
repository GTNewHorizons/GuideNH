---
navigation:
  title: ItemLink Italic Probe
  position: 8989
---

TEST GOAL: gathering boundary-gap evidence for italic text immediately followed by an item icon (ItemLink inline icon) — probe only, for diagnostics

INVARIANTS: none (this page is a diagnostic probe; it only measures the glyph ink → icon ink gap)

## A Italic tight tall

*Mink*<ItemLink id="minecraft:diamond" showIcon="left" showText="false" />

## B Upright tight tall

Mink<ItemLink id="minecraft:diamond" showIcon="left" showText="false" />

## C Italic tight natural

*Mine*<ItemLink id="minecraft:diamond" showIcon="left" showText="false" />

## D Upright tight natural

Mine<ItemLink id="minecraft:diamond" showIcon="left" showText="false" />

## E Italic space reference

*Mine* <ItemLink id="minecraft:diamond" showIcon="left" showText="false" />

## F Upright space reference

Mine <ItemLink id="minecraft:diamond" showIcon="left" showText="false" />

## G Italic right icon

*Iron*<ItemLink id="minecraft:diamond" showIcon="right" showText="false" />

## H Italic II tight

*II*<ItemLink id="minecraft:diamond" showIcon="left" showText="false" />

## I Italic WW tight

*WW*<ItemLink id="minecraft:diamond" showIcon="left" showText="false" />

## J Italic ll tight

*ll*<ItemLink id="minecraft:diamond" showIcon="left" showText="false" />

## K Upright II tight

II<ItemLink id="minecraft:diamond" showIcon="left" showText="false" />

## L ItemLink display name + right icon

<ItemLink id="minecraft:diamond" showIcon="right" showText="true" />
