# GUI Capture

[简体中文](GUI-Capture-zh-CN.md)

GUI Capture selects regions of an in-game interface and exports images. It supports transparency, multiple selections, recording, scheduled capture, and batch capture.

## Open the Capture Editor

Bind **Open GUI / HUD capture editor** in the GuideNH controls category. The binding is unset by default and works while a GUI is open or during gameplay.

The capture toolbar is excluded from exported images. The editor handles input while open and restores game focus on exit. Gameplay and GUI capture remember HUD inclusion separately; gameplay capture includes the HUD by default.

## Select and Save

Drag to create a selection and release to fix it. Drag its edges or corners to resize it. Use the add-selection button or hold Ctrl while dragging to add selections. Use the eraser to remove unwanted areas.

Snapping supports slots, components, windows, HUD elements, and notifications. Hidden controls and excluded content layers do not participate in snapping.

The default output combines selections into their enclosing rectangle while preserving relative positions. Unselected gaps and erased areas remain transparent, and overlapping selections do not duplicate pixels.

| Output layout | Purpose |
| --- | --- |
| Bounding rectangle | Combine selections and preserve their positions |
| Separate regions | Export each selection separately |
| Horizontal | Arrange selections horizontally |
| Vertical | Arrange selections vertically |
| Grid | Arrange selections in a grid |

Click the save icon or press Enter to save. Settings control the output directory, dimensions, format, background, content layers, snapping, and resource budgets.

### Keyboard Shortcuts

| Key | Action |
| --- | --- |
| Enter | Save; stop an active recording |
| Esc | Close a panel, cancel the current drag, or exit capture |
| Delete | Delete the active selection |
| Ctrl+Z / Ctrl+Y | Undo / redo selection edits |
| Ctrl+D | Duplicate the active selection |
| Ctrl+A | Select the entire GUI viewport |
| Arrow keys | Move the active selection by one GUI pixel |
| Shift+arrow keys | Move by ten GUI pixels |
| Ctrl+arrow keys | Resize the active selection |
| Space during recording | Pause / resume |

### Content and Display

GUI, HUD, player inventory, world, tooltips, NEI, debug information, and the GUI backdrop can be included separately. The world behind the GUI is excluded by default; models inside the GUI count as GUI content. Cursor inclusion preserves the system cursor image and hotspot.

**Invert transparency grid** is enabled by default and uses a light checkerboard. Disable it for a dark checkerboard. It changes the preview background only.

Each capture session opens with the toolbar visible. Press the opening shortcut again or use the visibility button to hide or restore it; the bottom-right eye button remains available while hidden. Selection-decoration visibility and complete-source preview are remembered separately. Settings support Ctrl+F search, sliders, direct numeric entry, and smooth scrolling.

### Capture Sources and Output Quality

Settings list available capture sources and pixel readers, with the active implementations shown in the footer. Automatic source selection uses the isolated GUI layer when world inclusion is disabled and the final frame when enabled. Both honor content-layer settings.

Select a source or reader manually to refresh the preview. Unavailable choices display a reason. Recording and export retain the settings chosen at task start; a recording pauses if a capability change makes its current configuration unavailable.

Output resolution is limited to 32 times the source size on each axis. Interfaces with safe redraw support can export tiled high-resolution images. Automatic quality permits resampling the frozen frame when redraw does not match the preview. Strict quality reports mismatches or unsupported combinations. Compatible output can enlarge the native HUD / GUI frame. Choose settings according to the displayed quality and backend capabilities.

## Recording and Scheduled Capture

Recording supports GIF, PNG sprite sheets, and PNG frame sequences. Set the frame rate, duration, or maximum frame count before starting; pause, resume, stop, or cancel while recording.

PNG sprite sheets include JSON with frame positions and timing. GIF transparency can use a configured threshold or background color.

Schedules support a delay, a specified local time, and repeating intervals. If the game loses focus or the target becomes unavailable, choose whether to wait, skip, or end the task. Recording queues and temporary output follow the configured resource budgets.

## Batch Capture

Click **Capture all GUIs**, review the target list, and select the content to capture before starting.

Visible layers, pages and subpages, and reachable GUI scopes are independent. Provider selection, popup handling, variants, navigation timeout, target limits, and restoration behavior are configurable. Capture waits for the interface, page, inventory synchronization, and scroll position to become ready; continuing animation does not block a ready target.

Some targets require mod integration or the server capture protocol. Unavailable targets and failures show their reasons. The server area accepts two inclusive corners in the current dimension as `x,y,z:x,y,z`; leave it empty to use the radius. Only loaded targets inside the area are enumerated.

### Supported Targets

