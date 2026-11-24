# GUI Capture

[简体中文](GUI-Capture-zh-CN.md)

GUI Capture allows you to select regions of an in-game interface and export them as images. It supports multiple selections, transparent areas, separate region exports, recording, scheduled capture, and batch capture.

Bind **Open GUI / HUD capture editor** in the GuideNH controls category. It is unbound by default. Press the binding while a GUI is open or during normal gameplay to enter the capture overlay. The toolbar and settings panel use a translucent layout, and the capture overlay itself is excluded from exported images.

Gameplay capture keeps the actual screen unset so mod HUDs and notifications retain their normal visibility conditions. The editor consumes input while active, then restores gameplay focus on exit. HUD inclusion is remembered separately for gameplay (enabled by default) and GUI capture. Late render-tick overlays and achievements follow the same HUD setting. Frozen previews retain their original pixels and magnetic targets.

## Select and Save

Drag to create a selection and release to fix it. Drag its corners or edges to resize it. Snapping supports slots, components, windows, HUD elements, and notifications. Widget metadata is supplemented by projected text and drawing bounds plus visible pixel regions, so custom HUDs can provide targets without individual mod adapters. The hovered target can display its class name.

Use the add-selection button or hold Ctrl while dragging to create additional selections. Use the eraser to remove unwanted areas.

By default, all positive selections are combined into their enclosing rectangle while preserving their original positions. Unselected gaps and erased areas remain transparent in the preview and exported image. Overlapping selections do not duplicate pixels.

The following output layouts are available:

- **Bounding rectangle:** Combines all positive selections while preserving their relative positions.
- **Separate regions:** Exports each selection independently.
- **Horizontal:** Arranges selections horizontally.
- **Vertical:** Arranges selections vertically.
- **Grid:** Arranges selections in a grid.

Click the save icon or press Enter to save. The settings panel controls output dimensions, format, background, content layers, snapping, output directory, and resource budgets.

World rendering is excluded by default. GUI models remain part of the selected GUI content. Tooltips, debug information, NEI, and the GUI backdrop can be configured independently.

### Capture Sources and Pixel Readers

Capture sources determine which rendering stage provides the image. Pixel readers convert capture buffers into exportable pixel data. The settings panel lists registered providers and their availability.

Capture sources support automatic selection and manual selection:

- Automatic selection uses an isolated GUI layer when world inclusion is disabled, and the opaque final frame when world inclusion is enabled.
- Both built-in sources honor content-layer settings.
- The final-frame source can also be selected explicitly while world inclusion is disabled.
- Unavailable providers display the reason they cannot be used.

Successful source or reader changes refresh the preview. Rejected selections preserve the current preview. The settings footer displays the implementations currently in use.

Recording and export retain snapshots of their capture source, pixel reader, and content settings until the task finishes. When provider capabilities change, idle resources are rebuilt and affected recordings are paused.

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

Output resolution is limited to 32 times the source size on each axis. GUIs that support safe redraw use tiled processing for large images.

For sources that cannot be safely redrawn, the selected quality policy determines whether native capture or resampling is permitted. Check the displayed quality and backend capabilities when choosing a source or backend.

### Content and Display Settings

GUI, HUD, player inventory, world, and tooltip inclusion can be configured independently.

Invert transparency grid is checked by default and uses a light checkerboard; unchecking it uses a dark checkerboard. This option changes only the preview background, without changing GUI lighting or exported pixels.

Hidden widgets and excluded content layers do not participate in snapping. Frozen tooltips retain the bounds collected during their actual rendering.

Cursor inclusion reads the current system cursor bitmap and hotspot through the native Windows or X11 API. System cursor rendering and inventory masking also apply to tiled GUI snapshots.

High-resolution HUD capture depends on safe redraw support:

- Strict quality mode rejects combinations that cannot be safely redrawn.
- Compatible output mode enlarges the complete native HUD/GUI frame.

### Overlay Controls

Use the visibility button to hide the toolbar. The button in the bottom-right corner restores it.

The capture interface visibility shortcut defaults to F6 and can be rebound in the controls settings. While capture is active, the original opening shortcut can also toggle the interface.

Additional display controls include:

- Temporarily hiding selection decorations.
- Revealing the complete capture source beneath the selection mask.

