# GUI Capture

[简体中文](GUI-Capture-zh-CN.md)

Bind **Open GUI capture** in the GuideNH controls category. It starts unbound. Open a GUI and press the binding to enter the capture overlay. Settings and the toolbar are translucent; the overlay is excluded from exported images.

## Select and save

Drag a selection, release to fix it, and use its corners or edges to resize it. Snapping can target slots, components, windows, and supported mod layers. The hovered target can display its class name. Use the add-selection button or Ctrl while dragging to add another selection.

The default output is the rectangle enclosing all positive selections, including the space between them. Use the eraser to remove unwanted parts. Overlapping selections do not duplicate pixels. Separate-region export and horizontal, vertical, or grid arrangements are optional settings.

Use the save icon or Enter to save. The settings panel controls dimensions, output format, background, content layers, snapping, output folder, and resource budgets. World rendering is off by default; GUI models remain part of the selected GUI content. Tooltip, debug, NEI, and GUI backdrop inclusion are separate choices.

The source and pixel-reader settings include every registered provider. Automatic source selection uses an isolated GUI layer while world inclusion is off, and the opaque final frame while it is on. An explicit incompatible source reports its reason. Successful changes refresh the preview; rejected choices preserve it. The settings footer shows the implementations currently in use. Recording and export keep their source, reader, and content snapshots until the task ends. Capability changes rebuild idle resources and pause an affected recording.

| Key | Action |
| --- | --- |
| Enter | Save; stop an active recording |
| Esc | Close a panel, cancel the current drag, or leave capture |
| Delete | Delete the active selection |
| Ctrl+Z / Ctrl+Y | Undo / redo selection edits |
| Ctrl+D | Duplicate the active selection |
| Ctrl+A | Select the full GUI viewport |
| Arrow keys | Move the active selection by one GUI pixel |
| Shift+arrow keys | Move by ten GUI pixels |
| Ctrl+arrow keys | Resize the active selection |
| Space during recording | Pause / resume |

Resolution is limited to 32 times the source size per axis. True GUI redraw uses tiles for large output. The selected quality policy determines whether a source that cannot be redrawn may use native capture or resampling. Read the displayed quality and capability information when choosing a source or backend.

HUD and cursor inclusion are independent options, both off by default. HUD capture starts the selected GUI layer before normal HUD drawing and also handles the Angelica cached-HUD entry. Frozen previews keep the source pointer for GUI drawing and Forge draw events; the exported software cursor uses the existing monochrome capture atlas. Cursor drawing is included in safe tiled GUI snapshots. Safe high-resolution HUD redraw remains pending: strict quality currently rejects that combination, while compatible output enlarges the complete native HUD/GUI frame.

## Record and schedule

Choose GIF, a PNG sprite sheet, or a PNG frame sequence, configure the frame rate and duration or frame limit, and start recording. Pause, resume, stop, and cancel are available in the overlay. Sprite sheets include frame rectangles and timing in JSON. GIF transparency uses the configured alpha threshold or matte color.

The timer supports a delay, a local scheduled time, and repeated capture. The unavailable-target policy controls whether an unfocused or unavailable GUI waits, skips, or ends the schedule. The recording queue and temporary output have configurable budgets.

## Batch capture

Use the capture-all icon to review targets before starting. Visible layers, pages, and reachable GUIs are independent scope settings. Provider selection, popups, variants, navigation timeout, target limit, and restoration are configurable. Reachable server containers require the capture protocol and a valid server context; availability and failures appear in the target results.

Enable contact sheets to produce paged thumbnails with optional provider grouping. Enable scroll images to combine supported GuideNH, NEI, MUI1, or BetterQuesting scroll slices at their document positions. MUI1 supports nested page controls, horizontal and vertical scroll views, and lists; batch restoration restores their page choices and scroll positions. Scroll images retain a separate original viewport capture for fixed controls. The last overlapping slice replaces earlier pixels; it does not blend them twice.

The `betterquesting_pages` provider starts from the quest home, chapter reader, or quest detail screen. It visits accessible chapters and visible quests, including description, task, reward, and map scroll content. Map output follows actual component bounds, including negative coordinates; the large empty range allowed for manual panning is excluded. Hidden content stays excluded, and visible chapters with locked navigation have an unavailable result. Close modal BQ popups before navigating pages. Restoration preserves the original chapter, bookmark, zoom, scroll positions, and quest scroll-cache entries; it leaves unrelated cache entries intact. Capture does not detect or claim tasks, grant rewards, or enable editing.

