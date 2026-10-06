<p align="center">
    <img width="690" src="./src/main/resources/assets/logo.png" alt="GuideNH" style="image-rendering: pixelated;">
</p>
<hr>
<p align="center">
    <img src="https://img.shields.io/badge/Available%20for-MC%201.7.10-c70039" alt="Supported Minecraft Version">
    <img src="https://img.shields.io/badge/Forge-10.13.4.1614-f6a21a" alt="Supported Forge Version">
    <img src="https://img.shields.io/badge/license-LGPL--3.0-green" alt="License">
</p>

<p align="center">
    <a href="README.md">English</a> |
    <a href="README_zh-CN.md">简体中文</a>
</p>

## Introduction

GuideNH is an in-game guide framework for Minecraft 1.7.10 / Forge 10.13.4.1614. It ports and
extends the GuideME-style Markdown documentation system, so that mods and modpacks can ship rich
guide books as resource packs: navigation and search, an item index, Mermaid and LaTeX rendering,
interactive 3D scenes, and live in-game editing of guide pages.

The [GuideNH online editor](https://www.gtnewhorizons.com/GuideNH) can import, edit, and preview
guide folders or ZIP archives in a browser. See the
[GTNH contribution guidelines](https://github.com/GTNewHorizons/.github/blob/main/CONTRIBUTING.md)
for authoring guidance.

## Requirements

- Required dependency: [GTNHLib (>= 0.11.16)](https://github.com/GTNewHorizons/GTNHLib).

- JDK 8, 17, or 21 for the supported runtime matrix. The Gradle build itself runs on a JDK 25
  toolchain, which the wrapper provisions from the settings in `gradle/gradle-daemon-jvm.properties`.
- A Rust toolchain installed with [rustup](https://rustup.rs), needed only to build the layout
  engine native library.
- No separate Gradle installation: use the bundled wrapper (`gradlew`, `gradlew.bat`).
- Python 3, invoked as `py -3` on Windows, for the visual test tools.

## Build

```powershell
./gradlew build
```

The guide engine renders through a native library. Build it once before running the game:

```powershell
./gradlew buildRustNative
```

Build the library from the crate directory instead:

```powershell
cd src/rust/layout-engine
cargo build --release
```

`buildRustNative` compiles `src/rust/layout-engine` and the Gradle build copies the resulting
`guide_layout_engine.dll` into `src/main/resources/natives/`. That directory is a generated build
output and is not tracked, so the copy is what puts the library where the game loads it. When the
cargo target directory is redirected, the copy follows the resolved directory; let the Gradle task
do it rather than copying the file by hand.

## Run

```powershell
./gradlew runClient
./gradlew runServer
```

In game:

- Press `G` to open the guide home page.
- Hold `G` while hovering an indexed item to jump to its guide entry.
- Press `F3+T` to reload edited guide resources.


## Test

```powershell
./gradlew compileJava compileTestJava test
```

```powershell
cd src/rust/layout-engine
cargo test
```

## Visual Verification

Render the visual fixture pack headlessly. Each batch is limited to 40 pages and must be wrapped in
the render watchdog:

```powershell
py -3 test/visual/tools/render_watchdog.py --timeout 2400 --log <log> -- cmd /c "gradlew.bat runClient25 -Dguidenh.guide.sources=<repo>/test/visual/resourcepack -Dguidenh.headlessRender=true -Dguidenh.renderpage.guide=guidenh:guidenh -Dguidenh.renderpage.list=<list-file> -Dguidenh.renderpage.out=<shots> -Dguidenh.renderpage.width=900 -Dguidenh.renderpage.scale=2 -Dguidenh.renderpage.bounds=true"
```

Check the rendered bounds against the assertion ratchet:

```powershell
py -3 test/visual/tools/assert_bounds.py --shots run/client_new/<shots> --assertions test/visual/ratchet/assertions.json
```

Render output and client logs are written under `run/client_new/`. See [test/visual/README.md](test/visual/README.md)
for the fixture corpus, the ratchet, and the available tools.

## Wiki

- [English wiki](wiki/Home-en-US.md)
- [Chinese wiki](wiki/Home-zh-CN.md)
- [Getting started](wiki/Getting-Started.md)
- [Guide page format](wiki/Guide-Page-Format.md)
- [Structure export](wiki/Structure-Export.md)
- [Example resource pack](wiki/resourcepack)

## License

- Code: [LGPL-3.0](LICENSE.txt)
- Bundled third-party libraries keep their own licenses.

## Credits

Thanks to persephone for the icon textures. GuideNH is based on ideas from
[GuideME](https://github.com/AppliedEnergistics/GuideME), distributed under LGPL-3.0, and uses
open-source libraries including SnakeYAML, Apache Lucene, Apache Commons Lang, FlatBuffers Java, and
JLaTeXMath.

<a href="https://github.com/GTNewHorizons/GuideNH/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=GTNewHorizons/GuideNH&max=1000" alt="contributors" />
</a>

## Coding Agents

See [AGENTS.md](AGENTS.md) for the repository layout, the full command reference, and the comment and
documentation conventions.
