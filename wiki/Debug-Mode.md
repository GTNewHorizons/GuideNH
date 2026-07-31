# Debug Mode

GuideNH includes a comprehensive debug mode system for developers and content creators to inspect and debug guide pages in real-time.

## Activation

Debug mode can be activated in two ways:

1. **Configuration**: Enable "GUI Debug Overlay" in the mod configuration (`ModConfig.Debug.guiDebugMode`)
2. **Hotkey**: Press `C + CTRL + SHIFT + ALT` while in any GuideNH screen to toggle debug mode on/off

## Features

### Left-Bottom Information Panel

When debug mode is active, the left-bottom corner displays comprehensive information:

- **Mouse Position**: Current cursor coordinates
- **FPS Counter**: Frames per second performance metric
- **Memory Usage**: JVM heap memory usage (used/max MB and percentage)
- **Hovered Element**: Detailed information about the element under your cursor:
  - Class name
  - Position (X, Y coordinates)
  - Size (width × height)
  - Element-specific extra information (varies by element type)
- **Parent Element**: Information about the parent container (when enabled)

### Center-Bottom Control Panel

Click the "Debug Options" panel at the bottom-center to access a dropdown menu with the following options:

#### Print Document Tree
Outputs the complete document hierarchy to the console with visual tree structure.

#### Hovered Element Info
Sub-menu to toggle display options for the hovered element:
- Show Any (master toggle)
- Show Position
- Show Size
- Show Theme
- Show Extra Info
- Show Outline

#### Parent Element Info
Sub-menu to toggle display options for the parent element:
- Show Any (master toggle)
- Show Position
- Show Size
- Show Theme
- Show Outline

#### Display Options
General display toggles:
- Show FPS
- Show Memory
- Show Mouse Position

#### Recompile Current Page
Forces recompilation of the currently displayed guide page.

#### Export Debug Data
Exports current debug information to console (useful for bug reports).

### Visual Feedback

#### Animated Dashed Borders
When hovering over elements with outline display enabled, GuideNH draws animated white dashed borders around:
- The hovered element (full opacity)
- The parent element (30% opacity, when enabled)

The animation flows smoothly, making it easy to identify element boundaries.

#### Class Name Labels
A small black label box displays the simplified class name of the hovered element, positioned near the top-left corner of the element.

#### Cursor Dot
A green dot marks the exact mouse cursor position for precise coordinate reference.

## Element-Specific Information

Debug mode provides tailored information for different element types:

### Document Blocks
- Paragraph: Text preview
- Heading: Level and text preview
- Item Display: Item information
- Interactive Slot: Slot details

### Charts and Diagrams
- Bar/Column/Pie Charts: Chart data information
- Mermaid Mindmaps: Node count

### 3D Scenes
- Guidebook Scenes: Element count, scene structure

### Interactive Elements
- Links: Href target
- Anchors: Anchor name
- Buttons: Button state

## Configuration

All debug options can be configured via the mod config GUI or `guidenh.cfg`:

### Visual Settings
- `debugTextColor`: ARGB color for debug text (default: `0xB4287BDC`)
- `debugOutlineColor`: ARGB color for element outlines (0 to mirror text color)
- `debugCursorColor`: ARGB color for cursor dot (default: `0xCC00FF00`)
- `debugTextScale`: Scale factor for debug text (0.5-2.0, default: 0.8)
- `debugOutlineThickness`: Outline border thickness (0.5-5.0px, default: 1.0)

### Animation Settings
- `debugDashAnimationCycleMs`: Animation cycle duration (500-10000ms, default: 2000)
- `debugDashWidth`: Dash line width (0.5-5.0px, default: 1.0)
- `debugDashOnLength`: Visible dash segment length (1-20px, default: 4.0)
- `debugDashOffLength`: Gap segment length (1-20px, default: 4.0)

### Display Toggles
Individual toggles for each information type (hovered element, parent element, FPS, memory, mouse position, etc.)

## Performance

When debug mode is disabled (`guiDebugMode = false`), the system is optimized for **zero performance overhead**:
- No frame timing calculations
- No memory monitoring
- No element detection
- No rendering

All debug-related code paths use early-exit checks to ensure minimal impact on normal gameplay.

## Use Cases

### Content Creation
- Verify element positioning and sizing
- Debug layout issues
- Inspect scene element structure
- Test interactive element behavior

### Development
- Trace document tree hierarchy
- Inspect compiled page structure
- Monitor performance during page rendering
- Debug custom element implementations

### Bug Reporting
- Export document tree for issue reports
- Capture element information for reproduction
- Monitor performance metrics during issues

## Notes

- Debug mode persists across guide page navigation
- Settings are saved to the configuration file
- Tree output is logged to the Minecraft console/log file
- The debug overlay renders on top of all GUI elements

## Extensibility

### Custom Element Info Extractors

The debug system uses a registry-based architecture for element information extraction. Developers can register custom extractors for mod-specific element types:

```java
// Implement the DebugInfoExtractor interface
public class MyCustomExtractor implements DebugInfoExtractor {
    @Override
    public boolean canHandle(LytNode node) {
        return node instanceof MyCustomElement;
    }
    
    @Override
    public void extract(LytNode node, HoveredElementInfo info) {
        MyCustomElement element = (MyCustomElement) node;
        info.addExtraInfo("Custom Property: " + element.getProperty());
    }
    
    @Override
    public int getPriority() {
        return 50; // Higher priority = checked first
    }
}

// Register during mod initialization
DebugInfoExtractorRegistry.register(new MyCustomExtractor());
```

Built-in extractors are automatically registered and handle:
- Document structure (Document, Paragraph, Heading)
- Interactive elements (Slots, Buttons, Links)
- Charts (Bar, Column, Pie, Line, Scatter)
- Diagrams (Mermaid Mindmap)
- Media (Images, Code Blocks, Latex)
- Containers (Lists, Tables, Boxes)
- 3D Scenes (Guidebook Scenes)
- Generic blocks (fallback)
