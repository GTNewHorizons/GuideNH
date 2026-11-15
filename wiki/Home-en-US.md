[中文](Home-zh-CN)

# GuideNH

GuideNH supports reading and authoring guides in Minecraft 1.7.10. This reference covers pages, assets, recipes, scenes and localization for guide authors.

## Author reference

- [Installation](Installation)
- [Getting Started](Getting-Started)
- [Guide Page Format](Guide-Page-Format)
- [Syntax Completion](Syntax-Completion)
- [Tags Reference](Tags-Reference)
- [Navigation](Navigation)
- [Search](Search)
- [Images And Assets](Images-And-Assets)
- [Recipes](Recipes)
- [GameScene](GameScene)
- [Annotations](Annotations)
- [Ponder](Ponder)
- [Structure Export](Structure-Export)
- [Templates](Templates)
- [Localization](Localization)
- [Mod Compatibility](Mod-Compatibility)
- [Examples](Examples)
- [FAQ](FAQ)

## Where to write

| Path | Purpose |
| --- | --- |
| `config/guidenh/DefaultGuide/<modid>/guidenh/` | Client-local guides; UTF-8 Markdown belongs in language folders |
| `wiki/resourcepack/assets/<modid>/guidenh/` | Repository example resource pack; load it separately or use live preview |
| `wiki/*.md` | GitHub Wiki documentation; show game syntax in code fences |

Create your first page with [Getting Started](Getting-Started). The example `index.md` lists examples by category; `scene-import.md` demonstrates structure imports.

## Development and deployment

- [Live Preview](Live-Preview)
- [Debug Mode](Debug-Mode)
- [Server Integration](Server-Integration)
- [Developer Integration](Developer-Integration)
