# Debug Mode

Use the debug overlay to inspect page layout, interaction regions and scene elements.

## Activation

Press `Ctrl + Shift + Alt + C` in any GuideNH screen, or enable `GUI Debug Overlay` (`guiDebugMode`) in the mod configuration.

## Inspecting a page

- The lower-left panel shows mouse position, FPS, memory, the hovered element and optional parent information.
- Enable element outlines to check positions, sizes and container boundaries.
- The bottom `Debug Options` menu controls displayed information, prints the document tree, recompiles the current page and exports debug data.
- Tree output and exported data are written to the Minecraft log. Include the page source, reproduction steps and relevant log entries when reporting a problem.

## Configuration

Use the debug category in `config/guidenh/guidenh.cfg` or the mod configuration screen.

| Setting | Default | Range |
| --- | --- | --- |
| `debugTextScale` | `0.8` | 0.5–2.0 |
| `debugOutlineThickness` | `2.0` | 0.5–5.0 px |
| `debugDashAnimationCycleMs` | `2000` | 500–10000 ms |
| `debugDashWidth` | `2.0` | 0.5–5.0 px |
| `debugDashOnLength` | `4.0` | 1–20 px |
| `debugDashOffLength` | `4.0` | 1–20 px |

Individual switches control hovered/parent information, outlines, FPS, memory and mouse position. Disabling debug mode hides the overlay and retains its settings.

Mod developers can register custom element information; see [Developer Integration](Developer-Integration).