The settings panel supports Ctrl+F search, smooth scrolling, numeric sliders, and direct value entry.

## Recording and Scheduling

Recording supports the following output formats:

- GIF
- PNG sprite sheets
- PNG frame sequences

Before recording, configure the frame rate, duration, or maximum frame count. The overlay provides pause, resume, stop, and cancel controls.

PNG sprite sheets include a JSON file containing each frame's rectangle and timing information. GIF transparency can be processed using a configured alpha threshold or matte color.

### Scheduled Capture

Scheduled capture supports:

- **Delay:** Start capturing after a specified delay.
- **Local time:** Capture at a specified local time.
- **Repeat:** Capture repeatedly at a configured interval.

When the game loses focus or the target GUI becomes unavailable, the unavailable-target policy determines whether the task waits, skips the capture, or ends the schedule.

Recording queues and temporary output are subject to configurable resource budgets.

## Batch Capture

Click the capture-all icon to review available targets before starting a batch.

The following scope settings are independent:

- **Visible layers:** Determines which content layers participate in capture.
- **Pages:** Determines which pages and subpages are traversed.
- **Reachable GUIs:** Determines which GUI targets can be navigated to.

Provider selection, popup handling, variants, navigation timeout, target limits, and restoration behavior are configurable.

Some targets require specific mod integrations or server support. For example, reachable server containers require the capture protocol and a valid server context. Unavailable targets and capture failures display their reasons in the results.

### Contact Sheets

Enable contact sheets to generate paginated thumbnails of batch results. Results can optionally be grouped by provider.

Contact sheets share the remaining disk budget of the batch task. Unpublished temporary files are cleaned up when the task is cancelled or fails.

### Scroll Images

Enable scroll images to combine supported GuideNH, NEI, MUI1, and BetterQuesting scroll slices into complete images.

Slices are composed according to their document positions rather than simply concatenated.

MUI1 supports:

- Nested page tabs.
- Horizontal and vertical scroll views.
- Lists.

Batch capture restores the original page selections and scroll positions after navigation.

Fixed controls are retained in a separate original viewport capture. For overlapping slices, the last slice replaces previous pixels instead of blending transparency multiple times.

### BetterQuesting Page Capture

The `betterquesting_pages` provider supports starting from:

- The quest home screen.
- A chapter reader.
- A quest detail screen.

It traverses accessible chapters and visible quests, capturing descriptions, tasks, rewards, and map scroll content.

Map output follows actual component bounds, including negative coordinates. Large empty ranges intended for manual panning are excluded.

Capture behavior:

- Hidden content is excluded from the batch directory.
- Visible chapters with locked navigation are reported as unavailable.
- Target limits and missing slices produce explicitly partial results.

Before page navigation, close BetterQuesting modal popups.

Restoration preserves the original chapter, bookmark, zoom, scroll positions, and quest scroll-cache entries without clearing unrelated cache entries.

Capture does not detect or claim quests, grant rewards, or enable editing mode.

### Thaumcraft Page Capture

The `thaumcraft_pages` provider supports starting from the native research browser or recipe book.

It lists accessible categories and visible research, and opens completed research through the native recipe-book constructor.

Research pages follow the native construction process: concealed-text filtering and dynamic aspect pages are handled by the constructor before the actual two-page spread is expanded.

Capture behavior:

- Uncompleted visible research is reported as unavailable.
- Hidden research is excluded.
- Category map slices follow visible research bounds, native pan limits, and the clipped 224 × 196 map viewport.
- Scroll composition excludes fixed controls.
- Fixed controls remain in the category viewport capture.

Target limits are respected. Remaining map slices are reported when the limit is reached, and uncovered areas remain explicit in partial results.

Restoration preserves the original category, map interpolation and target position, browser bookmarks, and recipe history.

Capture does not unlock research or consume research resources.

### Batch Output Structure

`batch.json` records individual target results, restoration state, and composed outputs.

Each scroll image contains:

- `scroll.png`: The composed image.
- `scroll.json`: Source crops, destination rectangles, pixel scale, and coverage information.

When a target limit is reached or slices are missing, the result is explicitly marked as partial, with uncovered areas preserved in the coverage information.

Contact sheets and scroll images share the remaining disk budget of the batch task. Unpublished temporary compositions are cleaned up when a task is cancelled or fails.

## Developer Integration

