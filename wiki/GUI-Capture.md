# GUI Capture

[简体中文](GUI-Capture-zh-CN.md)

Bind **Open GUI capture** in the GuideNH controls category. It starts unbound. Open a GUI and press the binding to enter the capture overlay. Settings and the toolbar are translucent; the overlay is excluded from exported images.

## Select and save

Drag a selection, release to fix it, and use its corners or edges to resize it. Snapping can target slots, components, windows, and supported mod layers. The hovered target can display its class name. Use the add-selection button or Ctrl while dragging to add another selection.

The default output is the rectangle enclosing all positive selections, including the space between them. Use the eraser to remove unwanted parts. Overlapping selections do not duplicate pixels. Separate-region export and horizontal, vertical, or grid arrangements are optional settings.

Use the save icon or Enter to save. The settings panel controls dimensions, output format, background, content layers, snapping, output folder, and resource budgets. World rendering is off by default; GUI models remain part of the selected GUI content. Tooltip, debug, NEI, and GUI backdrop inclusion are separate choices.

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

## Record and schedule

Choose GIF, a PNG sprite sheet, or a PNG frame sequence, configure the frame rate and duration or frame limit, and start recording. Pause, resume, stop, and cancel are available in the overlay. Sprite sheets include frame rectangles and timing in JSON. GIF transparency uses the configured alpha threshold or matte color.

The timer supports a delay, a local scheduled time, and repeated capture. The unavailable-target policy controls whether an unfocused or unavailable GUI waits, skips, or ends the schedule. The recording queue and temporary output have configurable budgets.

## Batch capture

Use the capture-all icon to review targets before starting. Visible layers, pages, and reachable GUIs are independent scope settings. Provider selection, popups, variants, navigation timeout, target limit, and restoration are configurable. Reachable server containers require the capture protocol and a valid server context; availability and failures appear in the target results.

Enable contact sheets to produce paged thumbnails with optional provider grouping. Enable scroll images to combine supported GuideNH, NEI, MUI1, or BetterQuesting scroll slices at their document positions. MUI1 supports nested page controls, horizontal and vertical scroll views, and lists; batch restoration restores their page choices and scroll positions. Scroll images retain a separate original viewport capture for fixed controls. The last overlapping slice replaces earlier pixels; it does not blend them twice.

The `betterquesting_pages` provider starts from the quest home, chapter reader, or quest detail screen. It visits accessible chapters and visible quests, including description, task, reward, and map scroll content. Map output follows actual component bounds, including negative coordinates; the large empty range allowed for manual panning is excluded. Hidden content stays excluded, and visible chapters with locked navigation have an unavailable result. Close modal BQ popups before navigating pages. Restoration preserves the original chapter, bookmark, zoom, scroll positions, and quest scroll-cache entries; it leaves unrelated cache entries intact. Capture does not detect or claim tasks, grant rewards, or enable editing.

`batch.json` lists target results, restoration, and composed outputs. Each scroll output includes `scroll.png` and `scroll.json` with the source crops, destination rectangles, pixel scale, and coverage. A target limit or missing slice leaves an explicitly partial result and uncovered space. Sheet and scroll outputs consume the remaining batch disk budget. Cancelled and failed unpublished compositions remove their temporary files.

## Integration

Geometry providers, GUI renderers, batch providers, and output exporters register through the capture interfaces. A scroll navigator can return `CaptureScrollGeometry` from `CaptureBatchNavigator.scrollGeometry`; the encoder operates on exported frames without importing a mod's GUI implementation. A container-derived screen that owns an independent update lifecycle can implement `StandaloneContainerScreen` and supply `updateStandaloneScreen`.

Compatibility depends on the installed mod versions and rendering backend. The complete shader/backend and mod acceptance matrix remains in progress.