The `thaumcraft_pages` provider starts from the native research browser or recipe book. It lists accessible categories and visible research, opens completed research through the native recipe-book constructor, and expands actual two-page spreads after the constructor applies concealed-text filtering and dynamic aspect pages. Uncompleted visible research remains unavailable; hidden research is excluded. Category map slices follow visible research bounds, native pan limits, and the clipped 224 by 196 map viewport. Scroll composition excludes fixed controls, retained in the category viewport capture. Target limits report remaining map slices and leave partial coverage explicit. The original category, map interpolation/target position, browser bookmarks, and recipe history are restored; capture does not unlock research or spend research resources. Implementation is present; its complete acceptance remains deferred.

`batch.json` lists target results, restoration, and composed outputs. Each scroll output includes `scroll.png` and `scroll.json` with the source crops, destination rectangles, pixel scale, and coverage. A target limit or missing slice leaves an explicitly partial result and uncovered space. Sheet and scroll outputs consume the remaining batch disk budget. Cancelled and failed unpublished compositions remove their temporary files.

## Integration

Geometry providers, GUI renderers, sources, pixel readers, batch providers, and output exporters register through the capture interfaces. Register `CaptureSourceProvider` with `CaptureRegistry.registerSource` and `CapturePixelReaderProvider` with `registerReader`. Providers report availability without allocating capture buffers; the pipeline opens their resources when needed. Automatic selection prefers the most recently registered available provider. Context keys must include the screen, rendering context, or capability generation on which a provider's resources depend. Sources receive paired GUI begin/finish callbacks. `CaptureSource.capturePhase` selects `WORLD_READY`, `GUI_BEGIN`, `GUI_END`, or `FRAME_READY`; its default is `FRAME_READY`. The generic `capture` callback preserves the legacy completed-frame callback. Sources return viewport-sized pixels at their selected phase, and early captures remain owned by the runtime until presentation. The finish callback also reaches sources that do not redirect GUI drawing.

On the client thread, `CaptureRuntime.selectSource` and `selectReader` select implementations by ID while no capture task is active. Failed selection preserves the current preview; a successful switch invalidates it and requests a fresh frame. Readers receive an explicit source alpha mode, return straight ARGB frames, and release their resources when the session closes. Native layers and tiled exports use the same reader interface. `CaptureFrameIdentity` records the actual backend, requested choices, and captured content; export options freeze the actual implementation IDs. Batch results include `capture.source` and `capture.reader`, including the identity of a retained visible-layer frame.

Register a client-thread `CapturePhaseListener` with `CaptureRegistry.registerPhaseListener` to observe the four ordered boundaries. `CaptureContext` carries the phase and observed render state. `WORLD_READY` follows world/shader-group rendering; `GUI_END` follows the screen renderer; `FRAME_READY` follows late overlays and the main framebuffer blit, before presentation submission. The isolated GUI source includes late RenderTick End drawing; the final-frame source reads the presentation back buffer. `CaptureFrameIdentity.readback` contains the captured phase, actual read target, and immutable snapshots from all four boundaries. Batch data records `capture.frame`, `capture.phase`, `capture.phases`, `capture.backend`, `capture.opengl`, framebuffer/buffer IDs, viewport, and shadow-pass state. Hardware PBO/fence capability flags describe hardware support, not an implemented asynchronous reader.

Client GUI navigation follows the screen actually opened by Forge initialization, including synchronous replacements such as Angelica video settings. Cancelled openings report failure.

A scroll navigator can return `CaptureScrollGeometry` from `CaptureBatchNavigator.scrollGeometry`; the encoder operates on exported frames without importing a mod's GUI implementation. A container-derived screen that owns an independent update lifecycle can implement `StandaloneContainerScreen` and supply `updateStandaloneScreen`.

Compatibility depends on the installed mod versions and rendering backend. The complete shader/backend and mod acceptance matrix remains in progress.

Angelica registers the `angelica` pixel reader for its active `RenderBackend`. It reads packed pixels through the backend API and restores pixel-pack settings and buffer binding before converting to straight ARGB. Automatic reader selection uses it when Angelica is loaded. The explicit `opengl` reader reports an unavailable reason on SDL GPU. Source resource keys preserve both the underlying source key and backend context identity. Native and backend readers share buffer reuse, row orientation, and alpha conversion through `CaptureBufferPixelReader`; its backend callback fills bottom-up ARGB words and restores any state it changes.

The direct backend reader has passed OpenGL acceptance. SDL GPU Vulkan initialized in the development environment with temporary shaderc/spvc startup libraries, but its capture acceptance was stopped when verification was deferred until all implementation tasks are finished. Non-GL capture, shader packs, and the specified Prism instance remain unverified. No rendering backend or shader preference is automatically changed.
