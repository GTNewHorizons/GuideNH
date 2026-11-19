---
navigation:
  title: Function Graphs
  parent: index.md
  position: 180
categories:
  - markdown
  - charts
---

# Function Graphs

Interactive function graphs support panning, zooming, and scrubbing values along a curve. The `funcgraph` fence, `<FunctionGraph>` container, and `<Function>` shorthand share the same attribute rules.

## Panel Attributes

| Attribute | Default | Meaning |
| --- | --- | --- |
| `title` | — | Panel title |
| `xLabel`, `yLabel` | — | Axis titles |
| `width`, `height` | 320, 220 | Preferred panel dimensions in pixels |
| `xRange`, `yRange` | auto | Axis bounds as `a..b`; take precedence over individual bounds |
| `xMin`, `xMax`, `yMin`, `yMax` | auto | Individual axis bounds; numeric literals or constants such as `pi` |
| `domain` | — | Container-only compatibility alias for X bounds when no X range or bounds are set |
| `xStep`, `yStep` | auto | Tick/grid spacing |
| `quadrants` | auto | `1,2,3,4` or `all`; default behavior is described below |
| `background`, `border` | theme | Panel fill and border colors |
| `axisColor`, `gridColor` | theme | Axis and grid colors |
| `showGrid`, `showAxes` | true | Grid and axis visibility |
| `cornerLegend` | none | `none`, `topLeft`, `topRight`, `bottomLeft`, or `bottomRight`; lists labelled curves inside the plot |
| `cornerLegendWidth`, `cornerLegendHeight` | 120, 64 | Maximum inset legend dimensions in pixels |
| `cornerLegendBackground` | #AA111922 | Inset legend background color |

Without explicit ranges or `quadrants`, the panel starts in quadrant 1. If sampling finds negative Y values and Y bounds are not explicit, it expands to quadrants 3 and 4. Labelled curves appear in a legend below the panel; `cornerLegend` controls an inset legend.

## Curve Attributes

Used by `<Plot>` / `<Function>` children and expression lines inside a fence.

| Attribute | Default | Meaning |
| --- | --- | --- |
| `expr` | required | Function expression |
| `inverse` | false | Plot `x = f(y)` instead of `y = f(x)` |
| `domain` | — | Curve domain, such as `-pi..pi` or `x>=0,x<5` |
| `color` | palette | Curve color |
| `label` | — | Name in the legend and tooltip |
| `tooltip` | — | Extra plain text in the tooltip |
| `showFunction`, `showValues` | true | Show the expression and current coordinates in the tooltip |
| `pointEveryX`, `pointEveryY` | off | Positive sampling intervals along X or Y; generate marked points on this curve |
| `autoPointLabel` | none | `none`, `x`, `y`, or `xy` |
| `autoPointColor` | curve color | Generated point color; omitted values inherit the curve color |

Tooltip order is label, expression, live coordinates, `tooltip` text, then child rich content. Omitted or disabled entries are skipped. Curves with `expr` can contain Markdown and GuideNH tags as rich tooltip content, which Site Export preserves.

## Point Attributes

| Attribute | Default | Meaning |
| --- | --- | --- |
| `x`, `y` | — | Explicit point coordinates; supply both |
| `plot` | — | Zero-based curve index for an anchored point |
| `atX`, `atY` | — | With `plot`, provide one coordinate to evaluate the other on the curve |
| `color` | auto | White for explicit points; curve color for anchored points |
| `label` | — | Point label |

## Fenced Blocks

The first line sets panel attributes. Subsequent non-blank lines are expressions, explicit points (`:x,y`), or anchored points (`@plot=N atX=v` / `@plot=N atY=v`). A `|` separates curve attributes from an expression. Comments start with `#`.

```funcgraph
width=360 height=220 xRange=-pi..pi yRange=-2..2 quadrants=all cornerLegend=topRight
sin(x) | color=#ff5566 label="sin" pointEveryX=1 autoPointLabel=x
cos(x) | color=#3399ff label="cos" pointEveryY=1
:0,0
@plot=0 atX=1.5708
```

## `<FunctionGraph>` Container

```mdx
<FunctionGraph width="360" height="220" xRange="-4..4" yRange="-2..6" quadrants="all" cornerLegend="topRight">
  <Plot expr="x^2" color="#4488ff" label="x²" pointEveryX="1" autoPointLabel="xy">
    **Fuel efficiency example:** <ItemLink id="minecraft:coal" />
  </Plot>
  <Function expr="x - 1" color="#ff6644" label="x - 1" showFunction={false} />
  <Point x="0" y="0" label="O" />
  <Point plot="0" atX="1" />
</FunctionGraph>
```

<FunctionGraph width="360" height="220" xRange="-4..4" yRange="-2..6" quadrants="all" cornerLegend="topRight">
  <Plot expr="x^2" color="#4488ff" label="x²" pointEveryX="1" autoPointLabel="xy">
    **Fuel efficiency example:** <ItemLink id="minecraft:coal" />
  </Plot>
  <Function expr="x - 1" color="#ff6644" label="x - 1" showFunction={false} />
  <Point x="0" y="0" label="O" />
  <Point plot="0" atX="1" />
</FunctionGraph>

## `<Function>` Shorthand

A standalone `<Function>` creates a single-curve panel and accepts both panel and curve attributes.

<Function expr="x^2 - 2x + 1" xRange="-2..4" yRange="-1..5" color="#3399ff" />

## Expression Syntax

- Operators: `+ - * / %`, right-associative `^`, and unary minus. Precedence: `^`, unary minus, `* / %`, then `+ -`.
- Implicit multiplication: `2x`, `2pi`, `(x+1)(x-1)`; `|expr|` is absolute value.
- Unary functions: `sin cos tan asin acos atan sinh cosh tanh exp ln log log2 log10 sqrt cbrt abs sign floor ceil round deg rad gamma`. Trigonometric functions use radians. `log` means `log10`; `round` uses ties-to-even rounding.
- Binary functions: `atan2(y,x)`, `min(a,b)`, `max(a,b)`, `pow(a,b)`, `hypot(a,b)`, `mod(a,b)`. Write `ln(x)/ln(b)` for a logarithm in base `b`.
- Postfix `!` is factorial, extended to real numbers via the gamma function; negative integers return NaN.
- Constants: `pi`, `tau`, `e`, `phi`; `√` / `sqrt` and `∛` / `cbrt` are roots.
- `domain` accepts `min..max` or comma-separated conditions such as `x>=0,x<5`. `inverse={true}` plots the expression as `x = f(y)`.
