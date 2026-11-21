# GuideNH Web Editor

A browser workspace for editing and previewing GuideNH pages. Import a source directory or ZIP archive, edit UTF-8 Markdown and download the updated project. The hosted editor is available at https://www.gtnewhorizons.com/GuideNH.

## Run locally

Use Node.js 20 or newer. From `web-editor`:

```powershell
npm install
npm start
```

Open `http://localhost:8787`. For Docker, run `docker compose up --build -d`. The health endpoint is `GET /api/health`.

## Editing

- Import a directory or ZIP with its local assets. Repository imports skip `.git`, build output, logs, `.gradle` and `node_modules`.
- Select a page and edit beside its preview. The selected page is retained in the URL; browser Back/Forward and refresh restore it while the project is stored locally.
- Enable Sync scroll to follow headings between source and preview. Pages without matching headings use proportional scrolling.
- Use the Syntax button to search the bundled author reference and follow links to related sections. Topics outside the bundled reference open in the GitHub Wiki.
- Download your work before clearing browser data. Projects are stored in the browser; normal editing does not upload page contents to the server.

## Preview coverage

Markdown tables, code, footnotes, Mermaid mindmaps, charts, function graphs, file trees, tabs, annotations and media have browser previews. Relative image/font/audio/video assets are retained on import; HTML pages use an isolated frame.

Item and fluid tags remain inline content. Registry-backed values, recipe data and interactive game scenes require the running game for full validation and rendering; offline placeholders do not establish whether a registry name exists.

## Maintaining the reference

Author references live in `../wiki/` and `public/reference/<language>/<region>/`. Keep matching Wiki and Web pages synchronized. Complete translations remain localized; pages with a localized overview and an English detailed reference identify that fallback explicitly.

Translate new and revised guidance into every applicable reference language, including headings, table descriptions, link labels and visible example text. Preserve syntax tokens and target IDs. Insert each addition in its owning topic before related references; completion pages describe editor behavior, tag pages document attributes, and developer integration pages document Java registration. Existing English fallback notices do not waive translation of new content. Live examples belong beside related examples, and the in-game reference is packaged from `public/reference/`.

After adding or removing locale/reference files, run:

```powershell
npm run sync:locales
```

This command updates locale and document indexes; it does not copy Wiki content. Bundled references are included in the Docker image.
