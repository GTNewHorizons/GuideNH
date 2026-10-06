# GuideNH Visual Test System

A fixture-driven visual regression system for the GuideNH guide engine. A curated corpus of guide
pages is rendered headlessly into screenshots and layout bounds, the results are inspected, and an
executable assertion ratchet locks the accepted geometry in place, so a later layout change cannot
silently move a page.

## Contents

| Path | Responsibility |
|---|---|
| `resourcepack/` | The fixture resource pack. Its pages are the render corpus, grouped by feature area: text, layout, floats, overflow, mermaid, latex, tables, charts, images, code, scenes, meta, nei, and stress. Pages live under `assets/guidenh/guidenh/_en_us/visualtest/`. |
| `ratchet/assertions.json` | The assertion ratchet. Each entry is keyed by page id and asserts class counts, node existence, or bounds read from the render bounds JSON. The file is append-only. |
| `tools/` | The Python toolset: the render watchdog, the bounds ratchet runner, the screenshot freshness check, the client log scanner, and optional image screening helpers. |

The pages under `resourcepack/` double as the corpus and as examples of the guide format. Fixture
payloads are intentionally allowed to contain non-English text, including CJK, because glyph
coverage and mixed-script line breaking are part of what the corpus verifies.

Detailed working documents that describe the internal visual loop, its ledgers, and its
adjudication history are maintained locally as research material and are not distributed with this
repository.

## Quick Start

1. Render the corpus headlessly. A render batch takes a list file with one page id per line, in the
   form `guidenh:visualtest/<path>.md`. Keep each batch to at most 40 pages, and always wrap it in
   the render watchdog so a hung client cannot block the run:

```powershell
py -3 test/visual/tools/render_watchdog.py --timeout 2400 --log <log> -- cmd /c "gradlew.bat runClient25 -Dguidenh.guide.sources=<repo>/test/visual/resourcepack -Dguidenh.headlessRender=true -Dguidenh.renderpage.guide=guidenh:guidenh -Dguidenh.renderpage.list=<list-file> -Dguidenh.renderpage.out=<shots> -Dguidenh.renderpage.width=900 -Dguidenh.renderpage.scale=2 -Dguidenh.renderpage.bounds=true"
```

2. Run the layout harness as a verification gate. It must report a total issue count of 0:

```powershell
./gradlew compileJava compileTestJava test runLayoutDump
```

3. Check the rendered bounds against the ratchet. It must exit 0:

```powershell
py -3 test/visual/tools/assert_bounds.py --shots run/client_new/<shots> --assertions test/visual/ratchet/assertions.json
```

`runClient25` is used because the render driver runs on the JDK 25 toolchain. Screenshots, bounds
JSON, and overlay images are written under `run/client_new/`, and client logs under
`run/client_new/logs/`. `py -3` is the Windows launcher for Python 3; on other platforms use
`python3`.

## Environment

- Python 3. The bounds ratchet runner, the freshness check, the log scanner, and the watchdog use
  the standard library only.
- Pillow, needed only by the screening subcommands of `tools/screen.py`.
- An API key for the screening helpers, copied from `tools/.env.example` to `tools/.env`. The
  `tools/.env` file holds credentials and is never tracked.

## Tracked And Untracked Material

Tracked in this directory: the fixture resource pack, `ratchet/assertions.json`, the toolset under
`tools/`, and this file.

Not tracked: render output and logs under `run/`, local scratch space under `test/visual/local/`,
screening credentials in `tools/.env`, and the local working documents mentioned above.
