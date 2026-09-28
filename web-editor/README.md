# GuideNH Web Editor

GuideNH Web Editor is a small Node.js service and browser workspace for editing and previewing GuideNH source pages. It accepts a GuideNH source directory selected through the browser directory picker or a ZIP archive.

## Run locally

```powershell
npm install
npm start
```

Open `http://localhost:8787`. The service also exposes `GET /api/health`.

The Syntax button opens a searchable English/Chinese reference built from the bundled wiki pages. After editing those wiki pages, run `npm run sync:reference` from `web-editor` to update the deployed copies. The reference is included in the Docker image.

The selected file is stored in the `page` URL parameter, so refreshing or using browser Back/Forward restores the page when the project is present in that browser's local storage.

The Preview toolbar includes a persisted Sync scroll switch. Markdown headings anchor bidirectional scrolling between the editor and preview; pages without matching headings use proportional scrolling. HTML source pages synchronize with their isolated iframe through scroll-position messages. The light and dark themes provide separate default colors for preview content and visualizations while preserving colors explicitly set by a guide page.

## Run with Docker

```powershell
docker compose up --build -d
```

The browser keeps project snapshots in IndexedDB and falls back to localStorage when IndexedDB is unavailable. The server does not receive document contents during normal editing.

## Import behavior

- A directory import reads UTF-8 text files and common image/font assets.
- A ZIP import reads stored and deflate compressed entries without an external browser library.
- Repository folders ignore build output, logs, `.git`, `.gradle`, and `node_modules` entries.
- Common image, font, audio, and video assets are retained as UTF-8-safe data URLs.
- HTML source pages are previewed in a sandboxed frame with local relative assets inlined when available.
- Markdown pages keep runtime-only values visible as placeholders: scenes use a framed runtime surface, item and block tags remain inline content, and hidden IDs use a muted italic treatment.
- Tables, CSV, code fences, footnotes, Mermaid mindmaps, charts, function graphs, file trees, tabs, floating images, annotations, and media tags are rendered in the browser preview.
- Inline `<CsvTable>` bodies are supported in addition to page-relative CSV imports. HTML flow tags (`p`, `div`, `table`, lists, headings, and inline emphasis/code tags) are preserved in the Markdown preview.
- Mermaid mindmaps support fixed viewports, explicit node positions, and `NodeContent` panels. Cartesian charts support horizontal bars, line overlays, and `PieInset` combinations.
- Chart axes, units, labels, value labels, corner legends, hover targets, and function-graph sampling markers are rendered from the documented attributes.

`ItemLink`, `ItemImage`, and `ItemIcon` are rendered as inline GuideNH content. `ItemLink` uses dotted underlining and does not become a button.