GUI Capture provides extensible interfaces for registering geometry providers, GUI renderers, capture sources, pixel readers, batch providers, and output encoders.

### Registering Capture Sources and Readers

Register a `CaptureSourceProvider` using `CaptureRegistry.registerSource`, and a `CapturePixelReaderProvider` using `registerReader`.

Providers should report availability without allocating capture buffers. The capture pipeline opens their resources only when needed.

Automatic selection prioritizes the most recently registered available provider.

Context keys must include the screen, rendering context, or capability generation on which the provider's resources depend.

Capture sources receive paired GUI begin and finish callbacks.

### Capture Phases

`CaptureSource.capturePhase` specifies the rendering phase in which a capture source operates.

| Phase | Description |
| --- | --- |
| `WORLD_READY` | After world and shader-group rendering |
| `GUI_BEGIN` | When GUI rendering begins |
| `GUI_END` | After the screen renderer returns |
| `FRAME_READY` | After late overlays and the main framebuffer blit, before presentation submission |

The default phase is `FRAME_READY`.

The generic `capture` callback preserves compatibility with the legacy completed-frame callback.

Sources return viewport-sized pixel data at their selected phase. Early captures remain owned by the runtime until the presentation stage. Sources that do not redirect GUI rendering also receive the finish callback.

A `CapturePhaseListener` can observe the four ordered phase boundaries. Register listeners on the client thread using `CaptureRegistry.registerPhaseListener`.

`CaptureContext` carries the current phase and observed rendering state.

The isolated GUI layer includes late RenderTick End rendering, while the final-frame source reads the presentation back buffer.

`CaptureFrameIdentity.readback` records the capture phase, actual readback target, and immutable snapshots of all four phase boundaries.

### Selecting Capture Sources and Readers

On the client thread, with no active capture task, use the following methods to select implementations by ID:

- `CaptureRuntime.selectSource`
- `CaptureRuntime.selectReader`

A failed selection preserves the current preview. A successful selection invalidates the old preview and requests a new frame.

Readers receive an explicit source alpha mode, return straight ARGB frames, and release their resources when the session closes.

Native GUI layers and tiled exports use the same reader interface.

### Capture Identity and Batch Data

`CaptureFrameIdentity` describes the actual captured frame, including:

- The capture backend in use.
- Requested capture options.
- Captured content.
- Capture phase.
- Actual readback target.
- Immutable snapshots of the four rendering phases.

Export options freeze the IDs of the implementations actually used, preventing configuration changes during a task from affecting its results.

Batch results use `capture.source` and `capture.reader` to record capture origins, including retained visible-layer frames.

Batch data can also record:

- `capture.frame`
- `capture.phase`
- `capture.phases`
- `capture.backend`
- `capture.opengl`
- Framebuffer and buffer IDs.
- Viewport.
- Shadow-pass state.

### GUI Navigation and Scroll Capture

Client GUI navigation follows the screen actually opened after Forge initialization, including synchronous screen replacements such as Angelica video settings.

Cancelled screen openings are reported as failures.

A scroll navigator can return `CaptureScrollGeometry` through `CaptureBatchNavigator.scrollGeometry`. The encoder operates on exported frames without depending on a mod's GUI implementation.

A container-derived screen with an independent update lifecycle can implement `StandaloneContainerScreen` and provide `updateStandaloneScreen`.

### Angelica Backend Integration

Angelica registers the `angelica` pixel reader for its active `RenderBackend`.

It reads packed pixels through the backend API and restores pixel-pack settings and buffer bindings before converting them to straight ARGB.

When Angelica is loaded, automatic reader selection uses the corresponding backend implementation.

Explicitly selecting the `opengl` reader under SDL GPU reports an unavailable reason when that implementation cannot be used.

Capture source resource keys preserve both the underlying source context key and the actual backend identity.

Native and backend readers share buffer reuse, row orientation, and alpha conversion through `CaptureBufferPixelReader`.

The backend callback fills bottom-up ARGB words and restores any rendering state it modifies.

### Compatibility

Capture availability depends on the installed mod versions and rendering backend.

Different capture sources, pixel readers, and GUI integrations may have different quality levels and capability restrictions. Select configurations according to the availability and quality information displayed in the settings panel.

GUI Capture does not automatically change rendering backend or shader preferences.