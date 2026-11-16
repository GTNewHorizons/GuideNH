# Live Preview

GuideNH supports a development-mode live preview flow for guide content. When enabled, GuideNH reads pages and assets
directly from a local folder and reloads changes in-game without requiring a full restart.

## What It Is For

Use live preview when you are actively editing runtime guide pages under `wiki/resourcepack/` and want a tight
authoring loop.

This is especially useful for:

- markdown edits
- navigation changes
- page-local images
- guide-root assets such as imported structures
- Ponder JSON files referenced by `<ImportPonder>`

## Supported System Properties

| Property | Meaning |
| --- | --- |
| `guideme.<guide_namespace>.<guide_path>.sources` | local folder used as the development source root |
| `guideme.<guide_namespace>.<guide_path>.sourcesNamespace` | optional namespace override for files loaded from that folder |
| `guideme.resourcePacks.sources` | standard resource-pack root to load and watch as an ordered development overlay; may be repeated |
| `guideme.resourcePack.sources` | alias of `guideme.resourcePacks.sources`; may be repeated |
| `guideme.showOnStartup` | optional guide or page to open automatically on the title screen |
| `guideme.validateAtStartup` | optional comma-separated guide ids to validate once on startup |

## GuideNH Repository Example

For the built-in example guide in this repository:

- guide id: `guidenh:guidenh`
- development source root: `wiki/resourcepack/assets/guidenh/guidenh`

The matching system property is:

```text
guideme.guidenh.guidenh.sources=<absolute-path-to-repo>/wiki/resourcepack/assets/guidenh/guidenh
```

GuideNH also supports broader development source roots:

| Source root | Page id mapping |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh` | content-root mode; files map through `sourcesNamespace`, defaulting to `guidenh` |
| `wiki/resourcepack/assets` | assets-root mode; `assets/<modid>/guidenh/...` maps to `<modid>:...` |
| `wiki/resourcepack` | resourcepack-root mode; `assets/<modid>/guidenh/...` maps to `<modid>:...` |

Each namespace has its own guide. Navigation combines the loaded guides, while page IDs preserve their namespace: `gregtech:index.md` and `appliedenergistics2:index.md` remain separate pages.

## Multi-Pack Live Preview

Use `guideme.resourcePacks.sources` when you want one or more folders to behave like normal resource packs and be
watched for all GuideNH content under `assets/<modid>/guidenh/...`.

```text
-Dguideme.resourcePacks.sources=E:/packs/base
-Dguideme.resourcePacks.sources=E:/packs/override
```

Repeat the property to load multiple source folders in the listed order.

Conflict rules are resource-pack-like but deterministic for live preview:

- different mod ids are isolated by namespace, so `gregtech:index.md` and `appliedenergistics2:index.md` are separate pages
- pages at the same namespace, language and path follow `navigation.priority`: higher values win, with later scanned packs winning ties
- configured development packs are scanned before regular packs, so an existing regular pack can replace a development page at equal priority
- assets from development packs use the first configured folder containing that asset

For a guide source that should directly override its resource-pack pages, use `guideme.<namespace>.<path>.sources` instead of the multi-pack setting.

Changing markdown, Ponder JSON, images, or other files under these development resource-pack roots triggers a client
resource reload so the merged guide and scene content refresh together.

## `showOnStartup` Formats

`guideme.showOnStartup` supports these forms:

- `guidenh:guidenh`
- `guidenh:guidenh!index.md`
- `guidenh:guidenh!index.md#anchor`

Relative page ids after `!` resolve against the guide namespace, so `index.md` becomes `guidenh:index.md`.

## Reload timing

Changes are checked once every 20 client ticks, about one second at normal tick speed. Pages and assets inside configured source folders can reload while the client runs.

## Recommended Workflow

For the repository examples, replace the path below with your checkout and add this JVM argument to the game launch configuration:

```text
-Dguideme.guidenh.guidenh.sources=E:/Github/GuideNH-NH/wiki/resourcepack/assets/guidenh/guidenh
```

Optionally add `-Dguideme.showOnStartup=guidenh:guidenh!index.md` to open the example guide automatically. JVM arguments must reach the game process; setting a property only on the Gradle process does not configure its forked client.

1. Add the source-folder JVM arguments to your launcher and start the client.
2. Edit runtime guide files under `wiki/resourcepack/assets/...`.
3. Wait about one second for the change to reload.
4. Reopen or revisit the page if you changed navigation structure or startup targets.

## When A Restart Is Still Needed

Live preview helps with content iteration, but a restart or full resource reload can still be needed when:

- you change code rather than guide content
- you change build logic
- you add resources outside the configured development source tree

## Related Pages

- [Installation](Installation)
- [Getting Started](Getting-Started)
- [GameScene](GameScene)
- [FAQ](FAQ)