- Owned written and writable books have read-only previews; enabling Pages includes their remaining pages.
- Single and double chests and idle or burning furnaces have independent previews.
- Loaded adult villagers, tamed adult horses, and chest or hopper minecarts require server support and obey container ownership and distance limits.
- Guide pages use a separate read-only view, preserving the original editor, draft, text selection, and undo history.

### Contact Sheets and Scroll Images

Contact sheets arrange batch results into paginated thumbnails, optionally grouped by provider.

Scroll images support GuideNH, NEI, MUI1, and BetterQuesting interfaces. Slices follow document positions; fixed controls remain in the original viewport capture. MUI1 supports nested tabs, horizontal and vertical scroll controls, and lists. Tabs and scroll positions are restored afterwards.

### BetterQuesting Pages

`betterquesting_pages` starts from the quest home screen, chapter reader, or quest detail screen. It traverses accessible chapters and visible quests and captures descriptions, tasks, rewards, and maps. Close modal popups before navigation.

Hidden content is excluded, and locked visible chapters are marked unavailable. Map bounds follow actual components and support negative coordinates. The original chapter, bookmarks, zoom, and scroll state are restored. Capture does not claim quests, grant rewards, or enable editing mode.

### Thaumcraft Pages

`thaumcraft_pages` starts from the research browser or recipe book. It lists accessible categories and visible research and captures the actual two-page spreads of completed research.

Hidden research is excluded, and incomplete visible research is marked unavailable. Category maps use visible research bounds and native pan limits within the 224 × 196 map area. Categories, map positions, and recipe history are restored. Capture does not unlock research or consume research resources.

### Output Files

`batch.json` records target results, restoration state, and composed outputs. Scroll images include `scroll.png` and `scroll.json`; the latter records crops, destination rectangles, pixel scale, and coverage.

Results are marked partial when target limits or missing slices leave uncovered areas. Contact sheets and scroll images share the batch task's remaining disk budget. Cancellation or failure cleans up unpublished temporary output.

## Developer Integration

Capture interfaces support geometry providers, GUI renderers, capture sources, pixel readers, batch providers, and output encoders independently.

| API | Purpose |
| --- | --- |
| `CaptureRegistry.registerSource` | Register a `CaptureSourceProvider` |
| `CaptureRegistry.registerReader` | Register a `CapturePixelReaderProvider` |
| `CaptureRegistry.registerPhaseListener` | Register a phase listener |
| `CaptureRuntime.selectSource` / `selectReader` | Select an implementation by ID |
| `CaptureBatchReadiness` | Declare batch target readiness |
| `CaptureBatchNavigator.scrollGeometry` | Provide `CaptureScrollGeometry` |
| `StandaloneContainerScreen` | Provide `updateStandaloneScreen` for an independently updated container screen |

Registration and implementation selection run on the client thread. Selection requires no active capture task. Providers report availability without allocating buffers; resource context keys include screen identity, rendering context, and capability generation. Automatic selection prioritizes the last registered available provider.

### Capture Phases and Resource Lifecycle

`CaptureSource.capturePhase` selects the phase, defaulting to `FRAME_READY`.

| Phase | Description |
| --- | --- |
| `WORLD_READY` | After world and shader-group rendering |
| `GUI_BEGIN` | When GUI rendering begins |
| `GUI_END` | After the screen renderer returns |
| `FRAME_READY` | After late overlays and the main framebuffer blit, before presentation |

Sources receive paired GUI begin and finish callbacks and return viewport-sized pixels. `CaptureContext` carries the phase and rendering state. `CaptureFrameIdentity` records the backend, options, actual content, readback target, and four phase snapshots. Batch output retains provenance through `capture.source`, `capture.reader`, and frame identity fields.

`CapturePixelReader.enqueue` and `CaptureLayerSource.enqueueGuiLayer` return caller-owned `CaptureReadback` handles. Poll, consume, and release each handle on its submitting render thread. Consume it once and close it on cancellation. Readers return straight ARGB and release resources when the session closes.

Snapshots that already apply content filtering declare `filtersContent()`. Batch readiness revisions describe prepared content; `CaptureBatchReadiness.viewRevision(context.capture())` supplies a base screen and viewport revision.

### Backends and Compatibility

Angelica registers the `angelica` reader for the active `RenderBackend`, and automatic selection prefers it. Backend callbacks fill bottom-up ARGB values and restore modified pixel-pack settings and buffer bindings. `CaptureBufferPixelReader` supplies shared buffer reuse, row orientation, and alpha conversion.

Custom shaders and drawing paths unsupported by generic redraw require a dedicated snapshot provider. Availability and quality depend on mod versions and the backend. Capture does not automatically change rendering backend or shader preferences.
