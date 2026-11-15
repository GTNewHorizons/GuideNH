[中文](Installation-zh-CN)

# Installation

Install GuideNH for Minecraft 1.7.10 and GTNHLib in the client's `mods/` directory. World structure placement and administrator commands also require GuideNH on the server; see [Server Integration](Server-Integration).

## Adding a local guide

The client creates an empty `config/guidenh/DefaultGuide/` directory at startup. Put UTF-8 pages in language folders and shared resources in the guide's `assets/` folder:

```text
config/guidenh/DefaultGuide/
`-- <modid>/
    `-- guidenh/
        |-- assets/
        |   `-- example_structure.snbt
        |-- _en_us/
        |   `-- index.md
        `-- _zh_cn/
            `-- index.md
```

Press `G` to open the guide and `F3+T` to reload added or edited resources. See [Getting Started](Getting-Started) for a first page.

## Using resource packs

Normal resource packs use `assets/<modid>/guidenh/`. `config/guidenh/DefaultGuide.zip` accepts the same full resource-pack layout and takes precedence when the directory and ZIP both exist:

```text
config/guidenh/DefaultGuide.zip
`-- assets/
    `-- <modid>/
        `-- guidenh/
            |-- assets/
            |   `-- example_structure.snbt
            |-- _en_us/
            |   `-- index.md
            `-- _zh_cn/
                `-- index.md
```

## Editing repository examples

Example pages are in `wiki/resourcepack/assets/`. Load `wiki/resourcepack/` as a resource pack, or use the JVM arguments in [Live Preview](Live-Preview) to load that folder directly. Building the mod does not bundle these examples.
